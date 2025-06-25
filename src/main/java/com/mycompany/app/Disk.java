package com.mycompany.app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Disk {

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
