package org.graded_classes.graded_attendance.controller.leaderboard;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseLoader {
    private final String root_path;
    private Connection connection;
    String name;

   public DatabaseLoader(String root_path, String name) {
        this.root_path = root_path;
        this.name = name;
        init();
    }

    private void init() {
        var done = create(root_path);
        if (done) {
            try {
                System.out.println("jdbc:sqlite:" + root_path + this.name);
                connection = DriverManager.getConnection("jdbc:sqlite:" + root_path + this.name);
                System.out.println("Opened database successfully");
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static boolean create(String root_path) {
        if (!new File(root_path).exists())
            return new File(root_path).mkdirs();
        return true;
    }

    Connection getConnection() {
        return connection;
    }

    Statement getStatement() {
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
