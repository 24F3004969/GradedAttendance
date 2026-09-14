package org.graded_classes.graded_attendance.components;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class OMRParser {

    private static final int DEFAULT_TOTAL_QUESTIONS = 90;

    private OMRParser() {
        // Utility class
    }

    /**
     * Reads an OMR response file containing records such as:
     *
     * #124332|1:C,2:A,3:B,...,90:D|#
     *
     * @param path path of the text file
     * @return roll number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path
    ) {
        return parse(path, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Reads and parses an OMR response file.
     *
     * @param path           path of the text file
     * @param totalQuestions expected number of questions
     * @return roll number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path,
            int totalQuestions
    ) {
        try {
            String text = Files.readString(Path.of(path));
            return parseText(text, totalQuestions);
        } catch (IOException e) {
            throw new OMRParseException(
                    "Unable to read OMR response file: " + path,
                    e
            );
        }
    }

    /**
     * Parses OMR records directly from a String.
     *
     * This method is useful when the response comes from a TextArea,
     * clipboard, API, or AI-generated text rather than a file.
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String text
    ) {
        return parseText(text, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Parses OMR records directly from a String.
     *
     * Supported values:
     * A, B, C, D  = single selected answer
     * -           = unanswered
     * A+B         = multiple marked answers
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String text,
            int totalQuestions
    ) {
        if (text == null || text.isBlank()) {
            throw new OMRParseException("OMR response text is empty.");
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
         * #124332|1:A,2:B,3:C|#
         *
         * Splitting by # produces:
         * ""
         * "124332|1:A,2:B,3:C|"
         * ""
         */
        String[] blocks = text
                .replace("\r", "")
                .split("#");

        for (String rawBlock : blocks) {
            String block = rawBlock.trim();

            if (block.isBlank()) {
                continue;
            }

            // Remove the ending | before the closing #.
            if (block.endsWith("|")) {
                block = block.substring(0, block.length() - 1).trim();
            }

            int firstPipe = block.indexOf('|');

            if (firstPipe < 0) {
                throw new OMRParseException(
                        "Invalid OMR record. Missing '|' in: " + block
                );
            }

            String rollNumber = block
                    .substring(0, firstPipe)
                    .trim();

            String answerSection = block
                    .substring(firstPipe + 1)
                    .trim();

            validateRollNumber(rollNumber);

            if (students.containsKey(rollNumber)) {
                throw new OMRParseException(
                        "Duplicate roll number found: " + rollNumber
                );
            }

            TreeMap<Integer, String> answers = initializeBlankAnswers(
                    totalQuestions
            );

            if (!answerSection.isBlank()) {
                parseAnswers(
                        rollNumber,
                        answerSection,
                        answers,
                        totalQuestions
                );
            }

            students.put(rollNumber, answers);
        }

        if (students.isEmpty()) {
            throw new OMRParseException(
                    "No valid OMR student records were found."
            );
        }

        return students;
    }

    private static void parseAnswers(
            String rollNumber,
            String answerSection,
            TreeMap<Integer, String> answers,
            int totalQuestions
    ) {
        String[] responseTokens = answerSection.split(",");

        for (String rawToken : responseTokens) {
            String token = rawToken.trim();

            if (token.isBlank()) {
                continue;
            }

            int colonIndex = token.indexOf(':');

            if (colonIndex < 0) {
                throw new OMRParseException(
                        "Invalid response for roll number "
                                + rollNumber
                                + ": "
                                + token
                                + ". Expected question:option."
                );
            }

            String questionPart = token
                    .substring(0, colonIndex)
                    .trim();

            String optionPart = token
                    .substring(colonIndex + 1)
                    .trim()
                    .toUpperCase(Locale.ROOT);

            int questionNumber;

            try {
                questionNumber = Integer.parseInt(questionPart);
            } catch (NumberFormatException e) {
                throw new OMRParseException(
                        "Invalid question number '"
                                + questionPart
                                + "' for roll number "
                                + rollNumber,
                        e
                );
            }

            if (questionNumber < 1 || questionNumber > totalQuestions) {
                throw new OMRParseException(
                        "Question number "
                                + questionNumber
                                + " is outside the allowed range 1-"
                                + totalQuestions
                                + " for roll number "
                                + rollNumber
                );
            }

            if (!isValidOption(optionPart)) {
                throw new OMRParseException(
                        "Invalid option '"
                                + optionPart
                                + "' for roll number "
                                + rollNumber
                                + ", question "
                                + questionNumber
                );
            }

            /*
             * The question initially contains "-".
             * If the value is no longer "-", the same question appeared twice.
             */
            if (!"-".equals(answers.get(questionNumber))) {
                throw new OMRParseException(
                        "Duplicate response for roll number "
                                + rollNumber
                                + ", question "
                                + questionNumber
                );
            }

            answers.put(questionNumber, normalizeOption(optionPart));
        }
    }

    /**
     * Creates entries for every question.
     *
     * Missing questions automatically remain "-".
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

    private static void validateRollNumber(String rollNumber) {
        if (rollNumber.isBlank()) {
            throw new OMRParseException("Roll number is missing.");
        }

        /*
         * Valid examples:
         * 124332
         * 12----
         * 12?332
         *
         * Digits are normal values.
         * - represents a missing digit.
         * ? represents an ambiguous digit.
         */
        if (!rollNumber.matches("[0-9?-]+")) {
            throw new OMRParseException(
                    "Invalid roll number: "
                            + rollNumber
                            + ". Only digits, '-' and '?' are permitted."
            );
        }
    }
    private static boolean isValidOption(String option) {
        if ("-".equals(option)) {
            return true;
        }

        // Matches a letter A-D, optionally followed by (+ and another letter A-D) repeated
        return option.matches("[A-D](\\+[A-D])*");
    }


    /**
     * Removes duplicate options and puts multiple answers in A-D order.
     *
     * Examples:
     * D+A -> A+D
     * A+A -> A
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

        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                if (!result.isEmpty()) {
                    result.append('+');
                }

                result.append((char) ('A' + i));
            }
        }

        return result.toString();
    }

    /**
     * Converts parsed data back into the required standard format.
     */
    public static String format(
            Map<String, ? extends Map<Integer, String>> students
    ) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, ? extends Map<Integer, String>> student
                : students.entrySet()) {

            if (!result.isEmpty()) {
                result.append(System.lineSeparator());
            }

            result.append('#')
                    .append(student.getKey())
                    .append('|');

            boolean firstAnswer = true;

            for (Map.Entry<Integer, String> response
                    : student.getValue().entrySet()) {

                if (!firstAnswer) {
                    result.append(',');
                }

                result.append(response.getKey())
                        .append(':')
                        .append(response.getValue());

                firstAnswer = false;
            }

            result.append("|#");
        }

        return result.toString();
    }

    /**
     * Returns the number of questions having a selected response.
     * Multiple-marked answers are counted as attempted.
     */
    public static long countAttempted(Map<Integer, String> answers) {
        return answers.values()
                .stream()
                .filter(option -> option != null && !option.equals("-"))
                .count();
    }

    /**
     * Returns the number of unanswered questions.
     */
    public static long countUnanswered(Map<Integer, String> answers) {
        return answers.values()
                .stream()
                .filter(option -> option == null || option.equals("-"))
                .count();
    }

    /**
     * Returns the number of questions containing multiple selections.
     */
    public static long countMultipleMarked(
            Map<Integer, String> answers
    ) {
        return answers.values()
                .stream()
                .filter(option -> option != null && option.contains("+"))
                .count();
    }

    public static class OMRParseException extends RuntimeException {

        public OMRParseException(String message) {
            super(message);
        }

        public OMRParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
