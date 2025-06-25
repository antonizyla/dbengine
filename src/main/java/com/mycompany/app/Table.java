package com.mycompany.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Table {

    private ArrayList<Column> columns;
    private HashMap<String, Integer> columnLocationMap;
    private String name;

    private ArrayList<Integer> pk_indexes;

    private HashMap<String, List<String>> data; // hold the primary key and the row

    public Table(String name, List<Column> columns) {
        this.columns = new ArrayList<>(columns.size());
        for (var col : columns) {
            this.columns.add(col);
        }

        this.name = name;

        this.columnLocationMap = new HashMap<>(); // keep a map of the location each attr in each row

        this.pk_indexes = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            columnLocationMap.put(columns.get(i).getName(), i + 1);
            if (columns.get(i).isPrimary()) {
                pk_indexes.add(i);
            }
        }

        // the internal primary key will be copied into row 0 regardless, also works
        // with composite keys
        columnLocationMap.put("primary_key", 0);

        data = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    private List<String> getColumnNames() {
        List<String> columnNames = new ArrayList<>(this.columns.size());
        for (int i = 0; i < this.columns.size(); i++) {
            columnNames.add(this.columns.get(i).getName());
        }
        return columnNames;
    }

    public List<List<String>> select(List<String> columns, Integer limit) {
        ArrayList<String> cols = new ArrayList<>(columns.size());
        for (var col : columns) {
            cols.add(col);
        }
        if (cols.contains("*")) {
            for (var col : getColumnNames()) {
                cols.add(col);
            }
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

    public String getRowCol(String rowPkey, String columnName) {
        int index = columnLocationMap.get(columnName);
        return data.get(rowPkey).get(index);
    }

    public void printData() {
        for (var key : data.keySet()) {
            printRow(key);
        }
    }

    public void printRow(String p_key) {
        for (var attr : data.get(p_key)) {
            System.out.printf("%s, ", attr);
        }
        System.out.println("");
    }

    public void printHeader() {
        for (var col : columns) {
            if (col.isPrimary()) {
                System.out.printf("*");
                System.out.printf("%s, ", col.getName());
            } else {
                System.out.printf("%s, ", col.getName());
            }
        }
        System.out.println("");
    }

    private String getPK(List<String> row) {
        String pk = "";
        for (var index : pk_indexes) {
            pk = String.join(pk, row.get(index));
        }
        return pk;
    }

    public void insert(List<String> r) {
        List<String> row = new ArrayList<>(r.size() + 1);
        row.add(getPK(r));
        for (String elem : r) {
            row.add(elem);
        }
        data.put(row.get(0), row);
    }

}
