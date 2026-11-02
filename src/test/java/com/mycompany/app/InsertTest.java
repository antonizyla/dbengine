package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InsertTest {

  private Database db;

  @BeforeEach
  public void setUp() {
    db = new Database("db", "unused.db");
    db.runQuery(
        "create table users (id number primary key, name string not null, nickname string);");
  }

  private void assertInsertFails(String sql, String message) {
    int before = db.getTable("users").rowCount();
    var e = assertThrows(RuntimeException.class, () -> db.runQuery(sql));
    assertEquals(message, e.getMessage());
    assertEquals(before, db.getTable("users").rowCount(), "failed insert must not store a row");
  }

  private List<List<String>> users() {
    return db.getTable("users").select(List.of("*"), null);
  }

  @Test
  public void testInsertNamedColumns() {
    assert db.runQuery("insert into users (id, name, nickname) values (1, \"Alice\", ali);");
    assertEquals(List.of(List.of("1", "Alice", "ali")), users());
  }

  @Test
  public void testInsertColumnsInAnyOrder() {
    db.runQuery("insert into users (name, id) values (\"Bob\", 2);");
    assertEquals(List.of(Arrays.asList("2", "Bob", null)), users());
  }

  @Test
  public void testInsertAllColumnsImplicitlyAndWithStar() {
    db.runQuery("insert into users values (1, \"Alice\", NULL);");
    db.runQuery("insert into users * values (2, \"Bob\", bobby);");
    assertEquals(List.of(Arrays.asList("1", "Alice", null), List.of("2", "Bob", "bobby")), users());
  }

  @Test
  public void testDecimalNumbersKeepFraction() {
    db.runQuery("create table prices (id number primary key, amount number);");
    db.runQuery("insert into prices values (1, 9.99);");
    db.runQuery("insert into prices values (2, -4);");
    assertEquals(
        List.of(List.of("1", "9.99"), List.of("2", "-4")),
        db.getTable("prices").select(List.of("*"), null));
  }

  @Test
  public void testInsertMarksDatabaseUncommitted(@TempDir Path dir) {
    db = new Database("db", dir.resolve("dirty.db").toString());
    db.runQuery("create table users (id number primary key, name string not null);");
    db.commit();
    assert !db.hasUncommittedChanges();
    db.runQuery("insert into users (id, name) values (1, \"Alice\");");
    assert db.hasUncommittedChanges();
  }

  @Test
  public void testUnknownTable() {
    var e =
        assertThrows(
            RuntimeException.class, () -> db.runQuery("insert into nobody (id) values (1);"));
    assertEquals("Table nobody does not exist.", e.getMessage());
  }

  @Test
  public void testUnknownColumn() {
    assertInsertFails(
        "insert into users (id, age) values (1, 3);", "Table users does not contain column age");
  }

  @Test
  public void testDuplicateColumn() {
    assertInsertFails(
        "insert into users (id, id, name) values (1, 2, a);",
        "Column id is specified more than once");
  }

  @Test
  public void testEmptyColumnList() {
    assertInsertFails("insert into users () values (1);", "No columns specified to insert into");
  }

  @Test
  public void testValueCountMismatch() {
    assertInsertFails(
        "insert into users (id, name) values (1);",
        "2 columns were specified but 1 values were given");
    assertInsertFails(
        "insert into users values (1, a);", "3 columns were specified but 2 values were given");
  }

  @Test
  public void testNotNullColumnMissing() {
    assertInsertFails("insert into users (id) values (1);", "Column name cannot be NULL");
  }

  @Test
  public void testNotNullColumnGivenNull() {
    assertInsertFails(
        "insert into users (id, name) values (1, NULL);", "Column name cannot be NULL");
  }

  @Test
  public void testPrimaryKeyCannotBeNull() {
    assertInsertFails(
        "insert into users (id, name) values (NULL, \"Alice\");", "Column id cannot be NULL");
  }

  @Test
  public void testTypeMismatch() {
    assertInsertFails(
        "insert into users (id, name) values (\"one\", \"Alice\");",
        "Type mismatch: column id expects a NUMBER but got 'one'");
    assertInsertFails(
        "insert into users (id, name) values (1, 5);",
        "Type mismatch: column name expects a STRING but got 5.0");
  }

  @Test
  public void testDuplicatePrimaryKey() {
    db.runQuery("insert into users (id, name) values (1, \"Alice\");");
    assertInsertFails(
        "insert into users (id, name) values (1, \"Bob\");",
        "Duplicate primary key 1 in table users");
    assertEquals("Alice", db.getTable("users").getRowCol("1", "name"));
  }

  @Test
  public void testForeignKeys() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    db.runQuery("insert into users (id, name) values (1, \"Alice\");");

    db.runQuery("insert into pets values (10, 1);");
    assertEquals(1, db.getTable("pets").rowCount());

    var e =
        assertThrows(RuntimeException.class, () -> db.runQuery("insert into pets values (11, 2);"));
    assertEquals("Foreign key violation: 2 does not exist in users.id", e.getMessage());
    assertEquals(1, db.getTable("pets").rowCount());
  }

  @Test
  public void testForeignKeyToMissingTableRejectedOnCreate() {
    var e =
        assertThrows(
            RuntimeException.class,
            () ->
                db.runQuery(
                    "create table pets (id number primary key, owner number references"
                        + " ghosts(id));"));
    assertEquals(
        "Foreign key `ghosts.id` references table ghosts which does not exist", e.getMessage());
  }

  @Test
  public void testInsertedRowsSurviveCommitAndReload(@TempDir Path dir) {
    String path = dir.resolve("persist.db").toString();
    Database persisted = new Database("persist", path);
    persisted.runQuery("create table users (id number primary key, name string);");
    persisted.runQuery("insert into users values (1, \"Alice\");");
    persisted.runQuery("insert into users (id) values (2);");
    persisted.commit();
    assert !persisted.hasUncommittedChanges();

    Database reloaded = Disk.readDatabase(path);
    assertEquals(
        List.of(List.of("1", "Alice"), Arrays.asList("2", null)),
        reloaded.getTable("users").select(List.of("*"), null));

    // the reloaded database keeps enforcing constraints
    assertThrows(
        RuntimeException.class, () -> reloaded.runQuery("insert into users values (1, \"x\");"));
  }
}
