package com.mycompany.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Set of utilities for writing/reading databae from files. */
public class Disk {

  /**
   * Read a database object from disk to memory.
   *
   * @param filepath Filepath of disk db
   * @return the database object to be used by the program
   */
  public static Database readDatabase(String filepath) {
    try {
      ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filepath));
      Database retrieved = (Database) objectInputStream.readObject();
      objectInputStream.close();
      if (retrieved.getFilePath() == null || retrieved.getFilePath().isEmpty()) {
        throw new RuntimeException("[DISK OPERATION] No database found at " + filepath);
      }
      System.out.println("[DISK OPERATION] Read Database ");
      return retrieved;
    } catch (Exception e) {
      throw new RuntimeException("[DISK OPERATION] Failed to read database", e);
    }
  }

  /**
   * Write the specified database object to Disk.
   *
   * @param db the database in memory
   */
  public static void writeDatabase(Database db) {
    try {
      // check to make sure that the if the file exists then it is a database file
      File f = new File(db.getFilePath());
      if (f.exists() && f.isFile()) {
        var fileRead = readDatabase(db.getFilePath());
        if (!fileRead.getName().equals(db.getName())) {
          throw new RuntimeException("The file already exists and is not a database file");
        }
      }
      FileOutputStream fileOutputStream = new FileOutputStream(db.getFilePath());
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(db);
      objectOutputStream.close();
      System.out.println("[DISK OPERATION] Saved Database");
    } catch (Exception e) {
      throw new RuntimeException("[DISK OPERATION] Failed to save database", e);
    }
  }

  public static void deleteDatabase(String filepath) {
    try {
      File f = new File(filepath);
      if (!f.exists() || !f.isFile()) {
        throw new RuntimeException("[DISK OPERATION] No database found at " + filepath);
      }
      if (!f.delete()) {
        throw new RuntimeException(
            "[DISK OPERATION] Failed to delete database at "
                + filepath
                + ". Please check if the file is in use or if you have the necessary permissions.");
      }
    } catch (Exception e) {
      throw new RuntimeException("[DISK OPERATION] Failed to delete database", e);
    }
  }
}
