package org.graded_classes.graded_attendance.components;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public final class BasicParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );

    private BasicParser() {
        // Utility class
    }

    /**
     * Reads questions from a JSON file.
     *
     * @param path    path of the JSON file
     * @param topicId topic ID assigned to every imported question
     * @return questions stored using 1-based keys
     */
    public static TreeMap<Integer, QuestionData> parse(
            String path,
            String topicId
    ) {
        TreeMap<Integer, QuestionData> result = new TreeMap<>();

        try {
            Path jsonPath = Path.of(path);

            if (!Files.exists(jsonPath)) {
                throw new IllegalArgumentException(
                        "JSON file does not exist: " + jsonPath.toAbsolutePath()
                );
            }

            if (!Files.isRegularFile(jsonPath)) {
                throw new IllegalArgumentException(
                        "The supplied path is not a file: "
                                + jsonPath.toAbsolutePath()
                );
            }

            QuestionFile questionFile = OBJECT_MAPPER.readValue(
                    jsonPath.toFile(),
                    QuestionFile.class
            );

            if (questionFile == null || questionFile.questions() == null) {
                throw new IllegalArgumentException(
                        "The JSON file must contain a 'questions' array."
                );
            }

            List<JsonQuestion> questions = questionFile.questions();

            for (int i = 0; i < questions.size(); i++) {
                JsonQuestion jsonQuestion = questions.get(i);

                validateQuestion(jsonQuestion, i + 1);

                LinkedHashMap<Integer, String> optionMap =
                        new LinkedHashMap<>();

                for (int optionIndex = 0;
                     optionIndex < jsonQuestion.options().size();
                     optionIndex++) {

                    optionMap.put(
                            optionIndex + 1,
                            normalizeText(
                                    jsonQuestion.options().get(optionIndex)
                            )
                    );
                }

                int questionNumber = i + 1;

                /*
                 * JSON correctOption is 1-based:
                 * 1 = first option, 2 = second option, etc.
                 *
                 * Your old parser subtracted 1, so this preserves the
                 * same zero-based value expected by OptionData.
                 */
                int zeroBasedCorrectOption =
                        jsonQuestion.correctOption() - 1;

                QuestionData questionData = new QuestionData(
                        String.valueOf(i),
                        topicId,
                        "1",
                        LocalDate.now().toString(),
                        defaultIfBlank(jsonQuestion.type(), "mcq"),
                        defaultIfBlank(jsonQuestion.level(), "Easy"),
                        normalizeText(jsonQuestion.question()),
                        "",
                        new OptionData(
                                zeroBasedCorrectOption,
                                optionMap
                        )
                );

                result.put(questionNumber, questionData);
            }

            return result;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read or parse JSON question file: " + path,
                    exception
            );
        }
    }

    private static void validateQuestion(
            JsonQuestion question,
            int questionNumber
    ) {
        if (question == null) {
            throw new IllegalArgumentException(
                    "Question " + questionNumber + " is null."
            );
        }

        if (question.question() == null
                || question.question().isBlank()) {
            throw new IllegalArgumentException(
                    "Question " + questionNumber
                            + " does not contain question text."
            );
        }

        if (question.options() == null
                || question.options().size() < 2) {
            throw new IllegalArgumentException(
                    "Question " + questionNumber
                            + " must contain at least two options."
            );
        }

        for (int i = 0; i < question.options().size(); i++) {
            String option = question.options().get(i);

            if (option == null || option.isBlank()) {
                throw new IllegalArgumentException(
                        "Option " + (i + 1)
                                + " of question " + questionNumber
                                + " is empty."
                );
            }
        }

        if (question.correctOption() < 1
                || question.correctOption() > question.options().size()) {
            throw new IllegalArgumentException(
                    "Question " + questionNumber
                            + " has invalid correctOption "
                            + question.correctOption()
                            + ". It must be between 1 and "
                            + question.options().size() + "."
            );
        }
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank()
                ? defaultValue
                : value.trim();
    }

    public record QuestionFile(
            List<JsonQuestion> questions
    ) {
    }

    public record JsonQuestion(
            String question,
            List<String> options,
            int correctOption,
            String type,
            String level
    ) {
    }
}