package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A representation of a Database table to interact with it using java.
 */

public class Table implements Serializable{

    private final ArrayList<Column> columns; // the 'schema' of the table
    private final HashMap<String, Integer> columnLocationMap; // where in the row a specific column is
    private final String name; // table name

    private final ArrayList<Integer> pk_indexes;

    private final HashMap<String, List<String>> data; // hold the primary key and the row

    /**
     * Creation of a Database Table
     *
     * @param name
     *            - Name of the table
     * @param columns
     *            - List of Columns to defin the schema
     */

    public Table(String name, List<Column> columns) {
        this.columns = new ArrayList<>(columns.size());
        for (var col : columns) {
            this.columns.add(col);
        }

        this.name = name;

        this.columnLocationMap = new HashMap<>(); // keep a map of the location each attr in each row

        this.pk_indexes = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            columnLocationMap.put(columns.get(i).name(), i + 1);
            if (columns.get(i).primary()) {
                pk_indexes.add(i);
            }
        }

        // the internal primary key will be copied into row 0 regardless, also works
        // with composite keys
        columnLocationMap.put("primary_key", 0);

        data = new HashMap<>();
    }

    /**
     * Name of table
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
     * Select Data from table
     *
     * @param columns
     *            which columns you want to get
     * @param limit
     *            maximum number of rows
     *
     * @return the data as list of list of strings
     */
    public List<List<String>> select(List<String> columns, Integer limit) {
        ArrayList<String> cols = new ArrayList<>(columns.size());
        cols.addAll(columns);
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
     * Get the value of a column in a specific row
     *
     * @param rowPkey
     *            the primary key of the row
     * @param columnName
     *            the column name as a string
     *
     * @return the value of the attribute
     */
    public String getRowCol(String rowPkey, String columnName) {
        int index = columnLocationMap.get(columnName);
        return data.get(rowPkey).get(index);
    }

    /**
     * Print the entire table's data
     */
    public void printData() {
        for (var key : data.keySet()) {
            printRow(key);
        }
    }

    /**
     * Print the values in a row based on primary key
     *
     * @param p_key
     *            the primary key of the row
     */
    public void printRow(String p_key) {
        for (var attr : data.get(p_key)) {
            System.out.printf("%s, ", attr);
        }
        System.out.println();
    }

    /**
     * Print the column names and if they are the primary keys of the tables
     */
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

    private String getPK(List<String> row) {
        String pk = "";
        for (var index : pk_indexes) {
            pk = String.join(pk, row.get(index));
        }
        return pk;
    }

    /**
     * Insert a row into the table
     *
     * @param r
     *            the row to insert as list of string
     */
    public void insert(List<String> r) {
        List<String> row = new ArrayList<>(r.size() + 1);
        row.add(getPK(r));
        row.addAll(r);
        data.put(row.get(0), row); //this cannot be row.getfirst()
    }

}
