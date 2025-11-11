package org.graded_classes.graded_attendance.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.graded_classes.graded_attendance.data.Student;
import org.graded_classes.graded_attendance.data.StudentInfo;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;
import org.kordamp.ikonli.material2.Material2OutlinedAL;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataBaseController implements Initializable {
    @FXML
    Button reload, delete;
    MainController mainController;
    @FXML
    VBox database_tab;
    @FXML
    private TableColumn<StudentInfo, String> doa, ed_no, grade, name;
    @FXML
    private TableColumn<StudentInfo, String> last_fee;
    @FXML
    private TableColumn<StudentInfo, CheckBox> checkboxes;
    ObservableList<StudentInfo> items = FXCollections.observableArrayList();
    ArrayList<StudentInfo> selectedStudent;
    Map<String, Student> studentsMap;
    @FXML
    private TableView<StudentInfo> studentData;
    @FXML
    TextField filterText;
    @FXML
    private MenuButton filterMenu;

    public DataBaseController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        studentsMap = mainController.gradedDataLoader.getStudentData();
        reload.getStyleClass().addAll(Styles.SUCCESS, Styles.FLAT, Styles.BUTTON_CIRCLE);
        delete.getStyleClass().addAll(Styles.DANGER, Styles.FLAT, Styles.BUTTON_CIRCLE);
        reload.setGraphic(new FontIcon(Material2MZ.REPLAY));
        delete.setGraphic(new FontIcon(Material2OutlinedAL.DELETE));
        var data = mainController.gradedDataLoader.getStudentData();
        Styles.toggleStyleClass(studentData, Styles.BORDERED);
        FilteredList<StudentInfo> filteredData = new FilteredList<>(items, _ -> true);
        filterText.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(val -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String upperCase = newValue.toUpperCase();
                return switch (filterMenu.getText()) {
                    case "Class" -> val.grade().getValue().equals(upperCase);
                    case "ED No." -> val.ed_no().getValue().trim().contains(upperCase);
                    case "Name" -> val.name().getValue().contains(upperCase);
                    default -> false;
                };
            });
        });
        selectedStudent = new ArrayList<>();
        checkboxes.setCellValueFactory(arg0 -> {
            CheckBox checkBox = new CheckBox();
            StudentInfo studentInfo = arg0.getValue();
            checkBox.selectedProperty().setValue(studentInfo.active().getValue());
            checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                studentInfo.setActive(new_val);
                if (checkBox.isSelected()) {
                    delete.setDisable(false);
                    selectedStudent.add(studentInfo);
                } else {
                    checkBox.setSelected(new_val);
                    selectedStudent.remove(studentInfo);
                    if (selectedStudent.isEmpty())
                        delete.setDisable(true);
                }
            });
            return new SimpleObjectProperty<>(checkBox);
        });
        ed_no.setCellValueFactory(map -> map.getValue().ed_no());
        name.setCellValueFactory(map -> map.getValue().name());
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        grade.setCellValueFactory(map -> map.getValue().grade());
        grade.setCellFactory(TextFieldTableCell.forTableColumn());
        doa.setCellValueFactory(map -> map.getValue().date_of_admission());
        doa.setCellFactory(TextFieldTableCell.forTableColumn());
        doa.setOnEditCommit(event -> {
            String value = event.getNewValue();
            try {
                LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String ed = event.getTableView().getItems().get(event.getTablePosition().getRow()).ed_no().getValue();
                System.out.println(ed);
                event.getTableView().getItems().get(event.getTablePosition().getRow()).date_of_admission().setValue(localDate.toString());
                updateAdmissionDateDb(localDate.toString(), ed);
            } catch (Exception e) {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid date or format is invalid.\nCorrect format is: dd/MM/yyyy");
                alert.showAndWait();
            }
        });
        last_fee.setCellValueFactory(map -> map.getValue().last_fee_date());
        last_fee.setCellFactory(TextFieldTableCell.forTableColumn());

        last_fee.setOnEditCommit(event -> {
            String value = event.getNewValue();
            try {
                LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String ed = event.getTableView().getItems().get(event.getTablePosition().getRow()).ed_no().getValue();
                System.out.println(ed);
                event.getTableView().getItems().get(event.getTablePosition().getRow()).last_fee_date().setValue(localDate.toString());
                updateLastPaymentDb(localDate.toString(), ed);
            } catch (Exception e) {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid date or format is invalid.\nCorrect format is: dd/MM/yyyy");
                alert.showAndWait();
            }
        });
        for (var keys : data.keySet()) {
            Student student = data.get(keys);
            StudentInfo studentInfo = new StudentInfo(false,
                    student.ed_no(), student.name(),
                    student._class(), student.getDoa(), student.getLastPaymentDate());
            items.add(studentInfo);
        }
        studentData.setItems(filteredData);
    }

    private void updateLastPaymentDb(String last_fee_date, String ed_no) {
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String sql = "UPDATE StudentData SET last_payment_date = ? WHERE ed_no = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, last_fee_date);
            pst.setString(2, ed_no);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateAdmissionDateDb(String date_of_admission, String ed_no) {
        Connection conn = mainController.gradedDataLoader.databaseLoader.getConnection();
        String sql = "UPDATE StudentData SET date_of_add = ? WHERE ed_no = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, date_of_admission);
            pst.setString(2, ed_no);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onDelete() {
        if (!selectedStudent.isEmpty()) {
            var result = showAlert(selectedStudent.getFirst().ed_no().getValue());
            if (result.isPresent() && result.get() == ButtonType.OK) {
                for (var select : selectedStudent) {
                    var x = studentsMap.get(select.ed_no().getValue());
                    mainController.gradedDataLoader.removeStudent(x);
                    mainController.gradedDataLoader.getStudentData().remove(x.ed_no());
                    studentData.getItems().remove(select);
                    items.remove(select);
                }
            }
        }
    }

    private static Optional<ButtonType> showAlert(String ed_no) {
        var alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText("Are you sure ?\nStudent with ED_NO " + ed_no + ".. will be deleted.");
        return alert.showAndWait();
    }

    @FXML
    void onFilterMenu(ActionEvent event) {
        var list = filterMenu.getItems();
        CheckMenuItem checkMenuItem = (CheckMenuItem) event.getSource();
        for (MenuItem m : list) {
            if (((CheckMenuItem) m).isSelected()) {
                ((CheckMenuItem) m).setSelected(false);
            }
        }
        checkMenuItem.setSelected(true);

        filterMenu.setText(checkMenuItem.getText());
    }
}

