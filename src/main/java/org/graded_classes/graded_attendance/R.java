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
    message_view("fxml/message_view.fxml"),
    message_box("fxml/message_box.fxml"),
    send_bubble_label("fxml/chat_bubble_send_layout.fxml"),
    receive_bubble_label("fxml/chat_bubble_receive_layout.fxml"),
    navigation("fxml/side_nav_bar.fxml"),
    edit_topic("fxml/editTopic.fxml"),
    edit_sub_topic("fxml/editSubTopic.fxml"),
    quiz_creator("fxml/quiz_generator.fxml"),
    quiz_taker("fxml/quiz_taker.fxml"),
    newTopic("fxml/new_topic_for quiz.fxml"),
    newQuiz("fxml/new_quiz.fxml"),
    question_editor("fxml/question_editor.fxml"),
    question("fxml/questions.fxml"),
    latex_editor("fxml/latex_editor.fxml"),
    triangular_button("fxml/triangular_buton.fxml"),
    question_preview("fxml/question_preview.fxml"),
    fee_receipt("fxml/fee_recipt.fxml"),
    points_table("fxml/leaderboard/data_edit.fxml"),
    leaderboard1("fxml/leaderboard/leader_board_view12.fxml"),
    leaderboard2("fxml/leaderboard/leader_board_view12.fxml"),
    seating_plan("fxml/seating_plan.fxml"),
    dashboard("fxml/dashboard.fxml");
    private final String path;

    R(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
