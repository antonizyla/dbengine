package com.mycompany.app;

public class Column {
    private final String name;
    private final String data;
    private final boolean nullable;
    private final boolean primary;
    private final String dependsOn;

    public Column(String name, String data, boolean nullable, boolean primary, String dependsOn) {
        this.name = name;
        this.data = data;
        this.nullable = nullable;
        this.primary = primary;
        this.dependsOn = dependsOn;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getData() {
        return data;
    }

    public String getName() {
        return name;
    }
}
