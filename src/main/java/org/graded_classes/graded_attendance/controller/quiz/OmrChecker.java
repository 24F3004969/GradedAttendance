package org.graded_classes.graded_attendance.controller.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.ImageCropperAndPreviewApp;
import org.graded_classes.graded_attendance.components.Omr;
import org.graded_classes.graded_attendance.data.ExamData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.graded_classes.graded_attendance.components.Omr.confidenceStatus;
import static org.graded_classes.graded_attendance.components.Omr.read;

public class OmrChecker implements Initializable {

    public StackPane loadingPane;
    @FXML
    private Button close;

    @FXML
    private SplitPane omrView;

    @FXML
    private ScrollPane rawImageView;

    @FXML
    private ToggleGroup typeType;
    @FXML
    private TilePane tilePane;
    ImageCropperAndPreviewApp imageCropperAndPreviewApp = new ImageCropperAndPreviewApp();
    File path;

    @FXML
    void addOmrImage(ActionEvent event) {
        Button btt = (Button) event.getSource();
        path = conductExam.folderChooser();
        Set<String> allowed = Set.of(".png", ".jpg", ".jpeg");

        File[] images = path.listFiles(file -> {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return allowed.stream().anyMatch(name::endsWith);
        });
        tilePane.getChildren().remove(btt);
        tilePane.setAlignment(Pos.TOP_LEFT);

        assert images != null;
        for (var name : images) {

            tilePane.getChildren().add(conductExam.mainController.
                    gradedFxmlLoader.createView(R.omr_preview));
        }


    }

    public void runOmr(String path) {
        Set<String> allowed = Set.of(".png", ".jpg", ".jpeg");

        File[] files = new File(path).listFiles(file -> {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return allowed.stream().anyMatch(name::endsWith);
        });
        List<Omr.OmrResult> omrResults = new ArrayList<>();
        Omr omr = new Omr();
        if (files != null) {
            for (File file : files) {
                String inputPath = file.getAbsolutePath();

                try (Omr.OmrResult result = read(inputPath, true)) {

                    System.out.printf(
                            "Roll/Enrollment No: %s (%s)%n",
                            result.rollNumber().value(),
                            result.rollNumber().valid() ? "VALID" : "INVALID"
                    );

                    for (Omr.QuestionResult question : result.questions()) {
                        double[] scores = question.scores();
                        System.out.printf(
                                Locale.US,
                                "Q%02d -> %s [A=%.3f B=%.3f C=%.3f D=%.3f, "
                                        + "threshold=%.3f, confidence=%.3f, status=%s]%n",
                                question.questionNumber(),
                                question.answer(),
                                scores[0], scores[1], scores[2], scores[3],
                                question.threshold(),
                                question.confidence(),
                                confidenceStatus(question.answer(), question.confidence(), question.faintRecovery())
                        );
                    }
                    omrResults.add(result);
                }
            }

        }
        omr.writeJsonFile(omrResults, new ArrayList<>(), new File("result.json").toPath());
    }

    ConductExam conductExam;
    ExamData exam;
    private ParallelTransition rawImageAnimation;
    int debugImageIndex = 0;
    Task<JsonNode> omrTask;

    @FXML
    void onClose(ActionEvent event) {
        conductExam.mainController.modalPane.hide();
    }
    public OmrChecker(ConductExam conductExam, ExamData exam) {
        this.conductExam = conductExam;
        this.exam = exam;
    }

    @FXML
    void checkAllQuestion(ActionEvent event) {

        // Must be a StackPane for the ring to appear above the content
        ProgressIndicator progressRing = new ProgressIndicator();
        progressRing.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressRing.setPrefSize(55, 55);
        progressRing.setMaxSize(55, 55);

        // Prevent multiple clicks while processing
        Node source = event == null ? null : (Node) event.getSource();
        if (source != null) {
            source.setDisable(true);
        }

        loadingPane.getChildren().setAll(progressRing);
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        loadingPane.toFront();

        omrTask = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {

                // Heavy OMR process runs in background
                runOmr(path.getAbsolutePath());

                ObjectMapper mapper = new ObjectMapper();

                // Wait until runOmr has produced result.json

                return mapper.readTree(new File("result.json"));
            }
        };
        omrTask.setOnSucceeded(workerStateEvent -> {
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            loadingPane.getChildren().clear();

            if (source != null) {
                source.setDisable(false);
            }

            changeType(null);
            loadingPane.setVisible(false);
        });

        omrTask.setOnFailed(workerStateEvent -> {
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            loadingPane.getChildren().clear();

            if (source != null) {
                source.setDisable(false);
            }

            Throwable error = omrTask.getException();
            error.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("OMR Processing Error");
            alert.setHeaderText("Unable to process the OMR image");
            alert.setContentText(
                    error.getMessage() == null
                            ? "An unexpected error occurred."
                            : error.getMessage()
            );
            alert.showAndWait();
        });

        Thread workerThread = new Thread(omrTask, "omr-processing-thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private final TextArea debugTextArea = new TextArea();

    @FXML
    void changeType(ActionEvent event) {
        if (event != null) {
            ToggleButton toggle = (ToggleButton) event.getSource();

            stopRawImageAnimation();

            if ("OMR Image".equals(toggle.getText())) {

                showOmrView();

            } else {

                showDebugView();
            }
        } else {
            typeType.getToggles().get(1).setSelected(true);
            showDebugView();
        }
    }

    List<String> debugImageList = new ArrayList<>();

    private void showOmrView() {

        omrView.getItems().clear();

        rawImageView.setOpacity(0);
        rawImageView.setTranslateY(20);

        omrView.getItems().add(rawImageView);

        FadeTransition fade = new FadeTransition(Duration.millis(250), rawImageView);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), rawImageView);
        slide.setFromY(20);
        slide.setToY(0);

        rawImageAnimation = new ParallelTransition(fade, slide);
        rawImageAnimation.play();
    }

    private void showDebugView() {
        debugImageList = getImageFileLocations(path + "\\" + "debugOmr");
        imageCropperAndPreviewApp.getImageView().setImage(
                new Image(
                        new File(debugImageList.get(debugImageIndex)
                        ).toURI().toString()
                )
        );
        try {
            String replace = new File(debugImageList.get(debugImageIndex)).getName();
            replace = replace.substring(0, replace.indexOf('.')).replace("-", "");
            debugTextArea.setText(getStudentByRollNumber(omrTask.getValue(), replace));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        imageCropperAndPreviewApp.getLeftButton().setOnAction(event -> {
            debugImageIndex=debugImageIndex < debugImageList.size() - 1 ?
                    ++debugImageIndex : 0;
            someUknownWork();
        });
        imageCropperAndPreviewApp.getRightButton().setOnAction(event -> {
            debugImageIndex=debugImageIndex > 0 ? --debugImageIndex :
                            debugImageList.size() - 1;
            someUknownWork();
        });
        Region root = imageCropperAndPreviewApp.getRoot();

        root.setMinWidth(0);
        root.setMaxWidth(Double.MAX_VALUE);

        debugTextArea.setMinWidth(0);

        root.setOpacity(0);
        root.setTranslateX(-30);
        loadingPane.setStyle("-fx-background-color: red");
        debugTextArea.setOpacity(0);
        debugTextArea.setTranslateX(30);

        omrView.getItems().setAll(root, debugTextArea);

        Platform.runLater(() -> {
            omrView.setDividerPositions(0.7);

            FadeTransition rootFade = new FadeTransition(
                    Duration.millis(250), root
            );
            rootFade.setFromValue(0);
            rootFade.setToValue(1);

            TranslateTransition rootSlide = new TranslateTransition(
                    Duration.millis(250), root
            );
            rootSlide.setFromX(-30);
            rootSlide.setToX(0);

            FadeTransition textFade = new FadeTransition(
                    Duration.millis(250), debugTextArea
            );
            textFade.setFromValue(0);
            textFade.setToValue(1);

            TranslateTransition textSlide = new TranslateTransition(
                    Duration.millis(250), debugTextArea
            );
            textSlide.setFromX(30);
            textSlide.setToX(0);

            ParallelTransition animation = new ParallelTransition(
                    rootFade,
                    rootSlide,
                    textFade,
                    textSlide
            );

            animation.setOnFinished(e -> {
                root.setOpacity(1);
                root.setTranslateX(0);

                debugTextArea.setOpacity(1);
                debugTextArea.setTranslateX(0);
            });

            animation.play();
        });
    }

    private void someUknownWork() {
        imageCropperAndPreviewApp.getImageView().setImage(
                new Image(
                        new File(debugImageList.get(debugImageIndex)
                        ).toURI().toString()
                )
        );
        String replace = new File(debugImageList.get(debugImageIndex)).getName();
        replace = replace.substring(0, replace.indexOf('.')).replace("-", "");
        try {
            debugTextArea.setText(getStudentByRollNumber(omrTask.getValue(), replace));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopRawImageAnimation() {
        if (rawImageAnimation != null) {
            rawImageAnimation.stop();
            rawImageAnimation = null;
        }
    }

    public static List<String> getImageFileLocations(String folderLocation) {
        Path folder = Path.of(folderLocation);

        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException(
                    "Invalid folder location: " + folderLocation
            );
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName()
                                .toString()
                                .toLowerCase(Locale.ROOT);

                        return fileName.endsWith(".png")
                                || fileName.endsWith(".jpg")
                                || fileName.endsWith(".jpeg");
                    })
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Unable to scan folder: " + folderLocation, e);
        }
    }

    public static String getStudentByRollNumber(
            JsonNode root,
            String rollNumber
    ) throws JsonProcessingException {

        for (JsonNode student : root.path("students")) {
            if (rollNumber.equals(student.path("enrollmentNumber").asText())) {
                return new ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(student);
            }
        }

        return null;
    }
}
