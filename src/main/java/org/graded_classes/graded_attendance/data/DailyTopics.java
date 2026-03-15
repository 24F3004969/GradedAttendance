package org.graded_classes.graded_attendance.data;

public class DailyTopics {

    private String date;
    private String className;  // 'class' is a reserved keyword in Java
    private String subject1;
    private String topic1;
    private String subject2;
    private String topic2;
    private String subject3;
    private String topic3;
    private String subject4;
    private String topic4;

    public DailyTopics() {}

    public DailyTopics(String date, String className,
                       String subject1, String topic1,
                       String subject2, String topic2,
                       String subject3, String topic3,
                       String subject4, String topic4) {

        this.date = date;
        this.className = className;
        this.subject1 = subject1;
        this.topic1 = topic1;
        this.subject2 = subject2;
        this.topic2 = topic2;
        this.subject3 = subject3;
        this.topic3 = topic3;
        this.subject4 = subject4;
        this.topic4 = topic4;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSubject1() {
        return subject1;
    }

    public void setSubject1(String subject1) {
        this.subject1 = subject1;
    }

    public String getTopic1() {
        return topic1;
    }

    public void setTopic1(String topic1) {
        this.topic1 = topic1;
    }

    public String getSubject2() {
        return subject2;
    }

    public void setSubject2(String subject2) {
        this.subject2 = subject2;
    }

    public String getTopic2() {
        return topic2;
    }

    public void setTopic2(String topic2) {
        this.topic2 = topic2;
    }

    public String getSubject3() {
        return subject3;
    }

    public void setSubject3(String subject3) {
        this.subject3 = subject3;
    }

    public String getTopic3() {
        return topic3;
    }

    public void setTopic3(String topic3) {
        this.topic3 = topic3;
    }

    public String getSubject4() {
        return subject4;
    }

    public void setSubject4(String subject4) {
        this.subject4 = subject4;
    }

    public String getTopic4() {
        return topic4;
    }

    public void setTopic4(String topic4) {
        this.topic4 = topic4;
    }

    @Override
    public String toString() {
        return "DailyTopics{" +
                "date='" + date + '\'' +
                ", className='" + className + '\'' +
                ", subject1='" + subject1 + '\'' +
                ", topic1='" + topic1 + '\'' +
                ", subject2='" + subject2 + '\'' +
                ", topic2='" + topic2 + '\'' +
                ", subject3='" + subject3 + '\'' +
                ", topic3='" + topic3 + '\'' +
                ", subject4='" + subject4 + '\'' +
                ", topic4='" + topic4 + '\'' +
                '}';
    }
}