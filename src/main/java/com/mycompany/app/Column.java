package com.mycompany.app;

import java.io.Serializable;

/**
 * Column of database definition
 */
public record Column(String name, String data, boolean nullable, boolean primary, String dependsOn)
        implements Serializable {
    /**
     * Constructor for a column
     *
     * @param name
     *            name of the attribute
     * @param data
     *            internal data stored for this attribute
     * @param nullable
     *            isNull?
     * @param primary
     *            is the attribute part of the priamry key for the row it's in
     * @param dependsOn
     *            is the attribute a foreign key to another column
     */
    public Column {
    }

    /**
     * Get if a column is foreign key
     *
     * @return Boolean result of this
     */
    @Override
    public String dependsOn() {
        return dependsOn;
    }

    /**
     * Is the column a primary key component
     *
     * @return Boolean result
     */
    @Override
    public boolean primary() {
        return primary;
    }

    /**
     * Is the attribute able to be null
     *
     * @return Boolean of this
     */
    @Override
    public boolean nullable() {
        return nullable;
    }

    /**
     * Return the internal data of this attribute
     *
     * @return Data
     */
    @Override
    public String data() {
        return data;
    }

    /**
     * The name of the attribute
     *
     * @return String of name of attributes
     */
    @Override
    public String name() {
        return name;
    }
}
