package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/** A representation of a Database table to interact with it using java. */
public class Table implements Serializable {

  private final ArrayList<Column> columns; // the 'schema' of the table
  private final HashMap<String, Integer> columnLocationMap; // where in the row a specific column is
  // ^ above is always {pk} {col1}, {col2}, ..., {coln} where pk is the primary
  // key
  private final String name; // table name

  private final ArrayList<Integer> pkIndexes;

  private final HashMap<String, List<String>> data; // hold the primary key and the row

  /**
   * Check if the table has a column with the given name.
   *
   * @param columnName The column to check for
   * @return if the column exists in the table
   */
  public boolean hasColumn(final String columnName) {
    return columnLocationMap.containsKey(columnName);
  }

  public List<Column> getColumns() {
    return new ArrayList<>(this.columns);
  }

  public List<Column> getNonNullableColumns() {
    return new ArrayList<Column>(
        this.columns.stream().filter((c) -> !c.nullable()).collect(Collectors.toList()));
  }

  public Column getColumn(String columnName) {
    return columns.get(columnLocationMap.get(columnName));
  }

  /**
   * Creation of a Database Table.
   *
   * @param tableName - Name of the table
   * @param cols - List of Columns to defin the schema
   */
  public Table(final String tableName, final List<Column> cols) {
    this.columns = new ArrayList<>(cols.size());
    for (var col : cols) {
      this.columns.add(col);
    }

    this.name = tableName;

    this.columnLocationMap = new HashMap<>(); // keep a map of the location each attr in each row

    this.pkIndexes = new ArrayList<>();

    for (int i = 0; i < cols.size(); i++) {
      columnLocationMap.put(cols.get(i).name(), i + 1);

      if (cols.get(i).primary()) {
        pkIndexes.add(i);
      }
    }

    columnLocationMap.put("primary_key", 0);
    /*
     * the internal primary key will be copied into row 0 regardless, also works
     * with composite keys
     */

    data = new HashMap<>();
  }

  /**
   * Name of table.
   *
   * @return Table name
   */
  public String getName() {
    return name;
  }

  private List<String> getColumnNames() {
    List<String> columnNames = new ArrayList<>(this.columns.size());
    for (int i = 0; i < this.columns.size(); i++) {
      columnNames.add(this.columns.get(i).name());
    }
    return columnNames;
  }

  /**
   * Select Data from table.
   *
   * @param colmns which columns you want to get
   * @param limit maximum number of rows
   * @return the data as list of list of strings
   */
  public List<List<String>> select(final List<String> colmns, final Integer limit) {

    ArrayList<String> cols = new ArrayList<>(colmns.size());
    cols.addAll(colmns);
    if (cols.contains("*")) {
      cols.addAll(getColumnNames());
    }

    ArrayList<Integer> indexes = new ArrayList<>();
    for (var col : cols) {
      indexes.add(columnLocationMap.get(col));
    }

    List<List<String>> res = new ArrayList<>();
    for (var r : data.values()) {
      ArrayList<String> row = new ArrayList<>();
      for (var index : indexes) {
        row.add(r.get(index));
      }
      res.add(row);
    }
    return res;
  }

  /**
   * Get the value of a column in a specific row.
   *
   * @param rowPkey the primary key of the row
   * @param columnName the column name as a string
   * @return the value of the attribute
   */
  public String getRowCol(final String rowPkey, final String columnName) {
    if (!columnLocationMap.containsKey(columnName)) {
      throw new RuntimeException("Column " + columnName + " does not exist in table " + name);
    }
    int index = columnLocationMap.get(columnName);
    return data.get(rowPkey).get(index);
  }

  /** Print the entire table's data. */
  public void printData() {
    for (var key : data.keySet()) {
      printRow(key);
    }
  }

  /**
   * Print the values in a row based on primary key.
   *
   * @param pkey the primary key of the row
   */
  public void printRow(final String pkey) {
    for (var attr : data.get(pkey)) {
      System.out.printf("%s, ", attr);
    }
    System.out.println();
  }

  /** Print the column names and if they are the primary keys of the tables. */
  public void printHeader() {
    for (var col : columns) {
      if (col.primary()) {
        System.out.print("*");
        System.out.printf("%s, ", col.name());
      } else {
        System.out.printf("%s, ", col.name());
      }
    }
    System.out.println();
  }

  /**
   * Generate the primary key for a row based on the indexes of the primary keys.
   *
   * @param row the row to generate the primary key
   * @return the primary key as a string
   */
  private String getPk(final List<String> row) {
    String pk = "";
    for (var index : pkIndexes) {
      pk = String.join(pk, row.get(index));
    }
    return pk;
  }

  /**
   * Insert a row into the table.
   *
   * @param r the row to insert as list of string
   */
  public void insert(final List<String> r) {
    List<String> row = new ArrayList<>(r.size() + 1);
    row.add(getPk(r));
    row.addAll(r);
    data.put(row.get(0), row); // this cannot be row.getfirst()
  }
}
