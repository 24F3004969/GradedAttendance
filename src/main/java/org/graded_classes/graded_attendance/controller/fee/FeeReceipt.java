package org.graded_classes.graded_attendance.controller.fee;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.data.FeeData;

import java.net.URL;
import java.util.ResourceBundle;

public class FeeReceipt implements Initializable {

    @FXML
    private Label amount;

    @FXML
    private Label edNo;

    @FXML
    private Label mode;
    @FXML
    private Label rec_name;
    @FXML
    private StackPane mainPane;
    @FXML
    private Label name;
    String s_name, s_ed, s_mode, recName;
    double d_amount;

    public FeeReceipt(String s_name, String s_ed, String s_mode, double d_amount, String recName) {
        this.s_name = s_name;
        this.s_ed = s_ed;
        this.s_mode = s_mode;
        this.d_amount = d_amount;
        this.recName = recName;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("FeeReceipt initializing");
        amount.setText("" + d_amount);
        edNo.setText(s_ed);
        mode.setText(s_mode);
        name.setText(s_name);
        rec_name.setText(recName);
    }


    private StackPane addStamp() {
        StackPane pane = new StackPane();
        Group textGroup = new Group();
        Font font = Font.font("Arial", FontWeight.BOLD, FontPosture.REGULAR, 12);
        String welcome = " ★ MANGO 2026★ ★ GRADED COACHING CLASSES ★";
        double rotation = 90;

        double radius = 30d;

        for (char c : welcome.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                Text text = new Text(Character.toString(c));
                text.setFont(font);
                text.setFill(Color.web("#1C75BCFF"));
                Rotate rotationMatrix = new Rotate(rotation, 0, radius);
                text.getTransforms().add(rotationMatrix);

                textGroup.getChildren().add(text);
            }
            rotation += 8.5;
        }
        pane.getChildren().add(textGroup);
        var imageView = new ImageView(new Image(GradedResourceLoader.load("icons/ed_short.png")));
        imageView.setFitWidth(20);
        imageView.setPreserveRatio(true);
        pane.getChildren().add(imageView);
        return pane;
    }
}
