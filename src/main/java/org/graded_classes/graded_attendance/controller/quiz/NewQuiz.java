package org.graded_classes.graded_attendance.controller.quiz;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class NewQuiz implements Initializable {

    @FXML
    private Spinner<Integer> NoOfQuestion;

    @FXML
    private ComboBox<String> classes;

    @FXML
    private Label quizName;
    @FXML
    private TilePane selectorDisplay;
    TreeItem<String> target;
    QuizGenerator quizGenerator;
    MainController mainController;

    public NewQuiz(TreeItem<String> target, QuizGenerator quizGenerator, MainController mainController) {
        this.target = target;
        this.quizGenerator = quizGenerator;
        this.mainController = mainController;
    }

    @FXML
    void onButtonClick() {
        var newChild = new TreeItem<>(quizName.getText());
        FontIcon node = new FontIcon();
        newChild.setGraphic(
                node);
        node.getStyleClass().add("ikonli-font-icon");
        node.setStyle("""
                    -fx-icon-code:mdi2f-folder;
                    -fx-icon-color:#944a00;
                    """);
        for (int i = 1; i <= 5; i++) {
            newChild.getChildren().add(new TreeItem<>("Question " + i,new FontIcon("mdi2n-note")));
        }
        target.getChildren().add(newChild);
        TabPane tabPane = (TabPane) quizGenerator.quiz_gen_layout.lookup("#tabs");
        var tb = mainController.gradedFxmlLoader.createView(R.question_editor,
                new QuestionEditor(mainController));
        Tab tab = new Tab(quizName.getText());
        tab.setContent(tb);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        target.setExpanded(true);
        mainController.modalPane.hide();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        quizName.setText("Draft_" + LocalDate.now() + "_" + LocalTime.now().
                format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 30, 5, 1);
        NoOfQuestion.setValueFactory(valueFactory);
        classes.setItems(FXCollections.observableArrayList("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"));
    }
}