package org.graded_classes.graded_attendance.controller;


import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

public class AttendanceReportController implements Initializable {

    String name;
    LinkedHashMap<String, String> view;
    String edNo;

    public AttendanceReportController(String name, String edNo, LinkedHashMap<String, String> view) {
        this.name = name;
        this.edNo = edNo;
        this.view = view;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initialize();
    }

    @FXML
    private Label nameLabel;
    @FXML
    private PieChart attendanceChart;


    LocalDate startDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 1);
    LocalDate endDate = LocalDate.now();

    long totalWokingDays = ChronoUnit.DAYS.between(startDate, endDate);

    @FXML
    public void initialize() {
        nameLabel.setText(name);

        int missingDay = Integer.parseInt(view.get("Missing Dates").trim());
        int present = Integer.parseInt(view.get(edNo).trim());
        System.out.println("Missing Dates: " + (totalWokingDays - missingDay - present));
        System.out.println("Present Dates: " + present);
        setAttendanceData(present, (int) (totalWokingDays - missingDay - present),missingDay);
    }

    public void setStudentName(String name) {
        nameLabel.setText(name != null ? name : "Name");
    }

    public void setAttendanceData(int presentDays, int absentDays,int missingDay) {


        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                        new PieChart.Data("Present", presentDays),
                        new PieChart.Data("Absent", absentDays));
        attendanceChart.setData(pieChartData);
        for (PieChart.Data data : pieChartData) {
            data.nameProperty().bind(
                    Bindings.concat(data.getName(), " ", data.pieValueProperty())
            );
        }
    }
}
