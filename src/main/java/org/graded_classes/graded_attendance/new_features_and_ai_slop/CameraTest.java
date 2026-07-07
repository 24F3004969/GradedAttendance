package org.graded_classes.graded_attendance.new_features_and_ai_slop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraTest {

    @FXML
    private ImageView cameraView;

    @FXML
    private StackPane root;

    private VideoCapture camera;
    private ScheduledExecutorService timer;
    private final Mat currentFrame = new Mat();

    private final Mat frame = new Mat();
    private final Mat previewFrame = new Mat();
    private final Mat gray = new Mat();
    private final MatOfRect faces = new MatOfRect();

    private CascadeClassifier faceDetector;

    private Rect lastDetectedFace;

    private int id = 1;

    @FXML
    public void initialize() {
        cameraView.fitWidthProperty().bind(root.widthProperty());
        cameraView.fitHeightProperty().bind(root.heightProperty());
        faceDetector = new CascadeClassifier(
                "C:\\Users\\hilal\\GradedAttendance\\src\\main\\resources\\org\\graded_classes\\graded_attendance\\haarcascade_frontalface_default.xml"
        );

        if (faceDetector.empty()) {
            System.out.println("Failed to load cascade");
            return;
        }

        camera = new VideoCapture(1);

        if (!camera.isOpened()) {
            System.out.println("Unable to open camera");
            return;
        }

        // Request 4K
        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 3840);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 2160);

        System.out.println(
                "Camera Resolution : "
                        + camera.get(Videoio.CAP_PROP_FRAME_WIDTH)
                        + " x "
                        + camera.get(Videoio.CAP_PROP_FRAME_HEIGHT)
        );

        timer = Executors.newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(() -> {

            if (!camera.read(frame) || frame.empty()) {
                return;
            }

            synchronized (currentFrame) {
                frame.copyTo(currentFrame);
            }

            // Preview at 720p
            Imgproc.resize(
                    frame,
                    previewFrame,
                    new Size(1280, 720)
            );

            Imgproc.cvtColor(
                    previewFrame,
                    gray,
                    Imgproc.COLOR_BGR2GRAY
            );

            Imgproc.equalizeHist(
                    gray,
                    gray
            );

            faceDetector.detectMultiScale(
                    gray,
                    faces,
                    1.1,
                    5,
                    0,
                    new Size(80, 80),
                    new Size()
            );

            Rect[] detectedFaces = faces.toArray();

            lastDetectedFace = null;

            if (detectedFaces.length > 0) {

                Rect largestFace = detectedFaces[0];

                for (Rect rect : detectedFaces) {

                    if (rect.area() > largestFace.area()) {
                        largestFace = rect;
                    }
                }

                lastDetectedFace = largestFace;

                Imgproc.rectangle(
                        previewFrame,
                        new Point(
                                largestFace.x,
                                largestFace.y
                        ),
                        new Point(
                                largestFace.x + largestFace.width,
                                largestFace.y + largestFace.height
                        ),
                        new Scalar(0, 255, 0),
                        3
                );
            }

            Image image = matToImage(previewFrame);

            Platform.runLater(() ->
                    cameraView.setImage(image));

        }, 0, 33, TimeUnit.MILLISECONDS); // ~20 FPS
    }

    private Image matToImage(Mat mat) {

        MatOfByte buffer = new MatOfByte();

        Imgcodecs.imencode(
                ".jpg",
                mat,
                buffer
        );

        return new Image(
                new ByteArrayInputStream(
                        buffer.toArray()
                )
        );
    }

    @FXML
    public void captureImage() {

        if (lastDetectedFace == null) {
            System.out.println("No face detected");
            return;
        }

        Mat original;

        synchronized (currentFrame) {
            original = currentFrame.clone();
        }

        double scaleX =
                original.width() / 1280.0;

        double scaleY =
                original.height() / 720.0;

        Rect face4k = new Rect(
                (int) (lastDetectedFace.x * scaleX),
                (int) (lastDetectedFace.y * scaleY),
                (int) (lastDetectedFace.width * scaleX),
                (int) (lastDetectedFace.height * scaleY)
        );

        int padding = 30;

        face4k.x = Math.max(0, face4k.x - padding);
        face4k.y = Math.max(0, face4k.y - padding);

        face4k.width = Math.min(
                original.width() - face4k.x,
                face4k.width + padding * 2
        );

        face4k.height = Math.min(
                original.height() - face4k.y,
                face4k.height + padding * 2
        );

        Mat face = new Mat(
                original,
                face4k
        );

        Mat grayFace = new Mat();

        Imgproc.cvtColor(
                face,
                grayFace,
                Imgproc.COLOR_BGR2GRAY
        );

        Mat resizedFace = new Mat();

        Imgproc.resize(
                grayFace,
                resizedFace,
                new Size(200, 200)
        );

        String fileName =
                "student_" + id++ + ".jpg";

        Imgcodecs.imwrite(
                fileName,
                resizedFace
        );

        System.out.println(
                "Saved : " + fileName
        );
    }

    public void stopCamera() {

        if (timer != null) {
            timer.shutdown();
        }

        if (camera != null &&
                camera.isOpened()) {

            camera.release();
        }
    }
}