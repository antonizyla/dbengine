package com.mycompany.app;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CreationTest {

  @Test
  public void testColumnNullity() {

    Scanner t = new Scanner("OrderID number not null,");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    Column column = p.column();
    assert column.name().equals("OrderID");
    assert column.type() == TokenType.NUMBER;
    assert column.nullable() == false;
    assert column.primary() == false;
  }

  @Test
  public void testColumnPrimaryKeys() {

    Scanner t = new Scanner("columnName String Primary Key,");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    Column column = p.column();
    assert column.name().equals("columnName");
    assert column.type() == TokenType.STRING_TYPE;
    assert column.nullable() == false;
    assert column.primary() == true;
  }

  @Test
  public void testColumnForeignReferences() {
    Scanner t = new Scanner("columnName String references tableName(attributeName),");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    Column column = p.column();
    assert column.name().equals("columnName");
    assert column.type() == TokenType.STRING_TYPE;
    assert column.nullable() == false;
    assert column.primary() == false;
    assert column.dependsOn().equals("tableName.attributeName");
  }

  @Test
  public void testColumnForeignMalformedPrimary() {
    Scanner t = new Scanner("columnName String Primary references tableName(attributeName),");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    try {
      @SuppressWarnings("unused")
      Column column = p.column();
    } catch (RuntimeException e) {
      assert true; // assert true because there is no `key` after `Primary`
      return;
    }
    assert false;
  }

  @Test
  public void testColumnForeignPrimary() {
    Scanner t = new Scanner("columnName Primary key String references tableName(attributeName),");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    Column column = p.column();
    assert column.name().equals("columnName");
    assert column.type() == TokenType.STRING_TYPE;
    assert column.nullable() == false;
    assert column.primary() == true;
    assert column.dependsOn().equals("tableName.attributeName");
  }
}
