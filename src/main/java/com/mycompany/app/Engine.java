package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Engine implements Serializable {

  List<String> databases;
  Database current;

  Engine() {
    // Initialise with a default database
    databases = new ArrayList<>();
    current = null;
  }

  public void evalLoop() {

    Scanner input = new Scanner(System.in);
    String command = "";

    while (!command.equals(".exit")) {
      System.out.print("dbengine>>> ");
      command = input.nextLine().trim();
      if (!command.equals(".exit")) {
        // Process the command
        if (current == null) {
          // the current database is null so in engine mode
          com.mycompany.app.Scanner scanner = new com.mycompany.app.Scanner(command);
          List<Token> tokens = scanner.scanTokens();
          Parser parser = new Parser(tokens);
          Expr.EngineExpr engineCommand = parser.parseEngineExpr();
          String dbName = engineCommand.database;
          if (engineCommand.create && !engineCommand.drop) {
            createDatabase(dbName);
          } else if (engineCommand.drop && !engineCommand.create) {
            dropDatabase(dbName);
          } else if (!engineCommand.create && !engineCommand.drop) {
            enterDatabase(dbName);
          } else {
            System.out.println("Unknown command in engine mode.");
          }
        } else {
          // Operating on the current database
          current.runQuery(command, true);
        }
      }
    }
  }

  private void createDatabase(String dbName) {
    // Create a new database and add it to the list
    databases.add(dbName);
    Database newDb = new Database(dbName, dbName + ".db");
    Disk.writeDatabase(newDb);
    System.out.println("Database " + dbName + " created.");
  }

  private void dropDatabase(String dbName) {
    // Find and remove the database from the list
    databases.removeIf(db -> db.equals(dbName));
    if (current.getName().equals(dbName)) {
      current = null; // Clear current if it was the one being dropped
      Disk.deleteDatabase(dbName);
    }
    System.out.println("Database " + dbName + " dropped.");
  }

  private void enterDatabase(String dbName) {
    // Find the database and set it as the current database
    for (String db : databases) {
      if (db.equals(dbName)) {
        current = Disk.readDatabase(dbName);
        System.out.println("Entered database " + dbName + ".");
        return;
      }
    }
    System.out.println("Database " + dbName + " not found.");
  }
}
