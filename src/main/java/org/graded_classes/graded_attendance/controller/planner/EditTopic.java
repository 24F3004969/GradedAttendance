package org.graded_classes.graded_attendance.controller.planner;

import atlantafx.base.controls.CustomTextField;
import javafx.fxml.FXML;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditTopic {
    @FXML
    public CustomTextField editName;
    TopicCreator topicCreator;

    public EditTopic(TopicCreator topicCreator) {
        this.topicCreator = topicCreator;
    }

    @FXML
    public void editTopicName() {
        updateTopic(editName.getText());
        topicCreator.titledPane.setText(editName.getText());
        topicCreator.planner.modalPane.hide();
    }

    public void updateTopic(String newSubtopic) {
        String sql = "UPDATE Topics SET topic_name = ? WHERE  topic_id = ?";
        try (PreparedStatement pst = topicCreator.planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, newSubtopic);
            pst.setInt(2, topicCreator.topic_id);

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Subtopic updated successfully.");
            } else {
                System.out.println("No matching record found to update.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
