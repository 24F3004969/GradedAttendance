package org.graded_classes.graded_attendance.controller.quiz;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeItem;

public class ContextTreeItem extends TreeItem<String> {
    private final ContextMenu contextMenu;

    public ContextTreeItem(String value, ContextMenu menu) {
        super(value);
        this.contextMenu = menu;
    }

    public ContextMenu getContextMenu() {
        return this.contextMenu;
    }
}
