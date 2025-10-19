package org.graded_classes.graded_attendance.controller;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
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
    private BarChart<String, Number> attendanceChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    LocalDate startDate = LocalDate.of(2025, 9, 6);
    LocalDate endDate = LocalDate.now();

    long totalWokingDays = ChronoUnit.DAYS.between(startDate, endDate);

    @FXML
    public void initialize() {
        xAxis.setLabel("Attendance Status");
        xAxis.setTickLabelsVisible(true);
        yAxis.setLabel("Days");
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(true);
        nameLabel.setText(name);

        int missingDay = Integer.parseInt(view.get("Missing Dates").trim());
        int present = Integer.parseInt(view.get(edNo).trim());
        System.out.println("Missing Dates: " + (totalWokingDays - missingDay - present));
        System.out.println("Present Dates: " + present);
        setAttendanceData(present, (int) (totalWokingDays - missingDay - present));
    }

    public void setStudentName(String name) {
        nameLabel.setText(name != null ? name : "Name");
    }

    public void setAttendanceData(int presentDays, int absentDays) {
        if (totalWokingDays > 0) {
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(totalWokingDays);
            yAxis.setTickUnit(5);
        } else {
            yAxis.setAutoRanging(true);
        }
        XYChart.Series<String, Number> present = new XYChart.Series<>();
        present.setName("Present");
        XYChart.Series<String, Number> absent = new XYChart.Series<>();
        absent.setName("Absent");
        present.getData().add(new XYChart.Data<>("Present", presentDays));
        absent.getData().add(new XYChart.Data<>("Absent", absentDays));
        attendanceChart.getData().setAll(present, absent);
        Platform.runLater(() -> {
          /*  for (XYChart.Data<String, Number> d : series.getData()) {
                String category = d.getXValue();
                String color = "Present".equals(category) ? "#1C75BC" : "#e74c3c"; // green / red
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-bar-fill: " + color + ";");
                }
            }*/
        });
    }
}
