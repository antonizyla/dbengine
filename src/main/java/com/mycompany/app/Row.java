package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

public class Row {

    private String primaryKey;
    private final ArrayList<String> data;

    // constructor for when definition is a set of cols with no value as a template
    public Row(List<Column> definition, List<String> values) {
        while (values.size() < definition.size()) {
            values.add("");
        }
        data = new ArrayList<>(definition.size() + 1);
        for (int i = 0; i < definition.size(); i++) {
            if (definition.get(i).isPrimary()) {
                primaryKey = String.format("%s%s", primaryKey, definition.get(i).getName());
            }
            data.add(values.get(i + 1));
        }
    }

    public String getPK() {
        return primaryKey;
    }

    // constructor for when column objects have a value already
    public Row(List<Column> columns) {
        data = new ArrayList<>(columns.size() + 1);
    }

    public String getPrimaryKey() {
        return data.getFirst();
    }

    public List<String> getData() {
        return data.subList(1, data.size() - 1);
    }

}
