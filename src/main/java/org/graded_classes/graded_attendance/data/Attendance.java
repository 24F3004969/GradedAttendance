package org.graded_classes.graded_attendance.data;

public class Attendance {
    String check_in;
    String check_out;
    Boolean homework_status;
    String topics;

    public Attendance(String check_in, String check_out, Boolean homework_status, String topics) {
        this.check_in = check_in;
        this.check_out = check_out;
        this.homework_status = homework_status;
        this.topics=topics;
    }

    public String getCheck_out() {
        return check_out;
    }

    public void setCheck_out(String check_out) {
        this.check_out = check_out;
    }

    public Boolean getHomework_status() {
        return homework_status;
    }

    public void setHomework_status(Boolean homework_status) {
        this.homework_status = homework_status;
    }

    public String getCheck_in() {
        return check_in;
    }

    public void setCheck_in(String check_in) {
        this.check_in = check_in;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "check_in='" + check_in + '\'' +
                ", check_out='" + check_out + '\'' +
                ", homework_status='" + homework_status + '\'' +
                ", topics='" + topics + '\'' +
                '}';
    }
}
