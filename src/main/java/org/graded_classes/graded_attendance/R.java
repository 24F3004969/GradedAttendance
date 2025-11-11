package org.graded_classes.graded_attendance;

public enum R {
    add_people("fxml/add_people.fxml"),
    custom_data_view("fxml/custom_data_view.fxml"), fee_report("fxml/fee_report.fxml"), home_layout("fxml/home_tab.fxml"),
    new_student_layout("fxml/new_student_layout.fxml"), new_teacher_layout("fxml/new_teacher_layout.fxml"), student_attendance_layout("fxml/student_attendance_layout.fxml"),
    student_fee_layout("fxml/student_fee_layout.fxml"),
    data_view("fxml/attendance_data_view.fxml"),
    teacher_attendance_layout("fxml/teacher_attendance_layout.fxml"),
    teacher_payment_layout("fxml/teachers_payment_layout.fxml"),
    teaching_progress_report("fxml/teaching_progress_report.fxml"),
    database_layout("fxml/database_edit.fxml"),
    chat_layout("fxml/chat_bot.fxml"),
    payment_done_animation("fxml/payment_done_animation.fxml"),
    calendar_layout("fxml/calendar_layout.fxml"),
    lesson_planner("fxml/lesson_planner.fxml"),
    classes("fxml/classes.fxml"),
    subject("fxml/subjects.fxml"),
    add_lesson("fxml/add_lesson.fxml"),
    add_subtopic("fxml/subtopic.fxml"),
    create_topic("fxml/topics_creator.fxml"),
    notification("fxml/notifications.fxml"),
    edit_time("fxml/editTime.fxml"),
    attendance_report("fxml/attendance_report.fxml"),
    teaching_progress_search("fxml/teaching_progress_layout_search.fxml"),
    topic_taught_today("fxml/todays_topics.fxml"),
    message_view("fxml/message_view.fxml"),;
    private final String path;

    R(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
