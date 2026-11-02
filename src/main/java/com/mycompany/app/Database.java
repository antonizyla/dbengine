package com.mycompany.app;

import com.mycompany.app.Expr.Create;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Database Object. */
public final class Database implements Serializable {

  private HashMap<String, Table> tables;
  private String name;
  private String filepath;

  /** Whether there are changes in memory that have not yet been written to disk. */
  private transient boolean dirty;

  /**
   * Create a database based on name and filepath to store it.
   *
   * @param name Name of database to create
   * @param filepath Filepath to store the database
   */
  public Database(String name, String filepath) {
    // read the database into memory
    this.name = name;
    this.filepath = filepath;
    this.tables = new HashMap<>();
  }

  /**
   * Default public interface to run a query on the database.
   *
   * @param query SQl Qeury to execute
   * @return true if the query was successful
   * @throws RuntimeException if the query could not be parsed or executed
   */
  public boolean runQuery(String query) {
    return runQuery(query, false);
  }

  /**
   * Public Interface to run a query on the database.
   *
   * @param query SQL query to run on the database
   * @param debug If true, print debug information along the way
   * @return true if the query was successful
   * @throws RuntimeException if the query could not be parsed or executed
   */
  public boolean runQuery(String query, boolean debug) {
    // scan query into tokens
    Scanner scanner = new Scanner(query);
    List<Token> tokens = scanner.scanTokens();
    if (debug) {
      System.out.println("Tokens: ");
      for (Token token : tokens) {
        System.out.println(token.toString());
      }
    }

    // parse the tokens into an AST
    Parser parser = new Parser(tokens);
    List<Expr> statements = parser.parse();
    if (debug) {
      System.out.println("Parsed AST: ");
      // TODO: some sort of AST pretty printing
      for (Expr expr : statements) {
        System.out.println(expr);
      }
    }

    // execute the statements
    for (Expr statement : statements) {
      if (statement instanceof Expr.Select) {
        executeSelect((Expr.Select) statement);
      } else if (statement instanceof Expr.Create) {
        executeCreateTable((Expr.Create) statement, debug);
        System.out.println("Table " + ((Expr.Create) statement).tableName + " created.");
      } else if (statement instanceof Expr.Insert) {
        executeInsert((Expr.Insert) statement);
        System.out.println("1 row inserted into " + ((Expr.Insert) statement).table.lexeme + ".");
      } else {
        throw new RuntimeException("Unsupported or unrecognised statement: " + query);
      }
    }

    return true;
  }

  private void executeCreateTable(Create statement, boolean debug) {
    // check that the table does not already exist
    if (tables.containsKey(statement.tableName)) {
      throw new RuntimeException("Table " + statement.tableName + " already exists.");
    }

    if (debug) {
      System.out.println("Creating table: " + statement.tableName);
      for (Column column : statement.columns) {
        System.out.println(
            "Column: "
                + column.name()
                + ", Type: "
                + column.type()
                + ", Nullable: "
                + column.nullable()
                + ", Primary: "
                + column.primary()
                + ", Foreign Key: "
                + column.dependsOn());
      }
      System.out.println("Table does not already exist, proceeding with creation.");
    }

    // check that foreign keys are valid
    for (Column column : statement.columns) {
      if (column.dependsOn() != null) {
        String[] reference = column.dependsOn().split("\\.");
        if (reference[0].equals(statement.tableName)) {
          throw new RuntimeException(
              "Foreign key `"
                  + column.dependsOn()
                  + "` cannot reference the same table "
                  + statement.tableName);
        }
        Table foreignTable = getTable(reference[0]);
        if (foreignTable == null) {
          throw new RuntimeException(
              "Foreign key `"
                  + column.dependsOn()
                  + "` references table "
                  + reference[0]
                  + " which does not exist");
        }
        if (!foreignTable.hasColumn(reference[1])) {
          throw new RuntimeException(
              "Foreign key `"
                  + column.dependsOn()
                  + "` does not exist in table "
                  + foreignTable.getName());
        }
      }
    }

    if (debug) {
      System.out.println("Foreign keys exist in other tables in database.");
    }

    // check that there exists at least one primary key
    boolean hasPrimaryKey = false;
    for (Column column : statement.columns) {
      if (column.primary()) {
        hasPrimaryKey = true;
        break;
      }
    }
    if (!hasPrimaryKey) {
      throw new RuntimeException(
          "Table " + statement.tableName + " must have at least one primary key.");
    }

    // create the table
    tables.put(statement.tableName, new Table(statement.tableName, statement.columns));
    dirty = true;
  }

  private void executeSelect(Expr.Select select) {
    // get the table from the database
    Table table = getTable(select.table.lexeme);
    if (table == null) {
      throw new RuntimeException("Table " + select.table.lexeme + " does not exist.");
    }
    if (!select.whereClause.isEmpty()) {
      throw new RuntimeException("WHERE clauses are not supported yet.");
    }

    // check that each of the columns exists
    List<String> columnNames = new ArrayList<>();
    for (Expr variable : select.variables) {
      if (!(variable instanceof Expr.Literal)) {
        throw new RuntimeException("Invalid column name: " + variable);
      }
      String columnName = ((Expr.Literal) variable).value.toString();
      if (!columnName.equals("*") && !table.hasColumn(columnName)) {
        throw new RuntimeException(
            "Column " + columnName + " does not exist in table " + table.getName());
      }
      columnNames.add(columnName);
    }

    // print the results
    List<String> header = new ArrayList<>();
    for (String columnName : columnNames) {
      if (columnName.equals("*")) {
        table.getColumns().forEach(c -> header.add(c.name()));
      } else {
        header.add(columnName);
      }
    }
    System.out.println(String.join(" | ", header));
    List<List<String>> rows = table.select(columnNames, null);
    for (List<String> row : rows) {
      List<String> printable = new ArrayList<>(row.size());
      for (String value : row) {
        printable.add(value == null ? "NULL" : value);
      }
      System.out.println(String.join(" | ", printable));
    }
    System.out.println("(" + rows.size() + " rows)");
  }

  /**
   * Validate an insert statement against the table schema and store the row. The row is only
   * stored once every check has passed, so a failed insert leaves the table unchanged.
   *
   * @param stmt the parsed insert statement
   * @throws RuntimeException describing the first validation failure
   */
  private void executeInsert(Expr.Insert stmt) {
    // check that table exists
    Table table = getTable(stmt.table.lexeme);
    if (table == null) {
      throw new RuntimeException("Table " + stmt.table.lexeme + " does not exist.");
    }

    // resolve the target columns, `*` or no column list means every column in order
    List<String> targetColumns = new ArrayList<>();
    if (stmt.columns.size() == 1
        && Parser.ALL_COLUMNS.equals(((Expr.Literal) stmt.columns.get(0)).value)) {
      table.getColumns().forEach(c -> targetColumns.add(c.name()));
    } else {
      Set<String> seen = new HashSet<>();
      for (Expr column : stmt.columns) {
        String columnName = (String) ((Expr.Literal) column).value;
        if (!table.hasColumn(columnName) || columnName.equals("primary_key")) {
          throw new RuntimeException(
              "Table " + stmt.table.lexeme + " does not contain column " + columnName);
        }
        if (!seen.add(columnName)) {
          throw new RuntimeException("Column " + columnName + " is specified more than once");
        }
        targetColumns.add(columnName);
      }
    }

    if (targetColumns.isEmpty()) {
      throw new RuntimeException("No columns specified to insert into");
    }

    // check that each column has exactly one value
    if (targetColumns.size() != stmt.values.size()) {
      throw new RuntimeException(
          String.format(
              "%d columns were specified but %d values were given",
              targetColumns.size(), stmt.values.size()));
    }

    // build the row in schema order, unspecified columns are NULL
    List<String> row = new ArrayList<>();
    for (Column column : table.getColumns()) {
      int position = targetColumns.indexOf(column.name());
      Object value =
          position < 0 ? null : ((Expr.Literal) stmt.values.get(position)).value;

      if (value == null) {
        if (!column.nullable()) {
          throw new RuntimeException("Column " + column.name() + " cannot be NULL");
        }
        row.add(null);
        continue;
      }

      String stored = toStoredValue(column, value);

      // a foreign key value must exist in the referenced table
      if (column.dependsOn() != null) {
        String[] reference = column.dependsOn().split("\\.");
        Table foreignTable = getTable(reference[0]);
        if (foreignTable == null || !foreignTable.containsValue(reference[1], stored)) {
          throw new RuntimeException(
              "Foreign key violation: "
                  + stored
                  + " does not exist in "
                  + column.dependsOn());
        }
      }

      row.add(stored);
    }

    // primary key presence and uniqueness are enforced by the table
    table.insert(row);
    dirty = true;
  }

  /**
   * Check a literal value against the column type and convert it into its stored form.
   *
   * @param column the column the value is going into
   * @param value the non-null literal value, either a {@link Double} or a {@link String}
   * @return the value as stored in the table
   */
  private static String toStoredValue(Column column, Object value) {
    if (column.type() == TokenType.NUMBER) {
      if (!(value instanceof Double)) {
        throw new RuntimeException(
            "Type mismatch: column " + column.name() + " expects a NUMBER but got '" + value + "'");
      }
      double number = (Double) value;
      if (number == Math.rint(number) && !Double.isInfinite(number)) {
        return Long.toString((long) number);
      }
      return Double.toString(number);
    }
    if (column.type() == TokenType.STRING_TYPE && !(value instanceof String)) {
      throw new RuntimeException(
          "Type mismatch: column " + column.name() + " expects a STRING but got " + value);
    }
    return value.toString();
  }

  /**
   * Return the table with certain name.
   *
   * @param name Name of table to retieve
   * @return the table object
   */
  public Table getTable(String name) {
    return tables.get(name);
  }

  /**
   * Return the location of the database file on the local disk.
   *
   * @return Filepath of that databse
   */
  public String getFilePath() {
    return filepath;
  }

  /**
   * Return the database name as a string.
   *
   * @return Name of the database
   */
  public String getName() {
    return this.name;
  }

  /**
   * Create table from definitions of columns and name.
   *
   * @param name Name of the table
   * @param definitions Definition of each columns
   */
  public void createTable(String name, List<Column> definitions) {
    tables.put(name, new Table(name, definitions));
    dirty = true;
  }

  /**
   * Change the schema of a table by providing a new schema.
   *
   * @param tableName The table in the database to be changed
   * @param definitions The new schema as a set of column definitions
   */
  public void alterTable(String tableName, List<Column> definitions) {}

  /**
   * Whether the database has changes that have not been committed to disk.
   *
   * @return true if there are uncommitted changes
   */
  public boolean hasUncommittedChanges() {
    return dirty;
  }

  /** Commit the changes as a transaction and write to the filesystem. */
  public void commit() {
    if (dirty) {
      Disk.writeDatabase(this);
      dirty = false;
    }
  }

  /** Rollback the database to the first transaction. */
  public void rollback() {}

  /** Begin a database Transaction. */
  public void startTransaction() {}

  /** Print the name of the database. */
  public void printAbout() {
    System.out.printf("[Database %s] %n", name);
  }
}
