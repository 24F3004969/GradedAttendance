package org.graded_classes.graded_attendance.controller.home;

import org.graded_classes.graded_attendance.data.DailyTopics;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DailyTopicsDao {

    private static final List<String> CLASSES =
            Arrays.asList("IV", "V", "VI", "VII", "VIII", "IX", "X");

    public static TreeMap<String, DailyTopics> loadForDateAllClasses(
            Connection connection, LocalDate date) throws SQLException {

        TreeMap<String, DailyTopics> result = new TreeMap<>();
        for (String cls : CLASSES) {
            DailyTopics empty = new DailyTopics();
            empty.setDate(date.toString());
            empty.setClassName(cls);
            result.put(cls, empty);
        }


        String sql =
                "SELECT date, \"class\", subject1, topic1, subject2, topic2, " +
                        "       subject3, topic3, subject4, topic4 " +
                        "FROM DailyTopics " +
                        "WHERE date = ? AND \"class\" IN (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, date.toString()); // <-- LocalDate correctly handled

            for (String cls : CLASSES) {
                ps.setString(idx++, cls);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DailyTopics dt = mapRow(rs);
                    result.put(dt.getClassName(), dt);  // overwrite default entry
                }
            }
        }

        return result;
    }

    private static DailyTopics mapRow(ResultSet rs) throws SQLException {
        DailyTopics dt = new DailyTopics();
        dt.setDate(rs.getString("date"));
        dt.setClassName(rs.getString("class"));
        dt.setSubject1(rs.getString("subject1"));
        dt.setTopic1(rs.getString("topic1"));
        dt.setSubject2(rs.getString("subject2"));
        dt.setTopic2(rs.getString("topic2"));
        dt.setSubject3(rs.getString("subject3"));
        dt.setTopic3(rs.getString("topic3"));
        dt.setSubject4(rs.getString("subject4"));
        dt.setTopic4(rs.getString("topic4"));
        return dt;
    }
}