package org.graded_classes.graded_attendance.data;

import atlantafx.base.theme.Styles;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.graded_classes.graded_attendance.controller.MainController;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

public class GradedDataLoader {

    public LinkedHashMap<String, Student> getStudentData() {
        return studentData;
    }

    LinkedHashMap<String, Student> studentData = new LinkedHashMap<>();
    LocalDate today = LocalDate.now();
    public DatabaseLoader databaseLoader;
    MainController mainController;

    public GradedDataLoader(MainController mainController) {
        this.mainController = mainController;
        databaseLoader = new DatabaseLoader("Graded_" + today.getYear());
        try {
            databaseLoader.getStatement().execute("PRAGMA foreign_keys = ON;");
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/GradedData.sql").getQuery());
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/AbandonedEd.sql").getQuery());
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/attendance_stu.sql").getQuery());
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/Fee.sql").getQuery());
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/LessonPlanner.sql").getQuery());
            databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/TopicAndSubtopic.sql").getQuery());
            for (int i = 4; i <=10 ; i++) {
                databaseLoader.getStatement().executeUpdate(new SqlFileReader("data/TimeTable.sql").getQuery().formatted(i,i));

            }
            loadData();

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean removeStudent(Student student) {
        String sql = "DELETE FROM StudentData WHERE ed_no = ?";

        try (PreparedStatement preparedStatement = databaseLoader.getConnection().prepareStatement(sql)) {
            var alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Are you sure ?\nStudent with ED_NO " + student.ed_no()+" will be deleted.");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("OK button clicked.");
                preparedStatement.setString(1, student.ed_no());
                addEdToAbandonedEd(student.ed_no());
                int affectedRows = preparedStatement.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("Student with ed_no " + student.ed_no() + " removed successfully.");
                } else {
                    System.out.println("No student found with ed_no " + student.ed_no());
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error removing student: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void addEdToAbandonedEd(String s) {
        String sql = "INSERT INTO abandonedEd (ed_no) VALUES (?)";
        try (PreparedStatement pst = databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, s);
            pst.executeUpdate();
            System.out.println("ed_no inserted successfully.");

        } catch (SQLException e) {
            System.err.println("Not able to add to abandonedEd table" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public void removeEdFromAbandonedEd(String edNo) {
        String sql = "DELETE FROM abandonedEd WHERE ed_no = ?";

        try (PreparedStatement pst = databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, edNo);
            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("ed_no '" + edNo + "' removed successfully from abandonedEd.");
            } else {
                System.out.println("ed_no '" + edNo + "' not found in abandonedEd.");
            }

        } catch (SQLException e) {
            System.err.println("Failed to remove ed_no from abandonedEd: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public List<String> loadAbandonedEdNos() {
        List<String> abandonedEdNos = new ArrayList<>();
        String sql = "SELECT ed_no FROM abandonedEd";

        try (PreparedStatement pst = databaseLoader.getConnection().prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                abandonedEdNos.add(rs.getString("ed_no"));
            }

        } catch (SQLException e) {
            System.err.println("Error loading abandonedEd data: " + e.getMessage());
        }

        return abandonedEdNos;
    }

    public void addStudent(Student student) {
        try {
            studentData.put(student.ed_no(), student);

            student.insertIntoDatabase(databaseLoader.getStatement().getConnection());
            mainController.sendNotification("Student with ed_no " + student.ed_no() + " added successfully.\n\n", Styles.SUCCESS);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println(student);
    }

    private void loadData() throws SQLException {
        try (ResultSet rs = databaseLoader.getStatement().executeQuery("SELECT * FROM StudentData ORDER BY ed_no;" +
                "select * from StudentData")) {
            while (rs.next()) {
                studentData.put(rs.getString("ed_no"), new Student(
                        rs.getString("ed_no"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("bloodGroup"),
                        rs.getString("guardian_phone"),
                        rs.getString("aadhaar_no"),
                        rs.getString("father_name"),
                        rs.getString("mother_name"),
                        rs.getString("class"),
                        rs.getString("gender"),
                        rs.getString("dob"),
                        rs.getString("address"),
                        rs.getString("father_occ"),
                        rs.getString("mother_occ"),
                        rs.getString("previous_ins_name"),
                        rs.getString("reason_leaving"),
                        rs.getString("school_n"),
                        rs.getString("suggestions"),
                        rs.getString("subjects").split(", "), // Assuming subjects are stored as a comma-separated string
                        rs.getString("telegram_id")
                ));
            }
        }
    }

}
