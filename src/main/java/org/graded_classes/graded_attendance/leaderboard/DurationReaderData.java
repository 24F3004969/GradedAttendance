package org.graded_classes.graded_attendance.leaderboard;

import org.graded_classes.graded_attendance.controller.MainController;

import java.sql.*;
import java.util.ArrayList;


public class DurationReaderData {
    MainController mainController;
    LeaderboardLoader leaderboardLoader;

    public DurationReaderData(MainController mainController, LeaderboardLoader loader) {
        this.mainController = mainController;
        this.leaderboardLoader = loader;
    }

    public void init() {
        try {
            if (!tableExists(mainController.gradedDataLoader.databaseLoader.getConnection())) {
                leaderboardLoader.generateDefaultAnimationDuration();
                try (Statement stmt = mainController.gradedDataLoader.databaseLoader.getConnection().createStatement()) {
                    String sql = "CREATE TABLE IF NOT EXISTS durations (" +
                            "key TEXT PRIMARY KEY, " +
                            "layout_duration DOUBLE, " +
                            "fade_time DOUBLE)";
                    stmt.execute(sql);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                updateDurationInDatabase();
            } else {
                durationReader();
                updateDurationInDatabase();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDurationInDatabase() {
        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO durations (key, layout_duration, fade_time) VALUES (?, ?, ?)");

            AnimationDuration lastDuration = leaderboardLoader.defaultAnimationDuration.isEmpty()
                    ? new AnimationDuration(0.0, 0.0)
                    : leaderboardLoader.defaultAnimationDuration.lastEntry().getValue();

            for (String key : leaderboardLoader.preview) {
                AnimationDuration duration = leaderboardLoader.defaultAnimationDuration.getOrDefault(key, lastDuration);
                pstmt.setString(1, key);
                pstmt.setDouble(2, duration.getLayoutDuration());
                pstmt.setDouble(3, duration.getFadeTime());
                pstmt.executeUpdate();

                if (!leaderboardLoader.defaultAnimationDuration.containsKey(key)) {
                    leaderboardLoader.defaultAnimationDuration.put(key, new AnimationDuration(
                            lastDuration.getLayoutDuration(),
                            lastDuration.getFadeTime()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating database: " + e.getMessage(), e.getCause());
        }
    }

    public void durationReader() {
        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT key, layout_duration, fade_time FROM durations");
            // Collect keys to delete (those not in preview)
            ArrayList<String> keysToDelete = new ArrayList<>();
            while (rs.next()) {
                String key = rs.getString("key");
                if (leaderboardLoader.preview.contains(key)) {
                    leaderboardLoader.defaultAnimationDuration.put(key, new AnimationDuration(
                            rs.getDouble("layout_duration"),
                            rs.getDouble("fade_time")));
                } else {
                    keysToDelete.add(key);
                }

            }
            // Delete keys that are not in preview
            try {
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM durations WHERE key = ?");
                for (String key : keysToDelete) {
                    pstmt.setString(1, key);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException e) {

            // Silently ignore if database is not found or empty, matching original behavior
        }
    }

    // Check if a table exists in the SQLite database
    private boolean tableExists(Connection conn) throws SQLException {
        try {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?");
            pstmt.setString(1, "durations");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}