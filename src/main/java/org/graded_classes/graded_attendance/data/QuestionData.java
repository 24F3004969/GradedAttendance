package org.graded_classes.graded_attendance.data;

public record QuestionData(String question_id,
                           String topic_id,
                           String user_id,
                           String date_of_making,
                           String type,
                           String level,
                           String question_txt,
                           String question_img_path,OptionData option_data) {
}
