package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class OMRReader {

    // ================================
    // MUST MATCH OMRGeneratorApp
    // ================================

    private static final double MARKER_SIZE = 40;

    private static final double MARKER_LEFT = 30;
    private static final double MARKER_TOP = 30;
    private static final double MARKER_RIGHT = 730;
    private static final double MARKER_BOTTOM = 1130;

    // OpenCV detects marker center, not top-left
    private static final double MARKER_CENTER_LEFT =
            MARKER_LEFT + MARKER_SIZE / 2.0;

    private static final double MARKER_CENTER_TOP =
            MARKER_TOP + MARKER_SIZE / 2.0;

    private static final double MARKER_CENTER_RIGHT =
            MARKER_RIGHT + MARKER_SIZE / 2.0;

    private static final double MARKER_CENTER_BOTTOM =
            MARKER_BOTTOM + MARKER_SIZE / 2.0;

    private static final double MARKER_WIDTH =
            MARKER_CENTER_RIGHT - MARKER_CENTER_LEFT;

    private static final double MARKER_HEIGHT =
            MARKER_CENTER_BOTTOM - MARKER_CENTER_TOP;

    private static final int QUESTIONS = 70;
    private static final int QUESTIONS_PER_COLUMN = 35;

    private static final double LEFT_COLUMN_X = 90;
    private static final double RIGHT_COLUMN_X = 420;

    private static final double FIRST_ROW_Y = 250;
    private static final double QUESTION_GAP_Y = 22;

    private static final double QUESTION_NUMBER_GAP = 45;
    private static final double OPTION_GAP_X = 50;

    private static final int ANSWER_RADIUS = 12;

    // Set true only after you add Roll No OMR bubbles in generator
    private static final boolean ENABLE_ROLL_OMR = false;

    // Roll number OMR constants
    private static final double ROLL_DIGIT_1_X = 180;
    private static final double ROLL_DIGIT_2_X = 290;

    private static final double ROLL_FIRST_DIGIT_Y = 220;
    private static final double ROLL_DIGIT_GAP_Y = 18;

    private static final int ROLL_BUBBLE_RADIUS = 12;

    public static void main(String[] args) {

        Mat image = imread("omr.png");

        if (image.empty()) {
            throw new RuntimeException("Cannot load image");
        }

        Mat debug = image.clone();

        Mat gray = new Mat();

        cvtColor(
                image,
                gray,
                COLOR_BGR2GRAY
        );

        Mat binary = makeBinary(gray);

        imwrite("binary_debug.png", binary);

        SheetBounds bounds =
                findMarkers(binary);

        System.out.println("Markers:");
        System.out.println(bounds);

        double scaleX =
                (bounds.right - bounds.left) / MARKER_WIDTH;

        double scaleY =
                (bounds.bottom - bounds.top) / MARKER_HEIGHT;

        if (ENABLE_ROLL_OMR) {

            String rollNo =
                    readRollNumber(
                            binary,
                            debug,
                            bounds,
                            scaleX,
                            scaleY
                    );

            System.out.println("Roll No -> " + rollNo);
        } else {
            System.out.println("Roll No OMR disabled. Current sheet has only text roll number field.");
        }

        for (int q = 1; q <= QUESTIONS; q++) {

            char answer =
                    readQuestion(
                            q,
                            binary,
                            debug,
                            bounds,
                            scaleX,
                            scaleY
                    );

            System.out.printf(
                    "Q%-2d -> %s%n",
                    q,
                    answer
            );
        }

        imwrite("debug.png", debug);

        System.out.println("Debug image saved as debug.png");
        System.out.println("Binary debug image saved as binary_debug.png");
    }

    private static Mat makeBinary(Mat gray) {

        Mat blurred = new Mat();

        GaussianBlur(
                gray,
                blurred,
                new Size(3, 3),
                0
        );

        Mat binary = new Mat();

        adaptiveThreshold(
                blurred,
                binary,
                255,
                ADAPTIVE_THRESH_GAUSSIAN_C,
                THRESH_BINARY_INV,
                31,
                10
        );

        Mat kernel =
                getStructuringElement(
                        MORPH_RECT,
                        new Size(3, 3)
                );

        morphologyEx(
                binary,
                binary,
                MORPH_CLOSE,
                kernel
        );

        return binary;
    }

    private static char readQuestion(
            int question,
            Mat binary,
            Mat debug,
            SheetBounds bounds,
            double scaleX,
            double scaleY
    ) {

        int[] values = new int[4];

        boolean rightColumn =
                question > QUESTIONS_PER_COLUMN;

        int row;
        double columnX;

        if (rightColumn) {
            row = question - QUESTIONS_PER_COLUMN - 1;
            columnX = RIGHT_COLUMN_X;
        } else {
            row = question - 1;
            columnX = LEFT_COLUMN_X;
        }

        double templateY =
                FIRST_ROW_Y + row * QUESTION_GAP_Y;

        for (int option = 0; option < 4; option++) {

            double templateX =
                    columnX
                            + QUESTION_NUMBER_GAP
                            + option * OPTION_GAP_X;

            int actualX =
                    convertTemplateX(
                            templateX,
                            bounds,
                            scaleX
                    );

            int actualY =
                    convertTemplateY(
                            templateY,
                            bounds,
                            scaleY
                    );

            values[option] =
                    bubbleScore(
                            binary,
                            actualX,
                            actualY,
                            ANSWER_RADIUS
                    );

            Scalar color;

            switch (option) {
                case 0 ->
                        color = new Scalar(255, 0, 0, 0);      // A blue
                case 1 ->
                        color = new Scalar(0, 255, 0, 0);      // B green
                case 2 ->
                        color = new Scalar(0, 0, 255, 0);      // C red
                default ->
                        color = new Scalar(255, 0, 255, 0);    // D purple
            }

            circle(
                    debug,
                    new Point(actualX, actualY),
                    6,
                    color,
                    2,
                    LINE_8,
                    0
            );
        }

        System.out.printf(
                "Q%d scores -> A=%d B=%d C=%d D=%d%n",
                question,
                values[0],
                values[1],
                values[2],
                values[3]
        );

        int maxIndex = 0;

        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxIndex]) {
                maxIndex = i;
            }
        }

        int secondIndex = -1;

        for (int i = 0; i < values.length; i++) {

            if (i == maxIndex) {
                continue;
            }

            if (secondIndex == -1 ||
                    values[i] > values[secondIndex]) {
                secondIndex = i;
            }
        }

        int maxValue = values[maxIndex];
        int secondValue = secondIndex == -1 ? 0 : values[secondIndex];

        // Blank detection
        if (maxValue < 80) {
            return '-';
        }

        // Multiple or unclear marking detection
        if (secondValue > 0 && maxValue < secondValue * 1.4) {
            return 'X';
        }

        return (char) ('A' + maxIndex);
    }

    private static String readRollNumber(
            Mat binary,
            Mat debug,
            SheetBounds bounds,
            double scaleX,
            double scaleY
    ) {

        int firstDigit =
                readRollDigit(
                        binary,
                        debug,
                        bounds,
                        scaleX,
                        scaleY,
                        ROLL_DIGIT_1_X
                );

        int secondDigit =
                readRollDigit(
                        binary,
                        debug,
                        bounds,
                        scaleX,
                        scaleY,
                        ROLL_DIGIT_2_X
                );

        if (firstDigit == -1 || secondDigit == -1) {
            return "INVALID_ROLL";
        }

        return "ED" + firstDigit + secondDigit;
    }

    private static int readRollDigit(
            Mat binary,
            Mat debug,
            SheetBounds bounds,
            double scaleX,
            double scaleY,
            double digitColumnX
    ) {

        int[] values = new int[10];

        for (int digit = 0; digit <= 9; digit++) {

            double templateX =
                    digitColumnX;

            double templateY =
                    ROLL_FIRST_DIGIT_Y
                            + digit * ROLL_DIGIT_GAP_Y;

            int actualX =
                    convertTemplateX(
                            templateX,
                            bounds,
                            scaleX
                    );

            int actualY =
                    convertTemplateY(
                            templateY,
                            bounds,
                            scaleY
                    );

            values[digit] =
                    bubbleScore(
                            binary,
                            actualX,
                            actualY,
                            ROLL_BUBBLE_RADIUS
                    );

            circle(
                    debug,
                    new Point(actualX, actualY),
                    5,
                    new Scalar(255, 150, 0, 0),
                    2,
                    LINE_8,
                    0
            );
        }

        int maxDigit = 0;

        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxDigit]) {
                maxDigit = i;
            }
        }

        System.out.print("Roll digit scores -> ");

        for (int i = 0; i < values.length; i++) {
            System.out.print(i + "=" + values[i] + " ");
        }

        System.out.println();

        if (values[maxDigit] < 80) {
            return -1;
        }

        return maxDigit;
    }

    private static int convertTemplateX(
            double templateX,
            SheetBounds bounds,
            double scaleX
    ) {

        return (int) Math.round(
                bounds.left
                        + (templateX - MARKER_CENTER_LEFT)
                        * scaleX
        );
    }

    private static int convertTemplateY(
            double templateY,
            SheetBounds bounds,
            double scaleY
    ) {

        return (int) Math.round(
                bounds.top
                        + (templateY - MARKER_CENTER_TOP)
                        * scaleY
        );
    }

    private static int bubbleScore(
            Mat binary,
            int centerX,
            int centerY,
            int radius
    ) {

        int x =
                Math.max(
                        centerX - radius,
                        0
                );

        int y =
                Math.max(
                        centerY - radius,
                        0
                );

        int width =
                Math.min(
                        radius * 2,
                        binary.cols() - x
                );

        int height =
                Math.min(
                        radius * 2,
                        binary.rows() - y
                );

        if (width <= 0 || height <= 0) {
            return 0;
        }

        Rect roi =
                new Rect(
                        x,
                        y,
                        width,
                        height
                );

        Mat bubble =
                new Mat(binary, roi);

        return countNonZero(bubble);
    }

    private static SheetBounds findMarkers(Mat binary) {

        int imageWidth = binary.cols();
        int imageHeight = binary.rows();

        int roiW = (int) (imageWidth * 0.30);
        int roiH = (int) (imageHeight * 0.30);

        Point2d topLeft =
                findMarkerInCorner(
                        binary,
                        new Rect(
                                0,
                                0,
                                roiW,
                                roiH
                        ),
                        "TOP_LEFT"
                );

        Point2d topRight =
                findMarkerInCorner(
                        binary,
                        new Rect(
                                imageWidth - roiW,
                                0,
                                roiW,
                                roiH
                        ),
                        "TOP_RIGHT"
                );

        Point2d bottomLeft =
                findMarkerInCorner(
                        binary,
                        new Rect(
                                0,
                                imageHeight - roiH,
                                roiW,
                                roiH
                        ),
                        "BOTTOM_LEFT"
                );

        Point2d bottomRight =
                findMarkerInCorner(
                        binary,
                        new Rect(
                                imageWidth - roiW,
                                imageHeight - roiH,
                                roiW,
                                roiH
                        ),
                        "BOTTOM_RIGHT"
                );

        double left =
                Math.min(topLeft.x(), bottomLeft.x());

        double right =
                Math.max(topRight.x(), bottomRight.x());

        double top =
                Math.min(topLeft.y(), topRight.y());

        double bottom =
                Math.max(bottomLeft.y(), bottomRight.y());

        return new SheetBounds(
                left,
                right,
                top,
                bottom
        );
    }

    private static Point2d findMarkerInCorner(
            Mat binary,
            Rect searchArea,
            String label
    ) {

        Mat roi =
                new Mat(binary, searchArea);

        MatVector contours =
                new MatVector();

        findContours(
                roi.clone(),
                contours,
                RETR_LIST,
                CHAIN_APPROX_SIMPLE
        );

        List<Rect> candidates =
                new ArrayList<>();

        double roiArea =
                searchArea.width() * searchArea.height();

        for (long i = 0; i < contours.size(); i++) {

            Rect r =
                    boundingRect(
                            contours.get(i)
                    );

            int w = r.width();
            int h = r.height();

            if (w < 20 || h < 20) {
                continue;
            }

            double area =
                    w * h;

            if (area > roiArea * 0.50) {
                continue;
            }

            double ratio =
                    (double) w / h;

            if (ratio < 0.55 || ratio > 1.45) {
                continue;
            }

            candidates.add(r);
        }

        candidates.sort(
                Comparator.comparingInt(
                        (Rect r) -> r.width() * r.height()
                ).reversed()
        );

        if (candidates.isEmpty()) {
            throw new RuntimeException(
                    "Marker not found in " + label +
                            ". Check binary_debug.png"
            );
        }

        Rect best =
                candidates.get(0);

        double centerX =
                searchArea.x()
                        + best.x()
                        + best.width() / 2.0;

        double centerY =
                searchArea.y()
                        + best.y()
                        + best.height() / 2.0;

        System.out.printf(
                "%s marker -> %.1f %.1f size=%dx%d%n",
                label,
                centerX,
                centerY,
                best.width(),
                best.height()
        );

        return new Point2d(centerX, centerY);
    }

    static class SheetBounds {

        double left;
        double right;
        double top;
        double bottom;

        SheetBounds(
                double left,
                double right,
                double top,
                double bottom
        ) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }

        @Override
        public String toString() {
            return "SheetBounds{" +
                    "left=" + left +
                    ", right=" + right +
                    ", top=" + top +
                    ", bottom=" + bottom +
                    '}';
        }
    }
}