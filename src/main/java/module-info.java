module org.graded_classes.graded_attendance {
    requires javafx.fxml;
    requires telegrambots.meta;


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
    requires org.checkerframework.checker.qual;
    requires fxgraphics2d;
    requires jlatexmath;
    requires javafx.media;
    requires javafx.swing;
    requires com.dlsc.pdfviewfx;
    requires org.apache.pdfbox;
    requires com.dlsc.fxmlkit;
    requires com.dlsc.gemsfx;
    requires com.lottie4j.core;
    requires com.lottie4j.fxplayer;
    requires org.kordamp.ikonli.bootstrapicons;
    requires annotations;
    requires org.bytedeco.opencv;
    requires sherpa.onnx.java.api;
    
    opens org.graded_classes.graded_attendance.controller.leaderboard to javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc, java.sql;

    opens org.graded_classes.graded_attendance.controller.planner to javafx.fxml;
    opens org.graded_classes.graded_attendance.controller.fee to javafx.fxml,java.sql;
    opens org.graded_classes.graded_attendance.messaging;
    exports org.graded_classes.graded_attendance.controller.home;
    exports org.graded_classes.graded_attendance;
    opens org.graded_classes.graded_attendance;
    exports org.graded_classes.graded_attendance.calender;
    opens org.graded_classes.graded_attendance.calender;
    exports org.graded_classes.graded_attendance.components;
    opens org.graded_classes.graded_attendance.components;
    opens org.graded_classes.graded_attendance.controller.quiz to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.data to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.controller.chat to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.controller.dashboard to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.controller.database to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    opens org.graded_classes.graded_attendance.controller.home to java.sql, javafx.fxml, javafx.graphics, org.xerial.sqlitejdbc;
    exports org.graded_classes.graded_attendance.controller.camera;
    opens org.graded_classes.graded_attendance.controller.camera;
}