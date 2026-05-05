package org.graded_classes.graded_attendance.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.*;

public class SeatingPlan implements Initializable {

    @FXML
    private VBox grade;

    @FXML
    private VBox room_no;

    @FXML
    private VBox subject;

    @FXML
    private VBox teacherView;

    @FXML
    private VBox timing;
    private final LinkedHashMap<String, ArrayList<DailyTimeTable>> seatingMap;
    private Map<String, String> colorMap = Map.of(
            "03:30 PM", "#FCE4EC",
            "04:15 PM", "#E8F5E9",
            "05:00 PM", "#FFF3E0",
            "05:45 PM", "#E3F2FD",
            "06:30 PM", "#F3E5F5"

    );

    public SeatingPlan(LinkedHashMap<String, ArrayList<DailyTimeTable>> seatingMap) {
        this.seatingMap = seatingMap;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (Map.Entry<String, ArrayList<DailyTimeTable>> entry : seatingMap.entrySet()) {
                for (DailyTimeTable t : entry.getValue()) {
                    timing.getChildren().add(createCell(t.time(),t.time()));
                    grade.getChildren().add(createCell(t.grade(),t.time()));
                    room_no.getChildren().add(createCell(t.roomNo(),t.time()));
                    subject.getChildren().add(createCell(t.subject(),t.time()));
                    teacherView.getChildren().add(createCell(t.teacherName(),t.time()));
                }
            }
        }

    private HBox createCell(String text,String time) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        HBox hBox = new HBox(label);
        hBox.setAlignment(Pos.CENTER);
        hBox.setMinWidth(120);
        hBox.setMinHeight(46);
        hBox.setStyle("""
                -fx-background-color:'%s';
                -fx-background-radius: 5;
                -fx-border-color: rgba(0, 0, 0, 0.2);
                -fx-border-radius: 5;
                """.formatted(colorMap.get(time)));
        return hBox;
    }

    public LinkedHashMap<String, ArrayList<DailyTimeTable>> getSeatingMap() {
        return seatingMap;
    }
}