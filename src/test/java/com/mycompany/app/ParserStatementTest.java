package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ParserStatementTest {

  private Parser parser(String sql) {
    return new Parser(new Scanner(sql).scanTokens());
  }

  @Test
  public void testParseCreateStatement() {
    var statements = parser("create table users (id number primary key);").parse();
    assertEquals(statements.size(), 1);
    Expr.Create create = assertInstanceOf(Expr.Create.class, statements.get(0));
    assertEquals(create.tableName, "users");
  }

  @Test
  public void testParseSelectStatement() {
    var statements = parser("select name from users").parse();
    Expr.Select select = assertInstanceOf(Expr.Select.class, statements.get(0));
    assertEquals(select.table.lexeme, "users");
  }

  @Test
  public void testParseInsertStatement() {
    var statements = parser("insert into users (name) values (liam)").parse();
    Expr.Insert insert = assertInstanceOf(Expr.Insert.class, statements.get(0));
    assertEquals(insert.table.lexeme, "users");
  }

  @Test
  public void testParseUnsupportedStatement() {
    var statements = parser("drop table users;").parse();
    assertEquals(statements.size(), 1);
    assertNull(statements.get(0));
  }

  @Test
  public void testCreateDatabase() {
    Expr.EngineExpr expr = parser("create database shop;").parseEngineExpr();
    assertEquals(expr.database, "shop");
    assertTrue(expr.create);
    assertFalse(expr.drop);
  }

  @Test
  public void testDropDatabase() {
    Expr.EngineExpr expr = parser("drop database shop;").parseEngineExpr();
    assertEquals(expr.database, "shop");
    assertTrue(expr.drop);
    assertFalse(expr.create);
  }

  @Test
  public void testEnterDatabase() {
    Expr.EngineExpr expr = parser("shop").parseEngineExpr();
    assertEquals(expr.database, "shop");
    assertFalse(expr.drop);
    assertFalse(expr.create);
  }

  @Test
  public void testEngineExprToString() {
    Expr.EngineExpr expr = parser("create database shop;").parseEngineExpr();
    assertEquals(expr.toString(), "EngineExpr{database='shop', drop=false, create=true}");
  }

  @Test
  public void testStandardExpr() {
    Expr expr = parser("1 + 2").standardExpr();
    Expr.Binary binary = assertInstanceOf(Expr.Binary.class, expr);
    assertEquals(binary.operator.type, TokenType.PLUS);
  }

  @Test
  public void testSubtraction() {
    Expr.Binary binary = assertInstanceOf(Expr.Binary.class, parser("5 - 2").expression());
    assertEquals(binary.operator.type, TokenType.MINUS);
  }

  @Test
  public void testAliasWithoutAliasName() {
    // 'col as' with nothing after falls back to the column name
    Expr.Alias alias = (Expr.Alias) parser("col as").alias();
    assertEquals(alias.colExpr.toString(), "Literal(col)");
    assertEquals(alias.alias.toString(), "Literal(col)");
  }

  @Test
  public void testAliasFollowedByOtherToken() {
    Expr.Alias alias = (Expr.Alias) parser("col , other").alias();
    assertEquals(alias.alias.toString(), "Literal(col)");
  }

  @Test
  public void testSelectStar() {
    Expr.Select select = (Expr.Select) parser("select * from users").selectStatement();
    assertEquals(select.variables.size(), 1);
    assertTrue(select.whereClause.isEmpty());
  }

  @Test
  public void testSelectWithoutVariables() {
    Expr.Select select = (Expr.Select) parser("select from users").selectStatement();
    assertTrue(select.variables.isEmpty());
    assertEquals(select.table.lexeme, "users");
  }

  @Test
  public void testColumnWithAllConstraints() {
    Column col = parser("owner string not null references users(id)").column();
    assertEquals(col.name(), "owner");
    assertEquals(col.type(), TokenType.STRING_TYPE);
    assertEquals(col.dependsOn(), "users.id");
    assertFalse(col.nullable());
    assertFalse(col.primary());
  }

  @Test
  public void testColumnAtEndOfInput() {
    Column col = parser("id number primary key").column();
    assertEquals(col.type(), TokenType.NUMBER);
    assertTrue(col.primary());
  }
}
