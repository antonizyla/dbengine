package com.mycompany.app;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** A representation of a Database table to interact with it using java. */
public class Table implements Serializable {

  /** Pinned to the value before foreign keys were added, so older database files still load. */
  private static final long serialVersionUID = -1927445668181961021L;

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

  /** Foreign keys declared by this table. Null only in files written before they existed. */
  private ArrayList<ForeignKey> foreignKeys;

  /**
   * For each foreign key name, the child rows grouped by the parent key they reference. Rows with a
   * NULL in any foreign key column reference nothing and are left out. Derived from {@link #data},
   * so it is rebuilt on load rather than stored.
   */
  private transient Map<String, Map<String, Set<String>>> fkIndex;

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
    this(tableName, cols, List.of());
  }

  /**
   * Creation of a Database Table with foreign key constraints. The constraints must already have
   * been validated against the referenced tables.
   *
   * @param tableName - Name of the table
   * @param cols - List of Columns to defin the schema
   * @param fks - Foreign keys declared by this table
   */
  public Table(final String tableName, final List<Column> cols, final List<ForeignKey> fks) {
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
    foreignKeys = new ArrayList<>(fks);
    rebuildForeignKeyIndex();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (foreignKeys != null) {
      rebuildForeignKeyIndex();
    }
  }

  /**
   * Whether this table was loaded from a file written before foreign key constraints existed.
   *
   * @return true if {@link #setForeignKeys} still needs to be called
   */
  boolean needsForeignKeyMigration() {
    return foreignKeys == null;
  }

  /**
   * Replace the foreign keys of this table, used when upgrading an older database file.
   *
   * @param fks the validated constraints
   */
  void setForeignKeys(final List<ForeignKey> fks) {
    foreignKeys = new ArrayList<>(fks);
    rebuildForeignKeyIndex();
  }

  /**
   * The foreign keys declared by this table.
   *
   * @return unmodifiable list of constraints
   */
  public List<ForeignKey> getForeignKeys() {
    return foreignKeys == null ? List.of() : Collections.unmodifiableList(foreignKeys);
  }

  /**
   * Names of the primary key columns, in key order.
   *
   * @return the primary key column names
   */
  public List<String> getPrimaryKeyColumns() {
    List<String> names = new ArrayList<>(pkIndexes.size());
    for (var index : pkIndexes) {
      names.add(columns.get(index).name());
    }
    return names;
  }

  /**
   * Join the parts of a key the same way rows are keyed internally.
   *
   * @param parts the key values in primary key order
   * @return the internal key
   */
  public static String joinKey(final List<String> parts) {
    return String.join(PK_SEPARATOR, parts);
  }

  /**
   * Make an internal key readable for error messages.
   *
   * @param key the internal key
   * @return the key parts separated by commas
   */
  public static String displayKey(final String key) {
    return key.replace(PK_SEPARATOR, ", ");
  }

  private void rebuildForeignKeyIndex() {
    fkIndex = new HashMap<>();
    for (var fk : foreignKeys) {
      fkIndex.put(fk.name(), new HashMap<>());
    }
    for (var entry : data.entrySet()) {
      indexRow(entry.getKey(), entry.getValue());
    }
  }

  /**
   * The parent key a row references through a foreign key.
   *
   * @param fk a foreign key declared by this table
   * @param row a stored row (offset 0 is the internal key)
   * @return the parent key, or {@code null} if any referencing column is NULL
   */
  private String referencedKey(final ForeignKey fk, final List<String> row) {
    List<String> parts = new ArrayList<>(fk.childColumns().size());
    for (var col : fk.childColumns()) {
      String value = row.get(columnLocationMap.get(col));
      if (value == null) {
        return null;
      }
      parts.add(value);
    }
    return joinKey(parts);
  }

  private void indexRow(final String pk, final List<String> row) {
    for (var fk : foreignKeys) {
      String parentKey = referencedKey(fk, row);
      if (parentKey != null) {
        fkIndex.get(fk.name()).computeIfAbsent(parentKey, k -> new LinkedHashSet<>()).add(pk);
      }
    }
  }

  private void unindexRow(final String pk, final List<String> row) {
    for (var fk : foreignKeys) {
      String parentKey = referencedKey(fk, row);
      if (parentKey == null) {
        continue;
      }
      Map<String, Set<String>> byParent = fkIndex.get(fk.name());
      Set<String> children = byParent.get(parentKey);
      if (children != null) {
        children.remove(pk);
        if (children.isEmpty()) {
          byParent.remove(parentKey);
        }
      }
    }
  }

  /**
   * The parent key a row in schema order (without the internal key) would reference.
   *
   * @param fk a foreign key declared by this table
   * @param values the row values in schema order
   * @return the parent key, or {@code null} if any referencing column is NULL
   */
  String referencedKeyOfValues(final ForeignKey fk, final List<String> values) {
    List<String> row = new ArrayList<>(values.size() + 1);
    row.add(null);
    row.addAll(values);
    return referencedKey(fk, row);
  }

  /**
   * The key a row in schema order (without the internal key) would be stored under.
   *
   * @param values the row values in schema order
   * @return the internal key
   */
  String keyOfValues(final List<String> values) {
    return getPk(values);
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
    return joinKey(parts);
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
      throw new RuntimeException("Duplicate primary key " + displayKey(pk) + " in table " + name);
    }
    List<String> row = new ArrayList<>(r.size() + 1);
    row.add(pk);
    row.addAll(r);
    data.put(pk, row);
    indexRow(pk, row);
  }

  /**
   * Whether a row with the given internal key exists.
   *
   * @param key the internal key, see {@link #joinKey}
   * @return true if the row exists
   */
  public boolean containsKey(final String key) {
    return data.containsKey(key);
  }

  /**
   * The rows that reference a parent row through one of this table's foreign keys.
   *
   * @param fk a foreign key declared by this table
   * @param parentKey internal key of the parent row
   * @return keys of the referencing rows, in insertion order
   */
  public Set<String> rowsReferencing(final ForeignKey fk, final String parentKey) {
    Set<String> children = fkIndex.get(fk.name()).get(parentKey);
    return children == null ? Set.of() : new LinkedHashSet<>(children);
  }

  /**
   * Find the rows where every given column equals the given value. Values are in stored form; a
   * {@code null} value matches nothing, as in SQL.
   *
   * @param equalities column name to required value; empty matches every row
   * @return keys of the matching rows, in insertion order
   */
  public List<String> findKeys(final Map<String, String> equalities) {
    for (var col : equalities.keySet()) {
      if (!hasColumn(col) || col.equals("primary_key")) {
        throw new RuntimeException("Column " + col + " does not exist in table " + name);
      }
    }
    if (equalities.containsValue(null)) {
      return List.of();
    }

    // the whole primary key is given, so at most one row can match
    List<String> pkColumns = getPrimaryKeyColumns();
    if (equalities.keySet().containsAll(pkColumns)) {
      List<String> parts = new ArrayList<>(pkColumns.size());
      for (var col : pkColumns) {
        parts.add(equalities.get(col));
      }
      List<String> row = data.get(joinKey(parts));
      return row != null && matches(row, equalities) ? List.of(row.get(0)) : List.of();
    }

    List<String> keys = new ArrayList<>();
    for (var row : data.values()) {
      if (matches(row, equalities)) {
        keys.add(row.get(0));
      }
    }
    return keys;
  }

  private boolean matches(final List<String> row, final Map<String, String> equalities) {
    for (var entry : equalities.entrySet()) {
      if (!entry.getValue().equals(row.get(columnLocationMap.get(entry.getKey())))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Delete a row.
   *
   * @param pk internal key of the row
   * @throws RuntimeException if the row does not exist
   */
  public void delete(final String pk) {
    List<String> row = data.remove(pk);
    if (row == null) {
      throw new RuntimeException("No row " + displayKey(pk) + " in table " + name);
    }
    unindexRow(pk, row);
  }

  /**
   * Change some non key columns of a row. Does not check foreign keys, the caller must.
   *
   * @param pk internal key of the row
   * @param values column name to new stored value, {@code null} for SQL NULL
   * @throws RuntimeException if the row does not exist, a column is part of the primary key, or a
   *     NOT NULL column would become NULL
   */
  public void setValues(final String pk, final Map<String, String> values) {
    List<String> row = data.get(pk);
    if (row == null) {
      throw new RuntimeException("No row " + displayKey(pk) + " in table " + name);
    }
    for (var entry : values.entrySet()) {
      Column col = getColumn(entry.getKey());
      if (col.primary()) {
        throw new RuntimeException("Primary key column " + col.name() + " cannot be changed");
      }
      if (entry.getValue() == null && !col.nullable()) {
        throw new RuntimeException("Column " + col.name() + " cannot be NULL");
      }
    }
    unindexRow(pk, row);
    for (var entry : values.entrySet()) {
      row.set(columnLocationMap.get(entry.getKey()), entry.getValue());
    }
    indexRow(pk, row);
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
