package com.mycompany.app;

/** Enum for token types. */
enum TokenType {
  // 1 or 2 character tokens
  LEFT_PAR,
  RIGHT_PAR,
  EQUALS,
  GREATER_THAN,
  GREATER_OR_EQ,
  LESS_THAN,
  LESS_THAN_OR_EQ,
  SEMICOLON,

  // literals
  IDENTIFIER,
  STRING,
  NUMBER,

  // keywords
  SELECT,
  INSERT,
  DELETE,
  FROM,
  WHERE,
  AND,
  OR,
  NOT,
  IN,

  EOF
}
