package org.graded_classes.graded_attendance.controller.leaderboard;



import java.util.Objects;

public record DailyTimeTable(String time, String grade, String roomNo, String subject, String teacherName) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DailyTimeTable that = (DailyTimeTable) o;
        return Objects.equals(time, that.time) && Objects.equals(grade, that.grade) && Objects.equals(roomNo, that.roomNo) && Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, grade, roomNo, subject);
    }
}
