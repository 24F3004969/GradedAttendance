package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ModalPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class Lesson implements Initializable {
    @FXML
    public VBox viewBox;
    public CustomTextField topic;
    Planner planner;
    String subject;
    String className;

    public Lesson(Planner planner, String subject, String className) {
        this.planner = planner;
        this.subject = subject;
        this.className = className;
    }

    @FXML
    private Label subjectName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        subjectName.setText(subject);
        readTopics();
    }

    @FXML
    public void addSubtopic() {
        viewBox.getChildren().add(planner.createView(R.create_topic, new  TopicCreator(planner, subject, className, topic.getText(),null)));
        insertTopic();
    }

    public void readTopics() {
        String sql = "SELECT * FROM Topics WHERE class = ? AND subject = ?";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {

            pst.setString(1, className);
            pst.setString(2, subject);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    viewBox.getChildren().add(planner.createView(R.create_topic, new TopicCreator(planner, rs.getString("subject"),
                            rs.getString("class"), rs.getString("topic_name"), rs.getString("topic_id"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertTopic() {
        String sql = "INSERT INTO Topics(class, subject, topic_name) VALUES(?, ?, ?)";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, className);
            pst.setString(2, subject);
            pst.setString(3, topic.getText());
            pst.executeUpdate();
            System.out.println("Topic inserted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
