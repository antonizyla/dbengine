package com.mycompany.app;

import org.junit.jupiter.api.Test;

public class ParserInsertTest extends Parser {

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

  @Test
  public void testInsertSingleColumn() {
    Scanner s = new Scanner("INSERT INTO users (name) VALUES (john);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("users");
    assert expr.columns.size() == 1;
    assert expr.values.size() == 1;
    assert ((Expr.Literal) expr.columns.get(0)).value.equals("name");
    assert ((Expr.Literal) expr.values.get(0)).value.equals("john");
  }

  @Test
  public void testInsertMultipleColumns() {
    Scanner s = new Scanner("INSERT INTO products (name, price, category) VALUES (laptop, price999, electronics);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("products");
    assert expr.columns.size() == 3;
    assert expr.values.size() == 3;
    assert ((Expr.Literal) expr.columns.get(0)).value.equals("name");
    assert ((Expr.Literal) expr.columns.get(1)).value.equals("price");
    assert ((Expr.Literal) expr.columns.get(2)).value.equals("category");
    assert ((Expr.Literal) expr.values.get(0)).value.equals("laptop");
    assert ((Expr.Literal) expr.values.get(1)).value.equals("price999");
    assert ((Expr.Literal) expr.values.get(2)).value.equals("electronics");
  }

  @Test
  public void testInsertWithSpaces() {
    Scanner s = new Scanner("INSERT INTO table1 ( col1 , col2 , col3 ) VALUES ( val1 , val2 , val3 ) ;");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("table1");
    assert expr.columns.size() == 3;
    assert expr.values.size() == 3;
  }

  @Test
  public void testInsertCaseInsensitive() {
    Scanner s = new Scanner("insert into TABLE1 (COLUMN1) values (VALUE1);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("TABLE1");
    assert expr.columns.size() == 1;
    assert expr.values.size() == 1;
  }

  @Test
  public void testInsertWildcardAllColumns() {
    Scanner s = new Scanner("INSERT INTO employees * VALUES (john, age30, engineer, salary75000);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("employees");
    assert expr.columns.size() == 1; // Wildcard is represented as single column
    assert expr.values.size() == 4;
  }

  @Test
  public void testInsertIdentifierValues() {
    Scanner s = new Scanner("INSERT INTO scores (id, points) VALUES (john, mary);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("scores");
    assert expr.columns.size() == 2;
    assert expr.values.size() == 2;
    assert ((Expr.Literal) expr.values.get(0)).value.equals("john");
    assert ((Expr.Literal) expr.values.get(1)).value.equals("mary");
  }

  @Test
  public void testInsertLongTableName() {
    Scanner s = new Scanner("INSERT INTO verylongtablename (column) VALUES (value);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("verylongtablename");
    assert expr.columns.size() == 1;
    assert expr.values.size() == 1;
  }

  @Test
  public void testInsertManyColumns() {
    Scanner s = new Scanner("INSERT INTO data (a, b, c, d, e, f, g, h) VALUES (val1, val2, val3, val4, val5, val6, val7, val8);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    assert expr.table.literal.equals("data");
    assert expr.columns.size() == 8;
    assert expr.values.size() == 8;
  }

  @Test
  public void testInsertErrorMissingInto() {
    Scanner s = new Scanner("INSERT table1 (column1) VALUES (value1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing INTO";
    } catch (RuntimeException e) {
      assert e.getMessage().equals("Requires INTO after Insert");
    }
  }

  @Test
  public void testInsertErrorMissingTableName() {
    Scanner s = new Scanner("INSERT INTO (column1) VALUES (value1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing table name";
    } catch (RuntimeException e) {
      assert e.getMessage().equals("Requires table to be specified");
    }
  }

  @Test
  public void testInsertErrorMissingValues() {
    Scanner s = new Scanner("INSERT INTO table1 (column1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing VALUES";
    } catch (RuntimeException e) {
      assert e.getMessage().equals("Missing Values");
    }
  }

  @Test
  public void testInsertErrorMissingOpeningBracketColumns() {
    Scanner s = new Scanner("INSERT INTO table1 column1) VALUES (value1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing opening bracket";
    } catch (RuntimeException e) {
      assert e.getMessage().contains("Missing opening bracket");
    }
  }

  @Test
  public void testInsertErrorMissingClosingBracketColumns() {
    Scanner s = new Scanner("INSERT INTO table1 (column1 VALUES (value1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing closing bracket";
    } catch (RuntimeException e) {
      assert e.getMessage().contains("Missing Closing Bracket");
    }
  }

  @Test
  public void testInsertErrorMissingOpeningBracketValues() {
    Scanner s = new Scanner("INSERT INTO table1 (column1) VALUES value1);");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing opening bracket in values";
    } catch (RuntimeException e) {
      assert e.getMessage().equals("Missing opening bracket");
    }
  }

  @Test
  public void testInsertErrorMissingClosingBracketValues() {
    Scanner s = new Scanner("INSERT INTO table1 (column1) VALUES (value1;");
    Parser p = new Parser(s.scanTokens());
    
    try {
      p.insertStatement();
      assert false : "Should have thrown RuntimeException for missing closing bracket in values";
    } catch (RuntimeException e) {
      assert e.getMessage().contains("Missing Closing Bracket");
    }
  }

  @Test
  public void testInsertErrorEmptyColumns() {
    Scanner s = new Scanner("INSERT INTO table1 () VALUES (value1);");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    // Empty columns should result in empty list
    assert expr.columns.size() == 0;
    assert expr.values.size() == 1;
  }

  @Test
  public void testInsertErrorEmptyValues() {
    Scanner s = new Scanner("INSERT INTO table1 (column1) VALUES ();");
    Parser p = new Parser(s.scanTokens());
    Expr.Insert expr = (Expr.Insert) p.insertStatement();
    
    // Empty values should result in empty list
    assert expr.columns.size() == 1;
    assert expr.values.size() == 0;
  }
}