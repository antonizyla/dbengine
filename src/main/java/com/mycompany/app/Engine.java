package com.mycompany.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** An abstraction of the entire database engine in a single point. */
public class Engine implements Serializable {

  List<String> databases;
  Database current;

  Engine() {
    // Initialise with a default database
    databases = new ArrayList<>();
    current = null;
  }

  /** Run the Evaluation loop of the main database engine entry point. */
  public void evalLoop() {

    Scanner in = new Scanner(System.in);
    String input = "";

    while (!input.equals(".exit")) {

      if (current == null) {
        System.out.print("dbengine> ");
      } else {
        System.out.print(current.getName() + ">>> ");
      }
      input = in.nextLine().trim();

      if (!input.equals(".exit")) {
        // Process the command
        if (current == null) {
          // the current database is null so in engine mode
          com.mycompany.app.Scanner scanner = new com.mycompany.app.Scanner(input);

          List<Token> tokens = scanner.scanTokens();

          Parser parser = new Parser(tokens);

          Expr.EngineExpr command = parser.parseEngineExpr();
          String dbName = command.database;

          if (command.create && !command.drop) {
            createDatabase(dbName);
          } else if (command.drop && !command.create) {
            dropDatabase(dbName);
          } else if (!command.create && !command.drop) {
            enterDatabase(dbName);
          } else {
            System.out.println("Unknown command in engine mode.");
          }
        } else {
          // Operating on the current database
          try {
            current.runQuery(input, false);
          } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
          }
        }
      }
    }

    in.close();
    System.out.println(input + " - Exiting dbengine.");
  }

  /**
   * Load an existing databse file into the engine.
   *
   * @param databaseName filename of file
   */
  public void loadDatabase(String databaseName) {
    // Load the database from disk and set it as current
    databases.add(databaseName);
    // check if provided file has .db at end, if not add it
    if (!databaseName.endsWith(".db")) {
      databaseName += ".db";
    }
    current = Disk.readDatabase(databaseName);
    current = null;
  }

  private void createDatabase(String databaseName) {
    // Create a new database and add it to the list
    databases.add(databaseName);
    Database newDb = new Database(databaseName, databaseName + ".db");
    Disk.writeDatabase(newDb);
    System.out.println("Database " + databaseName + " created.");
  }

  private void dropDatabase(String databaseName) {
    // Find and remove the database from the list
    databases.removeIf(db -> db.equals(databaseName));
    if (current.getName().equals(databaseName)) {
      current = null; // Clear current if it was the one being dropped
      Disk.deleteDatabase(databaseName);
    }
    System.out.println("Database " + databaseName + " dropped.");
  }

  private void enterDatabase(String databaseName) {
    // Find the database and set it as the current database
    for (String db : databases) {
      if (db.equals(databaseName)) {
        current = Disk.readDatabase(String.format("%s.db", databaseName));
        System.out.println("Entered database " + databaseName + ".");
        return;
      }
    }
    System.out.println("Database " + databaseName + " not found.");
  }
}
