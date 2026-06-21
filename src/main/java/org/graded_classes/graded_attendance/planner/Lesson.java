package org.graded_classes.graded_attendance.planner;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ModalPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

public class Lesson implements Initializable {
    @FXML
    public VBox viewBox;
    public CustomTextField topic;
    Planner planner;
    String subject;
    String className;
    LinkedHashMap<Integer, Node> topicHashMap = new LinkedHashMap<>();
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
    public void addTopic() {
        int id=insertTopic();
        Node view = planner.createView(R.create_topic,
                new TopicCreator(planner, subject, className, topic.getText(), id,this));
       topicHashMap.put(id,view);
        viewBox.getChildren().add(view);
    }

    public void readTopics() {
        String sql = "SELECT * FROM Topics WHERE class = ? AND subject = ?";
        try (PreparedStatement pst = planner.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {

            pst.setString(1, className);
            pst.setString(2, subject);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Node view = planner.createView(R.create_topic,
                            new TopicCreator(planner, rs.getString("subject"),
                                    rs.getString("class"),
                                    rs.getString("topic_name"),
                                    rs.getInt("topic_id"),this));
                    viewBox.getChildren().add(view);
                    topicHashMap.put(rs.getInt("topic_id"),view);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int insertTopic() {
        String sql = "INSERT INTO Topics(class, subject, topic_name) VALUES(?, ?, ?)";

        try (PreparedStatement pst = planner.gradedDataLoader
                .databaseLoader.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, className);
            pst.setString(2, subject);
            pst.setString(3, topic.getText());

            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                System.out.println("Inserted topic_id ID: " + generatedId);
                return generatedId;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1; // fallback if something fails
    }


}
