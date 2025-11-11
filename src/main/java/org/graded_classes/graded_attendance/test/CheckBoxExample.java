package org.graded_classes.graded_attendance.test;


import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;

public class CheckBoxExample extends Application {
    class BooleanCell extends TableCell<TableData, Boolean> {
        private CheckBox checkBox;

        public BooleanCell() {
            checkBox = new CheckBox();
            checkBox.setDisable(true);
            checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (isEditing())
                        commitEdit(newValue == null ? false : newValue);
                }
            });
            this.setGraphic(checkBox);
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            this.setEditable(true);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            checkBox.setDisable(true);
        }

        public void commitEdit(Boolean value) {
            super.commitEdit(value);

            checkBox.setDisable(true);
        }

        @Override
        public void startEdit() {
            super.startEdit();
            if (isEmpty()) {
                return;
            }
            checkBox.setDisable(false);
            checkBox.requestFocus();
        }

        @Override
        public void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEmpty()) {
                checkBox.setSelected(item);
            }
        }
    }

    // Pojo class. A Javabean
    public class TableData {
        SimpleBooleanProperty favorite;

        SimpleStringProperty stooge;

        // A javabean typically has a zero arg constructor
        // https://docs.oracle.com/javase/tutorial/javabeans/
        public TableData() {
        }

        // but can have others also
        public TableData(String stoogeIn, Boolean favoriteIn) {
            stooge = new SimpleStringProperty(stoogeIn);
            favorite = new SimpleBooleanProperty(favoriteIn);
        }

        /**
         * @return the stooge
         */
        public String getStooge() {
            return stooge.get();
        }

        /**
         * @return the favorite
         */
        public Boolean isFavorite() {
            return favorite.get();
        }

        /**
         * @param favorite the favorite to set
         */
        public void setFavorite(Boolean favorite) {
            this.favorite.setValue(favorite);
        }

        /**
         * @param stooge the stooge to set
         */
        public void setStooge(String stooge) {
            this.stooge.setValue(stooge);
        }
    }

    // Model class - The model in mvc
    // Typically a representation of a database or nosql source
    public class TableModel {
        ObservableList<TableData> stooges = FXCollections.observableArrayList();

        public TableModel() {
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", true));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
            stooges.add(new TableData("Larry", false));
            stooges.add(new TableData("Moe", false));
            stooges.add(new TableData("Curly", false));
        }

        public String displayModel() {
            StringBuilder sb = new StringBuilder();
            for (TableData stooge : stooges) {
                sb.append(stooge.getStooge() + "=" + stooge.isFavorite() + "|");
            }
            return sb.toString();
        }

        /**
         * @return the stooges
         */
        public ObservableList<TableData> getStooges() {
            return stooges;
        }

        public void updateStooge(TableData dataIn) {
            int index = stooges.indexOf(dataIn);
            stooges.set(index, dataIn);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private TableModel model;

    private TableModel getModel() {
        if (model == null) {
            model = new TableModel();
        }
        return model;

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        final VBox view = new VBox(10);
        final TableView<TableData> table = new TableView<>();
        final ObservableList<TableColumn<TableData, ?>> columns = table.getColumns();
        final TableModel model = getModel();

        final TableColumn<TableData, String> stoogeColumn = new TableColumn<>("Stooge");
        stoogeColumn.setCellValueFactory(new PropertyValueFactory<>("stooge"));
        columns.add(stoogeColumn);

        final Button showModelButton = new Button("Show me the Model, woo,woo,woo");
        final Label showModelLabel = new Label("Model?  Whats that?");
        showModelButton.setOnAction(event -> showModelLabel.setText(model.displayModel()));


        final TableColumn<TableData, CheckBox> favoriteColumn = getTableDataCheckBoxTableColumn(model);
        columns.add(favoriteColumn);
        table.setItems(model.getStooges());
        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(showModelButton, showModelLabel);
        view.getChildren().add(hbox);
        view.getChildren().add(table);

        Scene scene = new Scene(view, 640, 380);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private @NotNull TableColumn<TableData, CheckBox> getTableDataCheckBoxTableColumn(TableModel model) {
        final TableColumn<TableData, CheckBox> favoriteColumn = new TableColumn<>("Favorite");
        favoriteColumn.setCellValueFactory(
                arg0 -> {
                    TableData data = arg0.getValue();
                    CheckBox checkBox = new CheckBox();
                    checkBox.setOnMouseClicked(event -> {
                        System.out.println(checkBox.isSelected());
                    });
                    checkBox.selectedProperty().setValue(data.isFavorite());
                    checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                        data.setFavorite(new_val);
                        checkBox.setSelected(new_val);
                        model.updateStooge(data);
                    });

                    return new SimpleObjectProperty<>(checkBox);
                });
        return favoriteColumn;
    }

}
