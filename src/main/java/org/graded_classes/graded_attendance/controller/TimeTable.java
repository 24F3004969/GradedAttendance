package org.graded_classes.graded_attendance.controller;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimeTable {
    private String grade;
    final Map<String, Map<String, Object>> table;
    Connection connection;

    public TimeTable(String grade, Connection connection) {
        this.grade = grade;
        this.connection = connection;
        table = readTimeTable();
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void updateTimeSlot(String day, String timeSlot, String subject) {
        String sql = String.format("UPDATE \"TimeTable%s\" SET [%s] = ? WHERE Day = ?", grade, timeSlot);
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, subject);
            pst.setString(2, day);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Map<String, Object>> getTable() {
        return table;
    }

    public Map<String, Map<String, Object>> readTimeTable() {
        Map<String, Map<String, Object>> timeTable = new LinkedHashMap<>();
        String sql = String.format("SELECT * FROM \"TimeTable%s\"", grade);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String day = rs.getString("Day");
                Map<String, Object> slots = new LinkedHashMap<>();
                slots.put("Day", day);
                slots.put("3:30 PM", getIfPresent(rs.getString("3:30 PM")));
                slots.put("4:15 PM", getIfPresent(rs.getString("4:15 PM")));
                slots.put("5:00 PM", getIfPresent(rs.getString("5:00 PM")));
                slots.put("5:45 PM", getIfPresent(rs.getString("5:45 PM")));
                slots.put("6:30 PM", getIfPresent(rs.getString("6:30 PM")));
                slots.put("7:15 PM", getIfPresent(rs.getString("7:15 PM")));

                timeTable.put(day, slots);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return timeTable;
    }

    private Object getIfPresent(String string) {
        return string == null ? "" : string;
    }

    public void insertDailyTopics(
            String className,
            String subject1, String topic1,
            String subject2, String topic2,
            String subject3, String topic3,
            String subject4, String topic4) {
        final LocalDate date = LocalDate.now();
        final String sql = "INSERT INTO DailyTopics " +
                "(date, class, subject1, topic1, subject2, topic2, subject3, topic3, subject4, topic4) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {

            pst.setString(1, date.toString());
            pst.setString(2, className);
            pst.setString(3, subject1);
            pst.setString(4, topic1);
            pst.setString(5, subject2);
            pst.setString(6, topic2);
            pst.setString(7, subject3);
            pst.setString(8, topic3);
            pst.setString(9, subject4);
            pst.setString(10, topic4);

            int rowsInserted = pst.executeUpdate();

        } catch (SQLException e) {
            String msg = e.getMessage();
            System.err.println("Insert failed: " + msg);
        }
    }

    public boolean updateDailyTopics(
            LocalDate date,
            String className,
            String subject1, String topic1,
            String subject2, String topic2,
            String subject3, String topic3,
            String subject4, String topic4) {

        final String sql = "UPDATE DailyTopics SET " +
                "subject1 = ?, topic1 = ?, " +
                "subject2 = ?, topic2 = ?, " +
                "subject3 = ?, topic3 = ?, " +
                "subject4 = ?, topic4 = ? " +
                "WHERE date = ? AND class = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, subject1);
            pst.setString(2, topic1);
            pst.setString(3, subject2);
            pst.setString(4, topic2);
            pst.setString(5, subject3);
            pst.setString(6, topic3);
            pst.setString(7, subject4);
            pst.setString(8, topic4);

            pst.setDate(9, java.sql.Date.valueOf(date));
            pst.setString(10, className);

            int rowsUpdated = pst.executeUpdate();
            return rowsUpdated > 0; // true if a row matched and was updated
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
            return false;
        }
    }

}
