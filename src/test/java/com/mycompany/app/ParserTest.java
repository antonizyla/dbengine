package com.mycompany.app;

import org.junit.jupiter.api.Test;

public class ParserTest extends Parser {

  @Test
  public void testAliase() {
    // for parsing 'col as alias'
    Scanner s = new Scanner("col as alias");
    var tokens = s.scanTokens();

    Parser p = new Parser(tokens);
    Expr.Alias e = (Expr.Alias) p.alias();

    Expr.Alias expected = new Expr.Alias(new Expr.Literal("col"), new Expr.Literal("alias"));

    assert e.colExpr.toString().equals(expected.colExpr.toString());
    assert e.alias.toString().equals(expected.alias.toString());
  }

  @Test
  public void testAliaseSingleVariable() {
    // for parsing 'col'
    Scanner s = new Scanner("col");
    var tokens = s.scanTokens();

    Parser p = new Parser(tokens);
    Expr.Alias e = (Expr.Alias) p.alias();

    Expr.Alias expected = new Expr.Alias(new Expr.Literal("col"), new Expr.Literal("col"));

    assert e.colExpr.toString().equals(expected.colExpr.toString());
    assert e.alias.toString().equals(expected.alias.toString());
  }

  @Test
  public void testAliasWithExpr() {
    // for parsing 'col + 1 as alias' or any other expression
    Scanner s = new Scanner("col + 1 as alias");
    var tokens = s.scanTokens();

    Parser p = new Parser(tokens);
    Expr.Alias e = (Expr.Alias) p.alias();

    Expr.Alias expected =
        new Expr.Alias(
            new Expr.Binary(
                new Expr.Literal("Column"),
                new Token(TokenType.ADD, "null", "null", 0),
                new Expr.Literal(1)),
            new Expr.Literal("alias"));

    assert e.colExpr.toString().equals(expected.colExpr.toString());
    assert e.alias.toString().equals(expected.alias.toString());
  }

  @Test
  public void testGeneralExpressions() {
    // parse `(col1 + 10)/60` into an expression
    return;
  }

  @Test
  public void testSelectStatement() {
    Scanner s = new Scanner("Select column1 , column2 from table1;");
    var tokens = s.scanTokens();
    Parser p = new Parser(tokens);
    Expr.Select select = (Expr.Select) p.selectStatement();

    System.out.println("___________________________________");
    System.out.println(select.toString());
    assert select.variables.size() == 2;

    assert select.variables.get(0).toString().equals("Literal(column1)");
    assert select.variables.get(1).toString().equals("Literal(column2)");

    assert select.table.lexeme.toString().equals("table1");
  }
}
