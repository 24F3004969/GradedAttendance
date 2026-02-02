package org.graded_classes.graded_attendance.components;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class FilterComboBox extends ComboBox<String> {
    private final ObservableList<String> initialList;
    private ObservableList<String> bufferList = FXCollections.observableArrayList();
    private String previousValue = "";

    public FilterComboBox(ObservableList<String> items) {
        super(items);
        super.setEditable(true);
        this.initialList = items;
        this.configAutoFilterListener();
    }

    private void configAutoFilterListener() {
        final FilterComboBox currentInstance = this;
        currentInstance.setVisibleRowCount(10);
        this.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            previousValue = oldValue;
            final TextField editor = currentInstance.getEditor();
            final String selected = currentInstance.getSelectionModel().getSelectedItem();
            if (selected == null || !selected.equals(editor.getText()))
            {
                filterItems(newValue, currentInstance);
                currentInstance.show();
                if (currentInstance.getItems().size() == 1) {
                    setUserInputToOnlyOption(currentInstance, editor);
                }
            }

        });
    }

    private void filterItems(String filter, ComboBox<String> comboBox) {
        if (filter.contains(previousValue) && !previousValue.isEmpty()) {
            ObservableList<String> filteredList = this.readFromList(filter, bufferList);
            bufferList.clear();
            bufferList = filteredList;
        } else {
            bufferList = this.readFromList(filter, initialList);
        }
        comboBox.setItems(bufferList);
        comboBox.setValue(filter);
    }

    private ObservableList<String> readFromList(String filter, ObservableList<String> originalList) {
        ObservableList<String> filteredList = FXCollections.observableArrayList();
        for (String item : originalList) {
            if (item.toLowerCase().contains(filter.toLowerCase())) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }

    private void setUserInputToOnlyOption(ComboBox<String> currentInstance, final TextField editor) {
      final String onlyOption = currentInstance.getItems().getFirst();
        final String currentText = editor.getText();
        if (onlyOption.length() > currentText.length()) {
            editor.setText(onlyOption);
           Platform.runLater(() -> editor.selectRange(currentText.length(), onlyOption.length()));
        }
    }

}