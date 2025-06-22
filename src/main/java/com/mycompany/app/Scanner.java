package com.mycompany.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.System;

// takes in a raw string and returns a list of tokens
public class Scanner {

    // for checking if an identifier is a reserved word
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<String, TokenType>();
        keywords.put("select", TokenType.SELECT);
        keywords.put("where", TokenType.WHERE);
        keywords.put("insert", TokenType.INSERT);
        keywords.put("delete", TokenType.DELETE);
        keywords.put("from", TokenType.FROM);
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("not", TokenType.NOT);
        keywords.put("in", TokenType.IN);

    }

    private final String input;
    private final List<Token> tokens = new ArrayList<>();

    // store data about the current position of the parser
    private int line = 1;
    private int current = 0;
    private int start = 0;

    Scanner(String input) {
        this.input = input;
    }

    // determine if finished going through entire input string.
    private boolean atEnd() {
        return current >= input.length();
    }

    public List<Token> scanTokens() {
        while (!atEnd()) {
            start = current;
            scanToken();
        }
        return this.tokens;
    }

    private char advance() {
        return input.charAt(current++);
    }

    private boolean match(char nextchar) {
        if (atEnd() || input.charAt(current) != nextchar) {
            return false;
        }
        current++;
        return true;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ')':
                addToken(TokenType.RIGHT_PAR);
                break;
            case '(':
                addToken(TokenType.LEFT_PAR);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '=':
                addToken(TokenType.EQUALS);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_THAN_OR_EQ : TokenType.LESS_THAN);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_OR_EQ : TokenType.GREATER_THAN);
                break;
            case '"':
                scanString();
                break;
            case ' ':
                break;
            case '\t':
                break;
            default:
                if (isDigit(c)) {
                    scanNumber();
                    break;
                } else if (isLetter(c)) {
                    scanIdentifier();
                    break;
                }
                System.err.println(String.format("[Scanner] Unexpected Character, %s", c));
                break;
        }
    }

    private char lookahead() {
        if (atEnd())
            return '\0';
        return input.charAt(current);
    }

    private char lookaheadTwice() {
        if (current + 1 >= input.length())
            return '\0';
        return input.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return (c >= 48 && c <= 57);
    }

    private boolean isLetter(char c) {
        return (c >= 65 && c <= 90) || (c >= 97 && c <= 122);
    }

    private void scanNumber() {
        while (isDigit(lookahead()) && lookahead() != '\0') {
            advance();
        }
        // scan the decimal and then any decimal digits after it
        if ((lookahead() == '.') && isDigit(lookaheadTwice())) {
            advance();
            while (isDigit(lookahead())) {
                advance();
            }
        }

        addToken(TokenType.NUMBER, Double.parseDouble(input.substring(start, current)));
    }

    private void scanString() {
        while (lookahead() != '"' && !atEnd()) {
            if (lookahead() == '\n') {
                line++;
            }
            advance();
        }
        if (atEnd()) {
            System.err.println(String.format("[Scanner] Unterminated String, Line:%s", line));
            return;
        }

        advance(); // advance to the closing quote mark

        String value = input.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void scanIdentifier() {
        while (isLetter(lookahead()) && !atEnd()) {
            advance();
        }
        String value = input.substring(start, current);

        // determine token type ID vs Reserved Word
        TokenType t = keywords.get(value.toLowerCase());
        if (t == null) {
            t = TokenType.IDENTIFIER;
        }

        addToken(t, value);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = input.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

}
