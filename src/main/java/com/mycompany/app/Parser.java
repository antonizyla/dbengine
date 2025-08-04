package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

/** Allow for reading in sql and converting to an AST. */
public class Parser {
  private final List<Token> tokens;
  private int current = 0;

  /**
   * allow for reading in sql and converting to an AST.
   *
   * @param tokens takes in set of tokens from the scanner.
   */
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Parser() {
    this.tokens = new ArrayList<>();
    // for testing purposes
  }

  private boolean isAtEnd() {
    return current >= tokens.size();
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token advance() {
    if (!isAtEnd()) {
      return tokens.get(current++);
    }
    return previous();
  }

  List<Expr> parse() {
    List<Expr> statements = new ArrayList<>();
    // TODO FOR NOW JUST PARSING FIRST STATEMENT
    statements.add(statement(true));
    return statements;
  }

  protected Expr.EngineExpr parseEngineExpr() {
    if (peek().type == TokenType.CREATE) {
      return createDatabase();
    } else if (peek().type == TokenType.DROP) {
      return dropDatabase();
    } else if (peek().type == TokenType.IDENTIFIER) {
      // enter database
      match(TokenType.IDENTIFIER);
      String dbName = previous().lexeme;
      return new Expr.EngineExpr(dbName, false, false);
    } else {
      throw new RuntimeException("Expected CREATE or DROP keyword for engine expression");
    }
  }

  private Expr.EngineExpr createDatabase() {
    if (!match(TokenType.CREATE)) {
      throw new RuntimeException("Expected CREATE keyword");
    }
    if (!match(TokenType.DATABASE)) {
      throw new RuntimeException("Expected DATABASE keyword after CREATE");
    }
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected database name after CREATE DATABASE");
    }
    String dbName = previous().lexeme;
    if (!match(TokenType.SEMICOLON)) {
      throw new RuntimeException("Expected ';' after database name");
    }
    return new Expr.EngineExpr(dbName, false, true);
  }

  private Expr.EngineExpr dropDatabase() {
    if (!match(TokenType.DROP)) {
      throw new RuntimeException("Expected DROP keyword");
    }
    if (!match(TokenType.DATABASE)) {
      throw new RuntimeException("Expected DATABASE keyword after DROP");
    }
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected database name after DROP DATABASE");
    }
    String dbName = previous().lexeme;
    if (!match(TokenType.SEMICOLON)) {
      throw new RuntimeException("Expected ';' after database name");
    }
    return new Expr.EngineExpr(dbName, true, false);
  }

  private Expr statement(boolean verbose) {
    if (check(TokenType.CREATE)) {
      if (verbose) {
        System.out.println("Parsing CREATE statement");
      }
      return createTable();
    } else if (check(TokenType.SELECT)) {
      if (verbose) {
        System.out.println("Parsing SELECT statement");
      }
      return selectStatement();
    } else if (check(TokenType.INSERT)) {
      if (verbose) {
        System.out.println("Parsing INSERT statement");
      }
      return insertStatement();
    }
    return null; // Unimplemented everything else
  }

  protected Expr alias() {
    Token identifier = new Token(null, null, tokens, current);
    if (match(TokenType.IDENTIFIER)) {
      identifier = previous();
      if (!isAtEnd() && match(TokenType.AS)) {
        if (match(TokenType.IDENTIFIER)) {
          // got an alias
          return new Expr.Alias(
              new Expr.Literal(identifier.lexeme), new Expr.Literal(previous().lexeme));
        }
      }
    } else {
      throw new RuntimeException("Expected identifier after SELECT");
    }
    return new Expr.Alias(new Expr.Literal(identifier.lexeme), new Expr.Literal(identifier.lexeme));
  }

  protected Expr standardExpr() {
    return expression();
  }

  protected Expr expression() {
    return addition();
  }

  private Expr addition() {
    Expr expr = multiplication();

    while (match(TokenType.PLUS, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = multiplication();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr multiplication() {
    Expr expr = unary();

    while (match(TokenType.STAR, TokenType.SLASH)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(TokenType.MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(TokenType.NUMBER)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Literal(previous().lexeme);
    }

    if (match(TokenType.LEFT_PAR)) {
      Expr expr = expression();
      if (!match(TokenType.RIGHT_PAR)) {
        throw new RuntimeException("Expected ')' after expression");
      }
      return new Expr.Grouping(expr);
    }

    throw new RuntimeException("Expected expression");
  }

  protected List<Column> columnList() {
    // parse the columns
    List<Column> columns = new ArrayList<>();
    // a column is based on an identifier... to a comma
    while (!isAtEnd() && (match(TokenType.COMMA) || peek().type == TokenType.IDENTIFIER)) {
      columns.add(column());
    }
    return columns;
  }

  protected Expr createTable() {
    // to handle the create table statement
    match(TokenType.CREATE);

    if (!match(TokenType.TABLE)) {
      throw new RuntimeException(
          "Expected TABLE keyword after CREATE"); // will be changed when can do index etc...
    }

    // parse the table name
    String tablename = "";
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected table name after CREATE");
    }
    tablename = previous().lexeme;

    if (!match(TokenType.LEFT_PAR)) {
      throw new RuntimeException("Expected '(' after table name");
    }

    // parse the columns
    var columns = columnList();

    if (!match(TokenType.RIGHT_PAR)) {
      throw new RuntimeException("Expected ')' after columns");
    }

    if (!match(TokenType.SEMICOLON)) {
      throw new RuntimeException("Expected ';' after table definition");
    }

    return new Expr.Create(tablename, columns);
  }

  protected Column column() {
    String name = "";
    TokenType type = null;
    ;
    boolean isPrimaryKey = false;
    boolean nullable = true;
    String foreignTable = "";
    String foreignColumn = "";

    if (match(TokenType.IDENTIFIER)) {
      name = previous().lexeme;
    } else {
      throw new RuntimeException("Expected column name");
    }

    while (!isAtEnd() && peek().type != TokenType.COMMA && !check(TokenType.RIGHT_PAR)) {
      // foreign keys
      if (match(TokenType.REFERENCES)) {
        if (!match(TokenType.IDENTIFIER)) {
          throw new RuntimeException("Expected foreign table name after REFERENCES");
        }
        foreignTable = previous().lexeme;
        if (!match(TokenType.LEFT_PAR)) {
          throw new RuntimeException("Expected '(' after foreign table name. e.g. table(column)");
        }
        if (!match(TokenType.IDENTIFIER)) {
          throw new RuntimeException("Expected foreign column name after '(', e.g. table(column)");
        }
        foreignColumn = previous().lexeme;
        if (!match(TokenType.RIGHT_PAR)) {
          throw new RuntimeException("Expected ')' after foreign column name e.g. table(column)");
        }

        nullable = false; // foreign keys cannot be null
      }

      // primary keys
      if (match(TokenType.PRIMARY)) {
        if (!match(TokenType.KEY)) {
          throw new RuntimeException("Expected KEY after PRIMARY");
        } else {
          isPrimaryKey = true;
        }
      }

      // data type
      if (match(TokenType.STRING_TYPE, TokenType.NUMBER)) {
        type = previous().type;
      }

      // check if nullable
      if (match(TokenType.NOT)) {
        if (!match(TokenType.NULL)) {
          throw new RuntimeException("Unexpected token after NOT, expected NULL");
        }
        nullable = false;
      }
    }

    if (type == null) {
      throw new RuntimeException("Expected column type after column name");
    }

    if (String.format("%s.%s", foreignTable, foreignColumn).equals(".")) {
      return new Column(name, type, false, isPrimaryKey, null);
    }
    return new Column(
        name, type, nullable, isPrimaryKey, String.format("%s.%s", foreignTable, foreignColumn));
  }

  protected Expr selectStatement() {
    List<Expr> variables = new ArrayList<>();
    Token table = new Token(null, null, null, 0);

    if (!match(TokenType.SELECT)) {
      throw new RuntimeException("Expected SELECT keyword");
    }

    // parse the variables
    while (match(TokenType.STAR, TokenType.IDENTIFIER)) {
      variables.add(new Expr.Literal(previous().literal)); // give just the name of the column

      if (!match(TokenType.COMMA)) {
        break; // if no comma continue to next variable by doing loop again
      }
    }

    // parse the FROM keyword
    if (!match(TokenType.FROM)) {
      throw new RuntimeException("Expected FROM after SELECT variables");
    }

    // parse the table name
    if (match(TokenType.IDENTIFIER)) {
      table = previous();
    } else {
      throw new RuntimeException("Expected table name after SELECT");
    }

    // parse optional WHERE clause
    Expr whereCondition = whereClause();
    List<Expr> whereList = new ArrayList<>();
    if (whereCondition != null) {
      whereList.add(whereCondition);
    }

    return new Expr.Select(variables, table, whereList);
  }

  private Expr whereClause() {
    if (!match(TokenType.WHERE)) {
      return null;
    }
    return orExpression();
  }

  private Expr orExpression() {
    Expr expr = andExpression();

    while (match(TokenType.OR)) {
      Token operator = previous();
      Expr right = andExpression();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr andExpression() {
    Expr expr = logicalPrimary();

    while (match(TokenType.AND)) {
      Token operator = previous();
      Expr right = logicalPrimary();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr logicalPrimary() {
    if (match(TokenType.LEFT_PAR)) {
      Expr expr = orExpression();
      if (!match(TokenType.RIGHT_PAR)) {
        throw new RuntimeException("Expected ')' after logical expression");
      }
      return new Expr.Grouping(expr);
    }

    return equality();
  }

  private Expr equality() {
    Expr expr = comparison();

    while (match(TokenType.EQUALS)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = expression();

    while (match(
        TokenType.GREATER_THAN,
        TokenType.GREATER_OR_EQ,
        TokenType.LESS_THAN,
        TokenType.LESS_THAN_OR_EQ)) {
      Token operator = previous();
      Expr right = expression();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  protected Expr insertStatement() {

    if (!match(TokenType.INSERT)) {
      throw new RuntimeException("Requires Insert Statement");
    }

    if (match(TokenType.INTO)) {
      System.out.println("Matched into");
    } else {
      throw new RuntimeException("Requires INTO after Insert");
    }
    // parse the table;
    Token table = null;
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Requires table to be specified");
    } else {
      table = previous();
    }

    List<Expr> columns = new ArrayList<Expr>();
    List<Expr> values = new ArrayList<Expr>();

    // parse the list of columns to insert into
    if (check(TokenType.STAR)) {
      match(TokenType.STAR);
      columns.add(new Expr.Literal(previous()));
    } else {
      if (!match(TokenType.LEFT_PAR)) {
        throw new RuntimeException(
            "Missing opening bracket, Columns to be inserted need to be of form `(col, col2, col3)`"
                + " or `*`");
      }

      while (!check(TokenType.RIGHT_PAR)
          && (check(TokenType.COMMA) || check(TokenType.IDENTIFIER))) {
        if (check(TokenType.COMMA)) {
          match(TokenType.COMMA);
        } else if (!match(TokenType.IDENTIFIER)) {
          throw new RuntimeException("Error gl");
        } else {
          columns.add(new Expr.Literal(previous().lexeme));
        }
      }

      if (!match(TokenType.RIGHT_PAR)) {
        throw new RuntimeException(
            "Missing Closing Bracket, Columns to be inserted need to be of form `(col, col2,"
                + " col3...)` or `*`");
      }
    }

    if (!match(TokenType.VALUES)) {
      throw new RuntimeException("Missing Values");
    }

    if (!match(TokenType.LEFT_PAR)) {
      throw new RuntimeException("Missing opening bracket");
    }

    while (!check(TokenType.RIGHT_PAR) && (check(TokenType.COMMA) || check(TokenType.IDENTIFIER))) {
      if (check(TokenType.COMMA)) {
        match(TokenType.COMMA);
      } else if (!match(TokenType.IDENTIFIER)) {
        throw new RuntimeException("Error gl");
      } else {
        values.add(new Expr.Literal(previous().lexeme));
      }
    }

    if (!match(TokenType.RIGHT_PAR)) {
      throw new RuntimeException("Missing Closing Bracket");
    }

    return new Expr.Insert(columns, table, values);
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }
}
