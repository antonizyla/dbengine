package com.mycompany.app;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class Database implements Serializable {

    private HashMap<String, Table> tables;
    private String name;
    private boolean autoCommit;
    private String filepath;

    public Database(String name, String filepath) {
        // read the database into memory
        this.name = name;
        this.filepath = filepath;
        this.tables = new HashMap<>();
    }

    public Table getTable(String name){
        return tables.get(name);
    }

    public String getFilePath(){
        return filepath;
    }

    public String getName() {
        return this.name;
    }

    public void createTable(List<Column> definitions, String name) {
        tables.put(name, new Table(name, definitions));
    }

    public void alterTable(String tableName, List<Column> definitions) {

    }

    public void commit() {

    }

    public void rollback() {

    }

    public void startTransaction() {

    }

    public void printAbout() {
        System.out.printf("[Database %s] \n");
    }

}
