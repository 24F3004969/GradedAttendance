package org.graded_classes.graded_attendance.controller.leaderboard;


import org.graded_classes.graded_attendance.data.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;


public class StudentDataLoader {
    private final LinkedHashMap<String, StudentScore> studentLinkedHashMap = new LinkedHashMap<>(20);
    public DatabaseLoader databaseLoader;

    public LinkedHashMap<String, StudentScore> getStudentLinkedHashMap() {
        return studentLinkedHashMap;
    }

    public List<StudentScore> getSortedStudentList() {
        List<StudentScore> list = new ArrayList<>(studentLinkedHashMap.values());
        list.sort(Comparator.comparing(StudentScore::points, Comparator.reverseOrder()));
        System.out.println(list);
        return list;
    }

    public List<StudentScore> getStudentList() {
        return new ArrayList<>(studentLinkedHashMap.values());
    }

    public StudentDataLoader( LinkedHashMap<String, org.graded_classes.graded_attendance.data.Student> studentLinkedHashMap) {
         for (String key : studentLinkedHashMap.keySet()) {
             Student student = studentLinkedHashMap.get(key);
             this.studentLinkedHashMap.put(key,new StudentScore(student.ed_no(),student.name(),student._class(),student.points));
         }
    }

    /*private void init() {
        databaseLoader = new DatabaseLoader("G:/My Drive/", "LeaderBoard.db");
        try (var x = databaseLoader.getStatement().executeQuery("SELECT * FROM LEADERS")) {

            while (x.next()) {
                var stu = new StudentScore(x.getString("ED No."),
                        x.getString("Name"), x.getString("Class"),
                        Double.parseDouble(x.getString("Points")));
                studentLinkedHashMap.put(x.getString("ED No."), stu);
            }
            System.out.println(studentLinkedHashMap);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }*/


}
