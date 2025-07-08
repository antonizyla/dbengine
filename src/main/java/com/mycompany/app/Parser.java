package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.app.Expr.Create;

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
    while (!isAtEnd()) {
      statements.add(statement());
    }
    return statements;
  }

  private Expr statement() {
    if (match(TokenType.SELECT)) {
      return selectStatement();
    } else if (match(TokenType.INSERT)) {
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
          return new Expr.Alias(new Expr.Literal(identifier.lexeme), new Expr.Literal(previous().lexeme));
        }
      }
    } else {
      throw new RuntimeException("Expected identifier after SELECT");
    }
    return new Expr.Alias(new Expr.Literal(identifier.lexeme), new Expr.Literal(identifier.lexeme));
  }

  protected Expr standardExpr() {
    // to handle any standard expression
    return null;
  }

  protected Expr createTable() {
    // to handle the create table statement
    String tablename = null;
    List<Column> columns = new ArrayList<>();

    match(TokenType.CREATE);
    if (!match(TokenType.TABLE)) {
      throw new RuntimeException("Expected TABLE keyword after CREATE"); // will be changed when can do index etc...
    }

    // parse the table name
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected table name after CREATE");
    }
    tablename = previous().lexeme;

    if (!match(TokenType.LEFT_PAR)) {
      throw new RuntimeException("Expected '(' after table name");
    }

    // parse the columns

  }

  protected Column column() {
    String name = "";
    String type = "";
    boolean isPrimaryKey = false;
    boolean nullable = true; 
    boolean isForeignKey = false;
    String foreignTable = "";
    String foreignColumn = "";

    if (match(TokenType.IDENTIFIER)) {
      name = previous().lexeme;
    } else {
      throw new RuntimeException("Expected column name");
    }

    while (!match(TokenType.COMMA)) {
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
      }

      // primary keys
      if (match(TokenType.PRIMARY)) {
        if (!match(TokenType.KEY)) {
          throw new RuntimeException("Expected KEY after PRIMARY");
        }
      }

      // data type 
      if (match(TokenType.STRING_TYPE, TokenType.NUMBER)) {
        type = previous().lexeme;
      } else {
        throw new RuntimeException("Expected data type declaration");
      }

      // check if nullable
      if (match(TokenType.NOT)){
        if (!match(TokenType.NULL)){
          throw new RuntimeException("Unexpected token after NOT, expected NULL");
        }
        nullable = false;
      }

    }

    if (String.format("%s.%s", foreignTable, foreignColumn).equals(".")) {
      return new Column(name, type, false, isPrimaryKey, null);
    }
  }

  protected Expr selectStatement() {
    List<Expr> variables = new ArrayList<>();
    Token table = new Token(null, null, null, 0);

    if (!match(TokenType.SELECT)) {
      throw new RuntimeException("Expected SELECT keyword");
    }

    // parse the variables
    while (match(TokenType.IDENTIFIER)) {

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

    return new Expr.Select(variables, table, null);
  }

  private List<Expr> whereClauses() {
    return null;
  }

  private Expr insertStatement() {
    return null;
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
    if (isAtEnd())
      return false;
    return peek().type == type;
  }
}
