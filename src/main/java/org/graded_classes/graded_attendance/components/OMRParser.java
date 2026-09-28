package org.graded_classes.graded_attendance.components;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class OMRParser {

    private static final int DEFAULT_TOTAL_QUESTIONS = 90;

    /*
     * Enrollment number format:
     *
     * ED01
     * ED02
     * ED99
     * ED100
     *
     * At least two digits are required after ED.
     */
    private static final String ENROLLMENT_NUMBER_PATTERN = "ED\\d{2,}";

    private OMRParser() {
        // Utility class
    }

    /**
     * Reads an OMR response file containing records such as:
     *
     * #ED01|1:C,2:A,3:B,...,90:D|#
     *
     * @param path path of the OMR text file
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path
    ) {
        return parse(path, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Reads and parses an OMR response file.
     *
     * @param path           path of the OMR text file
     * @param totalQuestions expected number of questions
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path,
            int totalQuestions
    ) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "OMR response file path cannot be empty."
            );
        }

        try {
            String text = Files.readString(Path.of(path));
            return parseText(text, totalQuestions);

        } catch (IOException exception) {
            throw new OMRParseException(
                    "Unable to read OMR response file: " + path,
                    exception
            );
        }
    }

    /**
     * Parses OMR records directly from a String.
     *
     * This method is useful when the response comes from a TextArea,
     * clipboard, API, or AI-generated text rather than a file.
     *
     * @param text OMR response text
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String text
    ) {
        return parseText(text, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Parses OMR records directly from a String.
     *
     * Supported answer values:
     *
     * A, B, C, D = single selected answer
     * -          = unanswered
     * A+B        = multiple marked answers
     *
     * Example:
     *
     * #ED01|1:A,2:B,3:-,4:A+C|#
     *
     * Questions missing from the input are automatically assigned "-".
     *
     * @param text           OMR response text
     * @param totalQuestions expected number of questions
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String text,
            int totalQuestions
    ) {
        if (text == null || text.isBlank()) {
            throw new OMRParseException(
                    "OMR response text is empty."
            );
        }

        if (totalQuestions <= 0) {
            throw new IllegalArgumentException(
                    "Total questions must be greater than zero."
            );
        }

        LinkedHashMap<String, TreeMap<Integer, String>> students =
                new LinkedHashMap<>();

        /*
         * Every valid student record is enclosed between # and #.
         *
         * Example:
         *
         * #ED01|1:A,2:B,3:C|#
         *
         * Splitting by # produces blocks including:
         *
         * ED01|1:A,2:B,3:C|
         */
        String[] blocks = text
                .replace("\r", "")
                .split("#");

        for (String rawBlock : blocks) {
            String block = rawBlock.trim();

            if (block.isBlank()) {
                continue;
            }

            /*
             * Remove the ending pipe before the closing #.
             *
             * ED01|1:A,2:B|
             *
             * becomes:
             *
             * ED01|1:A,2:B
             */
            if (block.endsWith("|")) {
                block = block
                        .substring(0, block.length() - 1)
                        .trim();
            }

            int firstPipe = block.indexOf('|');

            if (firstPipe < 0) {
                throw new OMRParseException(
                        "Invalid OMR record. Missing '|' in: "
                                + block
                );
            }

            String enrollmentNumber = block
                    .substring(0, firstPipe)
                    .trim()
                    .toUpperCase(Locale.ROOT);

            String answerSection = block
                    .substring(firstPipe + 1)
                    .trim();

            validateEnrollmentNumber(enrollmentNumber);

            if (students.containsKey(enrollmentNumber)) {
                throw new OMRParseException(
                        "Duplicate enrollment number found: "
                                + enrollmentNumber
                );
            }

            TreeMap<Integer, String> answers =
                    initializeBlankAnswers(totalQuestions);
            if (!answerSection.isBlank()) {
                parseAnswers(
                        enrollmentNumber,
                        answerSection,
                        answers,
                        totalQuestions
                );
            }

            students.put(enrollmentNumber, answers);

        }

        if (students.isEmpty()) {
            throw new OMRParseException(
                    "No valid OMR student records were found."
            );
        }

        return students;
    }

    /**
     * Parses the comma-separated answer section belonging to one student.
     */
    private static void parseAnswers(
            String enrollmentNumber,
            String answerSection,
            TreeMap<Integer, String> answers,
            int totalQuestions
    ) {
        String[] responseTokens = answerSection.split(",");

        /*
         * A separate set is needed for duplicate detection.
         *
         * Checking answers.get(questionNumber) is insufficient because an
         * explicitly provided "-" is identical to the initial blank value.
         */
        Set<Integer> encounteredQuestions = new LinkedHashSet<>();

        for (String rawToken : responseTokens) {

            String token = rawToken.trim();

            if (token.isBlank()) {
                continue;
            }

            int colonIndex = token.indexOf(':');

            if (colonIndex < 0) {
                throw new OMRParseException(
                        "Invalid response for enrollment number "
                                + enrollmentNumber
                                + ": "
                                + token
                                + ". Expected the format question:option."
                );
            }

            String questionPart = token
                    .substring(0, colonIndex)
                    .trim();

            String optionPart = token
                    .substring(colonIndex + 1)
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (questionPart.isBlank()) {
                throw new OMRParseException(
                        "Question number is missing for enrollment number "
                                + enrollmentNumber
                                + " in response: "
                                + token
                );
            }

            if (optionPart.isBlank()) {
                throw new OMRParseException(
                        "Option is missing for enrollment number "
                                + enrollmentNumber
                                + ", question "
                                + questionPart
                );
            }

            int questionNumber;

            try {
                questionNumber = Integer.parseInt(questionPart);

            } catch (NumberFormatException exception) {
                throw new OMRParseException(
                        "Invalid question number '"
                                + questionPart
                                + "' for enrollment number "
                                + enrollmentNumber,
                        exception
                );
            }

            if (questionNumber < 1 || questionNumber > totalQuestions) {
                throw new OMRParseException(
                        "Question number "
                                + questionNumber
                                + " is outside the allowed range 1-"
                                + totalQuestions
                                + " for enrollment number "
                                + enrollmentNumber
                );
            }

            if (!encounteredQuestions.add(questionNumber)) {
                throw new OMRParseException(
                        "Duplicate response for enrollment number "
                                + enrollmentNumber
                                + ", question "
                                + questionNumber
                );
            }

            if (!isValidOption(optionPart)) {
                throw new OMRParseException(
                        "Invalid option '"
                                + optionPart
                                + "' for enrollment number "
                                + enrollmentNumber
                                + ", question "
                                + questionNumber
                                + ". Allowed values are A, B, C, D, -, "
                                + "or combinations such as A+B."
                );
            }

            answers.put(
                    questionNumber,
                    optionPart
            );
        }
    }

    /**
     * Creates entries for every expected question.
     *
     * Questions missing from the imported response remain "-".
     */
    private static TreeMap<Integer, String> initializeBlankAnswers(
            int totalQuestions
    ) {
        TreeMap<Integer, String> answers = new TreeMap<>();

        for (int questionNumber = 1;
             questionNumber <= totalQuestions;
             questionNumber++) {

            answers.put(questionNumber, "-");
        }

        return answers;
    }

    /**
     * Validates an enrollment number.
     *
     * Valid examples:
     *
     * ED01
     * ED02
     * ED99
     * ED100
     *
     * Invalid examples:
     *
     * ED00
     * ED1
     * 01
     * AB01
     * ED-1
     */
    private static void validateEnrollmentNumber(
            String enrollmentNumber
    ) {
        if (enrollmentNumber == null
                || enrollmentNumber.isBlank()) {

            throw new OMRParseException(
                    "Enrollment number is missing."
            );
        }

        if (!enrollmentNumber.matches(
                ENROLLMENT_NUMBER_PATTERN
        )) {
            throw new OMRParseException(
                    "Invalid enrollment number: "
                            + enrollmentNumber
                            + ". Expected format: ED01, ED02, ED100, etc."
            );
        }

        String numericPart = enrollmentNumber.substring(2);

        try {
            long enrollmentValue = Long.parseLong(numericPart);

            if (enrollmentValue < 1) {
                throw new OMRParseException(
                        "Enrollment number must start from ED01. Found: "
                                + enrollmentNumber
                );
            }

        } catch (NumberFormatException exception) {
            throw new OMRParseException(
                    "Invalid numeric part in enrollment number: "
                            + enrollmentNumber,
                    exception
            );
        }
    }

    /**
     * Checks whether an option is valid.
     *
     * Accepted values:
     *
     * -
     * A
     * B
     * C
     * D
     * A+B
     * A+C+D
     */
    private static boolean isValidOption(String option) {

        if (option == null || option.isBlank()) {
            return false;
        }

        if ("-".equals(option)) {
            return true;
        }

        return option.matches("[A-D](\\+[A-D])*");
    }

    /**
     * Removes duplicate options and arranges multiple answers
     * alphabetically from A to D.
     *
     * Examples:
     *
     * D+A   -> A+D
     * A+A   -> A
     * C+B+A -> A+B+C
     */
    private static String normalizeOption(String option) {
        if ("-".equals(option)) {
            return "-";
        }

        boolean[] selected = new boolean[4];

        for (String value : option.split("\\+")) {
            int index = value.charAt(0) - 'A';
            selected[index] = true;
        }

        StringBuilder result = new StringBuilder();

        for (int index = 0; index < selected.length; index++) {
            if (!selected[index]) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append('+');
            }

            result.append((char) ('A' + index));
        }

        return result.toString();
    }

    /**
     * Converts parsed OMR data back into the required standard format.
     *
     * Example:
     *
     * #ED01|1:A,2:B,3:-,...,90:D|#
     */
    public static String format(
            Map<String, ? extends Map<Integer, String>> students
    ) {
        if (students == null || students.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, ? extends Map<Integer, String>> student
                : students.entrySet()) {

            if (!result.isEmpty()) {
                result.append(System.lineSeparator());
            }

            String enrollmentNumber = student
                    .getKey()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            validateEnrollmentNumber(enrollmentNumber);

            result.append('#')
                    .append(enrollmentNumber)
                    .append('|');

            boolean firstAnswer = true;

            for (Map.Entry<Integer, String> response
                    : student.getValue().entrySet()) {

                if (!firstAnswer) {
                    result.append(',');
                }

                String option = response.getValue();

                if (option == null || option.isBlank()) {
                    option = "-";
                } else {
                    option = option
                            .trim()
                            .toUpperCase(Locale.ROOT);
                }

                if (!isValidOption(option)) {
                    throw new OMRParseException(
                            "Invalid option '"
                                    + option
                                    + "' for enrollment number "
                                    + enrollmentNumber
                                    + ", question "
                                    + response.getKey()
                    );
                }

                result.append(response.getKey())
                        .append(':')
                        .append(normalizeOption(option));

                firstAnswer = false;
            }

            result.append("|#");
        }

        return result.toString();
    }

    /**
     * Returns the number of questions having a selected response.
     *
     * Multiple-marked answers are counted as attempted.
     */
    public static long countAttempted(
            Map<Integer, String> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        return answers.values()
                .stream()
                .filter(option ->
                        option != null
                                && !option.isBlank()
                                && !"-".equals(option)
                )
                .count();
    }

    /**
     * Returns the number of unanswered questions.
     */
    public static long countUnanswered(
            Map<Integer, String> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        return answers.values()
                .stream()
                .filter(option ->
                        option == null
                                || option.isBlank()
                                || "-".equals(option)
                )
                .count();
    }

    /**
     * Returns the number of questions containing multiple selections.
     */
    public static long countMultipleMarked(
            Map<Integer, String> answers
    ) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        return answers.values()
                .stream()
                .filter(option ->
                        option != null && option.contains("+")
                )
                .count();
    }

    /**
     * Runtime exception used for malformed OMR input.
     */
    public static class OMRParseException
            extends RuntimeException {

        public OMRParseException(String message) {
            super(message);
        }

        public OMRParseException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}