package org.graded_classes.graded_attendance.new_features_and_ai_slop;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FloatingLabelExample extends Application {

    @Override
    public void start(Stage stage) {
        TextField textField = new TextField();
        Label floatingLabel = new Label("Username");

        floatingLabel.setStyle("-fx-text-fill: gray;");

        // Position label initially inside field
        floatingLabel.setTranslateY(0);

        // Animation
        TranslateTransition up = new TranslateTransition(Duration.millis(150), floatingLabel);
        up.setToY(-20);

        TranslateTransition down = new TranslateTransition(Duration.millis(150), floatingLabel);
        down.setToY(0);

        // Listeners
        textField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused || !textField.getText().isEmpty()) {
                up.play();
            } else {
                down.play();
            }
        });

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                up.play();
            } else if (!textField.isFocused()) {
                down.play();
            }
        });

        StackPane stack = new StackPane(textField, floatingLabel);

        VBox root = new VBox(20, stack);
        root.setStyle("-fx-padding: 20;");

        stage.setScene(new Scene(root, 300, 200));
        stage.setTitle("Floating Label Demo");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
