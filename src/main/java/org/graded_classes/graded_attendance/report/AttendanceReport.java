package org.graded_classes.graded_attendance.report;

import org.graded_classes.graded_attendance.data.DatabaseLoader;
import org.graded_classes.graded_attendance.data.SqlFileReader;

import java.sql.SQLException;
import java.util.LinkedHashMap;

public class AttendanceReport {
    public LinkedHashMap<String, String> getStudentAttendanceReport() {
        return studentAttendanceReport;
    }

    LinkedHashMap<String, String> studentAttendanceReport = new LinkedHashMap<>();
    public DatabaseLoader databaseLoader;

    public AttendanceReport(DatabaseLoader databaseLoader) {
        this.databaseLoader = databaseLoader;
        init();
    }

    public void init() {
        try {
            var connection = databaseLoader.getStatement().getConnection();
            var sql = new SqlFileReader("data/attendance_data_report.sql").getQuery();

            try (var pst = connection.prepareStatement(sql);
                 var r = pst.executeQuery()) {

                while (r.next()) {
                    studentAttendanceReport.put(r.getString("ed_no"),
                            r.getString("attendance_count"));
                }

            } catch (SQLException e) {
                System.err.println("SQL execution error: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
