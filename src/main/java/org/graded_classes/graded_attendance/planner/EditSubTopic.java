package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.CustomTextField;
import javafx.fxml.FXML;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditSubTopic {

    @FXML
    public CustomTextField editName;
    SubtopicCreator subtopicCreator;

    public EditSubTopic(SubtopicCreator subtopicCreator) {
        this.subtopicCreator = subtopicCreator;
    }

    @FXML
    private void editSubTopicName() {
        subtopicCreator.subtopicName.setText(editName.getText());
        subtopicCreator.topicCreator.planner.modalPane.hide();
        updateSubtopic(editName.getText());
    }

    public void updateSubtopic(String newSubtopic) {
        String sql = "UPDATE Subtopics SET subtopic_name = ? WHERE  subtopic_id = ?";
        try (PreparedStatement pst = subtopicCreator.topicCreator.planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, newSubtopic);
            pst.setInt(2, subtopicCreator.id);

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
