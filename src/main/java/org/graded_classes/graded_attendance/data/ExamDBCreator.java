package org.graded_classes.graded_attendance.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.io.File;

public class ExamDBCreator {

    public static void createDatabasesWithTable(int numOfDBs, String folder) {

        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 1; i <= numOfDBs; i++) {

            String dbPath = folder + "/" + (i <= 9 ? ("ED0" + i) : ("ED" + i)) + ".db";
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                continue;
            }
            String url = "jdbc:sqlite:" + dbPath;
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                String createTableSQL = """
                          CREATE TABLE IF NOT EXISTS answers (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            exam_id INTEGER NOT NULL,
                            question_id INTEGER NOT NULL,
                            selected_option_id INTEGER NOT NULL,
                            time_slot TEXT NOT NULL,
                            start_time DATETIME NOT NULL,
                            end_time DATETIME NOT NULL,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                        );
                        """;

                stmt.execute(createTableSQL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        createDatabasesWithTable(90, "G:/My Drive/GradeEd_Exam_2026");
    }
}