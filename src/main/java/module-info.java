module org.graded_classes.graded_attendance {
    requires javafx.fxml;
    requires telegrambots.meta;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j.nop;
    requires java.sql;
    requires atlantafx.base;
    requires com.twelvemonkeys.common.image;
    requires org.apache.xmlgraphics.batik.transcoder;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires com.calendarfx.view;
    requires telegrambots.client;
    requires telegrambots.longpolling;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.material2;
    requires java.desktop;
    requires com.gluonhq.emoji;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.jetbrains.annotations;
    requires org.checkerframework.checker.qual;
    requires fxgraphics2d;
    requires jlatexmath;
    requires com.dlsc.fxmlkit;


    opens org.graded_classes.graded_attendance.controller to javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc, java.sql;
    opens org.graded_classes.graded_attendance.planner to javafx.fxml;
    opens org.graded_classes.graded_attendance.data to java.sql, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.messaging;
    exports org.graded_classes.graded_attendance;
    opens org.graded_classes.graded_attendance;
    exports org.graded_classes.graded_attendance.calender;
    opens org.graded_classes.graded_attendance.calender;
    exports org.graded_classes.graded_attendance.components;
    opens org.graded_classes.graded_attendance.components;
    exports org.graded_classes.graded_attendance.test;
    opens org.graded_classes.graded_attendance.test;
}