package org.graded_classes.graded_attendance.controller.camera;

import com.dlsc.gemsfx.PhotoView;
import com.dlsc.gemsfx.SVGImageView;
import com.dlsc.gemsfx.SearchField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.FacemarkLBF;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.controller.tts.RealTimeTts;
import org.graded_classes.graded_attendance.data.Student;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_calib3d.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_videoio.*;
import static org.graded_classes.graded_attendance.controller.quiz.QuizTaker.extractResourceToTempFile;

public class CameraController {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    public ToggleGroup modes;
    @FXML
    private PhotoView cameraView;
    @FXML
    private Button addStudent;
    @FXML
    private ScrollPane srollAtt;
    @FXML
    private VBox glasses;

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

    private final Mat frame = new Mat();
    private final Mat gray = new Mat();

    private final Object frameLock = new Object();
    private Rect lastFace;
    ObservableList<String> studentData = FXCollections.observableArrayList(List.of());

    private static final int FACE_SIZE = 200;
    private static final double CONFIDENCE_THRESHOLD = 70;
    @FXML
    private Button startCapture;
    @FXML
    private SVGImageView myLogo;
    @FXML
    private SVGImageView background;
    private Mat objectPoints;
    private Mat imagePoints;
    private Mat cameraMatrix;
    MainController mainController;
    private final Mat cleanFrame = new Mat();
    @FXML
    private Label gFront, hFront, ldFront, llFront, nFront, nDown, nLeft, nRight;

    public CameraController(MainController mainController) {
        this.mainController = mainController;
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
    void onModeChange(ActionEvent event) {
        var toggle = (ToggleButton) event.getSource();
        if (toggle.getText().equals("Training")) {
            startCapture.setDisable(true);
            addStudent.setDisable(false);
            searchBox.setDisable(false);
            srollAtt.setVisible(true);
            checkBoxGroup.setVisible(true);
        } else if (toggle.getText().equals("Face Detector")) {
            startCapture.setDisable(false);

            addStudent.setDisable(true);
            searchBox.setDisable(true);
            srollAtt.setVisible(false);
            checkBoxGroup.setVisible(false);
        }
    }

    @FXML
    void play(ActionEvent event) {
        String id = ((Button) event.getSource()).getId();
        switch (id) {
            case "normal" -> {
                timeTts.readAloud(", , Normal mode , ,");
            }
            case "long" -> {
                timeTts.readAloud(", , Long distance mode , ,");

            }
            case "low" -> {
                timeTts.readAloud(", , Low Light mode , ,");

            }
            case "glasses" -> {
                timeTts.readAloud(", , Glasses mode , ,");

            }
            case "hijab" -> {
                timeTts.readAloud(", , Hijab mode , ,");

            }
        }
    }

    @FXML
    void startCapturing(ActionEvent event) {
        Button source = (Button) event.getSource();
        String buttonId = source.getId();
        switch (buttonId) {
            case "nf" -> {
                int x = nFront.getText().contains(" ") ? (Integer.parseInt(nFront.getText().substring(nFront.getText().indexOf(' ')).trim()))+1 : 1;
                nFront.setText("Front " + x);
                clickThePhoto(buttonId);
                if (x>=10)
                    source.setDisable(true);

            }
        }
        clickThePhoto(buttonId);
    }

    private void clickThePhoto(String id) {
        Mat captureFace;

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

            captureFace = new Mat(cleanFrame, roi).clone();
        }

        Mat faceGray = new Mat();

        cvtColor(
                captureFace,
                faceGray,
                COLOR_BGR2GRAY
        );
        File f = new File(imagePath + "/" + id);
        if (!f.exists()) {
            f.mkdir();
        }
        String fileName = imagePath + "/" + id + "/" + System.currentTimeMillis() + ".jpg";
        imwrite(fileName, faceGray);
    }

    String imagePath;

    @FXML
    void addNewTraining(ActionEvent event) {
        String id = searchBox.getText();
        if (id.equals("")) {

        } else {
            Result result = getResult(id);
            CompletableFuture.runAsync(() -> {
                timeTts.readAloud(", , " + result.ed() + result.name() + "Welcome to GradeED Coaching Classes. Please be ready and stand in front of the camera.");
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
        studentData.addAll(asList(mainController.gradedDataLoader.getStudentData().values()));
        searchBox.setSuggestionProvider(request ->
                studentData.stream().filter(country ->
                                country.toLowerCase().contains(request.getUserText().toLowerCase())).
                        collect(Collectors.toList()));
        background.setSvgUrl(GradedResourceLoader.load("icons/new-back1.svg"));
        background.setOpacity(0.3);
        myLogo.setSvgUrl(GradedResourceLoader.load("icons/my-logo.svg"));
        ObservableList<String> cameras = getAvailableCameras();
        cameraList.setItems(cameras);
        CompletableFuture.runAsync(() -> {
            timeTts = new RealTimeTts();
            try {
                timeTts.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (cameras.isEmpty()) {
            System.out.println("No camera found");
            return;
        }
        loadFaceDetector();
        loadRecognizer();
        loadFacemark();
        create3DModel();
        if (cameras.size() > 1) {
            startCamera(1);
            cameraList.getSelectionModel().select(1);
        } else {
            startCamera(0);
            cameraList.getSelectionModel().selectFirst();
        }

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

    @FXML
    public void tryToDetect() {
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

            Rect roi = new Rect(
                    lastFace.x(),
                    lastFace.y(),
                    lastFace.width(),
                    lastFace.height()
            );

            capturedFace = new Mat(cleanFrame, roi).clone();
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
                try {
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
                } finally {
                    processing.set(false);
                }
            });
        });
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

    private List<String> asList(Collection<Student> values) {
        List<String> result = new ArrayList<>();
        for (Student student : values) {
            studentData.add(student.ed_no() + " " + student.name());
        }
        return result;
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
            frame.copyTo(cleanFrame);
            cvtColor(
                    cleanFrame,
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
                faceMovementDetection(largestFace);
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

    private void faceMovementDetection(Rect largestFace) {
        try (RectVector faceVector = new RectVector(1)) {
            faceVector.put(0, largestFace);
            try (Point2fVectorVector landmarks = new Point2fVectorVector()) {
                boolean success =
                        facemark.fit(gray, faceVector, landmarks);

                if (success && landmarks.size() > 0) {
                    Point2fVector points = landmarks.get(0);
                    createCorresponding2D(points);
                    createCameraMatrix();

                    Mat rvec = new Mat();
                    Mat tvec = new Mat();

                    solvePnP(
                            objectPoints,
                            imagePoints,
                            cameraMatrix,
                            new Mat(),
                            rvec,
                            tvec
                    );
                    Mat rotationMatrix = new Mat();

                    Rodrigues(
                            rvec,
                            rotationMatrix
                    );

                    DoubleIndexer r = rotationMatrix.createIndexer();
                    Mat mtxR = new Mat();
                    Rodrigues(rvec, mtxR);

                    Mat mtxQ = new Mat();
                    Mat Qx = new Mat();
                    Mat Qy = new Mat();
                    Mat Qz = new Mat();

                    Point3d angles = RQDecomp3x3(
                            mtxR,
                            mtxR,
                            mtxR,
                            Qx,
                            Qy,
                            Qz
                    );
                    double m00 = r.get(0, 0);
                    double m01 = r.get(0, 1);
                    double m02 = r.get(0, 2);

                    double m10 = r.get(1, 0);
                    double m11 = r.get(1, 1);
                    double m12 = r.get(1, 2);

                    double m20 = r.get(2, 0);
                    double m21 = r.get(2, 1);
                    double m22 = r.get(2, 2);
                    double pitch = angles.get(0);
                    double yaw = angles.get(1);
                    double roll = angles.get(2);
                    /*System.out.printf(
                            "Yaw: %.1f  Pitch: %.1f  Roll: %.1f%n",
                            yaw,
                            pitch,
                            roll
                    );*/
                    for (long i = 0; i < points.size(); i++) {

                        Point2f p = points.get(i);

                        circle(
                                frame,
                                new Point(
                                        Math.round(p.x()),
                                        Math.round(p.y())
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

    void createCorresponding2D(Point2fVector points) {
        Point2f nose = points.get(30);
        Point2f chin = points.get(8);
        Point2f leftEye = points.get(36);
        Point2f rightEye = points.get(45);
        Point2f leftMouth = points.get(48);
        Point2f rightMouth = points.get(54);
        imagePoints = new Mat(6, 2, CV_64FC1);

        DoubleIndexer img = imagePoints.createIndexer();

        img.put(0, 0, nose.x());
        img.put(0, 1, nose.y());

        img.put(1, 0, chin.x());
        img.put(1, 1, chin.y());

        img.put(2, 0, leftEye.x());
        img.put(2, 1, leftEye.y());

        img.put(3, 0, rightEye.x());
        img.put(3, 1, rightEye.y());

        img.put(4, 0, leftMouth.x());
        img.put(4, 1, leftMouth.y());

        img.put(5, 0, rightMouth.x());
        img.put(5, 1, rightMouth.y());
    }

    private void createCameraMatrix() {

        double focalLength = frame.cols();

        cameraMatrix = Mat.eye(3, 3, CV_64FC1).asMat();

        DoubleIndexer cam = cameraMatrix.createIndexer();

        cam.put(0, 0, focalLength);
        cam.put(1, 1, focalLength);

        cam.put(0, 2, frame.cols() / 2.0);
        cam.put(1, 2, frame.rows() / 2.0);

        cam.put(2, 2, 1.0);
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

    private void startTraining() {

        String dataPath =
                "C:\\Users\\hey\\GradedAttendance\\data";

        File root = new File(dataPath);

        if (!root.exists()) {
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
                        studentFolder.getName()
                );
            } catch (NumberFormatException e) {
                continue;
            }

            File[] images = studentFolder.listFiles();

            if (images == null) {
                continue;
            }

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
                    "attendance_model.yml"
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
        }
    }
}