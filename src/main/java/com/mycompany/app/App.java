package com.mycompany.app;

import java.util.List;

public class App {
    public static void main(String[] args) {
        Table t = new Table("table1", List.of(new Column[] { new Column("username", "", false, true, ""),
                new Column("password", "", false, false, "") }));
        t.insert(List.of("col1", "col2"));
        print_table(t);

        System.out.println("");

        Table t2 = new Table("table1", List.of(new Column[] { new Column("username", "", false, true, ""),
                new Column("password", "", false, false, "") }));
        t2.insert(List.of("John smith", "Password1"));
        t2.insert(List.of("Liam", "NotPassword12"));
        print_table(t2);

        System.out.println(t2.select(List.of("username"), 2).get(0));

    }

    public static void print_table(Table t) {
        System.out.printf("Printing Table: %s%n", t.getName());
        t.printHeader();
        // t.select(List.of(new String[]{"*"}), 0);
        t.printData();
    }
}
