package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.CustomTextField;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class TopicCreator implements Initializable {
    private final Planner planner;
    private final String subject;
    private final String className;
    private final String topic;
    public ListView<HBox> subtopicListView;
    @FXML
    private CustomTextField subtopicName;
    @FXML
    TitledPane titledPane;
    ArrayList<HBox> subtopicList = new ArrayList<>();

    public TopicCreator(Planner planner, String subject, String className, String topic) {
        this.planner = planner;
        this.subject = subject;
        this.className = className;
        this.topic = topic;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titledPane.setText(topic);
        var listOfSubtopics = getSubtopic();
        if (listOfSubtopics != null && !listOfSubtopics.isEmpty()) {
            var v=listOfSubtopics.split(",");
            for (var x : v) {
                HBox node = (HBox) planner.createView(R.add_subtopic, new SubtopicCreator(planner, x));
                subtopicList.add(node);
                subtopicListView.setItems(FXCollections.observableList(subtopicList));
            }
        }
    }

    @FXML
    void editTopic() {

    }

    @FXML
    void removeTopic() {

    }

    public void addSubTopic() {
        String name = subtopicName.getText();
        HBox node = (HBox) planner.createView(R.add_subtopic, new SubtopicCreator(planner, name));
        subtopicList.add(node);
        subtopicListView.setItems(FXCollections.observableList(subtopicList));
        updateSubtopic(name);
    }

    public void updateSubtopic(String newSubtopic) {
        String sql = "UPDATE Topics SET subtopic = ? WHERE class = ? AND subject = ? AND topic = ?";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setString(1, addNewSubtopic(newSubtopic));
            pst.setString(2, className);
            pst.setString(3, subject);
            pst.setString(4, topic);

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

    private String addNewSubtopic(String newSubtopic) {
        var previousSubtopic = getSubtopic();
        if (previousSubtopic != null) {
            return previousSubtopic + "," + newSubtopic;
        }
        return newSubtopic;
    }

    public String getSubtopic() {
        String subtopic = "";
        String sql = "SELECT subtopic FROM Topics WHERE class = ? AND subject = ? AND topic = ?";

        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {

            pst.setString(1, className);
            pst.setString(2, subject);
            pst.setString(3, topic);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                subtopic = rs.getString("subtopic");
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return subtopic;
    }


}
