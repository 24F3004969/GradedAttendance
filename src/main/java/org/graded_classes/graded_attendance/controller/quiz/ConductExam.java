package org.graded_classes.graded_attendance.controller.quiz;


import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.ExamData;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ConductExam implements Initializable {
    MainController mainController;

    @FXML
    private TableColumn<ExamData, Button> action;

    @FXML
    private TableColumn<ExamData, String> classes, doe, id, room, subject, time, topic;
    @FXML
    private TableView<ExamData> scheduleTable;
    ObservableList<ExamData> items = FXCollections.observableList(new ArrayList<>());

    @FXML
    private Label today;
    private final ArrayList<ExamData> examSchedular;

    public ConductExam(MainController mainController, ArrayList<ExamData> examSchedular) {
        this.mainController = mainController;
        this.examSchedular = examSchedular;
    }

    @FXML
    void addNewExam() {
        mainController.modalPane.setPersistent(true);
        Node node = mainController.gradedFxmlLoader.createView(R.exam_create, new ExamCreator(mainController,this));
        mainController.modalPane.show(node);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        today.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        classes.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().classes()));
        doe.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().doe()));
        id.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().id()));
        room.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().room()));
        subject.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().subject()));
        time.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().time()));
        topic.setCellValueFactory(map -> new SimpleStringProperty(map.getValue().topic()));
        action.setCellValueFactory(arg0 -> {
            Button button = new Button("Start Exam");
            button.setPadding(new Insets(5, 5, 5, 5));
            button.getStyleClass().add(Styles.SUCCESS);
            ExamData studentInfo = arg0.getValue();
            button.setOnMouseClicked(a -> {
                mainController.modalPane.show(mainController.gradedFxmlLoader
                        .createView(R.exam_entry_login, new LoginBeforeEntry(mainController)));
            });
            return new SimpleObjectProperty<>(button);
        });
        for (var sec : examSchedular) {
            items.addAll(sec);
        }
        scheduleTable.setItems(items);
    }

    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }
}
