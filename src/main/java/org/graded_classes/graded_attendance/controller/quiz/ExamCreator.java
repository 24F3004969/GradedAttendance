package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import impl.com.calendarfx.view.NumericTextField;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.ExamType;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExamCreator implements Initializable {
    private boolean updatingDifficultySpinners;
    public Spinner<Integer> easy;
    public Spinner<Integer> medium;
    public Spinner<Integer> hard;
    @FXML
    private ComboBox<String> classNum;
    @FXML
    private ComboBox<String> board;
    @FXML
    private Button close;
    @FXML
    private HBox topics;
    @FXML
    private TimePicker endTime;
    @FXML
    private DatePicker exam;
    @FXML
    private NumericTextField questionCount;
    @FXML
    private ComboBox<String> roomNo;

    @FXML
    private TimePicker startTime;

    @FXML
    private SearchField<String> subject;
    public ArrayList<String> topicList = new ArrayList<>();
    @FXML
    private SearchField<String> topicName;
    ObservableList<String> observableListSubject = FXCollections.observableArrayList(List.of());
    ObservableList<String> observableListTopics = FXCollections.observableArrayList(List.of());
    ObservableList<String> observableListClasses = FXCollections.observableArrayList(List.of("III", "IV", "V", "VI", "VII", "VIII", "IX", "X"));
    ObservableList<String> observableListBoard = FXCollections.observableArrayList(List.of(
            "CBSE", "ICSE", "Both"

    ));
    ObservableList<String> observableListRoom = FXCollections.observableArrayList(List.of(
            "3E", "3D", "3A"

    ));
    MainController mainController;
    ConductExam conductExam;

    public ExamCreator(MainController mainController, ConductExam conductExam) {
        this.mainController = mainController;
        this.conductExam = conductExam;
        //init();

    }

    public void initWithClass(String className) {
        TreeSet<String> subjectSet = new TreeSet<>();
        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from Topics where class='%s'".formatted(className));

            while (rs.next()) {
                subjectSet.add(rs.getString("subject"));
            }
            observableListSubject.clear();
            observableListSubject.addAll(subjectSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void initWithClassAndSubject(String className, String subject) {
        TreeSet<String> topicSet = new TreeSet<>();
        try {
            Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from Topics where class='%s' and subject='%s'".formatted(className, subject));

            while (rs.next()) {
                topicSet.add(rs.getString("topic_name"));
            }
            observableListTopics.clear();
            observableListTopics.addAll(topicSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void initializeDifficultyControls() {
        int initialTotal = parseQuestionCount();

        easy.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0,
                        Math.max(initialTotal, 0),
                        0
                )
        );

        medium.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0,
                        Math.max(initialTotal, 0),
                        0
                )
        );

        hard.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0,
                        Math.max(initialTotal, 0),
                        0
                )
        );

        easy.setEditable(true);
        medium.setEditable(true);
        hard.setEditable(true);

        /*
         * Divide the initial question count equally.
         */
        distributeQuestionCount();

        /*
         * Redistribute whenever the total question count changes.
         */
        questionCount.textProperty().addListener(
                (observable, oldValue, newValue) -> distributeQuestionCount()
        );

        /*
         * Medium and Hard changes are primarily balanced against Easy.
         */
        medium.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!updatingDifficultySpinners) {
                        balanceAfterChange(Difficulty.MEDIUM);
                    }
                }
        );

        hard.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!updatingDifficultySpinners) {
                        balanceAfterChange(Difficulty.HARD);
                    }
                }
        );

        /*
         * If Easy is edited directly, balance it against Medium first,
         * and then Hard if required.
         */
        easy.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!updatingDifficultySpinners) {
                        balanceAfterChange(Difficulty.EASY);
                    }
                }
        );

        /*
         * Commit manually typed Spinner values when focus is lost.
         */
        addSpinnerCommitOnFocusLost(easy);
        addSpinnerCommitOnFocusLost(medium);
        addSpinnerCommitOnFocusLost(hard);
    }

    private void distributeQuestionCount() {
        int total = parseQuestionCount();

        updatingDifficultySpinners = true;

        try {
            updateSpinnerMaximum(easy, total);
            updateSpinnerMaximum(medium, total);
            updateSpinnerMaximum(hard, total);

            if (total <= 0) {
                setSpinnerValue(easy, 0);
                setSpinnerValue(medium, 0);
                setSpinnerValue(hard, 0);
                return;
            }

            int baseCount = total / 3;
            int remainder = total % 3;

            int easyCount = baseCount;
            int mediumCount = baseCount;
            int hardCount = baseCount;

            if (remainder >= 1) {
                easyCount++;
            }

            if (remainder == 2) {
                mediumCount++;
            }

            setSpinnerValue(easy, easyCount);
            setSpinnerValue(medium, mediumCount);
            setSpinnerValue(hard, hardCount);

        } finally {
            updatingDifficultySpinners = false;
        }
    }

    private void balanceAfterChange(Difficulty changedDifficulty) {
        int total = parseQuestionCount();

        updatingDifficultySpinners = true;

        try {
            commitSpinnerValue(easy);
            commitSpinnerValue(medium);
            commitSpinnerValue(hard);

            int easyCount = getSpinnerValue(easy);
            int mediumCount = getSpinnerValue(medium);
            int hardCount = getSpinnerValue(hard);

            switch (changedDifficulty) {
                case HARD -> {
                    /*
                     * Keep Hard and Medium as entered.
                     * Easy receives the remaining count.
                     */
                    easyCount = total - mediumCount - hardCount;

                    if (easyCount < 0) {
                        /*
                         * Hard + Medium exceeded the total.
                         * Keep the newly edited Hard value and reduce Medium.
                         */
                        mediumCount = Math.max(0, total - hardCount);
                        easyCount = Math.max(
                                0,
                                total - mediumCount - hardCount
                        );
                    }
                }

                case MEDIUM -> {
                    /*
                     * Keep Medium and Hard as entered.
                     * Easy receives the remaining count.
                     */
                    easyCount = total - mediumCount - hardCount;

                    if (easyCount < 0) {
                        /*
                         * Keep the newly edited Medium value and reduce Hard.
                         */
                        hardCount = Math.max(0, total - mediumCount);
                        easyCount = Math.max(
                                0,
                                total - mediumCount - hardCount
                        );
                    }
                }

                case EASY -> {
                    /*
                     * Keep the newly entered Easy value.
                     * Adjust Medium first.
                     */
                    mediumCount = total - easyCount - hardCount;

                    if (mediumCount < 0) {
                        /*
                         * Easy + Hard exceeded the total.
                         * Reduce Hard after Medium reaches zero.
                         */
                        mediumCount = 0;
                        hardCount = Math.max(0, total - easyCount);
                    }
                }
            }

            easyCount = clamp(easyCount, 0, total);
            mediumCount = clamp(mediumCount, 0, total);
            hardCount = clamp(hardCount, 0, total);

            setSpinnerValue(easy, easyCount);
            setSpinnerValue(medium, mediumCount);
            setSpinnerValue(hard, hardCount);

        } finally {
            updatingDifficultySpinners = false;
        }
    }

    private int parseQuestionCount() {
        String text = questionCount.getText();

        if (text == null || text.isBlank()) {
            return 0;
        }

        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getSpinnerValue(Spinner<Integer> spinner) {
        Integer value = spinner.getValue();
        return value == null ? 0 : value;
    }

    private void setSpinnerValue(
            Spinner<Integer> spinner,
            int value
    ) {
        SpinnerValueFactory<Integer> valueFactory =
                spinner.getValueFactory();

        if (valueFactory != null) {
            valueFactory.setValue(value);
        }
    }

    private void updateSpinnerMaximum(
            Spinner<Integer> spinner,
            int maximum
    ) {
        SpinnerValueFactory<Integer> factory =
                spinner.getValueFactory();

        if (factory instanceof
                SpinnerValueFactory.IntegerSpinnerValueFactory integerFactory) {

            integerFactory.setMin(0);
            integerFactory.setMax(Math.max(0, maximum));
        }
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private void addSpinnerCommitOnFocusLost(
            Spinner<Integer> spinner
    ) {
        spinner.getEditor().focusedProperty().addListener(
                (observable, wasFocused, isFocused) -> {
                    if (!isFocused) {
                        commitSpinnerValue(spinner);
                    }
                }
        );

        spinner.getEditor().setOnAction(event -> {
            commitSpinnerValue(spinner);
            spinner.getParent().requestFocus();
        });
    }

    private void commitSpinnerValue(Spinner<Integer> spinner) {
        if (!spinner.isEditable() || spinner.getValueFactory() == null) {
            return;
        }

        String text = spinner.getEditor().getText();

        if (text == null || text.isBlank()) {
            spinner.getEditor().setText(
                    String.valueOf(getSpinnerValue(spinner))
            );
            return;
        }

        try {
            int total = parseQuestionCount();
            int value = Integer.parseInt(text.trim());
            value = clamp(value, 0, total);

            spinner.getValueFactory().setValue(value);
            spinner.getEditor().setText(String.valueOf(value));

        } catch (NumberFormatException e) {
            spinner.getEditor().setText(
                    String.valueOf(getSpinnerValue(spinner))
            );
        }
    }

    private enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    @FXML
    void create() {
        var exam = initDb();
        if (exam != null) {
            conductExam.items.add(exam);
        }
        mainController.modalPane.hide();
    }

    private ExamData initDb() {

        String insertExamSql = """
                INSERT INTO ExamScheduler (
                    subject,
                    class,
                    exam_date,
                    start_time,
                    end_time,
                    room_no,
                    board
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String insertExamTopicSql = """
                INSERT INTO ExamTopic (
                    exam_id,
                    topic_id
                )
                VALUES (?, ?)
                """;

        Connection conn = null;
        boolean previousAutoCommit = true;

        try {
            conn = mainController
                    .gradedDataLoader
                    .databaseLoader
                    .getConnection();

            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // -----------------------------------------------------
            // Read and validate the form values
            // -----------------------------------------------------

            String classValue = classNum.getValue();
            String subjectValue = subject.getSelectedItem();
            String roomValue = roomNo.getValue();
            String boardValue = board.getValue();

            if (classValue == null || classValue.isBlank()) {
                throw new IllegalStateException("Please select a class.");
            }

            if (subjectValue == null || subjectValue.isBlank()) {
                throw new IllegalStateException("Please select a subject.");
            }

            if (exam.getValue() == null) {
                throw new IllegalStateException("Please select the exam date.");
            }

            if (startTime.getTime() == null) {
                throw new IllegalStateException("Please select the start time.");
            }

            if (endTime.getTime() == null) {
                throw new IllegalStateException("Please select the end time.");
            }

            String examDate = exam.getValue().toString();
            String start = startTime.getTime().toString();
            String end = endTime.getTime().toString();

            // -----------------------------------------------------
            // Get selected topic names and corresponding IDs
            // -----------------------------------------------------

            String topicNameCsv = getAsCSW(topicList);

            List<String> topicNames = Arrays.stream(topicNameCsv.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .toList();

            if (topicNames.isEmpty()) {
                throw new IllegalStateException(
                        "Please select at least one topic."
                );
            }

            List<Integer> topicIds = getTopicIds(
                    conn,
                    topicNames,
                    subjectValue,
                    classValue
            ).stream()
                    .distinct()
                    .toList();

            if (topicIds.isEmpty()) {
                throw new IllegalStateException(
                        "No matching topic IDs were found."
                );
            }

            /*
             * This prevents continuing when some selected topic names
             * could not be resolved to IDs.
             *
             * This assumes topic names are unique for a particular
             * subject and class.
             */
            if (topicIds.size() != topicNames.size()) {
                throw new IllegalStateException(
                        "Some selected topics could not be matched. " +
                                "Selected topics: " + topicNames +
                                ", matched topic IDs: " + topicIds
                );
            }

            System.out.println("Selected topic names: " + topicNames);
            System.out.println("Selected topic IDs: " + topicIds);

            // -----------------------------------------------------
            // Insert the main ExamScheduler record
            // -----------------------------------------------------

            int generatedExamId;

            try (PreparedStatement pstmt = conn.prepareStatement(
                    insertExamSql,
                    Statement.RETURN_GENERATED_KEYS
            )) {
                pstmt.setString(1, subjectValue);
                pstmt.setString(2, classValue);
                pstmt.setString(3, examDate);
                pstmt.setString(4, start);
                pstmt.setString(5, end);
                pstmt.setString(6, roomValue);
                pstmt.setString(7, boardValue);

                int insertedRows = pstmt.executeUpdate();

                if (insertedRows != 1) {
                    throw new SQLException(
                            "Failed to insert the exam schedule."
                    );
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException(
                                "Exam was inserted, but no generated ID was returned."
                        );
                    }

                    generatedExamId = generatedKeys.getInt(1);
                }
            }

            // -----------------------------------------------------
            // Insert all selected topics into ExamTopic
            // -----------------------------------------------------

            try (PreparedStatement pstmt = conn.prepareStatement(
                    insertExamTopicSql
            )) {
                for (Integer topicId : topicIds) {
                    pstmt.setInt(1, generatedExamId);
                    pstmt.setInt(2, topicId);
                    pstmt.addBatch();
                }

                int[] topicInsertResults = pstmt.executeBatch();

                for (int result : topicInsertResults) {
                    if (result == Statement.EXECUTE_FAILED) {
                        throw new SQLException(
                                "Failed to assign one or more topics to the exam."
                        );
                    }
                }
            }

            // -----------------------------------------------------
            // Create exam questions using all selected topics
            // -----------------------------------------------------

            createExamQuestion(
                    conn,
                    generatedExamId,
                    topicIds, easy.getValue(), medium.getValue(), hard.getValue()
            );

            // Save everything only after every step succeeds.
            conn.commit();

            System.out.println(
                    "Exam successfully created with ID: " + generatedExamId
            );

            return new ExamData(
                    String.valueOf(generatedExamId),
                    new SimpleBooleanProperty(false),
                    classValue,
                    examDate,
                    roomValue,
                    subjectValue,
                    start + " - " + end,
                    topicIds,
                    topicNames,
                    boardValue, ExamType.Normal
            );

        } catch (Exception e) {

            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println(
                            "Exam creation failed. Database changes were rolled back."
                    );
                } catch (SQLException rollbackException) {
                    rollbackException.addSuppressed(e);
                    rollbackException.printStackTrace();
                }
            }

            e.printStackTrace();
            return null;

        } finally {

            if (conn != null) {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getAsCSW(List<String> topicList) {
        if (topicList == null) {
            return "";
        }
        return String.join(",", topicList);
    }


    /*    private void createExamQuestion(Connection conn, int generatedId, List<Integer> topicIds) {
            if (topicIds == null || topicIds.isEmpty()) {
                System.out.println("No topics found. Exam questions not created.");
                return;
            }

            String placeholders = String.join(",", Collections.nCopies(topicIds.size(), "?"));

            String selectSql = "SELECT question_id FROM Questions WHERE topic_id IN (" + placeholders + ")";
            String insertSql = "INSERT INTO ExamQuestion (exam_id, question_id) VALUES (?, ?)";

            List<Integer> questionIds = new ArrayList<>();

            // 1. Fetch all question IDs matching the topic IDs
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

                for (int i = 0; i < topicIds.size(); i++) {
                    pstmt.setInt(i + 1, topicIds.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        questionIds.add(rs.getInt("question_id"));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (questionIds.isEmpty()) {
                System.out.println("No questions found for selected topics.");
                return;
            }

            // 2. Randomize the sequence of the question IDs
            Collections.shuffle(questionIds);
            System.out.println(Integer.parseInt(questionCount.getText()));
            // 3. Batch insert up to 20 randomized questions into ExamQuestion
            try (PreparedStatement innerPst = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < questionIds.size() && i < Integer.parseInt(questionCount.getText()); i++) {
                    int quesId = questionIds.get(i);

                    innerPst.setInt(1, generatedId);
                    innerPst.setInt(2, quesId);
                    System.out.println(generatedId+"     "+quesId);
                    innerPst.addBatch();
                }

                innerPst.executeBatch();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    private void createExamQuestion(
            Connection conn,
            int generatedId,
            List<Integer> topicIds,
            int easyCount,
            int mediumCount,
            int hardCount
    ) {
        if (topicIds == null || topicIds.isEmpty()) {
            System.out.println("No topics selected. Exam questions were not created.");
            return;
        }

        final int totalQuestions;

        try {
            totalQuestions = Integer.parseInt(questionCount.getText().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid total question count: " + questionCount.getText());
            return;
        }

        if (totalQuestions <= 0) {
            System.err.println("Question count must be greater than zero.");
            return;
        }

        if (easyCount < 0 || mediumCount < 0 || hardCount < 0) {
            System.err.println("Difficulty counts cannot be negative.");
            return;
        }

        if (easyCount + mediumCount + hardCount != totalQuestions) {
            System.err.printf(
                    "Difficulty distribution must total %d. Current total: %d%n",
                    totalQuestions,
                    easyCount + mediumCount + hardCount
            );
            return;
        }

        String placeholders = String.join(
                ", ",
                Collections.nCopies(topicIds.size(), "?")
        );

        String selectSql = """
                WITH RankedByLevel AS (
                    SELECT
                        q.question_id,
                        q.level,
                        ROW_NUMBER() OVER (
                            PARTITION BY LOWER(TRIM(q.level))
                            ORDER BY RANDOM()
                        ) AS level_rank
                    FROM Questions AS q
                    WHERE q.topic_id IN (%s)
                      AND LOWER(TRIM(q.level)) IN (
                          'easy',
                          'medium',
                          'hard'
                      )
                ),
                
                PreferredQuestions AS (
                    SELECT
                        question_id,
                        level
                    FROM RankedByLevel
                    WHERE level_rank <= CASE LOWER(TRIM(level))
                        WHEN 'easy'   THEN ?
                        WHEN 'medium' THEN ?
                        WHEN 'hard'   THEN ?
                        ELSE 0
                    END
                ),
                
                FallbackQuestions AS (
                    SELECT
                        r.question_id,
                        r.level
                    FROM RankedByLevel AS r
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM PreferredQuestions AS p
                        WHERE p.question_id = r.question_id
                    )
                    ORDER BY RANDOM()
                    LIMIT (
                        SELECT MAX(0, ? - COUNT(*))
                        FROM PreferredQuestions
                    )
                ),
                
                FinalQuestions AS (
                    SELECT question_id
                    FROM PreferredQuestions
                
                    UNION ALL
                
                    SELECT question_id
                    FROM FallbackQuestions
                )
                
                SELECT question_id
                FROM FinalQuestions
                ORDER BY RANDOM()
                """.formatted(placeholders);

        String insertSql = """
                INSERT INTO ExamQuestion (exam_id, question_id)
                VALUES (?, ?)
                """;

        List<Integer> selectedQuestionIds = new ArrayList<>();

        try (PreparedStatement selectStatement =
                     conn.prepareStatement(selectSql)) {

            int parameterIndex = 1;

            // Dynamic topic IDs
            for (Integer topicId : topicIds) {
                if (topicId == null) {
                    System.err.println("Topic IDs cannot contain null.");
                    return;
                }

                selectStatement.setInt(parameterIndex++, topicId);
            }

            // Requested distribution
            selectStatement.setInt(parameterIndex++, easyCount);
            selectStatement.setInt(parameterIndex++, mediumCount);
            selectStatement.setInt(parameterIndex++, hardCount);

            // Final required count
            selectStatement.setInt(parameterIndex, totalQuestions);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    selectedQuestionIds.add(
                            resultSet.getInt("question_id")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Could not select questions for the exam.");
            e.printStackTrace();
            return;
        }

        if (selectedQuestionIds.isEmpty()) {
            System.out.println("No questions found for the selected topics.");
            return;
        }

        if (selectedQuestionIds.size() < totalQuestions) {
            System.out.printf(
                    "Requested %d questions, but only %d matching questions are available.%n",
                    totalQuestions,
                    selectedQuestionIds.size()
            );
        }

        try (PreparedStatement insertStatement =
                     conn.prepareStatement(insertSql)) {

            for (Integer questionId : selectedQuestionIds) {
                insertStatement.setInt(1, generatedId);
                insertStatement.setInt(2, questionId);
                insertStatement.addBatch();

                System.out.printf(
                        "Exam ID: %d, Question ID: %d%n",
                        generatedId,
                        questionId
                );
            }

            int[] insertedRows = insertStatement.executeBatch();

            System.out.printf(
                    "%d questions added to exam %d.%n",
                    insertedRows.length,
                    generatedId
            );

        } catch (SQLException e) {
            System.err.println(
                    "Could not insert selected questions into ExamQuestion."
            );
            e.printStackTrace();
        }
    }

    private List<Integer> getTopicIds(Connection conn, List<String> topics, String subject, String className) {
        List<Integer> topicIds = new ArrayList<>();

        if (topics == null || topics.isEmpty()) {
            System.out.println("No topic names provided.");
            return topicIds;
        }

        String placeholders = String.join(",", Collections.nCopies(topics.size(), "?"));

        String sql = """
                SELECT topic_id
                FROM Topics
                WHERE topic_name IN (%s)
                  AND subject = ?
                  AND class = ?
                """.formatted(placeholders);

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int index = 1;

            // Set topic names
            for (String topic : topics) {
                pstmt.setString(index++, topic);
            }

            // Set subject and class
            pstmt.setString(index++, subject);
            pstmt.setString(index, className);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topicIds.add(rs.getInt("topic_id"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return topicIds;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDifficultyControls();
        close.setOnMouseClicked(event -> {
            this.mainController.modalPane.hide();
        });
        topicName.setSuggestionProvider(request ->
                observableListTopics.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
        subject.setSuggestionProvider(request -> observableListSubject.stream().filter(country ->
                country.toLowerCase().contains(request.getUserText().toLowerCase())).collect(Collectors.toList()));
        classNum.setItems(observableListClasses);
        classNum.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            initWithClass(classNum.getValue());
            if (subject.getText() != null && !subject.getText().isEmpty())
                initWithClassAndSubject(classNum.getValue(), subject.getText());
        });
        subject.setOnCommit(s -> {
            initWithClassAndSubject(classNum.getValue(), s);
        });
        board.setItems(observableListBoard);
        roomNo.setItems(observableListRoom);

        topicName.setOnCommit(event -> {
            if (!topicList.contains(event)) {
                topicList.add(event);
                addTopicList(event);
            }
        });
    }

    public void addTopicList(String topicName) {
        Label label = new Label(topicName);
        var graphics = new FontIcon(Material2AL.CLOSE);
        label.setGraphic(graphics);
        label.setStyle("-fx-border-color:rgba(0, 0, 0, 0.2);-fx-padding: 5;-fx-background-radius: 5;-fx-border-radius: 5;");
        topics.getChildren().add(label);
        graphics.setOnMouseClicked((event) -> {
            FontIcon fontIcon = (FontIcon) event.getSource();
            Label lab = (Label) fontIcon.getParent();
            topics.getChildren().remove(lab);
            topicList.remove(lab.getText());
        });

    }


}
