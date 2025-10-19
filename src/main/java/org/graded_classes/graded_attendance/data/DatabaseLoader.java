package org.graded_classes.graded_attendance.data;

import javafx.scene.control.Alert;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseLoader {
    private String root_path;
    private Connection connection;
    String name;

    public DatabaseLoader(String root_path, String name) {
        this.root_path = root_path;
        this.name = name;
        if (new File(System.getProperty("user.home") + "\\" + name + ".db").exists()) {
            this.root_path = System.getProperty("user.home") + "\\";
        }
        init();
    }

    public DatabaseLoader(String name) {
        this("G:/My Drive/", name);
    }

    private void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + root_path + this.name + ".db");
            System.out.println("Opened database successfully");
        } catch (SQLException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("You are not connected to the database please contact admin\nat graded");
            alert.showAndWait();
        }
    }


    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
