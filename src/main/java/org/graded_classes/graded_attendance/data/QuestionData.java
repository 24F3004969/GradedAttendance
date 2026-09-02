package org.graded_classes.graded_attendance.data;

import java.util.Objects;

public final class QuestionData {



    private  final String question_id;
    private final String topic_id;
    private final String user_id;
    private final String date_of_making;
    private  String type;
    private  String level;
    private  String question_txt;
    private  String question_img_path;
    private final OptionData option_data;

    public String getQuestion_id() {
        return question_id;
    }

    public String getTopic_id() {
        return topic_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public String getDate_of_making() {
        return date_of_making;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getQuestion_txt() {
        return question_txt;
    }

    public void setQuestion_txt(String question_txt) {
        this.question_txt = question_txt;
    }

    public String getQuestion_img_path() {
        return question_img_path;
    }

    public void setQuestion_img_path(String question_img_path) {
        this.question_img_path = question_img_path;
    }

    public OptionData getOption_data() {
        return option_data;
    }

    public QuestionData(String question_id,
                        String topic_id,
                        String user_id,
                        String date_of_making,
                        String type,
                        String level,
                        String question_txt,
                        String question_img_path, OptionData option_data) {
        this.question_id = question_id;
        this.topic_id = topic_id;
        this.user_id = user_id;
        this.date_of_making = date_of_making;
        this.type = type;
        this.level = level;
        this.question_txt = question_txt;
        this.question_img_path = question_img_path;
        this.option_data = option_data;
    }

    public String question_id() {
        return question_id;
    }

    public String topic_id() {
        return topic_id;
    }

    public String user_id() {
        return user_id;
    }

    public String date_of_making() {
        return date_of_making;
    }

    public String type() {
        return type;
    }

    public String level() {
        return level;
    }

    public String question_txt() {
        return question_txt;
    }

    public String question_img_path() {
        return question_img_path;
    }

    public OptionData option_data() {
        return option_data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QuestionData) obj;
        return Objects.equals(this.question_id, that.question_id) &&
                Objects.equals(this.topic_id, that.topic_id) &&
                Objects.equals(this.user_id, that.user_id) &&
                Objects.equals(this.date_of_making, that.date_of_making) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.level, that.level) &&
                Objects.equals(this.question_txt, that.question_txt) &&
                Objects.equals(this.question_img_path, that.question_img_path) &&
                Objects.equals(this.option_data, that.option_data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path, option_data);
    }

    @Override
    public String toString() {
        return "QuestionData[" +
                "question_id=" + question_id + ", " +
                "topic_id=" + topic_id + ", " +
                "user_id=" + user_id + ", " +
                "date_of_making=" + date_of_making + ", " +
                "type=" + type + ", " +
                "level=" + level + ", " +
                "question_txt=" + question_txt + ", " +
                "question_img_path=" + question_img_path + ", " +
                "option_data=" + option_data + ']';
    }

}
