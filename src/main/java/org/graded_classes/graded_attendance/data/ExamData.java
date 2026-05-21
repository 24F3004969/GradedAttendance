package org.graded_classes.graded_attendance.data;

public record ExamData(String id,
                       String classes, String doe,
                       String room,
                       String subject,
                       String time,
                       String topic) {
}
