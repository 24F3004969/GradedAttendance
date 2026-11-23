package org.graded_classes.graded_attendance.controller.syllabus;

import com.dlsc.gemsfx.SearchField;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SyllabusAnalysisController {

    @FXML private BorderPane root;
    @FXML private SplitPane mainSplitPane;
    @FXML private Button allStudentsButton;
    @FXML private Button needsAttentionButton;
    @FXML private ComboBox<String> sessionComboBox;
    @FXML private ComboBox<String> schoolComboBox;
    @FXML private ComboBox<String> boardComboBox;
    @FXML private ComboBox<String> classComboBox;
    @FXML private ComboBox<String> termComboBox;
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> topicStatusFilterComboBox;
    @FXML private SearchField<StudentData> studentSearchField;
    @FXML private ListView<StudentData> studentListView;
    @FXML private ListView<SyllabusTopic> attentionTopicsListView;
    @FXML private TableView<SyllabusTopic> syllabusTableView;
    @FXML private TableColumn<SyllabusTopic, String> chapterColumn;
    @FXML private TableColumn<SyllabusTopic, String> topicColumn;
    @FXML private TableColumn<SyllabusTopic, LocalDate> targetDateColumn;
    @FXML private TableColumn<SyllabusTopic, String> statusColumn;
    @FXML private TableColumn<SyllabusTopic, Double> progressColumn;
    @FXML private TableColumn<SyllabusTopic, String> scoreColumn;
    @FXML private TableColumn<SyllabusTopic, Void> actionColumn;
    @FXML private TextField topicSearchField;
    @FXML private TextArea teacherNotesTextArea;
    @FXML private CheckBox revisionRequiredCheckBox;
    @FXML private CheckBox extraPracticeCheckBox;
    @FXML private CheckBox parentFollowUpCheckBox;
    @FXML private PieChart topicDistributionChart;
    @FXML private Label assessmentAverageLabel;
    @FXML private Label assessmentCountLabel;
    @FXML private Label attentionTopicCountLabel;
    @FXML private Label classAverageLabel;
    @FXML private Label classCompletionLabel;
    @FXML private Label completedTopicsLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private Label notesSavedLabel;
    @FXML private Label remainingTopicsLabel;
    @FXML private Label selectedStudentClassLabel;
    @FXML private Label selectedStudentEnrollmentLabel;
    @FXML private Label selectedStudentNameLabel;
    @FXML private Label selectedStudentSchoolLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Label studentComparisonLabel;
    @FXML private Label studentCompletionLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label studentInitialsLabel;
    @FXML private Label studentStatusLabel;
    @FXML private Label studentsOnTrackLabel;
    @FXML private Label syllabusDescriptionLabel;
    @FXML private Label syllabusProgressLabel;
    @FXML private Label syllabusTitleLabel;
    @FXML private Label topicResultCountLabel;
    @FXML private ProgressBar classAverageProgress;
    @FXML private ProgressBar classCompletionProgress;
    @FXML private ProgressBar studentComparisonProgress;
    @FXML private ProgressBar studentCompletionProgress;

    private final ObservableList<StudentData> allStudents = FXCollections.observableArrayList();
    private final FilteredList<StudentData> filteredStudents = new FilteredList<>(allStudents, student -> true);
    private final ObservableList<SyllabusTopic> allTopics = FXCollections.observableArrayList();
    private final FilteredList<SyllabusTopic> filteredTopics = new FilteredList<>(allTopics, topic -> true);
    private final ObservableList<SyllabusTopic> attentionTopics = FXCollections.observableArrayList();
    private final Map<String, StudentSyllabusData> studentProgress = new HashMap<>();
    private StudentData selectedStudent;
    private boolean showingNeedsAttentionOnly;
    private boolean updatingFilters;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    private void initialize() {
        configureComboBoxes();
        configureStudentList();
        configureTopicTable();
        configureAttentionList();
        configureListeners();
        loadPrototypeData();
        studentListView.setItems(filteredStudents);
        syllabusTableView.setItems(filteredTopics);
        attentionTopicsListView.setItems(attentionTopics);
        if (!allStudents.isEmpty()) { studentListView.getSelectionModel().selectFirst(); }
        updateStudentCount();
        updateClassSummary();
        statusMessageLabel.setText("Prototype syllabus data loaded");
    }

    private void configureComboBoxes() {
        sessionComboBox.setItems(FXCollections.observableArrayList("2025-2026", "2026-2027"));
        schoolComboBox.setItems(FXCollections.observableArrayList("All Schools", "Delhi Public School", "St. Xavier's School", "DAV Public School", "Loyola School"));
        boardComboBox.setItems(FXCollections.observableArrayList("All Boards", "CBSE", "ICSE"));
        classComboBox.setItems(FXCollections.observableArrayList("All Classes", "VI", "VII", "VIII", "IX", "X"));
        termComboBox.setItems(FXCollections.observableArrayList("All Terms", "Unit Test 1", "Mid Term", "Unit Test 2", "Final Term"));
        subjectComboBox.setItems(FXCollections.observableArrayList("All Subjects", "Mathematics", "Physics", "Chemistry", "Biology", "English", "Computer"));
        topicStatusFilterComboBox.setItems(FXCollections.observableArrayList("All Statuses", TopicStatus.COMPLETED.displayName, TopicStatus.IN_PROGRESS.displayName, TopicStatus.NEEDS_REVISION.displayName, TopicStatus.NOT_STARTED.displayName));
        sessionComboBox.getSelectionModel().select("2026-2027");
        schoolComboBox.getSelectionModel().select("All Schools");
        boardComboBox.getSelectionModel().select("All Boards");
        classComboBox.getSelectionModel().select("All Classes");
        termComboBox.getSelectionModel().select("All Terms");
        subjectComboBox.getSelectionModel().select("All Subjects");
        topicStatusFilterComboBox.getSelectionModel().select("All Statuses");
    }

    private void configureStudentList() {
        studentListView.setCellFactory(list -> new ListCell<>() {
            private final Label nameLabel = new Label();
            private final Label detailsLabel = new Label();
            private final Label completionLabel = new Label();
            private final ProgressBar progressBar = new ProgressBar();
            private final VBox textBox = new VBox(2, nameLabel, detailsLabel);
            private final VBox progressBox = new VBox(3, completionLabel, progressBar);
            private final HBox container = new HBox(10, textBox, createSpacer(), progressBox);
            {
                nameLabel.getStyleClass().add("text-bold");
                detailsLabel.getStyleClass().add("text-muted");
                completionLabel.getStyleClass().add("text-muted");
                progressBar.setPrefWidth(70);
                progressBar.setMaxWidth(70);
                progressBox.setAlignment(Pos.CENTER_RIGHT);
                container.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(StudentData student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) { setGraphic(null); setText(null); return; }
                StudentSyllabusData data = studentProgress.get(student.enrollmentNumber());
                double progress = data == null ? 0 : calculateCompletion(data.topics());
                nameLabel.setText(student.name());
                detailsLabel.setText(student.enrollmentNumber() + " • Class " + student.studentClass() + " • " + student.board());
                completionLabel.setText(toPercentage(progress));
                progressBar.setProgress(progress);
                setGraphic(container);
            }
        });
    }

    private void configureAttentionList() {
        attentionTopicsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SyllabusTopic topic, boolean empty) {
                super.updateItem(topic, empty);
                if (empty || topic == null) { setText(null); setGraphic(null); return; }
                setText(topic.topicName() + "\n" + topic.chapter() + " • " + topic.status().displayName);
                setWrapText(true);
            }
        });
    }

    private void configureTopicTable() {
        chapterColumn.setCellValueFactory(data -> data.getValue().chapterProperty());
        topicColumn.setCellValueFactory(data -> data.getValue().topicNameProperty());
        targetDateColumn.setCellValueFactory(data -> data.getValue().targetDateProperty());
        targetDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : DATE_FORMATTER.format(date));
            }
        });
        statusColumn.setCellValueFactory(data -> data.getValue().statusTextProperty());
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("success", "warning", "accent", "text-muted");
                if (empty || status == null) { setText(null); return; }
                setText(status);
                if (status.equals(TopicStatus.COMPLETED.displayName)) { getStyleClass().add("success"); }
                else if (status.equals(TopicStatus.IN_PROGRESS.displayName)) { getStyleClass().add("accent"); }
                else if (status.equals(TopicStatus.NEEDS_REVISION.displayName)) { getStyleClass().add("warning"); }
                else { getStyleClass().add("text-muted"); }
            }
        });
        progressColumn.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        progressColumn.setCellFactory(ProgressBarTableCell.forTableColumn());
        scoreColumn.setCellValueFactory(data -> data.getValue().scoreTextProperty());
        configureActionColumn();
    }

    private void configureActionColumn() {
        actionColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<SyllabusTopic, Void> call(TableColumn<SyllabusTopic, Void> column) {
                return new TableCell<>() {
                    private final Button button = new Button("Update");
                    {
                        button.getStyleClass().addAll("small", "button-outlined");
                        button.setOnAction(event -> { SyllabusTopic topic = getTableView().getItems().get(getIndex()); showTopicUpdateDialog(topic); });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : button);
                    }
                };
            }
        });
    }

    private void configureListeners() {
        studentListView.getSelectionModel().selectedItemProperty().addListener((observable, oldStudent, newStudent) -> { if (newStudent != null) { showStudent(newStudent); } });
        topicStatusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyTopicFilter());
        topicSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyTopicFilter());
        topicDistributionChart.setLegendVisible(true);
        topicDistributionChart.setLabelsVisible(false);
    }

    private void loadPrototypeData() {
        StudentData student1 = new StudentData("ED092", "Aarav Kumar", "VIII", "CBSE", "Delhi Public School");
        StudentData student2 = new StudentData("ED083", "Sara Khan", "VIII", "CBSE", "Delhi Public School");
        StudentData student3 = new StudentData("ED071", "Aditya Raj", "IX", "ICSE", "St. Xavier's School");
        StudentData student4 = new StudentData("ED047", "Ananya Singh", "VII", "CBSE", "DAV Public School");
        StudentData student5 = new StudentData("ED079", "Rohan Das", "VIII", "ICSE", "Loyola School");
        StudentData student6 = new StudentData("ED008", "Meera Sharma", "IX", "CBSE", "Delhi Public School");
        allStudents.addAll(student1, student2, student3, student4, student5, student6);
        studentProgress.put(student1.enrollmentNumber(), createStudentSyllabus(student1, 0, false, false, false));
        studentProgress.put(student2.enrollmentNumber(), createStudentSyllabus(student2, 1, true, true, false));
        studentProgress.put(student3.enrollmentNumber(), createStudentSyllabus(student3, 2, true, false, true));
        studentProgress.put(student4.enrollmentNumber(), createStudentSyllabus(student4, 3, false, true, false));
        studentProgress.put(student5.enrollmentNumber(), createStudentSyllabus(student5, 4, true, true, true));
        studentProgress.put(student6.enrollmentNumber(), createStudentSyllabus(student6, 0, false, false, false));
    }

    private StudentSyllabusData createStudentSyllabus(StudentData student, int variation, boolean revisionRequired, boolean extraPractice, boolean parentFollowUp) {
        List<SyllabusTopic> topics = new ArrayList<>();
        topics.add(new SyllabusTopic("Number System", "Rational Numbers", LocalDate.now().plusDays(2), TopicStatus.COMPLETED, 1.0, 17 + variation, 20));
        topics.add(new SyllabusTopic("Number System", "Exponents and Powers", LocalDate.now().plusDays(5), variation >= 3 ? TopicStatus.NEEDS_REVISION : TopicStatus.COMPLETED, variation >= 3 ? 0.45 : 1.0, 14 - variation, 20));
        topics.add(new SyllabusTopic("Algebra", "Algebraic Expressions", LocalDate.now().plusDays(9), TopicStatus.IN_PROGRESS, Math.max(0.30, 0.75 - variation * 0.08), 11 + variation, 20));
        topics.add(new SyllabusTopic("Algebra", "Linear Equations", LocalDate.now().plusDays(14), variation >= 2 ? TopicStatus.NEEDS_REVISION : TopicStatus.IN_PROGRESS, Math.max(0.20, 0.60 - variation * 0.07), 10 - variation, 20));
        topics.add(new SyllabusTopic("Geometry", "Understanding Quadrilaterals", LocalDate.now().plusDays(20), TopicStatus.NOT_STARTED, 0.0, null, null));
        topics.add(new SyllabusTopic("Geometry", "Practical Geometry", LocalDate.now().plusDays(26), TopicStatus.NOT_STARTED, 0.0, null, null));
        topics.add(new SyllabusTopic("Mensuration", "Area of Plane Figures", LocalDate.now().plusDays(31), TopicStatus.NOT_STARTED, 0.0, null, null));
        return new StudentSyllabusData(student, "2026-2027", "Mid Term", "Mathematics", topics, "", revisionRequired, extraPractice, parentFollowUp, LocalDate.now());
    }

    private void showStudent(StudentData student) {
        selectedStudent = student;
        StudentSyllabusData data = studentProgress.get(student.enrollmentNumber());
        selectedStudentNameLabel.setText(student.name());
        selectedStudentEnrollmentLabel.setText("Enrollment: " + student.enrollmentNumber());
        selectedStudentClassLabel.setText("Class: " + student.studentClass() + " " + student.board());
        selectedStudentSchoolLabel.setText("School: " + student.school());
        studentInitialsLabel.setText(createInitials(student.name()));
        if (data == null) { clearStudentAnalysis(); return; }
        allTopics.setAll(data.topics());
        syllabusTitleLabel.setText(data.subject() + " • " + data.term());
        syllabusDescriptionLabel.setText(student.school() + " • Class " + student.studentClass() + " " + student.board() + " • " + data.session());
        teacherNotesTextArea.setText(data.teacherNotes());
        revisionRequiredCheckBox.setSelected(data.revisionRequired());
        extraPracticeCheckBox.setSelected(data.extraPracticeRequired());
        parentFollowUpCheckBox.setSelected(data.parentFollowUpRequired());
        lastUpdatedLabel.setText("Updated " + DATE_FORMATTER.format(data.lastUpdated()));
        applyTopicFilter();
        updateStudentAnalysis(data);
        statusMessageLabel.setText("Showing syllabus analysis for " + student.name());
    }

    private void clearStudentAnalysis() {
        allTopics.clear();
        attentionTopics.clear();
        studentCompletionLabel.setText("0%");
        studentCompletionProgress.setProgress(0);
        completedTopicsLabel.setText("0 / 0");
        remainingTopicsLabel.setText("0 remaining");
        assessmentAverageLabel.setText("--");
        assessmentCountLabel.setText("No assessments");
        studentStatusLabel.setText("No syllabus");
        lastUpdatedLabel.setText("Not updated");
        topicDistributionChart.getData().clear();
        teacherNotesTextArea.clear();
        revisionRequiredCheckBox.setSelected(false);
        extraPracticeCheckBox.setSelected(false);
        parentFollowUpCheckBox.setSelected(false);
    }

    private void updateStudentAnalysis(StudentSyllabusData data) {
        List<SyllabusTopic> topics = data.topics();
        int total = topics.size();
        long completed = topics.stream().filter(topic -> topic.status() == TopicStatus.COMPLETED).count();
        long inProgress = topics.stream().filter(topic -> topic.status() == TopicStatus.IN_PROGRESS).count();
        long needsRevision = topics.stream().filter(topic -> topic.status() == TopicStatus.NEEDS_REVISION).count();
        long notStarted = topics.stream().filter(topic -> topic.status() == TopicStatus.NOT_STARTED).count();
        double completion = calculateCompletion(topics);
        studentCompletionLabel.setText(toPercentage(completion));
        studentCompletionProgress.setProgress(completion);
        completedTopicsLabel.setText(completed + " / " + total);
        remainingTopicsLabel.setText((total - completed) + " remaining");
        syllabusProgressLabel.setText(completed + " of " + total + " topics completed");
        List<SyllabusTopic> assessedTopics = topics.stream().filter(topic -> topic.marksObtained() != null && topic.totalMarks() != null && topic.totalMarks() > 0).toList();
        double assessmentAverage = assessedTopics.stream().mapToDouble(topic -> topic.marksObtained() / (double) topic.totalMarks()).average().orElse(0);
        if (assessedTopics.isEmpty()) { assessmentAverageLabel.setText("--"); assessmentCountLabel.setText("No assessments"); }
        else { assessmentAverageLabel.setText(toPercentage(assessmentAverage)); assessmentCountLabel.setText(assessedTopics.size() + (assessedTopics.size() == 1 ? " assessment" : " assessments")); }
        studentStatusLabel.setText(getStudentStatus(completion, needsRevision));
        attentionTopics.setAll(topics.stream().filter(topic -> topic.status() == TopicStatus.NEEDS_REVISION || topic.progress() < 0.50).toList());
        attentionTopicCountLabel.setText(String.valueOf(attentionTopics.size()));
        topicDistributionChart.setData(FXCollections.observableArrayList(new PieChart.Data("Completed", completed), new PieChart.Data("In progress", inProgress), new PieChart.Data("Needs revision", needsRevision), new PieChart.Data("Not started", notStarted)));
        updateComparison(completion);
        updateTopicCount();
    }

    private void updateComparison(double studentCompletion) {
        double classAverage = allStudents.stream().map(studentProgress::get).filter(Objects::nonNull).mapToDouble(data -> calculateCompletion(data.topics())).average().orElse(0);
        studentComparisonLabel.setText(toPercentage(studentCompletion));
        studentComparisonProgress.setProgress(studentCompletion);
        classAverageLabel.setText(toPercentage(classAverage));
        classAverageProgress.setProgress(classAverage);
    }

    @FXML
    private void filterChanged(ActionEvent event) {
        if (updatingFilters) { return; }
        applyStudentFilter();
        if (!filteredStudents.isEmpty()) { studentListView.getSelectionModel().selectFirst(); }
        else { selectedStudent = null; clearStudentAnalysis(); statusMessageLabel.setText("No students match the selected filters"); }
    }

    private void applyStudentFilter() {
        String school = schoolComboBox.getValue();
        String board = boardComboBox.getValue();
        String studentClass = classComboBox.getValue();
        String term = termComboBox.getValue();
        String subject = subjectComboBox.getValue();
        String session = sessionComboBox.getValue();
        filteredStudents.setPredicate(student -> {
            StudentSyllabusData data = studentProgress.get(student.enrollmentNumber());
            boolean matchesSchool = school == null || school.equals("All Schools") || school.equals(student.school());
            boolean matchesBoard = board == null || board.equals("All Boards") || board.equals(student.board());
            boolean matchesClass = studentClass == null || studentClass.equals("All Classes") || studentClass.equals(student.studentClass());
            boolean matchesSession = data == null || session == null || session.equals(data.session());
            boolean matchesTerm = data == null || term == null || term.equals("All Terms") || term.equals(data.term());
            boolean matchesSubject = data == null || subject == null || subject.equals("All Subjects") || subject.equals(data.subject());
            boolean matchesAttention = !showingNeedsAttentionOnly || studentNeedsAttention(student);
            return matchesSchool && matchesBoard && matchesClass && matchesSession && matchesTerm && matchesSubject && matchesAttention;
        });
        updateStudentCount();
        updateClassSummary();
    }

    @FXML
    private void filterTopics(KeyEvent event) { applyTopicFilter(); }

    @FXML
    private void filterTopics(ActionEvent event) { applyTopicFilter(); }

    private void applyTopicFilter() {
        String text = topicSearchField.getText() == null ? "" : topicSearchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = topicStatusFilterComboBox.getValue();
        filteredTopics.setPredicate(topic -> {
            boolean matchesText = text.isBlank() || topic.topicName().toLowerCase(Locale.ROOT).contains(text) || topic.chapter().toLowerCase(Locale.ROOT).contains(text);
            boolean matchesStatus = status == null || status.equals("All Statuses") || status.equals(topic.status().displayName);
            return matchesText && matchesStatus;
        });
        updateTopicCount();
    }

    @FXML
    private void showAllStudents(ActionEvent event) {
        showingNeedsAttentionOnly = false;
        allStudentsButton.getStyleClass().add("accent");
        needsAttentionButton.getStyleClass().remove("accent");
        applyStudentFilter();
    }

    @FXML
    private void showStudentsNeedingAttention(ActionEvent event) {
        showingNeedsAttentionOnly = true;
        needsAttentionButton.getStyleClass().add("accent");
        allStudentsButton.getStyleClass().remove("accent");
        applyStudentFilter();
    }

    private boolean studentNeedsAttention(StudentData student) {
        StudentSyllabusData data = studentProgress.get(student.enrollmentNumber());
        if (data == null) { return true; }
        return data.revisionRequired() || data.extraPracticeRequired() || data.parentFollowUpRequired() || data.topics().stream().anyMatch(topic -> topic.status() == TopicStatus.NEEDS_REVISION);
    }

    @FXML
    private void refreshStudents(ActionEvent event) {
        studentListView.refresh();
        syllabusTableView.refresh();
        applyStudentFilter();
        updateClassSummary();
        statusMessageLabel.setText("Student and syllabus data refreshed");
    }

    @FXML
    private void showAddSyllabus(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Syllabus");
        dialog.setHeaderText("Create a syllabus for a class, school and examination");
        ComboBox<String> school = new ComboBox<>(schoolComboBox.getItems().filtered(value -> !value.equals("All Schools")));
        ComboBox<String> board = new ComboBox<>(boardComboBox.getItems().filtered(value -> !value.equals("All Boards")));
        ComboBox<String> studentClass = new ComboBox<>(classComboBox.getItems().filtered(value -> !value.equals("All Classes")));
        ComboBox<String> term = new ComboBox<>(termComboBox.getItems().filtered(value -> !value.equals("All Terms")));
        ComboBox<String> subject = new ComboBox<>(subjectComboBox.getItems().filtered(value -> !value.equals("All Subjects")));
        ComboBox<String> session = new ComboBox<>(sessionComboBox.getItems());
        school.setPromptText("School");
        board.setPromptText("Board");
        studentClass.setPromptText("Class");
        term.setPromptText("Exam / term");
        subject.setPromptText("Subject");
        session.setPromptText("Session");
        school.setMaxWidth(Double.MAX_VALUE);
        board.setMaxWidth(Double.MAX_VALUE);
        studentClass.setMaxWidth(Double.MAX_VALUE);
        term.setMaxWidth(Double.MAX_VALUE);
        subject.setMaxWidth(Double.MAX_VALUE);
        session.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(10, new Label("Academic session"), session, new Label("School"), school, new Label("Board"), board, new Label("Class"), studentClass, new Label("Exam / term"), term, new Label("Subject"), subject);
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Create Syllabus", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.showAndWait().filter(result -> result.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(result -> {
            syllabusTitleLabel.setText(safeValue(subject.getValue(), "New subject") + " • " + safeValue(term.getValue(), "New term"));
            syllabusDescriptionLabel.setText(safeValue(school.getValue(), "School") + " • Class " + safeValue(studentClass.getValue(), "--") + " " + safeValue(board.getValue(), "") + " • " + safeValue(session.getValue(), "Session"));
            statusMessageLabel.setText("New syllabus prototype created");
        });
    }

    @FXML
    private void showAddTopic(ActionEvent event) {
        if (selectedStudent == null) { showWarning("No student selected", "Select a student before adding a topic."); return; }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Syllabus Topic");
        dialog.setHeaderText("Add a topic to the selected syllabus");
        TextField chapterField = new TextField();
        chapterField.setPromptText("Example: Algebra");
        TextField topicField = new TextField();
        topicField.setPromptText("Example: Linear Equations");
        DatePicker targetDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        VBox content = new VBox(10, new Label("Chapter"), chapterField, new Label("Topic"), topicField, new Label("Target completion date"), targetDatePicker);
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Add Topic", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        Button addButton = (Button) dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        addButton.disableProperty().bind(chapterField.textProperty().isEmpty().or(topicField.textProperty().isEmpty()));
        dialog.showAndWait().filter(result -> result.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(result -> {
            StudentSyllabusData data = studentProgress.get(selectedStudent.enrollmentNumber());
            SyllabusTopic topic = new SyllabusTopic(chapterField.getText().trim(), topicField.getText().trim(), targetDatePicker.getValue(), TopicStatus.NOT_STARTED, 0, null, null);
            data.topics().add(topic);
            allTopics.add(topic);
            updateStudentAnalysis(data);
            updateClassSummary();
            syllabusTableView.refresh();
            statusMessageLabel.setText("Topic added: " + topic.topicName());
        });
    }

    @FXML
    private void updateStudentProgress(ActionEvent event) {
        SyllabusTopic selectedTopic = syllabusTableView.getSelectionModel().getSelectedItem();
        if (selectedTopic == null) { showWarning("No topic selected", "Select a syllabus topic from the table."); return; }
        showTopicUpdateDialog(selectedTopic);
    }

    private void showTopicUpdateDialog(SyllabusTopic topic) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Topic Progress");
        dialog.setHeaderText(topic.topicName());
        ComboBox<TopicStatus> statusComboBox = new ComboBox<>(FXCollections.observableArrayList(TopicStatus.values()));
        statusComboBox.setValue(topic.status());
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        Slider progressSlider = new Slider(0, 100, topic.progress() * 100);
        progressSlider.setShowTickLabels(true);
        progressSlider.setShowTickMarks(true);
        progressSlider.setMajorTickUnit(25);
        progressSlider.setBlockIncrement(5);
        Label progressValueLabel = new Label();
        progressValueLabel.textProperty().bind(progressSlider.valueProperty().asString("%.0f%%"));
        TextField marksObtainedField = new TextField(topic.marksObtained() == null ? "" : topic.marksObtained().toString());
        TextField totalMarksField = new TextField(topic.totalMarks() == null ? "" : topic.totalMarks().toString());
        marksObtainedField.setPromptText("Optional");
        totalMarksField.setPromptText("Optional");
        HBox progressHeader = new HBox(10, new Label("Progress"), createSpacer(), progressValueLabel);
        VBox content = new VBox(10, new Label("Status"), statusComboBox, progressHeader, progressSlider, new Label("Marks obtained"), marksObtainedField, new Label("Total marks"), totalMarksField);
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("Save Progress", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.showAndWait().filter(result -> result.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(result -> {
            topic.setStatus(statusComboBox.getValue());
            topic.setProgress(progressSlider.getValue() / 100.0);
            topic.setMarksObtained(parseOptionalInteger(marksObtainedField.getText()));
            topic.setTotalMarks(parseOptionalInteger(totalMarksField.getText()));
            StudentSyllabusData data = studentProgress.get(selectedStudent.enrollmentNumber());
            data.setLastUpdated(LocalDate.now());
            updateStudentAnalysis(data);
            updateClassSummary();
            syllabusTableView.refresh();
            studentListView.refresh();
            statusMessageLabel.setText("Progress updated for " + topic.topicName());
        });
    }

    @FXML
    private void saveTeacherNote(ActionEvent event) {
        if (selectedStudent == null) { showWarning("No student selected", "Select a student before saving a note."); return; }
        StudentSyllabusData data = studentProgress.get(selectedStudent.enrollmentNumber());
        data.setTeacherNotes(teacherNotesTextArea.getText());
        data.setLastUpdated(LocalDate.now());
        notesSavedLabel.setText("Saved");
        lastUpdatedLabel.setText("Updated " + DATE_FORMATTER.format(LocalDate.now()));
        statusMessageLabel.setText("Teacher note saved for " + selectedStudent.name());
    }

    @FXML
    private void updateTrackingFlags(ActionEvent event) {
        if (selectedStudent == null) { return; }
        StudentSyllabusData data = studentProgress.get(selectedStudent.enrollmentNumber());
        data.setRevisionRequired(revisionRequiredCheckBox.isSelected());
        data.setExtraPracticeRequired(extraPracticeCheckBox.isSelected());
        data.setParentFollowUpRequired(parentFollowUpCheckBox.isSelected());
        data.setLastUpdated(LocalDate.now());
        studentListView.refresh();
        applyStudentFilter();
        statusMessageLabel.setText("Tracking flags updated for " + selectedStudent.name());
    }

    @FXML
    private void openStudentReport(ActionEvent event) {
        if (selectedStudent == null) { showWarning("No student selected", "Select a student to view the report."); return; }
        StudentSyllabusData data = studentProgress.get(selectedStudent.enrollmentNumber());
        double completion = calculateCompletion(data.topics());
        long completed = data.topics().stream().filter(topic -> topic.status() == TopicStatus.COMPLETED).count();
        String report = "Student: " + selectedStudent.name() + "\n" + "Enrollment: " + selectedStudent.enrollmentNumber() + "\n" + "Class: " + selectedStudent.studentClass() + " " + selectedStudent.board() + "\n" + "School: " + selectedStudent.school() + "\n\n" + "Subject: " + data.subject() + "\n" + "Term: " + data.term() + "\n" + "Completion: " + toPercentage(completion) + "\n" + "Topics completed: " + completed + " of " + data.topics().size();
        showInformation("Student Syllabus Report", report);
    }

    @FXML
    private void openSyllabusManager(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Syllabus Manager");
        alert.setHeaderText("Prototype syllabus manager");
        alert.setContentText("The syllabus manager will contain:\n\n" + "• School and board-specific syllabuses\n" + "• Class and subject selection\n" + "• Term and examination mapping\n" + "• Chapter and topic management\n" + "• Syllabus copying between sessions");
        alert.showAndWait();
    }

    @FXML
    private void importSyllabus(ActionEvent event) {
        showInformation("Import Syllabus", "Prototype import action.\n\n" + "The production version can import syllabus " + "topics from Excel, CSV, or JSON.");
        statusMessageLabel.setText("Import syllabus action opened");
    }

    @FXML
    private void exportAnalysis(ActionEvent event) {
        if (selectedStudent == null) { showWarning("Nothing to export", "Select a student before exporting the analysis."); return; }
        showInformation("Export Analysis", "Prototype export prepared for " + selectedStudent.name() + ".\n\n" + "The production version can generate PDF " + "or Excel reports.");
        statusMessageLabel.setText("Analysis export prepared for " + selectedStudent.name());
    }

    private void updateClassSummary() {
        List<StudentSyllabusData> visibleData = filteredStudents.stream().map(student -> studentProgress.get(student.enrollmentNumber())).filter(Objects::nonNull).toList();
        double average = visibleData.stream().mapToDouble(data -> calculateCompletion(data.topics())).average().orElse(0);
        long onTrack = visibleData.stream().filter(data -> calculateCompletion(data.topics()) >= 0.60).count();
        classCompletionLabel.setText(toPercentage(average));
        classCompletionProgress.setProgress(average);
        studentsOnTrackLabel.setText(onTrack + " / " + visibleData.size());
    }

    private void updateStudentCount() {
        int count = filteredStudents.size();
        studentCountLabel.setText(count + (count == 1 ? " student" : " students"));
    }

    private void updateTopicCount() {
        int count = filteredTopics.size();
        topicResultCountLabel.setText(count + (count == 1 ? " topic" : " topics"));
    }

    private double calculateCompletion(List<SyllabusTopic> topics) {
        if (topics == null || topics.isEmpty()) { return 0; }
        return clamp(topics.stream().mapToDouble(SyllabusTopic::progress).average().orElse(0));
    }

    private String getStudentStatus(double completion, long needsRevision) {
        if (needsRevision > 0) { return "Needs attention"; }
        if (completion >= 0.80) { return "On track"; }
        if (completion >= 0.40) { return "In progress"; }
        return "Behind schedule";
    }

    private String createInitials(String name) {
        if (name == null || name.isBlank()) { return "ST"; }
        String[] components = name.trim().split("\\s+");
        if (components.length == 1) { return components[0].substring(0, Math.min(2, components[0].length())).toUpperCase(Locale.ROOT); }
        return (components[0].substring(0, 1) + components[components.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String toPercentage(double value) { return Math.round(clamp(value) * 100) + "%"; }

    private double clamp(double value) { return Math.max(0, Math.min(1, value)); }

    private Integer parseOptionalInteger(String value) {
        if (value == null || value.isBlank()) { return null; }
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException exception) { return null; }
    }

    private String safeValue(String value, String defaultValue) { return value == null || value.isBlank() ? defaultValue : value; }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Syllabus Analysis");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInformation(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Syllabus Analysis");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public record StudentData(String enrollmentNumber, String name, String studentClass, String board, String school) {
        @Override
        public String toString() { return name; }
    }

    public enum TopicStatus {
        COMPLETED("Completed"), IN_PROGRESS("In progress"), NEEDS_REVISION("Needs revision"), NOT_STARTED("Not started");
        private final String displayName;
        TopicStatus(String displayName) { this.displayName = displayName; }
        @Override
        public String toString() { return displayName; }
    }

    public static final class SyllabusTopic {
        private final StringProperty chapter = new SimpleStringProperty();
        private final StringProperty topicName = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> targetDate = new SimpleObjectProperty<>();
        private final ObjectProperty<TopicStatus> status = new SimpleObjectProperty<>();
        private final DoubleProperty progress = new SimpleDoubleProperty();
        private final ObjectProperty<Integer> marksObtained = new SimpleObjectProperty<>();
        private final ObjectProperty<Integer> totalMarks = new SimpleObjectProperty<>();

        public SyllabusTopic(String chapter, String topicName, LocalDate targetDate, TopicStatus status, double progress, Integer marksObtained, Integer totalMarks) {
            this.chapter.set(chapter);
            this.topicName.set(topicName);
            this.targetDate.set(targetDate);
            this.status.set(status);
            this.progress.set(progress);
            this.marksObtained.set(marksObtained);
            this.totalMarks.set(totalMarks);
        }

        public String chapter() { return chapter.get(); }
        public StringProperty chapterProperty() { return chapter; }
        public String topicName() { return topicName.get(); }
        public StringProperty topicNameProperty() { return topicName; }
        public LocalDate targetDate() { return targetDate.get(); }
        public ObjectProperty<LocalDate> targetDateProperty() { return targetDate; }
        public TopicStatus status() { return status.get(); }
        public void setStatus(TopicStatus value) { status.set(value); }
        public StringProperty statusTextProperty() { return new SimpleStringProperty(status() == null ? "" : status().displayName); }
        public double progress() { return progress.get(); }
        public void setProgress(double value) { progress.set(Math.max(0, Math.min(1, value))); }
        public DoubleProperty progressProperty() { return progress; }
        public Integer marksObtained() { return marksObtained.get(); }
        public void setMarksObtained(Integer value) { marksObtained.set(value); }
        public Integer totalMarks() { return totalMarks.get(); }
        public void setTotalMarks(Integer value) { totalMarks.set(value); }
        public StringProperty scoreTextProperty() { if (marksObtained() == null || totalMarks() == null) { return new SimpleStringProperty("--"); } return new SimpleStringProperty(marksObtained() + " / " + totalMarks()); }
        @Override
        public String toString() { return topicName(); }
    }

    public static final class StudentSyllabusData {
        private final StudentData student;
        private final String session;
        private final String term;
        private final String subject;
        private final List<SyllabusTopic> topics;
        private String teacherNotes;
        private boolean revisionRequired;
        private boolean extraPracticeRequired;
        private boolean parentFollowUpRequired;
        private LocalDate lastUpdated;

        public StudentSyllabusData(StudentData student, String session, String term, String subject, List<SyllabusTopic> topics, String teacherNotes, boolean revisionRequired, boolean extraPracticeRequired, boolean parentFollowUpRequired, LocalDate lastUpdated) {
            this.student = student;
            this.session = session;
            this.term = term;
            this.subject = subject;
            this.topics = topics;
            this.teacherNotes = teacherNotes;
            this.revisionRequired = revisionRequired;
            this.extraPracticeRequired = extraPracticeRequired;
            this.parentFollowUpRequired = parentFollowUpRequired;
            this.lastUpdated = lastUpdated;
        }

        public StudentData student() { return student; }
        public String session() { return session; }
        public String term() { return term; }
        public String subject() { return subject; }
        public List<SyllabusTopic> topics() { return topics; }
        public String teacherNotes() { return teacherNotes; }
        public void setTeacherNotes(String teacherNotes) { this.teacherNotes = teacherNotes; }
        public boolean revisionRequired() { return revisionRequired; }
        public void setRevisionRequired(boolean revisionRequired) { this.revisionRequired = revisionRequired; }
        public boolean extraPracticeRequired() { return extraPracticeRequired; }
        public void setExtraPracticeRequired(boolean extraPracticeRequired) { this.extraPracticeRequired = extraPracticeRequired; }
        public boolean parentFollowUpRequired() { return parentFollowUpRequired; }
        public void setParentFollowUpRequired(boolean parentFollowUpRequired) { this.parentFollowUpRequired = parentFollowUpRequired; }
        public LocalDate lastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}