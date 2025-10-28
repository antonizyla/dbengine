package com.mycompany.app;

import com.mycompany.app.Expr.Create;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Database Object. */
public final class Database implements Serializable {

  private HashMap<String, Table> tables;
  private String name;
  private String filepath;

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
   * @return true if the query was successful, false otherwise
   */
  public boolean runQuery(String query) {
    return runQuery(query, false);
  }

  /**
   * Public Interface to run a query on the database.
   *
   * @param query SQL query to run on the database
   * @param debug If true, print debug information along the way
   * @return true if the query was successful, false otherwise
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
    if (debug) {
      System.out.println("Parsed AST: ");
      // TODO: some sort of AST pretty printing
      for (Expr expr : parser.parse()) {
        System.out.println(expr.toString());
      }
    }

    List<Expr> statements = parser.parse();

    // execute the statements
    for (Expr statement : statements) {
      if (statement instanceof Expr.Select) {
        executeSelect((Expr.Select) statement);
      } else if (statement instanceof Expr.Create) {
        System.out.println("Executing CREATE statement: " + statement.toString());
        executeCreateTable((Expr.Create) statement, false);
      } else if (statement instanceof Expr.Insert) {
        System.out.println("Executing Insert statement: " + statement.toString());
      }
    }

    return false;
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
        Table foreignTable = getTable(column.dependsOn().split(".")[0]);
        if (!foreignTable.hasColumn(column.dependsOn().split(".")[1])) {
          throw new RuntimeException(
              "Foreign key `"
                  + column.dependsOn()
                  + "` does not exist in table "
                  + foreignTable.getName());
        }
        if (column.dependsOn().split(".")[0].equals(statement.tableName)) {
          throw new RuntimeException(
              "Foreign key `"
                  + column.dependsOn()
                  + "` cannot reference the same table "
                  + statement.tableName);
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
  }

  private void executeSelect(Expr.Select select) {
    // get the table from the database
    Table table = getTable(select.table.lexeme);
    if (table == null) {
      System.err.println("Table " + select.table.lexeme + " does not exist.");
      return;
    }
    // check that each of the columns exists
    for (Expr variable : select.variables) {
      if (variable instanceof Expr.Literal) { // to allow for having expresions like `col + 1`
        String columnName = ((Expr.Literal) variable).value.toString();
        if (!table.hasColumn(columnName)) {
          System.err.println(
              "Column " + columnName + " does not exist in table " + table.getName());
          return;
        }
      } else {
        System.err.println("Invalid column name: " + variable.toString());
        return;
      }
    }
  }

  private void executeInsert(Expr.Insert stmt) {
    // check that table exists
    Table table = getTable(stmt.table.lexeme);
    if (table == null) {
      System.err.println("Table " + stmt.table.lexeme + " does not exist.");
      return;
    }

    List<Column> nonNullableInserted = new ArrayList<>();

    // check that each column provided exists
    for (Expr column : stmt.columns) {
      Expr.Literal col = (Expr.Literal) column;
      if (!table.hasColumn(col.value.toString())) {
        System.err.println(
            "Table " + stmt.table.lexeme + " does not contain column " + col.value.toString());
        return;
      }
      if (!table.getColumn(name).nullable()) {
        nonNullableInserted.add(table.getColumn(name));
      }
    }
    // check that any unspecified columns are nullable
    // by comparing nonNullableInserted and nonNullable
    List<Column> nonNullable = table.getNonNullableColumns();
    if (nonNullable.size() != nonNullableInserted.size()) {
      System.err.println(
          "There is 1 or more columns that are non nullable that have not been given a value");
      return;
    }

    // Actually insert into the table
    List<String> row = new ArrayList<>();
    for (Column column : table.getColumns()) {
      if (stmt.columns.contains(new Expr.Literal(column.name()))) {
        row.add(stmt.values.get(stmt.columns.indexOf(new Expr.Literal(column.name()))).toString());
      } else if (column.nullable()) {
        row.add(""); // or some default value
      } else {
        System.err.println("Column " + column.name() + " is non-nullable and has no value.");
        return;
      }
    }
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
  }

  /**
   * Change the schema of a table by providing a new schema.
   *
   * @param tableName The table in the database to be changed
   * @param definitions The new schema as a set of column definitions
   */
  public void alterTable(String tableName, List<Column> definitions) {}

  /** Commit the changes as a transaction and write to the filesystem. */
  public void commit() {}

  /** Rollback the database to the first transaction. */
  public void rollback() {}

  /** Begin a database Transaction. */
  public void startTransaction() {}

  /** Print the name of the database. */
  public void printAbout() {
    System.out.printf("[Database %s] \n");
  }
}
