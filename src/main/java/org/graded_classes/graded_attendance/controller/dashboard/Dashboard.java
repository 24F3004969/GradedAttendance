package org.graded_classes.graded_attendance.controller.dashboard;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class Dashboard implements Initializable {


    @FXML
    private BarChart<String, Number> attendanceGraph;

    @FXML
    private AreaChart<String, Number> revenueTrendGraph;

    @FXML
    private LineChart<String, Number> admissionRateGraph;


    @FXML
    private Label attendanceRate;

    @FXML
    private Label classScheduled;

    @FXML
    private Label current_date;

    @FXML
    private TextField filterText;

    @FXML
    private Label monthlyRevenue;

    @FXML
    private Label paymentNumber;


    @FXML
    private Label studentNo;

    @FXML
    private Label teacherNumber;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        current_date.setText(LocalDate.now().getDayOfMonth() + " " +
                format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        loadAttendanceData();
        loadRevenueData();
        loadAdmissionData();
        XYChart.Series<String, Number> admissions = new XYChart.Series<>();
        admissions.setName("Admissions");

        animateLineChart(admissionRateGraph, admissions);
        animateBarChart(attendanceGraph);
        animateAreaChart(revenueTrendGraph);

    }
    private String format(String date) {
        return date.charAt(0) + date.substring(1).toLowerCase();
    }
    private void loadAttendanceData() {
        XYChart.Series<String, Number> attendance = new XYChart.Series<>();
        attendance.setName("Attendance %");

        attendance.getData().add(new XYChart.Data<>("Jan", 85));
        attendance.getData().add(new XYChart.Data<>("Feb", 88));
        attendance.getData().add(new XYChart.Data<>("Mar", 82));
        attendance.getData().add(new XYChart.Data<>("Apr", 90));
        attendance.getData().add(new XYChart.Data<>("May", 87));

        attendanceGraph.getData().add(attendance);
    }

    // ---------------- Revenue Area Chart ----------------
    private void loadRevenueData() {
        XYChart.Series<String, Number> revenue = new XYChart.Series<>();
        revenue.setName("Revenue (₹)");

        revenue.getData().add(new XYChart.Data<>("Jan", 12000));
        revenue.getData().add(new XYChart.Data<>("Feb", 15000));
        revenue.getData().add(new XYChart.Data<>("Mar", 18000));
        revenue.getData().add(new XYChart.Data<>("Apr", 16000));
        revenue.getData().add(new XYChart.Data<>("May", 21000));

        revenueTrendGraph.getData().add(revenue);
    }

    // ---------------- Admission Line Chart ----------------
    private void loadAdmissionData() {
        XYChart.Series<String, Number> admissions = new XYChart.Series<>();
        admissions.setName("Admissions");

        admissions.getData().add(new XYChart.Data<>("Jan", 120));
        admissions.getData().add(new XYChart.Data<>("Feb", 135));
        admissions.getData().add(new XYChart.Data<>("Mar", 110));
        admissions.getData().add(new XYChart.Data<>("Apr", 150));
        admissions.getData().add(new XYChart.Data<>("May", 170));

        admissionRateGraph.getData().add(admissions);

    }
    private void animateLineChart(
            LineChart<String, Number> chart,
            XYChart.Series<String, Number> series) {

        chart.getData().clear();
        chart.getData().add(series);
        series.getData().clear();

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May"};
        int[] values = {120, 135, 110, 150, 170};

        Timeline timeline = new Timeline();

        for (int i = 0; i < months.length; i++) {
            int index = i;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(i * 0.05), // ⬅ increase time here
                    e -> series.getData().add(
                            new XYChart.Data<>(months[index], values[index])
                    )
            );

            timeline.getKeyFrames().add(keyFrame);
        }

        timeline.play();
    }
    private void animateBarChart(BarChart<String, Number> chart) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance");

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May"};
        int[] values = {85, 88, 82, 90, 87};

        Timeline timeline = new Timeline();

        for (int i = 0; i < months.length; i++) {
            int index = i;
            timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(i * 0.05),
                            e -> series.getData().add(
                                    new XYChart.Data<>(months[index], values[index])
                            )
                    )
            );
        }

        chart.getData().clear();
        chart.getData().add(series);
        timeline.play();
    }
    private void animateAreaChart(AreaChart<String, Number> chart) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance");

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May"};
        int[] values = {85, 88, 82, 90, 87};

        Timeline timeline = new Timeline();

        for (int i = 0; i < months.length; i++) {
            int index = i;
            timeline.getKeyFrames().add(
                    new KeyFrame(Duration.seconds(i * 0.05),
                            e -> series.getData().add(
                                    new XYChart.Data<>(months[index], values[index])
                            )
                    )
            );
        }

        chart.getData().clear();
        chart.getData().add(series);
        timeline.play();
    }

}
