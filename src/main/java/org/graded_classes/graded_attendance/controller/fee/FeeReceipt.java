package org.graded_classes.graded_attendance.controller.fee;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import org.graded_classes.graded_attendance.GradedResourceLoader;
import org.graded_classes.graded_attendance.controller.StudentFeeLayout;
import org.graded_classes.graded_attendance.data.FeeData;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class FeeReceipt implements Initializable {

    @FXML
    private Label amount;

    @FXML
    private Label edNo;

    @FXML
    private Label mode, month_name, nextFeeDate, payDate, transactionNo, dueAmount;
    @FXML
    private Label rec_name;
    @FXML
    private StackPane mainPane;
    @FXML
    private Label name;
    String nameString;
    FeeData feeData;

    public FeeReceipt(String nameString, FeeData feeData) {
        this.feeData = feeData;
        this.nameString = nameString;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("FeeReceipt initializing");
        amount.setText("₹ " + feeData.amount());
        edNo.setText(feeData.edNo().getValue().replace("ED",""));
        mode.setText(feeData.paymentMode().name());
        name.setText(nameString);
        rec_name.setText(feeData.collectedByName().getValue());
        month_name.setText(feeData.month().name());
        nextFeeDate.setText(feeData.nextFeeDate().getValue());
        payDate.setText(LocalDate.now().getDayOfMonth() + " " +
                StudentFeeLayout.format(LocalDate.now().getMonth().toString()) +
                " " + LocalDate.now().getYear());
        transactionNo.setText(feeData.referenceNo().getValue() == null ? "Unknown" : feeData.referenceNo().getValue());
        dueAmount.setText("₹ "+feeData.dueAmount().getValue());
    }

}
