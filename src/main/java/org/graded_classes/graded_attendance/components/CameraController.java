package org.graded_classes.graded_attendance.components;

import com.dlsc.gemsfx.PhotoView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;

public class CameraController {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    @FXML
    private PhotoView cameraView;

    @FXML
    private ComboBox<String> cameraList;

    private VideoCapture camera;
    private ScheduledExecutorService timer;

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer recognizer;

    private final Mat frame = new Mat();
    private final Mat gray = new Mat();

    private final Object frameLock = new Object();

    private Rect lastFace;

    private static final int FACE_SIZE = 200;
    private static final double CONFIDENCE_THRESHOLD =70;

    @FXML
    public void initialize() {

        ObservableList<String> cameras = getAvailableCameras();
        cameraList.setItems(cameras);

        if (cameras.isEmpty()) {
            System.out.println("No camera found");
            return;
        }

        cameraList.getSelectionModel().selectFirst();

        loadFaceDetector();
        loadRecognizer();

        startCamera(0);

        cameraList.getSelectionModel()
                .selectedIndexProperty()
                .addListener((obs, oldValue, newValue) -> {

                    if (newValue == null) {
                        return;
                    }

                    stopCamera();

                    startCamera(newValue.intValue());
                });
    }

    private void loadFaceDetector() {

        faceDetector = new CascadeClassifier(
                "src/main/resources/org/graded_classes/graded_attendance/haarcascade_frontalface_default.xml"
        );

        if (faceDetector.empty()) {
            throw new RuntimeException(
                    "Unable to load Haar Cascade."
            );
        }
    }

    private void loadRecognizer() {

        recognizer = LBPHFaceRecognizer.create();

        try {

            recognizer.read("attendance_model.yml");

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

        synchronized (frameLock) {

            if (!camera.read(frame) || frame.empty()) {

                System.out.println("Camera frame lost. Restarting...");

                restartCamera();

                return;
            }
            flip(frame, frame, 1); // un-mirror webcam preview

            cvtColor(
                    frame,
                    gray,
                    COLOR_BGR2GRAY
            );

            RectVector faces =
                    new RectVector();

            faceDetector.detectMultiScale(
                    gray,
                    faces,
                    1.1,
                    5,
                    0,
                    new Size(50, 50),
                    new Size()
            );

            lastFace = null;

            if (faces.size() > 0) {

                Rect largestFace =
                        faces.get(0);

                for (long i = 1;
                     i < faces.size();
                     i++) {

                    Rect current =
                            faces.get(i);

                    if (current.area()
                            > largestFace.area()) {

                        largestFace =
                                current;
                    }
                }

                lastFace = largestFace;

                rectangle(
                        frame,
                        new Point(
                                largestFace.x(),
                                largestFace.y()
                        ),
                        new Point(
                                largestFace.x()
                                        + largestFace.width(),
                                largestFace.y()
                                        + largestFace.height()
                        ),
                        new Scalar(
                                0,
                                255,
                                0,
                                0
                        ),
                        2,
                        LINE_8,
                        0
                );
            }

            Image image =
                    matToImage(frame);

            boolean faceDetected =
                    !faces.empty();

            Platform.runLater(() -> {

                cameraView.setPhoto(
                        image
                );

                cameraView.getStyleClass()
                        .set(1,
                                faceDetected
                                        ? "border-circle-green"
                                        : "border-circle-red"
                        );
            });
        }
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

    @FXML
    public void captureImage() {

        Mat capturedFace;

        synchronized (frameLock) {

            if (lastFace == null) {
                System.out.println("No face detected.");
                return;
            }

            Rect roi = new Rect(
                    lastFace.x(),
                    lastFace.y(),
                    lastFace.width(),
                    lastFace.height()
            );

            Mat capturedFrame = frame.clone();

            capturedFace = new Mat(
                    capturedFrame,
                    roi
            ).clone();
        }

        executor.submit(() -> {

            Mat faceGray = new Mat();

            cvtColor(
                    capturedFace,
                    faceGray,
                    COLOR_BGR2GRAY
            );

            resize(
                    faceGray,
                    faceGray,
                    new Size(FACE_SIZE, FACE_SIZE)
            );

            int[] label = new int[1];
            double[] confidence = new double[1];

            recognizer.predict(
                    faceGray,
                    label,
                    confidence
            );

            Platform.runLater(() -> {

                System.out.println(
                        "Student ID: "
                                + label[0]
                );

                System.out.println(
                        "Confidence: "
                                + confidence[0]
                );

                if (confidence[0] < CONFIDENCE_THRESHOLD) {

                    System.out.println(
                            "Recognized Student "
                                    + label[0]
                    );

                } else {

                    System.out.println(
                            "Unknown Person"
                    );
                }
            });
        });
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
}