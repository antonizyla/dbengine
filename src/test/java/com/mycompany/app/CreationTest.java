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

  @Test
  public void testColumnListSingular() {
    Scanner t = new Scanner("columnName String references tableName(attributeName)");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    var cols = p.columnList();
    var col = cols.get(0);
    assert cols.size() == 1;
    assert col.name().equals("columnName");
    assert col.type() == TokenType.STRING_TYPE;
    assert col.nullable() == false;
    assert col.primary() == false;
    assert col.dependsOn().equals("tableName.attributeName");
  }

  @Test
  public void testColumnListMultiple() {
    Scanner t =
        new Scanner(
            "columnName String references tableName(attributeName), columnName2 String references"
                + " tableName2(attributeName2)");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    var cols = p.columnList();
    assert cols.size() == 2;
    var col1 = cols.get(0);
    assert col1.name().equals("columnName");
    assert col1.type() == TokenType.STRING_TYPE;
    assert col1.nullable() == false;
    assert col1.primary() == false;
    assert col1.dependsOn().equals("tableName.attributeName");
    var col2 = cols.get(1);
    assert col2.name().equals("columnName2");
    assert col2.type() == TokenType.STRING_TYPE;
    assert col2.nullable() == false;
    assert col2.primary() == false;
    assert col2.dependsOn().equals("tableName2.attributeName2");
  }

  @Test
  public void testColumnListMultipleTrailingComma() {
    Scanner t =
        new Scanner(
            "columnName String references tableName(attributeName), columnName2 String references"
                + " tableName2(attributeName2),");
    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);

    try {
      p.columnList();
    } catch (RuntimeException e) {
      assert e.getMessage().equals("Expected column name");
      return;
    }
    assert false;
  }

  @Test
  public void testCreateTable() {
    Scanner t =
        new Scanner(
            "create table tableName ( "
                + "columnName String primary key, "
                + "columnName2 number not null "
                + ") ;");

    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);
    Expr.Create create = (Expr.Create) p.createTable();
    assert create.tableName.equals("tableName");
    assert create.columns.size() == 2;
    var col1 = create.columns.get(0);
    assert col1.name().equals("columnName");
    assert col1.type() == TokenType.STRING_TYPE;
    assert col1.nullable() == false;
    assert col1.primary() == true;
    var col2 = create.columns.get(1);

    assert col2.name().equals("columnName2");
    assert col2.type() == TokenType.NUMBER;
    assert col2.nullable() == false;
    assert col2.primary() == false;
  }

  @Test
  public void testCreateTableWithForeignKey() {
    Scanner t =
        new Scanner(
            "create table tableName ( "
                + "columnName String primary key, "
                + "columnName2 number not null references tableName2(columnName3) "
                + ") ;");

    List<Token> tokens = t.scanTokens();
    Parser p = new Parser(tokens);
    Expr.Create create = (Expr.Create) p.createTable();
    assert create.tableName.equals("tableName");
    assert create.columns.size() == 2;
    var col1 = create.columns.get(0);
    assert col1.name().equals("columnName");
    assert col1.type() == TokenType.STRING_TYPE;
    assert col1.nullable() == false;
    assert col1.primary() == true;
    var col2 = create.columns.get(1);

    assert col2.name().equals("columnName2");
    assert col2.type() == TokenType.NUMBER;
    assert col2.nullable() == false;
    assert col2.primary() == false;
    assert col2.dependsOn().equals("tableName2.columnName3");
  }

  @Test
  public void testCreationViaDbInterface() {
    Database db = new Database("db", "testCreationViaDbInterface.db");
    String createTableQuery =
        "create table employees( "
            + "employeeID Number primary key, "
            + "employeeName String not null "
            + ") ;";
    db.runQuery(createTableQuery, false);

    Table table = db.getTable("employees");

    // check insertion and retrival of data via db interface
    table.insert(List.of("23", "Alice"));

    List<List<String>> res = table.select(List.of("primary_key", "employeeName"), 10);
    assert res.size() == 1;
    assert res.get(0).get(0).equals("23");
    assert res.get(0).get(1).equals("Alice");
  }
}
