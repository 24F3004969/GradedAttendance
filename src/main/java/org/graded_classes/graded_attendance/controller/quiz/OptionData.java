package org.graded_classes.graded_attendance.controller.quiz;

import java.util.ArrayList;

public record OptionData(
        int option_index,
       ArrayList<String> options
) {
}
