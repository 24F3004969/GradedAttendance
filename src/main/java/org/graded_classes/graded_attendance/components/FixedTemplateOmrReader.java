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
 * OMR reader dedicated to the fixed 70-question Graded Coaching Classes sheet.
 *
 * Instead of detecting every printed bubble, this reader:
 * 1. Finds the four large black registration squares.
 * 2. Corrects scanner rotation and perspective to the original 1414 x 2000 layout.
 * 3. Reads the 280 known bubble positions from the normalized sheet.
 */
public final class FixedTemplateOmrReader {

    private static final int PAGE_WIDTH = 1414;
    private static final int PAGE_HEIGHT = 2000;

    // Registration-square centers measured from the supplied master sheet.
    private static final Point2d DEST_TOP_LEFT = new Point2d(115, 109);
    private static final Point2d DEST_TOP_RIGHT = new Point2d(1352, 109);
    private static final Point2d DEST_BOTTOM_RIGHT = new Point2d(1352, 1942);
    private static final Point2d DEST_BOTTOM_LEFT = new Point2d(115, 1942);

    // Bubble centers measured from the supplied master sheet.
    private static final int[] LEFT_X = {266, 354, 442, 531};
    private static final int[] RIGHT_X = {849, 937, 1026, 1114};
    private static final double FIRST_ROW_Y = 442.0;
    private static final double LAST_ROW_Y = 1688.0;
    private static final int ROWS_PER_SIDE = 35;

    // The normalized bubbles are about 27 to 31 pixels wide.
    private static final int SCORE_RADIUS = 9;
    private static final int DEBUG_RADIUS = 15;

    // Tune only if pen/scanner behaviour is substantially different.
    private static final double FILLED_THRESHOLD = 0.35;
    private static final double MIN_WINNING_MARGIN = 0.14;

    private FixedTemplateOmrReader() {
    }

    public static void main(String[] args) {
        String inputPath = args.length > 0 ? args[0] : "omr.png";
        OmrResult result = read(inputPath, true);

        for (QuestionResult question : result.questions()) {
            System.out.printf(
                    "Q%02d -> %s  [A=%.3f B=%.3f C=%.3f D=%.3f]%n",
                    question.questionNumber(),
                    question.answer(),
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

        List<QuestionResult> questions = new ArrayList<>(70);

        for (int row = 0; row < ROWS_PER_SIDE; row++) {
            int y = rowY(row);
            questions.add(readQuestion(binary, debug, row + 1, LEFT_X, y));
        }

        for (int row = 0; row < ROWS_PER_SIDE; row++) {
            int y = rowY(row);
            questions.add(readQuestion(binary, debug, row + 36, RIGHT_X, y));
        }

        if (saveDebugImages) {
            imwrite("normalized_original.png", normalized);
            imwrite("normalized_binary.png", binary);
            imwrite("normalized_debug.png", debug);
        }

        return new OmrResult(List.copyOf(questions), normalized, binary, debug);
    }

    private static int rowY(int zeroBasedRow) {
        double step = (LAST_ROW_Y - FIRST_ROW_Y) / (ROWS_PER_SIDE - 1);
        return (int) Math.round(FIRST_ROW_Y + zeroBasedRow * step);
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

        // Close small white scanner gaps inside the black registration blocks.
        Mat kernel = getStructuringElement(MORPH_RECT, new Size(7, 7));
        morphologyEx(binary, binary, MORPH_CLOSE, kernel);

        MatVector contours = new MatVector();
        findContours(binary.clone(), contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

        List<MarkerCandidate> candidates = new ArrayList<>();
        double imageArea = (double) input.cols() * input.rows();
        int minimumSide = Math.min(input.cols(), input.rows());

        for (long i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            Rect rect = boundingRect(contour);
            double area = contourArea(contour);

            if (rect.width() < minimumSide * 0.025 || rect.height() < minimumSide * 0.025) {
                continue;
            }

            double relativeArea = area / imageArea;
            double aspect = (double) rect.width() / rect.height();
            double extent = area / ((double) rect.width() * rect.height());

            if (relativeArea < 0.0007 || relativeArea > 0.03) continue;
            if (aspect < 0.60 || aspect > 1.45) continue;
            if (extent < 0.60) continue;

            candidates.add(new MarkerCandidate(
                    new Point2d(rect.x() + rect.width() / 2.0,
                            rect.y() + rect.height() / 2.0),
                    area,
                    rect
            ));
        }

        if (candidates.size() < 4) {
            throw new IllegalStateException(
                    "Could not find all four registration squares. Found " + candidates.size()
                            + ". Ensure all four black corner squares are visible."
            );
        }

        MarkerCandidate topLeft = nearestCorner(candidates, 0, 0, input.cols(), input.rows());
        MarkerCandidate topRight = nearestCorner(candidates, input.cols(), 0, input.cols(), input.rows());
        MarkerCandidate bottomRight = nearestCorner(candidates, input.cols(), input.rows(), input.cols(), input.rows());
        MarkerCandidate bottomLeft = nearestCorner(candidates, 0, input.rows(), input.cols(), input.rows());

        if (topLeft == topRight || topLeft == bottomLeft || topLeft == bottomRight
                || topRight == bottomLeft || topRight == bottomRight || bottomLeft == bottomRight) {
            throw new IllegalStateException("Registration-square detection produced duplicate corners.");
        }

        return new SheetMarkers(
                topLeft.center(), topRight.center(),
                bottomRight.center(), bottomLeft.center()
        );
    }

    private static MarkerCandidate nearestCorner(
            List<MarkerCandidate> candidates,
            double cornerX,
            double cornerY,
            int imageWidth,
            int imageHeight
    ) {
        double maxCornerDistance = Math.hypot(imageWidth, imageHeight) * 0.45;

        return candidates.stream()
                .filter(c -> Math.hypot(c.center().x() - cornerX,
                        c.center().y() - cornerY) < maxCornerDistance)
                .min(Comparator.comparingDouble(c -> {
                    double dx = (c.center().x() - cornerX) / imageWidth;
                    double dy = (c.center().y() - cornerY) / imageHeight;
                    // Distance dominates. Larger solid squares receive a small preference.
                    return Math.hypot(dx, dy) - Math.min(c.area() / (imageWidth * (double) imageHeight), 0.01);
                }))
                .orElseThrow(() -> new IllegalStateException(
                        "No registration square found near image corner " + cornerX + "," + cornerY
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
        points.position(index).x((float) value.x()).y((float) value.y());
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

    private static QuestionResult readQuestion(
            Mat binary,
            Mat debug,
            int questionNumber,
            int[] optionX,
            int y
    ) {
        double[] scores = new double[4];
        for (int option = 0; option < 4; option++) {
            scores[option] = fillRatio(binary, optionX[option], y, SCORE_RADIUS);
        }

        Integer[] order = {0, 1, 2, 3};
        java.util.Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        int bestIndex = order[0];
        int secondIndex = order[1];
        double best = scores[bestIndex];
        double second = scores[secondIndex];

        int markedCount = 0;
        for (double score : scores) {
            if (score >= FILLED_THRESHOLD) markedCount++;
        }

        char answer;
        if (markedCount == 0) {
            answer = '-';
        } else if (markedCount > 1 || best - second < MIN_WINNING_MARGIN) {
            answer = 'X';
        } else {
            answer = (char) ('A' + bestIndex);
        }

        drawQuestionDebug(debug, questionNumber, optionX, y, scores, answer, bestIndex, secondIndex);
        return new QuestionResult(questionNumber, answer, scores.clone());
    }

    private static double fillRatio(Mat binary, int centerX, int centerY, int radius) {
        int x = Math.max(0, centerX - radius);
        int y = Math.max(0, centerY - radius);
        int width = Math.min(radius * 2 + 1, binary.cols() - x);
        int height = Math.min(radius * 2 + 1, binary.rows() - y);

        if (width <= 0 || height <= 0) return 0;

        Mat roi = new Mat(binary, new Rect(x, y, width, height));
        Mat mask = Mat.zeros(height, width, CV_8UC1).asMat();
        circle(mask, new Point(centerX - x, centerY - y), radius,
                new Scalar(255), FILLED, LINE_8, 0);

        Mat counted = new Mat();
        bitwise_and(roi, mask, counted);

        int maskPixels = countNonZero(mask);
        return maskPixels == 0 ? 0 : (double) countNonZero(counted) / maskPixels;
    }

    private static void drawQuestionDebug(
            Mat debug,
            int questionNumber,
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

            circle(debug, new Point(optionX[option], y), SCORE_RADIUS,
                    color, 1, LINE_AA, 0);
            putText(debug, String.format("%.0f", scores[option] * 100),
                    new Point(optionX[option] + 12, y - 4),
                    FONT_HERSHEY_SIMPLEX, 0.25, color, 1, LINE_AA, false);
        }

        if (answer >= 'A' && answer <= 'D') {
            circle(debug, new Point(optionX[bestIndex], y), DEBUG_RADIUS,
                    new Scalar(0, 210, 0, 0), 3, LINE_AA, 0);
        } else if (answer == 'X') {
            circle(debug, new Point(optionX[bestIndex], y), DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0), 3, LINE_AA, 0);
            circle(debug, new Point(optionX[secondIndex], y), DEBUG_RADIUS,
                    new Scalar(0, 140, 255, 0), 3, LINE_AA, 0);
        }

        int labelX = Math.max(5, optionX[0] - 78);
        Scalar labelColor = answer == 'X'
                ? new Scalar(0, 0, 255, 0)
                : answer == '-'
                ? new Scalar(0, 170, 255, 0)
                : new Scalar(0, 150, 0, 0);
        putText(debug, "Q" + questionNumber + "=" + answer,
                new Point(labelX, y + 4), FONT_HERSHEY_SIMPLEX,
                0.32, labelColor, 1, LINE_AA, false);
    }

    private record MarkerCandidate(Point2d center, double area, Rect bounds) {}

    private record SheetMarkers(
            Point2d topLeft,
            Point2d topRight,
            Point2d bottomRight,
            Point2d bottomLeft
    ) {}

    public record QuestionResult(int questionNumber, char answer, double[] scores) {}

    public record OmrResult(
            List<QuestionResult> questions,
            Mat normalizedImage,
            Mat binaryImage,
            Mat debugImage
    ) {
        public QuestionResult question(int number) {
            if (number < 1 || number > questions.size()) {
                throw new IllegalArgumentException("Question must be between 1 and " + questions.size());
            }
            return questions.get(number - 1);
        }
    }
}
