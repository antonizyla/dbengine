package com.mycompany.app;

import java.io.Serializable;

/**
 * Column of database definition.
 *
 * @param name Name of the attribute
 * @param type Data for particular isntance
 * @param nullable is Not Null
 * @param primary is part of priamry key
 * @param dependsOn is it a foreign key, as {@code table.column}
 * @param onDelete action of a column level foreign key when the referenced row is deleted
 * @param defaultValue stored value used when an insert gives no value, or {@code null}
 */
public record Column(
    String name,
    TokenType type,
    boolean nullable,
    boolean primary,
    String dependsOn,
    ReferentialAction onDelete,
    String defaultValue)
    implements Serializable {

  /**
   * Column without a delete action or default value.
   *
   * @param name Name of the attribute
   * @param type Data for particular isntance
   * @param nullable is Not Null
   * @param primary is part of priamry key
   * @param dependsOn is it a foreign key, as {@code table.column}
   */
  public Column(String name, TokenType type, boolean nullable, boolean primary, String dependsOn) {
    this(name, type, nullable, primary, dependsOn, null, null);
  }

  /**
   * Copy of this column with different key and nullability flags.
   *
   * @param isPrimary whether the copy is part of the primary key
   * @param isNullable whether the copy may hold NULL
   * @return the new column definition
   */
  public Column withKeyFlags(boolean isPrimary, boolean isNullable) {
    return new Column(name, type, isNullable, isPrimary, dependsOn, onDelete, defaultValue);
  }

  /**
   * Check a literal value against the column type and convert it into its stored form.
   *
   * @param value the non-null literal value, either a {@link Double} or a {@link String}
   * @return the value as stored in the table
   * @throws RuntimeException if the value does not match the column type
   */
  public String toStoredValue(Object value) {
    if (type == TokenType.NUMBER) {
      if (!(value instanceof Double)) {
        throw new RuntimeException(
            "Type mismatch: column " + name + " expects a NUMBER but got '" + value + "'");
      }
      double number = (Double) value;
      if (number == Math.rint(number) && !Double.isInfinite(number)) {
        return Long.toString((long) number);
      }
      return Double.toString(number);
    }
    if (type == TokenType.STRING_TYPE && !(value instanceof String)) {
      throw new RuntimeException(
          "Type mismatch: column " + name + " expects a STRING but got " + value);
    }
    return value.toString();
  }

  /**
   * Get if a column is foreign key.
   *
   * @return Boolean result of this
   */
  @Override
  public String dependsOn() {
    return dependsOn;
  }

  /**
   * Is the column a primary key component.
   *
   * @return Boolean result
   */
  @Override
  public boolean primary() {
    return primary;
  }

  /**
   * Is the attribute able to be null.
   *
   * @return Boolean of this
   */
  @Override
  public boolean nullable() {
    return nullable;
  }

  /**
   * Return the internal data of this attribute.
   *
   * @return Data
   */
  @Override
  public TokenType type() {
    return type;
  }

  /**
   * The name of the attribute.
   *
   * @return String of name of attributes
   */
  @Override
  public String name() {
    return name;
  }
}
