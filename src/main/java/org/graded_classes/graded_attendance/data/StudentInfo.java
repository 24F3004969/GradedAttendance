package org.graded_classes.graded_attendance.data;

import javafx.beans.property.*;


public record StudentInfo(IntegerProperty sno, BooleanProperty active, StringProperty ed_no, StringProperty name,
                          StringProperty grade,
                          StringProperty date_of_admission, StringProperty last_fee_date, SimpleStringProperty fee,
                          SimpleStringProperty subjects, SimpleStringProperty board) {


    public StudentInfo(int sno, boolean active, String ed_no, String name, String grade,
                       String date_of_admission, String last_fee_date, String fee, String subjects, String board) {
        this(new SimpleIntegerProperty(sno), new SimpleBooleanProperty(active), new SimpleStringProperty(ed_no),
                new SimpleStringProperty(name), new SimpleStringProperty(grade),
                new SimpleStringProperty(date_of_admission),
                new SimpleStringProperty(last_fee_date), new SimpleStringProperty(fee), new SimpleStringProperty(subjects), new SimpleStringProperty(board));
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public BooleanProperty activeProperty() {
        return active;
    }


}
