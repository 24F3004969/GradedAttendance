package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.checkerframework.checker.units.qual.N;
import org.graded_classes.graded_attendance.R;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatController implements Initializable {
    @FXML
    ListView<HBox> list;
    @FXML
    Button send;
    MainController mainController;
    @FXML
    CustomTextField searchBar;

    public ChatController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        send.setGraphic(new FontIcon(Material2MZ.SEND));
        Node searchIcon = new FontIcon(Material2MZ.SEARCH);
        searchBar.setLeft(searchIcon);
        send.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_CIRCLE);
        ObservableList<HBox> items = list.getItems();
        String[] names = {"Helal Anwar", "Sara Khan", "John Doe", "Aisha Malik", "David Smith"};
        String[] messages = {"Hello", "How are you?", "Meeting at 5", "Check this out", "See you soon"};
        String[] times = {"11:00 PM", "10:45 PM", "9:30 AM", "8:15 PM", "7:00 AM"};

        for (int i = 0; i < 100; i++) {
            String name = names[i % names.length];
            String message = messages[i % messages.length];
            String time = times[i % times.length];

            HBox box = (HBox) mainController.gradedFxmlLoader.createView(
                    R.message_view,
                    new MessageViewController(String.valueOf(i + 1), name,"",message, time)
            );
            items.add(box);
        }

        list.setItems(items);

    }
}
