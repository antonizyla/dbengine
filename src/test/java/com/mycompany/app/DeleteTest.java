package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteTest {

  private Database db;

  @BeforeEach
  public void setUp() {
    db = new Database("db", "unused.db");
    db.runQuery("create table users (id number primary key, name string);");
    db.runQuery("insert into users values (1, \"Alice\");");
    db.runQuery("insert into users values (2, \"Bob\");");
    db.runQuery("insert into users values (3, \"Bob\");");
  }

  private List<List<String>> rows(String table) {
    return db.getTable(table).select(List.of("*"), null);
  }

  private int count(String table) {
    return db.getTable(table).rowCount();
  }

  /** Assert the statement fails with the message and leaves every listed table unchanged. */
  private void assertDeleteFails(String sql, String message, String... tables) {
    var before = Arrays.stream(tables).map(this::rows).toList();
    var e = assertThrows(RuntimeException.class, () -> db.runQuery(sql));
    assertEquals(message, e.getMessage());
    assertEquals(before, Arrays.stream(tables).map(this::rows).toList());
  }

  // plain deletes

  @Test
  public void testDeleteByPrimaryKey() {
    db.runQuery("delete from users where id = 2;");
    assertEquals(List.of(List.of("1", "Alice"), List.of("3", "Bob")), rows("users"));
  }

  @Test
  public void testDeleteByOtherColumn() {
    db.runQuery("delete from users where name = \"Bob\";");
    assertEquals(List.of(List.of("1", "Alice")), rows("users"));
  }

  @Test
  public void testDeleteWithAnd() {
    db.runQuery("delete from users where name = \"Bob\" and id = 3;");
    assertEquals(2, count("users"));
  }

  @Test
  public void testContradictionDeletesNothing() {
    db.runQuery("delete from users where id = 1 and id = 2;");
    assertEquals(3, count("users"));
  }

  @Test
  public void testDeleteAll() {
    db.runQuery("delete from users;");
    assertEquals(0, count("users"));
  }

  @Test
  public void testDeleteMarksDirty() {
    db.commit();
    db.runQuery("delete from users where id = 99;");
    assert !db.hasUncommittedChanges();
    db.runQuery("delete from users where id = 1;");
    assert db.hasUncommittedChanges();
  }

  @Test
  public void testDeleteErrors() {
    assertDeleteFails("delete from ghosts;", "Table ghosts does not exist.", "users");
    assertDeleteFails(
        "delete from users where age = 1;", "Column age does not exist in table users", "users");
    assertDeleteFails(
        "delete from users where id > 1;",
        "DELETE only supports WHERE conditions of the form column = value joined by AND",
        "users");
    assertDeleteFails(
        "delete from users where id = 1 or id = 2;",
        "DELETE only supports WHERE conditions of the form column = value joined by AND",
        "users");
    assertDeleteFails(
        "delete from users where id = \"x\";",
        "Type mismatch: column id expects a NUMBER but got 'x'",
        "users");
    // a misspelt WHERE must not delete every row
    assertDeleteFails(
        "delete from users wher id = 1;", "Unexpected token 'wher' in DELETE statement", "users");
  }

  // referential actions

  @Test
  public void testNoActionBlocksDelete() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    db.runQuery("insert into pets values (10, 1);");
    assertDeleteFails(
        "delete from users where id = 1;",
        "Cannot delete 1 from users: row 10 of pets still references it through fk_pets_owner (ON"
            + " DELETE NO ACTION)",
        "users",
        "pets");
    db.runQuery("delete from users where id = 2;"); // unreferenced rows can still go
    assertEquals(2, count("users"));
  }

  @Test
  public void testCascadeThroughThreeLevels() {
    db.runQuery(
        "create table orders (id number primary key, buyer number references users(id) on delete"
            + " cascade);");
    db.runQuery(
        "create table items (id number primary key, ord number references orders(id) on delete"
            + " cascade);");
    db.runQuery("insert into orders values (100, 1);");
    db.runQuery("insert into orders values (101, 1);");
    db.runQuery("insert into orders values (200, 2);");
    db.runQuery("insert into items values (1, 100);");
    db.runQuery("insert into items values (2, 101);");
    db.runQuery("insert into items values (3, 200);");

    db.runQuery("delete from users where id = 1;");
    assertEquals(List.of(List.of("200", "2")), rows("orders"));
    assertEquals(List.of(List.of("3", "200")), rows("items"));
    assertEquals(2, count("users"));
  }

  @Test
  public void testRestrictDeepInCascadeBlocksEverything() {
    db.runQuery(
        "create table orders (id number primary key, buyer number references users(id) on delete"
            + " cascade);");
    db.runQuery(
        "create table invoices (id number primary key, ord number references orders(id) on delete"
            + " restrict);");
    db.runQuery("insert into orders values (100, 1);");
    db.runQuery("insert into invoices values (5, 100);");

    assertDeleteFails(
        "delete from users where id = 1;",
        "Cannot delete 100 from orders: row 5 of invoices still references it through"
            + " fk_invoices_ord (ON DELETE RESTRICT)",
        "users",
        "orders",
        "invoices");
  }

  @Test
  public void testRestrictedRowDeletedByAnotherPathIsAllowed() {
    // pets references users twice: owner cascades, vet restricts. Deleting the user removes the
    // pet through owner, so vet no longer blocks
    db.runQuery(
        "create table pets (id number primary key, owner number references users(id) on delete"
            + " cascade, vet number references users(id) on delete restrict);");
    db.runQuery("insert into pets values (10, 1, 1);");
    db.runQuery("delete from users where id = 1;");
    assertEquals(0, count("pets"));
  }

  @Test
  public void testSetNull() {
    db.runQuery(
        "create table pets (id number primary key, owner number references users(id) on delete"
            + " set null);");
    db.runQuery("insert into pets values (10, 1);");
    db.runQuery("insert into pets values (11, 2);");
    db.runQuery("delete from users where id = 1;");
    assertEquals(List.of(Arrays.asList("10", null), List.of("11", "2")), rows("pets"));

    // the row no longer references anything, so deleting more users is unaffected by it
    db.runQuery("delete from users where id = 2;");
    assertEquals(List.of(Arrays.asList("10", null), Arrays.asList("11", null)), rows("pets"));
  }

  @Test
  public void testSetDefault() {
    db.runQuery(
        "create table pets (id number primary key, owner number default 3 references users(id) on"
            + " delete set default);");
    db.runQuery("insert into pets values (10, 1);");
    db.runQuery("delete from users where id = 1;");
    assertEquals(List.of(List.of("10", "3")), rows("pets"));

    // now the default itself would be deleted, so the pet would point at nothing
    assertDeleteFails(
        "delete from users where id = 3;",
        "Cannot delete from users: ON DELETE SET DEFAULT of fk_pets_owner needs 3 to exist in"
            + " users.id",
        "users",
        "pets");
  }

  @Test
  public void testSelfReferenceCascadeDeletesSubtree() {
    db.runQuery(
        "create table staff (id number primary key, boss number references staff(id) on delete"
            + " cascade);");
    db.runQuery("insert into staff values (1, NULL);");
    db.runQuery("insert into staff values (2, 1);");
    db.runQuery("insert into staff values (3, 2);");
    db.runQuery("insert into staff values (4, 1);");
    db.runQuery("insert into staff values (5, NULL);");
    db.runQuery("insert into staff values (6, 6);");

    db.runQuery("delete from staff where id = 2;");
    assertEquals(
        List.of(
            Arrays.asList("1", null),
            List.of("4", "1"),
            Arrays.asList("5", null),
            List.of("6", "6")),
        rows("staff"));
    db.runQuery("delete from staff where id = 1;");
    assertEquals(List.of(Arrays.asList("5", null), List.of("6", "6")), rows("staff"));
    db.runQuery("delete from staff where id = 6;"); // references only itself
    assertEquals(1, count("staff"));
  }

  @Test
  public void testSelfReferenceNoActionBlocks() {
    db.runQuery("create table staff (id number primary key, boss number references staff(id));");
    db.runQuery("insert into staff values (1, NULL);");
    db.runQuery("insert into staff values (2, 1);");
    assertDeleteFails(
        "delete from staff where id = 1;",
        "Cannot delete 1 from staff: row 2 of staff still references it through fk_staff_boss (ON"
            + " DELETE NO ACTION)",
        "staff");
    // deleting both together is fine, nothing is left referencing a missing row
    db.runQuery("delete from staff;");
    assertEquals(0, count("staff"));
  }

  @Test
  public void testCompositeCascade() {
    db.runQuery("create table orders (customer number, num number, primary key (customer, num));");
    db.runQuery(
        "create table lines (id number primary key, c number, n number, foreign key (c, n)"
            + " references orders(customer, num) on delete cascade);");
    db.runQuery("insert into orders values (1, 1);");
    db.runQuery("insert into orders values (1, 2);");
    db.runQuery("insert into lines values (1, 1, 1);");
    db.runQuery("insert into lines values (2, 1, 2);");
    db.runQuery("delete from orders where customer = 1 and num = 2;");
    assertEquals(List.of(List.of("1", "1", "1")), rows("lines"));
  }

  @Test
  public void testDiamondCascadeDeletesRowOnce() {
    // c is reached from a directly and through b
    db.runQuery("create table a (id number primary key);");
    db.runQuery(
        "create table b (id number primary key, a number references a(id) on delete cascade);");
    db.runQuery(
        "create table c (id number primary key, b number references b(id) on delete cascade, a"
            + " number references a(id) on delete cascade);");
    db.runQuery("insert into a values (1);");
    db.runQuery("insert into b values (1, 1);");
    db.runQuery("insert into c values (1, 1, 1);");
    db.runQuery("delete from a where id = 1;");
    assertEquals(0, count("b"));
    assertEquals(0, count("c"));
  }

  @Test
  public void testIndexStaysCorrectAfterDeleteAndReinsert() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    db.runQuery("insert into pets values (10, 1);");
    db.runQuery("delete from pets where id = 10;");
    db.runQuery("delete from users where id = 1;"); // no longer referenced
    db.runQuery("insert into pets values (10, 2);");
    assertThrows(RuntimeException.class, () -> db.runQuery("delete from users where id = 2;"));
  }
}
