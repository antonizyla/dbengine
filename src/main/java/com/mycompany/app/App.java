/**
 * Entry point of the Application
 */
package com.mycompany.app;

import java.util.List;

public class App {
    /**
     * Entry point of the Program when ran as a CLI application.
     *
     * @param args
     *            command line arguments
     */
    public static void main(String[] args) {
        
        Database d = new Database("testing", "./testingdbfile");

        d.createTable(List.of(
                new Column[] { new Column("username", "", false, true, ""),
                new Column("password", "", false, false, "")}
            ), "Users");
        
        d.getTable("Users").insert(List.of("Liam Crossley", "passowrd"));
        d.getTable("Users").printData();
        
        Disk.writeDatabase(d);

        var e = Disk.readDatabase("testingdbfile");

        e.getTable("Users").printData();

    }

    /**
     * Prints a primitive String Represnetation of a table.
     *
     * @param t
     *            table object to to be printed
     */
    public static void print_table(Table t) {
        System.out.printf("Printing Table: %s%n", t.getName());
        t.printHeader();
        // t.select(List.of(new String[]{"*"}), 0);
        t.printData();
    }
}
