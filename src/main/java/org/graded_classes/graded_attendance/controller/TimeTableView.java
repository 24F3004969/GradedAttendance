package org.graded_classes.graded_attendance.controller;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.sql.Connection;
import java.util.Map;
import java.util.ResourceBundle;

public class TimeTableView extends TimeTable implements Initializable {
    public Label heading;
    @FXML
    private TableView<Map<String, Object>> table_view;
    @FXML
    private TableColumn<Map<String, Object>, String> day, three, four, five, six, seven, eight;
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

}
