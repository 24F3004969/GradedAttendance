package org.graded_classes.graded_attendance.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_core.bitwise_and;
import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_core.inRange;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2HSV;
import static org.bytedeco.opencv.global.opencv_imgproc.FILLED;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_AREA;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_CUBIC;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_NEAREST;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.MORPH_CLOSE;
import static org.bytedeco.opencv.global.opencv_imgproc.MORPH_ELLIPSE;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.circle;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.getStructuringElement;
import static org.bytedeco.opencv.global.opencv_imgproc.line;
import static org.bytedeco.opencv.global.opencv_imgproc.morphologyEx;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.threshold;

/**
 * Special fixed-template blue-ink OMR scanner for sheets where the two
 * bottom registration squares were not printed.
 *
 * <p>This scanner does not use four-point perspective correction. It crops
 * the input to the expected 2:3 page ratio, resizes it to 800 x 1200, and
 * uses coordinates calibrated specifically for the special sheet batch.</p>
 *
 * <p>Output rules:</p>
 * <ul>
 *     <li>Questions: A/B/C/D, '-' for blank, '+' for multiple.</li>
 *     <li>Roll number: 1-9, '-' for blank, '?' for multiple.</li>
 * </ul>
 */
public final class SpecialOmr {

    private static final int PAGE_WIDTH = 800;
    private static final int PAGE_HEIGHT = 1200;
    private static final double TARGET_PAGE_RATIO = PAGE_WIDTH / (double) PAGE_HEIGHT;

    private static final int MINIMUM_INPUT_WIDTH = 600;
    private static final int MINIMUM_INPUT_HEIGHT = 900;
    private static final double MAXIMUM_CROPPABLE_RATIO_ERROR = 0.12;

    /* Change these only if the debug image shows a consistent crop offset. */
    private static final int FALLBACK_HORIZONTAL_CROP_OFFSET = 0;
    private static final int FALLBACK_VERTICAL_CROP_OFFSET = 0;

    private static final int TOTAL_QUESTIONS = 90;
    private static final int QUESTIONS_PER_COLUMN = 30;
    private static final int QUESTION_COLUMNS = 3;

    private static final int FIRST_ROW_Y = 348;
    private static final int QUESTION_GAP_Y = 25;

    /*
     * Special coordinates calibrated from the original sheet after
     * centre cropping to the 2:3 ratio and resizing to 800 x 1200.
     */
    private static final int[][] QUESTION_OPTION_X = {
            {55, 93, 132, 170},
            {327, 365, 404, 443},
            {596, 635, 674, 713}
    };

    private static final int ROLL_DIGITS = 4;
    private static final int ROLL_VALUES = 9;

    private static final int ROLL_FIRST_COLUMN_X = 507;
    private static final int ROLL_COLUMN_GAP_X = 35;

    private static final int ROLL_FIRST_ROW_Y = 145;
    private static final int ROLL_ROW_GAP_Y = 31;

    private static final int[] ROLL_COLUMN_X =
            createRollColumnXCoordinates();

    private static final int[] ROLL_VALUE_Y =
            createRollValueYCoordinates();

    private static final int QUESTION_SCORE_RADIUS = 7;
    private static final int ROLL_SCORE_RADIUS = 7;
    private static final int QUESTION_DEBUG_RADIUS = 12;
    private static final int ROLL_DEBUG_RADIUS = 12;
    private static final int LOCAL_SEARCH_RADIUS = 4;

    private static final double QUESTION_ABSOLUTE_MINIMUM = 0.35;
    private static final double ROLL_ABSOLUTE_MINIMUM = 0.35;
    private static final double QUESTION_RELATIVE_OFFSET = 0.08;
    private static final double ROLL_RELATIVE_OFFSET = 0.08;
    private static final double QUESTION_MULTIPLE_RELATIVE_FACTOR = 0.55;
    private static final double ROLL_MULTIPLE_RELATIVE_FACTOR = 0.55;
    private static final double MINIMUM_WINNING_MARGIN = 0.15;
    private static final double STRICT_CONFIDENCE_MINIMUM = 0.75;
    private static final double REVIEW_CONFIDENCE_MINIMUM = 0.55;
    private static final double MINIMUM_PAGE_BLUE_COVERAGE = 0.00008;

    private SpecialOmr() {
    }

    public static void main(String[] args) {
        File[] files = new File("C:\\Users\\hilal\\GradedAttendance\\omr").listFiles();
        List<OmrResult> omrResults = new ArrayList<>();
        SpecialOmr omr = new SpecialOmr();
        if (files != null) {
            for (File file : files) {
                String inputPath = file.getAbsolutePath();

                try (OmrResult result = read(inputPath, true)) {

                    System.out.printf(
                            "Roll/Enrollment No: %s (%s)%n",
                            result.rollNumber().value(),
                            result.rollNumber().valid() ? "VALID" : "INVALID"
                    );

                    for (QuestionResult question : result.questions()) {
                        double[] scores = question.scores();
                        System.out.printf(
                                Locale.US,
                                "Q%02d -> %s [A=%.3f B=%.3f C=%.3f D=%.3f, "
                                        + "threshold=%.3f, confidence=%.3f, status=%s]%n",
                                question.questionNumber(),
                                question.answer(),
                                scores[0], scores[1], scores[2], scores[3],
                                question.threshold(),
                                question.confidence(),
                                confidenceStatus(question.answer(), question.confidence())
                        );
                    }
                    omrResults.add(result);
                }
            }

        }
        omr.writeJsonFile(omrResults, new ArrayList<>(),new File("result_X.json").toPath());


    }


    public void writeJsonFile(
            List<OmrResult> scanResults,
            List<String> notes,
            Path outputFile
    ) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<StudentResult> students = scanResults.stream()
                .map(scan -> {
                    List<AnswerResult> answers = scan.questions()
                            .stream()
                            .limit(20)
                            .map(question -> new AnswerResult(
                                    question.questionNumber(),
                                    question.answer()
                            ))
                            .toList();

                    return new StudentResult(
                            scan.rollNumber().value().replace("-",""),
                            answers
                    );
                })
                .toList();

        OmrJsonResult result = new OmrJsonResult(students, notes);

        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            Path parentDirectory = outputFile.getParent();

            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            Files.writeString(
                    outputFile,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to write OMR results to JSON file: " + outputFile,
                    exception
            );
        }
    }

    public static OmrResult read(String imagePath, boolean saveDebugImages) {
        System.out.println("Reading image: " + imagePath);
        if (imagePath == null || imagePath.isBlank()) {
            throw new IllegalArgumentException("The OMR image path is empty.");
        }

        Mat input = imread(imagePath);
        if (input == null || input.empty()) {
            if (input != null) {
                input.close();
            }
            throw new IllegalArgumentException("Cannot load OMR image: " + imagePath);
        }

        if (input.channels() != 3) {
            input.close();
            throw new IllegalArgumentException(
                    "Blue-ink detection requires a full-color 3-channel BGR image."
            );
        }

        try {
            try (NormalizedImages normalized = normalizeWithoutBottomMarkers(input)) {
                Mat normalizedColor = normalized.colorImage();
                Mat normalizedBlueMask = normalized.blueMask();

                double pageBlueCoverage = calculateBlueCoverage(normalizedBlueMask);
                double bubbleBlueCoverage = calculateBubbleBlueCoverage(normalizedBlueMask);

                System.out.printf(Locale.US, "Page blue coverage: %.6f%n", pageBlueCoverage);
                System.out.printf(Locale.US, "Bubble blue coverage: %.6f%n", bubbleBlueCoverage);

                if (pageBlueCoverage < MINIMUM_PAGE_BLUE_COVERAGE) {
                    throw new IllegalStateException(
                            "No usable blue ink was detected. Use a dark blue ballpoint pen "
                                    + "and a full-color image."
                    );
                }

                Mat debug = normalizedColor.clone();
                try {
                    putText(
                            debug,
                            "SPECIAL OMR - FALLBACK ALIGNMENT",
                            new Point(205, 24),
                            FONT_HERSHEY_SIMPLEX,
                            0.45,
                            new Scalar(0, 0, 255, 0),
                            1,
                            LINE_AA,
                            false
                    );

                    RollNumberResult rollNumber = readRollNumber(normalizedBlueMask, debug);
                    List<QuestionResult> questions = new ArrayList<>(TOTAL_QUESTIONS);

                    for (int questionNumber = 1;
                         questionNumber <= TOTAL_QUESTIONS;
                         questionNumber++) {

                        int zeroBasedQuestion = questionNumber - 1;
                        int columnIndex = zeroBasedQuestion / QUESTIONS_PER_COLUMN;
                        int rowIndex = zeroBasedQuestion % QUESTIONS_PER_COLUMN;
                        int y = FIRST_ROW_Y + rowIndex * QUESTION_GAP_Y;

                        questions.add(readQuestion(
                                normalizedBlueMask,
                                debug,
                                questionNumber,
                                columnIndex,
                                QUESTION_OPTION_X[columnIndex],
                                y
                        ));
                    }

                    if (saveDebugImages) {
                        imwrite("C:\\Users\\hilal\\GradedAttendance\\debugOmr\\" + new File(imagePath).getName() + "special_normalized_debug.png", debug);
                    }

                    return new OmrResult(
                            rollNumber,
                            List.copyOf(questions),
                            normalizedColor.clone(),
                            normalizedBlueMask.clone(),
                            debug
                    );
                } catch (RuntimeException exception) {
                    debug.close();
                    throw exception;
                }
            }
        } finally {
            input.close();
        }
    }

    private static NormalizedImages normalizeWithoutBottomMarkers(Mat input) {
        if (input == null || input.empty()) {
            throw new IllegalArgumentException("Cannot normalize an empty OMR image.");
        }
        if (input.channels() != 3) {
            throw new IllegalArgumentException(
                    "Fallback normalization requires a 3-channel BGR image."
            );
        }
        if (input.cols() < MINIMUM_INPUT_WIDTH || input.rows() < MINIMUM_INPUT_HEIGHT) {
            throw new IllegalArgumentException(
                    "OMR image resolution is too low: " + input.cols() + " x " + input.rows()
                            + ". Minimum is " + MINIMUM_INPUT_WIDTH + " x " + MINIMUM_INPUT_HEIGHT + "."
            );
        }
        if (input.cols() >= input.rows()) {
            throw new IllegalArgumentException("Fallback normalization requires a portrait image.");
        }

        double actualRatio = input.cols() / (double) input.rows();
        double ratioError = Math.abs(actualRatio - TARGET_PAGE_RATIO) / TARGET_PAGE_RATIO;
        if (ratioError > MAXIMUM_CROPPABLE_RATIO_ERROR) {
            throw new IllegalArgumentException(String.format(
                    Locale.US,
                    "Fallback normalization rejected the image. Expected ratio %.4f, "
                            + "received %.4f, difference %.2f%%; safe limit is %.0f%%.",
                    TARGET_PAGE_RATIO,
                    actualRatio,
                    ratioError * 100.0,
                    MAXIMUM_CROPPABLE_RATIO_ERROR * 100.0
            ));
        }

        CropArea crop = calculateCenteredCrop(input.cols(), input.rows(), actualRatio);
        Mat croppedColor = null;
        Mat fullResolutionBlueMask = null;
        Mat normalizedColor = new Mat();
        Mat normalizedBlueMask = new Mat();
        boolean success = false;

        try {
            System.out.printf(
                    Locale.US,
                    "Fallback crop: input=%dx%d, crop=(x=%d, y=%d, width=%d, height=%d), "
                            + "ratio %.4f -> %.4f%n",
                    input.cols(), input.rows(),
                    crop.x(), crop.y(), crop.width(), crop.height(),
                    actualRatio, crop.width() / (double) crop.height()
            );

            try (Mat cropView = new Mat(input, new Rect(
                    crop.x(), crop.y(), crop.width(), crop.height()))) {
                croppedColor = cropView.clone();
            }

            if (croppedColor.empty()) {
                throw new IllegalStateException("Fallback crop produced an empty image.");
            }

            fullResolutionBlueMask = makeBlueInkMask(croppedColor);
            int colorInterpolation =
                    croppedColor.cols() > PAGE_WIDTH || croppedColor.rows() > PAGE_HEIGHT
                            ? INTER_AREA
                            : INTER_CUBIC;

            resize(croppedColor, normalizedColor,
                    new Size(PAGE_WIDTH, PAGE_HEIGHT), 0.0, 0.0, colorInterpolation);
            resize(fullResolutionBlueMask, normalizedBlueMask,
                    new Size(PAGE_WIDTH, PAGE_HEIGHT), 0.0, 0.0, INTER_NEAREST);

            threshold(normalizedBlueMask, normalizedBlueMask, 127, 255, THRESH_BINARY);

            try (Mat kernel = getStructuringElement(MORPH_ELLIPSE, new Size(3, 3))) {
                morphologyEx(normalizedBlueMask, normalizedBlueMask, MORPH_CLOSE, kernel);
            }

            if (normalizedColor.empty()
                    || normalizedColor.cols() != PAGE_WIDTH
                    || normalizedColor.rows() != PAGE_HEIGHT) {
                throw new IllegalStateException("Failed to produce an 800 x 1200 color image.");
            }
            if (normalizedBlueMask.empty()
                    || normalizedBlueMask.cols() != PAGE_WIDTH
                    || normalizedBlueMask.rows() != PAGE_HEIGHT) {
                throw new IllegalStateException("Failed to produce an 800 x 1200 blue mask.");
            }

            success = true;
            return new NormalizedImages(normalizedColor, normalizedBlueMask);
        } finally {
            if (croppedColor != null) {
                croppedColor.close();
            }
            if (fullResolutionBlueMask != null) {
                fullResolutionBlueMask.close();
            }
            if (!success) {
                normalizedColor.close();
                normalizedBlueMask.close();
            }
        }
    }

    private static CropArea calculateCenteredCrop(int inputWidth, int inputHeight, double actualRatio) {
        int cropX = 0;
        int cropY = 0;
        int cropWidth = inputWidth;
        int cropHeight = inputHeight;

        if (actualRatio > TARGET_PAGE_RATIO) {
            cropWidth = Math.min(inputWidth, (int) Math.round(inputHeight * TARGET_PAGE_RATIO));
            int maximumX = inputWidth - cropWidth;
            cropX = clampInt(maximumX / 2 + FALLBACK_HORIZONTAL_CROP_OFFSET, 0, maximumX);
        } else if (actualRatio < TARGET_PAGE_RATIO) {
            cropHeight = Math.min(inputHeight, (int) Math.round(inputWidth / TARGET_PAGE_RATIO));
            int maximumY = inputHeight - cropHeight;
            cropY = clampInt(maximumY / 2 + FALLBACK_VERTICAL_CROP_OFFSET, 0, maximumY);
        }

        if (cropWidth <= 0 || cropHeight <= 0
                || cropX < 0 || cropY < 0
                || cropX + cropWidth > inputWidth
                || cropY + cropHeight > inputHeight) {
            throw new IllegalStateException("Calculated fallback crop is outside image bounds.");
        }

        return new CropArea(cropX, cropY, cropWidth, cropHeight);
    }

    private static Mat makeBlueInkMask(Mat colorImage) {
        if (colorImage == null || colorImage.empty() || colorImage.channels() != 3) {
            throw new IllegalArgumentException("Blue mask requires a non-empty 3-channel BGR image.");
        }

        Mat hsv = new Mat();
        Mat blueMask = new Mat();
        try {
            cvtColor(colorImage, hsv, COLOR_BGR2HSV);

            try (
                    Mat lower = new Mat(hsv.rows(), hsv.cols(), hsv.type(),
                            new Scalar(85, 25, 10, 0));
                    Mat upper = new Mat(hsv.rows(), hsv.cols(), hsv.type(),
                            new Scalar(150, 255, 255, 0))
            ) {
                inRange(hsv, lower, upper, blueMask);
            }

            try (Mat kernel = getStructuringElement(MORPH_ELLIPSE, new Size(3, 3))) {
                morphologyEx(blueMask, blueMask, MORPH_CLOSE, kernel);
            }
            return blueMask;
        } catch (RuntimeException exception) {
            blueMask.close();
            throw exception;
        } finally {
            hsv.close();
        }
    }

    private static RollNumberResult readRollNumber(Mat blueMask, Mat debug) {
        StringBuilder value = new StringBuilder(ROLL_DIGITS);
        List<RollDigitResult> results = new ArrayList<>(ROLL_DIGITS);
        boolean valid = true;

        for (int position = 0; position < ROLL_DIGITS; position++) {
            double[] scores = new double[ROLL_VALUES];
            BubbleLocation[] locations = new BubbleLocation[ROLL_VALUES];

            for (int valueIndex = 0; valueIndex < ROLL_VALUES; valueIndex++) {
                BubbleMeasurement measurement = measureBubbleWithLocalSearch(
                        blueMask,
                        ROLL_COLUMN_X[valueIndex],
                        ROLL_VALUE_Y[position],
                        ROLL_SCORE_RADIUS
                );
                scores[valueIndex] = measurement.score();
                locations[valueIndex] = new BubbleLocation(
                        measurement.centerX(), measurement.centerY());
            }

            double adaptiveThreshold = Math.max(
                    ROLL_ABSOLUTE_MINIMUM,
                    average(scores) + ROLL_RELATIVE_OFFSET
            );
            boolean[] marked = classifyBlueMarks(
                    scores,
                    adaptiveThreshold,
                    ROLL_ABSOLUTE_MINIMUM,
                    ROLL_MULTIPLE_RELATIVE_FACTOR
            );

            int count = countTrue(marked);
            char digit;
            double confidence;

            if (count == 0) {
                digit = '-';
                confidence = 0.0;
            } else if (count > 1) {
                digit = '?';
                confidence = 0.0;
            } else {
                int selected = firstTrueIndex(marked);
                double selectedScore = scores[selected];
                double competingScore = highestOtherScore(scores, selected);
                double margin = selectedScore - competingScore;

                confidence = calculateConfidence(
                        selectedScore, competingScore, adaptiveThreshold);

                if (selectedScore < ROLL_ABSOLUTE_MINIMUM
                        || margin < MINIMUM_WINNING_MARGIN
                        || confidence < STRICT_CONFIDENCE_MINIMUM) {
                    digit = '-';
                    confidence = 0.0;
                    clearMarks(marked);
                } else {
                    digit = (char) ('1' + selected);
                }
            }

            if (digit < '1' || digit > '9') {
                valid = false;
            }

            value.append(digit);
            results.add(new RollDigitResult(
                    position + 1, digit, scores, adaptiveThreshold, confidence, false));
            drawRollDigitDebug(debug, position, locations, marked, digit);
        }

        drawRollResult(debug, value.toString(), valid);
        return new RollNumberResult(value.toString(), valid, results);
    }

    private static QuestionResult readQuestion(
            Mat blueMask,
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] optionX,
            int y
    ) {
        double[] scores = new double[4];
        BubbleLocation[] locations = new BubbleLocation[4];

        for (int option = 0; option < 4; option++) {
            BubbleMeasurement measurement = measureBubbleWithLocalSearch(
                    blueMask, optionX[option], y, QUESTION_SCORE_RADIUS);
            scores[option] = measurement.score();
            locations[option] = new BubbleLocation(
                    measurement.centerX(), measurement.centerY());
        }

        RankedScores ranking = rank(scores);
        double adaptiveThreshold = Math.max(
                QUESTION_ABSOLUTE_MINIMUM,
                average(scores) + QUESTION_RELATIVE_OFFSET
        );
        boolean[] marked = classifyBlueMarks(
                scores,
                adaptiveThreshold,
                QUESTION_ABSOLUTE_MINIMUM,
                QUESTION_MULTIPLE_RELATIVE_FACTOR
        );

        int count = countTrue(marked);
        char answer;
        double confidence;

        if (count == 0) {
            answer = '-';
            confidence = 0.0;
        } else if (count > 1) {
            answer = '+';
            confidence = 0.0;
        } else {
            int selected = firstTrueIndex(marked);
            double selectedScore = scores[selected];
            double competingScore = highestOtherScore(scores, selected);
            double margin = selectedScore - competingScore;

            confidence = calculateConfidence(
                    selectedScore, competingScore, adaptiveThreshold);

            if (selectedScore < QUESTION_ABSOLUTE_MINIMUM
                    || margin < MINIMUM_WINNING_MARGIN
                    || confidence < STRICT_CONFIDENCE_MINIMUM) {
                answer = '-';
                confidence = 0.0;
                clearMarks(marked);
            } else {
                answer = (char) ('A' + selected);
            }
        }

        drawQuestionDebug(
                debug, questionNumber, columnIndex, optionX, y,
                locations, marked, answer, ranking.bestIndex());

        return new QuestionResult(
                questionNumber, answer, scores, adaptiveThreshold, confidence, false);
    }

    private static boolean[] classifyBlueMarks(
            double[] scores,
            double adaptiveThreshold,
            double absoluteMinimum,
            double relativeFactor
    ) {
        boolean[] marked = new boolean[scores.length];
        RankedScores ranking = rank(scores);

        if (ranking.bestIndex() < 0) {
            return marked;
        }

        double requiredBestScore = Math.max(absoluteMinimum, adaptiveThreshold);
        if (ranking.bestScore() < requiredBestScore) {
            return marked;
        }

        double secondaryThreshold = Math.max(
                absoluteMinimum,
                ranking.bestScore() * relativeFactor
        );

        int independentlyStrongCount = 0;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] >= secondaryThreshold) {
                marked[index] = true;
                independentlyStrongCount++;
            }
        }

        if (independentlyStrongCount > 1) {
            return marked;
        }

        double winningMargin = ranking.bestScore() - ranking.secondScore();
        if (winningMargin < MINIMUM_WINNING_MARGIN) {
            clearMarks(marked);
            return marked;
        }

        marked[ranking.bestIndex()] = true;
        return marked;
    }

    private static BubbleMeasurement measureBubbleWithLocalSearch(
            Mat blueMask,
            int expectedX,
            int expectedY,
            int radius
    ) {
        double bestAdjustedScore = Double.NEGATIVE_INFINITY;
        double bestActualScore = 0.0;
        int bestX = expectedX;
        int bestY = expectedY;

        for (int dy = -LOCAL_SEARCH_RADIUS; dy <= LOCAL_SEARCH_RADIUS; dy++) {
            for (int dx = -LOCAL_SEARCH_RADIUS; dx <= LOCAL_SEARCH_RADIUS; dx++) {
                int x = expectedX + dx;
                int y = expectedY + dy;
                double actualScore = fillRatio(blueMask, x, y, radius);
                double adjustedScore = actualScore - Math.hypot(dx, dy) * 0.002;

                if (adjustedScore > bestAdjustedScore) {
                    bestAdjustedScore = adjustedScore;
                    bestActualScore = actualScore;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        return new BubbleMeasurement(bestActualScore, bestX, bestY);
    }

    private static double fillRatio(Mat binary, int centerX, int centerY, int radius) {
        int left = Math.max(0, centerX - radius);
        int top = Math.max(0, centerY - radius);
        int right = Math.min(binary.cols() - 1, centerX + radius);
        int bottom = Math.min(binary.rows() - 1, centerY + radius);
        int width = right - left + 1;
        int height = bottom - top + 1;

        if (width <= 0 || height <= 0) {
            return 0.0;
        }

        try (
                Mat roi = new Mat(binary, new Rect(left, top, width, height));
                Mat mask = Mat.zeros(height, width, CV_8UC1).asMat();
                Mat counted = new Mat()
        ) {
            circle(
                    mask,
                    new Point(centerX - left, centerY - top),
                    radius,
                    new Scalar(255),
                    FILLED,
                    LINE_8,
                    0
            );
            bitwise_and(roi, mask, counted);
            int maskPixels = countNonZero(mask);
            return maskPixels == 0 ? 0.0 : countNonZero(counted) / (double) maskPixels;
        }
    }

    private static double calculateBlueCoverage(Mat blueMask) {
        if (blueMask == null || blueMask.empty()) {
            return 0.0;
        }
        double totalPixels = blueMask.rows() * (double) blueMask.cols();
        return totalPixels <= 0.0 ? 0.0 : countNonZero(blueMask) / totalPixels;
    }

    private static double calculateBubbleBlueCoverage(Mat blueMask) {
        double total = 0.0;
        int measured = 0;

        for (int question = 1; question <= TOTAL_QUESTIONS; question++) {
            int zeroBased = question - 1;
            int column = zeroBased / QUESTIONS_PER_COLUMN;
            int row = zeroBased % QUESTIONS_PER_COLUMN;
            int y = FIRST_ROW_Y + row * QUESTION_GAP_Y;

            for (int option = 0; option < 4; option++) {
                total += fillRatio(
                        blueMask,
                        QUESTION_OPTION_X[column][option],
                        y,
                        QUESTION_SCORE_RADIUS
                );
                measured++;
            }
        }

        for (int position = 0; position < ROLL_DIGITS; position++) {
            for (int value = 0; value < ROLL_VALUES; value++) {
                total += fillRatio(
                        blueMask,
                        ROLL_COLUMN_X[value],
                        ROLL_VALUE_Y[position],
                        ROLL_SCORE_RADIUS
                );
                measured++;
            }
        }

        return measured == 0 ? 0.0 : total / measured;
    }

    private static RankedScores rank(double[] scores) {
        int bestIndex = -1;
        int secondIndex = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        double secondScore = Double.NEGATIVE_INFINITY;

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

        if (!Double.isFinite(bestScore)) {
            bestScore = 0.0;
        }
        if (!Double.isFinite(secondScore)) {
            secondScore = 0.0;
        }
        return new RankedScores(bestIndex, secondIndex, bestScore, secondScore);
    }

    private static double calculateConfidence(
            double selectedScore,
            double secondScore,
            double threshold
    ) {
        selectedScore = clamp(selectedScore, 0.0, 1.0);
        secondScore = clamp(secondScore, 0.0, 1.0);
        threshold = clamp(threshold, 0.0, 1.0);

        if (selectedScore < threshold) {
            return 0.0;
        }

        double thresholdStrength = clamp(
                (selectedScore - threshold) / Math.max(1.0e-9, 1.0 - threshold),
                0.0,
                1.0
        );
        double separation = clamp(selectedScore - secondScore, 0.0, 1.0);
        double dominance = selectedScore <= 1.0e-9
                ? 0.0
                : clamp(1.0 - secondScore / selectedScore, 0.0, 1.0);

        double confidence =
                Math.pow(thresholdStrength, 0.35)
                        * Math.pow(separation, 0.35)
                        * Math.pow(dominance, 0.30);

        return clamp(Math.min(confidence, 0.99), 0.0, 0.99);
    }

    private static void drawQuestionDebug(
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] expectedX,
            int expectedY,
            BubbleLocation[] locations,
            boolean[] marked,
            char answer,
            int bestIndex
    ) {
        Scalar[] optionColors = {
                new Scalar(255, 0, 0, 0),
                new Scalar(0, 180, 0, 0),
                new Scalar(0, 0, 255, 0),
                new Scalar(255, 0, 255, 0)
        };

        for (int option = 0; option < 4; option++) {
            BubbleLocation location = locations[option];
            circle(debug, new Point(location.x(), location.y()),
                    QUESTION_SCORE_RADIUS, optionColors[option], 1, LINE_AA, 0);

            if (location.x() != expectedX[option] || location.y() != expectedY) {
                line(
                        debug,
                        new Point(expectedX[option], expectedY),
                        new Point(location.x(), location.y()),
                        new Scalar(180, 180, 180, 0),
                        1,
                        LINE_AA,
                        0
                );
            }

            if (marked[option]) {
                Scalar selectedColor = answer == '+'
                        ? new Scalar(0, 0, 255, 0)
                        : new Scalar(0, 210, 0, 0);
                circle(debug, new Point(location.x(), location.y()),
                        QUESTION_DEBUG_RADIUS, selectedColor, 2, LINE_AA, 0);
            }
        }

        int labelX = columnIndex == QUESTION_COLUMNS - 1
                ? expectedX[0] - 31
                : expectedX[3] + 12;
        Scalar labelColor = answer == '+'
                ? new Scalar(0, 0, 255, 0)
                : answer == '-'
                ? new Scalar(0, 170, 255, 0)
                : new Scalar(0, 150, 0, 0);

        putText(
                debug,
                "Q" + questionNumber + "=" + answer,
                new Point(labelX, expectedY + 4),
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
            BubbleLocation[] locations,
            boolean[] marked,
            char result
    ) {
        Scalar[] rowColors = {
                new Scalar(255, 0, 0, 0),
                new Scalar(0, 165, 255, 0),
                new Scalar(0, 215, 255, 0),
                new Scalar(255, 0, 255, 0)
        };
        Scalar rowColor = rowColors[position % rowColors.length];

        for (int value = 0; value < ROLL_VALUES; value++) {
            BubbleLocation location = locations[value];
            circle(debug, new Point(location.x(), location.y()),
                    ROLL_SCORE_RADIUS, rowColor, 1, LINE_AA, 0);

            if (marked[value]) {
                Scalar selectedColor = result == '?'
                        ? new Scalar(0, 0, 255, 0)
                        : new Scalar(0, 200, 0, 0);
                circle(debug, new Point(location.x(), location.y()),
                        ROLL_DEBUG_RADIUS, selectedColor, 3, LINE_AA, 0);
            }
        }

        Scalar textColor = result == '?'
                ? new Scalar(0, 0, 255, 0)
                : result == '-'
                ? new Scalar(0, 165, 255, 0)
                : new Scalar(0, 160, 0, 0);

        putText(
                debug,
                String.valueOf(result),
                new Point(ROLL_COLUMN_X[0] - 8, ROLL_VALUE_Y[position] - 13),
                FONT_HERSHEY_SIMPLEX,
                0.35,
                textColor,
                1,
                LINE_AA,
                false
        );
    }

    private static void drawRollResult(Mat debug, String value, boolean valid) {
        putText(
                debug,
                "ROLL=" + value,
                new Point(510, 310),
                FONT_HERSHEY_SIMPLEX,
                0.38,
                valid ? new Scalar(0, 150, 0, 0) : new Scalar(0, 0, 255, 0),
                1,
                LINE_AA,
                false
        );
    }

    private static String confidenceStatus(char result, double confidence) {
        if (result == '-' || result == '+' || result == '?') {
            return "INVALID";
        }
        if (confidence >= STRICT_CONFIDENCE_MINIMUM) {
            return "ACCEPTED";
        }
        if (confidence >= REVIEW_CONFIDENCE_MINIMUM) {
            return "REVIEW";
        }
        return "UNRELIABLE";
    }

    private static int[] createRollColumnXCoordinates() {
        int[] coordinates = new int[ROLL_VALUES];
        for (int index = 0; index < ROLL_VALUES; index++) {
            coordinates[index] = ROLL_FIRST_COLUMN_X + index * ROLL_COLUMN_GAP_X;
        }
        return coordinates;
    }

    private static int[] createRollValueYCoordinates() {
        int[] coordinates = new int[ROLL_DIGITS];
        for (int position = 0; position < ROLL_DIGITS; position++) {
            coordinates[position] = ROLL_FIRST_ROW_Y + position * ROLL_ROW_GAP_Y;
        }
        return coordinates;
    }

    private static double average(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return total / values.length;
    }

    private static int countTrue(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private static int firstTrueIndex(boolean[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index]) {
                return index;
            }
        }
        return -1;
    }

    private static double highestOtherScore(double[] scores, int selectedIndex) {
        double highest = 0.0;
        for (int index = 0; index < scores.length; index++) {
            if (index != selectedIndex) {
                highest = Math.max(highest, scores[index]);
            }
        }
        return highest;
    }

    private static void clearMarks(boolean[] marked) {
        if (marked != null) {
            Arrays.fill(marked, false);
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record CropArea(int x, int y, int width, int height) {
    }

    private record RankedScores(
            int bestIndex,
            int secondIndex,
            double bestScore,
            double secondScore
    ) {
    }

    private record BubbleMeasurement(double score, int centerX, int centerY) {
    }

    private record BubbleLocation(int x, int y) {
    }

    private record NormalizedImages(Mat colorImage, Mat blueMask) implements AutoCloseable {
        @Override
        public void close() {
            if (colorImage != null) {
                colorImage.close();
            }
            if (blueMask != null) {
                blueMask.close();
            }
        }
    }

    public record RollDigitResult(
            int position,
            char value,
            double[] scores,
            double threshold,
            double confidence,
            boolean faintRecovery
    ) {
        public RollDigitResult {
            scores = scores == null ? new double[0] : scores.clone();
        }

        @Override
        public double[] scores() {
            return scores.clone();
        }
    }

    public record RollNumberResult(
            String value,
            boolean valid,
            List<RollDigitResult> digits
    ) {
        public RollNumberResult {
            value = value == null ? "" : value;
            digits = digits == null ? List.of() : List.copyOf(digits);
        }
    }

    public record QuestionResult(
            int questionNumber,
            char answer,
            double[] scores,
            double threshold,
            double confidence,
            boolean faintRecovery
    ) {
        public QuestionResult {
            scores = scores == null ? new double[0] : scores.clone();
        }

        @Override
        public double[] scores() {
            return scores.clone();
        }

        public boolean blank() {
            return answer == '-';
        }

        public boolean multiple() {
            return answer == '+';
        }

        public boolean hasSingleAnswer() {
            return answer >= 'A' && answer <= 'D';
        }
    }

    /**
     * Owns its three native Mat objects and must be closed.
     */
    public record OmrResult(
            RollNumberResult rollNumber,
            List<QuestionResult> questions,
            Mat normalizedImage,
            Mat blueMaskImage,
            Mat debugImage
    ) implements AutoCloseable {

        public OmrResult {
            rollNumber = rollNumber == null
                    ? new RollNumberResult("", false, List.of())
                    : rollNumber;
            questions = questions == null ? List.of() : List.copyOf(questions);
        }

        public QuestionResult question(int questionNumber) {
            if (questionNumber < 1 || questionNumber > questions.size()) {
                throw new IllegalArgumentException(
                        "Question number must be between 1 and " + questions.size()
                );
            }
            return questions.get(questionNumber - 1);
        }


        @Override
        public void close() {
            if (normalizedImage != null) {
                normalizedImage.close();
            }
            if (blueMaskImage != null) {
                blueMaskImage.close();
            }
            if (debugImage != null) {
                debugImage.close();
            }
        }
    }

    public record OmrJsonResult(
            List<StudentResult> students,
            List<String> notes
    ) {
    }

    public record StudentResult(
            String enrollmentNumber,
            List<AnswerResult> answers
    ) {
    }

    public record AnswerResult(
            int questionNumber,
            char option
    ) {
    }
}
