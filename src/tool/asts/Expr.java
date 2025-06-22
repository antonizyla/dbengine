package com.mycompany.app;

import java.util.List;

abstract class Expr {
    static class Binary extends Expr {
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.Token = Token;
            this.Expr = Expr;
        }

        final Expr left;
        final Token operator;
        final Expr right;
    }
}
