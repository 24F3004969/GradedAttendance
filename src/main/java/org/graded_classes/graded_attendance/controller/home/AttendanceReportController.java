package org.graded_classes.graded_attendance.controller.home;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.report.AttendanceReport;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AttendanceReportController implements Initializable {

    private static final String PRESENT_COLOR = "#2196F3";
    private static final String ABSENT_COLOR = "#F44336";
    private static final String NO_ATTENDANCE_COLOR = "#FF9800";

    private final String name;
    private final String edNo;
    private final AttendanceDataView dataView;

    private LinkedHashMap<String, String> view;

    @FXML
    private Spinner<String> months;

    @FXML
    private Label nameLabel;

    @FXML
    private PieChart attendanceChart;

    @FXML
    private Button fullYearReportButton;

    public AttendanceReportController(
            String name,
            String edNo,
            AttendanceDataView dataView
    ) {
        this.name = name;
        this.edNo = edNo;
        this.dataView = dataView;
    }

    @Override
    public void initialize(
            URL location,
            ResourceBundle resources
    ) {
        nameLabel.setText(
                name != null && !name.isBlank()
                        ? name
                        : "Name"
        );

        initializeMonthSpinner();

        months.valueProperty().addListener(
                (observable, oldMonth, newMonth) -> {
                    if (newMonth == null || newMonth.isBlank()) {
                        return;
                    }

                    int monthNumber = Month.valueOf(
                            newMonth.toUpperCase(Locale.ENGLISH)
                    ).getValue();

                    loadAttendanceForMonth(monthNumber);
                }
        );

        /*
         * The spinner already has the current month selected,
         * so load that month's report during initialization.
         */
        loadAttendanceForMonth(
                LocalDate.now().getMonthValue()
        );
    }

    private void initializeMonthSpinner() {
        List<String> monthNames = List.of(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
        );

        SpinnerValueFactory.ListSpinnerValueFactory<String>
                valueFactory =
                new SpinnerValueFactory.ListSpinnerValueFactory<>(
                        FXCollections.observableArrayList(monthNames)
                );

        String currentMonth = LocalDate.now()
                .getMonth()
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                );

        valueFactory.setValue(currentMonth);
        months.setValueFactory(valueFactory);

        if (!months.getStyleClass().contains(
                Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL
        )) {
            months.getStyleClass().add(
                    Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL
            );
        }
    }

    private AttendanceReport createAttendanceReport(
            int monthNumber
    ) {
        return new AttendanceReport(
                dataView.studentAttendance
                        .mainController
                        .gradedDataLoader
                        .databaseLoader,
                monthNumber
        );
    }

    private void loadAttendanceForMonth(
            int monthNumber
    ) {
        try {
            AttendanceReport attendanceReport =
                    createAttendanceReport(monthNumber);

            attendanceReport.init();

            view = attendanceReport
                    .getStudentAttendanceReport();

            refreshMonthlyChart(monthNumber);

        } catch (Exception exception) {
            exception.printStackTrace();

            attendanceChart.getData().clear();
            attendanceChart.setTitle(
                    "Unable to Load Attendance"
            );

            showAlert(
                    Alert.AlertType.ERROR,
                    "Attendance Error",
                    "Could not load attendance for the selected month.\n\n"
                            + getExceptionMessage(exception)
            );
        }
    }

    private void refreshMonthlyChart(
            int monthNumber
    ) {
        int reportYear = LocalDate.now().getYear();

        YearMonth reportMonth = YearMonth.of(
                reportYear,
                monthNumber
        );

        /*
         * Do not calculate or display future months.
         */
        if (reportMonth.isAfter(YearMonth.now())) {
            attendanceChart.getData().clear();

            attendanceChart.setTitle(
                    getFullMonthName(monthNumber)
                            + " "
                            + reportYear
                            + " - Future Month"
            );

            return;
        }

        AttendanceMonthData monthData =
                calculateMonthData(
                        reportMonth,
                        view
                );

        attendanceChart.setTitle(
                getFullMonthName(monthNumber)
                        + " "
                        + reportYear
                        + " Attendance Report"
        );

        printMonthData(monthData);

        setAttendanceData(
                monthData.presentDays(),
                monthData.absentDays(),
                monthData.noAttendanceDays()
        );
    }

    /**
     * Calculates one month's final validated values.
     *
     * The same method is used by both the monthly pie chart
     * and the full-year bar chart.
     */
    private AttendanceMonthData calculateMonthData(
            YearMonth reportMonth,
            LinkedHashMap<String, String> monthlyView
    ) {
        if (reportMonth.isAfter(YearMonth.now())) {
            return new AttendanceMonthData(
                    reportMonth,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        int elapsedDays = calculateElapsedDays(
                reportMonth
        );

        int presentDays = getAttendanceValue(
                monthlyView,
                edNo
        );

        /*
         * Present cannot exceed the number of elapsed dates.
         */
        presentDays = Math.min(
                presentDays,
                elapsedDays
        );

        /*
         * Requested behavior:
         *
         * If the student has no attendance records for this month,
         * mark all elapsed dates as No Attendance.
         *
         * Do not mark these dates as Present or Absent.
         */
        if (presentDays == 0) {
            return new AttendanceMonthData(
                    reportMonth,
                    elapsedDays,
                    0,
                    0,
                    elapsedDays,
                    0
            );
        }

        int rawMissingDays = getAttendanceValue(
                monthlyView,
                "Missing Dates"
        );

        /*
         * AttendanceReport may calculate Missing Dates using
         * the complete calendar month.
         *
         * For the current month, remove future dates.
         */
        int futureDays = calculateFutureDays(
                reportMonth
        );

        int noAttendanceDays = Math.max(
                0,
                rawMissingDays - futureDays
        );

        /*
         * No Attendance cannot exceed the number of elapsed
         * dates remaining after Present is removed.
         */
        int maximumNoAttendanceDays = Math.max(
                0,
                elapsedDays - presentDays
        );

        noAttendanceDays = Math.min(
                noAttendanceDays,
                maximumNoAttendanceDays
        );

        int absentDays = Math.max(
                0,
                elapsedDays
                        - presentDays
                        - noAttendanceDays
        );

        return new AttendanceMonthData(
                reportMonth,
                elapsedDays,
                presentDays,
                absentDays,
                noAttendanceDays,
                rawMissingDays
        );
    }

    private int calculateElapsedDays(
            YearMonth reportMonth
    ) {
        YearMonth currentMonth = YearMonth.now();

        if (reportMonth.isAfter(currentMonth)) {
            return 0;
        }

        LocalDate startDate =
                reportMonth.atDay(1);

        LocalDate endDate;

        if (reportMonth.equals(currentMonth)) {
            /*
             * Current month:
             * First day of the month through today.
             */
            endDate = LocalDate.now();
        } else {
            /*
             * Previous month:
             * Count the complete month.
             */
            endDate = reportMonth.atEndOfMonth();
        }

        /*
         * plusDays(1) makes the date range inclusive.
         *
         * October 1 through October 18 = 18 days.
         */
        return (int) ChronoUnit.DAYS.between(
                startDate,
                endDate.plusDays(1)
        );
    }

    private int calculateFutureDays(
            YearMonth reportMonth
    ) {
        YearMonth currentMonth = YearMonth.now();

        if (reportMonth.isBefore(currentMonth)) {
            return 0;
        }

        if (reportMonth.isAfter(currentMonth)) {
            return reportMonth.lengthOfMonth();
        }

        return reportMonth.lengthOfMonth()
                - LocalDate.now().getDayOfMonth();
    }

    private int getAttendanceValue(
            LinkedHashMap<String, String> attendanceView,
            String key
    ) {
        if (attendanceView == null
                || key == null
                || key.isBlank()) {
            return 0;
        }

        String value = attendanceView.get(key);

        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Math.max(
                    0,
                    Integer.parseInt(value.trim())
            );

        } catch (NumberFormatException exception) {
            System.err.println(
                    "Invalid attendance value for key '"
                            + key
                            + "': "
                            + value
            );

            return 0;
        }
    }

    private void printMonthData(
            AttendanceMonthData monthData
    ) {
        int validationTotal =
                monthData.presentDays()
                        + monthData.absentDays()
                        + monthData.noAttendanceDays();

        System.out.println("--------------------------------");
        System.out.println("Student: " + name);
        System.out.println("ED No: " + edNo);

        System.out.println(
                "Month: "
                        + monthData.reportMonth()
                        .getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.ENGLISH
                        )
        );

        System.out.println(
                "Elapsed days: "
                        + monthData.elapsedDays()
        );

        System.out.println(
                "Raw Missing Dates: "
                        + monthData.rawMissingDays()
        );

        System.out.println(
                "Present: "
                        + monthData.presentDays()
        );

        System.out.println(
                "Absent: "
                        + monthData.absentDays()
        );

        System.out.println(
                "No Attendance: "
                        + monthData.noAttendanceDays()
        );

        System.out.println(
                "Validation total: "
                        + validationTotal
        );

        if (validationTotal
                != monthData.elapsedDays()) {
            System.err.println(
                    "Attendance validation failed: "
                            + validationTotal
                            + " != "
                            + monthData.elapsedDays()
            );
        }

        System.out.println("--------------------------------");
    }

    public void setAttendanceData(
            int presentDays,
            int absentDays,
            int noAttendanceDays
    ) {
        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList();

        if (presentDays > 0) {
            PieChart.Data presentData =
                    new PieChart.Data(
                            "Present",
                            presentDays
                    );

            assignPieChartColor(
                    presentData,
                    PRESENT_COLOR
            );

            pieChartData.add(presentData);
        }

        if (absentDays > 0) {
            PieChart.Data absentData =
                    new PieChart.Data(
                            "Absent",
                            absentDays
                    );

            assignPieChartColor(
                    absentData,
                    ABSENT_COLOR
            );

            pieChartData.add(absentData);
        }

        if (noAttendanceDays > 0) {
            PieChart.Data noAttendanceData =
                    new PieChart.Data(
                            "No Attendance",
                            noAttendanceDays
                    );

            assignPieChartColor(
                    noAttendanceData,
                    NO_ATTENDANCE_COLOR
            );

            pieChartData.add(noAttendanceData);
        }

        attendanceChart.setData(pieChartData);

        for (PieChart.Data chartData : pieChartData) {
            String categoryName =
                    chartData.getName();

            chartData.nameProperty().bind(
                    Bindings.concat(
                            categoryName,
                            ": ",
                            chartData
                                    .pieValueProperty()
                                    .asString("%.0f"),
                            " Days"
                    )
            );
        }

        if (pieChartData.isEmpty()) {
            attendanceChart.setTitle(
                    months.getValue()
                            + " - No Attendance Data"
            );
        }

        /*
         * Ensure the PieChart has completed CSS and layout
         * before applying the colors to its legend symbols.
         */
        Platform.runLater(() -> {
            attendanceChart.applyCss();
            applyPieLegendColors();
        });
    }

    private void assignPieChartColor(
            PieChart.Data chartData,
            String color
    ) {
        chartData.nodeProperty().addListener(
                (observable, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(
                                "-fx-pie-color: "
                                        + color
                                        + ";"
                        );
                    }
                }
        );

        if (chartData.getNode() != null) {
            chartData.getNode().setStyle(
                    "-fx-pie-color: "
                            + color
                            + ";"
            );
        }
    }

    private void applyPieLegendColors() {
        setLegendColor(
                attendanceChart,
                "Present",
                PRESENT_COLOR
        );

        setLegendColor(
                attendanceChart,
                "Absent",
                ABSENT_COLOR
        );

        setLegendColor(
                attendanceChart,
                "No Attendance",
                NO_ATTENDANCE_COLOR
        );
    }

    public void setStudentName(
            String studentName
    ) {
        nameLabel.setText(
                studentName != null
                        && !studentName.isBlank()
                        ? studentName
                        : "Name"
        );
    }

    @FXML
    private void showFullYearReport() {
        int reportYear =
                LocalDate.now().getYear();

        generateFullYearReport(reportYear);
    }

    private void generateFullYearReport(
            int reportYear
    ) {
        int[] presentByMonth = new int[12];
        int[] absentByMonth = new int[12];
        int[] noAttendanceByMonth = new int[12];

        int totalPresent = 0;
        int totalAbsent = 0;
        int totalNoAttendance = 0;
        int totalElapsedDays = 0;

        try {
            for (int monthNumber = 1;
                 monthNumber <= 12;
                 monthNumber++) {

                YearMonth reportMonth =
                        YearMonth.of(
                                reportYear,
                                monthNumber
                        );

                /*
                 * Future months are excluded.
                 */
                if (reportMonth.isAfter(YearMonth.now())) {
                    continue;
                }

                AttendanceReport report =
                        createAttendanceReport(
                                monthNumber
                        );

                report.init();

                LinkedHashMap<String, String> monthlyView =
                        report.getStudentAttendanceReport();

                AttendanceMonthData monthData =
                        calculateMonthData(
                                reportMonth,
                                monthlyView
                        );

                int index = monthNumber - 1;

                presentByMonth[index] =
                        monthData.presentDays();

                absentByMonth[index] =
                        monthData.absentDays();

                noAttendanceByMonth[index] =
                        monthData.noAttendanceDays();

                totalPresent +=
                        monthData.presentDays();

                totalAbsent +=
                        monthData.absentDays();

                totalNoAttendance +=
                        monthData.noAttendanceDays();

                totalElapsedDays +=
                        monthData.elapsedDays();

                printMonthData(monthData);
            }

            openFullYearReportWindow(
                    reportYear,
                    presentByMonth,
                    absentByMonth,
                    noAttendanceByMonth,
                    totalPresent,
                    totalAbsent,
                    totalNoAttendance,
                    totalElapsedDays
            );

        } catch (Exception exception) {
            exception.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Yearly Report Error",
                    "Could not generate the full-year "
                            + "attendance report.\n\n"
                            + getExceptionMessage(exception)
            );
        }
    }

    private void openFullYearReportWindow(
            int reportYear,
            int[] presentByMonth,
            int[] absentByMonth,
            int[] noAttendanceByMonth,
            int totalPresent,
            int totalAbsent,
            int totalNoAttendance,
            int totalElapsedDays
    ) {
        CategoryAxis monthAxis =
                new CategoryAxis();

        monthAxis.setLabel("Month");

        NumberAxis daysAxis =
                new NumberAxis();

        daysAxis.setLabel("Number of Days");
        daysAxis.setForceZeroInRange(true);
        daysAxis.setMinorTickVisible(false);
        daysAxis.setTickUnit(1);

        BarChart<String, Number> yearlyChart =
                new BarChart<>(
                        monthAxis,
                        daysAxis
                );

        yearlyChart.setTitle(
                "Monthly Attendance Details - "
                        + reportYear
        );

        yearlyChart.setAnimated(false);
        yearlyChart.setCategoryGap(10);
        yearlyChart.setBarGap(2);

        XYChart.Series<String, Number> presentSeries =
                new XYChart.Series<>();

        presentSeries.setName("Present");

        XYChart.Series<String, Number> absentSeries =
                new XYChart.Series<>();

        absentSeries.setName("Absent");

        XYChart.Series<String, Number> noAttendanceSeries =
                new XYChart.Series<>();

        noAttendanceSeries.setName(
                "No Attendance"
        );

        YearMonth currentMonth =
                YearMonth.now();

        for (int monthNumber = 1;
             monthNumber <= 12;
             monthNumber++) {

            YearMonth chartMonth =
                    YearMonth.of(
                            reportYear,
                            monthNumber
                    );

            /*
             * Do not add future months to the chart.
             */
            if (chartMonth.isAfter(currentMonth)) {
                continue;
            }

            int index = monthNumber - 1;

            String monthName =
                    Month.of(monthNumber)
                            .getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.ENGLISH
                            );

            XYChart.Data<String, Number> presentData =
                    new XYChart.Data<>(
                            monthName,
                            presentByMonth[index]
                    );

            assignBarColor(
                    presentData,
                    PRESENT_COLOR
            );

            presentSeries.getData().add(
                    presentData
            );

            XYChart.Data<String, Number> absentData =
                    new XYChart.Data<>(
                            monthName,
                            absentByMonth[index]
                    );

            assignBarColor(
                    absentData,
                    ABSENT_COLOR
            );

            absentSeries.getData().add(
                    absentData
            );

            XYChart.Data<String, Number> noAttendanceData =
                    new XYChart.Data<>(
                            monthName,
                            noAttendanceByMonth[index]
                    );

            assignBarColor(
                    noAttendanceData,
                    NO_ATTENDANCE_COLOR
            );

            noAttendanceSeries.getData().add(
                    noAttendanceData
            );
        }

        yearlyChart.getData().addAll(
                presentSeries,
                absentSeries,
                noAttendanceSeries
        );

        /*
         * Exclude No Attendance from the attendance percentage.
         */
        int recordedDays =
                totalPresent + totalAbsent;

        double attendancePercentage =
                recordedDays == 0
                        ? 0
                        : totalPresent
                        * 100.0
                        / recordedDays;

        Label studentLabel = new Label(
                name != null && !name.isBlank()
                        ? name
                        : "Student"
        );

        studentLabel.setStyle(
                "-fx-font-size: 22px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #172033;"
        );

        Label enrollmentLabel = new Label(
                "Enrollment No: " + edNo
        );

        enrollmentLabel.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: #555555;"
        );

        Label yearLabel = new Label(
                "Attendance Report for "
                        + reportYear
        );

        yearLabel.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: #555555;"
        );

        VBox headingBox = new VBox(
                4,
                studentLabel,
                enrollmentLabel,
                yearLabel
        );

        headingBox.setAlignment(Pos.CENTER);

        Label presentLabel =
                createSummaryLabel(
                        "Present",
                        String.valueOf(totalPresent),
                        PRESENT_COLOR
                );

        Label absentLabel =
                createSummaryLabel(
                        "Absent",
                        String.valueOf(totalAbsent),
                        ABSENT_COLOR
                );

        Label noAttendanceLabel =
                createSummaryLabel(
                        "No Attendance",
                        String.valueOf(totalNoAttendance),
                        NO_ATTENDANCE_COLOR
                );

        Label percentageLabel =
                createSummaryLabel(
                        "Attendance",
                        String.format(
                                Locale.ENGLISH,
                                "%.2f%%",
                                attendancePercentage
                        ),
                        getPercentageColor(
                                attendancePercentage
                        )
                );

        HBox summaryBox = new HBox(
                12,
                presentLabel,
                absentLabel,
                noAttendanceLabel,
                percentageLabel
        );

        summaryBox.setAlignment(Pos.CENTER);

        Label noteLabel = new Label(
                "Elapsed days: "
                        + totalElapsedDays
                        + " | Recorded days: "
                        + recordedDays
                        + " | Months with no student attendance "
                        + "are marked as No Attendance."
        );

        noteLabel.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #666666;"
        );

        Button closeButton =
                new Button("Close");

        closeButton.setPrefWidth(160);
        closeButton.getStyleClass()
                .add("accent");

        VBox root = new VBox(
                14,
                headingBox,
                yearlyChart,
                summaryBox,
                noteLabel,
                closeButton
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        root.setStyle(
                "-fx-background-color: #fafafa;"
        );

        VBox.setVgrow(
                yearlyChart,
                Priority.ALWAYS
        );

        Stage reportStage = new Stage();

        reportStage.setTitle(
                "Full Year Attendance Report - "
                        + reportYear
        );

        reportStage.initModality(
                Modality.APPLICATION_MODAL
        );

        if (attendanceChart.getScene() != null
                && attendanceChart
                .getScene()
                .getWindow() != null) {

            reportStage.initOwner(
                    attendanceChart
                            .getScene()
                            .getWindow()
            );
        }

        Scene scene = new Scene(
                root,
                1000,
                700
        );

        reportStage.setScene(scene);
        reportStage.setMinWidth(850);
        reportStage.setMinHeight(600);

        /*
         * Apply legend colors after JavaFX has created
         * the chart legend nodes.
         */
        Platform.runLater(() -> {
            root.applyCss();
            root.layout();

            applyBarColorsAgain(
                    presentSeries,
                    PRESENT_COLOR
            );

            applyBarColorsAgain(
                    absentSeries,
                    ABSENT_COLOR
            );

            applyBarColorsAgain(
                    noAttendanceSeries,
                    NO_ATTENDANCE_COLOR
            );

            setLegendColor(
                    yearlyChart,
                    "Present",
                    PRESENT_COLOR
            );

            setLegendColor(
                    yearlyChart,
                    "Absent",
                    ABSENT_COLOR
            );

            setLegendColor(
                    yearlyChart,
                    "No Attendance",
                    NO_ATTENDANCE_COLOR
            );
        });

        closeButton.setOnAction(
                event -> reportStage.close()
        );

        reportStage.showAndWait();
    }

    private void assignBarColor(
            XYChart.Data<String, Number> chartData,
            String color
    ) {
        chartData.nodeProperty().addListener(
                (observable, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle(
                                "-fx-bar-fill: "
                                        + color
                                        + ";"
                        );
                    }
                }
        );

        if (chartData.getNode() != null) {
            chartData.getNode().setStyle(
                    "-fx-bar-fill: "
                            + color
                            + ";"
            );
        }
    }

    private void applyBarColorsAgain(
            XYChart.Series<String, Number> series,
            String color
    ) {
        for (XYChart.Data<String, Number> data
                : series.getData()) {

            if (data.getNode() != null) {
                data.getNode().setStyle(
                        "-fx-bar-fill: "
                                + color
                                + ";"
                );
            }
        }
    }

    private void setLegendColor(
            Chart chart,
            String itemName,
            String color
    ) {
        if (chart == null) {
            return;
        }

        for (Node legendItem : chart.lookupAll(".chart-legend-item")) {
            if (!(legendItem instanceof Label legendLabel)) {
                continue;
            }

            String legendText = legendLabel.getText();

            /*
             * Pie-chart labels may contain values, for example:
             * "Present: 7 Days"
             *
             * Bar-chart legend labels normally contain:
             * "Present"
             */
            if (legendText == null ||
                    !legendText.startsWith(itemName)) {
                continue;
            }

            Node symbol = legendLabel.getGraphic();

            if (symbol != null) {
                symbol.setStyle(
                        "-fx-background-color: " +
                                color + ", " + color + ";"
                );
            }
        }
    }

    private Label createSummaryLabel(
            String title,
            String value,
            String color
    ) {
        Label label = new Label(
                title + "\n" + value
        );

        label.setAlignment(Pos.CENTER);
        label.setPrefSize(180, 75);

        label.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-color: "
                        + color
                        + ";"
                        + "-fx-border-radius: 8;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + color
                        + ";"
                        + "-fx-padding: 8;"
        );

        return label;
    }

    private String getPercentageColor(
            double percentage
    ) {
        if (percentage >= 75) {
            return PRESENT_COLOR;
        }

        if (percentage >= 60) {
            return NO_ATTENDANCE_COLOR;
        }

        return ABSENT_COLOR;
    }

    private String getFullMonthName(
            int monthNumber
    ) {
        return Month.of(monthNumber)
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                );
    }

    private String getExceptionMessage(
            Exception exception
    ) {
        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return exception.getMessage();
    }

    private void showAlert(
            Alert.AlertType alertType,
            String title,
            String message
    ) {
        Alert alert = new Alert(alertType);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (attendanceChart.getScene() != null
                && attendanceChart
                .getScene()
                .getWindow() != null) {

            alert.initOwner(
                    attendanceChart
                            .getScene()
                            .getWindow()
            );
        }

        alert.showAndWait();
    }

    /**
     * Stores the final validated attendance values
     * for one month.
     */
    private record AttendanceMonthData(
            YearMonth reportMonth,
            int elapsedDays,
            int presentDays,
            int absentDays,
            int noAttendanceDays,
            int rawMissingDays
    ) {
    }
}