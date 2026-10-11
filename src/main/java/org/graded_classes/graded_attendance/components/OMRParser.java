package org.graded_classes.graded_attendance.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class OMRParser {

    private static final int DEFAULT_TOTAL_QUESTIONS = 90;

    /*
     * Numeric part of the enrollment number (digits only).
     */
    private static final String NUMERIC_PART_PATTERN = "\\d+";

    /*
     * Valid option patterns:
     *
     *   -                       unanswered
     *   A, B, C, D              single answer
     *   A+B, A+C+D, ...         multiple answers
     */
    private static final String OPTION_PATTERN = "[A-D](\\+[A-D])*";

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false
                    );

    private OMRParser() {
        // Utility class
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    /**
     * Reads an OMR JSON file using the default total of 90 questions.
     *
     * @param path path of the OMR JSON file
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path
    ) {
        return parse(path, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Reads an OMR JSON file.
     *
     * Questions missing from the JSON are automatically assigned "-".
     *
     * @param path           path of the OMR JSON file
     * @param totalQuestions expected number of questions in the exam
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parse(
            String path,
            int totalQuestions
    ) {
        validatePath(path);
        validateTotalQuestions(totalQuestions);

        Path jsonPath = requireReadableFile(path);

        try {
            String json = Files.readString(jsonPath);
            return parseText(json, totalQuestions);

        } catch (IOException exception) {
            throw new OMRParseException(
                    "Unable to read OMR JSON file: "
                            + jsonPath.toAbsolutePath(),
                    exception
            );
        }
    }

    /**
     * Parses JSON text using the default total of 90 questions.
     *
     * @param json OMR JSON text
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String json
    ) {
        return parseText(json, DEFAULT_TOTAL_QUESTIONS);
    }

    /**
     * Parses OMR JSON directly from a String.
     *
     * Supported option values:
     *
     * A, B, C, D = single answer
     * -          = blank/unanswered
     * A+B        = multiple marked answers
     *
     * @param json           OMR JSON text
     * @param totalQuestions expected number of questions
     * @return enrollment number -> question number -> selected option
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>> parseText(
            String json,
            int totalQuestions
    ) {
        validateTotalQuestions(totalQuestions);

        OMRFile omrFile = readJson(json);
        return convertToAnswerMap(omrFile, totalQuestions);
    }

    /**
     * Reads all JSON information, including names, filenames,
     * notes, and missing/unparseable page details.
     *
     * Use this method when you need the complete JSON metadata.
     */
    public static OMRFile parseComplete(String path) {
        validatePath(path);

        Path jsonPath = requireReadableFile(path);

        try {
            String json = Files.readString(jsonPath);
            return parseCompleteText(json);

        } catch (IOException exception) {
            throw new OMRParseException(
                    "Unable to read OMR JSON file: "
                            + jsonPath.toAbsolutePath(),
                    exception
            );
        }
    }

    /**
     * Reads the complete JSON structure directly from a String.
     */
    public static OMRFile parseCompleteText(String json) {
        return readJson(json);
    }

    /**
     * Converts the complete OMR JSON structure into the nested map
     * required by the existing exam-processing code.
     */
    public static LinkedHashMap<String, TreeMap<Integer, String>>
    toAnswerMap(OMRFile omrFile, int totalQuestions) {
        validateTotalQuestions(totalQuestions);
        return convertToAnswerMap(omrFile, totalQuestions);
    }

    // ------------------------------------------------------------------
    // Internal parsing helpers
    // ------------------------------------------------------------------

    private static OMRFile readJson(String json) {
        if (json == null || json.isBlank()) {
            throw new OMRParseException("OMR JSON text is empty.");
        }

        try {
            OMRFile omrFile = OBJECT_MAPPER.readValue(json, OMRFile.class);

            if (omrFile == null) {
                throw new OMRParseException(
                        "OMR JSON root object is missing."
                );
            }

            return omrFile;

        } catch (MismatchedInputException exception) {
            throw new OMRParseException(
                    "Invalid OMR JSON structure. Expected a root "
                            + "object containing a 'students' array.",
                    exception
            );

        } catch (JsonProcessingException exception) {
            String location = "";

            if (exception.getLocation() != null) {
                location =
                        " at line "
                                + exception.getLocation().getLineNr()
                                + ", column "
                                + exception.getLocation().getColumnNr();
            }

            throw new OMRParseException(
                    "Malformed OMR JSON"
                            + location
                            + ": "
                            + exception.getOriginalMessage(),
                    exception
            );
        }
    }

    /**
     * Converts the deserialized students into the nested map used
     * by the rest of the application.
     */
    private static LinkedHashMap<String, TreeMap<Integer, String>>
    convertToAnswerMap(OMRFile omrFile, int totalQuestions) {

        if (omrFile == null) {
            throw new OMRParseException("OMR data cannot be null.");
        }

        if (omrFile.students() == null) {
            throw new OMRParseException(
                    "OMR JSON must contain a 'students' array."
            );
        }

        if (omrFile.students().isEmpty()) {
            throw new OMRParseException(
                    "The OMR JSON 'students' array is empty."
            );
        }

        LinkedHashMap<String, TreeMap<Integer, String>> students =
                new LinkedHashMap<>();

        for (int studentIndex = 0;
             studentIndex < omrFile.students().size();
             studentIndex++) {

            StudentResponse student = omrFile.students().get(studentIndex);
            int studentPosition = studentIndex + 1;

            if (student == null) {
                throw new OMRParseException(
                        "Student entry " + studentPosition + " is null."
                );
            }

            String enrollmentNumber =
                    normalizeEnrollmentNumber(student.enrollmentNumber());

            validateEnrollmentNumber(enrollmentNumber);

            if (students.containsKey(enrollmentNumber)) {
                throw new OMRParseException(
                        "Duplicate enrollment number found: "
                                + enrollmentNumber
                );
            }

            TreeMap<Integer, String> answers =
                    initializeBlankAnswers(totalQuestions);

            parseStudentAnswers(
                    enrollmentNumber,
                    student.answers(),
                    answers,
                    totalQuestions
            );

            students.put(enrollmentNumber, answers);
        }

        return students;
    }

    /**
     * Validates and imports the answers belonging to one student.
     */
    private static void parseStudentAnswers(
            String enrollmentNumber,
            List<AnswerResponse> answerResponses,
            TreeMap<Integer, String> answers,
            int totalQuestions
    ) {
        /*
         * A missing or empty answers array means that every question
         * was unanswered.
         */
        if (answerResponses == null || answerResponses.isEmpty()) {
            return;
        }

        Set<Integer> encounteredQuestions = new LinkedHashSet<>();

        for (int answerIndex = 0;
             answerIndex < answerResponses.size();
             answerIndex++) {

            AnswerResponse answerResponse =
                    answerResponses.get(answerIndex);
            int answerPosition = answerIndex + 1;

            if (answerResponse == null) {
                throw new OMRParseException(
                        "Answer entry "
                                + answerPosition
                                + " is null for enrollment number "
                                + enrollmentNumber
                );
            }

            int questionNumber = answerResponse.questionNumber();

            validateQuestionNumber(
                    enrollmentNumber,
                    questionNumber,
                    totalQuestions
            );

            if (!encounteredQuestions.add(questionNumber)) {
                throw new OMRParseException(
                        "Duplicate response for enrollment number "
                                + enrollmentNumber
                                + ", question "
                                + questionNumber
                );
            }

            String option =
                    normalizeInputOption(answerResponse.option());

            if (!isValidOption(option)) {
                throw new OMRParseException(
                        "Invalid option '"
                                + option
                                + "' for enrollment number "
                                + enrollmentNumber
                                + ", question "
                                + questionNumber
                                + ". Allowed values are A, B, C, D, -, "
                                + "or combinations such as A+B."
                );
            }

            answers.put(questionNumber, normalizeOption(option));
        }
    }

    /**
     * Initializes every expected question as unanswered.
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

    // ------------------------------------------------------------------
    // Validation helpers
    // ------------------------------------------------------------------

    private static void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "OMR JSON file path cannot be empty."
            );
        }
    }

    private static void validateTotalQuestions(int totalQuestions) {
        if (totalQuestions <= 0) {
            throw new IllegalArgumentException(
                    "Total questions must be greater than zero."
            );
        }
    }

    private static void validateQuestionNumber(
            String enrollmentNumber,
            int questionNumber,
            int totalQuestions
    ) {
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
    }

    /**
     * Ensures the file exists and is a regular file.
     */
    private static Path requireReadableFile(String path) {
        Path jsonPath = Path.of(path);

        if (!Files.exists(jsonPath)) {
            throw new OMRParseException(
                    "OMR JSON file does not exist: "
                            + jsonPath.toAbsolutePath()
            );
        }

        if (!Files.isRegularFile(jsonPath)) {
            throw new OMRParseException(
                    "OMR JSON path is not a regular file: "
                            + jsonPath.toAbsolutePath()
            );
        }

        return jsonPath;
    }

    /**
     * Normalizes an enrollment number into the canonical form:
     *
     *   - optional "ED" prefix is stripped
     *   - numeric part is parsed as a positive long
     *   - numbers less than 10 are zero-padded to two digits
     *   - the final string is always prefixed with "ED"
     *
     * Examples:
     *
     *   1     -> ED01
     *   01    -> ED01
     *   8     -> ED08
     *   10    -> ED10
     *   100   -> ED100
     *   ED1   -> ED01
     *   ED01  -> ED01
     *   ed8   -> ED08
     *
     * If the input cannot be parsed as a valid enrollment number,
     * the trimmed original value is returned so that validation
     * can produce a meaningful error message.
     */
    private static String normalizeEnrollmentNumber(String enrollmentNumber) {
        if (enrollmentNumber == null) {
            return "";
        }

        String trimmed = enrollmentNumber.trim();

        // Strip optional "ED" prefix (case-insensitive).
        String numericPart = trimmed;
        if (trimmed.length() >= 2
                && trimmed.substring(0, 2).equalsIgnoreCase("ED")) {
            numericPart = trimmed.substring(2);
        }

        if (!numericPart.matches(NUMERIC_PART_PATTERN)) {
            return trimmed; // let validation handle it
        }

        try {
            long value = Long.parseLong(numericPart);

            if (value < 1) {
                return trimmed; // let validation handle it
            }

            String padded = (value < 10)
                    ? String.format("%02d", value)
                    : String.valueOf(value);

            return "ED" + padded;

        } catch (NumberFormatException exception) {
            return trimmed; // let validation handle it
        }
    }

    /**
     * Validates an enrollment number.
     *
     * Accepted forms:
     *
     *   - purely numeric (e.g. 1, 01, 85, 100)
     *   - numeric with an "ED" prefix (case-insensitive)
     *     (e.g. ED01, ed85, Ed100)
     *
     * The numeric part must be greater than zero.
     *
     * Rejected:
     *
     *   0
     *   00
     *   ED0
     *   A85
     *   85A
     *   -85
     *   ED
     *   (blank)
     */
    private static void validateEnrollmentNumber(String enrollmentNumber) {
        if (enrollmentNumber == null || enrollmentNumber.isBlank()) {
            throw new OMRParseException("Enrollment number is missing.");
        }

        String trimmed = enrollmentNumber.trim();

        // Strip optional "ED" prefix (case-insensitive).
        String numericPart = trimmed;
        if (trimmed.length() >= 2
                && trimmed.substring(0, 2).equalsIgnoreCase("ED")) {
            numericPart = trimmed.substring(2);
        }

        if (!numericPart.matches(NUMERIC_PART_PATTERN)) {
            throw new OMRParseException(
                    "Invalid enrollment number: "
                            + enrollmentNumber
                            + ". Expected a positive numeric value "
                            + "optionally prefixed with ED, such as "
                            + "01, 85, ED01, or ED100."
            );
        }

        try {
            long value = Long.parseLong(numericPart);

            if (value < 1) {
                throw new OMRParseException(
                        "Enrollment number must be greater than zero. "
                                + "Found: "
                                + enrollmentNumber
                );
            }

        } catch (NumberFormatException exception) {
            throw new OMRParseException(
                    "Enrollment number is too large or invalid: "
                            + enrollmentNumber,
                    exception
            );
        }
    }

    /**
     * Converts values such as " a + c " to "A+C".
     *
     * Null and blank options are treated as unanswered.
     */
    private static String normalizeInputOption(String option) {
        if (option == null || option.isBlank()) {
            return "-";
        }

        return option
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    /**
     * Checks whether an option is valid.
     *
     * Accepted:
     *
     * -
     * A
     * B
     * C
     * D
     * A+B
     * D+A
     * A+C+D
     *
     * Duplicate values such as A+A are accepted here and normalized
     * to A by {@link #normalizeOption(String)}.
     *
     * Rejected:
     *
     * (blank)
     * E
     * AB
     * +A
     * A+
     * A+B+E
     */
    private static boolean isValidOption(String option) {
        if (option == null || option.isBlank()) {
            return false;
        }

        if ("-".equals(option)) {
            return true;
        }

        return option.matches(OPTION_PATTERN);
    }

    /**
     * Removes duplicate options and arranges multiple answers
     * alphabetically.
     *
     * Examples:
     *
     * D+A   -> A+D
     * A+A   -> A
     * C+B+A -> A+B+C
     *
     * Assumes the option has already passed {@link #isValidOption(String)}.
     */
    private static String normalizeOption(String option) {
        if (option == null || option.isBlank()) {
            return "-";
        }

        if ("-".equals(option)) {
            return "-";
        }

        boolean[] selected = new boolean[4];

        for (String value : option.split("\\+")) {
            if (value.isEmpty()) {
                continue;
            }

            int index = value.charAt(0) - 'A';

            if (index < 0 || index >= selected.length) {
                continue;
            }

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

        return result.length() == 0 ? "-" : result.toString();
    }

    // ------------------------------------------------------------------
    // Formatting / writing
    // ------------------------------------------------------------------

    /**
     * Converts the nested answer map into formatted JSON.
     *
     * Since a basic map does not contain file names or student names,
     * those fields are written as empty strings.
     */
    public static String format(
            Map<String, ? extends Map<Integer, String>> students
    ) {
        OMRFile omrFile = convertMapToOMRFile(students);
        return formatComplete(omrFile);
    }

    /**
     * Converts a complete OMRFile object into formatted JSON.
     *
     * This preserves file names, student names, missing-page details,
     * and notes.
     */
    public static String formatComplete(OMRFile omrFile) {
        if (omrFile == null) {
            throw new IllegalArgumentException("OMR data cannot be null.");
        }

        try {
            return OBJECT_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(omrFile);

        } catch (JsonProcessingException exception) {
            throw new OMRParseException(
                    "Unable to convert OMR data to JSON.",
                    exception
            );
        }
    }

    /**
     * Writes map-based OMR data to a JSON file.
     */
    public static void write(
            String path,
            Map<String, ? extends Map<Integer, String>> students
    ) {
        writeText(path, format(students));
    }

    /**
     * Writes complete OMR data, including metadata, to a JSON file.
     */
    public static void writeComplete(String path, OMRFile omrFile) {
        writeText(path, formatComplete(omrFile));
    }

    private static void writeText(String path, String json) {
        validatePath(path);

        Path outputPath = Path.of(path);

        try {
            Path parent = outputPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(outputPath, json);

        } catch (IOException exception) {
            throw new OMRParseException(
                    "Unable to write OMR JSON file: "
                            + outputPath.toAbsolutePath(),
                    exception
            );
        }
    }

    /**
     * Converts the original nested map into the JSON DTO structure.
     */
    private static OMRFile convertMapToOMRFile(
            Map<String, ? extends Map<Integer, String>> students
    ) {
        if (students == null || students.isEmpty()) {
            return new OMRFile(
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<StudentResponse> studentResponses = new ArrayList<>();

        for (Map.Entry<String, ? extends Map<Integer, String>> studentEntry
                : students.entrySet()) {

            String enrollmentNumber =
                    normalizeEnrollmentNumber(studentEntry.getKey());

            validateEnrollmentNumber(enrollmentNumber);

            List<AnswerResponse> answerResponses = new ArrayList<>();
            Map<Integer, String> answers = studentEntry.getValue();

            if (answers != null) {
                answers.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(answerEntry -> {
                            Integer questionNumber = answerEntry.getKey();

                            if (questionNumber == null || questionNumber < 1) {
                                throw new OMRParseException(
                                        "Invalid question number "
                                                + questionNumber
                                                + " for enrollment number "
                                                + enrollmentNumber
                                );
                            }

                            String option =
                                    normalizeInputOption(answerEntry.getValue());

                            if (!isValidOption(option)) {
                                throw new OMRParseException(
                                        "Invalid option '"
                                                + option
                                                + "' for enrollment number "
                                                + enrollmentNumber
                                                + ", question "
                                                + questionNumber
                                );
                            }

                            answerResponses.add(
                                    new AnswerResponse(
                                            questionNumber,
                                            normalizeOption(option)
                                    )
                            );
                        });
            }

            studentResponses.add(
                    new StudentResponse(
                            "",
                            "",
                            enrollmentNumber,
                            answerResponses
                    )
            );
        }

        return new OMRFile(
                studentResponses,
                List.of(),
                List.of()
        );
    }

    // ------------------------------------------------------------------
    // Statistics helpers
    // ------------------------------------------------------------------

    /**
     * Returns the number of attempted questions.
     *
     * Multiple-marked answers are counted as attempted.
     *
     * Returns 0 if the map is null or empty.
     */
    public static long countAttempted(Map<Integer, String> answers) {
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
     *
     * Returns 0 if the map is null or empty.
     */
    public static long countUnanswered(Map<Integer, String> answers) {
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
    public static long countMultipleMarked(Map<Integer, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }

        return answers.values()
                .stream()
                .filter(option -> option != null && option.contains("+"))
                .count();
    }

    // ------------------------------------------------------------------
    // Lookup helpers
    // ------------------------------------------------------------------

    /**
     * Finds a student by enrollment number in the complete OMR data.
     */
    public static StudentResponse findStudent(
            OMRFile omrFile,
            String enrollmentNumber
    ) {
        if (omrFile == null || omrFile.students() == null) {
            return null;
        }

        String normalizedEnrollmentNumber =
                normalizeEnrollmentNumber(enrollmentNumber);

        return omrFile.students()
                .stream()
                .filter(student ->
                        student != null
                                && normalizedEnrollmentNumber.equals(
                                normalizeEnrollmentNumber(
                                        student.enrollmentNumber()
                                )
                        )
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the metadata for missing or unparseable pages.
     */
    public static List<UnparseablePage> getUnparseablePages(OMRFile omrFile) {
        if (omrFile == null || omrFile.missingOrUnparseablePages() == null) {
            return List.of();
        }

        return List.copyOf(omrFile.missingOrUnparseablePages());
    }

    /**
     * Returns all notes from the JSON.
     */
    public static List<String> getNotes(OMRFile omrFile) {
        if (omrFile == null || omrFile.notes() == null) {
            return List.of();
        }

        return List.copyOf(omrFile.notes());
    }

    // ------------------------------------------------------------------
    // JSON DTOs
    // ------------------------------------------------------------------

    public record OMRFile(
            List<StudentResponse> students,
            List<UnparseablePage> missingOrUnparseablePages,
            List<String> notes
    ) {
    }

    public record StudentResponse(
            String file,
            String name,
            String enrollmentNumber,
            List<AnswerResponse> answers
    ) {
    }

    public record AnswerResponse(
            int questionNumber,
            String option
    ) {
    }

    public record UnparseablePage(
            String file,
            String issue
    ) {
    }

    /**
     * Runtime exception used for malformed OMR JSON.
     */
    public static class OMRParseException extends RuntimeException {

        public OMRParseException(String message) {
            super(message);
        }

        public OMRParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}