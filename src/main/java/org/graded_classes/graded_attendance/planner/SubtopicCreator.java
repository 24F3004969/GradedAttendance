package org.graded_classes.graded_attendance.planner;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import org.graded_classes.graded_attendance.R;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SubtopicCreator implements Initializable {
     final TopicCreator topicCreator;
    private final String name;
    public Label subtopicName;
    int id;
    public SubtopicCreator(TopicCreator topicCreator, String name,int id) {
        this.topicCreator = topicCreator;
        this.name = name;
        this.id = id;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
         subtopicName.setText(name);
    }
    @FXML
    void editSubtopic() {
        topicCreator.planner.modalPane.setAlignment(Pos.CENTER);
        topicCreator.planner.modalPane.show(topicCreator.planner.createView(R.edit_sub_topic,new EditSubTopic(this)));
    }

    @FXML
    void removeSubtopic() {
        if (removeItFromDb())
            topicCreator.observableSubtopics.remove(topicCreator.integerHBoxLinkedHashMap.remove(id));
    }

    private boolean removeItFromDb() {
      String sql="delete from Subtopics where subtopic_id=?";
        try (PreparedStatement pst = topicCreator.planner.gradedDataLoader.
                databaseLoader.getConnection().prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
      return false;
    }
}
