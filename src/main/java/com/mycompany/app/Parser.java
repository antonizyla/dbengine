package com.mycompany.app;

import java.util.List;

// allow for reading in sql and converting to an AST
public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }
}
