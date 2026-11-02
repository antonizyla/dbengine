package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
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

  private Table people() {
    return new Table(
        "people",
        List.of(
            new Column("id", TokenType.NUMBER, false, true, null),
            new Column("name", TokenType.STRING_TYPE, true, false, null)));
  }

  @Test
  public void testGetColumnReturnsMatchingColumn() {
    Table table = people();
    assert table.getColumn("id").name().equals("id");
    assert table.getColumn("name").name().equals("name");
    assertThrows(RuntimeException.class, () -> table.getColumn("primary_key"));
  }

  @Test
  public void testDuplicatePrimaryKeyRejected() {
    Table table = people();
    table.insert(List.of("1", "Alice"));
    var e = assertThrows(RuntimeException.class, () -> table.insert(List.of("1", "Bob")));
    assert e.getMessage().startsWith("Duplicate primary key");
    assert table.getRowCol("1", "name").equals("Alice");
  }

  @Test
  public void testCompositePrimaryKeysDoNotCollide() {
    Table table =
        new Table(
            "pairs",
            List.of(
                new Column("a", TokenType.STRING_TYPE, false, true, null),
                new Column("b", TokenType.STRING_TYPE, false, true, null)));
    table.insert(List.of("x", "y"));
    table.insert(List.of("y", "x"));
    table.insert(List.of("xy", ""));
    assert table.rowCount() == 3;
  }

  @Test
  public void testNullPrimaryKeyRejected() {
    Table table = people();
    assertThrows(RuntimeException.class, () -> table.insert(Arrays.asList(null, "Alice")));
  }

  @Test
  public void testSelectStarAndNulls() {
    Table table = people();
    table.insert(Arrays.asList("1", null));
    table.insert(List.of("2", "Bob"));
    var rows = table.select(List.of("*"), null);
    assert rows.size() == 2;
    assert rows.get(0).equals(Arrays.asList("1", null));
    assert rows.get(1).equals(List.of("2", "Bob"));
    assert table.select(List.of("*"), 1).size() == 1;
  }
}
