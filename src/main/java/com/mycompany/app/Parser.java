package com.mycompany.app;

import java.util.List;

/**
 * allow for reading in sql and converting to an AST.
 *
 * @param tokens takes in set of tokens from the scanner.
 */
public class Parser {
  private final List<Token> tokens;
  private final int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
}
