package org.graded_classes.graded_attendance.data;

import javafx.beans.property.SimpleStringProperty;

public record FeeData(
        StringBuilder s_no,
        Integer paymentId,
        SimpleStringProperty edNo,
        SimpleStringProperty name,
        MonthAbbrev month,
        double amount,
        SimpleStringProperty paidOn,
        SimpleStringProperty nextFeeDate,
        SimpleStringProperty collectedByName,
        PaymentMode paymentMode,
        Gateway gateway,
        SimpleStringProperty referenceNo,
        SimpleStringProperty dueAmount,
        SimpleStringProperty phone_no
) {
    public FeeData(StringBuilder s_no, Integer paymentId,
                   String edNo,
                   String name,
                   MonthAbbrev month,
                   double amount,
                   String paidOn,
                   String nextFeeDate,
                   String collectedByName,
                   PaymentMode paymentMode,
                   Gateway gateway,
                   String referenceNo,
                   String dueAmount,StringBuilder phone_no) {
        this(s_no, paymentId, new SimpleStringProperty(edNo), new SimpleStringProperty(name), month, amount, new SimpleStringProperty(paidOn),
                new SimpleStringProperty(nextFeeDate), new SimpleStringProperty(collectedByName),
                paymentMode, gateway, new SimpleStringProperty(referenceNo), new SimpleStringProperty(dueAmount),
                new SimpleStringProperty(phone_no.toString()));
    }

    public enum MonthAbbrev {
        Jan, Feb, Mar, Apr, May, Jun,
        Jul, Aug, Sep, Oct, Nov, Dec
    }

    public enum PaymentMode {
        Online, Offline
    }

    public enum Gateway {
        UPI, Card, NetBanking, Cash, Cheque
    }

}