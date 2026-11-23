package org.graded_classes.graded_attendance.data;

import javafx.beans.property.BooleanProperty;

import java.util.List;

public record ExamData(
        String id,
        BooleanProperty active,
        String classes,
        String doe,
        String room,
        String subject,
        String time,
        List<Integer> topicIds,
        List<String> topicNames,
        String board,
        ExamType examType

) {
    public ExamData {
        // Prevent callers from modifying the lists stored in this record.
        topicIds = topicIds == null ? List.of() : List.copyOf(topicIds);
        topicNames = topicNames == null ? List.of() : List.copyOf(topicNames);
    }


    /**
     * Useful when displaying all topic names in a TableView column.
     *
     * Example:
     * Motion, Force, Work and Energy
     */
    public String topicNamesForDisplay() {
        return String.join(", ", topicNames);
    }

    /**
     * Useful when the topic IDs are needed as text.
     *
     * Example:
     * 3, 8, 12
     */
    public String topicIdsForDisplay() {
        return topicIds.stream()
                .map(String::valueOf)
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }
}