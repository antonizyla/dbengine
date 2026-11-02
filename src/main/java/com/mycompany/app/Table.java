package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

  // hold the primary key and the row, in insertion order
  private final HashMap<String, List<String>> data;

  /** Separates the parts of a composite primary key; cannot appear in normal input. */
  private static final String PK_SEPARATOR = "\u001F";

  /**
   * Check if the table has a column with the given name.
   *
   * @param columnName The column to check for
   * @return if the column exists in the table
   */
  public boolean hasColumn(final String columnName) {
    return columnLocationMap.containsKey(columnName);
  }

  /**
   * The schema of the table in column order.
   *
   * @return copy of the column definitions
   */
  public List<Column> getColumns() {
    return new ArrayList<>(this.columns);
  }

  /**
   * The columns of the table that must be given a value.
   *
   * @return the columns that are NOT NULL
   */
  public List<Column> getNonNullableColumns() {
    return new ArrayList<Column>(
        this.columns.stream().filter((c) -> !c.nullable()).collect(Collectors.toList()));
  }

  /**
   * Get the definition of a column by name.
   *
   * @param columnName the column to look up
   * @return the column definition
   */
  public Column getColumn(String columnName) {
    Integer location = columnLocationMap.get(columnName);
    if (location == null || location == 0) {
      throw new RuntimeException("Column " + columnName + " does not exist in table " + name);
    }
    // row offset 0 holds the internal primary key, so schema index is one less
    return columns.get(location - 1);
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

    data = new LinkedHashMap<>();
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
    for (var col : colmns) {
      if (col.equals("*")) {
        cols.addAll(getColumnNames());
      } else {
        cols.add(col);
      }
    }

    ArrayList<Integer> indexes = new ArrayList<>();
    for (var col : cols) {
      Integer index = columnLocationMap.get(col);
      if (index == null) {
        throw new RuntimeException("Column " + col + " does not exist in table " + name);
      }
      indexes.add(index);
    }

    List<List<String>> res = new ArrayList<>();
    for (var r : data.values()) {
      if (limit != null && res.size() >= limit) {
        break;
      }
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
      System.out.printf("%s, ", attr == null ? "NULL" : attr);
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
    List<String> parts = new ArrayList<>(pkIndexes.size());
    for (var index : pkIndexes) {
      String part = row.get(index);
      if (part == null) {
        throw new RuntimeException(
            "Primary key column " + columns.get(index).name() + " cannot be NULL");
      }
      parts.add(part);
    }
    return String.join(PK_SEPARATOR, parts);
  }

  /**
   * Insert a row into the table. Values are in schema order; {@code null} represents SQL NULL.
   *
   * @param r the row to insert as list of string
   * @throws RuntimeException if the row has the wrong width or its primary key is NULL or already
   *     present
   */
  public void insert(final List<String> r) {
    if (r.size() != columns.size()) {
      throw new RuntimeException(
          String.format("Table %s expects %d values but got %d", name, columns.size(), r.size()));
    }
    String pk = getPk(r);
    if (data.containsKey(pk)) {
      throw new RuntimeException(
          "Duplicate primary key " + pk.replace(PK_SEPARATOR, ", ") + " in table " + name);
    }
    List<String> row = new ArrayList<>(r.size() + 1);
    row.add(pk);
    row.addAll(r);
    data.put(pk, row);
  }

  /**
   * Check whether any row holds the given value in a column, used for foreign key checks.
   *
   * @param columnName the column to search
   * @param value the value to look for
   * @return true if at least one row has that value
   */
  public boolean containsValue(final String columnName, final String value) {
    int index = columnLocationMap.get(getColumn(columnName).name());
    for (var row : data.values()) {
      if (value.equals(row.get(index))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Number of rows stored in the table.
   *
   * @return row count
   */
  public int rowCount() {
    return data.size();
  }
}
