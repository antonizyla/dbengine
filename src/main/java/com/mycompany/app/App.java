package com.mycompany.app;

import java.util.Scanner;

/** Entry of the CLI Application. */
public class App {
  /**
   * Entry point of the Program when ran as a CLI application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    // create infitie loop to read commands
    System.out.println("--Welcome to the dbengine!--");

    System.out.println("Type '.exit' to exit the program.");

    Database db = new Database("testdb", "testdb.db");

    Scanner input = new Scanner(System.in);
    String command = "";
    while (!command.equals(".exit")) {
      System.out.print("dbengine> ");
      command = input.nextLine().trim();
      if (!command.equals(".exit")) {
        db.runQuery(command, true);
      }
    }
    input.close();
  }
}
