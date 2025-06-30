package com.mycompany.app;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class FileTest {

  private static List<String> files = new ArrayList<>();

  @AfterAll
  public static void cleanup() {
    // delete all files from each test
    for (String file : files) {
      File f = new File(file);
      if (f.exists()) {
        f.delete();
      }
    }
  }

  @Test
  public void testWritingExistingFile() {
    // expected behaviour is to check that the file is an existing
    // database file. If so then overwrite. If not we need to preserve
    // the file and throw an error.

    // create file and fill with text
    try {
      var f = new File("./database");
      f.createNewFile();
      FileWriter writer = new FileWriter(f);
      writer.write("This is a test file for the database.");
      writer.close();
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    files.add("database");

    // create an empty database
    var db = new Database("Testdatabase", "./database");

    // attempt to write to the database file
    boolean testSuccess = false;
    try {
      Disk.writeDatabase(db);
    } catch (Exception e) {
      testSuccess = true;
      // will only get here if the file exists and cannot be read as a database.
    }
    assert testSuccess;
  }

  @Test
  public void testWriteAndRetrieveNewFile() {
    try {
      // create a new database and write it to disk
      var db = new Database("dbname", "./file");
      db.createTable(List.of(new Column("username", null, false, true, null)), "Users");
      db.getTable("Users").insert(List.of("Liam"));
      Disk.writeDatabase(db);

      // check exists and that it can be read
      var f = new File("./file");
      files.add("./file");
      assert f.exists() && f.isFile();
      var read = Disk.readDatabase("./file");

      // check the read database.
      assert read.getName().equals("dbname");
      assert read.getTable("Users").select(List.of("username"), 1).get(0).get(0).equals("Liam");

    } catch (Exception e) {
      e.printStackTrace();
      assert false : "Test failed due to exception: " + e.getMessage();
    }
  }
}
