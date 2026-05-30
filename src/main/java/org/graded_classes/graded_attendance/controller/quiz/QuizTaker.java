package org.graded_classes.graded_attendance.controller.quiz;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class QuizTaker implements Initializable {

    Timeline timeline;
    LocalTime totalTime;
    @FXML
    private TilePane questionNum;
    @FXML
    private Label question_num;
    @FXML
    private Label quizName;
    @FXML
    private VBox questionStack;
    @FXML
    private Label timer;
    int indexOfQuestion = 0;
    MainController mainController;
    ArrayList<QuestionData> questionList = new ArrayList<>();
    ArrayList<SplitPane> listOfSplitPane = new ArrayList<>();
    LinkedHashMap<QuestionData, ArrayList<Integer>> selectedOptions = new LinkedHashMap<>();

    public QuizTaker(MainController mainController) {
        this.mainController = mainController;
        CompletableFuture.runAsync(this::initDb);
    }

    private void initDb() {
        try {
            var connection = mainController.gradedDataLoader.databaseLoader.getConnection();
            var sql = "select * from Questions where user_id=1";
            PreparedStatement pst = connection.prepareStatement(sql);
            var rs = pst.executeQuery();
            while (rs.next()) {
                var inner_sql = "Select * from QuestionOptions where question_id=?";
                PreparedStatement _pst = connection.prepareStatement(inner_sql);
                _pst.setString(1, rs.getString("question_id"));
                var _rs = _pst.executeQuery();
                OptionData questionOption = null;
                ArrayList<String> options = new ArrayList<>();
                int correctId = 0;
                while (_rs.next()) {
                    options.add(_rs.getString("option_text"));
                    var id = _rs.getInt("is_correct");
                    if (id == 1)
                        correctId = options.size();
                }
                questionOption = new OptionData(correctId, options);
                QuestionData questionData = new QuestionData(rs.getString("question_id"),
                        rs.getString("topic_id"),
                        rs.getString("user_id"),
                        rs.getString("date_of_making"),
                        rs.getString("type"),
                        rs.getString("level"),
                        rs.getString("question_txt"),
                        rs.getString("question_img_path"), questionOption);
                questionList.add(questionData);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void nextQuestion() {
        var index = indexOfQuestion < listOfSplitPane.size() - 1 ?
                ++indexOfQuestion : listOfSplitPane.size() - 1;
        VBox.setVgrow(listOfSplitPane.get(index), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.get(index));
        question_num.setText("Question " + (index + 1));
    }

    @FXML
    void onQuizSubmit() {
        totalTime = LocalTime.parse("00:00:00",
                DateTimeFormatter.ofPattern("HH:mm:ss"));
        saveQuizInstance("ED01", selectedOptions);
    }

    @FXML
    void previousQuestion() {
        var index = indexOfQuestion > 0 ? --indexOfQuestion : 0;
        VBox.setVgrow(listOfSplitPane.get(index), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.get(index));
        question_num.setText("Question " + (index + 1));
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        for (QuestionData questionData : questionList) {
            var qp = new QuestionTakerWthOp(questionData, selectedOptions);
            var qop = (SplitPane) mainController.gradedFxmlLoader.createView(R.question_taker_with_op, qp);
            listOfSplitPane.add(qop);
        }
        VBox.setVgrow(listOfSplitPane.getFirst(), Priority.ALWAYS);
        questionStack.getChildren().set(1, listOfSplitPane.getFirst());
        for (int i = 0; i < questionList.size(); i++) {
            Button button = getButton(i);
            questionNum.getChildren().add(button);
        }
    }

    private @NotNull Button getButton(int i) {
        Button button = new Button();
        button.setText((i + 1) + "");
        button.setOnAction(event -> {
            indexOfQuestion = Integer.parseInt(button.getText()) - 1;
            int index = indexOfQuestion;
            VBox.setVgrow(listOfSplitPane.get(index), Priority.ALWAYS);
            questionStack.getChildren().set(1, listOfSplitPane.get(index));
            question_num.setText("Question " + (index + 1));
        });
        button.setPrefHeight(50);
        button.setPrefWidth(50);
        return button;
    }

    public void startQuiz(ExamLogin login, Stage stage) {
        quizName.setText("Quiz");
        totalTime = LocalTime.parse("00:15:10", DateTimeFormatter.ofPattern("HH:mm:ss"));

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (totalTime.equals(LocalTime.MIDNIGHT)) {
                        timeline.stop();
                        onQuizSubmit();
                        stage.setTitle("Exam Login");
                        stage.setFullScreen(true);
                        animationLottie4j(login.root);
                        stage.getScene().setRoot(login.root);
                        System.out.println("Time is up!");
                    } else {
                        totalTime = totalTime.minusSeconds(1);
                        timer.setText(totalTime.toString());
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        stage.getScene().setOnKeyPressed(event1 -> {


            if (event1.getCode() == KeyCode.E &&
                    event1.isControlDown() &&
                    event1.isShiftDown()) {

                login.showPasswordDialog();
            }


        });

    }

    //Be careful it this function is AI written
    public void animationLottie4j(StackPane stackPane) {

        // ✅ Use the *actual* resource path as it exists in the jar.
        // From your log, it is:
        // /org/graded_classes/graded_attendance/css/motivation.json
        final String resource = "/org/graded_classes/graded_attendance/css/motivation.json";

        final Animation animationFile;
        try {
            File lottieJson = extractResourceToTempFile(resource, "motivation-", ".json");
            animationFile = LottieFileLoader.load(lottieJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Lottie animation from " + resource, e);
        }

        var lottiePlayer = new LottiePlayer(animationFile, animationFile.width(), animationFile.height());

        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-background-color: white;");
        box.getChildren().add(lottiePlayer);

        Label leb = new Label(
                "“You survived the done_using_ai — and that already deserves a medal 🏅! " +
                        "Remember, even pencils make mistakes but they keep going. " +
                        "So whether you nailed it or just wrestled with it… you showed up, " +
                        "and that’s what winners do! Now go celebrate — you’ve earned it!”"
        );
        leb.setWrapText(true);
        leb.setAlignment(Pos.CENTER);
        leb.setTextAlignment(TextAlignment.CENTER);
        leb.setFont(Font.font(30));
        leb.maxWidthProperty().bind(box.widthProperty().multiply(0.8));
        box.getChildren().add(leb);
        IO.println(GradedResourceLoader.load("icons/my-logo.svg"));
        IO.println(GradedResourceLoader.load("css/motivation.json"));
        ImageView imageView = new ImageView(new Image(GradedResourceLoader.load("icons/my-logo.svg")));
        imageView.setFitWidth(200);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        StackPane.setAlignment(box, Pos.CENTER);
        StackPane.setAlignment(imageView, Pos.BOTTOM_CENTER);
        StackPane.setMargin(imageView, new Insets(5, 5, 15, 5));
        stackPane.getChildren().set(1, box);
        stackPane.getChildren().add(imageView);

        lottiePlayer.play();
    }

    public void saveQuizInstance(String studentEd, LinkedHashMap<QuestionData, ArrayList<Integer>>
            selectedOptions) {
        String url = "jdbc:sqlite:" + "G:/My Drive/GradeEd_Exam_2026/" + studentEd + ".db";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            for (QuestionData question : selectedOptions.keySet()) {
                String createTableSQL = """
                        INSERT INTO answers (
                            exam_id,
                            question_id,
                            selected_option_id,
                            time_slot,
                            start_time,
                            end_time,
                            created_at
                        ) VALUES (%s, %s, %s, '%s', '%s', '%s', '%s');
                        """.formatted(
                        "1",
                        question.question_id(),
                        question.option_data().option_index(),
                        "",
                        "",
                        "",
                        ""
                );
                stmt.execute(createTableSQL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract a classpath resource (even when inside a jar / jpackage image)
     * into a real temp file and return it.
     */
    //Be careful it this function is AI written
    private static File extractResourceToTempFile(String resourcePath, String prefix, String suffix) throws IOException {
        try (InputStream in = QuizTaker.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException(
                        "Resource not found on classpath: " + resourcePath +
                                "\nMake sure it exists inside the jar with the same path."
                );
            }

            Path tmp = Files.createTempFile(prefix, suffix);
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, REPLACE_EXISTING);
            return tmp.toFile();
        }

    }
}
