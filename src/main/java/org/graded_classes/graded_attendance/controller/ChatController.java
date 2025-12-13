package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.checkerframework.checker.units.qual.N;
import org.graded_classes.graded_attendance.R;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {
    @FXML
    BorderPane root_box;
    @FXML
    ListView<HBox> list;
    @FXML
    Button send;
    MainController mainController;
    @FXML
    CustomTextField searchBar;
    @FXML
    Button add_new_member;

    public ChatController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //send.setGraphic(new FontIcon(Material2MZ.SEND));
        Node searchIcon = new FontIcon(Material2MZ.SEARCH);
        searchBar.setLeft(searchIcon);
        add_new_member.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT,Styles.FLAT);
        FontIcon node = new FontIcon();
        add_new_member.setGraphic(node);
        node.setStyle("""
                -fx-icon-code: mdral-add_comment;
                -fx-icon-size: 24;
                """);
        ObservableList<HBox> items = list.getItems();
        var mapStudent = mainController.gradedDataLoader.getStudentData();
        String[] messages = {"Hello", "How are you?", "Meeting at 5", "Check this out", "See you soon"};
        String[] times = {"11:00 PM", "10:45 PM", "9:30 AM", "8:15 PM", "7:00 AM"};
        for (var stu : mapStudent.keySet()) {
            int i = Integer.parseInt(stu.replace("ED", ""));
            String name = mapStudent.get(stu).name();
            String message = messages[i % messages.length];
            String time = times[i % times.length];

            HBox box = (HBox) mainController.gradedFxmlLoader.createView(
                    R.message_view,
                    new MessageViewController(String.valueOf(i + 1), name, "", message, time)
            );
            items.add(box);
        }

        list.setItems(items);
        list.getSelectionModel().selectedItemProperty().
                addListener((observable,
                             oldValue, newValue) -> {
                    String name = String.valueOf(((Label) newValue.lookup("#name")).getText());
                    String ed = String.valueOf(((Text) newValue.lookup("#num")).getText());
                    VBox box = (VBox) mainController.gradedFxmlLoader.createView(
                            R.message_box, new MessageBoxController(ed, "Last seen at 7:30pm", name)
                    );
                    root_box.setCenter(box);
                });

    }
}
