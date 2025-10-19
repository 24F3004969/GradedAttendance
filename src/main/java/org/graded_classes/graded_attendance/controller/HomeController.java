package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.ModalPane;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.data.GradedDataLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.graded_classes.graded_attendance.GradedResourceLoader.loadStream;
import static org.graded_classes.graded_attendance.GradedResourceLoader.loadURL;

public class HomeController implements Initializable {
    @FXML
    AnchorPane searchBarHolder;
    @FXML
    VBox outer_main_box;
    @FXML
    VBox stu_atten, tea_atten, st_fee, tea_fee, tea_prog;
    @FXML
    AnchorPane home_tab_main_view;
    ModalPane modalPane;
    VBox dummy_box = new VBox();
    GradedDataLoader gradedDataLoader;
    MainController mainController;
    GradedFxmlLoader gradedFxmlLoader = new GradedFxmlLoader();

    public HomeController(ModalPane modalPane, GradedDataLoader gradedDataLoader, MainController mainController) {
        this.modalPane = modalPane;
        this.gradedDataLoader = gradedDataLoader;
        this.mainController = mainController;
    }

    private VBox selectedItem;


    @FXML
    void onItemClick(MouseEvent mouseEvent) {
        VBox vBox = (VBox) mouseEvent.getSource();
        System.out.println(vBox);
        if (vBox.getId().equals(stu_atten.getId())) {
            toggleOut(getIconPaths());
            toggleIn(vBox, "icons/attendance_student.png");
            selectedItem = stu_atten;
        } else if (vBox.getId().equals(tea_atten.getId())) {
            toggleOut(getIconPaths());
            toggleIn(vBox, "icons/teacher_attendance.png");
            selectedItem = tea_atten;

        } else if (vBox.getId().equals(st_fee.getId())) {
            toggleOut(getIconPaths());
            toggleIn(vBox, "icons/fee_book.png");
            selectedItem = st_fee;

        } else if (vBox.getId().equals(tea_fee.getId())) {
            toggleOut(getIconPaths());
            toggleIn(vBox, "icons/teacher_payment.png");
            selectedItem = tea_fee;

        } else if (vBox.getId().equals(tea_prog.getId())) {
            toggleOut(getIconPaths());
            toggleIn(vBox, "icons/teaching_progress.png");
            selectedItem = tea_prog;
        }
        if (outer_main_box.getChildren().size() > 3)
            outer_main_box.getChildren().removeLast();
        Node searchBox = getSearchView(vBox);
        if (searchBox != null) {
            if (!home_tab_main_view.getChildren().contains(searchBarHolder))
                home_tab_main_view.getChildren().add(searchBarHolder);
            VBox.setVgrow(searchBox, Priority.ALWAYS);
            AnchorPane.setRightAnchor(searchBox, 0.0);
            AnchorPane.setLeftAnchor(searchBox, 0.0);
            searchBarHolder.getChildren().set(searchBarHolder.getChildren().size() - 1, searchBox);
        }

    }


    private Node getSearchView(VBox vBox) {
        return switch (vBox.getId()) {
            case "stu_atten", "st_fee"-> gradedFxmlLoader.createView(R.student_attendance_layout,
                    new StudentAttendance(mainController, gradedFxmlLoader, outer_main_box, vBox.getId()));
            case "tea_prog"-> gradedFxmlLoader.createView(R.teaching_progress_search,
                    new StudentAttendance(mainController, gradedFxmlLoader, outer_main_box, vBox.getId()));
            case "tea_atten", "tea_fee" -> gradedFxmlLoader.createView(R.teacher_attendance_layout);
            default -> null;
        };
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        selectedItem = stu_atten;
        VBox studentAttendanceLayout = (VBox) mainController.gradedFxmlLoader.createView(R.student_attendance_layout,
                new StudentAttendance(mainController, gradedFxmlLoader, outer_main_box, "stu_atten"));
        dummy_box.setMinHeight(70);
        // var data_viewer = gradedFxmlLoader.createView(R.data_view);
        VBox.setVgrow(studentAttendanceLayout, Priority.ALWAYS);
        AnchorPane.setRightAnchor(studentAttendanceLayout, 0.0);
        AnchorPane.setLeftAnchor(studentAttendanceLayout, 0.0);
        //VBox.setVgrow(data_viewer, Priority.ALWAYS);
        searchBarHolder.getChildren().add(studentAttendanceLayout);
        outer_main_box.getChildren().add(dummy_box);
        //outer_main_box.getChildren().add(data_viewer);

    }

    private void toggleIn(VBox vBox, String paths) {
        ImageView imageView = (ImageView) vBox.lookup("#box_icon");
        imageView.setImage(new Image(loadStream(paths)));
        vBox.getStylesheets().removeFirst();
        vBox.getStylesheets().add(loadURL("css/toggle_in.css").toExternalForm());
    }

    private void toggleOut(String paths) {
        ImageView imageView = (ImageView) selectedItem.lookup("#box_icon");
        imageView.setImage(new Image(loadStream(paths)));
        selectedItem.getStylesheets().removeFirst();
        selectedItem.getStylesheets().add(loadURL("css/toggle_out.css").toExternalForm());
    }

    private String getIconPaths() {
        if (selectedItem.equals(stu_atten)) {
            return "icons/attendance_student_normal.png";
        } else if (selectedItem.equals(tea_atten)) {
            return "icons/teacher_attendance_normal.png";
        } else if (selectedItem.equals(st_fee)) {
            return "icons/fee_book_normal.png";
        } else if (selectedItem.equals(tea_fee)) {
            return "icons/teacher_payment_normal.png";
        } else if (selectedItem.equals(tea_prog)) {
            return "icons/teaching_progress_normal.png";
        }
        return "icons/fee_book_normal.png";
    }

    @FXML
    public void onButtonPressed(ActionEvent actionEvent) {
        BorderPane dialog = null;
        try {
           FXMLLoader fxmlLoader = new FXMLLoader(loadURL("fxml/time_table_classes.fxml"));
            var time=new TimeTableClass(this);
            fxmlLoader.setControllerFactory(c -> time);
            dialog = fxmlLoader.load();
            dialog.setStyle("-fx-background-color: #fafafa;");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        modalPane.show(dialog);

    }

    @FXML
    public void onButtonPressedNewStudent(ActionEvent actionEvent) {
        Parent dialog = null;
        try {
            var x = new FXMLLoader(loadURL("fxml/add_people.fxml"));
            x.setControllerFactory(c -> new AddPeople(this));
            dialog = x.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        modalPane.show(dialog);
    }
}