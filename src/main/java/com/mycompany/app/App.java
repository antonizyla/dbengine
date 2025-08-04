package com.mycompany.app;

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

    Engine e = new Engine();
    if (args.length != 0) {
      for (String dbName : args) {
        e.loadDatabase(dbName);
      }
    }
    e.evalLoop();
  }
}
