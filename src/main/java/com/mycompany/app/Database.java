package com.mycompany.app;

import java.io.Serializable;
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
      System.out.println("Parsed AST: "); // should get some sort of AST pretty printing
      for (Expr expr : parser.parse()) {
        System.out.println(expr.toString());
      }
    }

    List<Expr> statements = parser.parse();
    if (debug) {
      System.out.println("Statements: ");
      for (Expr statement : statements) {
        System.out.println(statement.toString());
      }
    }

    // execute the statements
    for (Expr statement : statements) {
      if (statement instanceof Expr.Select) {
        executeSelect((Expr.Select) statement);
      }
    }

    return false;
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
   * @param definitions Definition of each columns
   * @param name Name of the table
   */
  public void createTable(List<Column> definitions, String name) {
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
