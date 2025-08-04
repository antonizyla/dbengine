package com.mycompany.app;

import org.junit.jupiter.api.Test;

public class ParserTest extends Parser {

  @Test
  public void testAlias() {
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
  public void testAliasSingleVariable() {
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
  public void testSimpleAddition() {
    Scanner s = new Scanner("2 + 3");
    Parser p = new Parser(s.scanTokens());
    Expr expr = p.expression();
    assert expr instanceof Expr.Binary;
  }

  @Test
  public void testPrecedenceMultiplicationBeforeAddition() {
    Scanner s = new Scanner("2 + 3 * 4");
    Parser p = new Parser(s.scanTokens());
    Expr expr = p.expression();
    assert expr instanceof Expr.Binary;
    Expr.Binary binary = (Expr.Binary) expr;
    assert binary.operator.type == TokenType.PLUS;
    assert binary.right instanceof Expr.Binary;
  }

  @Test
  public void testParenthesesChangePrecedence() {
    Scanner s = new Scanner("(2 + 3) * 4");
    Parser p = new Parser(s.scanTokens());
    Expr expr = p.expression();
    assert expr instanceof Expr.Binary;
    Expr.Binary binary = (Expr.Binary) expr;
    assert binary.operator.type == TokenType.STAR;
    assert binary.left instanceof Expr.Grouping;
  }

  @Test
  public void testUnaryMinus() {
    Scanner s = new Scanner("-5 + 3");
    Parser p = new Parser(s.scanTokens());
    Expr expr = p.expression();
    assert expr instanceof Expr.Binary;
    Expr.Binary binary = (Expr.Binary) expr;
    assert binary.left instanceof Expr.Unary;
    assert binary.operator.type == TokenType.PLUS;
    Expr.Unary unary = (Expr.Unary) binary.left;
    assert unary.operator.type == TokenType.MINUS;
  }

  @Test
  public void testComplexExpression() {
    Scanner s = new Scanner("(col1 + 10) / 60");
    Parser p = new Parser(s.scanTokens());
    Expr expr = p.expression();
    assert expr instanceof Expr.Binary;
    Expr.Binary binary = (Expr.Binary) expr;
    assert binary.operator.type == TokenType.SLASH;
    assert binary.left instanceof Expr.Grouping;
  }

  @Test
  public void testSelectStatement() {
    Scanner s = new Scanner("Select column1 , column2 from table1;");
    var tokens = s.scanTokens();
    Parser p = new Parser(tokens);
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.variables.size() == 2;

    assert select.variables.get(0).toString().equals("Literal(column1)");
    assert select.variables.get(1).toString().equals("Literal(column2)");

    assert select.table.lexeme.toString().equals("table1");
  }

  @Test
  public void testSelectWithSimpleWhere() {
    Scanner s = new Scanner("Select column1 from table1 where id = 5");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Binary;
  }

  @Test
  public void testSelectWithAndCondition() {
    Scanner s = new Scanner("Select column1 from table1 where id = 5 and name = john");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical logical = (Expr.Logical) select.whereClause.get(0);
    assert logical.operator.type == TokenType.AND;
  }

  @Test
  public void testSelectWithOrCondition() {
    Scanner s = new Scanner("Select column1 from table1 where id = 5 or id = 10");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical logical = (Expr.Logical) select.whereClause.get(0);
    assert logical.operator.type == TokenType.OR;
  }

  @Test
  public void testSelectWithComparison() {
    Scanner s = new Scanner("Select column1 from table1 where age > 18");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Binary;
    Expr.Binary binary = (Expr.Binary) select.whereClause.get(0);
    assert binary.operator.type == TokenType.GREATER_THAN;
  }

  @Test
  public void testSelectWithComplexWhere() {
    Scanner s =
        new Scanner("Select column1 from table1 where age > 18 and name = john or status = active");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical topLevel = (Expr.Logical) select.whereClause.get(0);
    assert topLevel.operator.type == TokenType.OR;
  }

  @Test
  public void testSelectWithNestedParentheses() {
    Scanner s =
        new Scanner(
            "Select column1 from table1 where (age > 18 and name = john) or (status = active and"
                + " score >= 75)");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical topLevel = (Expr.Logical) select.whereClause.get(0);
    assert topLevel.operator.type == TokenType.OR;
    assert topLevel.left instanceof Expr.Grouping;
    assert topLevel.right instanceof Expr.Grouping;
  }

  @Test
  public void testSelectWithDeeplyNestedConditions() {
    Scanner s =
        new Scanner(
            "Select column1 from table1 where ((age > 18 and name = john) or status = active) and"
                + " department = sales");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical topLevel = (Expr.Logical) select.whereClause.get(0);
    assert topLevel.operator.type == TokenType.AND;
    assert topLevel.left instanceof Expr.Grouping;
  }

  @Test
  public void testSelectWithArithmeticInWhere() {
    Scanner s =
        new Scanner("Select column1 from table1 where age + 5 > 25 and salary * 12 >= 50000");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical logical = (Expr.Logical) select.whereClause.get(0);
    assert logical.operator.type == TokenType.AND;
    assert logical.left instanceof Expr.Binary;
    assert logical.right instanceof Expr.Binary;
  }

  @Test
  public void testSelectWithComplexArithmeticConditions() {
    Scanner s =
        new Scanner(
            "Select column1 from table1 where (price * quantity - discount > 100) or total / count"
                + " <= average;");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical logical = (Expr.Logical) select.whereClause.get(0);
    assert logical.operator.type == TokenType.OR;
  }

  @Test
  public void testSelectWithUnaryExpressionInWhere() {
    Scanner s = new Scanner("Select column1 from table1 where -balance > 0 and status = active");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical logical = (Expr.Logical) select.whereClause.get(0);
    assert logical.left instanceof Expr.Binary;
    Expr.Binary leftBinary = (Expr.Binary) logical.left;
    assert leftBinary.left instanceof Expr.Unary;
  }

  @Test
  public void testSelectWithTripleNestedConditions() {
    Scanner s =
        new Scanner(
            "Select column1 from table1 where (((age > 18 or age < 65) and status = active) or"
                + " department = hr) and salary > 30000");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;
    assert select.whereClause.get(0) instanceof Expr.Logical;
    Expr.Logical topLevel = (Expr.Logical) select.whereClause.get(0);
    assert topLevel.operator.type == TokenType.AND;
    assert topLevel.left instanceof Expr.Grouping;
  }

  @Test
  public void testSelectWithMixedComparisonOperators() {
    Scanner s =
        new Scanner(
            "Select column1 from table1 where id >= 10 and id <= 100 and score > 80 and rating <"
                + " 5");
    Parser p = new Parser(s.scanTokens());
    Expr.Select select = (Expr.Select) p.selectStatement();

    assert select.whereClause != null;
    assert select.whereClause.size() == 1;

    // Should be a chain of AND operations with different comparison operators
    Expr current = select.whereClause.get(0);
    while (current instanceof Expr.Logical) {
      Expr.Logical logical = (Expr.Logical) current;
      assert logical.operator.type == TokenType.AND;
      current = logical.left;
    }
  }

  @Test
  public void testInsertParsing() {
    Scanner s = new Scanner("Insert into table1 (column1, column2) values (value1, value2);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    assert expr.table.literal.equals("table1");
    assert expr.columns.size() == 2;
    assert expr.values.size() == 2;
  }

  @Test
  public void testInsertParsingWildcardCols() {
    Scanner s = new Scanner("Insert into table1 * values (value1, value2);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    assert expr.table.literal.equals("table1");
    assert expr.columns.size() == 1;
    assert expr.values.size() == 2;
  }
}
