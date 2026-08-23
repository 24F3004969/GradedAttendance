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
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.FacemarkLBF;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.controller.home.HomeController;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.controller.tts.RealTimeTts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.IntBuffer;
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
        background.setSvgUrl(
                GradedResourceLoader.load(
                        "icons/new-back1.svg"
                )
        );
        var studentLogo = new SVGImageView();
        studentLogo.setFitHeight(48);
        studentLogo.setSvgUrl(
                GradedResourceLoader.load(
                        "icons/my-logo.svg"
                )
        );

        HBox logoBar = new HBox(studentLogo);
        VBox.setVgrow(logoBar, Priority.ALWAYS);
        logoBar.setAlignment(Pos.BOTTOM_CENTER);
        VBox.setMargin(logoBar, new Insets(8));
        studentCameraView = new PhotoView();
        studentCameraView.setEditable(false);

        studentCameraView.setMinSize(512, 512);
        studentCameraView.setMaxSize(800, 800);

        studentCameraView.getStyleClass()
                .add("border-circle-green");

        studentMessage = new Label(
                "Please stand in front of the camera"
        );

        studentMessage.setStyle("""
                -fx-font-size: 22px;
                -fx-font-weight: bold;
                """);

        VBox content = new VBox(
                20,
                studentCameraView,
                studentMessage
        );

        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox mainContent = new VBox(content, logoBar);

        StackPane root = new StackPane(background, mainContent);
        Scene scene = new Scene(root, 1280, 720);

        scene.getStylesheets().add(
                GradedResourceLoader.load(
                        "css/camera-style.css"
                )
        );

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
        var toggle = (ToggleButton) event.getSource();
        if (toggle.getText().equals("Training")) {
            addStudent.setDisable(false);
            searchBox.setDisable(false);
            checkBoxGroup.setVisible(true);
            startCapture.setText("Start Training");
            startCapture.setDisable(false);
        } else if (toggle.getText().equals("Face Detector")) {
            startCapture.setDisable(false);
            addStudent.setDisable(true);
            searchBox.setDisable(true);
            scrollAtt.setVisible(false);
            startCapture.setDisable(true);
            checkBoxGroup.setVisible(false);
        }
    }

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
            if (lastFace == null || cleanFrame.empty()) {
                System.out.println("No face detected.");
                return;
            }

            Rect safeFace = clampFaceToFrame(lastFace, cleanFrame);

            if (safeFace.width() <= 0 || safeFace.height() <= 0) {
                System.out.println("Invalid face area.");
                return;
            }

            capturedFace = new Mat(cleanFrame, safeFace).clone();
        }

        executor.submit(() -> saveCapturedFace(capturedFace, id));
    }

    private void saveCapturedFace(Mat capturedFace, String id) {
        try (capturedFace; Mat faceGray = new Mat()) {
            cvtColor(
                    capturedFace,
                    faceGray,
                    COLOR_BGR2GRAY
            );

            // Keep training and recognition preprocessing consistent.
            resize(
                    faceGray,
                    faceGray,
                    new Size(FACE_SIZE, FACE_SIZE)
            );

            equalizeHist(faceGray, faceGray);

            File directory = new File(imagePath, id);

            if (!directory.exists() && !directory.mkdirs()) {
                System.err.println(
                        "Unable to create directory: "
                                + directory.getAbsolutePath()
                );
                return;
            }

            File outputFile = new File(
                    directory,
                    System.currentTimeMillis() + ".jpg"
            );

            boolean saved = imwrite(
                    outputFile.getAbsolutePath(),
                    faceGray
            );

            if (saved) {
                System.out.println(
                        "Photo saved: " + outputFile.getAbsolutePath()
                );
            } else {
                System.err.println(
                        "Failed to save photo: "
                                + outputFile.getAbsolutePath()
                );
            }
        } catch (Exception exception) {
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

    private record Result(String ed, String name) {
    }

    RealTimeTts timeTts;

    @FXML
    public void initialize() {
        initializeStudentSearch();
        initializeGraphics();
        initializeTtsAsync();
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
        searchBox.setOnCommit(event -> {
            System.out.println(event);

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
                    //loadFacemark();
                    //create3DModel();

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

    private void switchCamera(int cameraIndex) {
        if (!cameraSwitching.compareAndSet(false, true)) {
            return;
        }

        try {
            stopCamera();

            currentCameraIndex = cameraIndex;

            startCamera(cameraIndex);
        } finally {
            cameraSwitching.set(false);
        }
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
    public void tryToDetect() {

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);

        Label statusLabel = new Label("Preparing training...");

        VBox content = new VBox(10,
                statusLabel,
                progressBar
        );
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        com.dlsc.gemsfx.DialogPane dialogPane = new com.dlsc.gemsfx.DialogPane();

        com.dlsc.gemsfx.DialogPane.Dialog<ButtonType> dialog =
                new com.dlsc.gemsfx.DialogPane.Dialog<>(
                        dialogPane,
                        com.dlsc.gemsfx.DialogPane.Type.INFORMATION
                );

        dialog.setTitle("Model Training");
        dialog.setContent(content);

        rootLayer.getChildren()
                .add(dialogPane);

        Task<Void> trainingTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                startTraining(this);

                return null;
            }
        };

        progressBar.progressProperty()
                .bind(trainingTask.progressProperty());

        statusLabel.textProperty()
                .bind(trainingTask.messageProperty());

        trainingTask.setOnSucceeded(e -> {

            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();

            statusLabel.setText(
                    "Training completed successfully"
            );

            dialog.cancel();
        });

        trainingTask.setOnFailed(e -> {

            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();

            statusLabel.setText(
                    "Training failed"
            );

            trainingTask.getException()
                    .printStackTrace();
        });

        dialog.show();

        Thread thread = new Thread(trainingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void tryIdentifying() {
        if (!processing.compareAndSet(false, true)) {
            System.out.println("Recognition already running");
            return;
        }
        Mat capturedFace;
        synchronized (frameLock) {
            if (lastFace == null) {
                processing.set(false);
                System.out.println("No face detected.");
                return;
            }
            Rect roi = new Rect(lastFace.x(), lastFace.y(), lastFace.width(), lastFace.height());
            capturedFace = new Mat(cleanFrame, roi).clone();
        }

        executor.submit(() -> {
            Mat faceGray = new Mat();
            cvtColor(capturedFace, faceGray, COLOR_BGR2GRAY);
            resize(faceGray, faceGray, new Size(FACE_SIZE, FACE_SIZE));
            int[] label = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(faceGray, label, confidence);
           /* Platform.runLater(() -> {
                try {
                    if (tempAttendanceRecord.isEmpty() || tempAttendanceRecord.firstEntry().getValue().size() < requiredRecognitionCount) {
                        if (confidence[0] < CONFIDENCE_THRESHOLD) {
                            if (tempAttendanceRecord.containsKey(label[0]))
                                tempAttendanceRecord.get(label[0]).add(confidence[0]);
                            else if (tempAttendanceRecord.size() == 1)
                                tempAttendanceRecord.clear();
                            else
                                tempAttendanceRecord.put(label[0], new ArrayList<>(List.of(confidence[0])));
                        } else {
                            System.out.println("Unknown Person");
                        }
                    } else {
                        double sum = tempAttendanceRecord.firstEntry().
                                getValue().stream().mapToDouble(x -> x).sum();
                        int id = tempAttendanceRecord.firstEntry().getKey();
                        double v = sum / tempAttendanceRecord.firstEntry().
                                getValue().size();
                        if (v < CONFIDENCE_THRESHOLD && id == label[0]) {
                            System.out.println("Student ID: " + id);
                            System.out.println("Confidence: " + v);
                            String key = "ED" + (id < 10 ? (0 + "" + id) : id);
                            if (!attendanceRecord.containsKey(id) && !homeController.studentAttendance.
                                    attendanceMap.containsKey(key)) {
                                attendanceRecord.put(id, new AttendanceStatus(true, false));
                                homeController.studentAttendance.updateCheckIn(key);
                                String name = homeController.studentAttendance.mainController.gradedDataLoader.
                                        getStudentData().get(key).name();
                                outputMessage.setText("  ED" + id + "  " + name + " you are marked as present");
                                CompletableFuture.runAsync(() -> timeTts.readAloud("  ED" + id + "  " + name + " you are marked as present"));
                            } else if (homeController.studentAttendance.attendanceMap.get(key).getCheck_out() == null) {
                                var attendanceRecord = homeController.studentAttendance.attendanceMap.get(key);
                                String in = attendanceRecord.getCheck_in();
                                DateTimeFormatter formatter =
                                        DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

                                LocalTime givenTime =
                                        LocalTime.parse(in.toUpperCase(Locale.ENGLISH), formatter);
                                LocalTime currentTime = LocalTime.now();
                                Duration duration = Duration.between(givenTime, currentTime);
                                if (duration.toMinutes() >= 45) {
                                    System.out.println("your check out is done");
                                    homeController.studentAttendance.updateCheckOut(key);
                                    outputMessage.setText("  ED" + id + "  checkout done");
                                    CompletableFuture.runAsync(() -> timeTts.readAloud("  ED" + id + "  checkout done"));
                                } else
                                    System.out.println("You cannot checkout before 45 minutes you have spent only " + duration.toMinutes());
                            }

                        } else {
                            tempAttendanceRecord.clear();
                        }

                    }
                } finally {
                    processing.set(false);
                }
            });*/
            Platform.runLater(() -> {
                try {

                    if (confidence[0] < CONFIDENCE_THRESHOLD) {

                        if (currentCandidate == label[0]) {

                            consecutiveMatches++;
                            confidenceSum += confidence[0];

                        } else {

                            currentCandidate = label[0];
                            consecutiveMatches = 1;
                            confidenceSum = confidence[0];
                        }

                        if (consecutiveMatches >= requiredRecognitionCount) {

                            int id = currentCandidate;

                            double avgConfidence =
                                    confidenceSum / consecutiveMatches;

                            if (avgConfidence < CONFIDENCE_THRESHOLD
                                    && id == label[0]) {

                                System.out.println("Student ID: " + id);
                                System.out.println(
                                        "Average Confidence: "
                                                + avgConfidence
                                );

                                String key =
                                        "ED" + (id < 10
                                                ? "0" + id
                                                : id);

                                if (!attendanceRecord.containsKey(id)
                                        && !homeController.studentAttendance
                                        .attendanceMap.containsKey(key)) {

                                    attendanceRecord.put(
                                            id,
                                            new AttendanceStatus(
                                                    true,
                                                    false
                                            )
                                    );

                                    homeController.studentAttendance
                                            .updateCheckIn(key, LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));

                                    String name =
                                            homeController.studentAttendance
                                                    .mainController
                                                    .gradedDataLoader
                                                    .getStudentData()
                                                    .get(key)
                                                    .name();

                                    outputMessage.setText(
                                            "ED"
                                                    + id
                                                    + " "
                                                    + name
                                                    + " you are marked as present"
                                    );
                                    if (studentMessage != null)
                                        studentMessage.setText(
                                                "ED"
                                                        + id
                                                        + " "
                                                        + name
                                                        + " you are marked as present"
                                        );

                                    CompletableFuture.runAsync(
                                            () -> {
                                                timeTts.readAloud(
                                                        "ED"
                                                                + id
                                                                + " "
                                                                + name
                                                                + " you are marked as present"
                                                );
                                            }
                                    );

                                } else if (
                                        homeController.studentAttendance
                                                .attendanceMap.containsKey(key)
                                                && homeController.studentAttendance
                                                .attendanceMap.get(key)
                                                .getCheck_out() == null
                                ) {

                                    var attendance =
                                            homeController.studentAttendance
                                                    .attendanceMap.get(key);

                                    String in =
                                            attendance.getCheck_in();

                                    DateTimeFormatter formatter =
                                            DateTimeFormatter.ofPattern(
                                                    "hh:mm a",
                                                    Locale.ENGLISH
                                            );

                                    LocalTime givenTime =
                                            LocalTime.parse(
                                                    in.toUpperCase(
                                                            Locale.ENGLISH
                                                    ),
                                                    formatter
                                            );

                                    LocalTime currentTime =
                                            LocalTime.now();

                                    Duration duration =
                                            Duration.between(
                                                    givenTime,
                                                    currentTime
                                            );

                                    if (duration.toMinutes() >= 45) {

                                        homeController.studentAttendance
                                                .updateCheckOut(key);

                                        outputMessage.setText(
                                                "ED"
                                                        + id
                                                        + " checkout done"
                                        );
                                        studentMessage.setText(
                                                "ED"
                                                        + id
                                                        + " checkout done"
                                        );
                                        CompletableFuture.runAsync(
                                                () -> {
                                                    timeTts.readAloud(
                                                            "ED"
                                                                    + id
                                                                    + " checkout done"
                                                    );
                                                }
                                        );

                                    } else {
                                        if (studentMessage != null)
                                            studentMessage.setText(
                                                    "Cannot checkout before 45 minutes. Spent "
                                                            + duration.toMinutes()
                                                            + " minutes."
                                            );
                                    }
                                }
                            }

                            resetRecognition();
                        }

                    } else {

                        System.out.println("Unknown Person");
                        resetRecognition();
                    }

                } finally {

                    processing.set(false);
                }
            });
        });
    }

    private int currentCandidate = -1;
    private int consecutiveMatches = 0;
    private double confidenceSum = 0;

    private void resetRecognition() {
        currentCandidate = -1;
        consecutiveMatches = 0;
        confidenceSum = 0;
    }

    private FacemarkLBF facemark;

    private void loadFacemark() {
        try {
            File modelFile = extractResourceToTempFile(
                    "/org/graded_classes/graded_attendance/opencv/lbfmodel.yaml",
                    "lbfmodel-",
                    ".yaml"
            );

            facemark = FacemarkLBF.create();
            facemark.loadModel(modelFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
        if (recognizer != null) {
            recognizer.close();
        }
        recognizer = LBPHFaceRecognizer.create();

        try {

            recognizer.read(System.getProperty("user.home") + "/attendance_model.yml");

            System.out.println(
                    "Attendance model loaded."
            );

        } catch (Exception e) {
            System.out.println(
                    "Could not load model: "
                            + e.getMessage()
            );
        }
    }

    private void startCamera(int cameraIndex) {
        currentCameraIndex = cameraIndex;
        camera = new VideoCapture(
                cameraIndex,
                CAP_DSHOW
        );

        if (!camera.isOpened()) {

            System.out.println(
                    "Failed to open camera "
                            + cameraIndex
            );

            return;
        }

        camera.set(
                CAP_PROP_FRAME_WIDTH,
                1280
        );

        camera.set(
                CAP_PROP_FRAME_HEIGHT,
                720
        );

        timer = Executors
                .newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(
                this::grabFrame,
                0,
                100,
                TimeUnit.MILLISECONDS
        );
    }

    private void grabFrame() {
        Mat localFrame = new Mat();
        Mat localGray = new Mat();

        try {
            VideoCapture currentCamera = camera;

            if (currentCamera == null ||
                    !currentCamera.isOpened() ||
                    !currentCamera.read(localFrame) ||
                    localFrame.empty()) {

                System.out.println("Camera frame lost.");
                restartCameraAsync();
                return;
            }

            flip(localFrame, localFrame, 1);
            cvtColor(localFrame, localGray, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();

            try {
                faceDetector.detectMultiScale(
                        localGray,
                        faces,
                        1.1,
                        5,
                        0,
                        new Size(50, 50),
                        new Size()
                );

                Rect detectedFace = findLargestFace(faces);

                /*
                 * Publish the clean frame and detected rectangle quickly.
                 * Do not perform expensive processing while holding the lock.
                 */
                synchronized (frameLock) {
                    localFrame.copyTo(cleanFrame);

                    lastFace = detectedFace == null
                            ? null
                            : new Rect(
                            detectedFace.x(),
                            detectedFace.y(),
                            detectedFace.width(),
                            detectedFace.height()
                    );
                }

                if (detectedFace != null) {
                    // Use localGray/localFrame instead of shared gray/frame.
                   /* faceMovementDetection(
                            localFrame,
                            localGray,
                            detectedFace
                    );*/

                    rectangle(
                            localFrame,
                            new Point(
                                    detectedFace.x(),
                                    detectedFace.y()
                            ),
                            new Point(detectedFace.x() + detectedFace.width(), detectedFace.y() + detectedFace.height()),
                            new Scalar(0, 255, 0, 0),
                            2,
                            LINE_8,
                            0
                    );
                }

                Image image = matToImage(localFrame);
                boolean faceDetected = detectedFace != null;

                Platform.runLater(() -> {
                    cameraView.setPhoto(image);

                    if (cameraView.getStyleClass().size() > 1) {
                        if (faceDetected)
                            if (((ToggleButton) modes.getSelectedToggle()).getText().equals("Face Detector"))
                                tryIdentifying();
                        cameraView.getStyleClass().set(
                                1,
                                faceDetected ? "border-circle-green" : "border-circle-red"
                        );
                        if (studentCameraView != null)
                            studentCameraView.getStyleClass().set(
                                    1,
                                    faceDetected ? "border-circle-green" : "border-circle-red"
                            );
                    }
                    if (studentCameraView != null) {
                        studentCameraView.setPhoto(image);
                    }
                });
            } finally {
                faces.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            localFrame.close();
            localGray.close();
        }
    }

    private void faceMovementDetection(
            Mat outputFrame,
            Mat grayFrame,
            Rect largestFace
    ) {
        try (RectVector faceVector = new RectVector(1)) {
            faceVector.put(0, largestFace);

            try (Point2fVectorVector landmarks =
                         new Point2fVectorVector()) {

                boolean success = facemark.fit(
                        grayFrame,
                        faceVector,
                        landmarks
                );

                if (success && landmarks.size() > 0) {
                    Point2fVector points = landmarks.get(0);

                    // Remaining pose calculation...

                    for (long i = 0; i < points.size(); i++) {
                        Point2f point = points.get(i);

                        circle(
                                outputFrame,
                                new Point(
                                        Math.round(point.x()),
                                        Math.round(point.y())
                                ),
                                4,
                                new Scalar(0, 255, 0, 0),
                                FILLED,
                                LINE_8,
                                0
                        );
                    }
                }
            }
        }
    }

    private Rect findLargestFace(RectVector faces) {
        if (faces == null || faces.size() == 0) {
            Platform.runLater(() -> {
                if (studentMessage != null)
                    studentMessage.setText("Please stand in front of the camera");
            });
            return null;
        }

        Rect first = faces.get(0);

        Rect largest = new Rect(
                first.x(),
                first.y(),
                first.width(),
                first.height()
        );

        for (long i = 1; i < faces.size(); i++) {
            Rect current = faces.get(i);

            if (current.area() > largest.area()) {
                largest.close();

                largest = new Rect(
                        current.x(),
                        current.y(),
                        current.width(),
                        current.height()
                );
            }
        }

        return largest;
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

    private void create3DModel() {
        DoubleIndexer obj;
        objectPoints = new Mat(6, 3, CV_64FC1);
        obj = objectPoints.createIndexer();


        obj.put(0, 0, 0.0);
        obj.put(0, 1, 0.0);
        obj.put(0, 2, 0.0);      // Nose
        obj.put(1, 0, 0.0);
        obj.put(1, 1, -330.0);
        obj.put(1, 2, -65.0);    // Chin
        obj.put(2, 0, -225.0);
        obj.put(2, 1, 170.0);
        obj.put(2, 2, -135.0);   // Left eye
        obj.put(3, 0, 225.0);
        obj.put(3, 1, 170.0);
        obj.put(3, 2, -135.0);   // Right eye
        obj.put(4, 0, -150.0);
        obj.put(4, 1, -150.0);
        obj.put(4, 2, -125.0);   // Left mouth
        obj.put(5, 0, 150.0);
        obj.put(5, 1, -150.0);
        obj.put(5, 2, -125.0);   // Right mouth
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


    public void stopCamera() {

        if (timer != null) {

            timer.shutdownNow();

            timer = null;
        }

        if (camera != null) {

            camera.release();

            camera.close();

            camera = null;
        }
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

        try {

            if (camera != null) {
                camera.release();
                camera.close();
            }

            Thread.sleep(1000);

            camera = new VideoCapture(
                    currentCameraIndex,
                    CAP_DSHOW
            );

            camera.set(
                    CAP_PROP_FRAME_WIDTH,
                    1280
            );

            camera.set(
                    CAP_PROP_FRAME_HEIGHT,
                    720
            );

            System.out.println(
                    "Camera restarted."
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void startTraining(Task<?> task) {

        String dataPath = System.getProperty("user.home") +
                "/gardeEdAttendanceData";

        File root = new File(dataPath);

        if (!root.exists()) {
            System.out.println(dataPath);
            System.out.println("Data folder not found");
            return;
        }

        List<Mat> imageList = new ArrayList<>();
        List<Integer> labelList = new ArrayList<>();

        File[] studentFolders = root.listFiles(File::isDirectory);

        if (studentFolders == null ||
                studentFolders.length == 0) {

            System.out.println("No student folders found");
            return;
        }

        int totalImages = 0;
        System.out.println(studentFolders.length);
        for (File studentFolder : studentFolders) {

            int label;

            try {
                label = Integer.parseInt(
                        studentFolder.getName().replace("ED", "")
                );
            } catch (NumberFormatException e) {
                continue;
            }
            List<File> images = findTrainingImages(studentFolder);

            for (File imageFile : images) {

                Mat image = imread(
                        imageFile.getAbsolutePath(),
                        0
                );

                if (image.empty()) {
                    continue;
                }

                resize(
                        image,
                        image,
                        new Size(200, 200)
                );

                equalizeHist(
                        image,
                        image
                );

                imageList.add(image);
                labelList.add(label);

                totalImages++;
            }
        }

        if (imageList.isEmpty()) {

            System.out.println(
                    "No valid training images found"
            );
            return;
        }

        MatVector images =
                new MatVector(imageList.size());

        for (int i = 0; i < imageList.size(); i++) {

            images.put(
                    i,
                    imageList.get(i)
            );
        }

        Mat labels = new Mat(
                labelList.size(),
                1,
                CV_32SC1
        );

        IntBuffer buffer =
                labels.createBuffer();

        for (int i = 0; i < labelList.size(); i++) {

            buffer.put(
                    i,
                    labelList.get(i)
            );
        }
        try (
                LBPHFaceRecognizer recognizer =
                        LBPHFaceRecognizer.create(
                                1,      // radius
                                8,      // neighbors
                                8,      // gridX
                                8,      // gridY
                                80      // threshold
                        )
        ) {

            recognizer.train(
                    images,
                    labels
            );

            recognizer.save(
                    System.getProperty("user.home") + "/attendance_model.yml"
            );

            System.out.println(
                    "Training completed successfully"
            );

            System.out.println(
                    "Students : "
                            + studentFolders.length
            );

            System.out.println(
                    "Images : "
                            + totalImages
            );

            System.out.println(
                    "Model : attendance_model.yml"
            );
            loadRecognizer();
            resetRecognition();
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