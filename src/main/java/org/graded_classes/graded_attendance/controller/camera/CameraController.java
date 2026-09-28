package org.graded_classes.graded_attendance.controller.camera;

import com.dlsc.gemsfx.PhotoView;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.controller.home.HomeController;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.kordamp.ikonli.javafx.FontIcon;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.graded_classes.graded_attendance.controller.quiz.QuizTaker.extractResourceToTempFile;

public final class CameraController {
    private static final int CAMERA_WIDTH = 1280;
    private static final int CAMERA_HEIGHT = 720;
    private static final int CAMERA_FPS = 30;
    private static final int DETECTION_WIDTH = 640;
    private static final int DETECTION_HEIGHT = 360;
    private static final long FRAME_DELAY_MS = 66;
    private static final long UI_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(150);
    private static final long RECOGNITION_INTERVAL_NS = TimeUnit.MILLISECONDS.toNanos(300);
    private static final long RECOGNITION_COOLDOWN_MS = 5_000L;
    private static final int REQUIRED_NO_FACE_FRAMES = 8;
    private static final int MAX_CAMERA_INDEX = 5;
    private static final int MAX_READ_FAILURES = 5;
    @FXML
    private Button rebuildAllButton;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.60;
    private static final double DEFAULT_SCORE_MARGIN = 0.06;
    private static final int DEFAULT_REQUIRED_CHECKS = 3;
    private static final double DETECTION_SCORE_THRESHOLD = 0.85;
    private static final double NMS_THRESHOLD = 0.30;
    private static final int TOP_K = 5000;
    private static final String YUNET_RESOURCE =
            "/org/graded_classes/graded_attendance/opencv/face_detection_yunet_2023mar.onnx";
    private static final String SFACE_RESOURCE =
            "/org/graded_classes/graded_attendance/opencv/face_recognition_sface_2021dec.onnx";
    private final AtomicBoolean folderTraining =
            new AtomicBoolean(false);
    private final MainController mainController;
    private final HomeController homeController;
    @FXML
    private PhotoView cameraView;
    @FXML
    private Button addStudent;
    @FXML
    private ScrollPane scrollAtt;
    @FXML
    private VBox glasses;
    @FXML
    private VBox hijab;
    @FXML
    private HBox checkBoxGroup;
    @FXML
    private ComboBox<CameraDevice> cameraList;
    @FXML
    private SearchField<String> searchBox;
    @FXML
    private Button startCapture;
    @FXML
    private SVGImageView myLogo;
    @FXML
    private SVGImageView background;
    @FXML
    private Label gFront, hFront, ldFront, llFront, nFront, nDown, nLeft, nRight, outputMessage;
    @FXML
    public ToggleGroup modes;
    private final ObservableList<String> studentData = FXCollections.observableArrayList();
    private final List<Button> completedCaptureButtons = new ArrayList<>();
    private final Map<String, List<float[]>> knownEmbeddings = new ConcurrentHashMap<>();

    @FXML
    private void rebuildAllStudents() {
        if (!modelsReady.get()
                || detector == null
                || recognizer == null) {
            showRecognitionMessage(
                    "YuNet and SFace are still loading."
            );
            return;
        }
        if (!folderTraining.compareAndSet(false, true)) {
            showRecognitionMessage(
                    "Another training operation is already running."
            );
            return;
        }
        Path trainingRoot = Path.of(
                System.getProperty("user.home"),
                "gardeEdAttendanceData"
        );
        if (!Files.isDirectory(trainingRoot)) {
            folderTraining.set(false);
            showRecognitionMessage(
                    "Training folder was not found: "
                            + trainingRoot
            );
            return;
        }
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
        );
        confirmation.setTitle(
                "Rebuild All Face Embeddings"
        );
        confirmation.setHeaderText(
                "Rebuild embeddings for every student?"
        );
        confirmation.setContentText(
                """
                        This operation will process every valid student folder \
                        and replace the current face-embedding database.
                        
                        The previous embeddings will remain unchanged \
                        if processing fails before database replacement."""
        );
        Optional<ButtonType> result =
                confirmation.showAndWait();
        if (result.isEmpty()
                || result.get() != ButtonType.OK) {
            folderTraining.set(false);
            return;
        }
        startRebuildAllTask(trainingRoot);
    }

    private void startRebuildAllTask(
            Path trainingRoot
    ) {
        Dialog<ButtonType> dialog =
                new Dialog<>();
        dialog.setTitle(
                "Rebuilding All Face Embeddings"
        );
        Label statusLabel =
                new Label(
                        "Scanning student folders..."
                );
        statusLabel.setWrapText(true);
        ProgressBar progressBar =
                new ProgressBar(0);
        progressBar.setPrefWidth(520);
        Label warningLabel = new Label(
                "Do not close the application while "
                        + "the embedding database is being saved."
        );
        warningLabel.setWrapText(true);
        warningLabel.getStyleClass()
                .add("warning-label");
        VBox content = new VBox(
                12,
                statusLabel,
                progressBar,
                warningLabel
        );
        content.setPadding(
                new Insets(16)
        );
        dialog.getDialogPane()
                .setContent(content);
        ButtonType cancelButton =
                new ButtonType(
                        "Cancel",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );
        dialog.getDialogPane()
                .getButtonTypes()
                .setAll(cancelButton);
        FolderTrainingTask task =
                new FolderTrainingTask(
                        trainingRoot
                );
        progressBar.progressProperty()
                .bind(
                        task.progressProperty()
                );
        statusLabel.textProperty()
                .bind(
                        task.messageProperty()
                );
        setEmbeddingControlsDisabled(true);
        dialog.setOnCloseRequest(event -> {
            if (task.isRunning()) {
                task.cancel(true);
            }
        });
        task.setOnSucceeded(event -> {
            cleanupTrainingDialog(
                    dialog,
                    progressBar,
                    statusLabel
            );

            TrainingSummary summary =
                    task.getValue();

            String message = String.format(
                    Locale.ROOT,
                    "Rebuild complete: %d students, "
                            + "%d embeddings and "
                            + "%d rejected images.",
                    summary.students(),
                    summary.embeddings(),
                    summary.rejected()
            );

            /*
             * Reset before displaying the completion message because
             * resetTrainingCaptureUi() clears the search and capture UI.
             */
            resetTrainingCaptureUi();

            showRecognitionMessage(message);
        });
        task.setOnFailed(event -> {
            cleanupTrainingDialog(
                    dialog,
                    progressBar,
                    statusLabel
            );
            Throwable exception =
                    task.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
            showError(
                    "Rebuild-all operation failed",
                    exception == null
                            ? "Unknown rebuild error."
                            : rootMessage(exception)
            );
        });
        task.setOnCancelled(event -> {
            cleanupTrainingDialog(
                    dialog,
                    progressBar,
                    statusLabel
            );
            showRecognitionMessage(
                    "Rebuild-all operation was cancelled. "
                            + "Existing embeddings were kept."
            );
        });
        dialog.show();
        try {
            worker.submit(task);
        } catch (RejectedExecutionException exception) {
            cleanupTrainingDialog(
                    dialog,
                    progressBar,
                    statusLabel
            );
            showError(
                    "Unable to start rebuild",
                    "The face-recognition worker "
                            + "is shutting down."
            );
        }
    }

    private void cleanupTrainingDialog(
            Dialog<ButtonType> dialog,
            ProgressBar progressBar,
            Label statusLabel
    ) {
        progressBar.progressProperty()
                .unbind();
        statusLabel.textProperty()
                .unbind();
        folderTraining.set(false);
        processing.set(false);
        setEmbeddingControlsDisabled(false);
        dialog.close();
    }

    private void setEmbeddingControlsDisabled(
            boolean disabled
    ) {
        if (rebuildAllButton != null) {
            rebuildAllButton.setDisable(disabled);
        }
        if (addStudent != null) {
            addStudent.setDisable(disabled);
        }
        if (searchBox != null) {
            searchBox.setDisable(disabled);
        }
        if (startCapture != null) {
            startCapture.setDisable(disabled);
        }
        if (cameraList != null) {
            cameraList.setDisable(disabled);
        }
        if (checkBoxGroup != null) {
            checkBoxGroup.setDisable(disabled);
        }
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "attendance-face-worker");
        t.setDaemon(true);
        return t;
    });
    private ScheduledExecutorService cameraTimer;
    private final Object cameraLock = new Object();
    private final Object frameLock = new Object();
    private final Object modelLock = new Object();
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicBoolean uiFramePending = new AtomicBoolean(false);
    private final AtomicBoolean cameraSwitching = new AtomicBoolean(false);
    private final AtomicBoolean restartingCamera = new AtomicBoolean(false);
    private final AtomicBoolean recognitionMode = new AtomicBoolean(false);
    private final AtomicBoolean modelsReady = new AtomicBoolean(false);
    private VideoCapture camera;
    private FaceDetectorYN detector;
    private FaceRecognizerSF recognizer;
    private final Mat cleanFrame = new Mat();
    private Mat lastFaceRow;
    private int currentCameraIndex;
    private int consecutiveReadFailures;
    private long lastUiFrameTime;
    private long lastRecognitionTime;
    private double similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;
    private double scoreMargin = DEFAULT_SCORE_MARGIN;
    private int requiredRecognitionCount = DEFAULT_REQUIRED_CHECKS;
    private String currentCandidate;
    private int consecutiveMatches;
    private double scoreSum;
    private String lastAcceptedEdNo;
    private long lastAcceptedAt;
    private int consecutiveNoFaceFrames;
    private boolean acceptedFaceMustLeave;
    private String imagePath;
    private Result selectedStudent;
    private Stage studentStage;
    private PhotoView studentCameraView;
    private Label studentMessage;
    private Boolean mainViewFaceState;
    private Boolean studentViewFaceState;

    public CameraController(MainController mainController, HomeController homeController) {
        this.mainController = Objects.requireNonNull(mainController);
        this.homeController = Objects.requireNonNull(homeController);
    }

    @FXML
    public void initialize() {
        initializeStudentSearch();
        initializeGraphics();
        loadCameraSettings();
        initializeAsync();
    }

    private Connection connection() throws SQLException {
        return mainController.gradedDataLoader.databaseLoader.getConnection();
    }

    private void initializeAsync() {
        cameraList.setDisable(true);
        startCapture.setDisable(true);
        CompletableFuture.supplyAsync(() -> {
            initializeSchema();
            loadModels();
            reloadEmbeddings();
            return scanCameras();
        }, worker).whenComplete((devices, error) -> runFx(() -> {
            if (error != null) {
                showError("Face system initialization failed", rootMessage(error));
                return;
            }
            configureCameraList(devices);
        }));
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS FaceEmbedding (
                embedding_id INTEGER PRIMARY KEY AUTOINCREMENT,
                ed_no TEXT NOT NULL,
                capture_mode TEXT,
                embedding BLOB NOT NULL,
                embedding_length INTEGER NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """;
        try (Statement statement = connection().createStatement()) {
            statement.execute(sql);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_face_embedding_ed_no ON FaceEmbedding(ed_no)");
        } catch (SQLException e) {
            throw new CompletionException(e);
        }
    }

    private void loadModels() {
        try {
            File yunet = extractResourceToTempFile(YUNET_RESOURCE, "yunet-", ".onnx");
            File sface = extractResourceToTempFile(SFACE_RESOURCE, "sface-", ".onnx");
            synchronized (modelLock) {
                detector = FaceDetectorYN.create(yunet.getAbsolutePath(), "",
                        new Size(DETECTION_WIDTH, DETECTION_HEIGHT),
                        (float) DETECTION_SCORE_THRESHOLD, (float) NMS_THRESHOLD, TOP_K);
                recognizer = FaceRecognizerSF.create(sface.getAbsolutePath(), "");
                modelsReady.set(true);
            }
        } catch (Exception e) {
            modelsReady.set(false);
            throw new CompletionException(e);
        }
    }

    private void reloadEmbeddings() {
        Map<String, List<float[]>> loaded = new HashMap<>();
        String sql = "SELECT ed_no, embedding, embedding_length FROM FaceEmbedding";
        try (PreparedStatement st = connection().prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                byte[] bytes = rs.getBytes("embedding");
                int length = rs.getInt("embedding_length");
                if (bytes == null || length <= 0 || bytes.length != length * Float.BYTES) continue;
                float[] vector = bytesToFloats(bytes);
                normalize(vector);
                loaded.computeIfAbsent(rs.getString("ed_no"), k -> new ArrayList<>()).add(vector);
            }
        } catch (SQLException e) {
            throw new CompletionException(e);
        }
        knownEmbeddings.clear();
        knownEmbeddings.putAll(loaded);
    }

    @FXML
    void onModeChange(ActionEvent event) {
        ToggleButton toggle = (ToggleButton) event.getSource();
        boolean recognize = "Face Detector".equals(toggle.getText());
        recognitionMode.set(recognize);
        resetRecognitionSession();
        addStudent.setDisable(recognize);
        searchBox.setDisable(recognize);
        scrollAtt.setVisible(false);
        checkBoxGroup.setVisible(!recognize);
        startCapture.setText(recognize ? "Recognition Active" : "Start Training");
        startCapture.setDisable(recognize);
    }
    private void resetTrainingCaptureUi() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::resetTrainingCaptureUi);
            return;
        }

        resetCaptureLabel(nFront);
        resetCaptureLabel(nDown);
        resetCaptureLabel(nLeft);
        resetCaptureLabel(nRight);
        resetCaptureLabel(llFront);
        resetCaptureLabel(ldFront);
        resetCaptureLabel(hFront);
        resetCaptureLabel(gFront);

        /*
         * Re-enable buttons that were disabled after reaching
         * their required capture count.
         */
        for (Button button : completedCaptureButtons) {
            if (button != null) {
                button.setDisable(false);
            }
        }

        completedCaptureButtons.clear();

        selectedStudent = null;
        imagePath = null;

        if (searchBox != null) {
            searchBox.setText("");
        }

        if (scrollAtt != null) {
            scrollAtt.setVisible(false);
        }

        if (glasses != null) {
            glasses.setDisable(true);
        }

        if (hijab != null) {
            hijab.setDisable(true);
        }
    }
    @FXML
    void addNewTraining(ActionEvent event) {
        String text = searchBox.getText();
        Result result = parseSelectedStudent(text);
        if (result == null) {
            showRecognitionMessage("Select a valid student first.");
            return;
        }
        imagePath = Path.of(System.getProperty("user.home"), "gardeEdAttendanceData", result.ed()).toString();
        try {
            Files.createDirectories(Path.of(imagePath));
            selectedStudent = result;
            scrollAtt.setVisible(true);
            showRecognitionMessage("Capture face samples for " + result.ed() + " " + result.name());
        } catch (IOException e) {
            showError("Unable to prepare enrollment", e.getMessage());
        }
    }
    private void resetCaptureLabel(Label label) {
        if (label == null) {
            return;
        }

        String currentText = label.getText();

        if (currentText == null || currentText.isBlank()) {
            return;
        }

        label.setText(
                currentText.replaceFirst(
                        "\\s+\\d+$",
                        ""
                )
        );
    }
    @FXML
    void startCapturing(ActionEvent event) {
        Button source = (Button) event.getSource();
        Label label = switch (source.getId()) {
            case "nf" -> nFront;
            case "nd" -> nDown;
            case "nl" -> nLeft;
            case "nr" -> nRight;
            case "llf" -> llFront;
            case "ldf" -> ldFront;
            case "hf" -> hFront;
            case "gf" -> gFront;
            default -> null;
        };
        if (label == null || selectedStudent == null) {
            showRecognitionMessage("Select a student before capturing.");
            return;
        }
        Mat frameCopy;
        synchronized (frameLock) {
            if (cleanFrame.empty() || lastFaceRow == null) {
                showRecognitionMessage("No usable face detected.");
                return;
            }
            frameCopy = cleanFrame.clone();
        }
        source.setDisable(true);
        worker.submit(() -> {
            try {
                float[] embedding = extractEmbedding(frameCopy, true);
                if (embedding == null) throw new IllegalStateException("Exactly one clear face is required.");
                saveEmbedding(selectedStudent.ed(), source.getId(), embedding);
                knownEmbeddings.computeIfAbsent(selectedStudent.ed(), k -> new CopyOnWriteArrayList<>()).add(embedding);
                saveEnrollmentImage(frameCopy, source.getId());
                runFx(() -> {
                    int count = countLabel(label) + 1;
                    label.setText(baseLabel(label) + " " + count);
                    source.setDisable(count >= 10);
                    if (count >= 10 && !completedCaptureButtons.contains(source)) completedCaptureButtons.add(source);
                    showRecognitionMessage("Sample saved for " + selectedStudent.ed());
                });
            } catch (Exception e) {
                runFx(() -> {
                    source.setDisable(false);
                    showRecognitionMessage("Capture rejected: " + rootMessage(e));
                });
            } finally {
                frameCopy.release();
            }
        });
    }

    /**
     * Compatibility handler: embeddings are created during capture, so no global model training is needed.
     */
    @FXML
    public void goForTraining() {
        Result student = parseSelectedStudent(
                searchBox.getText()
        );
        if (student == null) {
            showRecognitionMessage(
                    "Select a student before training."
            );
            return;
        }
        Path studentFolder = Path.of(
                System.getProperty("user.home"),
                "gardeEdAttendanceData",
                student.ed()
        );
        if (!Files.isDirectory(studentFolder)) {
            showRecognitionMessage(
                    "Training folder not found for "
                            + student.ed()
            );
            return;
        }
        rebuildSelectedStudent(
                student.ed(),
                studentFolder
        );
    }

    private void rebuildSelectedStudent(
            String edNo,
            Path studentFolder
    ) {
        if (!modelsReady.get()) {
            showRecognitionMessage(
                    "YuNet and SFace are still loading."
            );
            return;
        }
        if (!folderTraining.compareAndSet(false, true)) {
            showRecognitionMessage(
                    "Training is already running."
            );
            return;
        }
        javafx.concurrent.Task<TrainingSummary> task =
                new javafx.concurrent.Task<>() {
                    @Override
                    protected TrainingSummary call()
                            throws Exception {
                        return rebuildOneStudent(
                                edNo,
                                studentFolder,
                                this
                        );
                    }
                };
        task.setOnSucceeded(event -> {
            folderTraining.set(false);

            TrainingSummary summary =
                    task.getValue();

            showRecognitionMessage(
                    "Training completed for "
                            + edNo
                            + ": "
                            + summary.embeddings()
                            + " embeddings, "
                            + summary.rejected()
                            + " rejected images."
            );

            resetTrainingCaptureUi();
        });
        task.setOnFailed(event -> {
            folderTraining.set(false);
            Throwable error =
                    task.getException();
            if (error != null) {
                error.printStackTrace();
            }
            showError(
                    "Student training failed",
                    error == null
                            ? "Unknown error"
                            : rootMessage(error)
            );
        });
        task.setOnCancelled(event -> {
            folderTraining.set(false);
            showRecognitionMessage(
                    "Training cancelled for "
                            + edNo
            );
        });
        try {
            worker.submit(task);
        } catch (RejectedExecutionException exception) {
            folderTraining.set(false);
            showError(
                    "Student training failed",
                    "Face worker is shutting down."
            );
        }
    }

    private TrainingSummary rebuildOneStudent(
            String edNo,
            Path studentFolder,
            javafx.concurrent.Task<?> task
    ) throws Exception {
        List<Path> imageFiles;
        try (var paths = Files.walk(studentFolder)) {
            imageFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedTrainingImage)
                    .sorted()
                    .toList();
        }
        if (imageFiles.isEmpty()) {
            throw new IllegalStateException(
                    "No training images found for "
                            + edNo
            );
        }
        List<PendingEmbedding> generated =
                new ArrayList<>();
        int rejected = 0;
        for (int index = 0;
             index < imageFiles.size();
             index++) {
            if (task.isCancelled()
                    || Thread.currentThread().isInterrupted()) {
                throw new CancellationException(
                        "Training cancelled."
                );
            }
            Path imagePath =
                    imageFiles.get(index);
            Mat loadedImage =
                    Imgcodecs.imread(
                            imagePath.toString(),
                            Imgcodecs.IMREAD_UNCHANGED
                    );
            if (loadedImage.empty()) {
                loadedImage.release();
                rejected++;
                continue;
            }
            Mat bgrImage = null;
            try {
                bgrImage =
                        ensureBgr(loadedImage);
                float[] embedding =
                        extractLegacyEmbedding(
                                bgrImage
                        );
                if (embedding == null) {
                    rejected++;
                    continue;
                }
                Path relative =
                        studentFolder.relativize(
                                imagePath
                        );
                String captureMode =
                        relative.getNameCount() > 1
                                ? relative
                                .getName(0)
                                .toString()
                                : "training";
                generated.add(
                        new PendingEmbedding(
                                edNo,
                                captureMode,
                                embedding
                        )
                );
            } catch (Exception exception) {
                rejected++;
                System.err.println(
                        "Rejected image: "
                                + imagePath
                                + " because "
                                + exception.getMessage()
                );
            } finally {
                if (bgrImage != null) {
                    bgrImage.release();
                }
                loadedImage.release();
            }
        }
        if (generated.isEmpty()) {
            throw new IllegalStateException(
                    "No valid embeddings were generated for "
                            + edNo
            );
        }
        replaceStudentEmbeddings(
                edNo,
                generated
        );
        reloadEmbeddings();
        return new TrainingSummary(
                1,
                generated.size(),
                rejected
        );
    }

    private void replaceStudentEmbeddings(
            String edNo,
            List<PendingEmbedding> embeddings
    ) throws SQLException {
        Connection databaseConnection =
                connection();
        boolean previousAutoCommit =
                databaseConnection.getAutoCommit();
        String deleteSql = """
                DELETE FROM FaceEmbedding
                WHERE ed_no = ?
                """;
        String insertSql = """
                INSERT INTO FaceEmbedding (
                ed_no,
                capture_mode,
                embedding,
                embedding_length
                )
                VALUES (?, ?, ?, ?)
                """;
        try {
            databaseConnection.setAutoCommit(false);
            try (PreparedStatement deleteStatement =
                         databaseConnection.prepareStatement(
                                 deleteSql
                         )) {
                deleteStatement.setString(
                        1,
                        edNo
                );
                deleteStatement.executeUpdate();
            }
            try (PreparedStatement insertStatement =
                         databaseConnection.prepareStatement(
                                 insertSql
                         )) {
                for (PendingEmbedding embedding
                        : embeddings) {
                    insertStatement.setString(
                            1,
                            embedding.edNo()
                    );
                    insertStatement.setString(
                            2,
                            embedding.captureMode()
                    );
                    insertStatement.setBytes(
                            3,
                            floatsToBytes(
                                    embedding.embedding()
                            )
                    );
                    insertStatement.setInt(
                            4,
                            embedding.embedding().length
                    );
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
            }
            databaseConnection.commit();
        } catch (Exception exception) {
            databaseConnection.rollback();
            if (exception instanceof SQLException sql) {
                throw sql;
            }
            throw new SQLException(
                    "Unable to replace embeddings for "
                            + edNo,
                    exception
            );
        } finally {
            databaseConnection.setAutoCommit(
                    previousAutoCommit
            );
        }
    }

    private final class FolderTrainingTask
            extends javafx.concurrent.Task<TrainingSummary> {
        private final Path trainingRoot;

        private FolderTrainingTask(Path trainingRoot) {
            this.trainingRoot = trainingRoot;
        }

        @Override
        protected TrainingSummary call() throws Exception {
            return rebuildEmbeddingsFromFolders(
                    trainingRoot,
                    this
            );
        }

        private void reportMessage(String message) {
            updateMessage(message);
        }

        private void reportProgress(
                long completed,
                long total
        ) {
            updateProgress(completed, total);
        }
    }

    private TrainingSummary rebuildEmbeddingsFromFolders(
            Path trainingRoot,
            FolderTrainingTask task
    ) throws Exception {
        task.reportMessage(
                "Scanning existing training images..."
        );
        List<LegacyImage> trainingImages =
                discoverLegacyImages(trainingRoot);
        if (trainingImages.isEmpty()) {
            throw new IllegalStateException(
                    "No JPG, JPEG, PNG or BMP images were found in "
                            + trainingRoot
            );
        }
        List<PendingEmbedding> generatedEmbeddings =
                new ArrayList<>();
        Set<String> trainedStudents =
                new HashSet<>();
        int rejectedImages = 0;
        for (int index = 0;
             index < trainingImages.size();
             index++) {
            checkFolderTrainingCancellation(task);
            LegacyImage item =
                    trainingImages.get(index);
            task.reportMessage(
                    "Processing "
                            + item.edNo()
                            + " : "
                            + item.path().getFileName()
            );
            task.reportProgress(
                    index,
                    trainingImages.size()
            );
            Mat loadedImage = Imgcodecs.imread(
                    item.path().toString(),
                    Imgcodecs.IMREAD_UNCHANGED
            );
            if (loadedImage.empty()) {
                loadedImage.release();
                rejectedImages++;
                continue;
            }
            Mat bgrImage = null;
            try {
                bgrImage = ensureBgr(loadedImage);
                float[] embedding =
                        extractLegacyEmbedding(bgrImage);
                if (embedding == null) {
                    rejectedImages++;
                    System.err.println(
                            "No usable face found in: "
                                    + item.path()
                    );
                    continue;
                }
                generatedEmbeddings.add(
                        new PendingEmbedding(
                                item.edNo(),
                                item.captureMode(),
                                embedding
                        )
                );
                trainedStudents.add(
                        item.edNo()
                );
            } catch (Exception exception) {
                rejectedImages++;
                System.err.println(
                        "Rejected training image: "
                                + item.path()
                );
                exception.printStackTrace();
            } finally {
                if (bgrImage != null) {
                    bgrImage.release();
                }
                loadedImage.release();
            }
        }
        checkFolderTrainingCancellation(task);
        if (generatedEmbeddings.isEmpty()) {
            throw new IllegalStateException(
                    "None of the training photographs "
                            + "produced valid SFace embeddings. "
                            + "Existing embeddings were not changed."
            );
        }
        task.reportMessage(
                "Saving "
                        + generatedEmbeddings.size()
                        + " face embeddings..."
        );
        task.reportProgress(
                trainingImages.size(),
                trainingImages.size()
        );
        replaceAllEmbeddings(
                generatedEmbeddings,
                task
        );
        reloadEmbeddings();
        return new TrainingSummary(
                trainedStudents.size(),
                generatedEmbeddings.size(),
                rejectedImages
        );
    }

    private List<LegacyImage> discoverLegacyImages(
            Path trainingRoot
    ) throws IOException {
        List<LegacyImage> results =
                new ArrayList<>();
        try (var studentFolders =
                     Files.list(trainingRoot)) {
            List<Path> folders = studentFolders
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
            for (Path studentFolder : folders) {
                String edNo =
                        studentFolder
                                .getFileName()
                                .toString()
                                .trim();
                if (!mainController
                        .gradedDataLoader
                        .getStudentData()
                        .containsKey(edNo)) {
                    System.err.println(
                            "Ignoring unknown student folder: "
                                    + studentFolder
                    );
                    continue;
                }
                try (var imagePaths =
                             Files.walk(studentFolder)) {
                    List<Path> images = imagePaths
                            .filter(Files::isRegularFile)
                            .filter(this::isSupportedTrainingImage)
                            .sorted()
                            .toList();
                    for (Path image : images) {
                        Path relativePath =
                                studentFolder.relativize(image);
                        String captureMode =
                                relativePath.getNameCount() > 1
                                        ? relativePath
                                        .getName(0)
                                        .toString()
                                        : "legacy";
                        results.add(
                                new LegacyImage(
                                        edNo,
                                        captureMode,
                                        image
                                )
                        );
                    }
                }
            }
        }
        return results;
    }

    private boolean isSupportedTrainingImage(
            Path imagePath
    ) {
        String filename = imagePath
                .getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);
        return filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".bmp");
    }

    private Mat ensureBgr(Mat source) {
        if (source == null || source.empty()) {
            throw new IllegalArgumentException(
                    "Training image is empty."
            );
        }
        Mat result = new Mat();
        if (source.channels() == 3) {
            source.copyTo(result);
        } else if (source.channels() == 1) {
            Imgproc.cvtColor(
                    source,
                    result,
                    Imgproc.COLOR_GRAY2BGR
            );
        } else if (source.channels() == 4) {
            Imgproc.cvtColor(
                    source,
                    result,
                    Imgproc.COLOR_BGRA2BGR
            );
        } else {
            result.release();
            throw new IllegalArgumentException(
                    "Unsupported image channel count: "
                            + source.channels()
            );
        }
        return result;
    }

    private float[] extractLegacyEmbedding(
            Mat bgrImage
    ) {
        float[] directEmbedding =
                extractEmbedding(
                        bgrImage,
                        true
                );
        if (directEmbedding != null) {
            return directEmbedding;
        }
        int horizontalPadding =
                Math.max(
                        24,
                        bgrImage.cols() / 4
                );
        int verticalPadding =
                Math.max(
                        24,
                        bgrImage.rows() / 4
                );
        Mat paddedImage = new Mat();
        try {
            Core.copyMakeBorder(
                    bgrImage,
                    paddedImage,
                    verticalPadding,
                    verticalPadding,
                    horizontalPadding,
                    horizontalPadding,
                    Core.BORDER_REPLICATE
            );
            return extractEmbedding(
                    paddedImage,
                    true
            );
        } finally {
            paddedImage.release();
        }
    }

    private void replaceAllEmbeddings(
            List<PendingEmbedding> embeddings,
            FolderTrainingTask task
    ) throws SQLException {
        Connection databaseConnection =
                connection();
        boolean previousAutoCommit =
                databaseConnection.getAutoCommit();
        String insertSql = """
                INSERT INTO FaceEmbedding (
                ed_no,
                capture_mode,
                embedding,
                embedding_length
                )
                VALUES (?, ?, ?, ?)
                """;
        try {
            databaseConnection.setAutoCommit(false);
            try (Statement deleteStatement =
                         databaseConnection.createStatement()) {
                deleteStatement.executeUpdate(
                        "DELETE FROM FaceEmbedding"
                );
            }
            try (PreparedStatement insertStatement =
                         databaseConnection.prepareStatement(
                                 insertSql
                         )) {
                int batchCount = 0;
                for (PendingEmbedding item : embeddings) {
                    checkFolderTrainingCancellation(task);
                    insertStatement.setString(
                            1,
                            item.edNo()
                    );
                    insertStatement.setString(
                            2,
                            item.captureMode()
                    );
                    insertStatement.setBytes(
                            3,
                            floatsToBytes(
                                    item.embedding()
                            )
                    );
                    insertStatement.setInt(
                            4,
                            item.embedding().length
                    );
                    insertStatement.addBatch();
                    batchCount++;
                    if (batchCount % 250 == 0) {
                        insertStatement.executeBatch();
                    }
                }
                insertStatement.executeBatch();
            }
            databaseConnection.commit();
        } catch (Exception exception) {
            databaseConnection.rollback();
            if (exception
                    instanceof CancellationException cancellation) {
                throw cancellation;
            }
            if (exception
                    instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException(
                    "Unable to replace all face embeddings.",
                    exception
            );
        } finally {
            databaseConnection.setAutoCommit(
                    previousAutoCommit
            );
        }
    }

    private void checkFolderTrainingCancellation(
            FolderTrainingTask task
    ) {
        if (task.isCancelled()
                || Thread.currentThread().isInterrupted()) {
            throw new CancellationException(
                    "Folder training was cancelled."
            );
        }
    }

    private record LegacyImage(
            String edNo,
            String captureMode,
            Path path
    ) {
    }

    private record PendingEmbedding(
            String edNo,
            String captureMode,
            float[] embedding
    ) {
    }

    private record TrainingSummary(
            int students,
            int embeddings,
            int rejected
    ) {
    }

    private void saveEnrollmentImage(Mat frame, String mode) throws IOException {
        File dir = new File(imagePath, mode);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        if (!Imgcodecs.imwrite(new File(dir, System.nanoTime() + ".jpg").getAbsolutePath(), frame))
            throw new IOException("OpenCV could not save enrollment image.");
    }

    private void saveEmbedding(String edNo, String mode, float[] vector) throws SQLException {
        String sql = "INSERT INTO FaceEmbedding(ed_no,capture_mode,embedding,embedding_length) VALUES(?,?,?,?)";
        try (PreparedStatement st = connection().prepareStatement(sql)) {
            st.setString(1, edNo);
            st.setString(2, mode);
            st.setBytes(3, floatsToBytes(vector));
            st.setInt(4, vector.length);
            st.executeUpdate();
        }
    }

    private float[] extractEmbedding(Mat bgrFrame, boolean demandSingleFace) {
        Detection detection = detectBestFace(bgrFrame, demandSingleFace);
        if (detection == null) return null;
        Mat aligned = new Mat();
        Mat feature = new Mat();
        try {
            synchronized (modelLock) {
                recognizer.alignCrop(bgrFrame, detection.originalScaleFaceRow(), aligned);
                recognizer.feature(aligned, feature);
            }
            int length = (int) feature.total() * feature.channels();
            if (length <= 0) return null;
            float[] values = new float[length];
            feature.get(0, 0, values);
            normalize(values);
            return values;
        } finally {
            detection.close();
            aligned.release();
            feature.release();
        }
    }

    private Detection detectBestFace(Mat original, boolean demandSingleFace) {
        if (!modelsReady.get() || original.empty()) return null;
        Mat small = new Mat();
        Mat faces = new Mat();
        try {
            Imgproc.resize(original, small, new Size(DETECTION_WIDTH, DETECTION_HEIGHT));
            synchronized (modelLock) {
                detector.setInputSize(small.size());
                detector.detect(small, faces);
            }
            if (faces.empty() || faces.rows() == 0) return null;
            if (demandSingleFace && faces.rows() != 1) return null;
            int best = 0;
            double bestArea = -1;
            float[] row = new float[15];
            for (int i = 0; i < faces.rows(); i++) {
                faces.get(i, 0, row);
                double area = row[2] * row[3];
                if (area > bestArea) {
                    bestArea = area;
                    best = i;
                }
            }
            faces.get(best, 0, row);
            double sx = original.cols() / (double) DETECTION_WIDTH;
            double sy = original.rows() / (double) DETECTION_HEIGHT;
            float[] scaled = row.clone();
            for (int i = 0; i <= 12; i += 2) {
                scaled[i] *= (float) sx;
                scaled[i + 1] *= (float) sy;
            }
            Mat faceRow = new Mat(1, 15, CvType.CV_32F);
            faceRow.put(0, 0, scaled);
            Rect rect = new Rect(
                    Math.max(0, Math.round(row[0] * (float) sx)),
                    Math.max(0, Math.round(row[1] * (float) sy)),
                    Math.max(1, Math.round(row[2] * (float) sx)),
                    Math.max(1, Math.round(row[3] * (float) sy)));
            rect.width = Math.min(rect.width, original.cols() - rect.x);
            rect.height = Math.min(rect.height, original.rows() - rect.y);
            return new Detection(faceRow, rect, row[14]);
        } finally {
            small.release();
            faces.release();
        }
    }

    private FaceMatch findBestMatch(float[] query) {
        String bestEd = null, secondEd = null;
        double best = -1.0, second = -1.0;
        for (var entry : knownEmbeddings.entrySet()) {
            double identityBest = -1.0;
            for (float[] candidate : entry.getValue()) {
                if (candidate.length == query.length) identityBest = Math.max(identityBest, dot(query, candidate));
            }
            if (identityBest > best) {
                second = best;
                secondEd = bestEd;
                best = identityBest;
                bestEd = entry.getKey();
            } else if (identityBest > second) {
                second = identityBest;
                secondEd = entry.getKey();
            }
        }
        if (bestEd == null) return null;
        return new FaceMatch(bestEd, best, secondEd, second);
    }

    private void requestRecognition() {
        if (folderTraining.get()) {
            return;
        }
        if (!recognitionMode.get() || !modelsReady.get() || knownEmbeddings.isEmpty()) return;
        long now = System.nanoTime();
        if (now - lastRecognitionTime < RECOGNITION_INTERVAL_NS) return;
        if (!processing.compareAndSet(false, true)) return;
        lastRecognitionTime = now;
        Mat copy;
        synchronized (frameLock) {
            if (cleanFrame.empty()) {
                processing.set(false);
                return;
            }
            copy = cleanFrame.clone();
        }
        try {
            worker.submit(() -> {
                try {
                    float[] query = extractEmbedding(copy, false);
                    FaceMatch match = query == null ? null : findBestMatch(query);
                    runFx(() -> processRecognitionResult(match));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    copy.release();
                    processing.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            copy.release();
            processing.set(false);
        }
    }

    private void processRecognitionResult(FaceMatch match) {
        if (match == null || match.bestScore() < similarityThreshold ||
                match.bestScore() - match.secondScore() < scoreMargin) {
            resetRecognition();
            return;
        }
        if (Objects.equals(currentCandidate, match.edNo())) {
            consecutiveMatches++;
            scoreSum += match.bestScore();
        } else {
            currentCandidate = match.edNo();
            consecutiveMatches = 1;
            scoreSum = match.bestScore();
        }
        if (consecutiveMatches < requiredRecognitionCount) return;
        String accepted = currentCandidate;
        double average = scoreSum / consecutiveMatches;
        resetRecognition();
        if (average >= similarityThreshold && tryAcceptRecognition(accepted)) processAcceptedStudent(accepted, average);
    }

    private synchronized boolean tryAcceptRecognition(String edNo) {
        long now = System.currentTimeMillis();
        if (acceptedFaceMustLeave && Objects.equals(edNo, lastAcceptedEdNo)) return false;
        if (Objects.equals(edNo, lastAcceptedEdNo) && now - lastAcceptedAt < RECOGNITION_COOLDOWN_MS) return false;
        lastAcceptedEdNo = edNo;
        lastAcceptedAt = now;
        acceptedFaceMustLeave = true;
        consecutiveNoFaceFrames = 0;
        return true;
    }

    private void processAcceptedStudent(String studentKey, double score) {
        var studentMap = homeController.studentAttendance.mainController.gradedDataLoader.getStudentData();
        var student = studentMap.get(studentKey);
        if (student == null) {
            showRecognitionMessage("Recognized enrollment was not found: " + studentKey);
            return;
        }
        var attendanceMap = homeController.studentAttendance.attendanceMap;
        if (!attendanceMap.containsKey(studentKey)) {
            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
            homeController.studentAttendance.updateCheckIn(studentKey, now);
            showRecognitionMessage(studentKey + " " + student.name() + " you are marked as present");
            return;
        }
        var attendance = attendanceMap.get(studentKey);
        if (attendance == null) {
            showRecognitionMessage("Attendance information is unavailable for " + studentKey);
            return;
        }
        if (attendance.getCheck_out() != null) {
            showRecognitionMessage(studentKey + " attendance is already completed.");
            return;
        }
        processCheckout(studentKey, attendance.getCheck_in());
    }

    private void processCheckout(String edNo, String checkInText) {
        if (checkInText == null || checkInText.isBlank()) {
            showRecognitionMessage("Check-in time is unavailable for " + edNo);
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        try {
            LocalTime checkIn = LocalTime.parse(checkInText.trim().toUpperCase(Locale.ENGLISH), formatter);
            Duration duration = Duration.between(checkIn, LocalTime.now());
            if (duration.isNegative()) duration = duration.plusDays(1);
            long minutes = duration.toMinutes();
            if (minutes < 45) {
                showRecognitionMessage("Cannot checkout before 45 minutes. Spent " + minutes + " minutes.");
                return;
            }
            homeController.studentAttendance.updateCheckOut(edNo);
            showRecognitionMessage(edNo + " checkout done");
        } catch (Exception e) {
            showRecognitionMessage("Invalid check-in time for " + edNo);
        }
    }

    private void startCamera(int index) {
        stopCameraOnly();
        VideoCapture opened = new VideoCapture(index, Videoio.CAP_DSHOW);
        if (!opened.isOpened()) {
            opened.release();
            throw new IllegalStateException("Cannot open camera " + index);
        }
        opened.set(Videoio.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
        opened.set(Videoio.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);
        opened.set(Videoio.CAP_PROP_FPS, CAMERA_FPS);
        synchronized (cameraLock) {
            camera = opened;
            currentCameraIndex = index;
        }
        cameraTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "attendance-camera-capture");
            t.setDaemon(true);
            return t;
        });
        cameraTimer.scheduleWithFixedDelay(this::grabFrameSafely, 0, FRAME_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void grabFrameSafely() {
        if (disposed.get()) return;
        try {
            grabFrame();
        } catch (Throwable t) {
            t.printStackTrace();
            scheduleRestart();
        }
    }

    private void grabFrame() {
        Mat frame = new Mat();
        try {
            boolean read;
            synchronized (cameraLock) {
                read = camera != null && camera.isOpened() && camera.read(frame);
            }
            if (!read || frame.empty()) {
                if (++consecutiveReadFailures >= MAX_READ_FAILURES) scheduleRestart();
                return;
            }
            consecutiveReadFailures = 0;
            Core.flip(frame, frame, 1);
            Detection detection = modelsReady.get() ? detectBestFace(frame, false) : null;
            boolean facePresent = detection != null;
            synchronized (frameLock) {
                frame.copyTo(cleanFrame);
                if (lastFaceRow != null) lastFaceRow.release();
                lastFaceRow = detection == null ? null : detection.originalScaleFaceRow().clone();
            }
            if (detection != null) {
                Imgproc.rectangle(frame, detection.rect(), new Scalar(0, 255, 0), 2);
                detection.close();
            }
            updateFacePresence(facePresent);
            if (facePresent) requestRecognition();
            publishFrame(frame, facePresent);
        } finally {
            frame.release();
        }
    }

    private synchronized void updateFacePresence(boolean present) {
        if (present) {
            consecutiveNoFaceFrames = 0;
            return;
        }
        if (++consecutiveNoFaceFrames >= REQUIRED_NO_FACE_FRAMES) {
            acceptedFaceMustLeave = false;
            consecutiveNoFaceFrames = 0;
            resetRecognition();
        }
    }

    private void publishFrame(Mat bgr, boolean present) {
        long now = System.nanoTime();
        if (now - lastUiFrameTime < UI_INTERVAL_NS || !uiFramePending.compareAndSet(false, true)) return;
        lastUiFrameTime = now;
        Image image;
        try {
            image = matToImage(bgr);
        } catch (Exception e) {
            uiFramePending.set(false);
            return;
        }
        runFx(() -> {
            try {
                if (cameraView != null) {
                    cameraView.setPhoto(image);
                    applyFaceBorder(cameraView, present, true);
                }
                if (studentCameraView != null && studentStage != null && studentStage.isShowing()) {
                    studentCameraView.setPhoto(image);
                    applyFaceBorder(studentCameraView, present, false);
                }
            } finally {
                uiFramePending.set(false);
            }
        });
    }

    private Image matToImage(Mat bgr) {
        Mat rgb = new Mat();
        Mat resized = new Mat();
        try {
            Imgproc.resize(bgr, resized, new Size(DETECTION_WIDTH, DETECTION_HEIGHT));
            Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);
            byte[] data = new byte[(int) (rgb.total() * rgb.channels())];
            rgb.get(0, 0, data);
            WritableImage image = new WritableImage(rgb.cols(), rgb.rows());
            image.getPixelWriter().setPixels(0, 0, rgb.cols(), rgb.rows(),
                    PixelFormat.getByteRgbInstance(), data, 0, rgb.cols() * 3);
            return image;
        } finally {
            rgb.release();
            resized.release();
        }
    }

    private List<CameraDevice> scanCameras() {
        List<CameraDevice> found = new ArrayList<>();
        for (int i = 0; i < MAX_CAMERA_INDEX; i++) {
            VideoCapture test = new VideoCapture(i, Videoio.CAP_DSHOW);
            try {
                if (test.isOpened()) found.add(new CameraDevice(i, "Camera " + i));
            } finally {
                test.release();
            }
        }
        return found;
    }

    private void configureCameraList(List<CameraDevice> devices) {
        cameraList.setItems(FXCollections.observableArrayList(devices));
        if (devices.isEmpty()) {
            showRecognitionMessage("No camera found.");
            return;
        }
        cameraList.valueProperty().addListener((obs, old, value) -> {
            if (value != null) switchCamera(value.index());
        });
        cameraList.setDisable(false);
        startCapture.setDisable(false);
        cameraList.getSelectionModel().select(devices.size() > 1 ? 1 : 0);
    }

    private void switchCamera(int index) {
        if (!cameraSwitching.compareAndSet(false, true) || disposed.get()) return;
        cameraList.setDisable(true);
        worker.submit(() -> {
            try {
                startCamera(index);
            } catch (Exception e) {
                runFx(() -> showError("Unable to start camera", rootMessage(e)));
            } finally {
                cameraSwitching.set(false);
                runFx(() -> cameraList.setDisable(false));
            }
        });
    }

    private void scheduleRestart() {
        if (!restartingCamera.compareAndSet(false, true) || disposed.get()) return;
        worker.submit(() -> {
            try {
                Thread.sleep(1000);
                if (!disposed.get()) startCamera(currentCameraIndex);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                restartingCamera.set(false);
            }
        });
    }

    public void stopCamera() {
        stopCameraOnly();
        runFx(() -> {
            if (cameraView != null) cameraView.setPhoto(null);
            if (studentCameraView != null) studentCameraView.setPhoto(null);
        });
    }

    private void stopCameraOnly() {
        ScheduledExecutorService timer = cameraTimer;
        cameraTimer = null;
        if (timer != null) timer.shutdownNow();
        VideoCapture old;
        synchronized (cameraLock) {
            old = camera;
            camera = null;
        }
        if (old != null) old.release();
        synchronized (frameLock) {
            cleanFrame.release();
            if (lastFaceRow != null) {
                lastFaceRow.release();
                lastFaceRow = null;
            }
        }
        processing.set(false);
        resetRecognitionSession();
    }

    public void dispose() {
        if (!disposed.compareAndSet(false, true)) return;
        stopCameraOnly();
        worker.shutdownNow();
        synchronized (modelLock) {
            if (detector != null) {
//detector.clear();
                detector = null;
            }
            if (recognizer != null) {
//recognizer.clear();
                recognizer = null;
            }
        }
        cleanFrame.release();
        if (studentStage != null) studentStage.close();
    }

    @FXML
    private void openStudentDisplay() {
        if (studentStage != null && studentStage.isShowing()) {
            studentStage.toFront();
            return;
        }
        studentCameraView = new PhotoView();
        studentCameraView.setEditable(false);
        studentCameraView.setMinSize(512, 512);
        studentMessage = new Label("Please stand in front of the camera");
        studentMessage.setStyle("-fx-font-size:22px;-fx-font-weight:bold;");
        VBox content = new VBox(20, studentCameraView, studentMessage);
        content.setAlignment(Pos.CENTER);
        SVGImageView logo = new SVGImageView();
        logo.setFitHeight(48);
        logo.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        var logoView=new HBox(logo);
        logoView.setAlignment(Pos.CENTER);
        VBox root = new VBox(content, logoView);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(12));
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(GradedResourceLoader.load("css/camera-style.css"));
        studentStage = new Stage();
        studentStage.setTitle("Student Display");
        studentStage.setScene(scene);
        studentStage.setOnHidden(e -> {
            studentCameraView = null;
            studentMessage = null;
            studentStage = null;
        });
        studentStage.show();
    }

    @FXML
    void onClicked(ActionEvent event) {
        CheckBox box = (CheckBox) event.getSource();
        if ("Glasses".equals(box.getText())) glasses.setDisable(!box.isSelected());
        if ("Hijab".equals(box.getText())) hijab.setDisable(!box.isSelected());
    }

    @FXML
    void onSetting() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Face Recognition Settings");
        Slider threshold = new Slider(0.30, 0.80, similarityThreshold);
        Slider margin = new Slider(0.01, 0.20, scoreMargin);
        Spinner<Integer> checks = new Spinner<>(1, 10, requiredRecognitionCount);
        Label t = new Label();
        Label m = new Label();
        Runnable labels = () -> {
            t.setText(String.format(Locale.ROOT, "Similarity threshold: %.2f", threshold.getValue()));
            m.setText(String.format(Locale.ROOT, "Second-best margin: %.2f", margin.getValue()));
        };
        labels.run();
        threshold.valueProperty().addListener((o, a, b) -> labels.run());
        margin.valueProperty().addListener((o, a, b) -> labels.run());
        dialog.getDialogPane().setContent(new VBox(12, t, threshold, m, margin, new Label("Required consecutive matches"), checks));
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(x -> {
            similarityThreshold = threshold.getValue();
            scoreMargin = margin.getValue();
            requiredRecognitionCount = checks.getValue();
            saveCameraSettings();
        });
    }

    private void loadCameraSettings() {
        try (Statement st = connection().createStatement(); ResultSet rs = st.executeQuery("SELECT confidence_threshold, required_checks FROM camera_data WHERE id=1")) {
            if (rs.next()) {
                double old = rs.getDouble(1);
                if (old >= 0.0 && old <= 1.0) similarityThreshold = old;
                requiredRecognitionCount = Math.max(1, rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }

    private void saveCameraSettings() {
        try (PreparedStatement st = connection().prepareStatement("UPDATE camera_data SET confidence_threshold=?, required_checks=? WHERE id=1")) {
            st.setDouble(1, similarityThreshold);
            st.setInt(2, requiredRecognitionCount);
            st.executeUpdate();
        } catch (SQLException e) {
            showError("Could not save settings", e.getMessage());
        }
    }

    private void initializeStudentSearch() {
        studentData.setAll(mainController.gradedDataLoader.getStudentData().values().stream()
                .map(s -> s.ed_no() + " " + s.name()).toList());
        searchBox.setSuggestionProvider(request -> {
            String q = request.getUserText();
            if (q == null || q.isBlank()) return List.of();
            String normalized = q.toLowerCase(Locale.ROOT).trim();
            return studentData.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(normalized)).collect(Collectors.toList());
        });
        FontIcon search = new FontIcon("mdmz-search");
        FontIcon clear = new FontIcon("mdal-close");
        searchBox.setGraphic(search);
        searchBox.textProperty().addListener((o, a, b) -> searchBox.setGraphic(b == null || b.isBlank() ? search : clear));
        clear.setOnMouseClicked(e -> {
            searchBox.setText("");
            selectedStudent = null;
            scrollAtt.setVisible(false);
        });
    }

    private void initializeGraphics() {
        background.setSvgUrl(GradedResourceLoader.load("icons/new-back1.svg"));
        background.setOpacity(0.3);
        myLogo.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
    }

    @FXML
    void play(ActionEvent event) {
    }

    private void applyFaceBorder(PhotoView view, boolean detected, boolean main) {
        Boolean previous = main ? mainViewFaceState : studentViewFaceState;
        if (previous != null && previous == detected) return;
        if (main) mainViewFaceState = detected;
        else studentViewFaceState = detected;
        view.getStyleClass().removeAll("border-circle-green", "border-circle-red");
        view.getStyleClass().add(detected ? "border-circle-green" : "border-circle-red");
    }

    private Result parseSelectedStudent(String value) {
        if (value == null) return null;
        int space = value.indexOf(' ');
        if (space <= 0 || space == value.length() - 1) return null;
        String ed = value.substring(0, space).trim();
        String name = value.substring(space + 1).trim();
        return mainController.gradedDataLoader.getStudentData().containsKey(ed) ? new Result(ed, name) : null;
    }

    private void resetRecognition() {
        currentCandidate = null;
        consecutiveMatches = 0;
        scoreSum = 0.0;
    }

    private synchronized void resetRecognitionSession() {
        resetRecognition();
        lastAcceptedEdNo = null;
        lastAcceptedAt = 0L;
        acceptedFaceMustLeave = false;
        consecutiveNoFaceFrames = 0;
        lastRecognitionTime = 0L;
    }

    private void showRecognitionMessage(String message) {
        if (outputMessage != null) outputMessage.setText(message);
        if (studentMessage != null) studentMessage.setText(message);
    }

    private void showError(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Camera Error");
        a.setHeaderText(header);
        a.setContentText(message);
        a.show();
    }

    private void runFx(Runnable action) {
        if (!disposed.get()) Platform.runLater(() -> {
            if (!disposed.get()) action.run();
        });
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    private static int countLabel(Label label) {
        String s = label.getText();
        int p = s.lastIndexOf(' ');
        if (p < 0) return 0;
        try {
            return Integer.parseInt(s.substring(p + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String baseLabel(Label label) {
        String s = label.getText();
        int p = s.lastIndexOf(' ');
        if (p > 0) try {
            Integer.parseInt(s.substring(p + 1));
            return s.substring(0, p);
        } catch (NumberFormatException ignored) {
        }
        return s;
    }

    private static double dot(float[] a, float[] b) {
        double value = 0;
        for (int i = 0; i < a.length; i++) value += a[i] * b[i];
        return value;
    }

    private static void normalize(float[] v) {
        double n = 0;
        for (float x : v) n += x * x;
        n = Math.sqrt(n);
        if (n > 0) for (int i = 0; i < v.length; i++) v[i] /= (float) n;
    }

    private static byte[] floatsToBytes(float[] values) {
        ByteBuffer b = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) b.putFloat(v);
        return b.array();
    }

    private static float[] bytesToFloats(byte[] bytes) {
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[bytes.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = b.getFloat();
        return v;
    }

    private record Result(String ed, String name) {
    }

    private record CameraDevice(int index, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record FaceMatch(String edNo, double bestScore, String secondEdNo, double secondScore) {
    }

    private record Detection(Mat originalScaleFaceRow, Rect rect, float score) implements AutoCloseable {
        @Override
        public void close() {
            originalScaleFaceRow.release();
        }
    }
}