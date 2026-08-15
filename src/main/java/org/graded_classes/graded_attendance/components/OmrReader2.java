package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.graded_classes.graded_attendance.components.OMRReader.findMarkerInCorner;
import static org.graded_classes.graded_attendance.components.OMRReader.makeBinary;

public class OmrReader2 {
    static Mat debug;
    public static void main(String[] args) {

        Mat input = imread("omr.png");

        if (input.empty()) {
            throw new RuntimeException("Cannot load image");
        }

        // First pass: find registration markers.
        Mat inputGray = new Mat();
        cvtColor(input, inputGray, COLOR_BGR2GRAY);

        Mat inputBinary = makeBinary(inputGray);

        SheetMarkers markers =
                findMarkers(inputBinary);

        // Normalize rotation and perspective.
        Mat normalized = normalizeSheet(
                input,
                markers.topLeft,
                markers.topRight,
                markers.bottomRight,
                markers.bottomLeft
        );

        // Second pass: detect bubbles on normalized sheet.
        Mat gray = new Mat();
        cvtColor(normalized, gray, COLOR_BGR2GRAY);

        Mat binary = makeBinary(gray);
        debug = normalized.clone();
        List<Bubble> bubbles =
                detectBubbles(binary);

        double medianDiameter =
                medianBubbleDiameter(bubbles);

        double rowTolerance =
                medianDiameter * 0.60;

        double divider =
                findColumnDivider(bubbles);

        List<Bubble> leftBubbleList = bubbles.stream()
                .filter(b -> b.centerX() < divider)
                .collect(java.util.stream.Collectors.toList());

        List<Bubble> rightBubbleList = bubbles.stream()
                .filter(b -> b.centerX() > divider)
                .collect(java.util.stream.Collectors.toList());

        List<List<Bubble>> leftRows =
                groupIntoRows(leftBubbleList, rowTolerance);

        List<List<Bubble>> rightRows =
                groupIntoRows(rightBubbleList, rowTolerance);

        leftRows.removeIf(row -> row.size() != 4);
        rightRows.removeIf(row -> row.size() != 4);

        int questionNumber = 1;

        for (List<Bubble> row : leftRows) {

            char answer = readDetectedRow(
                    binary,
                    debug,
                    row,
                    questionNumber
            );

            System.out.printf(
                    "Q%d -> %s%n",
                    questionNumber,
                    answer
            );

            questionNumber++;
        }

        for (List<Bubble> row : rightRows) {

            char answer = readDetectedRow(
                    binary,
                    debug,
                    row,
                    questionNumber
            );

            System.out.printf(
                    "Q%d -> %s%n",
                    questionNumber,
                    answer
            );

            questionNumber++;
        }
        imwrite(
                "normalized_debug.png",
                debug
        );

        imwrite(
                "normalized_binary.png",
                binary
        );

        imwrite(
                "normalized_original.png",
                normalized
        );

        System.out.println(
                "Debug image saved as normalized_debug.png"
        );

        System.out.println(
                "Binary image saved as normalized_binary.png"
        );
    }
    private static Mat normalizeSheet(
            Mat image,
            Point2d topLeft,
            Point2d topRight,
            Point2d bottomRight,
            Point2d bottomLeft
    ) {
        int targetWidth = 800;
        int targetHeight = 1200;

        Point2f srcPoints = new Point2f(4);
        srcPoints.position(0).x((float) topLeft.x()).y((float) topLeft.y());
        srcPoints.position(1).x((float) topRight.x()).y((float) topRight.y());
        srcPoints.position(2).x((float) bottomRight.x()).y((float) bottomRight.y());
        srcPoints.position(3).x((float) bottomLeft.x()).y((float) bottomLeft.y());

        Point2f dstPoints = new Point2f(4);
        dstPoints.position(0).x(0).y(0);
        dstPoints.position(1).x(targetWidth - 1).y(0);
        dstPoints.position(2).x(targetWidth - 1).y(targetHeight - 1);
        dstPoints.position(3).x(0).y(targetHeight - 1);

        srcPoints.position(0);
        dstPoints.position(0);

        Mat transform = getPerspectiveTransform(srcPoints, dstPoints);

        Mat normalized = new Mat();

        warpPerspective(
                image,
                normalized,
                transform,
                new Size(targetWidth, targetHeight),
                INTER_LINEAR,
                BORDER_CONSTANT,
                new Scalar(255, 255, 255, 0)
        );

        return normalized;
    }
    static class SheetMarkers {

        final Point2d topLeft;
        final Point2d topRight;
        final Point2d bottomLeft;
        final Point2d bottomRight;

        SheetMarkers(
                Point2d topLeft,
                Point2d topRight,
                Point2d bottomLeft,
                Point2d bottomRight
        ) {
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
        }
    }
    private static SheetMarkers findMarkers(Mat binary) {

        int imageWidth = binary.cols();
        int imageHeight = binary.rows();

        int roiW = (int) (imageWidth * 0.30);
        int roiH = (int) (imageHeight * 0.30);

        Point2d topLeft = findMarkerInCorner(
                binary,
                new Rect(0, 0, roiW, roiH),
                "TOP_LEFT"
        );

        Point2d topRight = findMarkerInCorner(
                binary,
                new Rect(imageWidth - roiW, 0, roiW, roiH),
                "TOP_RIGHT"
        );

        Point2d bottomLeft = findMarkerInCorner(
                binary,
                new Rect(0, imageHeight - roiH, roiW, roiH),
                "BOTTOM_LEFT"
        );

        Point2d bottomRight = findMarkerInCorner(
                binary,
                new Rect(
                        imageWidth - roiW,
                        imageHeight - roiH,
                        roiW,
                        roiH
                ),
                "BOTTOM_RIGHT"
        );

        return new SheetMarkers(
                topLeft,
                topRight,
                bottomLeft,
                bottomRight
        );
    }
    static class Bubble {

        final int x;
        final int y;
        final int width;
        final int height;

        Bubble(Rect rect) {
            this.x = rect.x();
            this.y = rect.y();
            this.width = rect.width();
            this.height = rect.height();
        }

        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        double area() {
            return width * height;
        }

        @Override
        public String toString() {
            return "Bubble{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
    private static List<Bubble> detectBubbles(Mat binary) {

        MatVector contours = new MatVector();

        findContours(
                binary.clone(),
                contours,
                RETR_EXTERNAL,
                CHAIN_APPROX_SIMPLE
        );

        List<Bubble> candidates = new ArrayList<>();

        double imageArea = binary.cols() * binary.rows();

        for (long i = 0; i < contours.size(); i++) {

            Mat contour = contours.get(i);
            Rect rect = boundingRect(contour);

            int width = rect.width();
            int height = rect.height();

            if (width <= 0 || height <= 0) {
                continue;
            }

            double aspectRatio = (double) width / height;
            double relativeArea = (double) (width * height) / imageArea;

            // Relative values are more adaptable than fixed pixel sizes.
            if (relativeArea < 0.00008 || relativeArea > 0.002) {
                continue;
            }

            if (aspectRatio < 0.70 || aspectRatio > 1.30) {
                continue;
            }

            double contourAreaValue = contourArea(contour);
            double boundingArea = width * height;

            if (boundingArea <= 0) {
                continue;
            }

            double extent = contourAreaValue / boundingArea;

            if (extent < 0.30 || extent > 0.95) {
                continue;
            }

            candidates.add(new Bubble(rect));
        }

        return removeNestedOrDuplicateBubbles(candidates);
    }
    private static List<Bubble> removeNestedOrDuplicateBubbles(
            List<Bubble> candidates
    ) {
        candidates.sort(
                Comparator.comparingDouble(Bubble::area).reversed()
        );

        List<Bubble> result = new ArrayList<>();

        for (Bubble candidate : candidates) {

            boolean duplicate = false;

            for (Bubble accepted : result) {

                double dx = candidate.centerX() - accepted.centerX();
                double dy = candidate.centerY() - accepted.centerY();

                double distance = Math.sqrt(dx * dx + dy * dy);

                double tolerance =
                        Math.min(
                                accepted.width,
                                accepted.height
                        ) * 0.40;

                if (distance < tolerance) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                result.add(candidate);
            }
        }

        return result;
    }
    private static double medianBubbleDiameter(List<Bubble> bubbles) {

        if (bubbles.isEmpty()) {
            throw new RuntimeException("No bubbles detected");
        }

        List<Double> diameters = new ArrayList<>();

        for (Bubble bubble : bubbles) {
            diameters.add(
                    (bubble.width + bubble.height) / 2.0
            );
        }

        diameters.sort(Double::compareTo);

        int middle = diameters.size() / 2;

        if (diameters.size() % 2 == 1) {
            return diameters.get(middle);
        }

        return (
                diameters.get(middle - 1)
                        + diameters.get(middle)
        ) / 2.0;
    }
    private static List<List<Bubble>> groupIntoRows(
            List<Bubble> bubbles,
            double rowTolerance
    ) {
        bubbles.sort(
                Comparator
                        .comparingInt(Bubble::centerY)
                        .thenComparingInt(Bubble::centerX)
        );

        List<List<Bubble>> rows = new ArrayList<>();

        for (Bubble bubble : bubbles) {

            List<Bubble> matchingRow = null;

            for (List<Bubble> row : rows) {

                double averageY = row.stream()
                        .mapToInt(Bubble::centerY)
                        .average()
                        .orElse(0);

                if (Math.abs(bubble.centerY() - averageY) <= rowTolerance) {
                    matchingRow = row;
                    break;
                }
            }

            if (matchingRow == null) {
                matchingRow = new ArrayList<>();
                rows.add(matchingRow);
            }

            matchingRow.add(bubble);
        }

        for (List<Bubble> row : rows) {
            row.sort(Comparator.comparingInt(Bubble::centerX));
        }

        rows.sort(
                Comparator.comparingDouble(
                        row -> row.stream()
                                .mapToInt(Bubble::centerY)
                                .average()
                                .orElse(0)
                )
        );

        return rows;
    }
    private static List<List<Bubble>> keepAnswerRows(
            List<List<Bubble>> detectedRows
    ) {
        List<List<Bubble>> answerGroups = new ArrayList<>();

        for (List<Bubble> row : detectedRows) {

            if (row.size() == 4) {
                answerGroups.add(new ArrayList<>(row));
            } else if (row.size() == 8) {
                answerGroups.add(
                        new ArrayList<>(row.subList(0, 4))
                );

                answerGroups.add(
                        new ArrayList<>(row.subList(4, 8))
                );
            }
        }

        return answerGroups;
    }
    private static double findColumnDivider(List<Bubble> bubbles) {

        List<Integer> xValues = bubbles.stream()
                .map(Bubble::centerX)
                .distinct()
                .sorted()
                .toList();

        if (xValues.size() < 2) {
            throw new RuntimeException(
                    "Not enough bubble columns detected"
            );
        }

        int largestGap = 0;
        double divider = 0;

        for (int i = 1; i < xValues.size(); i++) {

            int previous = xValues.get(i - 1);
            int current = xValues.get(i);

            int gap = current - previous;

            if (gap > largestGap) {
                largestGap = gap;
                divider = (previous + current) / 2.0;
            }
        }

        return divider;
    }
    private static double bubbleFillRatio(
            Mat binary,
            Bubble bubble
    ) {
        int centerX = bubble.centerX();
        int centerY = bubble.centerY();

        // Use the inner area to avoid counting the printed outline.
        int radius = (int) Math.round(
                Math.min(bubble.width, bubble.height) * 0.30
        );

        int x = Math.max(centerX - radius, 0);
        int y = Math.max(centerY - radius, 0);

        int width = Math.min(
                radius * 2 + 1,
                binary.cols() - x
        );

        int height = Math.min(
                radius * 2 + 1,
                binary.rows() - y
        );

        if (width <= 0 || height <= 0) {
            return 0;
        }

        Mat roi = new Mat(
                binary,
                new Rect(x, y, width, height)
        );

        Mat mask = Mat.zeros(
                height,
                width,
                CV_8UC1
        ).asMat();

        circle(
                mask,
                new Point(width / 2, height / 2),
                radius,
                new Scalar(255),
                FILLED,
                LINE_8,
                0
        );

        Mat masked = new Mat();

        bitwise_and(
                roi,
                mask,
                masked
        );

        int filledPixels = countNonZero(masked);
        int maskPixels = countNonZero(mask);

        if (maskPixels == 0) {
            return 0;
        }

        return (double) filledPixels / maskPixels;
    }
    private static char readDetectedRow(
            Mat binary,
            Mat debug,
            List<Bubble> row,
            int questionNumber
    ) {
        if (row.size() != 4) {

            System.out.printf(
                    "Q%d invalid row: expected 4 bubbles, found %d%n",
                    questionNumber,
                    row.size()
            );

            return '?';
        }

        // Ensure A, B, C, D ordering from left to right.
        row.sort(Comparator.comparingInt(Bubble::centerX));

        double[] scores = new double[4];

        for (int i = 0; i < 4; i++) {
            scores[i] = bubbleFillRatio(
                    binary,
                    row.get(i)
            );
        }

        Integer[] indexes = {0, 1, 2, 3};

        java.util.Arrays.sort(
                indexes,
                (a, b) -> Double.compare(
                        scores[b],
                        scores[a]
                )
        );

        int bestIndex = indexes[0];
        int secondIndex = indexes[1];

        double best = scores[bestIndex];
        double second = scores[secondIndex];

        char result;

        // Blank answer.
        if (best < 0.25) {
            result = '-';
        }

        // Multiple or unclear answer.
        else if (
                second >= 0.25 &&
                        best - second < 0.12
        ) {
            result = 'X';
        }

        // Valid answer.
        else {
            result = (char) ('A' + bestIndex);
        }

        System.out.printf(
                "Q%-2d scores -> A=%.3f B=%.3f C=%.3f D=%.3f result=%s%n",
                questionNumber,
                scores[0],
                scores[1],
                scores[2],
                scores[3],
                result
        );

        drawRowDebug(
                debug,
                row,
                scores,
                bestIndex,
                secondIndex,
                result,
                questionNumber
        );

        return result;
    }
    private static void drawRowDebug(
            Mat debug,
            List<Bubble> row,
            double[] scores,
            int bestIndex,
            int secondIndex,
            char result,
            int questionNumber
    ) {
        for (int option = 0; option < row.size(); option++) {

            Bubble bubble = row.get(option);

            int centerX = bubble.centerX();
            int centerY = bubble.centerY();

            int detectedRadius = Math.max(
                    4,
                    Math.min(
                            bubble.width,
                            bubble.height
                    ) / 2
            );

            Scalar optionColor =
                    getOptionDebugColor(option);

            /*
             * Draw the detected bubble boundary.
             *
             * A = blue
             * B = green
             * C = red
             * D = purple
             */
            circle(
                    debug,
                    new Point(centerX, centerY),
                    detectedRadius,
                    optionColor,
                    2,
                    LINE_AA,
                    0
            );

            /*
             * Draw a smaller center circle so the exact scoring
             * position is visible.
             */
            int scoringRadius = Math.max(
                    3,
                    (int) Math.round(detectedRadius * 0.60)
            );

            circle(
                    debug,
                    new Point(centerX, centerY),
                    scoringRadius,
                    optionColor,
                    1,
                    LINE_AA,
                    0
            );

            /*
             * Display the fill ratio as an integer percentage.
             * For example, 72 means a 0.72 fill ratio.
             */
            String scoreText =
                    String.format("%.0f", scores[option] * 100);

            putText(
                    debug,
                    scoreText,
                    new Point(
                            centerX + detectedRadius + 3,
                            centerY - 3
                    ),
                    FONT_HERSHEY_SIMPLEX,
                    0.28,
                    optionColor,
                    1,
                    LINE_AA,
                    false
            );
        }

        if (result >= 'A' && result <= 'D') {

            /*
             * A valid selected answer gets a large green ring.
             */
            Bubble selected = row.get(bestIndex);

            int selectedRadius =
                    Math.max(
                            selected.width,
                            selected.height
                    ) / 2 + 4;

            circle(
                    debug,
                    new Point(
                            selected.centerX(),
                            selected.centerY()
                    ),
                    selectedRadius,
                    new Scalar(0, 200, 0, 0),
                    3,
                    LINE_AA,
                    0
            );
        } else if (result == 'X') {

            /*
             * Multiple or unclear answers get orange rings around
             * the two highest-scoring bubbles.
             */
            drawDecisionCircle(
                    debug,
                    row.get(bestIndex),
                    new Scalar(0, 140, 255, 0)
            );

            drawDecisionCircle(
                    debug,
                    row.get(secondIndex),
                    new Scalar(0, 140, 255, 0)
            );
        } else if (result == '-') {

            /*
             * Blank rows get yellow circles around all options.
             */
            for (Bubble bubble : row) {
                drawDecisionCircle(
                        debug,
                        bubble,
                        new Scalar(0, 255, 255, 0)
                );
            }
        }

        drawQuestionResult(
                debug,
                row,
                questionNumber,
                result
        );
    }
    private static Scalar getOptionDebugColor(int option) {

        return switch (option) {
            case 0 ->
                    new Scalar(255, 0, 0, 0);       // A: blue

            case 1 ->
                    new Scalar(0, 255, 0, 0);       // B: green

            case 2 ->
                    new Scalar(0, 0, 255, 0);       // C: red

            default ->
                    new Scalar(255, 0, 255, 0);     // D: purple
        };
    }private static void drawDecisionCircle(
            Mat debug,
            Bubble bubble,
            Scalar color
    ) {
        int radius =
                Math.max(
                        bubble.width,
                        bubble.height
                ) / 2 + 4;

        circle(
                debug,
                new Point(
                        bubble.centerX(),
                        bubble.centerY()
                ),
                radius,
                color,
                3,
                LINE_AA,
                0
        );
    }private static void drawQuestionResult(
            Mat debug,
            List<Bubble> row,
            int questionNumber,
            char result
    ) {
        Bubble firstBubble = row.get(0);

        int textX = Math.max(
                firstBubble.centerX() - 65,
                2
        );

        int textY = firstBubble.centerY() + 4;

        Scalar resultColor;

        if (result >= 'A' && result <= 'D') {
            resultColor = new Scalar(0, 150, 0, 0);
        } else if (result == 'X') {
            resultColor = new Scalar(0, 0, 255, 0);
        } else if (result == '-') {
            resultColor = new Scalar(0, 180, 255, 0);
        } else {
            resultColor = new Scalar(0, 0, 255, 0);
        }

        String text =
                "Q" + questionNumber + "=" + result;

        putText(
                debug,
                text,
                new Point(textX, textY),
                FONT_HERSHEY_SIMPLEX,
                0.35,
                resultColor,
                1,
                LINE_AA,
                false
        );
    }
}