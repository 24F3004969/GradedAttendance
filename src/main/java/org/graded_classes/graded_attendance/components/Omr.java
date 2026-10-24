package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Fixed-template blue-ink OMR scanner.
 * <p>
 * Expected layout:
 * - Normalized page: 800 x 1200
 * - Four black registration markers
 * - Six roll-number columns
 * - Roll values 0 through 6
 * - Ninety questions
 * - Three columns of thirty questions
 * - Four options: A, B, C and D
 * <p>
 * Rules:
 * - Blue ballpoint pen is required.
 * - Questions return A/B/C/D, '-' for blank, '+' for multiple.
 * - Roll digits return 0-6, '-' for blank, '?' for multiple.
 */
public final class Omr {

    // =========================================================
    // Normalized page dimensions
    // =========================================================
    private static final double QUESTION_ABSOLUTE_MINIMUM = 0.35;
    private static final double ROLL_ABSOLUTE_MINIMUM = 0.35;
    private static final int PAGE_WIDTH = 800;
    private static final int PAGE_HEIGHT = 1200;
    private static final int ROLL_DIGITS = 4;
    private static final int ROLL_VALUES = 10;

    private static final int ROLL_FIRST_COLUMN_X = 415;
    private static final int ROLL_COLUMN_GAP_X = 33;

    private static final int ROLL_FIRST_ROW_Y = 148;
    private static final int ROLL_ROW_GAP_Y = 30;
    /*
     * Destination registration-marker centers.
     *
     * Printed markers:
     * Top-left:     x=30,  y=30,   width=40, height=40
     * Top-right:    x=730, y=30,   width=40, height=40
     * Bottom-left:  x=30,  y=1130, width=40, height=40
     * Bottom-right: x=730, y=1130, width=40, height=40
     *
     * Therefore, the marker centers are:
     * (50,50), (750,50), (50,1150), (750,1150)
     */
    private static final Point2d DEST_TOP_LEFT =
            new Point2d(50, 50);

    private static final Point2d DEST_TOP_RIGHT =
            new Point2d(750, 50);

    private static final Point2d DEST_BOTTOM_RIGHT =
            new Point2d(750, 1150);

    private static final Point2d DEST_BOTTOM_LEFT =
            new Point2d(50, 1150);

    // =========================================================
    // Question layout
    // =========================================================

    private static final int TOTAL_QUESTIONS = 90;
    private static final int QUESTIONS_PER_COLUMN = 30;
    private static final int QUESTION_COLUMNS = 3;

    private static final int FIRST_ROW_Y = 350;
    private static final int QUESTION_GAP_Y = 25;
    private static final double STRICT_CONFIDENCE_MINIMUM = 0.75;
    private static final double REVIEW_CONFIDENCE_MINIMUM = 0.55;
    private static final int[][] QUESTION_OPTION_X = {
            {90, 126, 162, 198},
            {345, 381, 417, 453},
            {600, 636, 672, 708}
    };
// =========================================================
// Roll-number layout
// Physical layout remains exactly as generated
// =========================================================

    /*
     * Four rows represent the four places of the roll number.
     * Nine columns represent selectable digits 1 through 9.
     *
     * Example: 1649
     *
     * Row 1 -> column 1
     * Row 2 -> column 6
     * Row 3 -> column 4
     * Row 4 -> column 9
     */



    /*
     * Nine X coordinates representing digit values 1 through 9:
     *
     * 1 -> 515
     * 2 -> 548
     * 3 -> 581
     * 4 -> 614
     * 5 -> 647
     * 6 -> 680
     * 7 -> 713
     * 8 -> 746
     * 9 -> 779
     */
    private static final int[] ROLL_COLUMN_X =
            createRollColumnXCoordinates();

    /*
     * Four Y coordinates representing roll-number places:
     *
     * First digit  -> 148
     * Second digit -> 178
     * Third digit  -> 208
     * Fourth digit -> 238
     */
    private static final int[] ROLL_VALUE_Y =
            createRollValueYCoordinates();

    private static int[] createRollColumnXCoordinates() {
        int[] coordinates = new int[ROLL_VALUES];

        for (int valueIndex = 0;
             valueIndex < ROLL_VALUES;
             valueIndex++) {

            coordinates[valueIndex] =
                    ROLL_FIRST_COLUMN_X
                            + valueIndex * ROLL_COLUMN_GAP_X;
        }

        return coordinates;
    }

    private static int[] createRollValueYCoordinates() {
        int[] coordinates = new int[ROLL_DIGITS];

        for (int position = 0;
             position < ROLL_DIGITS;
             position++) {

            coordinates[position] =
                    ROLL_FIRST_ROW_Y
                            + position * ROLL_ROW_GAP_Y;
        }

        return coordinates;
    }

    // =========================================================
    // Bubble-scoring configuration
    // =========================================================

    private static final int QUESTION_SCORE_RADIUS = 7;
    private static final int ROLL_SCORE_RADIUS = 7;

    private static final int QUESTION_DEBUG_RADIUS = 12;
    private static final int ROLL_DEBUG_RADIUS = 12;

    /*
     * Search within +/-4 pixels of every expected bubble center.
     */
    private static final int LOCAL_SEARCH_RADIUS = 4;

    /*
     * Blue-mask question thresholds.
     *
     * These are lower than grayscale thresholds because black
     * printing is mostly absent from the blue mask.
     */
    private static final double QUESTION_RELATIVE_OFFSET = 0.08;
    private static final double ROLL_RELATIVE_OFFSET = 0.08;

    private static final double QUESTION_FAINT_MINIMUM = 0.045;
    private static final double QUESTION_FAINT_MARGIN = 0.035;

    /*
     * Blue-mask roll-number thresholds.
     */


    private static final double ROLL_FAINT_MINIMUM = 0.045;
    private static final double ROLL_FAINT_MARGIN = 0.035;

    /*
     * Independent multiple-mark detection.
     *
     * A second bubble must pass the absolute minimum and contain
     * at least this fraction of the strongest bubble's score.
     */
    private static final double QUESTION_MULTIPLE_RELATIVE_FACTOR = 0.55;
    private static final double ROLL_MULTIPLE_RELATIVE_FACTOR = 0.55;

    /*
     * Reject an image when practically no blue pixels exist.
     */
    private static final double MINIMUM_PAGE_BLUE_COVERAGE = 0.00008;

    private Omr() {
    }

    // =========================================================
    // Test entry point
    // =========================================================

    public static void main(String[] args) {

        String inputPath =
                args.length > 0
                        ? args[0]
                        : "omr5.png";

        try (OmrResult result = read(inputPath, true)) {

            System.out.printf(
                    "Roll/Enrollment No: %s (%s)%n",
                    result.rollNumber().value(),
                    result.rollNumber().valid()
                            ? "VALID"
                            : "INVALID"
            );

            for (QuestionResult question : result.questions()) {

                double[] scores = question.scores();

                System.out.printf(
                        Locale.US,
                        "Q%02d -> %s "
                                + "[A=%.3f B=%.3f C=%.3f D=%.3f, "
                                + "threshold=%.3f, confidence=%.3f, status=%s]%n",
                        question.questionNumber(),
                        question.answer(),
                        scores[0],
                        scores[1],
                        scores[2],
                        scores[3],
                        question.threshold(),
                        question.confidence(),
                        confidenceStatus(
                                question.answer(),
                                question.confidence(),
                                question.faintRecovery()
                        )
                );
            }

            System.out.println();
            System.out.println(result.toMachineReadableLine());

            System.out.println();
            System.out.println(
                    "Normalized color: normalized_original.png"
            );
            System.out.println(
                    "Blue mask:       normalized_blue_mask.png"
            );
            System.out.println(
                    "Debug image:     normalized_debug.png"
            );
        }
    }

    // =========================================================
    // Main scanning method
    // =========================================================

    public static OmrResult read(
            String imagePath,
            boolean saveDebugImages
    ) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "The OMR image path is empty."
            );
        }

        Mat input = imread(imagePath);

        if (input == null || input.empty()) {
            if (input != null) {
                input.close();
            }

            throw new IllegalArgumentException(
                    "Cannot load OMR image: " + imagePath
            );
        }

        if (input.channels() != 3) {
            input.close();

            throw new IllegalArgumentException(
                    "Blue-ink detection requires a full-color image. "
                            + "Expected 3 BGR channels."
            );
        }

        try {
            SheetMarkers sourceMarkers =
                    findRegistrationMarkers(input);

            try (
                    NormalizedImages normalizedImages =
                            normalizeAll(
                                    input,
                                    sourceMarkers
                            )
            ) {
                Mat normalizedColor =
                        normalizedImages.colorImage();

                Mat normalizedBlueMask =
                        normalizedImages.blueMask();

                double pageBlueCoverage =
                        calculateBlueCoverage(
                                normalizedBlueMask
                        );

                double bubbleBlueCoverage =
                        calculateBubbleBlueCoverage(
                                normalizedBlueMask
                        );

                System.out.printf(
                        Locale.US,
                        "Page blue coverage: %.6f%n",
                        pageBlueCoverage
                );

                System.out.printf(
                        Locale.US,
                        "Bubble blue coverage: %.6f%n",
                        bubbleBlueCoverage
                );

                if (pageBlueCoverage
                        < MINIMUM_PAGE_BLUE_COVERAGE) {

                    throw new IllegalStateException(
                            "No usable blue ink was detected. "
                                    + "Use a dark blue ballpoint pen, "
                                    + "capture the sheet in full color, "
                                    + "and disable grayscale or "
                                    + "black-and-white scan filters."
                    );
                }

                Mat debug = normalizedColor.clone();

                try {
                    RollNumberResult rollNumber =
                            readRollNumber(
                                    normalizedBlueMask,
                                    debug
                            );

                    List<QuestionResult> questions =
                            new ArrayList<>(
                                    TOTAL_QUESTIONS
                            );

                    for (
                            int questionNumber = 1;
                            questionNumber <= TOTAL_QUESTIONS;
                            questionNumber++
                    ) {
                        int zeroBasedQuestion =
                                questionNumber - 1;

                        int columnIndex =
                                zeroBasedQuestion
                                        / QUESTIONS_PER_COLUMN;

                        int rowIndex =
                                zeroBasedQuestion
                                        % QUESTIONS_PER_COLUMN;

                        int y =
                                FIRST_ROW_Y
                                        + rowIndex
                                        * QUESTION_GAP_Y;

                        QuestionResult question =
                                readQuestion(
                                        normalizedBlueMask,
                                        debug,
                                        questionNumber,
                                        columnIndex,
                                        QUESTION_OPTION_X[
                                                columnIndex
                                                ],
                                        y
                                );

                        questions.add(question);
                    }

                    if (saveDebugImages) {
                        imwrite(
                                "normalized_original.png",
                                normalizedColor
                        );

                        imwrite(
                                "normalized_blue_mask.png",
                                normalizedBlueMask
                        );

                        imwrite(
                                "normalized_debug.png",
                                debug
                        );
                    }

                    /*
                     * NormalizedImages closes its own Mats when its
                     * try block ends. Therefore, clones are returned
                     * and become owned by OmrResult.
                     */
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

    // =========================================================
    // Registration-marker detection
    // =========================================================

    private static SheetMarkers findRegistrationMarkers(
            Mat input
    ) {
        try (
                Mat gray = new Mat();
                Mat blurred = new Mat();
                Mat binary = new Mat();
                MatVector contours = new MatVector()
        ) {
            if (input.channels() == 1) {
                input.copyTo(gray);
            } else {
                cvtColor(
                        input,
                        gray,
                        COLOR_BGR2GRAY
                );
            }

            GaussianBlur(
                    gray,
                    blurred,
                    new Size(5, 5),
                    0
            );

            threshold(
                    blurred,
                    binary,
                    0,
                    255,
                    THRESH_BINARY_INV | THRESH_OTSU
            );

            int minimumSide =
                    Math.min(
                            input.cols(),
                            input.rows()
                    );

            int kernelSize =
                    Math.max(
                            3,
                            minimumSide / 250
                    );

            if (kernelSize % 2 == 0) {
                kernelSize++;
            }

            try (
                    Mat kernel = getStructuringElement(
                            MORPH_RECT,
                            new Size(
                                    kernelSize,
                                    kernelSize
                            )
                    )
            ) {
                morphologyEx(
                        binary,
                        binary,
                        MORPH_CLOSE,
                        kernel
                );
            }

            try (Mat contourInput = binary.clone()) {
                findContours(
                        contourInput,
                        contours,
                        RETR_EXTERNAL,
                        CHAIN_APPROX_SIMPLE
                );
            }

            List<MarkerCandidate> candidates =
                    new ArrayList<>();

            double imageArea =
                    input.cols()
                            * (double) input.rows();

            for (long index = 0;
                 index < contours.size();
                 index++) {

                Mat contour = contours.get(index);

                if (contour == null || contour.empty()) {
                    continue;
                }

                Rect rect = boundingRect(contour);

                if (rect.width()
                        < minimumSide * 0.025
                        || rect.height()
                        < minimumSide * 0.025) {
                    continue;
                }

                double area =
                        contourArea(contour);

                double relativeArea =
                        area / imageArea;

                double aspect =
                        rect.height() == 0
                                ? 0.0
                                : rect.width()
                                / (double) rect.height();

                double rectangleArea =
                        rect.width()
                                * (double) rect.height();

                double extent =
                        rectangleArea <= 0.0
                                ? 0.0
                                : area / rectangleArea;

                if (relativeArea < 0.0007
                        || relativeArea > 0.03) {
                    continue;
                }

                if (aspect < 0.60
                        || aspect > 1.45) {
                    continue;
                }

                if (extent < 0.60) {
                    continue;
                }

                /*
                 * Use contour moments instead of bounding-box center.
                 * This handles perspective-distorted markers better.
                 */
                Moments contourMoments =
                        moments(contour);

                double m00 =
                        contourMoments.m00();

                if (Math.abs(m00) < 1.0e-6) {
                    continue;
                }

                double centerX =
                        contourMoments.m10() / m00;

                double centerY =
                        contourMoments.m01() / m00;

                candidates.add(
                        new MarkerCandidate(
                                new Point2d(
                                        centerX,
                                        centerY
                                ),
                                area,
                                aspect,
                                extent
                        )
                );
            }

            if (candidates.size() < 4) {
                throw new IllegalStateException(
                        "Could not find all four registration "
                                + "markers. Found "
                                + candidates.size()
                                + ". Ensure all four black squares "
                                + "are completely visible."
                );
            }

            MarkerCandidate topLeft =
                    nearestCorner(
                            candidates,
                            0,
                            0,
                            input.cols(),
                            input.rows()
                    );

            MarkerCandidate topRight =
                    nearestCorner(
                            candidates,
                            input.cols(),
                            0,
                            input.cols(),
                            input.rows()
                    );

            MarkerCandidate bottomRight =
                    nearestCorner(
                            candidates,
                            input.cols(),
                            input.rows(),
                            input.cols(),
                            input.rows()
                    );

            MarkerCandidate bottomLeft =
                    nearestCorner(
                            candidates,
                            0,
                            input.rows(),
                            input.cols(),
                            input.rows()
                    );

            ensureUniqueMarkers(
                    topLeft,
                    topRight,
                    bottomRight,
                    bottomLeft
            );

            return new SheetMarkers(
                    copyPoint(topLeft.center()),
                    copyPoint(topRight.center()),
                    copyPoint(bottomRight.center()),
                    copyPoint(bottomLeft.center())
            );
        }
    }

    private static MarkerCandidate nearestCorner(
            List<MarkerCandidate> candidates,
            double cornerX,
            double cornerY,
            int imageWidth,
            int imageHeight
    ) {
        double maximumDistance =
                Math.hypot(
                        imageWidth,
                        imageHeight
                ) * 0.45;

        return candidates.stream()
                .filter(candidate -> {
                    double distance = Math.hypot(
                            candidate.center().x()
                                    - cornerX,
                            candidate.center().y()
                                    - cornerY
                    );

                    return distance
                            < maximumDistance;
                })
                .min(
                        Comparator.comparingDouble(
                                candidate -> {

                                    double dx =
                                            (
                                                    candidate
                                                            .center()
                                                            .x()
                                                            - cornerX
                                            ) / imageWidth;

                                    double dy =
                                            (
                                                    candidate
                                                            .center()
                                                            .y()
                                                            - cornerY
                                            ) / imageHeight;

                                    double distance =
                                            Math.hypot(
                                                    dx,
                                                    dy
                                            );

                                    double shapePenalty =
                                            Math.abs(
                                                    1.0
                                                            - candidate
                                                            .aspect()
                                            ) * 0.04;

                                    double extentBonus =
                                            candidate.extent()
                                                    * 0.02;

                                    double relativeArea =
                                            candidate.area()
                                                    / (
                                                    imageWidth
                                                            * (double)
                                                            imageHeight
                                            );

                                    double areaBonus =
                                            Math.min(
                                                    relativeArea,
                                                    0.01
                                            );

                                    return distance
                                            + shapePenalty
                                            - extentBonus
                                            - areaBonus;
                                }
                        )
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No registration marker found "
                                        + "near image corner "
                                        + cornerX
                                        + ","
                                        + cornerY
                        )
                );
    }

    private static void ensureUniqueMarkers(
            MarkerCandidate topLeft,
            MarkerCandidate topRight,
            MarkerCandidate bottomRight,
            MarkerCandidate bottomLeft
    ) {
        if (topLeft == topRight
                || topLeft == bottomRight
                || topLeft == bottomLeft
                || topRight == bottomRight
                || topRight == bottomLeft
                || bottomRight == bottomLeft) {

            throw new IllegalStateException(
                    "The same detected contour was selected "
                            + "for more than one registration marker."
            );
        }
    }

    private static Point2d copyPoint(
            Point2d point
    ) {
        return new Point2d(
                point.x(),
                point.y()
        );
    }

    // =========================================================
    // Full-resolution blue extraction
    // =========================================================

    private static Mat makeBlueInkMask(
            Mat colorImage
    ) {
        if (colorImage == null
                || colorImage.empty()) {

            throw new IllegalArgumentException(
                    "Cannot make a blue mask from an empty image."
            );
        }

        if (colorImage.channels() != 3) {
            throw new IllegalArgumentException(
                    "Blue detection requires a 3-channel BGR image."
            );
        }

        Mat hsv = new Mat();
        Mat blueMask = new Mat();

        try {
            cvtColor(
                    colorImage,
                    hsv,
                    COLOR_BGR2HSV
            );

            /*
             * JavaCPP exposes the following overload:
             *
             * inRange(Mat src, Mat lower, Mat upper, Mat dst)
             *
             * Therefore, full-sized lower and upper Mats are used
             * instead of Scalar arguments.
             *
             * HSV range:
             * H: 85-150
             * S: 25-255
             * V: 10-255
             */
            try (
                    Mat lowerBound = new Mat(
                            hsv.rows(),
                            hsv.cols(),
                            hsv.type(),
                            new Scalar(
                                    85,
                                    25,
                                    10,
                                    0
                            )
                    );

                    Mat upperBound = new Mat(
                            hsv.rows(),
                            hsv.cols(),
                            hsv.type(),
                            new Scalar(
                                    150,
                                    255,
                                    255,
                                    0
                            )
                    )
            ) {
                inRange(
                        hsv,
                        lowerBound,
                        upperBound,
                        blueMask
                );
            }

            /*
             * Join small gaps in ballpoint marks.
             *
             * MORPH_OPEN is deliberately not used because it can
             * erase faint and thin blue strokes.
             */
            try (
                    Mat kernel = getStructuringElement(
                            MORPH_ELLIPSE,
                            new Size(3, 3)
                    )
            ) {
                morphologyEx(
                        blueMask,
                        blueMask,
                        MORPH_CLOSE,
                        kernel
                );
            }

            return blueMask;

        } catch (RuntimeException exception) {
            blueMask.close();
            throw exception;

        } finally {
            hsv.close();
        }
    }

    // =========================================================
    // Normalize color image and blue mask
    // =========================================================

    private static NormalizedImages normalizeAll(
            Mat input,
            SheetMarkers markers
    ) {
        /*
         * Blue extraction happens before normalization so resizing
         * cannot desaturate the blue ink.
         */
        Mat fullResolutionBlueMask =
                makeBlueInkMask(input);

        try (
                Point2f source = new Point2f(4);
                Point2f destination = new Point2f(4)
        ) {
            setPoint(
                    source,
                    0,
                    markers.topLeft()
            );

            setPoint(
                    source,
                    1,
                    markers.topRight()
            );

            setPoint(
                    source,
                    2,
                    markers.bottomRight()
            );

            setPoint(
                    source,
                    3,
                    markers.bottomLeft()
            );

            source.position(0);

            setPoint(
                    destination,
                    0,
                    DEST_TOP_LEFT
            );

            setPoint(
                    destination,
                    1,
                    DEST_TOP_RIGHT
            );

            setPoint(
                    destination,
                    2,
                    DEST_BOTTOM_RIGHT
            );

            setPoint(
                    destination,
                    3,
                    DEST_BOTTOM_LEFT
            );

            destination.position(0);

            try (
                    Mat transform =
                            getPerspectiveTransform(
                                    source,
                                    destination
                            )
            ) {
                Mat normalizedColor =
                        new Mat();

                Mat normalizedBlueMask =
                        new Mat();

                try {
                    warpPerspective(
                            input,
                            normalizedColor,
                            transform,
                            new Size(
                                    PAGE_WIDTH,
                                    PAGE_HEIGHT
                            ),
                            INTER_CUBIC,
                            BORDER_CONSTANT,
                            new Scalar(
                                    255,
                                    255,
                                    255,
                                    0
                            )
                    );

                    /*
                     * Binary masks must be transformed with
                     * INTER_NEAREST to prevent interpolation from
                     * weakening white foreground pixels.
                     */
                    warpPerspective(
                            fullResolutionBlueMask,
                            normalizedBlueMask,
                            transform,
                            new Size(
                                    PAGE_WIDTH,
                                    PAGE_HEIGHT
                            ),
                            INTER_NEAREST,
                            BORDER_CONSTANT,
                            new Scalar(0)
                    );

                    try (
                            Mat kernel =
                                    getStructuringElement(
                                            MORPH_ELLIPSE,
                                            new Size(3, 3)
                                    )
                    ) {
                        morphologyEx(
                                normalizedBlueMask,
                                normalizedBlueMask,
                                MORPH_CLOSE,
                                kernel
                        );
                    }

                    return new NormalizedImages(
                            normalizedColor,
                            normalizedBlueMask
                    );

                } catch (RuntimeException exception) {
                    normalizedColor.close();
                    normalizedBlueMask.close();
                    throw exception;
                }
            }

        } finally {
            fullResolutionBlueMask.close();
        }
    }

    private static void setPoint(
            Point2f points,
            int index,
            Point2d value
    ) {
        points.position(index)
                .x((float) value.x())
                .y((float) value.y());
    }

    // =========================================================
    // Blue-mask validation
    // =========================================================

    private static double calculateBlueCoverage(
            Mat blueMask
    ) {
        if (blueMask == null
                || blueMask.empty()) {
            return 0.0;
        }

        double totalPixels =
                blueMask.rows()
                        * (double) blueMask.cols();

        if (totalPixels <= 0.0) {
            return 0.0;
        }

        return countNonZero(blueMask)
                / totalPixels;
    }

    private static double calculateBubbleBlueCoverage(
            Mat blueMask
    ) {
        double totalScore = 0.0;
        int measuredBubbles = 0;

        for (
                int questionNumber = 1;
                questionNumber <= TOTAL_QUESTIONS;
                questionNumber++
        ) {
            int zeroBasedQuestion =
                    questionNumber - 1;

            int columnIndex =
                    zeroBasedQuestion
                            / QUESTIONS_PER_COLUMN;

            int rowIndex =
                    zeroBasedQuestion
                            % QUESTIONS_PER_COLUMN;

            int y =
                    FIRST_ROW_Y
                            + rowIndex
                            * QUESTION_GAP_Y;

            for (int option = 0;
                 option < 4;
                 option++) {

                totalScore += fillRatio(
                        blueMask,
                        QUESTION_OPTION_X[
                                columnIndex
                                ][option],
                        y,
                        QUESTION_SCORE_RADIUS
                );

                measuredBubbles++;
            }
        }

        for (int position = 0;
             position < ROLL_DIGITS;
             position++) {

            for (int valueIndex = 0;
                 valueIndex < ROLL_VALUES;
                 valueIndex++) {

                totalScore += fillRatio(
                        blueMask,
                        ROLL_COLUMN_X[valueIndex],
                        ROLL_VALUE_Y[position],
                        ROLL_SCORE_RADIUS
                );

                measuredBubbles++;
            }
        }
        return measuredBubbles == 0
                ? 0.0
                : totalScore / measuredBubbles;
    }

    // =========================================================
    // Roll-number scanning
    // =========================================================

    private static RollNumberResult readRollNumber(
            Mat blueMask,
            Mat debug
    ) {
        StringBuilder rollNumber =
                new StringBuilder(ROLL_DIGITS);

        List<RollDigitResult> digitResults =
                new ArrayList<>(ROLL_DIGITS);

        boolean valid = true;

        /*
         * Each horizontal row represents one place in the
         * four-digit roll number.
         *
         * Row 0 = first roll-number digit
         * Row 1 = second roll-number digit
         * Row 2 = third roll-number digit
         * Row 3 = fourth roll-number digit
         */
        for (int position = 0;
             position < ROLL_DIGITS;
             position++) {

            /*
             * Each row contains nine selectable digit columns.
             *
             * scores[0] = digit 1
             * scores[1] = digit 2
             * ...
             * scores[8] = digit 9
             */
            double[] scores =
                    new double[ROLL_VALUES];

            BubbleLocation[] locations =
                    new BubbleLocation[ROLL_VALUES];

            /*
             * Scan all nine digit columns across the current row.
             */
            for (int valueIndex = 0;
                 valueIndex < ROLL_VALUES;
                 valueIndex++) {

                BubbleMeasurement measurement =
                        measureBubbleWithLocalSearch(
                                blueMask,

                                // Column decides the digit value 1-9.
                                ROLL_COLUMN_X[valueIndex],

                                // Row decides the place in the roll number.
                                ROLL_VALUE_Y[position],

                                ROLL_SCORE_RADIUS
                        );

                scores[valueIndex] =
                        measurement.score();

                locations[valueIndex] =
                        new BubbleLocation(
                                measurement.centerX(),
                                measurement.centerY()
                        );
            }

            RankedScores ranking =
                    rank(scores);

            double rowAverage =
                    average(scores);

            double adaptiveThreshold =
                    Math.max(
                            ROLL_ABSOLUTE_MINIMUM,
                            rowAverage
                                    + ROLL_RELATIVE_OFFSET
                    );

            boolean[] marked =
                    classifyBlueMarks(
                            scores,
                            adaptiveThreshold,
                            ROLL_ABSOLUTE_MINIMUM,
                            ROLL_MULTIPLE_RELATIVE_FACTOR
                    );

            int markedCount =
                    countTrue(marked);

            char digit;
            double confidence;
            boolean faintRecovery = false;

            /*
             * No bubble passed the normal threshold.
             * Attempt faint-mark recovery.
             */
            if (markedCount == 0) {
                digit = '-';
                confidence = 0.0;
                faintRecovery = false;

            } else if (markedCount > 1) {
                digit = '?';
                confidence = 0.0;
                faintRecovery = false;

            } else {
                int selectedValueIndex =
                        firstTrueIndex(marked);

                double selectedScore =
                        scores[selectedValueIndex];

                double competingScore =
                        highestOtherScore(
                                scores,
                                selectedValueIndex
                        );

                double winningMargin =
                        selectedScore - competingScore;

                if (selectedScore < 0.30
                        || winningMargin < 0.15) {

                    digit = '-';
                    confidence = 0.0;
                    faintRecovery = false;

                } else {
                    digit = (char) ('0' + selectedValueIndex);


                    confidence =
                            calculateConfidence(
                                    selectedScore,
                                    competingScore,
                                    adaptiveThreshold,
                                    false
                            );

                    if (confidence < 0.75) {
                        digit = '-';
                        confidence = 0.0;
                    }
                }
            }
            /*
             * Only 1 through 9 are valid.
             *
             * A blank '-' or multiple mark '?' makes the
             * complete roll number invalid.
             */
            if (digit < '0' || digit > '9') {
                valid = false;
            }

            rollNumber.append(digit);

            digitResults.add(
                    new RollDigitResult(
                            position + 1,
                            digit,
                            scores,
                            adaptiveThreshold,
                            confidence,
                            faintRecovery
                    )
            );

            drawRollDigitDebug(
                    debug,
                    position,
                    locations,
                    marked,
                    digit,
                    faintRecovery
            );
        }

        drawRollResult(
                debug,
                rollNumber.toString(),
                valid
        );

        return new RollNumberResult(
                rollNumber.toString(),
                valid,
                digitResults
        );
    }

    // =========================================================
    // Question scanning
    // Question scanning
    // =========================================================

    private static QuestionResult readQuestion(
            Mat blueMask,
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] optionX,
            int y
    ) {
        double[] scores = new double[4];

        BubbleLocation[] locations =
                new BubbleLocation[4];

        for (int option = 0;
             option < 4;
             option++) {

            BubbleMeasurement measurement =
                    measureBubbleWithLocalSearch(
                            blueMask,
                            optionX[option],
                            y,
                            QUESTION_SCORE_RADIUS
                    );

            scores[option] =
                    measurement.score();

            locations[option] =
                    new BubbleLocation(
                            measurement.centerX(),
                            measurement.centerY()
                    );
        }

        RankedScores ranking =
                rank(scores);

        double rowAverage =
                average(scores);

        double adaptiveThreshold =
                Math.max(
                        QUESTION_ABSOLUTE_MINIMUM,
                        rowAverage
                                + QUESTION_RELATIVE_OFFSET
                );

        boolean[] marked =
                classifyBlueMarks(
                        scores,
                        adaptiveThreshold,
                        QUESTION_ABSOLUTE_MINIMUM,
                        QUESTION_MULTIPLE_RELATIVE_FACTOR
                );

        int markedCount =
                countTrue(marked);

        char answer;
        double confidence;
        boolean faintRecovery = false;

        if (markedCount == 0) {
            /*
             * Strict mode:
             * Never convert a weak or faint mark into an answer.
             *
             * A question with no bubble passing the required thresholds
             * is treated as blank.
             */
            answer = '-';
            confidence = 0.0;
            faintRecovery = false;

        } else if (markedCount > 1) {
            /*
             * More than one sufficiently strong bubble was detected.
             */
            answer = '+';
            confidence = 0.0;
            faintRecovery = false;

        } else {
            int selectedOption =
                    firstTrueIndex(marked);

            double selectedScore =
                    scores[selectedOption];

            double competingScore =
                    highestOtherScore(
                            scores,
                            selectedOption
                    );

            double winningMargin =
                    selectedScore - competingScore;

            /*
             * Final strict validation.
             *
             * The mark must:
             * 1. Have at least 30% blue fill.
             * 2. Be at least 15 percentage points stronger than
             *    every competing bubble.
             */
            boolean strongEnough =
                    selectedScore >= 0.30;

            boolean separatedEnough =
                    winningMargin >= 0.15;

            if (!strongEnough || !separatedEnough) {
                answer = '-';
                confidence = 0.0;
                faintRecovery = false;

            } else {
                answer = (char) (
                        'A' + selectedOption
                );

                confidence =
                        calculateConfidence(
                                selectedScore,
                                competingScore,
                                adaptiveThreshold,
                                false
                        );

                /*
                 * Do not retain an answer unless confidence itself
                 * reaches the strict acceptance level.
                 */
                if (confidence < 0.75) {
                    answer = '-';
                    confidence = 0.0;
                }
            }
        }

        drawQuestionDebug(
                debug,
                questionNumber,
                columnIndex,
                optionX,
                y,
                locations,
                marked,
                answer,
                faintRecovery,
                ranking.bestIndex()
        );

        return new QuestionResult(
                questionNumber,
                answer,
                scores,
                adaptiveThreshold,
                confidence,
                faintRecovery
        );
    }

    // =========================================================
    // Local center search
    // =========================================================

    private static BubbleMeasurement measureBubbleWithLocalSearch(
            Mat blueMask,
            int expectedCenterX,
            int expectedCenterY,
            int radius
    ) {
        double bestAdjustedScore =
                Double.NEGATIVE_INFINITY;

        double bestActualScore = 0.0;

        int bestCenterX =
                expectedCenterX;

        int bestCenterY =
                expectedCenterY;

        for (
                int offsetY = -LOCAL_SEARCH_RADIUS;
                offsetY <= LOCAL_SEARCH_RADIUS;
                offsetY++
        ) {
            for (
                    int offsetX = -LOCAL_SEARCH_RADIUS;
                    offsetX <= LOCAL_SEARCH_RADIUS;
                    offsetX++
            ) {
                int currentX =
                        expectedCenterX + offsetX;

                int currentY =
                        expectedCenterY + offsetY;

                double actualScore =
                        fillRatio(
                                blueMask,
                                currentX,
                                currentY,
                                radius
                        );

                double distance =
                        Math.hypot(
                                offsetX,
                                offsetY
                        );

                /*
                 * Penalize unnecessary movement away from the
                 * expected center.
                 */
                double adjustedScore =
                        actualScore
                                - distance * 0.002;

                if (adjustedScore
                        > bestAdjustedScore) {

                    bestAdjustedScore =
                            adjustedScore;

                    bestActualScore =
                            actualScore;

                    bestCenterX =
                            currentX;

                    bestCenterY =
                            currentY;
                }
            }
        }

        return new BubbleMeasurement(
                bestActualScore,
                bestCenterX,
                bestCenterY
        );
    }

    // =========================================================
    // Blue-pixel fill ratio
    // =========================================================

    private static double fillRatio(
            Mat binary,
            int centerX,
            int centerY,
            int radius
    ) {
        int x =
                Math.max(
                        0,
                        centerX - radius
                );

        int y =
                Math.max(
                        0,
                        centerY - radius
                );

        int width =
                Math.min(
                        radius * 2 + 1,
                        binary.cols() - x
                );

        int height =
                Math.min(
                        radius * 2 + 1,
                        binary.rows() - y
                );

        if (width <= 0 || height <= 0) {
            return 0.0;
        }

        try (
                Mat roi = new Mat(
                        binary,
                        new Rect(
                                x,
                                y,
                                width,
                                height
                        )
                );

                Mat mask = Mat.zeros(
                        height,
                        width,
                        CV_8UC1
                ).asMat();

                Mat counted = new Mat()
        ) {
            circle(
                    mask,
                    new Point(
                            centerX - x,
                            centerY - y
                    ),
                    radius,
                    new Scalar(255),
                    FILLED,
                    LINE_8,
                    0
            );

            bitwise_and(
                    roi,
                    mask,
                    counted
            );

            int maskPixels =
                    countNonZero(mask);

            if (maskPixels == 0) {
                return 0.0;
            }

            return countNonZero(counted)
                    / (double) maskPixels;
        }
    }

    // =========================================================
    // Mark classification
    // =========================================================

    private static boolean[] classifyBlueMarks(
            double[] scores,
            double adaptiveThreshold,
            double absoluteMinimum,
            double relativeFactor
    ) {
        boolean[] marked =
                new boolean[scores.length];

        RankedScores ranking =
                rank(scores);

        if (ranking.bestIndex() < 0) {
            return marked;
        }

        double requiredBestScore =
                Math.max(
                        absoluteMinimum,
                        adaptiveThreshold
                );

        /*
         * The strongest mark must independently pass the strict
         * absolute and adaptive thresholds.
         */
        if (ranking.bestScore() < requiredBestScore) {
            return marked;
        }

        /*
         * Reject weakly separated marks.
         */
        double winningMargin =
                ranking.bestScore()
                        - ranking.secondScore();

        if (winningMargin < 0.15) {
            return marked;
        }

        marked[ranking.bestIndex()] = true;

        /*
         * A second mark counts as multiple only when it is itself
         * strong. This prevents small amounts of blue noise from
         * producing a multiple-mark result.
         */
        double secondaryThreshold =
                Math.max(
                        absoluteMinimum,
                        ranking.bestScore()
                                * relativeFactor
                );

        for (int index = 0;
             index < scores.length;
             index++) {

            if (index == ranking.bestIndex()) {
                continue;
            }

            if (scores[index] >= secondaryThreshold) {
                marked[index] = true;
            }
        }

        return marked;
    }

    // =========================================================
    // Score helpers
    // =========================================================

    private static RankedScores rank(
            double[] scores
    ) {
        int bestIndex = -1;
        int secondIndex = -1;

        double bestScore =
                Double.NEGATIVE_INFINITY;

        double secondScore =
                Double.NEGATIVE_INFINITY;

        for (int index = 0;
             index < scores.length;
             index++) {

            double score =
                    scores[index];

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

        return new RankedScores(
                bestIndex,
                secondIndex,
                bestScore,
                secondScore
        );
    }

    private static double average(
            double[] values
    ) {
        if (values == null
                || values.length == 0) {
            return 0.0;
        }

        double total = 0.0;

        for (double value : values) {
            total += value;
        }

        return total / values.length;
    }

    private static int countTrue(
            boolean[] values
    ) {
        int count = 0;

        for (boolean value : values) {
            if (value) {
                count++;
            }
        }

        return count;
    }

    private static int firstTrueIndex(
            boolean[] values
    ) {
        for (int index = 0;
             index < values.length;
             index++) {

            if (values[index]) {
                return index;
            }
        }

        return -1;
    }

    private static double highestOtherScore(
            double[] scores,
            int selectedIndex
    ) {
        double highest = 0.0;

        for (int index = 0;
             index < scores.length;
             index++) {

            if (index == selectedIndex) {
                continue;
            }

            highest = Math.max(
                    highest,
                    scores[index]
            );
        }

        return highest;
    }

    private static double calculateConfidence(
            double selectedScore,
            double secondScore,
            double threshold,
            boolean faintRecovery
    ) {
        selectedScore = clamp(selectedScore, 0.0, 1.0);
        secondScore = clamp(secondScore, 0.0, 1.0);
        threshold = clamp(threshold, 0.0, 1.0);

        if (selectedScore < threshold) {
            return 0.0;
        }

        /*
         * Measures how far the selected bubble is above the required
         * threshold.
         *
         * A score only slightly above the threshold receives a low value.
         */
        double thresholdStrength =
                (selectedScore - threshold)
                        / Math.max(1.0e-9, 1.0 - threshold);

        thresholdStrength = clamp(
                thresholdStrength,
                0.0,
                1.0
        );

        /*
         * Measures the absolute difference between the strongest
         * and second-strongest bubble.
         */
        double separation =
                clamp(
                        selectedScore - secondScore,
                        0.0,
                        1.0
                );

        /*
         * Measures how small the second bubble is relative to the
         * selected bubble.
         *
         * For example:
         * selected = 1.000
         * second   = 0.114
         * dominance = 0.886
         */
        double dominance;

        if (selectedScore <= 1.0e-9) {
            dominance = 0.0;
        } else {
            dominance = clamp(
                    1.0 - secondScore / selectedScore,
                    0.0,
                    1.0
            );
        }

        /*
         * Strict weighted confidence:
         *
         * 35% mark strength above threshold
         * 35% absolute separation
         * 30% relative dominance
         *
         * Using multiplication prevents one excellent measurement
         * from hiding a weak measurement.
         */
        double confidence =
                Math.pow(thresholdStrength, 0.35)
                        * Math.pow(separation, 0.35)
                        * Math.pow(dominance, 0.30);

        /*
         * Faint recovery should never receive normal confidence.
         */
        if (faintRecovery) {
            confidence *= 0.55;
            confidence = Math.min(confidence, 0.65);
        }

        /*
         * Avoid displaying perfect confidence because image-based
         * OMR detection is never absolutely certain.
         */
        confidence = Math.min(confidence, 0.99);

        return clamp(
                confidence,
                0.0,
                0.99
        );
    }
    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    // =========================================================
    // Question debug drawing
    // =========================================================

    private static void drawQuestionDebug(
            Mat debug,
            int questionNumber,
            int columnIndex,
            int[] expectedOptionX,
            int expectedY,
            BubbleLocation[] locations,
            boolean[] marked,
            char answer,
            boolean faintRecovery,
            int bestIndex
    ) {
        Scalar[] optionColors = {
                new Scalar(255, 0, 0, 0),
                new Scalar(0, 180, 0, 0),
                new Scalar(0, 0, 255, 0),
                new Scalar(255, 0, 255, 0)
        };

        for (int option = 0;
             option < 4;
             option++) {

            BubbleLocation location =
                    locations[option];

            circle(
                    debug,
                    new Point(
                            location.x(),
                            location.y()
                    ),
                    QUESTION_SCORE_RADIUS,
                    optionColors[option],
                    1,
                    LINE_AA,
                    0
            );

            /*
             * Show displacement from expected position.
             */
            if (location.x()
                    != expectedOptionX[option]
                    || location.y()
                    != expectedY) {

                line(
                        debug,
                        new Point(
                                expectedOptionX[option],
                                expectedY
                        ),
                        new Point(
                                location.x(),
                                location.y()
                        ),
                        new Scalar(
                                180,
                                180,
                                180,
                                0
                        ),
                        1,
                        LINE_AA,
                        0
                );
            }

            if (marked[option]) {
                Scalar selectionColor =
                        answer == '+'
                                ? new Scalar(
                                0,
                                140,
                                255,
                                0
                        )
                                : new Scalar(
                                0,
                                210,
                                0,
                                0
                        );

                circle(
                        debug,
                        new Point(
                                location.x(),
                                location.y()
                        ),
                        QUESTION_DEBUG_RADIUS,
                        selectionColor,
                        2,
                        LINE_AA,
                        0
                );
            }
        }

        if (faintRecovery
                && bestIndex >= 0
                && bestIndex
                < locations.length) {

            BubbleLocation location =
                    locations[bestIndex];

            circle(
                    debug,
                    new Point(
                            location.x(),
                            location.y()
                    ),
                    QUESTION_DEBUG_RADIUS,
                    new Scalar(
                            0,
                            200,
                            255,
                            0
                    ),
                    2,
                    LINE_AA,
                    0
            );
        }

        int labelX =
                expectedOptionX[3] + 12;

        if (columnIndex
                == QUESTION_COLUMNS - 1) {

            labelX =
                    expectedOptionX[0] - 31;
        }

        Scalar labelColor;

        if (answer == '+') {
            labelColor =
                    new Scalar(
                            0,
                            0,
                            255,
                            0
                    );

        } else if (answer == '-') {
            labelColor =
                    new Scalar(
                            0,
                            170,
                            255,
                            0
                    );

        } else if (faintRecovery) {
            labelColor =
                    new Scalar(
                            0,
                            200,
                            255,
                            0
                    );

        } else {
            labelColor =
                    new Scalar(
                            0,
                            150,
                            0,
                            0
                    );
        }

        putText(
                debug,
                "Q" + questionNumber
                        + "=" + answer,
                new Point(
                        labelX,
                        expectedY + 4
                ),
                FONT_HERSHEY_SIMPLEX,
                0.24,
                labelColor,
                1,
                LINE_AA,
                false
        );
    }

    // =========================================================
    // Roll-number debug drawing
    // =========================================================

    private static void drawRollDigitDebug(
            Mat debug,
            int position,
            BubbleLocation[] locations,
            boolean[] marked,
            char result,
            boolean faintRecovery
    ) {
        Scalar[] positionColors = {
                new Scalar(255, 0, 0, 0),
                new Scalar(0, 165, 255, 0),
                new Scalar(0, 215, 255, 0),
                new Scalar(255, 0, 255, 0)
        };

        Scalar positionColor =
                positionColors[
                        position % positionColors.length
                        ];

        /*
         * Draw all nine selectable digit locations
         * in the current position row.
         */
        for (int valueIndex = 0;
             valueIndex < ROLL_VALUES;
             valueIndex++) {

            BubbleLocation location =
                    locations[valueIndex];

            circle(
                    debug,
                    new Point(
                            location.x(),
                            location.y()
                    ),
                    ROLL_SCORE_RADIUS,
                    positionColor,
                    1,
                    LINE_AA,
                    0
            );

            if (marked[valueIndex]) {
                Scalar selectedColor =
                        result == '?'
                                ? new Scalar(0, 0, 255, 0)
                                : new Scalar(0, 200, 0, 0);

                circle(
                        debug,
                        new Point(
                                location.x(),
                                location.y()
                        ),
                        ROLL_DEBUG_RADIUS,
                        selectedColor,
                        3,
                        LINE_AA,
                        0
                );
            }
        }

        /*
         * Highlight a faint-recovered mark.
         */
        if (faintRecovery
                && result >= '0'
                && result <= '9') {

            int selectedValueIndex =
                    result - '0';

            if (selectedValueIndex >= 0
                    && selectedValueIndex < locations.length) {

                BubbleLocation location =
                        locations[selectedValueIndex];

                circle(
                        debug,
                        new Point(
                                location.x(),
                                location.y()
                        ),
                        ROLL_DEBUG_RADIUS,
                        new Scalar(0, 165, 255, 0),
                        3,
                        LINE_AA,
                        0
                );
            }
        }

        Scalar textColor;

        if (result == '?') {
            textColor =
                    new Scalar(0, 0, 255, 0);
        } else if (result == '-' || faintRecovery) {
            textColor =
                    new Scalar(0, 165, 255, 0);
        } else {
            textColor =
                    new Scalar(0, 160, 0, 0);
        }

        /*
         * Display the detected digit at the left side
         * of the corresponding position row.
         */
        putText(
                debug,
                String.valueOf(result),
                new Point(
                        ROLL_COLUMN_X[0] - 8,
                        ROLL_VALUE_Y[position] - 13
                ),
                FONT_HERSHEY_SIMPLEX,
                0.35,
                textColor,
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
                        ? new Scalar(
                        0,
                        150,
                        0,
                        0
                )
                        : new Scalar(
                        0,
                        0,
                        255,
                        0
                ),
                1,
                LINE_AA,
                false
        );
    }

    // =========================================================
    // Internal records
    // =========================================================

    private record MarkerCandidate(
            Point2d center,
            double area,
            double aspect,
            double extent
    ) {
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

    private record BubbleMeasurement(
            double score,
            int centerX,
            int centerY
    ) {
    }

    private record BubbleLocation(
            int x,
            int y
    ) {
    }

    private record NormalizedImages(
            Mat colorImage,
            Mat blueMask
    ) implements AutoCloseable {

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

    // =========================================================
    // Public result records
    // =========================================================

    public record RollDigitResult(
            int position,
            char value,
            double[] scores,
            double threshold,
            double confidence,
            boolean faintRecovery
    ) {
        public RollDigitResult {
            scores = scores == null
                    ? new double[0]
                    : scores.clone();
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
            value = value == null
                    ? ""
                    : value;

            digits = digits == null
                    ? List.of()
                    : List.copyOf(digits);
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
            scores = scores == null
                    ? new double[0]
                    : scores.clone();
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
            return answer >= 'A'
                    && answer <= 'D';
        }
    }

    /**
     * OmrResult owns all three native Mat objects.
     * <p>
     * Always close the result:
     * <p>
     * try (OmrResult result = Omr.read(path, true)) {
     * ...
     * }
     */
    public record OmrResult(
            RollNumberResult rollNumber,
            List<QuestionResult> questions,
            Mat normalizedImage,
            Mat blueMaskImage,
            Mat debugImage
    ) implements AutoCloseable {

        public OmrResult {
            questions = questions == null
                    ? List.of()
                    : List.copyOf(questions);
        }

        public QuestionResult question(
                int questionNumber
        ) {
            if (questionNumber < 1
                    || questionNumber
                    > questions.size()) {

                throw new IllegalArgumentException(
                        "Question number must be between 1 and "
                                + questions.size()
                );
            }

            return questions.get(
                    questionNumber - 1
            );
        }

        /**
         * Output format:
         * <p>
         * #roll_no|question_no:option,...|#
         */
        public String toMachineReadableLine() {

            StringBuilder output =
                    new StringBuilder();

            output.append('#')
                    .append(rollNumber.value())
                    .append('|');

            for (int index = 0;
                 index < questions.size();
                 index++) {

                if (index > 0) {
                    output.append(',');
                }

                QuestionResult question =
                        questions.get(index);

                output.append(
                                question.questionNumber()
                        )
                        .append(':')
                        .append(question.answer());
            }

            output.append("|#");

            return output.toString();
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
    private static NormalizedImages normalizeWithoutBottomMarkers(Mat input) {
        Mat normalizedColor = new Mat();
        Mat normalizedBlueMask = null;

        try {
            resize(
                    input,
                    normalizedColor,
                    new Size(PAGE_WIDTH, PAGE_HEIGHT),
                    0.0,
                    0.0,
                    INTER_CUBIC
            );

            normalizedBlueMask = makeBlueInkMask(normalizedColor);

            return new NormalizedImages(
                    normalizedColor,
                    normalizedBlueMask
            );

        } catch (RuntimeException exception) {
            normalizedColor.close();

            if (normalizedBlueMask != null) {
                normalizedBlueMask.close();
            }

            throw exception;
        }
    }
    private static String confidenceStatus(
            char result,
            double confidence,
            boolean faintRecovery
    ) {
        if (result == '-' || result == '+' || result == '?') {
            return "INVALID";
        }

        if (faintRecovery) {
            return "REVIEW";
        }

        if (confidence >= STRICT_CONFIDENCE_MINIMUM) {
            return "ACCEPTED";
        }

        if (confidence >= REVIEW_CONFIDENCE_MINIMUM) {
            return "REVIEW";
        }

        return "UNRELIABLE";
    }
}