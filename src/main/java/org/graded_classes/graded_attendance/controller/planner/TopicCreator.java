package org.graded_classes.graded_attendance.controller.planner;

import atlantafx.base.controls.CustomTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

public class TopicCreator implements Initializable {
    final Planner planner;
    private final String subject;
    private final String className;
    private final String topic;
    public ListView<HBox> subtopicListView;
    @FXML
    private CustomTextField subtopicName;
    @FXML
    TitledPane titledPane;
    ObservableList<HBox> observableSubtopics = FXCollections.observableArrayList();
    LinkedHashMap<Integer, HBox> integerHBoxLinkedHashMap = new LinkedHashMap<>();
    int topic_id;
    Lesson lesson;

    public TopicCreator(Planner planner, String subject, String className, String topic, int topic_id, Lesson lesson) {
        this.planner = planner;
        this.subject = subject;
        this.className = className;
        this.topic = topic;
        this.topic_id = topic_id;
        this.lesson = lesson;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titledPane.setText(topic);
        var listOfSubtopics = getSubtopic();
        if (listOfSubtopics != null && !listOfSubtopics.isEmpty()) {
            for (var x : listOfSubtopics) {
                HBox node = (HBox) planner.createView(R.add_subtopic, new SubtopicCreator(this, x.name(), x.id()));
                integerHBoxLinkedHashMap.put(x.id(), node);
            }
        }
        observableSubtopics.addAll(integerHBoxLinkedHashMap.values());
        subtopicListView.setItems(observableSubtopics);
    }

    @FXML
    void editTopic() {
        planner.modalPane.setAlignment(Pos.CENTER);
        planner.modalPane.show(planner.createView(R.edit_topic,new EditTopic(this)));
    }

    @FXML
    void removeTopic() {
        lesson.viewBox.getChildren().remove(lesson.topicHashMap.get(topic_id));
        String sql = "delete from Topics where topic_id = ?";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().
                prepareStatement(sql)) {
            pst.setInt(1, topic_id);

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

    public void addSubTopic() {
        String name = subtopicName.getText();
        int id = addSubtopic(name);
        if (id != -1) {
            HBox node = (HBox) planner.createView(R.add_subtopic, new SubtopicCreator(this, name, id));
            integerHBoxLinkedHashMap.put(id, node);
            observableSubtopics.add(node);
        }
    }
    public int addSubtopic(String newSubtopic) {

        String insertSql = "INSERT INTO Subtopics (subtopic_name, subject, topic_id) VALUES (?, ?, ?)";

        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader
                .getConnection().prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, newSubtopic);
            pst.setString(2, subject);
            pst.setInt(3, topic_id);

            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        System.out.println("New subtopic inserted with ID: " + generatedId);
                        return generatedId;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // fallback if insert failed
    }

    public ArrayList<SubTopic> getSubtopic() {
        ArrayList<SubTopic> subtopic = new ArrayList<>();
        String sql = "SELECT * FROM Subtopics WHERE subject = ? AND topic_id = ?";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {

            pst.setString(1, subject);
            pst.setInt(2, topic_id);

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                subtopic.add(new SubTopic(rs.getInt("subtopic_id"), rs.getString("subtopic_name")));
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return subtopic;
    }


}
