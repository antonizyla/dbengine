package com.mycompany.app;

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

      System.out.println("[DISK OPERATION] Read Database ");
      objectInputStream.close();
      return retrieved;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Write the specified database object to Disk.
   *
   * @param db the database in memory
   */
  public static void writeDatabase(Database db) {
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(db.getFilePath());
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(db);
      objectOutputStream.close();
      System.out.println("[DISK OPERATION] Saved Database");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
