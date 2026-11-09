package com.mycompany.app;

import com.mycompany.app.Expr.Create;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Database Object. */
public final class Database implements Serializable {

  /** Pinned to the value before foreign keys were added, so older database files still load. */
  private static final long serialVersionUID = -1438848577340601931L;

  private HashMap<String, Table> tables;

  /**
   * Foreign keys grouped by the table they reference. Derived from the tables' own constraints, so
   * it is never stored; {@code null} means it must be rebuilt.
   */
  private transient Map<String, List<ForeignKey>> incoming;

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
      } else if (statement instanceof Expr.Delete) {
        System.out.println(executeDelete((Expr.Delete) statement));
      } else if (statement instanceof Expr.DropTable) {
        executeDropTable((Expr.DropTable) statement);
        System.out.println("Table " + ((Expr.DropTable) statement).table.lexeme + " dropped.");
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

    Set<String> columnNames = new HashSet<>();
    for (Column column : statement.columns) {
      if (!columnNames.add(column.name())) {
        throw new RuntimeException(
            "Column " + column.name() + " is defined more than once in " + statement.tableName);
      }
    }

    // the table without constraints, used to check them; a table may reference itself
    Table candidate = new Table(statement.tableName, statement.columns);
    List<ForeignKey> foreignKeys = new ArrayList<>();
    Set<String> constraintNames = new HashSet<>();
    for (ForeignKey fk : statement.foreignKeys) {
      foreignKeys.add(validateForeignKey(fk, candidate, constraintNames));
    }

    if (debug) {
      System.out.println("Foreign keys reference primary keys of tables in the database.");
    }

    // create the table
    tables.put(statement.tableName, new Table(statement.tableName, statement.columns, foreignKeys));
    incoming = null;
    dirty = true;
  }

  /**
   * Check a parsed foreign key against the tables it joins and put it in its stored form: named,
   * and with the parent columns in the order of the parent's primary key.
   *
   * @param fk the parsed constraint
   * @param child the table being created
   * @param usedNames constraint names already taken in the child table, updated
   * @return the validated constraint
   */
  private ForeignKey validateForeignKey(ForeignKey fk, Table child, Set<String> usedNames) {
    String reference = "Foreign key `" + fk.describeParent() + "`";
    Table parent = fk.parentTable().equals(child.getName()) ? child : getTable(fk.parentTable());
    if (parent == null) {
      throw new RuntimeException(
          reference + " references table " + fk.parentTable() + " which does not exist");
    }
    for (String column : fk.parentColumns()) {
      if (!parent.hasColumn(column) || column.equals("primary_key")) {
        throw new RuntimeException(reference + " does not exist in table " + parent.getName());
      }
    }
    for (String column : fk.childColumns()) {
      if (!child.hasColumn(column) || column.equals("primary_key")) {
        throw new RuntimeException(
            "Foreign key column " + column + " does not exist in table " + child.getName());
      }
    }
    if (new HashSet<>(fk.childColumns()).size() != fk.childColumns().size()) {
      throw new RuntimeException(reference + " lists a column of " + child.getName() + " twice");
    }

    // referencing the full primary key means a parent row is found directly by its key
    List<String> parentKey = parent.getPrimaryKeyColumns();
    if (fk.parentColumns().size() != parentKey.size()
        || !new HashSet<>(fk.parentColumns()).equals(new HashSet<>(parentKey))) {
      throw new RuntimeException(
          reference
              + " must reference the primary key of "
              + parent.getName()
              + " ("
              + String.join(", ", parentKey)
              + ")");
    }
    List<String> childColumns = new ArrayList<>(parentKey.size());
    for (String parentColumn : parentKey) {
      childColumns.add(fk.childColumns().get(fk.parentColumns().indexOf(parentColumn)));
    }

    for (int i = 0; i < parentKey.size(); i++) {
      Column childColumn = child.getColumn(childColumns.get(i));
      Column parentColumn = parent.getColumn(parentKey.get(i));
      if (childColumn.type() != parentColumn.type()) {
        throw new RuntimeException(
            String.format(
                "Foreign key column %s is a %s but %s.%s is a %s",
                childColumn.name(),
                typeName(childColumn.type()),
                parent.getName(),
                parentColumn.name(),
                typeName(parentColumn.type())));
      }
      if (fk.onDelete() == ReferentialAction.SET_NULL && !childColumn.nullable()) {
        throw new RuntimeException(
            "ON DELETE SET NULL needs column " + childColumn.name() + " to allow NULL");
      }
      if (fk.onDelete() == ReferentialAction.SET_DEFAULT
          && (childColumn.defaultValue() == null || childColumn.primary())) {
        throw new RuntimeException(
            "ON DELETE SET DEFAULT needs a DEFAULT on non key column " + childColumn.name());
      }
    }

    String name = "fk_" + child.getName() + "_" + String.join("_", childColumns);
    String unique = name;
    for (int i = 2; !usedNames.add(unique); i++) {
      unique = name + "_" + i;
    }
    return new ForeignKey(
        unique, child.getName(), childColumns, parent.getName(), parentKey, fk.onDelete());
  }

  private static String typeName(TokenType type) {
    return type == TokenType.STRING_TYPE ? "STRING" : String.valueOf(type);
  }

  /**
   * Foreign keys grouped by the table they reference.
   *
   * @return parent table name to the constraints referencing it
   */
  private Map<String, List<ForeignKey>> incoming() {
    if (incoming == null) {
      incoming = new HashMap<>();
      for (Table table : tables.values()) {
        for (ForeignKey fk : table.getForeignKeys()) {
          incoming.computeIfAbsent(fk.parentTable(), k -> new ArrayList<>()).add(fk);
        }
      }
    }
    return incoming;
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
   * Validate an insert statement against the table schema and store the row. The row is only stored
   * once every check has passed, so a failed insert leaves the table unchanged.
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

    // build the row in schema order, unspecified columns take their default or NULL
    List<String> row = new ArrayList<>();
    for (Column column : table.getColumns()) {
      int position = targetColumns.indexOf(column.name());
      String stored;
      if (position < 0) {
        stored = column.defaultValue();
      } else {
        Object value = ((Expr.Literal) stmt.values.get(position)).value;
        stored = value == null ? null : column.toStoredValue(value);
      }

      if (stored == null && !column.nullable()) {
        throw new RuntimeException("Column " + column.name() + " cannot be NULL");
      }
      row.add(stored);
    }

    // every foreign key must match an existing parent row, unless part of it is NULL
    for (ForeignKey fk : table.getForeignKeys()) {
      String parentKey = table.referencedKeyOfValues(fk, row);
      if (parentKey == null) {
        continue;
      }
      // a row may reference itself
      if (fk.parentTable().equals(table.getName()) && parentKey.equals(table.keyOfValues(row))) {
        continue;
      }
      Table parent = getTable(fk.parentTable());
      if (parent == null || !parent.containsKey(parentKey)) {
        throw new RuntimeException(
            "Foreign key violation: "
                + Table.displayKey(parentKey)
                + " does not exist in "
                + fk.describeParent());
      }
    }

    // primary key presence and uniqueness are enforced by the table
    table.insert(row);
    dirty = true;
  }

  /** A row of a table, identified by its internal key. */
  private record RowRef(String table, String key) {}

  /** A child row whose foreign key would stop a delete unless the child is deleted too. */
  private record Blocker(ForeignKey fk, String childKey, String parentKey) {}

  /**
   * Delete the rows matching the WHERE clause and apply the ON DELETE action of every foreign key
   * that references them. Everything that would change is worked out first and only applied once
   * every check has passed, so a failed delete leaves the database unchanged.
   *
   * @param stmt the parsed delete statement
   * @return a summary of what changed
   * @throws RuntimeException if the table or a column does not exist, or a referenced row is
   *     protected by NO ACTION or RESTRICT
   */
  private String executeDelete(Expr.Delete stmt) {
    Table table = getTable(stmt.table.lexeme);
    if (table == null) {
      throw new RuntimeException("Table " + stmt.table.lexeme + " does not exist.");
    }
    Map<String, String> equalities = new LinkedHashMap<>();
    if (!stmt.whereClause.isEmpty()) {
      collectEqualities(table, stmt.whereClause.get(0), equalities);
    }
    List<String> matches = table.findKeys(equalities);

    // follow the foreign keys from every deleted row, breadth first
    Map<String, Set<String>> toDelete = new LinkedHashMap<>();
    Map<String, Map<String, Map<String, String>>> toUpdate = new LinkedHashMap<>();
    List<Blocker> blockers = new ArrayList<>();
    Map<RowRef, List<ForeignKey>> setToDefault = new LinkedHashMap<>();
    Deque<RowRef> queue = new ArrayDeque<>();
    for (String key : matches) {
      queue.add(new RowRef(table.getName(), key));
    }

    while (!queue.isEmpty()) {
      RowRef row = queue.poll();
      // also ends cycles, including rows that reference themselves
      if (!toDelete.computeIfAbsent(row.table(), k -> new LinkedHashSet<>()).add(row.key())) {
        continue;
      }
      for (ForeignKey fk : incoming().getOrDefault(row.table(), List.of())) {
        Table child = getTable(fk.childTable());
        for (String childKey : child.rowsReferencing(fk, row.key())) {
          switch (fk.onDelete()) {
            case CASCADE -> queue.add(new RowRef(child.getName(), childKey));
            case SET_NULL, SET_DEFAULT -> {
              Map<String, String> values =
                  toUpdate
                      .computeIfAbsent(child.getName(), k -> new LinkedHashMap<>())
                      .computeIfAbsent(childKey, k -> new LinkedHashMap<>());
              for (String column : fk.childColumns()) {
                values.put(
                    column,
                    fk.onDelete() == ReferentialAction.SET_NULL
                        ? null
                        : child.getColumn(column).defaultValue());
              }
              if (fk.onDelete() == ReferentialAction.SET_DEFAULT) {
                setToDefault
                    .computeIfAbsent(new RowRef(child.getName(), childKey), k -> new ArrayList<>())
                    .add(fk);
              }
            }
            default -> blockers.add(new Blocker(fk, childKey, row.key()));
          }
        }
      }
    }

    // a referencing row only blocks the delete if it is not being deleted as well
    for (Blocker blocker : blockers) {
      String childTable = blocker.fk().childTable();
      if (!toDelete.getOrDefault(childTable, Set.of()).contains(blocker.childKey())) {
        throw new RuntimeException(
            String.format(
                "Cannot delete %s from %s: row %s of %s still references it through %s (ON DELETE"
                    + " %s)",
                Table.displayKey(blocker.parentKey()),
                blocker.fk().parentTable(),
                Table.displayKey(blocker.childKey()),
                childTable,
                blocker.fk().name(),
                blocker.fk().onDelete()));
      }
    }

    // rows being deleted do not need their references changed first
    for (var entry : toUpdate.entrySet()) {
      Set<String> deleted = toDelete.getOrDefault(entry.getKey(), Set.of());
      entry.getValue().keySet().removeIf(deleted::contains);
    }

    // a default must itself reference a row that will still exist
    for (var entry : setToDefault.entrySet()) {
      RowRef ref = entry.getKey();
      if (toDelete.getOrDefault(ref.table(), Set.of()).contains(ref.key())) {
        continue;
      }
      Table child = getTable(ref.table());
      for (ForeignKey fk : entry.getValue()) {
        List<String> defaults = new ArrayList<>();
        for (String column : fk.childColumns()) {
          defaults.add(child.getColumn(column).defaultValue());
        }
        String parentKey = Table.joinKey(defaults);
        boolean parentRemains =
            getTable(fk.parentTable()).containsKey(parentKey)
                && !toDelete.getOrDefault(fk.parentTable(), Set.of()).contains(parentKey);
        boolean selfReference = fk.parentTable().equals(ref.table()) && parentKey.equals(ref.key());
        if (!parentRemains && !selfReference) {
          throw new RuntimeException(
              String.format(
                  "Cannot delete from %s: ON DELETE SET DEFAULT of %s needs %s to exist in %s",
                  table.getName(), fk.name(), Table.displayKey(parentKey), fk.describeParent()));
        }
      }
    }

    // every check has passed, apply the changes
    int updated = 0;
    for (var entry : toUpdate.entrySet()) {
      Table child = getTable(entry.getKey());
      for (var change : entry.getValue().entrySet()) {
        child.setValues(change.getKey(), change.getValue());
        updated++;
      }
    }
    int deleted = 0;
    for (var entry : toDelete.entrySet()) {
      Table target = getTable(entry.getKey());
      for (String key : entry.getValue()) {
        target.delete(key);
        deleted++;
      }
    }
    if (deleted > 0 || updated > 0) {
      dirty = true;
    }

    StringBuilder summary = new StringBuilder();
    summary.append(plural(matches.size())).append(" deleted from ").append(table.getName());
    if (deleted > matches.size()) {
      summary.append(", ").append(plural(deleted - matches.size())).append(" cascaded");
    }
    if (updated > 0) {
      summary.append(", ").append(plural(updated)).append(" updated by ON DELETE SET");
    }
    return summary.append('.').toString();
  }

  private static String plural(int rows) {
    return rows + (rows == 1 ? " row" : " rows");
  }

  /**
   * Turn a WHERE condition of the form {@code col = value [AND col = value ...]} into the stored
   * values each column must equal. A column required to equal two different values gets {@code
   * null}, which matches no row.
   *
   * @param table the table the condition is on
   * @param condition the parsed condition
   * @param result column name to stored value, filled in
   */
  private static void collectEqualities(Table table, Expr condition, Map<String, String> result) {
    if (condition instanceof Expr.Grouping grouping) {
      collectEqualities(table, grouping.expression, result);
      return;
    }
    if (condition instanceof Expr.Logical logical && logical.operator.type == TokenType.AND) {
      collectEqualities(table, logical.left, result);
      collectEqualities(table, logical.right, result);
      return;
    }
    if (condition instanceof Expr.Binary binary
        && binary.operator.type == TokenType.EQUALS
        && binary.left instanceof Expr.Literal left
        && left.value instanceof String columnName) {
      if (!table.hasColumn(columnName) || columnName.equals("primary_key")) {
        throw new RuntimeException(
            "Column " + columnName + " does not exist in table " + table.getName());
      }
      Object value = literalValue(binary.right);
      if (value != null) {
        String stored = table.getColumn(columnName).toStoredValue(value);
        if (result.containsKey(columnName) && !stored.equals(result.get(columnName))) {
          stored = null;
        }
        result.put(columnName, stored);
        return;
      }
    }
    throw new RuntimeException(
        "DELETE only supports WHERE conditions of the form column = value joined by AND");
  }

  /**
   * The value of a literal, allowing a leading minus on numbers.
   *
   * @param expr the expression
   * @return a {@link Double} or {@link String}, or {@code null} if it is not a literal
   */
  private static Object literalValue(Expr expr) {
    if (expr instanceof Expr.Literal literal) {
      return literal.value;
    }
    if (expr instanceof Expr.Unary unary
        && unary.operator.type == TokenType.MINUS
        && unary.right instanceof Expr.Literal literal
        && literal.value instanceof Double number) {
      return -number;
    }
    return null;
  }

  /**
   * Remove a table, refusing if another table still references it.
   *
   * @param stmt the parsed drop statement
   */
  private void executeDropTable(Expr.DropTable stmt) {
    String name = stmt.table.lexeme;
    if (getTable(name) == null) {
      throw new RuntimeException("Table " + name + " does not exist.");
    }
    for (ForeignKey fk : incoming().getOrDefault(name, List.of())) {
      if (!fk.childTable().equals(name)) {
        throw new RuntimeException(
            "Cannot drop table "
                + name
                + ": it is referenced by "
                + fk.name()
                + " in table "
                + fk.childTable());
      }
    }
    tables.remove(name);
    incoming = null;
    dirty = true;
  }

  /**
   * Upgrade tables from files written before foreign keys were constraints. Their single column
   * references become NO ACTION foreign keys where they point at a primary key.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    for (Table table : tables.values()) {
      if (!table.needsForeignKeyMigration()) {
        continue;
      }
      List<ForeignKey> foreignKeys = new ArrayList<>();
      for (Column column : table.getColumns()) {
        if (column.dependsOn() == null) {
          continue;
        }
        String[] reference = column.dependsOn().split("\\.");
        Table parent = tables.get(reference[0]);
        if (parent == null || !parent.getPrimaryKeyColumns().equals(List.of(reference[1]))) {
          System.err.println(
              "[Database] Dropping foreign key "
                  + table.getName()
                  + "."
                  + column.name()
                  + ": "
                  + column.dependsOn()
                  + " is not a primary key");
          continue;
        }
        foreignKeys.add(
            new ForeignKey(
                "fk_" + table.getName() + "_" + column.name(),
                table.getName(),
                List.of(column.name()),
                parent.getName(),
                List.of(reference[1]),
                ReferentialAction.NO_ACTION));
      }
      table.setForeignKeys(foreignKeys);
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
