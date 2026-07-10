package org.graded_classes.graded_attendance.controller.leaderboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;



public class LayoutAnimatorTimer implements Initializable {

    @FXML
    private Spinner<Double> layout_duration, fade_duration;

    @FXML
    private Text node_name;

    String name;
    double l_duration, f_duration;

    public LayoutAnimatorTimer(String name, double l_duration, double f_duration) {
        this.l_duration = l_duration;
        this.f_duration = f_duration;
        this.name = name;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        node_name.setText(name);
        layout_duration.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(7, 3600, 7)); // min, max, initialValue;
        fade_duration.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(2, 5, 2)); // min, max, initialValue;
        layout_duration.getValueFactory().setValue(l_duration);
        fade_duration.getValueFactory().setValue(f_duration);
        layout_duration.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
           // var am=defaultAnimationDuration.get(name);
           // am.setLayoutDuration(Double.parseDouble(newValue));
        });
        fade_duration.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            //var am=defaultAnimationDuration.get(name);
            //am.setFadeTime(Double.parseDouble(newValue));
        });

    }

}
