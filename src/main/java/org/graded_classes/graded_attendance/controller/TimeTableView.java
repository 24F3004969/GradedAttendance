package org.graded_classes.graded_attendance.controller;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.sql.Connection;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class TimeTableView extends TimeTable implements Initializable {
    public Label heading;
    @FXML
    private TableView<Map<String, Object>> table_view;
    @FXML
    private TableColumn<Map<String, Object>, String> day, three, four, five, six, seven, eight;
    private final TreeMap<Integer, String> grades = new TreeMap<>(Map.of(10, "X"
            , 9, "IX", 8, "VIII", 7, "VII", 6, "VI", 5, "V", 4, "IV"));
    ObservableList<Map<String, Object>> items = FXCollections.observableArrayList();

    public TimeTableView(String grade, Connection connection) {
        super(grade, connection);

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        heading.setText(heading.getText() + " " + getGrade());
        day.setCellValueFactory(map -> getValues(map, "Day"));
        three.setCellValueFactory(map -> getValues(map, "3:00 PM"));
        three.setCellFactory(TextFieldTableCell.forTableColumn());
        four.setCellValueFactory(map -> getValues(map, "4:00 PM"));
        four.setCellFactory(TextFieldTableCell.forTableColumn());
        five.setCellValueFactory(map -> getValues(map, "5:00 PM"));
        five.setCellFactory(TextFieldTableCell.forTableColumn());
        six.setCellValueFactory(map -> getValues(map, "6:00 PM"));
        six.setCellFactory(TextFieldTableCell.forTableColumn());
        seven.setCellValueFactory(map -> getValues(map, "7:00 PM"));
        seven.setCellFactory(TextFieldTableCell.forTableColumn());
        eight.setCellValueFactory(map -> getValues(map, "8:00 PM"));
        eight.setCellFactory(TextFieldTableCell.forTableColumn());
        three.setOnEditCommit(event -> eventResolver(event, "3:00 PM"));
        four.setOnEditCommit(event -> eventResolver(event, "4:00 PM"));
        five.setOnEditCommit(event -> eventResolver(event, "5:00 PM"));
        six.setOnEditCommit(event -> eventResolver(event, "6:00 PM"));
        seven.setOnEditCommit(event -> eventResolver(event, "7:00 PM"));
        eight.setOnEditCommit(event -> eventResolver(event, "8:00 PM"));
        for (var da : table.keySet())
            items.add(table.get(da));
        table_view.setItems(items);
        Styles.toggleStyleClass(table_view, Styles.BORDERED);
        Styles.toggleStyleClass(table_view, Styles.STRIPED);
    }

    private void eventResolver(TableColumn.CellEditEvent<Map<String, Object>, String> event, String key) {
        String listKey = event.getTableView().getItems().get(event.getTablePosition().getRow()).get("Day").toString();
        String object = "";
        Map<String, Object> timeSlot = table.get(listKey);
        object = event.getNewValue();
        timeSlot.put(key, object);
        updateTimeSlot(listKey, key, object);
        event.getTableView().getItems().get(event.getTablePosition().getRow()).put(key, object);


    }


    public ObservableValueBase<String> getValues(TableColumn.CellDataFeatures<Map<String, Object>, String> mapStringCellDataFeatures, String key) {
        return new ObservableValueBase<>() {
            @Override
            public String getValue() {

                return mapStringCellDataFeatures.getValue().get(key).toString();
            }
        };
    }

    @FXML
    private TextField topic1;

    @FXML
    private TextField topic2;

    @FXML
    private TextField topic3;

    @FXML
    private TextField topic4;
    @FXML
    private TextField subject1;

    @FXML
    private TextField subject2;

    @FXML
    private TextField subject3;

    @FXML
    private TextField subject4;

    @FXML
    void onNewTopic(ActionEvent event) {
        String sub1 = subject1.getText();
        String top1 = topic1.getText();
        String sub2 = subject2.getText();
        String top2 = topic2.getText();
        String sub3 = subject3.getText();
        String top3 = topic3.getText();
        String sub4 = subject4.getText();
        String top4 = topic4.getText();
        System.out.println(getGrade());
        insertDailyTopics(grades.get(Integer.parseInt(getGrade().trim())), sub1, top1, sub2, top2, sub3, top3, sub4, top4);

    }


}
