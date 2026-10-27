package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ParserErrorTest {

  private Parser parser(String sql) {
    return new Parser(new Scanner(sql).scanTokens());
  }

  private void assertCreateFails(String sql, String message) {
    var e = assertThrows(RuntimeException.class, () -> parser(sql).createTable());
    assertEquals(message, e.getMessage());
  }

  private void assertSelectFails(String sql, String message) {
    var e = assertThrows(RuntimeException.class, () -> parser(sql).selectStatement());
    assertEquals(message, e.getMessage());
  }

  private void assertInsertFails(String sql, String message) {
    var e = assertThrows(RuntimeException.class, () -> parser(sql).insertStatement());
    assertEquals(message, e.getMessage());
  }

  private void assertEngineFails(String sql, String message) {
    var e = assertThrows(RuntimeException.class, () -> parser(sql).parseEngineExpr());
    assertEquals(message, e.getMessage());
  }

  // engine expressions

  @Test
  public void testEngineExprUnknownKeyword() {
    assertEngineFails("select", "Expected CREATE or DROP keyword for engine expression");
  }

  @Test
  public void testCreateDatabaseErrors() {
    assertEngineFails("create shop;", "Expected DATABASE keyword after CREATE");
    assertEngineFails("create database ;", "Expected database name after CREATE DATABASE");
    assertEngineFails("create database shop", "Expected ';' after database name");
  }

  @Test
  public void testDropDatabaseErrors() {
    assertEngineFails("drop shop;", "Expected DATABASE keyword after DROP");
    assertEngineFails("drop database ;", "Expected database name after DROP DATABASE");
    assertEngineFails("drop database shop", "Expected ';' after database name");
  }

  // expressions

  @Test
  public void testAliasRequiresIdentifier() {
    var e = assertThrows(RuntimeException.class, () -> parser("5").alias());
    assertEquals("Expected identifier after SELECT", e.getMessage());
  }

  @Test
  public void testGroupingMissingClosingParen() {
    var e = assertThrows(RuntimeException.class, () -> parser("(1 + 2").expression());
    assertEquals("Expected ')' after expression", e.getMessage());
  }

  @Test
  public void testMissingExpression() {
    var e = assertThrows(RuntimeException.class, () -> parser("+").expression());
    assertEquals("Expected expression", e.getMessage());
  }

  // create table

  @Test
  public void testCreateTableErrors() {
    assertCreateFails("create users", "Expected TABLE keyword after CREATE");
    assertCreateFails("create table (", "Expected table name after CREATE");
    assertCreateFails("create table users id number);", "Expected '(' after table name");
    assertCreateFails("create table users (id number", "Expected ')' after columns");
    assertCreateFails("create table users (id number)", "Expected ';' after table definition");
  }

  @Test
  public void testColumnRequiresName() {
    var e = assertThrows(RuntimeException.class, () -> parser("string").column());
    assertEquals("Expected column name", e.getMessage());
  }

  @Test
  public void testColumnForeignKeyErrors() {
    assertCreateFails(
        "create table t (a string references);", "Expected foreign table name after REFERENCES");
    assertCreateFails(
        "create table t (a string references users);",
        "Expected '(' after foreign table name. e.g. table(column)");
    assertCreateFails(
        "create table t (a string references users());",
        "Expected foreign column name after '(', e.g. table(column)");
    assertCreateFails(
        "create table t (a string references users(id;",
        "Expected ')' after foreign column name e.g. table(column)");
  }

  @Test
  public void testColumnConstraintErrors() {
    assertCreateFails("create table t (a primary string);", "Expected KEY after PRIMARY");
    assertCreateFails(
        "create table t (a string not 5);", "Unexpected token after NOT, expected NULL");
    assertCreateFails("create table t (a not null);", "Expected column type after column name");
  }

  @Test
  @Timeout(1)
  public void testColumnUnexpectedTokenDoesNotHang() {
    assertCreateFails(
        "create table t (a string foo);", "Unexpected token 'foo' in column definition");
  }

  // select

  @Test
  public void testSelectErrors() {
    assertSelectFails("from users", "Expected SELECT keyword");
    assertSelectFails("select name users", "Expected FROM after SELECT variables");
    assertSelectFails("select name from ;", "Expected table name after SELECT");
    assertSelectFails(
        "select name from users where (id = 1", "Expected ')' after logical expression");
  }

  // insert

  @Test
  public void testInsertErrors() {
    assertInsertFails("select", "Requires Insert Statement");
    assertInsertFails("insert users", "Requires INTO after Insert");
    assertInsertFails("insert into ;", "Requires table to be specified");
    assertInsertFails(
        "insert into users name values (liam)",
        "Missing opening bracket, Columns to be inserted need to be of form `(col, col2, col3)`"
            + " or `*`");
    assertInsertFails(
        "insert into users (name values (liam)",
        "Missing Closing Bracket, Columns to be inserted need to be of form `(col, col2,"
            + " col3...)` or `*`");
    assertInsertFails("insert into users (name) (liam)", "Missing Values");
    assertInsertFails("insert into users (name) values liam", "Missing opening bracket");
    assertInsertFails(
        "insert into users (name) values (liam",
        "[Parser] Insert Parsing: Missing Closing Bracket");
  }
}
