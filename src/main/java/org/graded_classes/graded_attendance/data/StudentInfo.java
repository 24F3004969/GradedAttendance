package org.graded_classes.graded_attendance.data;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public record StudentInfo(BooleanProperty active, StringProperty ed_no, StringProperty name, StringProperty grade,
                          StringProperty date_of_admission, StringProperty last_fee_date) {


    public StudentInfo(boolean active, String ed_no, String name, String grade,
                       String date_of_admission, String last_fee_date) {
        this(new SimpleBooleanProperty(active), new SimpleStringProperty(ed_no),
                new SimpleStringProperty(name), new SimpleStringProperty(grade),
                new SimpleStringProperty(date_of_admission),
                new SimpleStringProperty(last_fee_date));
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public BooleanProperty activeProperty() {
        return active;
    }


}
