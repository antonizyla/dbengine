package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ForeignKeyTest {

  private Database db;

  @BeforeEach
  public void setUp() {
    db = new Database("db", "unused.db");
    db.runQuery("create table users (id number primary key, name string);");
  }

  private void assertFails(String sql, String message) {
    var e = assertThrows(RuntimeException.class, () -> db.runQuery(sql));
    assertEquals(message, e.getMessage());
  }

  private List<List<String>> rows(String table) {
    return db.getTable(table).select(List.of("*"), null);
  }

  @Test
  public void testColumnLevelForeignKeyIsStored() {
    db.runQuery(
        "create table pets (id number primary key, owner number references users(id) on delete"
            + " cascade);");
    ForeignKey fk = db.getTable("pets").getForeignKeys().get(0);
    assertEquals("fk_pets_owner", fk.name());
    assertEquals(List.of("owner"), fk.childColumns());
    assertEquals("users", fk.parentTable());
    assertEquals(List.of("id"), fk.parentColumns());
    assertEquals(ReferentialAction.CASCADE, fk.onDelete());
  }

  @Test
  public void testDefaultActionIsNoAction() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    assertEquals(
        ReferentialAction.NO_ACTION, db.getTable("pets").getForeignKeys().get(0).onDelete());
  }

  @Test
  public void testParsesEveryAction() {
    var cases =
        List.of(
            List.of("cascade", ReferentialAction.CASCADE),
            List.of("restrict", ReferentialAction.RESTRICT),
            List.of("no action", ReferentialAction.NO_ACTION),
            List.of("set null", ReferentialAction.SET_NULL));
    for (var c : cases) {
      Column col =
          new Parser(
                  new Scanner("owner number references users(id) on delete " + c.get(0))
                      .scanTokens())
              .column();
      assertEquals(c.get(1), col.onDelete());
    }
  }

  @Test
  public void testForeignKeyMustReferencePrimaryKey() {
    assertFails(
        "create table pets (id number primary key, owner string references users(name));",
        "Foreign key `users.name` must reference the primary key of users (id)");
  }

  @Test
  public void testForeignKeyToMissingColumn() {
    assertFails(
        "create table pets (id number primary key, owner number references users(nme));",
        "Foreign key `users.nme` does not exist in table users");
  }

  @Test
  public void testForeignKeyTypesMustMatch() {
    assertFails(
        "create table pets (id number primary key, owner string references users(id));",
        "Foreign key column owner is a STRING but users.id is a NUMBER");
  }

  @Test
  public void testOnDeleteWithoutReferences() {
    assertFails(
        "create table pets (id number primary key on delete cascade);",
        "ON DELETE requires REFERENCES on column id");
  }

  @Test
  public void testOnUpdateNotSupported() {
    assertFails(
        "create table pets (id number primary key, owner number references users(id) on update"
            + " cascade);",
        "ON UPDATE is not supported yet");
  }

  @Test
  public void testSetNullNeedsNullableColumn() {
    assertFails(
        "create table pets (id number primary key, owner number not null references users(id) on"
            + " delete set null);",
        "ON DELETE SET NULL needs column owner to allow NULL");
  }

  @Test
  public void testSetDefaultNeedsDefault() {
    assertFails(
        "create table pets (id number primary key, owner number references users(id) on delete"
            + " set default);",
        "ON DELETE SET DEFAULT needs a DEFAULT on non key column owner");
  }

  @Test
  public void testDefaultTypeIsChecked() {
    assertFails(
        "create table pets (id number primary key, owner number default \"x\");",
        "Type mismatch: column owner expects a NUMBER but got 'x'");
  }

  @Test
  public void testInsertUsesDefaultForMissingColumns() {
    db.runQuery("create table pets (id number primary key, legs number default 4);");
    db.runQuery("insert into pets (id) values (1);");
    db.runQuery("insert into pets values (2, NULL);");
    assertEquals(List.of(List.of("1", "4"), Arrays.asList("2", null)), rows("pets"));
  }

  @Test
  public void testInsertNullForeignKeyIsAllowed() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    db.runQuery("insert into pets (id) values (1);");
    assertNull(db.getTable("pets").getRowCol("1", "owner"));
  }

  @Test
  public void testCompositeForeignKey() {
    db.runQuery(
        "create table orders (customer number, num number, total number, primary key (customer,"
            + " num));");
    db.runQuery(
        "create table lines (id number primary key, c number, n number, foreign key (n, c)"
            + " references orders(num, customer) on delete cascade);");
    ForeignKey fk = db.getTable("lines").getForeignKeys().get(0);
    // stored in the order of the parent's primary key
    assertEquals(List.of("c", "n"), fk.childColumns());
    assertEquals(List.of("customer", "num"), fk.parentColumns());

    db.runQuery("insert into orders values (1, 7, 100);");
    db.runQuery("insert into lines values (1, 1, 7);");
    assertFails(
        "insert into lines values (2, 7, 1);",
        "Foreign key violation: 7, 1 does not exist in orders(customer, num)");
    assertEquals(1, db.getTable("lines").rowCount());
  }

  @Test
  public void testCompositeForeignKeyMustCoverWholeKey() {
    db.runQuery("create table orders (customer number, num number, primary key (customer, num));");
    assertFails(
        "create table lines (id number primary key, c number references orders(customer));",
        "Foreign key `orders.customer` must reference the primary key of orders (customer, num)");
  }

  @Test
  public void testTablePrimaryKeyClause() {
    db.runQuery("create table pairs (a string, b string, primary key (a, b));");
    Table pairs = db.getTable("pairs");
    assertEquals(List.of("a", "b"), pairs.getPrimaryKeyColumns());
    assert !pairs.getColumn("a").nullable();
  }

  @Test
  public void testTablePrimaryKeyUnknownColumn() {
    assertFails(
        "create table pairs (a string, primary key (a, b));",
        "PRIMARY KEY column b does not exist in table pairs");
  }

  @Test
  public void testSelfReferenceInsert() {
    db.runQuery("create table staff (id number primary key, boss number references staff(id));");
    db.runQuery("insert into staff values (1, NULL);");
    db.runQuery("insert into staff values (2, 1);");
    db.runQuery("insert into staff values (3, 3);"); // a row may reference itself
    assertFails(
        "insert into staff values (4, 9);", "Foreign key violation: 9 does not exist in staff.id");
  }

  @Test
  public void testDropReferencedTableRefused() {
    db.runQuery("create table pets (id number primary key, owner number references users(id));");
    assertFails(
        "drop table users;",
        "Cannot drop table users: it is referenced by fk_pets_owner in table pets");
    db.runQuery("drop table pets;");
    db.runQuery("drop table users;");
    assertNull(db.getTable("users"));
  }

  @Test
  public void testDropSelfReferencingTable() {
    db.runQuery("create table staff (id number primary key, boss number references staff(id));");
    db.runQuery("drop table staff;");
    assertNull(db.getTable("staff"));
  }

  @Test
  public void testConstraintsSurviveReload(@TempDir Path dir) {
    String path = dir.resolve("fk.db").toString();
    Database persisted = new Database("fk", path);
    persisted.runQuery("create table users (id number primary key);");
    persisted.runQuery(
        "create table pets (id number primary key, owner number references users(id) on delete"
            + " cascade);");
    persisted.runQuery("insert into users values (1);");
    persisted.runQuery("insert into pets values (10, 1);");
    persisted.commit();

    Database reloaded = Disk.readDatabase(path);
    assertThrows(
        RuntimeException.class, () -> reloaded.runQuery("insert into pets values (11, 2);"));
    reloaded.runQuery("delete from users where id = 1;");
    assertEquals(0, reloaded.getTable("pets").rowCount());
  }
}
