package org.graded_classes.graded_attendance.components;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;

public class CameraTest {

    @FXML
    private ImageView cameraView;

    @FXML
    private StackPane root;

    private VideoCapture camera;
    private ScheduledExecutorService timer;

    private final Mat frame = new Mat();
    private final Mat gray = new Mat();

    private CascadeClassifier faceDetector;

    private Rect lastFace;

    private int id = 108;

    @FXML
    public void initialize() {

        cameraView.fitWidthProperty().bind(root.widthProperty());
        cameraView.fitHeightProperty().bind(root.heightProperty());

        faceDetector = new CascadeClassifier(
                "src/main/resources/org/graded_classes/graded_attendance/haarcascade_frontalface_default.xml"
        );

        if (faceDetector.empty()) {
            System.out.println("Failed to load cascade");
            return;
        }

        camera = new VideoCapture(1, CAP_DSHOW);

        if (!camera.isOpened()) {
            System.out.println("Unable to open camera");
            return;
        }

        camera.set(CAP_PROP_FRAME_WIDTH, 1280);
        camera.set(CAP_PROP_FRAME_HEIGHT, 720);

        timer = Executors.newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(() -> {

            if (!camera.read(frame) || frame.empty()) {
                return;
            }

            cvtColor(frame, gray, COLOR_BGR2GRAY);

            RectVector faces = new RectVector();

            faceDetector.detectMultiScale(gray, faces);

            lastFace = null;

            if (faces.size() > 0) {

                Rect largestFace = faces.get(0);

                for (long i = 1; i < faces.size(); i++) {

                    Rect r = faces.get(i);

                    if (r.area() > largestFace.area()) {
                        largestFace = r;
                    }
                }

                lastFace = largestFace;

                rectangle(
                        frame,
                        new Point(largestFace.x(), largestFace.y()),
                        new Point(
                                largestFace.x() + largestFace.width(),
                                largestFace.y() + largestFace.height()
                        ),
                        new Scalar(0, 255, 0, 0),
                        2,
                        LINE_8,
                        0
                );
            }

            Image image = matToImage(frame);

            Platform.runLater(() ->
                    cameraView.setImage(image));

        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    private Image matToImage(Mat mat) {

        BytePointer buf = new BytePointer();

        imencode(".jpg", mat, buf);

        byte[] bytes = new byte[(int) buf.limit()];
        buf.get(bytes);

        return new Image(
                new ByteArrayInputStream(bytes)
        );
    }

    @FXML
    public void captureImage() {

        if (lastFace == null) {
            System.out.println("No face found");
            return;
        }

        Rect roi = new Rect(
                lastFace.x(),
                lastFace.y(),
                lastFace.width(),
                lastFace.height()
        );

        Mat face = new Mat(frame, roi);

        Mat faceGray = new Mat();

        cvtColor(face, faceGray, COLOR_BGR2GRAY);

        resize(
                faceGray,
                faceGray,
                new Size(200, 200)
        );

        String fileName =
                "student" + /*id++ + */".jpg";

        imwrite(fileName, faceGray);

        System.out.println(
                "Saved: " + fileName
        );
        try (LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create()) {

            recognizer.read("attendance_model.yml");

            Mat testImage = imread("student.jpg", 0);

            resize(testImage, testImage, new Size(200, 200));

            int[] label = new int[1];
            double[] confidence = new double[1];

            recognizer.predict(testImage, label, confidence);

            System.out.println("Label: " + label[0]);
            System.out.println("Confidence: " + (100-confidence[0]));

            if (label[0] == 1 && confidence[0] < 60) {
                System.out.println("Attendance Marked");
            } else {
                System.out.println("Unknown Person");
            }
        }
    }

    public void stopCamera() {

        if (timer != null) {
            timer.shutdown();
        }

        if (camera != null) {
            camera.release();
        }
    }
}