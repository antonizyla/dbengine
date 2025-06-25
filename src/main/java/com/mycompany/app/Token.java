package com.mycompany.app;

/**
 * Enum for token types
 */
enum TokenType {
    // 1 or 2 character tokens
    LEFT_PAR, RIGHT_PAR, EQUALS, GREATER_THAN, GREATER_OR_EQ, LESS_THAN, LESS_THAN_OR_EQ, SEMICOLON,

    // literals
    IDENTIFIER, STRING, NUMBER,

    // keywords
    SELECT, INSERT, DELETE, FROM, WHERE, AND, OR, NOT, IN,

    EOF
}

/**
 * Represent a Token when parsing Sql
 */
public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    /**
     * Constructor for token objects
     *
     * @param type
     *            enum of tokentype
     * @param lexeme
     *            the actual character/characters that were parsed into this token
     * @param literal
     *            the literal that is represented e.g. "7" -> Int 7
     * @param line
     *            Line number of the token
     */
    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * String represenation of a token
     */
    public String toString() {
        return String.format("%s, %s, %s", type, lexeme, literal);
    }
}
