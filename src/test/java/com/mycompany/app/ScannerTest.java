package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ScannerTest {

  @Test
  public void testNumberParsing() {
    String a = "23";
    String b = "1223";
    String c = "0.0101";

    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().get(0).literal, 23.0);

    Scanner B = new Scanner(b);
    assertEquals(B.scanTokens().get(0).literal, 1223.0);

    Scanner C = new Scanner(c);
    assertEquals(C.scanTokens().get(0).literal, 0.0101);
  }

  @Test
  public void testNumbersFollowedByTokens() {
    String a = "22 >";
    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().size(), 2);
  }

  @Test
  public void testNumbersPrefixedByTokens() {
    String a = ") 22";
    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().size(), 2);
  }

  @Test
  public void testBeforeAndAfterTokens() {
    String a = ") 22 \"hello\" 4";
    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().size(), 4);
  }

  @Test
  public void testStringScanning() {
    String a = "\"hello\"";
    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().get(0).literal, "hello");
  }

  @Test
  public void testIdentifier() {
    String a = "Hello";
    Scanner A = new Scanner(a);
    assertEquals(A.scanTokens().get(0).type, TokenType.IDENTIFIER);
  }

  @Test
  public void testReserved() {
    String a = "Select from";
    Scanner A = new Scanner(a);
    var tokens = A.scanTokens();
    assertEquals(tokens.get(0).type, TokenType.SELECT);
    assertEquals(tokens.get(1).type, TokenType.FROM);
  }

  @Test
  public void testSingleCharacterTokens() {
    Scanner A = new Scanner("( ) ; = , + - * /");
    var types = A.scanTokens().stream().map(t -> t.type).toList();
    assertEquals(
        List.of(
            TokenType.LEFT_PAR,
            TokenType.RIGHT_PAR,
            TokenType.SEMICOLON,
            TokenType.EQUALS,
            TokenType.COMMA,
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH),
        types);
  }

  @Test
  public void testComparisonOperators() {
    Scanner A = new Scanner("< <= > >=");
    var types = A.scanTokens().stream().map(t -> t.type).toList();
    assertEquals(
        List.of(
            TokenType.LESS_THAN,
            TokenType.LESS_THAN_OR_EQ,
            TokenType.GREATER_THAN,
            TokenType.GREATER_OR_EQ),
        types);
  }

  @Test
  public void testComparisonAtEndOfInput() {
    Scanner A = new Scanner("<");
    var tokens = A.scanTokens();
    assertEquals(tokens.size(), 1);
    assertEquals(tokens.get(0).type, TokenType.LESS_THAN);
  }

  @Test
  public void testTabsIgnored() {
    Scanner A = new Scanner("select\t*\tfrom");
    assertEquals(A.scanTokens().size(), 3);
  }

  @Test
  public void testUnexpectedCharactersSkipped() {
    // characters below, between and above the letter ranges are not valid tokens
    Scanner A = new Scanner("a ! [ ~ @ b");
    var tokens = A.scanTokens();
    assertEquals(tokens.size(), 2);
    assertEquals(tokens.get(0).lexeme, "a");
    assertEquals(tokens.get(1).lexeme, "b");
  }

  @Test
  public void testNumberWithTrailingDot() {
    // the '.' is not part of the number when no digit follows it
    Scanner A = new Scanner("12.");
    var tokens = A.scanTokens();
    assertEquals(tokens.size(), 1);
    assertEquals(tokens.get(0).literal, 12.0);
  }

  @Test
  public void testNumberWithDotThenLetter() {
    Scanner A = new Scanner("12.a");
    var tokens = A.scanTokens();
    assertEquals(tokens.size(), 2);
    assertEquals(tokens.get(0).literal, 12.0);
    assertEquals(tokens.get(1).type, TokenType.IDENTIFIER);
  }

  @Test
  public void testUnterminatedString() {
    Scanner A = new Scanner("\"hello");
    assertEquals(A.scanTokens().size(), 0);
  }

  @Test
  public void testMultilineStringIncrementsLine() {
    Scanner A = new Scanner("\"hello\nworld\" next");
    var tokens = A.scanTokens();
    assertEquals(tokens.get(0).literal, "hello\nworld");
    assertEquals(tokens.get(1).line, 2);
  }

  @Test
  public void testIdentifierWithDigits() {
    Scanner A = new Scanner("table1");
    var tokens = A.scanTokens();
    assertEquals(tokens.size(), 1);
    assertEquals(tokens.get(0).lexeme, "table1");
    assertEquals(tokens.get(0).type, TokenType.IDENTIFIER);
  }

  @Test
  public void testKeywordsAreCaseInsensitive() {
    Scanner A = new Scanner("INSERT into VaLuEs");
    var tokens = A.scanTokens();
    assertEquals(tokens.get(0).type, TokenType.INSERT);
    assertEquals(tokens.get(1).type, TokenType.INTO);
    assertEquals(tokens.get(2).type, TokenType.VALUES);
  }
}
