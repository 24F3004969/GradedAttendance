package org.graded_classes.graded_attendance.data;
public record FeeData(
        Integer paymentId,
        String edNo,
        MonthAbbrev month,
        double amount,
        String paidOn,
        String nextFeeDate,
        String collectedByName,
        PaymentMode paymentMode,
        Gateway gateway,
        String referenceNo,
        String dueAmount
) {

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