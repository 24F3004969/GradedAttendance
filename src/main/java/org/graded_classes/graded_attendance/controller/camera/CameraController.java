package org.graded_classes.graded_attendance.controller.camera;

import com.dlsc.gemsfx.PhotoView;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.FacemarkLBF;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.controller.home.HomeController;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.controller.tts.RealTimeTts;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import static org.graded_classes.graded_attendance.controller.quiz.QuizTaker.extractResourceToTempFile;

public class CameraController {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    public ToggleGroup modes;
    private Stage studentStage;
    private PhotoView studentCameraView;
    private Label studentMessage;
    @FXML
    private StackPane rootLayer;
    private final AtomicBoolean uiFramePending = new AtomicBoolean(false);
    @FXML
    private PhotoView cameraView;
    @FXML
    private Button addStudent;
    @FXML
    private ScrollPane scrollAtt;
    @FXML
    private VBox glasses;
    int requiredRecognitionCount = 5;
    @FXML
    private VBox hijab;
    @FXML
    private HBox checkBoxGroup;
    @FXML
    private ComboBox<String> cameraList;
    private final AtomicBoolean processing =
            new AtomicBoolean(false);
    @FXML
    private SearchField<String> searchBox;
    private VideoCapture camera;
    private ScheduledExecutorService timer;

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer recognizer;
    private final Object frameLock = new Object();
    private Rect lastFace;
    ObservableList<String> studentData = FXCollections.observableArrayList(List.of());
    private static final long RECOGNITION_INTERVAL_NS =
            TimeUnit.MILLISECONDS.toNanos(250);


    private final AtomicBoolean training =
            new AtomicBoolean(false);

    private final AtomicBoolean recognizerReady =
            new AtomicBoolean(false);

    private final Object recognizerLock =
            new Object();
    private long lastRecognitionTime;

    private int currentCandidate = -1;
    private int consecutiveMatches = 0;
    private double confidenceSum = 0.0;

    private static final long RECOGNITION_COOLDOWN_MS =
            10_000L;

    private static final int REQUIRED_NO_FACE_FRAMES = 8;

    private int lastAcceptedId = -1;
    private long lastAcceptedAt = 0L;
    private int consecutiveNoFaceFrames = 0;
    private boolean acceptedFaceMustLeave = false;
    private static final int FACE_SIZE = 200;
    private static double CONFIDENCE_THRESHOLD = 40;
    @FXML
    private Button startCapture;
    @FXML
    private SVGImageView myLogo;
    @FXML
    private SVGImageView background;
    private Mat objectPoints;
    MainController mainController;
    ArrayList<Button> sourceList = new ArrayList<>();
    private final Mat cleanFrame = new Mat();
    @FXML
    private Label gFront, hFront, ldFront, llFront, nFront, nDown, nLeft, nRight, outputMessage;
    private final LinkedHashMap<Integer, ArrayList<Double>> tempAttendanceRecord = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, AttendanceStatus> attendanceRecord = new LinkedHashMap<>();

    HomeController homeController;


    public CameraController(MainController mainController, HomeController homeController) {
        this.mainController = mainController;
        this.homeController = homeController;
    }

    @FXML
    void onClicked(ActionEvent event) {
        var box = (CheckBox) (event.getSource());
        var text = box.getText();
        if (text.equals("Glasses") && box.isSelected()) {
            glasses.setDisable(false);
        } else if (text.equals("Hijab") && box.isSelected()) {
            hijab.setDisable(false);
        } else if (text.equals("Glasses") && !box.isSelected()) {
            glasses.setDisable(true);
        } else if (text.equals("Hijab") && !box.isSelected()) {
            hijab.setDisable(true);
        }
    }

    @FXML
    private void openStudentDisplay() {
        var background = new SVGImageView();
        background.setSvgUrl(GradedResourceLoader.load("icons/new-back1.svg"));
        var studentLogo = new SVGImageView();
        studentLogo.setFitHeight(48);
        studentLogo.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        HBox logoBar = new HBox(studentLogo);
        VBox.setVgrow(logoBar, Priority.ALWAYS);
        logoBar.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setMargin(logoBar, new Insets(8));
        studentCameraView = new PhotoView();
        studentCameraView.setEditable(false);
        studentCameraView.setMinSize(512, 512);
        studentCameraView.setMaxSize(800, 800);
        studentCameraView.getStyleClass().add("border-circle-green");
        studentMessage = new Label("Please stand in front of the camera");
        studentMessage.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                """);
        VBox content = new VBox(20, studentCameraView, studentMessage);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox mainContent = new VBox(content, logoBar);
        StackPane root = new StackPane(background, mainContent);
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(GradedResourceLoader.load("css/camera-style.css"));
        studentStage = new Stage();
        studentStage.setTitle("Student Display");
        studentStage.setScene(scene);
        studentStage.show();
    }

    private void loadCameraSettings() {

        String sql = """
                SELECT
                    confidence_threshold,
                    required_checks
                FROM camera_data
                WHERE id = 1
                """;

        try (
                PreparedStatement statement =
                        mainController.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {

            if (rs.next()) {

                CONFIDENCE_THRESHOLD =
                        rs.getDouble("confidence_threshold");

                requiredRecognitionCount =
                        rs.getInt("required_checks");

                System.out.println(
                        "Threshold Loaded = "
                                + CONFIDENCE_THRESHOLD
                );

                System.out.println(
                        "Required Checks Loaded = "
                                + requiredRecognitionCount
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onSetting(ActionEvent event) {

        Slider thresholdSlider =
                new Slider(20, 100, CONFIDENCE_THRESHOLD);

        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setShowTickMarks(true);

        Label thresholdLabel = new Label(
                String.format(
                        "Confidence Threshold : %.0f",
                        CONFIDENCE_THRESHOLD
                )
        );
        Spinner<Integer> checkSpinner =
                new Spinner<>(1, 20, requiredRecognitionCount);
        thresholdSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                thresholdLabel.setText(
                        String.format(
                                "Confidence Threshold : %.0f",
                                newVal.doubleValue()
                        )
                )
        );
        checkSpinner.setEditable(true);

        VBox content = new VBox(
                15,
                new Label("Recognition Settings"),
                thresholdLabel,
                thresholdSlider,
                new Label("Required Consecutive Matches"),
                checkSpinner
        );

        com.dlsc.gemsfx.DialogPane dialogPane =
                new com.dlsc.gemsfx.DialogPane();

        com.dlsc.gemsfx.DialogPane.Dialog<ButtonType> dialog =
                new com.dlsc.gemsfx.DialogPane.Dialog<>(
                        dialogPane,
                        com.dlsc.gemsfx.DialogPane.Type.INFORMATION
                );

        dialog.setTitle("Face Recognition Settings");
        dialog.setContent(content);

        ButtonType saveButton =
                new ButtonType(
                        "Save",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getButtonTypes().setAll(
                saveButton,
                ButtonType.CANCEL
        );

        rootLayer.getChildren().add(dialogPane);

        dialog.show();

        dialog.onClose(e -> {

            if (e == saveButton) {

                CONFIDENCE_THRESHOLD =
                        thresholdSlider.getValue();

                requiredRecognitionCount =
                        checkSpinner.getValue();

                System.out.println(
                        "Threshold = " +
                                CONFIDENCE_THRESHOLD
                );
                saveCameraSettings(
                        CONFIDENCE_THRESHOLD,
                        requiredRecognitionCount
                );
                System.out.println(
                        "Required Checks = " +
                                requiredRecognitionCount
                );
            }

            rootLayer.getChildren().remove(dialogPane);
        });
    }

    private void saveCameraSettings(
            double threshold,
            int checks
    ) {
        String sql = """
                UPDATE camera_data
                SET confidence_threshold = ?,
                    required_checks = ?
                """;

        try (PreparedStatement statement =
                     mainController.gradedDataLoader.databaseLoader.getConnection().prepareStatement(sql)) {

            statement.setDouble(1, threshold);
            statement.setInt(2, checks);

            statement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Result getKnownStudentIfPresent(String id) {
        String ed = id.substring(0, id.indexOf(' '));
        String name = id.substring(id.indexOf(' ') + 1).trim();

        imagePath = System.getProperty("user.home")
                + "/gardeEdAttendanceData/"
                + ed;

        File folder = new File(imagePath);

        if (folder.exists()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Student Already Exists");
            alert.setHeaderText("Training data already exists");
            alert.setContentText(
                    ed + " (" + name + ") already has training images."
            );
            alert.showAndWait();
            return null;
        }
        return new Result(ed, name);
    }

    @FXML
    void onModeChange(ActionEvent event) {
        ToggleButton toggle =
                (ToggleButton) event.getSource();

        boolean recognitionMode =
                "Face Detector".equals(toggle.getText());

        recognitionModeSelected.set(recognitionMode);
        resetRecognitionSession();

        if ("Training".equals(toggle.getText())) {
            addStudent.setDisable(false);
            searchBox.setDisable(false);
            checkBoxGroup.setVisible(true);

            startCapture.setText("Start Training");
            startCapture.setDisable(false);

        } else if (recognitionMode) {
            addStudent.setDisable(true);
            searchBox.setDisable(true);
            scrollAtt.setVisible(false);
            checkBoxGroup.setVisible(false);

            startCapture.setDisable(true);
        }
    }
    private final AtomicBoolean recognitionModeSelected = new AtomicBoolean(false);
    @FXML
    void startCapturing(ActionEvent event) {
        Button source = (Button) event.getSource();
        String buttonId = source.getId();
        switch (buttonId) {
            case "nf" -> initCapture(buttonId, source, nFront);
            case "nd" -> initCapture(buttonId, source, nDown);
            case "nl" -> initCapture(buttonId, source, nLeft);
            case "nr" -> initCapture(buttonId, source, nRight);
            case "llf" -> initCapture(buttonId, source, llFront);
            case "ldf" -> initCapture(buttonId, source, ldFront);
            case "hf" -> initCapture(buttonId, source, hFront);
            case "gf" -> initCapture(buttonId, source, gFront);
        }
    }

    private void initCapture(String buttonId, Button source, Label label) {
        if (lastFace == null || cleanFrame.empty()) {
            System.out.println("No face detected.");
            return;
        }
        int x = label.getText().contains(" ") ? (Integer.parseInt(label.
                getText().substring(label.getText().indexOf(' ')).trim())) + 1 : 1;
        label.setText(label.getText().contains(" ") ?
                (label.getText().substring(0, label.getText().indexOf(' ')) + " " + x) :
                label.getText() + " " + x);
        clickThePhoto(buttonId);
        if (x >= 10) {
            source.setDisable(true);
            label.setText(label.getText().substring(0, label.getText().length() - 3));
            sourceList.add(source);
        }
    }

    private void clickThePhoto(String id) {
        Mat capturedFace;

        synchronized (frameLock) {
            if (lastFace == null
                    || cleanFrame.empty()) {

                System.out.println(
                        "No face detected."
                );

                return;
            }

            try (Rect safeFace =
                         clampFaceToFrame(
                                 lastFace,
                                 cleanFrame
                         )) {

                if (safeFace.width() <= 0
                        || safeFace.height() <= 0) {

                    System.out.println(
                            "Invalid face area."
                    );

                    return;
                }

                capturedFace =
                        new Mat(
                                cleanFrame,
                                safeFace
                        ).clone();
            }
        }

        try {
            executor.submit(() ->
                    saveCapturedFace(
                            capturedFace,
                            id
                    )
            );

        } catch (RejectedExecutionException exception) {
            capturedFace.close();
            exception.printStackTrace();
        }
    }

    private void saveCapturedFace(
            Mat capturedFace,
            String captureMode
    ) {
        try (capturedFace;
             Mat faceGray = new Mat();
             Size faceSize =
                     new Size(FACE_SIZE, FACE_SIZE)) {

            cvtColor(
                    capturedFace,
                    faceGray,
                    COLOR_BGR2GRAY
            );

            resize(
                    faceGray,
                    faceGray,
                    faceSize,
                    0,
                    0,
                    INTER_AREA
            );

            /*
             * Do not equalize here. Training and recognition
             * already perform equalizeHist().
             */
            File directory =
                    new File(imagePath, captureMode);

            if (!directory.exists()
                    && !directory.mkdirs()) {

                throw new IOException(
                        "Unable to create directory: "
                                + directory.getAbsolutePath()
                );
            }

            File outputFile =
                    new File(
                            directory,
                            System.nanoTime() + ".jpg"
                    );

            boolean saved =
                    imwrite(
                            outputFile.getAbsolutePath(),
                            faceGray
                    );

            if (!saved) {
                throw new IOException(
                        "Unable to save image: "
                                + outputFile.getAbsolutePath()
                );
            }

            System.out.println(
                    "Photo saved: "
                            + outputFile.getAbsolutePath()
            );

        } catch (Exception exception) {
            System.err.println(
                    "Unable to save training image: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private Rect clampFaceToFrame(Rect face, Mat source) {
        int x = Math.max(0, face.x());
        int y = Math.max(0, face.y());

        int right = Math.min(
                source.cols(),
                face.x() + face.width()
        );

        int bottom = Math.min(
                source.rows(),
                face.y() + face.height()
        );

        return new Rect(
                x,
                y,
                Math.max(0, right - x),
                Math.max(0, bottom - y)
        );
    }

    String imagePath;
    private void printMemory(String stage) {
        Runtime runtime = Runtime.getRuntime();

        long usedHeap =
                runtime.totalMemory()
                        - runtime.freeMemory();

        System.out.printf(
                Locale.ROOT,
                "%s | heap used: %.2f MB | allocated heap: %.2f MB | max heap: %.2f MB%n",
                stage,
                usedHeap / 1024.0 / 1024.0,
                runtime.totalMemory() / 1024.0 / 1024.0,
                runtime.maxMemory() / 1024.0 / 1024.0
        );
    }
    @FXML
    void addNewTraining(ActionEvent event) {
        String id = searchBox.getText();
        if (!id.isEmpty() && getKnownStudentIfPresent(id) != null) {
            scrollAtt.setVisible(true);
            Result result = getResult(id);
            CompletableFuture.runAsync(() -> {
                timeTts.readAloud(", , " + result.ed() + result.name() +
                        "Welcome to GradeED Coaching Classes. Please be ready and stand in front of the camera.");
            });
        }
    }

    private Result getResult(String id) {
        String ed = id.substring(0, id.indexOf(' '));
        String name = id.substring(id.indexOf(' ') + 1).trim();
        imagePath = System.getProperty("user.home") + "/gardeEdAttendanceData/" + id.substring(0, id.indexOf(' '));
        File f = new File(imagePath);
        if (!f.exists()) {
            f.mkdirs();
        }
        Result result = new Result(ed, name);
        return result;
    }

    private boolean isOnCooldown(int id) {
        long now = System.currentTimeMillis();
        return id == lastAcceptedId && now - lastAcceptedAt < RECOGNITION_COOLDOWN_MS;
    }

    private record Result(String ed, String name) {
    }

    RealTimeTts timeTts;

    @FXML
    public void initialize() {
        initializeStudentSearch();
        initializeGraphics();
        //initializeTtsAsync();
        initializeCameraAsync();
        loadCameraSettings();
    }

    private void initializeStudentSearch() {
        studentData.setAll(
                mainController.gradedDataLoader
                        .getStudentData()
                        .values()
                        .stream()
                        .map(student ->
                                student.ed_no() + " " + student.name()
                        )
                        .toList()
        );

        searchBox.setSuggestionProvider(request -> {
            String searchText = request.getUserText();

            if (searchText == null || searchText.isBlank()) {
                return List.of();
            }

            String normalizedSearch =
                    searchText.toLowerCase().trim();

            return studentData.stream()
                    .filter(student ->
                            student.toLowerCase()
                                    .contains(normalizedSearch)
                    )
                    .collect(Collectors.toList());
        });
        FontIcon searchIcon = new FontIcon("mdmz-search");
        FontIcon clearIcon = new FontIcon("mdal-close");

        searchBox.setGraphic(searchIcon);

        searchBox.textProperty().addListener((obs, oldText, newText) -> {

            if (newText == null || newText.isBlank()) {

                searchBox.setGraphic(searchIcon);
                for (var bt : sourceList) {
                    bt.setDisable(false);
                }

                sourceList.clear();
                scrollAtt.setVisible(false);

            } else {

                searchBox.setGraphic(clearIcon);
            }
        });

        clearIcon.setOnMouseClicked(event -> {

            searchBox.setText("");

            for (var bt : sourceList) {
                bt.setDisable(false);
            }

            sourceList.clear();
            scrollAtt.setVisible(false);
        });

    }

    private void initializeGraphics() {
        background.setSvgUrl(
                GradedResourceLoader.load(
                        "icons/new-back1.svg"
                )
        );

        background.setOpacity(0.3);

        myLogo.setSvgUrl(
                GradedResourceLoader.load(
                        "icons/my-logo.svg"
                )
        );
    }

    private final AtomicBoolean ttsReady =
            new AtomicBoolean(false);

    private void initializeTtsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                RealTimeTts initializedTts =
                        new RealTimeTts();

                initializedTts.init();

                timeTts = initializedTts;
                ttsReady.set(true);

                System.out.println(
                        "Text-to-speech initialized."
                );
            } catch (Exception exception) {
                System.err.println(
                        "Could not initialize text-to-speech."
                );

                exception.printStackTrace();
            }
        });
    }

    @FXML
    void play(ActionEvent event) {
        if (!ttsReady.get() || timeTts == null) {
            System.out.println(
                    "Text-to-speech is still loading."
            );
            return;
        }

        String id = ((Button) event.getSource()).getId();

        switch (id) {
            case "normal" -> timeTts.readAloud(
                    ", , Normal mode , ,"
            );

            case "long" -> timeTts.readAloud(
                    ", , Long distance mode , ,"
            );

            case "low" -> timeTts.readAloud(
                    ", , Low Light mode , ,"
            );

            case "glasses" -> timeTts.readAloud(
                    ", , Glasses mode , ,"
            );

            case "hijab" -> timeTts.readAloud(
                    ", , Hijab mode , ,"
            );

            default -> System.out.println(
                    "Unknown TTS button: " + id
            );
        }
    }

    private final AtomicBoolean cameraSwitching =
            new AtomicBoolean(false);

    private void initializeCameraAsync() {
        cameraList.setDisable(true);
        startCapture.setDisable(true);

        CompletableFuture
                .supplyAsync(() -> {
                    loadFaceDetector();
                    loadRecognizer();
                    return getAvailableCameras();
                })
                .thenAccept(cameras ->
                        Platform.runLater(() ->
                                configureCameras(cameras)
                        )
                )
                .exceptionally(exception -> {
                    exception.printStackTrace();

                    Platform.runLater(() -> {
                        cameraList.setDisable(true);
                        startCapture.setDisable(true);

                        showInitializationError(
                                "Camera initialization failed",
                                getRootCauseMessage(exception)
                        );
                    });

                    return null;
                });
    }

    private void configureCameras(
            ObservableList<String> cameras
    ) {
        cameraList.setItems(cameras);

        if (cameras.isEmpty()) {
            System.out.println("No camera found");
            cameraList.setDisable(true);
            startCapture.setDisable(true);

            return;
        }

        cameraList.getSelectionModel()
                .selectedIndexProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue == null) {
                                return;
                            }

                            int newIndex = newValue.intValue();

                            if (newIndex < 0) {
                                return;
                            }

                            switchCamera(newIndex);
                        }
                );

        cameraList.setDisable(false);
        startCapture.setDisable(false);

        int initialCameraIndex =
                cameras.size() > 1 ? 1 : 0;

        /*
         * Selecting the camera triggers the listener.
         * Do not separately call startCamera() here.
         */
        cameraList.getSelectionModel()
                .select(initialCameraIndex);
    }

    private void showInitializationError(
            String header,
            String message
    ) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle("Initialization Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.show();
    }

    private String getRootCauseMessage(
            Throwable throwable
    ) {
        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    @FXML
    public void goForTraining() {
        /*
         * Prevent multiple training operations from running together.
         */
        if (!training.compareAndSet(false, true)) {
            outputMessage.setText(
                    "Model training is already running."
            );
            return;
        }

        resetRecognition();
        setTrainingControlsDisabled(true);

        ProgressBar progressBar =
                new ProgressBar(
                        ProgressIndicator.INDETERMINATE_PROGRESS
                );

        progressBar.setPrefWidth(500);

        Label statusLabel =
                new Label("Preparing training...");

        VBox content = new VBox(
                10,
                statusLabel,
                progressBar
        );

        content.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        com.dlsc.gemsfx.DialogPane dialogPane =
                new com.dlsc.gemsfx.DialogPane();

        com.dlsc.gemsfx.DialogPane.Dialog<ButtonType> dialog =
                new com.dlsc.gemsfx.DialogPane.Dialog<>(
                        dialogPane,
                        com.dlsc.gemsfx.DialogPane.Type.INFORMATION
                );

        dialog.setTitle("Model Training");
        dialog.setContent(content);

        rootLayer.getChildren().add(dialogPane);

        Task<Void> trainingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage(
                        "Waiting for recognition to finish..."
                );

                waitForRecognitionToFinish();

                checkTrainingCancellation(this);

                updateMessage(
                        "Loading and preparing training images..."
                );

                startTraining(this);

                updateMessage(
                        "Training completed successfully."
                );

                return null;
            }
        };

        progressBar.progressProperty().bind(
                trainingTask.progressProperty()
        );

        statusLabel.textProperty().bind(
                trainingTask.messageProperty()
        );

        trainingTask.setOnSucceeded(event -> {
            finishTraining(
                    dialogPane,
                    progressBar,
                    statusLabel
            );

            outputMessage.setText(
                    "Training completed successfully."
            );
        });

        trainingTask.setOnFailed(event -> {
            Throwable exception =
                    trainingTask.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            finishTraining(
                    dialogPane,
                    progressBar,
                    statusLabel
            );

            String errorMessage =
                    exception == null
                            ? "Unknown training error"
                            : getRootCauseMessage(exception);

            outputMessage.setText(
                    "Training failed: " + errorMessage
            );
        });

        trainingTask.setOnCancelled(event -> {
            finishTraining(
                    dialogPane,
                    progressBar,
                    statusLabel
            );

            outputMessage.setText(
                    "Training was cancelled."
            );
        });

        dialog.show();

        Thread thread = new Thread(
                trainingTask,
                "attendance-model-training"
        );

        thread.setDaemon(true);
        thread.start();
    }

    private void waitForRecognitionToFinish() {
        while (processing.get()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException(
                        "Training was interrupted."
                );
            }

            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new CancellationException(
                        "Training was interrupted."
                );
            }
        }
    }

    private void finishTraining(
            com.dlsc.gemsfx.DialogPane dialogPane,
            ProgressBar progressBar,
            Label statusLabel
    ) {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();

        training.set(false);
        processing.set(false);
        resetRecognition();

        setTrainingControlsDisabled(false);

        rootLayer.getChildren().remove(dialogPane);
    }

    private void setTrainingControlsDisabled(
            boolean disabled
    ) {
        cameraList.setDisable(disabled);
        addStudent.setDisable(disabled);
        searchBox.setDisable(disabled);
        checkBoxGroup.setDisable(disabled);
        startCapture.setDisable(disabled);
    }


    private void resetRecognition() {
        currentCandidate = -1;
        consecutiveMatches = 0;
        confidenceSum = 0;
    }

    private FacemarkLBF facemark;

    private void loadFaceDetector() {

        faceDetector = loadFaceDetectorFromFile();

        if (faceDetector.empty()) {
            throw new RuntimeException(
                    "Unable to load Haar Cascade."
            );
        }
    }

    private CascadeClassifier loadFaceDetectorFromFile() {
        try {
            File cascadeFile = extractResourceToTempFile(
                    "/org/graded_classes/graded_attendance/opencv/haarcascade_frontalface_default.xml",
                    "haarcascade-",
                    ".xml"
            );

            CascadeClassifier classifier =
                    new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (classifier.empty()) {
                throw new RuntimeException("Failed to load cascade classifier");
            }

            return classifier;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load face detector", e);
        }
    }

    private void loadRecognizer() {
        printMemory("After loading recognizer");
        Path modelPath = Path.of(
                System.getProperty("user.home"),
                "attendance_model.yml"
        );
        System.out.println(modelPath);
        if (!Files.isRegularFile(modelPath)) {
            recognizerReady.set(false);

            System.err.println(
                    "Recognition model does not exist: "
                            + modelPath
            );
            printMemory("After loading recognizer");
            return;
        }

        synchronized (recognizerLock) {
            LBPHFaceRecognizer newRecognizer =
                    LBPHFaceRecognizer.create();

            try {
                newRecognizer. read(
                        modelPath.toString()
                );

                LBPHFaceRecognizer oldRecognizer =
                        recognizer;

                recognizer = newRecognizer;
                recognizerReady.set(true);
                printMemory("After recognizer loaded");
                if (oldRecognizer != null) {
                    oldRecognizer.close();
                }

                System.out.println(
                        "Attendance model loaded successfully: "
                                + modelPath
                );

            } catch (Exception exception) {
                recognizerReady.set(false);
                newRecognizer.close();

                System.err.println(
                        "Could not load recognition model: "
                                + exception.getMessage()
                );

                exception.printStackTrace();
            }
        }
    }


    private static final int CAMERA_WIDTH = 1280;
    private static final int CAMERA_HEIGHT = 720;
    private static final int CAMERA_FPS = 30;

    /*
     * About 15 processed frames per second.
     * This is normally sufficient for attendance recognition.
     */
    private static final long FRAME_DELAY_MS = 66;

    private final Object cameraLock = new Object();

    private void startCamera(int cameraIndex) {
        if (cameraIndex < 0) {
            throw new IllegalArgumentException(
                    "Camera index cannot be negative: " + cameraIndex
            );
        }

        /*
         * Ensure an existing timer and camera are completely stopped
         * before opening another camera.
         */
        stopCamera();

        VideoCapture newCamera = null;
        ScheduledExecutorService newTimer = null;

        try {
            newCamera = new VideoCapture(
                    cameraIndex,
                    CAP_DSHOW
            );

            if (!newCamera.isOpened()) {
                throw new IllegalStateException(
                        "Failed to open camera " + cameraIndex
                );
            }

            configureCamera(newCamera);

            /*
             * Publish the fully initialized camera only after it has
             * successfully opened and been configured.
             */
            synchronized (cameraLock) {
                camera = newCamera;
                currentCameraIndex = cameraIndex;

                /*
                 * Ownership has been transferred to the camera field.
                 */
                newCamera = null;
            }

            newTimer =
                    Executors.newSingleThreadScheduledExecutor(
                            runnable -> {
                                Thread thread = new Thread(
                                        runnable,
                                        "attendance-camera-capture"
                                );

                                thread.setDaemon(true);

                                thread.setUncaughtExceptionHandler(
                                        (failedThread, exception) -> {
                                            System.err.println(
                                                    "Unexpected camera-thread failure"
                                            );

                                            exception.printStackTrace();
                                        }
                                );

                                return thread;
                            }
                    );

            synchronized (cameraLock) {
                timer = newTimer;

                /*
                 * Ownership has been transferred to the timer field.
                 */
                newTimer = null;
            }

            /*
             * Fixed delay provides backpressure. The next frame begins
             * FRAME_DELAY_MS after the previous grabFrame() completes.
             */
            timer.scheduleWithFixedDelay(
                    this::grabFrameSafely,
                    0,
                    FRAME_DELAY_MS,
                    TimeUnit.MILLISECONDS
            );

            System.out.printf(
                    "Camera %d started at requested resolution %dx%d.%n",
                    cameraIndex,
                    CAMERA_WIDTH,
                    CAMERA_HEIGHT
            );

        } catch (Exception exception) {
            /*
             * Clean up partially initialized resources.
             */
            if (newTimer != null) {
                newTimer.shutdownNow();
            }

            if (newCamera != null) {
                try {
                    newCamera.release();
                } catch (Exception releaseException) {
                    exception.addSuppressed(releaseException);
                }

                try {
                    newCamera.close();
                } catch (Exception closeException) {
                    exception.addSuppressed(closeException);
                }
            }

            /*
             * stopCamera() cleans resources that may already have been
             * transferred to the controller fields.
             */
            stopCamera();

            System.err.println(
                    "Unable to start camera "
                            + cameraIndex
                            + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            Platform.runLater(() ->
                    showInitializationError(
                            "Unable to start camera",
                            "Camera "
                                    + cameraIndex
                                    + " could not be opened.\n"
                                    + getRootCauseMessage(exception)
                    )
            );
        }
    }

    private void configureCamera(
            VideoCapture targetCamera
    ) {
        /*
         * Camera drivers may reject some properties. The return value
         * indicates whether the property request was accepted.
         */
        boolean widthAccepted =
                targetCamera.set(
                        CAP_PROP_FRAME_WIDTH,
                        CAMERA_WIDTH
                );

        boolean heightAccepted =
                targetCamera.set(
                        CAP_PROP_FRAME_HEIGHT,
                        CAMERA_HEIGHT
                );

        boolean fpsAccepted =
                targetCamera.set(
                        CAP_PROP_FPS,
                        CAMERA_FPS
                );

        if (!widthAccepted) {
            System.err.println(
                    "Camera did not accept requested width: "
                            + CAMERA_WIDTH
            );
        }

        if (!heightAccepted) {
            System.err.println(
                    "Camera did not accept requested height: "
                            + CAMERA_HEIGHT
            );
        }

        if (!fpsAccepted) {
            System.err.println(
                    "Camera did not accept requested FPS: "
                            + CAMERA_FPS
            );
        }

        double actualWidth =
                targetCamera.get(CAP_PROP_FRAME_WIDTH);

        double actualHeight =
                targetCamera.get(CAP_PROP_FRAME_HEIGHT);

        double actualFps =
                targetCamera.get(CAP_PROP_FPS);

        System.out.printf(
                "Camera configuration: %.0fx%.0f at reported %.1f FPS%n",
                actualWidth,
                actualHeight,
                actualFps
        );
    }
    private synchronized void updateFacePresence(
            boolean faceDetected
    ) {
        if (faceDetected) {
            consecutiveNoFaceFrames = 0;
            return;
        }

        consecutiveNoFaceFrames++;

        if (consecutiveNoFaceFrames
                < REQUIRED_NO_FACE_FRAMES) {
            return;
        }

        acceptedFaceMustLeave = false;
        consecutiveNoFaceFrames = 0;

        resetRecognition();
    }
    private void grabFrameSafely() {
        try {
            grabFrame();
        } catch (Throwable throwable) {
            /*
             * Catch Throwable only at this outer scheduler boundary so an
             * unexpected failure is logged instead of silently stopping
             * future scheduled executions.
             */
            System.err.println(
                    "Unexpected frame-capture failure: "
                            + throwable.getMessage()
            );

            throwable.printStackTrace();

            restartCameraAsync();
        }
    }


    private void grabFrame() {
        try (Mat localFrame = new Mat();
             Mat localGray = new Mat();
             RectVector faces = new RectVector();
             Size minimumFaceSize = new Size(50, 50);
             Size maximumFaceSize = new Size()) {

            if (!readCameraFrame(localFrame)) {
                System.err.println(
                        "Camera frame was not available."
                );

                restartCameraAsync();
                return;
            }

            flip(
                    localFrame,
                    localFrame,
                    1
            );

            cvtColor(
                    localFrame,
                    localGray,
                    COLOR_BGR2GRAY
            );

            faceDetector.detectMultiScale(
                    localGray,
                    faces,
                    1.1,
                    5,
                    0,
                    minimumFaceSize,
                    maximumFaceSize
            );

            Rect detectedFace = null;

            try {
                detectedFace =
                        findLargestFace(faces);

                synchronized (frameLock) {
                    localFrame.copyTo(cleanFrame);

                    if (lastFace != null) {
                        lastFace.close();
                        lastFace = null;
                    }

                    if (detectedFace != null) {
                        lastFace = new Rect(
                                detectedFace.x(),
                                detectedFace.y(),
                                detectedFace.width(),
                                detectedFace.height()
                        );
                    }
                }

                if (detectedFace != null) {
                    drawFaceRectangle(
                            localFrame,
                            detectedFace
                    );
                }

                boolean faceDetected =
                        detectedFace != null;

                updateFacePresence(faceDetected);
                requestRecognitionIfNecessary(
                        faceDetected
                );

                publishCameraFrame(
                        localFrame,
                        faceDetected
                );

            } finally {
                if (detectedFace != null) {
                    detectedFace.close();
                }
            }

        } catch (Exception exception) {
            System.err.println(
                    "Unable to process camera frame: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }
    private void publishCameraFrame(
            Mat frame,
            boolean faceDetected
    ) {
        if (!uiFramePending.compareAndSet(
                false,
                true
        )) {
            return;
        }

        final Image image;

        try {
            image = matToImage(frame);

        } catch (Exception exception) {
            uiFramePending.set(false);
            exception.printStackTrace();
            return;
        }

        Platform.runLater(() -> {
            try {
                cameraView.setPhoto(image);

                updateFaceBorder(
                        cameraView,
                        faceDetected
                );

                if (studentCameraView != null) {
                    studentCameraView.setPhoto(image);

                    updateFaceBorder(
                            studentCameraView,
                            faceDetected
                    );
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                uiFramePending.set(false);
            }
        });
    }
    private void drawFaceRectangle(
            Mat frame,
            Rect face
    ) {
        try (Point topLeft =
                     new Point(
                             face.x(),
                             face.y()
                     );

             Point bottomRight =
                     new Point(
                             face.x() + face.width(),
                             face.y() + face.height()
                     );

             Scalar green =
                     new Scalar(0, 255, 0, 0)) {

            rectangle(
                    frame,
                    topLeft,
                    bottomRight,
                    green,
                    2,
                    LINE_8,
                    0
            );
        }
    }

    private void updateFaceBorder(
            PhotoView photoView,
            boolean faceDetected
    ) {
        if (photoView == null) {
            return;
        }

        photoView.getStyleClass().removeAll(
                "border-circle-green",
                "border-circle-red"
        );

        photoView.getStyleClass().add(
                faceDetected
                        ? "border-circle-green"
                        : "border-circle-red"
        );
    }

    private boolean readCameraFrame(
            Mat destination
    ) {
        synchronized (cameraLock) {
            return camera != null
                    && camera.isOpened()
                    && camera.read(destination)
                    && !destination.empty();
        }
    }

    public void stopCamera() {
        ScheduledExecutorService timerToStop;
        VideoCapture cameraToClose;

        synchronized (cameraLock) {
            /*
             * Detach resources first so no new code obtains them from
             * the controller while shutdown is in progress.
             */
            timerToStop = timer;
            timer = null;

            cameraToClose = camera;
            camera = null;
        }

        if (timerToStop != null) {
            timerToStop.shutdownNow();

            /*
             * Do not await termination from the camera timer's own thread,
             * because that would make the thread wait for itself.
             */
            if (!Thread.currentThread()
                    .getName()
                    .equals("attendance-camera-capture")) {

                try {
                    if (!timerToStop.awaitTermination(
                            2,
                            TimeUnit.SECONDS
                    )) {
                        System.err.println(
                                "Camera executor did not terminate in time."
                        );
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            processing.set(false);
            resetRecognitionSession();
        }

        if (cameraToClose != null) {
            synchronized (cameraLock) {
                try {
                    cameraToClose.release();
                } catch (Exception exception) {
                    System.err.println(
                            "Unable to release camera: "
                                    + exception.getMessage()
                    );
                } finally {
                    try {
                        cameraToClose.close();
                    } catch (Exception exception) {
                        System.err.println(
                                "Unable to close camera: "
                                        + exception.getMessage()
                        );
                    }
                }
            }
        }

        synchronized (frameLock) {
            if (lastFace != null) {
                lastFace.close();
                lastFace = null;
            }
        }

        processing.set(false);
        resetRecognition();
    }

    private void switchCamera(
            int cameraIndex
    ) {
        if (!cameraSwitching.compareAndSet(false, true)) {
            return;
        }

        cameraList.setDisable(true);

        executor.submit(() -> {
            try {
                startCamera(cameraIndex);
            } finally {
                cameraSwitching.set(false);

                Platform.runLater(() ->
                        cameraList.setDisable(false)
                );
            }
        });
    }
    private Rect findLargestFace(RectVector faces) {
        if (faces == null || faces.size() == 0) {
            return null;
        }

        int largestX = 0;
        int largestY = 0;
        int largestWidth = 0;
        int largestHeight = 0;
        long largestArea = -1;

        for (long index = 0; index < faces.size(); index++) {
            Rect current = faces.get(index);

            if (current == null
                    || current.width() <= 0
                    || current.height() <= 0) {
                continue;
            }

            long area =
                    (long) current.width()
                            * current.height();

            if (area > largestArea) {
                largestArea = area;

                largestX = current.x();
                largestY = current.y();
                largestWidth = current.width();
                largestHeight = current.height();
            }
        }

        if (largestArea < 0) {
            return null;
        }

        /*
         * Return an independent Rect. The returned Rect must be closed
         * by the caller after it has finished using it.
         */
        return new Rect(
                largestX,
                largestY,
                largestWidth,
                largestHeight
        );
    }

    private final AtomicBoolean restartingCamera =
            new AtomicBoolean(false);

    private void restartCameraAsync() {
        if (!restartingCamera.compareAndSet(false, true)) {
            return;
        }

        executor.submit(() -> {
            try {
                restartCamera();
            } finally {
                restartingCamera.set(false);
            }
        });
    }

    private Image matToImage(Mat mat) {

        try (BytePointer buffer =
                     new BytePointer()) {

            imencode(
                    ".jpg",
                    mat,
                    buffer
            );

            byte[] bytes =
                    new byte[(int)
                            buffer.limit()];

            buffer.get(bytes);

            return new Image(
                    new ByteArrayInputStream(
                            bytes
                    )
            );
        }
    }


    private void tryIdentifying() {
        /*
         * Recognition must not begin while the model is being trained.
         */
        if (training.get()) {
            return;
        }

        /*
         * Do not predict using an unavailable or untrained recognizer.
         */
        if (!recognizerReady.get()) {
            return;
        }

        /*
         * Apply recognition throttling before claiming the processing flag.
         */
        long now = System.nanoTime();

        if (now - lastRecognitionTime < RECOGNITION_INTERVAL_NS) {
            return;
        }

        /*
         * Permit only one recognition request at a time.
         */
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        lastRecognitionTime = now;

        Mat capturedFace;

        /*
         * Copy the current face while holding the frame lock.
         * The worker thread receives an independent cloned Mat.
         */
        synchronized (frameLock) {
            if (training.get()
                    || lastFace == null
                    || cleanFrame.empty()) {

                processing.set(false);
                return;
            }

            try (Rect safeFace =
                         clampFaceToFrame(lastFace, cleanFrame)) {

                if (safeFace.width() <= 0
                        || safeFace.height() <= 0) {

                    processing.set(false);
                    return;
                }

                capturedFace =
                        new Mat(cleanFrame, safeFace).clone();
            } catch (Exception exception) {
                processing.set(false);
                exception.printStackTrace();
                return;
            }
        }

        try {
            executor.submit(() ->
                    recognizeCapturedFace(capturedFace)
            );
        } catch (RejectedExecutionException exception) {
            /*
             * The executor may reject work when the controller is closing.
             */
            capturedFace.close();
            processing.set(false);

            System.err.println(
                    "Recognition task was rejected because "
                            + "the executor is shutting down."
            );
        }
    }

    private void recognizeCapturedFace(
            Mat capturedFace
    ) {
        boolean resultQueuedToJavaFx = false;

        try (capturedFace;
             Mat faceGray = new Mat();
             Size targetSize =
                     new Size(FACE_SIZE, FACE_SIZE)) {

            if (training.get()) {
                return;
            }

            cvtColor(
                    capturedFace,
                    faceGray,
                    COLOR_BGR2GRAY
            );

            resize(
                    faceGray,
                    faceGray,
                    targetSize,
                    0,
                    0,
                    INTER_AREA
            );

            /*
             * Recognition preprocessing must match training
             * preprocessing.
             */
            equalizeHist(
                    faceGray,
                    faceGray
            );

            if (training.get()) {
                return;
            }

            int[] predictedLabel =
                    new int[1];

            double[] predictedConfidence =
                    new double[1];

            synchronized (recognizerLock) {
                if (training.get()
                        || !recognizerReady.get()
                        || recognizer == null) {

                    return;
                }

                recognizer.predict(
                        faceGray,
                        predictedLabel,
                        predictedConfidence
                );
                System.out.printf(
                        Locale.ROOT,
                        "Prediction: label=%d, confidence=%.2f, threshold=%.2f%n",
                        predictedLabel[0],
                        predictedConfidence[0],
                        CONFIDENCE_THRESHOLD
                );
            }

            int label =
                    predictedLabel[0];

            double confidence =
                    predictedConfidence[0];

            /*
             * The callback captures primitives, not native objects.
             */
            Platform.runLater(() -> {
                try {
                    processRecognitionResult(
                            label,
                            confidence
                    );
                } catch (Exception exception) {
                    exception.printStackTrace();

                    showRecognitionMessage(
                            "Unable to process recognition result."
                    );
                } finally {
                    processing.set(false);
                }
            });

            resultQueuedToJavaFx = true;

        } catch (Exception exception) {
            System.err.println(
                    "Face recognition failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

        } finally {
            /*
             * When no JavaFX callback was scheduled, reset processing
             * here. Otherwise, the callback resets it after processing
             * the attendance result.
             */
            if (!resultQueuedToJavaFx) {
                processing.set(false);
            }
        }
    }

    private void processRecognitionResult(
            int label,
            double confidence
    ) {
        /*
         * Ignore a delayed result if training started after prediction.
         */
        if (training.get()) {
            resetRecognition();
            return;
        }

        if (label < 0
                || !Double.isFinite(confidence)
                || confidence >= CONFIDENCE_THRESHOLD) {

            resetRecognition();
            return;
        }

        if (currentCandidate == label) {
            consecutiveMatches++;
            confidenceSum += confidence;
        } else {
            currentCandidate = label;
            consecutiveMatches = 1;
            confidenceSum = confidence;
        }

        if (consecutiveMatches
                < requiredRecognitionCount) {

            return;
        }

        int acceptedId =
                currentCandidate;

        double averageConfidence =
                confidenceSum / consecutiveMatches;

        /*
         * The current consecutive-match sequence is complete.
         */
        resetRecognition();

        if (!Double.isFinite(averageConfidence)
                || averageConfidence
                >= CONFIDENCE_THRESHOLD) {

            return;
        }

        /*
         * Prevent a continuously visible face from creating
         * repeated attendance actions.
         */
        if (!tryAcceptRecognition(acceptedId)) {
            return;
        }

        processAcceptedStudent(
                acceptedId,
                averageConfidence
        );
    }

    private synchronized boolean tryAcceptRecognition(
            int studentId
    ) {
        long now =
                System.currentTimeMillis();

        /*
         * The previously accepted student has not left the
         * camera view yet.
         */
        if (acceptedFaceMustLeave
                && studentId == lastAcceptedId) {

            return false;
        }

        /*
         * Also apply a time-based cooldown.
         */
        if (studentId == lastAcceptedId
                && now - lastAcceptedAt
                < RECOGNITION_COOLDOWN_MS) {

            return false;
        }

        lastAcceptedId = studentId;
        lastAcceptedAt = now;

        acceptedFaceMustLeave = true;
        consecutiveNoFaceFrames = 0;

        return true;
    }

    private void requestRecognitionIfNecessary(
            boolean faceDetected
    ) {
        if (!faceDetected) {
            return;
        }

        if (training.get()) {
            System.out.println(
                    "Recognition skipped: training is running."
            );

            return;
        }

        if (!recognizerReady.get()) {
            System.out.println(
                    "Recognition skipped: recognizer is not ready."
            );

            return;
        }

        if (processing.get()) {
            return;
        }

        if (modes == null) {
            System.out.println(
                    "Recognition skipped: ToggleGroup is null."
            );

            return;
        }

        Toggle selectedToggle =
                modes.getSelectedToggle();

        if (!(selectedToggle
                instanceof ToggleButton toggleButton)) {

            System.out.println(
                    "Recognition skipped: no mode selected."
            );

            return;
        }

        if (!"Face Detector".equals(
                toggleButton.getText()
        )) {
            return;
        }

        tryIdentifying();
    }

    private void processAcceptedStudent(
            int studentId,
            double averageConfidence
    ) {
        String studentKey =
                String.format(
                        Locale.ROOT,
                        "ED%02d",
                        studentId
                );

        var studentDataMap =
                homeController.studentAttendance
                        .mainController
                        .gradedDataLoader
                        .getStudentData();

        var student =
                studentDataMap.get(studentKey);

        if (student == null) {
            showRecognitionMessage(
                    "Recognized student was not found: "
                            + studentKey
            );

            return;
        }

        System.out.println(
                "Student ID: " + studentId
        );

        System.out.println(
                "Average confidence: "
                        + averageConfidence
        );

        var attendanceMap =
                homeController.studentAttendance
                        .attendanceMap;

        /*
         * Student has not checked in.
         */
        if (!attendanceMap.containsKey(studentKey)) {
            String checkInTime =
                    LocalTime.now().format(
                            DateTimeFormatter.ofPattern(
                                    "hh:mm a",
                                    Locale.ENGLISH
                            )
                    );

            homeController.studentAttendance
                    .updateCheckIn(
                            studentKey,
                            checkInTime
                    );

            attendanceRecord.put(
                    studentId,
                    new AttendanceStatus(
                            true,
                            false
                    )
            );

            String message =
                    studentKey
                            + " "
                            + student.name()
                            + " you are marked as present";

            showRecognitionMessage(message);
            speakAsync(message);

            return;
        }

        var attendance =
                attendanceMap.get(studentKey);

        if (attendance == null) {
            showRecognitionMessage(
                    "Attendance information is unavailable for "
                            + studentKey
            );

            return;
        }

        /*
         * Attendance has already been completed.
         */
        if (attendance.getCheck_out() != null) {
            showRecognitionMessage(
                    studentKey
                            + " attendance is already completed."
            );

            return;
        }

        processStudentCheckout(
                studentKey,
                studentId,
                attendance.getCheck_in()
        );
    }

    private void processStudentCheckout(
            String studentKey,
            int studentId,
            String checkInText
    ) {
        if (checkInText == null
                || checkInText.isBlank()) {

            showRecognitionMessage(
                    "Check-in time is unavailable for "
                            + studentKey
            );

            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "hh:mm a",
                        Locale.ENGLISH
                );

        final LocalTime checkInTime;

        try {
            checkInTime =
                    LocalTime.parse(
                            checkInText
                                    .trim()
                                    .toUpperCase(
                                            Locale.ENGLISH
                                    ),
                            formatter
                    );

        } catch (Exception exception) {
            showRecognitionMessage(
                    "Invalid check-in time for "
                            + studentKey
            );

            exception.printStackTrace();
            return;
        }

        Duration duration =
                Duration.between(
                        checkInTime,
                        LocalTime.now()
                );

        /*
         * Temporary midnight handling. Storing LocalDateTime in
         * the database would be more reliable.
         */
        if (duration.isNegative()) {
            duration = duration.plusDays(1);
        }

        long minutesSpent =
                duration.toMinutes();

        if (minutesSpent < 45) {
            showRecognitionMessage(
                    "Cannot checkout before 45 minutes. Spent "
                            + minutesSpent
                            + " minutes."
            );

            return;
        }

        homeController.studentAttendance
                .updateCheckOut(studentKey);

        attendanceRecord.put(
                studentId,
                new AttendanceStatus(
                        true,
                        true
                )
        );

        String message =
                studentKey + " checkout done";

        showRecognitionMessage(message);
        speakAsync(message);
    }

    private void speakAsync(
            String message
    ) {
        RealTimeTts currentTts =
                timeTts;

        if (!ttsReady.get()
                || currentTts == null
                || message == null
                || message.isBlank()) {

            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                currentTts.readAloud(message);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private void showRecognitionMessage(
            String message
    ) {
        if (outputMessage != null) {
            outputMessage.setText(message);
        }

        if (studentMessage != null) {
            studentMessage.setText(message);
        }
    }

    private synchronized void resetRecognitionSession() {
        resetRecognition();

        lastAcceptedId = -1;
        lastAcceptedAt = 0L;

        acceptedFaceMustLeave = false;
        consecutiveNoFaceFrames = 0;

        lastRecognitionTime = 0L;
    }

    public ObservableList<String> getAvailableCameras() {

        ObservableList<String> cameras =
                FXCollections.observableArrayList();

        for (int i = 0; i < 2; i++) {

            try (VideoCapture cap =
                         new VideoCapture(
                                 i,
                                 CAP_DSHOW
                         )) {

                if (cap.isOpened()) {

                    cameras.add(
                            "Camera " + i
                    );

                    cap.release();
                }
            } catch (Exception ignored) {
            }
        }

        return cameras;
    }

    private int currentCameraIndex = 0;

    private void restartCamera() {
        int cameraIndex =
                currentCameraIndex;

        try {
            Thread.sleep(1000);

            startCamera(cameraIndex);

            System.out.println(
                    "Camera restarted successfully."
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

        } catch (Exception exception) {
            System.err.println(
                    "Camera restart failed: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    private void startTraining(Task<?> task) throws Exception {
        printMemory("Training: beginning");
        File trainingRoot = new File(
                System.getProperty("user.home"),
                "gardeEdAttendanceData"
        );

        if (!trainingRoot.isDirectory()) {
            throw new IllegalStateException(
                    "Training data folder was not found: "
                            + trainingRoot.getAbsolutePath()
            );
        }

        File[] studentFolders =
                trainingRoot.listFiles(File::isDirectory);

        if (studentFolders == null
                || studentFolders.length == 0) {

            throw new IllegalStateException(
                    "No student folders were found in: "
                            + trainingRoot.getAbsolutePath()
            );
        }

        List<Mat> trainingImages =
                new ArrayList<>();

        List<Integer> trainingLabels =
                new ArrayList<>();

        int validStudentCount = 0;
        int totalImageCount = 0;
        boolean modelSaved = false;

        try {
            for (File studentFolder : studentFolders) {

                checkTrainingCancellation(task);

                Integer studentLabel =
                        parseStudentLabel(studentFolder);

                if (studentLabel == null) {
                    System.err.println(
                            "Ignoring invalid student folder: "
                                    + studentFolder.getName()
                    );

                    continue;
                }

                List<File> studentImages =
                        findTrainingImages(studentFolder);

                int validImagesForStudent = 0;

                for (File imageFile : studentImages) {

                    checkTrainingCancellation(task);

                    Mat loadedImage = null;
                    Mat preparedImage = null;

                    try {
                        loadedImage = imread(
                                imageFile.getAbsolutePath(),
                                IMREAD_GRAYSCALE
                        );

                        if (loadedImage == null
                                || loadedImage.empty()) {

                            System.err.println(
                                    "Unable to read training image: "
                                            + imageFile.getAbsolutePath()
                            );

                            continue;
                        }

                        /*
                         * Use a separate destination Mat instead of resizing
                         * the loaded image in place. This makes native-resource
                         * ownership much clearer.
                         */
                        preparedImage = new Mat();

                        try (Size faceSize =
                                     new Size(FACE_SIZE, FACE_SIZE)) {

                            resize(
                                    loadedImage,
                                    preparedImage,
                                    faceSize,
                                    0,
                                    0,
                                    INTER_AREA
                            );
                        }

                        equalizeHist(
                                preparedImage,
                                preparedImage
                        );

                        /*
                         * Ownership of preparedImage is transferred to
                         * trainingImages. It will be closed in the outer
                         * finally block after training completes.
                         */
                        trainingImages.add(preparedImage);
                        trainingLabels.add(studentLabel);

                        preparedImage = null;

                        validImagesForStudent++;
                        totalImageCount++;

                    } catch (Exception exception) {
                        System.err.println(
                                "Unable to prepare training image: "
                                        + imageFile.getAbsolutePath()
                        );

                        exception.printStackTrace();

                    } finally {
                        if (loadedImage != null) {
                            loadedImage.close();
                        }

                        /*
                         * If ownership was successfully transferred,
                         * preparedImage was set to null.
                         */
                        if (preparedImage != null) {
                            preparedImage.close();
                        }
                    }
                }

                if (validImagesForStudent > 0) {
                    validStudentCount++;
                }
            }

            checkTrainingCancellation(task);

            if (trainingImages.isEmpty()) {
                throw new IllegalStateException(
                        "No valid training images were found."
                );
            }

            if (trainingImages.size()
                    != trainingLabels.size()) {

                throw new IllegalStateException(
                        "Training image and label counts do not match."
                );
            }

            Path modelPath = Path.of(
                    System.getProperty("user.home"),
                    "attendance_model.yml"
            );

            Path temporaryModelPath = Path.of(
                    System.getProperty("user.home"),
                    "attendance_model.tmp.yml"
            );

            /*
             * Remove a temporary model left behind by a previous
             * interrupted or failed training operation.
             */
            Files.deleteIfExists(temporaryModelPath);

            try (MatVector imageVector =
                         new MatVector(trainingImages.size());

                 Mat labelMatrix =
                         new Mat(
                                 trainingLabels.size(),
                                 1,
                                 CV_32SC1
                         );

                 LBPHFaceRecognizer trainingRecognizer =
                         LBPHFaceRecognizer.create(
                                 1,  // radius
                                 8,  // neighbors
                                 8,  // grid X
                                 8,  // grid Y
                                 80  // internal prediction threshold
                         )) {

                for (int index = 0;
                     index < trainingImages.size();
                     index++) {

                    imageVector.put(
                            index,
                            trainingImages.get(index)
                    );
                }

                IntBuffer labelBuffer =
                        labelMatrix.createBuffer();

                for (int index = 0;
                     index < trainingLabels.size();
                     index++) {

                    labelBuffer.put(
                            index,
                            trainingLabels.get(index)
                    );
                }

                checkTrainingCancellation(task);

                trainingRecognizer.train(
                        imageVector,
                        labelMatrix
                );

                checkTrainingCancellation(task);

                /*
                 * Save to a temporary file first. This prevents a failed
                 * training operation from corrupting the working model.
                 */
                trainingRecognizer.save(
                        temporaryModelPath.toString()
                );

                if (!Files.isRegularFile(temporaryModelPath)
                        || Files.size(temporaryModelPath) == 0) {

                    throw new IllegalStateException(
                            "The trained model was not saved correctly."
                    );
                }

                replaceModelFile(
                        temporaryModelPath,
                        modelPath
                );

                modelSaved = true;
            }

            System.out.println(
                    "Training completed successfully"
            );

            System.out.println(
                    "Valid students: "
                            + validStudentCount
            );

            System.out.println(
                    "Valid images: "
                            + totalImageCount
            );

            System.out.println(
                    "Model: "
                            + Path.of(
                            System.getProperty("user.home"),
                            "attendance_model.yml"
                    )
            );

        } finally {
            /*
             * MatVector does not replace the need to close the individual
             * Mat objects that were loaded for training.
             */
            for (Mat trainingImage : trainingImages) {
                if (trainingImage != null) {
                    try {
                        trainingImage.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }

            trainingImages.clear();
            trainingLabels.clear();
            printMemory("Training: images loaded");
        }

        /*
         * Reload only after MatVector, labelMatrix, training recognizer,
         * and all training images have been released.
         *
         * The caller must keep recognition disabled while this method is
         * running.
         */
        if (modelSaved) {
            loadRecognizer();
            resetRecognition();
        }
    }

    private Integer parseStudentLabel(
            File studentFolder
    ) {
        if (studentFolder == null
                || !studentFolder.isDirectory()) {

            return null;
        }

        String folderName =
                studentFolder.getName().trim();

        if (!folderName.matches("(?i)ED\\d+")) {
            return null;
        }

        try {
            return Integer.parseInt(
                    folderName.substring(2)
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void checkTrainingCancellation(
            Task<?> task
    ) {
        if (task != null
                && task.isCancelled()) {

            throw new CancellationException(
                    "Model training was cancelled."
            );
        }

        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException(
                    "Model training thread was interrupted."
            );
        }
    }

    private void replaceModelFile(
            Path temporaryModel,
            Path destinationModel
    ) throws IOException, IOException {

        try {
            Files.move(
                    temporaryModel,
                    destinationModel,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            /*
             * Some filesystems do not support atomic moves.
             */
            Files.move(
                    temporaryModel,
                    destinationModel,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private List<File> findTrainingImages(File directory) {
        List<File> images = new ArrayList<>();

        File[] files = directory.listFiles();

        if (files == null) {
            return images;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                images.addAll(findTrainingImages(file));
            } else if (isSupportedImage(file)) {
                images.add(file);
            }
        }

        return images;
    }

    private boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();

        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".bmp");
    }
}