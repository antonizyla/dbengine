package com.mycompany.app;

import java.util.List;

abstract class Expr {

  interface Vistor<R> {
    R visitBinaryExpr(Binary expr);
  }

  static class Select extends Expr {
    final List<Expr> variables;
    final Token table;
    final List<Expr> whereClause;

    Select(List<Expr> variables, Token table, List<Expr> whereClause) {
      this.variables = variables;
      this.table = table;
      this.whereClause = whereClause;
    }
  }

  static class Create extends Expr {
    final String tableName;
    final List<Column> columns;

    Create(String tableName, List<Column> cols) {
      this.tableName = tableName;
      this.columns = cols;
    }

    @Override
    public String toString() {
      return "Create{" + "tableName='" + tableName + '\'' + ", columns=" + columns + '}';
    }
  }

  // represents x o y where o is an operator
  static class Binary extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
  }

  // rpresents ( expr )
  static class Grouping extends Expr {
    final Expr expression;

    Grouping(Expr expression) {
      this.expression = expression;
    }
  }

  // represents a literal value
  static class Literal extends Expr {
    final Object value;

    Literal(Object value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Literal(" + value.toString() + ")";
    }
  }

  static class EngineExpr extends Expr {
    final String database;
    final boolean drop;
    final boolean create;

    // if not drop, then it is create
    EngineExpr(String database, boolean drop, boolean create) {
      this.drop = drop;
      this.create = create;
      this.database = database;
    }

    @Override
    public String toString() {
      return "EngineExpr{"
          + "database='"
          + database
          + '\''
          + ", drop="
          + drop
          + ", create="
          + create
          + '}';
    }
  }

  // represents logical expression
  // e.g exp1 and exp2, exp1 is exp2 etc..
  static class Logical extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
  }

  static class Unary extends Expr {
    final Token operator;
    final Expr right;

    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }
  }

  // to handle select a,b,c from table
  static class VariableList extends Expr {
    final List<Expr> variables;

    VariableList(List<Expr> variables) {
      this.variables = variables;
    }
  }

  // to handle x as y
  static class Alias extends Expr {
    final Expr colExpr;
    final Literal alias;

    Alias(Expr columnName, Literal alias) {
      this.colExpr = columnName;
      this.alias = alias;
    }

    @Override
    public String toString() {
      return "Alias{" + "columnName=" + colExpr + ", alias=" + alias + '}';
    }
  }
}
