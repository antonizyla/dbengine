package com.mycompany.app;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TableTest {

  @Test
  public void testHasColumn() {
    Table table =
        new Table(
            "TestTable",
            List.of(
                new Column("id", TokenType.NUMBER, false, true, null),
                new Column("name", TokenType.STRING_TYPE, true, false, null)));

    assert table.hasColumn("id");
    assert table.hasColumn("name");
    assert !table.hasColumn("nonexistent_column");
  }

  @Test
  public void testGetName() {
    Table table =
        new Table(
            "TestTable",
            List.of(
                new Column("id", TokenType.NUMBER, false, true, null),
                new Column("name", TokenType.STRING_TYPE, true, false, null)));

    assert table.getName().equals("TestTable");
  }

  @Test
  public void testGetColumns() {
    Table table =
        new Table(
            "TestTable",
            List.of(
                new Column("id", TokenType.NUMBER, false, true, null),
                new Column("name", TokenType.STRING_TYPE, true, false, null)));
    List<Column> columns = table.getColumns();
    assert columns.size() == 2;
    assert columns.get(0).name().equals("id");
    assert columns.get(1).name().equals("name");
  }

  @Test
  public void insertAndQuery() {
    Table table =
        new Table(
            "TestTable",
            List.of(
                new Column("id", TokenType.NUMBER, false, true, null),
                new Column("name", TokenType.STRING_TYPE, true, false, null)));

    // Insert a row
    table.insert(List.of("1", "Alice"));

    // Query the row
    var id = table.getRowCol("1", "id");
    var name = table.getRowCol("1", "name");

    assert id.equals("1");
    assert name.equals("Alice");
  }
}
