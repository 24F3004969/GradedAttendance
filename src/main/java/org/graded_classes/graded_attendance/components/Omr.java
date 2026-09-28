package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Fixed-template OMR reader for the current 800 x 1200 sheet.
 *
 * Layout:
 * - Four 40 x 40 registration markers
 * - Six roll/enrollment columns, with values 0 through 6
 * - Ninety questions in three columns of thirty
 * - Four options per question: A, B, C and D
 *
 * Requires Java 17+ and JavaCPP Presets for OpenCV.
 */
public final class Omr {

    private static final int PAGE_WIDTH = 800;
    private static final int PAGE_HEIGHT = 1200;

    // Registration marker centers from OMRGeneratorApp.
    private static final Point2d DEST_TOP_LEFT = new Point2d(50, 50);
    private static final Point2d DEST_TOP_RIGHT = new Point2d(750, 50);
    private static final Point2d DEST_BOTTOM_RIGHT = new Point2d(750, 1150);
    private static final Point2d DEST_BOTTOM_LEFT = new Point2d(50, 1150);

    private static final int TOTAL_QUESTIONS = 90;
    private static final int QUESTIONS_PER_COLUMN = 30;
    private static final int QUESTION_COLUMNS = 3;
    private static final int FIRST_ROW_Y = 350;
    private static final int QUESTION_GAP_Y = 25;

    // optionX = columnX + 35 + option * 36
    private static final int[][] QUESTION_OPTION_X = {
            {90, 126, 162, 198},
            {345, 381, 417, 453},
            {600, 636, 672, 708}
    };

    private static final int QUESTION_SCORE_RADIUS = 5;
    private static final int QUESTION_DEBUG_RADIUS = 12;

    private static final int ROLL_DIGITS = 6;
    private static final int ROLL_VALUES = 7; // Current generator contains 0 through 6.
    private static final int[] ROLL_COLUMN_X = {515, 548, 581, 614, 647, 680};
    private static final int[] ROLL_VALUE_Y = {148, 168, 188, 208, 228, 248, 268};
    private static final int ROLL_SCORE_RADIUS = 5;
    private static final int ROLL_DEBUG_RADIUS = 12;

    private static final double QUESTION_FILLED_THRESHOLD = 0.50;
    private static final double ROLL_FILLED_THRESHOLD = 0.30;
    private static final double MIN_WINNING_MARGIN = 0.12;

    private Omr() {
    }

    public static void main(String[] args) {
        String inputPath = args.length > 0 ? args[0] : "omr13.jpeg";
        OmrResult result = read(inputPath, true);

        System.out.printf("Roll/Enrollment No: %s (%s)%n",
                result.rollNumber().value(),
                result.rollNumber().valid() ? "VALID" : "INVALID");

        for (QuestionResult question : result.questions()) {
            System.out.printf(
                    "Q%02d -> %s [A=%.3f B=%.3f C=%.3f D=%.3f]%n",
                    question.questionNumber(), question.answer(),
                    question.scores()[0], question.scores()[1],
                    question.scores()[2], question.scores()[3]
            );
        }

        System.out.println("Normalized image: normalized_original.png");
        System.out.println("Binary image:     normalized_binary.png");
        System.out.println("Debug image:      normalized_debug.png");
    }

    public static OmrResult read(String imagePath, boolean saveDebugImages) {
        Mat input = imread(imagePath);
        if (input == null || input.empty()) {
            throw new IllegalArgumentException("Cannot load OMR image: " + imagePath);
        }

        SheetMarkers sourceMarkers = findRegistrationMarkers(input);
        Mat normalized = normalize(input, sourceMarkers);
        Mat binary = makeScoringBinary(normalized);
        Mat debug = normalized.clone();

        RollNumberResult rollNumber = readRollNumber(binary, debug);
        List<QuestionResult> questions = new ArrayList<>(TOTAL_QUESTIONS);

        for (int questionNumber = 1;
             questionNumber <= TOTAL_QUESTIONS;
             questionNumber++) {

            int zeroBasedQuestion = questionNumber - 1;
            int columnIndex = zeroBasedQuestion / QUESTIONS_PER_COLUMN;
            int rowIndex = zeroBasedQuestion % QUESTIONS_PER_COLUMN;
            int y = FIRST_ROW_Y + rowIndex * QUESTION_GAP_Y;

            questions.add(readQuestion(
                    binary,
                    debug,
                    questionNumber,
                    columnIndex,
                    QUESTION_OPTION_X[columnIndex],
                    y
            ));
        }

        if (saveDebugImages) {
            imwrite("normalized_original.png", normalized);
            imwrite("normalized_binary.png", binary);
            imwrite("normalized_debug.png", debug);
        }

        return new OmrResult(
                rollNumber,
                List.copyOf(questions),
                normalized,
                binary,
                debug
        );
    }

    private static SheetMarkers findRegistrationMarkers(Mat input) {
        Mat gray = new Mat();
        if (input.channels() == 1) {
            input.copyTo(gray);
        } else {
            cvtColor(input, gray, COLOR_BGR2GRAY);
        }

        Mat blurred = new Mat();
        GaussianBlur(gray, blurred, new Size(5, 5), 0);

        Mat binary = new Mat();
        threshold(blurred, binary, 0, 255, THRESH_BINARY_INV | THRESH_OTSU);

        int kernelSize = Math.max(3, Math.min(input.cols(), input.rows()) / 250);
        if (kernelSize % 2 == 0) {
            kernelSize++;
        }

        Mat kernel = getStructuringElement(
                MORPH_RECT,
                new Size(kernelSize, kernelSize)
        );
        morphologyEx(binary, binary, MORPH_CLOSE, kernel);

        MatVector contours = new MatVector();
        findContours(binary.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        List<MarkerCandidate> candidates = new ArrayList<>();
        double imageArea = (double) input.cols() * input.rows();
        int minimumSide = Math.min(input.cols(), input.rows());

        for (long i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            Rect rect = boundingRect(contour);

            if (rect.width() < minimumSide * 0.025
                    || rect.height() < minimumSide * 0.025) {
                continue;
            }

            double area = contourArea(contour);
            double relativeArea = area / imageArea;
            double aspect = (double) rect.width() / rect.height();
            double extent = area / ((double) rect.width() * rect.height());

            if (relativeArea < 0.0007 || relativeArea > 0.03) continue;
            if (aspect < 0.60 || aspect > 1.45) continue;
            if (extent < 0.60) continue;

            candidates.add(new MarkerCandidate(
                    new Point2d(
                            rect.x() + rect.width() / 2.0,
                            rect.y() + rect.height() / 2.0
                    ),
                    area
            ));
        }

        if (candidates.size() < 4) {
            throw new IllegalStateException(
                    "Could not find all four registration markers. Found "
                            + candidates.size()
                            + ". Keep all four black squares visible in the scan."
            );
        }

        MarkerCandidate topLeft = nearestCorner(
                candidates, 0, 0, input.cols(), input.rows());
        MarkerCandidate topRight = nearestCorner(
                candidates, input.cols(), 0, input.cols(), input.rows());
        MarkerCandidate bottomRight = nearestCorner(
                candidates, input.cols(), input.rows(), input.cols(), input.rows());
        MarkerCandidate bottomLeft = nearestCorner(
                candidates, 0, input.rows(), input.cols(), input.rows());

        if (topLeft == topRight || topLeft == bottomLeft || topLeft == bottomRight
                || topRight == bottomLeft || topRight == bottomRight
                || bottomLeft == bottomRight) {
            throw new IllegalStateException(
                    "Registration marker detection selected a duplicate corner."
            );
        }

        return new SheetMarkers(
                topLeft.center(),
                topRight.center(),
                bottomRight.center(),
                bottomLeft.center()
        );
    }

    private static MarkerCandidate nearestCorner(
            List<MarkerCandidate> candidates,
            double cornerX,
            double cornerY,
            int imageWidth,
            int imageHeight
    ) {
        double maximumDistance = Math.hypot(imageWidth, imageHeight) * 0.45;

        return candidates.stream()
                .filter(candidate -> Math.hypot(
                        candidate.center().x() - cornerX,
                        candidate.center().y() - cornerY
                ) < maximumDistance)
                .min(Comparator.comparingDouble(candidate -> {
                    double dx = (candidate.center().x() - cornerX) / imageWidth;
                    double dy = (candidate.center().y() - cornerY) / imageHeight;
                    double areaPreference = Math.min(
                            candidate.area() / (imageWidth * (double) imageHeight),
                            0.01
                    );
                    return Math.hypot(dx, dy) - areaPreference;
                }))
                .orElseThrow(() -> new IllegalStateException(
                        "No registration marker found near image corner "
                                + cornerX + "," + cornerY
                ));
    }

    private static Mat normalize(Mat input, SheetMarkers markers) {
        Point2f source = new Point2f(4);
        setPoint(source, 0, markers.topLeft());
        setPoint(source, 1, markers.topRight());
        setPoint(source, 2, markers.bottomRight());
        setPoint(source, 3, markers.bottomLeft());
        source.position(0);

        Point2f destination = new Point2f(4);
        setPoint(destination, 0, DEST_TOP_LEFT);
        setPoint(destination, 1, DEST_TOP_RIGHT);
        setPoint(destination, 2, DEST_BOTTOM_RIGHT);
        setPoint(destination, 3, DEST_BOTTOM_LEFT);
        destination.position(0);

        Mat transform = getPerspectiveTransform(source, destination);
        Mat normalized = new Mat();

        warpPerspective(
                input,
                normalized,
                transform,
                new Size(PAGE_WIDTH, PAGE_HEIGHT),
                INTER_CUBIC,
                BORDER_CONSTANT,
                new Scalar(255, 255, 255, 0)
        );

        return normalized;
    }

    private static void setPoint(Point2f points, int index, Point2d value) {
        points.position(index)
                .x((float) value.x())
                .y((float) value.y());
    }

    private static Mat makeScoringBinary(Mat normalized) {
        Mat gray = new Mat();
        if (normalized.channels() == 1) {
            normalized.copyTo(gray);
        } else {
            cvtColor(normalized, gray, COLOR_BGR2GRAY);
        }

        Mat blurred = new Mat();
        GaussianBlur(gray, blurred, new Size(3, 3), 0);

        Mat binary = new Mat();
        adaptiveThreshold(
                blurred,
                binary,
                255,
                ADAPTIVE_THRESH_GAUSSIAN_C,
                THRESH_BINARY_INV,
                31,
                12
        );
        return binary;
    }

    private static RollNumberResult readRollNumber(Mat binary, Mat debug) {
        StringBuilder value = new StringBuilder(ROLL_DIGITS);
        List<RollDigitResult> digitResults = new ArrayList<>(ROLL_DIGITS);
        boolean valid = true;

        for (int position = 0; position < ROLL_DIGITS; position++) {
            double[] scores = new double[ROLL_VALUES];

            for (int rollValue = 0; rollValue < ROLL_VALUES; rollValue++) {
                scores[rollValue] = fillRatio(
                        binary,
                        ROLL_COLUMN_X[position],
                        ROLL_VALUE_Y[rollValue],
                        ROLL_SCORE_RADIUS
                );
            }

            RankedScores ranking = rank(scores);
            char digit;

            if (ranking.bestScore() < ROLL_FILLED_THRESHOLD) {
                digit = '-';
            } else if (ranking.secondScore() >= ROLL_FILLED_THRESHOLD
                    && ranking.bestScore() - ranking.secondScore()
                    < MIN_WINNING_MARGIN) {
                digit = 'X';
            } else {
                digit = (char) ('0' + ranking.bestIndex());
            }

            if (!Character.isDigit(digit)) {
                valid = false;
            }

            value.append(digit);
            digitResults.add(new RollDigitResult(
                    position + 1,
                    digit,
                    scores.clone()
            ));

            drawRollDigitDebug(
                    debug,
                    position,
                    scores,
                    ranking.bestIndex(),
                    ranking.secondIndex(),
                    digit
            );
        }

        drawRollResult(debug, value.toString(), valid);

        return new RollNumberResult(
                value.toString(),
                valid,
                List.copyOf(digitResults)
        );
    }

    private static QuestionResult readQuestion(
            Mat binary,
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] optionX,
            int y
    ) {
        double[] scores = new double[4];

        for (int option = 0; option < 4; option++) {
            scores[option] = fillRatio(
                    binary,
                    optionX[option],
                    y,
                    QUESTION_SCORE_RADIUS
            );
        }

        RankedScores ranking = rank(scores);
        char answer;

        if (ranking.bestScore() < QUESTION_FILLED_THRESHOLD) {
            answer = '-';
        } else if (ranking.secondScore() >= QUESTION_FILLED_THRESHOLD
                && ranking.bestScore() - ranking.secondScore()
                < MIN_WINNING_MARGIN) {
            answer = 'X';
        } else {
            answer = (char) ('A' + ranking.bestIndex());
        }

        drawQuestionDebug(
                debug,
                questionNumber,
                columnIndex,
                optionX,
                y,
                scores,
                answer,
                ranking.bestIndex(),
                ranking.secondIndex()
        );

        return new QuestionResult(
                questionNumber,
                answer,
                scores.clone()
        );
    }

    private static RankedScores rank(double[] scores) {
        int bestIndex = -1;
        int secondIndex = -1;
        double bestScore = -1;
        double secondScore = -1;

        for (int index = 0; index < scores.length; index++) {
            double score = scores[index];

            if (score > bestScore) {
                secondScore = bestScore;
                secondIndex = bestIndex;
                bestScore = score;
                bestIndex = index;
            } else if (score > secondScore) {
                secondScore = score;
                secondIndex = index;
            }
        }

        return new RankedScores(
                bestIndex,
                secondIndex,
                bestScore,
                secondScore
        );
    }

    private static double fillRatio(
            Mat binary,
            int centerX,
            int centerY,
            int radius
    ) {
        int x = Math.max(0, centerX - radius);
        int y = Math.max(0, centerY - radius);
        int width = Math.min(radius * 2 + 1, binary.cols() - x);
        int height = Math.min(radius * 2 + 1, binary.rows() - y);

        if (width <= 0 || height <= 0) {
            return 0;
        }

        Mat roi = new Mat(binary, new Rect(x, y, width, height));
        Mat mask = Mat.zeros(height, width, CV_8UC1).asMat();

        circle(
                mask,
                new Point(centerX - x, centerY - y),
                radius,
                new Scalar(255),
                FILLED,
                LINE_8,
                0
        );

        Mat counted = new Mat();
        bitwise_and(roi, mask, counted);

        int maskPixels = countNonZero(mask);
        return maskPixels == 0
                ? 0
                : (double) countNonZero(counted) / maskPixels;
    }

    private static void drawQuestionDebug(
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] optionX,
            int y,
            double[] scores,
            char answer,
            int bestIndex,
            int secondIndex
    ) {
        for (int option = 0; option < 4; option++) {
            Scalar color = switch (option) {
                case 0 -> new Scalar(255, 0, 0, 0);
                case 1 -> new Scalar(0, 180, 0, 0);
                case 2 -> new Scalar(0, 0, 255, 0);
                default -> new Scalar(255, 0, 255, 0);
            };

            circle(
                    debug,
                    new Point(optionX[option], y),
                    QUESTION_SCORE_RADIUS,
                    color,
                    1,
                    LINE_AA,
                    0
            );
        }

        if (answer >= 'A' && answer <= 'D') {
            circle(
                    debug,
                    new Point(optionX[bestIndex], y),
                    QUESTION_DEBUG_RADIUS,
                    new Scalar(0, 210, 0, 0),
                    2,
                    LINE_AA,
                    0
            );
        } else if (answer == 'X') {
            circle(
                    debug,
                    new Point(optionX[bestIndex], y),
                    QUESTION_DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0),
                    2,
                    LINE_AA,
                    0
            );
            circle(
                    debug,
                    new Point(optionX[secondIndex], y),
                    QUESTION_DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0),
                    2,
                    LINE_AA,
                    0
            );
        }

        // Place labels in the free gap following each question block.
        int labelX = optionX[3] + 12;
        if (columnIndex == QUESTION_COLUMNS - 1) {
            labelX = optionX[0] - 31;
        }

        Scalar labelColor = answer == 'X'
                ? new Scalar(0, 0, 255, 0)
                : answer == '-'
                    ? new Scalar(0, 170, 255, 0)
                    : new Scalar(0, 150, 0, 0);

        putText(
                debug,
                "Q" + questionNumber + "=" + answer,
                new Point(labelX, y + 4),
                FONT_HERSHEY_SIMPLEX,
                0.24,
                labelColor,
                1,
                LINE_AA,
                false
        );
    }

    private static void drawRollDigitDebug(
            Mat debug,
            int position,
            double[] scores,
            int bestIndex,
            int secondIndex,
            char result
    ) {
        int x = ROLL_COLUMN_X[position];

        for (int rollValue = 0; rollValue < ROLL_VALUES; rollValue++) {
            circle(
                    debug,
                    new Point(x, ROLL_VALUE_Y[rollValue]),
                    ROLL_SCORE_RADIUS,
                    new Scalar(255, 120, 0, 0),
                    1,
                    LINE_AA,
                    0
            );
        }

        if (Character.isDigit(result)) {
            int selectedValue = result - '0';
            circle(
                    debug,
                    new Point(x, ROLL_VALUE_Y[selectedValue]),
                    ROLL_DEBUG_RADIUS,
                    new Scalar(0, 210, 0, 0),
                    2,
                    LINE_AA,
                    0
            );
        } else if (result == 'X') {
            circle(
                    debug,
                    new Point(x, ROLL_VALUE_Y[bestIndex]),
                    ROLL_DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0),
                    2,
                    LINE_AA,
                    0
            );
            circle(
                    debug,
                    new Point(x, ROLL_VALUE_Y[secondIndex]),
                    ROLL_DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0),
                    2,
                    LINE_AA,
                    0
            );
        }

        putText(
                debug,
                String.valueOf(result),
                new Point(x - 3, 288),
                FONT_HERSHEY_SIMPLEX,
                0.30,
                result == 'X'
                        ? new Scalar(0, 0, 255, 0)
                        : new Scalar(0, 150, 0, 0),
                1,
                LINE_AA,
                false
        );
    }

    private static void drawRollResult(
            Mat debug,
            String value,
            boolean valid
    ) {
        putText(
                debug,
                "ROLL=" + value,
                new Point(510, 310),
                FONT_HERSHEY_SIMPLEX,
                0.38,
                valid
                        ? new Scalar(0, 150, 0, 0)
                        : new Scalar(0, 0, 255, 0),
                1,
                LINE_AA,
                false
        );
    }

    private record MarkerCandidate(Point2d center, double area) {
    }

    private record SheetMarkers(
            Point2d topLeft,
            Point2d topRight,
            Point2d bottomRight,
            Point2d bottomLeft
    ) {
    }

    private record RankedScores(
            int bestIndex,
            int secondIndex,
            double bestScore,
            double secondScore
    ) {
    }

    public record RollDigitResult(
            int position,
            char value,
            double[] scores
    ) {
    }

    public record RollNumberResult(
            String value,
            boolean valid,
            List<RollDigitResult> digits
    ) {
    }

    public record QuestionResult(
            int questionNumber,
            char answer,
            double[] scores
    ) {
    }

    public record OmrResult(
            RollNumberResult rollNumber,
            List<QuestionResult> questions,
            Mat normalizedImage,
            Mat binaryImage,
            Mat debugImage
    ) {
        public QuestionResult question(int number) {
            if (number < 1 || number > questions.size()) {
                throw new IllegalArgumentException(
                        "Question must be between 1 and " + questions.size()
                );
            }
            return questions.get(number - 1);
        }
    }
}
