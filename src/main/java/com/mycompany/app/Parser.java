package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

/** Allow for reading in sql and converting to an AST. */
public class Parser {
  /** Column name used in an insert statement to mean every column of the table. */
  static final String ALL_COLUMNS = "*";

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
    } else if (check(TokenType.DELETE)) {
      if (verbose) {
        System.out.println("Parsing DELETE statement");
      }
      return deleteStatement();
    } else if (check(TokenType.DROP)) {
      if (verbose) {
        System.out.println("Parsing DROP statement");
      }
      return dropTableStatement();
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

    if (match(TokenType.STRING_LITERAL)) {
      return new Expr.Literal(previous().literal);
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

    // parse the columns and table level constraints
    List<Column> columns = new ArrayList<>();
    List<ForeignKey> foreignKeys = new ArrayList<>();
    List<String> tablePrimaryKey = null;
    if (!check(TokenType.RIGHT_PAR)) {
      do {
        if (match(TokenType.PRIMARY)) {
          if (!match(TokenType.KEY)) {
            throw new RuntimeException("Expected KEY after PRIMARY");
          }
          if (tablePrimaryKey != null) {
            throw new RuntimeException("Table " + tablename + " has more than one PRIMARY KEY");
          }
          tablePrimaryKey = identifierList("PRIMARY KEY");
        } else if (match(TokenType.FOREIGN)) {
          foreignKeys.add(tableForeignKey(tablename));
        } else {
          columns.add(column());
        }
      } while (match(TokenType.COMMA));
    }

    if (!match(TokenType.RIGHT_PAR)) {
      throw new RuntimeException("Expected ')' after columns");
    }

    if (!match(TokenType.SEMICOLON)) {
      throw new RuntimeException("Expected ';' after table definition");
    }

    if (tablePrimaryKey != null) {
      columns = applyTablePrimaryKey(tablename, columns, tablePrimaryKey);
    }

    // column level foreign keys become constraints too, ahead of the table level ones
    List<ForeignKey> allForeignKeys = new ArrayList<>();
    for (Column column : columns) {
      if (column.dependsOn() != null) {
        String[] reference = column.dependsOn().split("\\.");
        allForeignKeys.add(
            new ForeignKey(
                null,
                tablename,
                List.of(column.name()),
                reference[0],
                List.of(reference[1]),
                column.onDelete()));
      }
    }
    allForeignKeys.addAll(foreignKeys);

    return new Expr.Create(tablename, columns, allForeignKeys);
  }

  /**
   * Mark the columns named in a table level {@code PRIMARY KEY (a, b)} as the primary key.
   *
   * @param tablename the table being created, for error messages
   * @param columns the parsed columns
   * @param keyColumns the column names inside the clause
   * @return the columns with key flags applied
   */
  private static List<Column> applyTablePrimaryKey(
      String tablename, List<Column> columns, List<String> keyColumns) {
    for (Column column : columns) {
      if (column.primary()) {
        throw new RuntimeException(
            "Table " + tablename + " has more than one PRIMARY KEY, column " + column.name());
      }
    }
    List<Column> result = new ArrayList<>(columns);
    for (String keyColumn : keyColumns) {
      int index = -1;
      for (int i = 0; i < result.size(); i++) {
        if (result.get(i).name().equals(keyColumn)) {
          index = i;
        }
      }
      if (index < 0) {
        throw new RuntimeException(
            "PRIMARY KEY column " + keyColumn + " does not exist in table " + tablename);
      }
      result.set(index, result.get(index).withKeyFlags(true, false));
    }
    return result;
  }

  /**
   * Parse {@code KEY (a, b) REFERENCES parent(x, y) [ON DELETE action]}, after {@code FOREIGN}.
   *
   * @param tablename the table being created
   * @return the constraint, not yet validated against the parent table
   */
  private ForeignKey tableForeignKey(String tablename) {
    if (!match(TokenType.KEY)) {
      throw new RuntimeException("Expected KEY after FOREIGN");
    }
    List<String> childColumns = identifierList("FOREIGN KEY");
    if (!match(TokenType.REFERENCES)) {
      throw new RuntimeException("Expected REFERENCES after FOREIGN KEY columns");
    }
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected foreign table name after REFERENCES");
    }
    String parentTable = previous().lexeme;
    List<String> parentColumns = identifierList("REFERENCES " + parentTable);
    if (childColumns.size() != parentColumns.size()) {
      throw new RuntimeException(
          String.format(
              "FOREIGN KEY has %d columns but references %d columns of %s",
              childColumns.size(), parentColumns.size(), parentTable));
    }
    ReferentialAction onDelete = null;
    if (match(TokenType.ON)) {
      onDelete = onDeleteAction();
    }
    return new ForeignKey(null, tablename, childColumns, parentTable, parentColumns, onDelete);
  }

  /**
   * Parse a bracketed, comma separated list of column names, e.g. {@code (a, b)}.
   *
   * @param clause the clause the list belongs to, for error messages
   * @return the column names
   */
  private List<String> identifierList(String clause) {
    if (!match(TokenType.LEFT_PAR)) {
      throw new RuntimeException("Expected '(' after " + clause);
    }
    List<String> names = new ArrayList<>();
    do {
      if (!match(TokenType.IDENTIFIER)) {
        throw new RuntimeException("Expected column name in " + clause);
      }
      names.add(previous().lexeme);
    } while (match(TokenType.COMMA));
    if (!match(TokenType.RIGHT_PAR)) {
      throw new RuntimeException("Expected ')' after " + clause + " columns");
    }
    return names;
  }

  /**
   * Parse the rest of an {@code ON DELETE <action>} clause, after {@code ON}.
   *
   * @return the action
   */
  private ReferentialAction onDeleteAction() {
    if (match(TokenType.UPDATE)) {
      throw new RuntimeException("ON UPDATE is not supported yet");
    }
    if (!match(TokenType.DELETE)) {
      throw new RuntimeException("Expected DELETE after ON");
    }
    if (match(TokenType.CASCADE)) {
      return ReferentialAction.CASCADE;
    }
    if (match(TokenType.RESTRICT)) {
      return ReferentialAction.RESTRICT;
    }
    if (match(TokenType.NO)) {
      if (!match(TokenType.ACTION)) {
        throw new RuntimeException("Expected ACTION after NO");
      }
      return ReferentialAction.NO_ACTION;
    }
    if (match(TokenType.SET)) {
      if (match(TokenType.NULL)) {
        return ReferentialAction.SET_NULL;
      }
      if (match(TokenType.DEFAULT)) {
        return ReferentialAction.SET_DEFAULT;
      }
      throw new RuntimeException("Expected NULL or DEFAULT after SET");
    }
    throw new RuntimeException(
        "Expected CASCADE, RESTRICT, NO ACTION, SET NULL or SET DEFAULT after ON DELETE");
  }

  protected Column column() {
    String name = "";
    TokenType type = null;
    ;
    boolean isPrimaryKey = false;
    boolean nullable = true;
    String foreignTable = "";
    String foreignColumn = "";
    ReferentialAction onDelete = null;
    Object defaultLiteral = null;

    if (match(TokenType.IDENTIFIER)) {
      name = previous().lexeme;
    } else {
      throw new RuntimeException("Expected column name");
    }

    while (!isAtEnd() && peek().type != TokenType.COMMA && !check(TokenType.RIGHT_PAR)) {
      final int iterationStart = current;

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

      // referential action of the foreign key
      if (match(TokenType.ON)) {
        onDelete = onDeleteAction();
      }

      // default value
      if (match(TokenType.DEFAULT)) {
        Expr.Literal value = insertValue();
        if (value == null) {
          throw new RuntimeException("Expected value after DEFAULT");
        }
        defaultLiteral = value.value;
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

      // no constraint matched, so the token can never be consumed
      if (current == iterationStart) {
        throw new RuntimeException(
            String.format("Unexpected token '%s' in column definition", peek().lexeme));
      }
    }

    if (type == null) {
      throw new RuntimeException("Expected column type after column name");
    }

    // stored the same way an inserted value would be, which also checks its type
    String defaultValue =
        defaultLiteral == null
            ? null
            : new Column(name, type, true, false, null).toStoredValue(defaultLiteral);

    if (String.format("%s.%s", foreignTable, foreignColumn).equals(".")) {
      if (onDelete != null) {
        throw new RuntimeException("ON DELETE requires REFERENCES on column " + name);
      }
      // primary key columns can never be null
      return new Column(
          name, type, nullable && !isPrimaryKey, isPrimaryKey, null, null, defaultValue);
    }
    return new Column(
        name,
        type,
        nullable && !isPrimaryKey,
        isPrimaryKey,
        String.format("%s.%s", foreignTable, foreignColumn),
        onDelete,
        defaultValue);
  }

  /**
   * Parse {@code DELETE FROM table [WHERE condition] [;]}.
   *
   * @return the parsed {@link Expr.Delete}
   */
  protected Expr deleteStatement() {
    if (!match(TokenType.DELETE)) {
      throw new RuntimeException("Expected DELETE keyword");
    }
    if (!match(TokenType.FROM)) {
      throw new RuntimeException("Expected FROM after DELETE");
    }
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected table name after DELETE FROM");
    }
    final Token table = previous();

    List<Expr> whereList = new ArrayList<>();
    Expr whereCondition = whereClause();
    if (whereCondition != null) {
      whereList.add(whereCondition);
    }

    match(TokenType.SEMICOLON); // optional terminator
    // anything left over, such as a misspelt WHERE, must not turn into deleting every row
    if (!isAtEnd()) {
      throw new RuntimeException(
          String.format("Unexpected token '%s' in DELETE statement", peek().lexeme));
    }
    return new Expr.Delete(table, whereList);
  }

  /**
   * Parse {@code DROP TABLE table [;]}.
   *
   * @return the parsed {@link Expr.DropTable}
   */
  protected Expr dropTableStatement() {
    if (!match(TokenType.DROP)) {
      throw new RuntimeException("Expected DROP keyword");
    }
    if (!match(TokenType.TABLE)) {
      throw new RuntimeException("Expected TABLE after DROP");
    }
    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Expected table name after DROP TABLE");
    }
    final Token table = previous();
    match(TokenType.SEMICOLON); // optional terminator
    if (!isAtEnd()) {
      throw new RuntimeException(
          String.format("Unexpected token '%s' in DROP TABLE statement", peek().lexeme));
    }
    return new Expr.DropTable(table);
  }

  protected Expr selectStatement() {
    List<Expr> variables = new ArrayList<>();
    Token table = new Token(null, null, null, 0);

    if (!match(TokenType.SELECT)) {
      throw new RuntimeException("Expected SELECT keyword");
    }

    // parse the variables
    while (match(TokenType.STAR, TokenType.IDENTIFIER)) {
      variables.add(new Expr.Literal(previous().lexeme)); // give just the name of the column

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

  /**
   * Parse an insert statement of the form {@code INSERT INTO table [(col, ...) | *] VALUES (value,
   * ...) [;]}. Omitting the column list, or using {@code *}, inserts into every column in schema
   * order; this is represented as a single {@code Literal("*")} column.
   *
   * @return the parsed {@link Expr.Insert}
   */
  protected Expr insertStatement() {
    if (!match(TokenType.INSERT)) {
      throw new RuntimeException("Requires Insert Statement");
    }

    if (!match(TokenType.INTO)) {
      throw new RuntimeException("Requires INTO after Insert");
    }

    if (!match(TokenType.IDENTIFIER)) {
      throw new RuntimeException("Requires table to be specified");
    }
    final Token table = previous();

    List<Expr> columns = new ArrayList<>();
    List<Expr> values = new ArrayList<>();

    // parse the list of columns to insert into
    if (match(TokenType.STAR) || check(TokenType.VALUES)) {
      columns.add(new Expr.Literal(ALL_COLUMNS));
    } else {
      if (!match(TokenType.LEFT_PAR)) {
        throw new RuntimeException(
            "Missing opening bracket, Columns to be inserted need to be of form `(col, col2, col3)`"
                + " or `*`");
      }

      if (!check(TokenType.RIGHT_PAR)) {
        do {
          if (!match(TokenType.IDENTIFIER)) {
            if (columns.isEmpty()) {
              break; // reported as a missing closing bracket below
            }
            throw new RuntimeException("Expected column name after ','");
          }
          columns.add(new Expr.Literal(previous().lexeme));
        } while (match(TokenType.COMMA));
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

    if (!check(TokenType.RIGHT_PAR)) {
      do {
        Expr value = insertValue();
        if (value == null) {
          if (values.isEmpty()) {
            break; // reported as a missing closing bracket below
          }
          throw new RuntimeException("Expected value after ','");
        }
        values.add(value);
      } while (match(TokenType.COMMA));
    }

    if (!match(TokenType.RIGHT_PAR)) {
      throw new RuntimeException("[Parser] Insert Parsing: Missing Closing Bracket");
    }

    match(TokenType.SEMICOLON); // optional terminator

    return new Expr.Insert(columns, table, values);
  }

  /**
   * Parse a single value of an insert statement. Numbers become {@link Double}, quoted strings and
   * bare words become {@link String}, and {@code NULL} becomes a literal holding {@code null}.
   *
   * @return the literal, or {@code null} if the next token is not a value
   */
  private Expr.Literal insertValue() {
    if (match(TokenType.NUMBER)) {
      return new Expr.Literal(previous().literal);
    }
    if (check(TokenType.MINUS)
        && current + 1 < tokens.size()
        && tokens.get(current + 1).type == TokenType.NUMBER) {
      advance();
      advance();
      return new Expr.Literal(-((Double) previous().literal));
    }
    if (match(TokenType.STRING_LITERAL)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Literal(previous().lexeme);
    }
    if (match(TokenType.NULL)) {
      return new Expr.Literal(null);
    }
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
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }
}
