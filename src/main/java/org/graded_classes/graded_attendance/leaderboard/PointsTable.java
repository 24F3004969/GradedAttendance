package org.graded_classes.graded_attendance.leaderboard;

import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class PointsTable implements Initializable {
    @FXML
    private TableView<Map<String, Object>> points_table;
    @FXML
    private TableColumn<Map<String, Object>, String> ed_column, points_column,
            name_column, class_column;
    @FXML
    private TextField filterField, new_ed, new_name, new_class;
    @FXML
    private MenuButton filterMenu;
    StudentDataLoader studentDataLoader;
    ArrayList<CustomView> l1, l2;
    ArrayList<String> sqlQueries = new ArrayList<>();
    ObservableList<Map<String, Object>> items = FXCollections.observableArrayList();
    int max_ed;
    Stage stage2;
    public PointsTable(StudentDataLoader studentDataLoader, ArrayList<CustomView> l1, ArrayList<CustomView> l2, Stage stage2) {
        this.studentDataLoader = studentDataLoader;
        this.l1 = l1;
        this.l2 = l2;
        this.stage2 = stage2;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        name_column.setCellValueFactory(map -> getValues(map, "Name"));
        name_column.setCellFactory(TextFieldTableCell.forTableColumn());
        ed_column.setCellValueFactory(map -> getValues(map, "ED No."));
        class_column.setCellValueFactory(map -> getValues(map, "Class"));
        class_column.setCellFactory(TextFieldTableCell.forTableColumn());
        points_column.setCellValueFactory(map -> getValues(map, "Points"));
        points_column.setCellFactory(TextFieldTableCell.forTableColumn());
        name_column.setOnEditCommit(event -> eventResolver(event, "Name"));
        class_column.setOnEditCommit(event -> eventResolver(event, "Class"));
        points_column.setOnEditCommit(event -> eventResolver(event, "Points"));
        FilteredList<Map<String, Object>> filteredData = new FilteredList<>(items, _ -> true);
        for (var st : studentDataLoader.getStudentList()) {
            Map<String, Object> item1 = getStringObjectMap(st);
            items.add(item1);
        }
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(val -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String upperCase = newValue.toUpperCase();
                return switch (filterMenu.getText()) {
                    case "Class" -> val.get("Class").toString().equals(upperCase);
                    case "ED No." -> val.get("ED No.").toString().trim().contains(upperCase);
                    case "Name" -> val.get("Name").toString().contains(upperCase);
                    default -> false;
                };
            });
        });
        points_table.setItems(filteredData);
        max_ed = Integer.parseInt(studentDataLoader.getStudentList().getLast().id().replace("ED", ""));
        new_ed.setText("ED" + (max_ed + 1));
    }

    private void eventResolver(TableColumn.CellEditEvent<Map<String, Object>, String> event, String key) {
        String listKey = event.getTableView().getItems().get(event.getTablePosition().getRow()).get("ED No.").toString();
        String object = "";
        Student student = studentDataLoader.getStudentLinkedHashMap().get(listKey);
        switch (key) {
            case "Name" -> {
                object = event.getNewValue();
                student.setName(object);
                update("Name", object, listKey);
            }
            case "Class" -> {
                object = event.getNewValue();
                student.setGrade(object);
                update("Class", object, listKey);
            }
            case "Points" -> {
                if (event.getNewValue() == null || event.getNewValue().isEmpty()) {
                    student.setPoints(Double.parseDouble(event.getOldValue()));
                    update("Points", event.getOldValue(), listKey);
                }
                object = new Operators(event.getNewValue()).solve() + "";
                student.setPoints(Double.parseDouble(object));
                update("Points", object, listKey);
            }
            default -> throw new IllegalStateException("Unexpected value: " + key);
        }
        event.getTableView().getItems().get(event.getTablePosition().getRow()).put(key, object);
        var newSortedStudentList = studentDataLoader.getSortedStudentList();
        for (int i = 0; i < 24; i++) {
            if (i < 12) {
                l1.get(i).getText1().setText(newSortedStudentList.get(i).getName());
                l1.get(i).getText2().setText(newSortedStudentList.get(i).getGrade());
                l1.get(i).getText3().setText((int) newSortedStudentList.get(i).points() + "");
            } else {
                l2.get(i - 12).getText1().setText(newSortedStudentList.get(i).getName());
                l2.get(i - 12).getText2().setText(newSortedStudentList.get(i).getGrade());
                l2.get(i - 12).getText3().setText((int) newSortedStudentList.get(i).points() + "");
            }

        }


    }

    @FXML
    private void onApply() {
        for (var sql : sqlQueries) {
            try {
                studentDataLoader.databaseLoader.getStatement().execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
        if (!sqlQueries.isEmpty()) {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Marks got updated");
            alert.show();
        }
        sqlQueries.clear();

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


    private static Map<String, Object> getStringObjectMap(Student st) {
        Map<String, Object> item1 = new HashMap<>();
        item1.put("Name", st.name());
        item1.put("ED No.", st.id());
        item1.put("Class", st.grade());
        item1.put("Points", st.points() + "");
        return item1;
    }

    public ObservableValueBase<String> getValues(TableColumn.CellDataFeatures<Map<String, Object>, String> mapStringCellDataFeatures, String key) {
        return new ObservableValueBase<>() {
            @Override
            public String getValue() {
                return mapStringCellDataFeatures.getValue().get(key).toString();
            }
        };
    }

    public void update(String column_name, String value, String key) {
        String sql = "UPDATE LEADERS SET " + column_name + " = '" + value + "' WHERE [ED No.] = '" + key + "'";
        sqlQueries.add(sql);
    }

    @FXML
    public void addNewStudent() {
        if (!new_name.getText().isEmpty() && !new_class.getText().isEmpty()) {
            String ed = "ED" + (Integer.parseInt(studentDataLoader.getStudentList().getLast().
                    id().replace("ED", "")) + 1);
            updateNewStudent(ed, new_name.getText(), new_class.getText(), 0);
            studentDataLoader.getStudentLinkedHashMap().put("ED" + (max_ed + 1), new Student("ED" + (max_ed + 1), new_name.getText(), new_class.getText(), 0));
            Map<String, Object> item1 = getStringObjectMap(studentDataLoader.getStudentList().getLast());
            items.add(item1);
            max_ed++;
            new_ed.setText("ED" + (max_ed + 1));
            new_name.setText("");
            new_class.setText("");
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("New Student added");
            alert.show();
        }
    }

    public void updateNewStudent(String edNo, String name, String className, int points) {

        String sql = "INSERT INTO LEADERS ([ED No.], Name, Class, Points) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = studentDataLoader.databaseLoader.getConnection().prepareStatement(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            pstmt.setString(1, edNo);
            pstmt.setString(2, name);
            pstmt.setString(3, className);
            pstmt.setInt(4, points);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @FXML
    private void onClose() {
        stage2.close();
    }

    @FXML
    void changeDuration() {
        Stage timerStage = new Stage();
        timerStage.setTitle("Animation Duration");
        try {
            var layout = new FXMLLoader(LeaderboardResourcesLoader.loadURL("fxml/timer.fxml"));
            layout.setControllerFactory(_ -> new Timer(timerStage));
            timerStage.setScene(new Scene(layout.load(),1100,720));
            timerStage.getIcons().add(new Image(Objects.requireNonNull(getClass().
                    getResourceAsStream("icons/__logo.png"))));
            timerStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
