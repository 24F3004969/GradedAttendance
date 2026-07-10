package org.graded_classes.graded_attendance.controller.leaderboard;

import java.util.Objects;

public final class StudentScore {
    private final String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    private  String name;
    private  String grade;

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    private double points;

    public StudentScore(String id, String name, String grade, double points) {
        super();
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.points = points;
    }

    @Override
    public String toString() {
        return "StudentScore[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "grade=" + grade + ", " +
                "points=" + points + ']';
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String grade() {
        return grade;
    }

    public double points() {
        return points;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StudentScore) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.grade, that.grade) &&
                Double.doubleToLongBits(this.points) == Double.doubleToLongBits(that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, grade, points);
    }


}
