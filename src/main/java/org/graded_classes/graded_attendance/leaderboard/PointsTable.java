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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.GradedFxmlLoader;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.controller.MainController;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class PointsTable implements Initializable {
    @FXML
    private TableView<Map<String, Object>> points_table;
    @FXML
    private TableColumn<Map<String, Object>, String> ed_column, points_column,
            name_column, class_column;
    @FXML
    private TextField filterField;
    @FXML
    private MenuButton filterMenu;
    StudentDataLoader studentDataLoader;
    ArrayList<CustomView> l1customView, l2customView;
    ArrayList<String> sqlQueries = new ArrayList<>();
    ObservableList<Map<String, Object>> items = FXCollections.observableArrayList();
    Stage stage2 = new Stage();
    LeaderBoard2 l2;
    Leaderboard1 l1;
    MainController mainController;

    public PointsTable(StudentDataLoader studentDataLoader, Leaderboard1 l1, LeaderBoard2 l2, MainController mainController) {
        this.studentDataLoader = studentDataLoader;
        this.l1 = l1;
        this.l2 = l2;
        l1customView = l1.customViews;
        l2customView = l2.customViews;
        this.mainController = mainController;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainController.getStage().getScene().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.P) {
                stage2.setTitle("Leaderboard");
                try {
                    stage2.setScene(LeaderboardLoader.load((StackPane) mainController.gradedFxmlLoader.createView(R.leaderboard1),
                            (StackPane) mainController.gradedFxmlLoader.createView(R.leaderboard2)));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                stage2.show();
            }
            stage2.getScene().setOnKeyPressed(event1 -> {
                stage2.setFullScreen(event1.getCode() == KeyCode.F11 && !mainController.getStage().isFullScreen());

            });

        });
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

    }

    private void eventResolver(TableColumn.CellEditEvent<Map<String, Object>, String> event, String key) {
        String listKey = event.getTableView().getItems().get(event.getTablePosition().getRow()).get("ED No.").toString();
        String object = "";
        StudentScore studentScore = studentDataLoader.getStudentLinkedHashMap().get(listKey);
        switch (key) {
           /* case "Name" -> {
                object = event.getNewValue();
                studentScore.setName(object);
                update("Name", object, listKey);
            }
            case "Class" -> {
                object = event.getNewValue();
                studentScore.setGrade(object);
                update("Class", object, listKey);
            }*/
            case "Points" -> {
                if (event.getNewValue() == null || event.getNewValue().isEmpty()) {
                    studentScore.setPoints(Double.parseDouble(event.getOldValue()));
                    update(event.getOldValue(), listKey);
                }
                object = new Operators(event.getNewValue()).solve() + "";
                studentScore.setPoints(Double.parseDouble(object));
                update( object, listKey);
            }
            default -> throw new IllegalStateException("Unexpected value: " + key);
        }
        event.getTableView().getItems().get(event.getTablePosition().getRow()).put(key, object);
        var newSortedStudentList = studentDataLoader.getSortedStudentList();
        for (int i = 0; i < 24; i++) {
            if (i < 12) {
                l1customView.get(i).getText1().setText(newSortedStudentList.get(i).getName());
                l1customView.get(i).getText2().setText(newSortedStudentList.get(i).getGrade());
                l1customView.get(i).getText3().setText((int) newSortedStudentList.get(i).points() + "");
            } else {
                l2customView.get(i - 12).getText1().setText(newSortedStudentList.get(i).getName());
                l2customView.get(i - 12).getText2().setText(newSortedStudentList.get(i).getGrade());
                l2customView.get(i - 12).getText3().setText((int) newSortedStudentList.get(i).points() + "");
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


    private static Map<String, Object> getStringObjectMap(StudentScore st) {
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

    public void update(String value, String key) {
        String sql = """
                UPDATE StudentData SET points = %s  WHERE ed_no =%s
                """.formatted(value, key);
        sqlQueries.add(sql);
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
            timerStage.setScene(new Scene(layout.load(), 1100, 720));
            timerStage.getIcons().add(new Image(Objects.requireNonNull(getClass().
                    getResourceAsStream("icons/__logo.png"))));
            timerStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
