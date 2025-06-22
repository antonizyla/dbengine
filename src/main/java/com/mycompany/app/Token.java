package com.mycompany.app;

enum TokenType {
    // 1 or 2 character tokens
    LEFT_PAR, RIGHT_PAR, EQUALS, GREATER_THAN, GREATER_OR_EQ, LESS_THAN, LESS_THAN_OR_EQ, SEMICOLON,

    // literals
    IDENTIFIER, STRING, NUMBER,

    // keywords
    SELECT, INSERT, DELETE, FROM, WHERE, AND, OR, NOT, IN,

    EOF
}

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return String.format("%s, %s, %s", type, lexeme, literal);
    }
}
