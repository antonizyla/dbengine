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
