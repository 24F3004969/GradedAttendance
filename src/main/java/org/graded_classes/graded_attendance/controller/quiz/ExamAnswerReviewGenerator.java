package org.graded_classes.graded_attendance.controller.quiz;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.components.LatexImageRenderer;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExamAnswerReviewGenerator {

    private static final PDRectangle REVIEW_PAGE_SIZE = PDRectangle.A4;
    private static final float REVIEW_MARGIN = 45f;
    private static final float REVIEW_TOP_MARGIN = 48f;
    private static final float REVIEW_BOTTOM_MARGIN = 45f;
    private static final float REVIEW_CONTENT_WIDTH =
            REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN * 2f;
    private static final float REVIEW_NORMAL_FONT_SIZE = 10f;
    private static final float REVIEW_SMALL_FONT_SIZE = 8f;
    private static final float REVIEW_HEADING_FONT_SIZE = 16f;
    private static final float REVIEW_LINE_HEIGHT = 14f;
    private static final float REVIEW_OPTION_LINE_HEIGHT = 13f;
    private static final int MARKS_PER_QUESTION = 4;

    private static final Pattern MATHEMATICAL_COMMAND_PATTERN = Pattern.compile(
            "\\\\(?:frac|dfrac|tfrac|sqrt|sin|cos|tan|cot|sec|csc|cosec|log|ln|sum|prod|int|lim|begin|end|left|right|operatorname|mathrm|alpha|beta|gamma|theta|pi|leq|geq|neq|infty|times|div|circ)\\b"
    );
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "[A-Za-z0-9)}]\\s*[\\^_]\\s*(?:\\{[^{}]+}|[^\\s,.!?]+)"
    );
    private static final Pattern RATIO_PATTERN = Pattern.compile("\\b\\d+\\s*:\\s*\\d+\\b");
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "[A-Za-z0-9)}]\\s*[=+*/]\\s*[A-Za-z0-9({\\\\]"
    );
    private static final Pattern MATHEMATICAL_WORD_PATTERN = Pattern.compile(
            "(?i)\\b(cosec|csc|sec|cot|tan|sin|cos|log|ln)\\b"
    );

    public AnswerReviewPdfResult generate(
            ExamData examInfo,
            String studentEd,
            String studentName,
            TreeMap<String, QuestionData> questionMap,
            Map<Integer, Integer> submittedAnswers
    ) {
        Objects.requireNonNull(examInfo, "Exam information cannot be null.");
        Objects.requireNonNull(questionMap, "Question map cannot be null.");
        Objects.requireNonNull(submittedAnswers, "Submitted answers cannot be null.");

        if (studentEd == null || studentEd.isBlank()) {
            throw new IllegalArgumentException("Student enrollment number cannot be blank.");
        }

        String normalizedStudentEd = studentEd.trim();
        String safeName = studentName == null || studentName.isBlank()
                ? normalizedStudentEd
                : studentName.trim();

        int attempted = 0;
        int correct = 0;

        for (QuestionData question : questionMap.values()) {
            int questionId = parseQuestionId(question);
            Integer selected = submittedAnswers.get(questionId);

            if (isValidOption(selected)) {
                attempted++;
                if (question.option_data() != null
                        && selected == question.option_data().option_index()) {
                    correct++;
                }
            }
        }

        int wrong = attempted - correct;
        int unanswered = Math.max(0, questionMap.size() - attempted);
        int obtainedMarks = correct * MARKS_PER_QUESTION;
        int totalMarks = questionMap.size() * MARKS_PER_QUESTION;

        Path directory = Path.of(
                Main.getRootPath(),
                "ExamAnswerReviews",
                "Exam_" + examInfo.id()
        );

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Could not create the answer-review directory: " + directory,
                    exception
            );
        }

        File output = directory.resolve(
                sanitizePdfFileName(
                        normalizedStudentEd + "_" + safeName + "_Answer_Review.pdf"
                )
        ).toFile();

        try (PDDocument document = new PDDocument();
             InputStream normalStream = getClass().getResourceAsStream(
                     "/org/graded_classes/graded_attendance/fonts/NotoSans-Regular.ttf"
             );
             InputStream boldStream = getClass().getResourceAsStream(
                     "/org/graded_classes/graded_attendance/fonts/NotoSans-Bold.ttf"
             )) {

            if (normalStream == null || boldStream == null) {
                throw new IllegalStateException(
                        "NotoSans PDF fonts were not found in the application resources."
                );
            }

            PDFont normalFont = PDType0Font.load(document, normalStream, true);
            PDFont boldFont = PDType0Font.load(document, boldStream, true);

            try (PdfReviewContext context =
                         new PdfReviewContext(document, normalFont, boldFont)) {

                writeReviewHeader(
                        context,
                        examInfo,
                        normalizedStudentEd,
                        safeName,
                        obtainedMarks,
                        totalMarks,
                        attempted,
                        correct,
                        wrong,
                        unanswered
                );

                int number = 1;
                for (QuestionData question : questionMap.values()) {
                    writeQuestionReview(
                            context,
                            document,
                            number++,
                            question,
                            submittedAnswers
                    );
                }

                writePdfFooterNote(context);
            }

            document.save(output);

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not generate answer-review PDF for " + safeName,
                    exception
            );
        }

        return new AnswerReviewPdfResult(
                normalizedStudentEd,
                safeName,
                obtainedMarks,
                totalMarks,
                attempted,
                correct,
                wrong,
                unanswered,
                output
        );
    }

    private void writeReviewHeader(
            PdfReviewContext context,
            ExamData exam,
            String studentEd,
            String studentName,
            int obtained,
            int total,
            int attempted,
            int correct,
            int wrong,
            int unanswered
    ) throws IOException {
        context.ensureSpace(190f);

        writePdfText(
                context,
                "EXAM ANSWER REVIEW",
                REVIEW_MARGIN,
                context.y(),
                REVIEW_HEADING_FONT_SIZE,
                context.boldFont(),
                new Color(18, 52, 91)
        );
        context.moveDown(25f);

        writePdfText(
                context,
                "Student: " + nullSafePdfText(studentName),
                REVIEW_MARGIN,
                context.y(),
                11f,
                context.boldFont(),
                Color.BLACK
        );
        context.moveDown(17f);

        writePdfText(
                context,
                "Enrollment No.: " + nullSafePdfText(studentEd),
                REVIEW_MARGIN,
                context.y(),
                10f,
                context.normalFont(),
                Color.DARK_GRAY
        );
        context.moveDown(15f);

        writePdfText(
                context,
                "Exam ID: " + nullSafePdfText(exam.id()),
                REVIEW_MARGIN,
                context.y(),
                10f,
                context.normalFont(),
                Color.DARK_GRAY
        );
        context.moveDown(15f);

        writePdfText(
                context,
                "Subject: " + nullSafePdfText(exam.subject()),
                REVIEW_MARGIN,
                context.y(),
                10f,
                context.normalFont(),
                Color.DARK_GRAY
        );
        context.moveDown(15f);

        writeWrappedPdfText(
                context,
                "Topics: " + nullSafePdfText(exam.topicNamesForDisplay()),
                REVIEW_MARGIN,
                REVIEW_CONTENT_WIDTH,
                10f,
                REVIEW_LINE_HEIGHT,
                context.normalFont(),
                Color.DARK_GRAY
        );

        context.moveDown(4f);
        drawPdfLine(
                context,
                REVIEW_MARGIN,
                context.y(),
                REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN,
                context.y(),
                new Color(200, 210, 220),
                0.8f
        );
        context.moveDown(18f);

        double percentage = total == 0 ? 0.0 : obtained * 100.0 / total;
        writePdfText(
                context,
                String.format(
                        Locale.ROOT,
                        "Marks: %d/%d     Percentage: %.2f%%",
                        obtained,
                        total,
                        percentage
                ),
                REVIEW_MARGIN,
                context.y(),
                12f,
                context.boldFont(),
                new Color(18, 52, 91)
        );
        context.moveDown(20f);

        writePdfText(
                context,
                "Attempted: " + attempted
                        + "     Correct: " + correct
                        + "     Wrong: " + wrong
                        + "     Unanswered: " + unanswered,
                REVIEW_MARGIN,
                context.y(),
                10f,
                context.boldFont(),
                Color.DARK_GRAY
        );
        context.moveDown(25f);
    }

    private void writeQuestionReview(
            PdfReviewContext context,
            PDDocument document,
            int number,
            QuestionData question,
            Map<Integer, Integer> answers
    ) throws IOException {
        int questionId = parseQuestionId(question);
        Integer selected = answers.get(questionId);
        int correctIndex = question.option_data() == null
                ? 0
                : question.option_data().option_index();

        boolean attempted = isValidOption(selected);
        boolean correct = attempted && selected == correctIndex;

        Color statusColor = !attempted
                ? new Color(217, 119, 6)
                : correct
                ? new Color(22, 163, 74)
                : new Color(220, 38, 38);

        String statusText = !attempted
                ? "NOT ANSWERED"
                : correct
                ? "CORRECT"
                : "INCORRECT";

        context.ensureSpace(165f);

        drawPdfLine(
                context,
                REVIEW_MARGIN,
                context.y(),
                REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN,
                context.y(),
                statusColor,
                2f
        );
        context.moveDown(17f);

        writePdfText(
                context,
                "Question " + number + "  [ID: " + questionId + "]",
                REVIEW_MARGIN,
                context.y(),
                12f,
                context.boldFont(),
                new Color(18, 52, 91)
        );

        writeRightAlignedPdfText(
                context,
                statusText,
                REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN,
                context.y(),
                9f,
                context.boldFont(),
                statusColor
        );
        context.moveDown(22f);

        PreparedQuestion preparedQuestion = classifyQuestionContent(question.question_txt());

        if (preparedQuestion.plainText()) {
            writePlainQuestionText(context, preparedQuestion.content());
        } else {
            float questionHeight = drawLatexOnPdf(
                    context,
                    preparedQuestion.content(),
                    REVIEW_MARGIN,
                    context.y(),
                    REVIEW_CONTENT_WIDTH,
                    11.5f,
                    Color.BLACK
            );
            context.moveDown(Math.max(questionHeight, REVIEW_LINE_HEIGHT) + 12f);
        }

        writeQuestionImageIfPresent(
                context,
                document,
                question.question_img_path()
        );

        Map<Integer, String> options = question.option_data() == null
                || question.option_data().options() == null
                ? Collections.emptyMap()
                : question.option_data().options();

        for (Map.Entry<Integer, String> option : options.entrySet()) {
            int optionIndex = option.getKey();
            writeReviewOption(
                    context,
                    optionIndex,
                    option.getValue(),
                    Objects.equals(selected, optionIndex),
                    optionIndex == correctIndex
            );
        }

        context.moveDown(6f);

        String explanation = "Student answer: "
                + (attempted ? optionIndexToLetter(selected) : "Not answered")
                + "    |    Correct answer: "
                + optionIndexToLetter(correctIndex);

        Color explanationColor = !attempted
                ? new Color(146, 64, 14)
                : correct
                ? new Color(22, 101, 52)
                : new Color(153, 27, 27);

        writeWrappedPdfText(
                context,
                explanation,
                REVIEW_MARGIN,
                REVIEW_CONTENT_WIDTH,
                10f,
                REVIEW_LINE_HEIGHT,
                context.boldFont(),
                explanationColor
        );
        context.moveDown(22f);
    }

    private void writeReviewOption(
            PdfReviewContext context,
            int index,
            String value,
            boolean selected,
            boolean correct
    ) throws IOException {
        Color color = Color.DARK_GRAY;
        String status = "";

        if (correct && selected) {
            color = new Color(22, 101, 52);
            status = "[CORRECT - SELECTED]";
        } else if (correct) {
            color = new Color(22, 101, 52);
            status = "[CORRECT ANSWER]";
        } else if (selected) {
            color = new Color(153, 27, 27);
            status = "[SELECTED - WRONG]";
        }

        context.ensureSpace(42f);

        float top = context.y();
        float labelX = REVIEW_MARGIN + 14f;
        float statusX = labelX + 24f;
        float formulaX = REVIEW_MARGIN + 178f;
        float baseline = top - 10f;

        writePdfText(
                context,
                optionIndexToLetter(index) + ".",
                labelX,
                baseline,
                10f,
                correct || selected ? context.boldFont() : context.normalFont(),
                color
        );

        if (!status.isBlank()) {
            writePdfText(
                    context,
                    status,
                    statusX,
                    baseline,
                    8.5f,
                    context.boldFont(),
                    color
            );
        }

        float available = REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN - formulaX;
        float contentHeight;

        if (isPlainOptionText(value)) {
            List<String> lines = wrapPdfText(
                    value == null ? "" : value.trim(),
                    correct || selected ? context.boldFont() : context.normalFont(),
                    REVIEW_NORMAL_FONT_SIZE,
                    available
            );

            float lineY = baseline;
            for (String line : lines) {
                writePdfText(
                        context,
                        line,
                        formulaX,
                        lineY,
                        REVIEW_NORMAL_FONT_SIZE,
                        correct || selected ? context.boldFont() : context.normalFont(),
                        color
                );
                lineY -= REVIEW_OPTION_LINE_HEIGHT;
            }
            contentHeight = Math.max(
                    REVIEW_OPTION_LINE_HEIGHT,
                    lines.size() * REVIEW_OPTION_LINE_HEIGHT
            );
        } else {
            contentHeight = drawLatexOnPdf(
                    context,
                    prepareOptionLatex(value),
                    formulaX,
                    top,
                    available,
                    REVIEW_NORMAL_FONT_SIZE,
                    color
            );
        }

        context.moveDown(
                Math.max(contentHeight, REVIEW_OPTION_LINE_HEIGHT) + 8f
        );
    }

    private PreparedQuestion classifyQuestionContent(String value) {
        if (value == null || value.isBlank()) {
            return new PreparedQuestion(true, "No question text available");
        }

        String source = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        boolean mathematical = containsUnescapedDollar(source)
                || containsUnicodeMathematics(source)
                || MATHEMATICAL_COMMAND_PATTERN.matcher(source).find()
                || SCRIPT_PATTERN.matcher(source).find()
                || RATIO_PATTERN.matcher(source).find()
                || EXPRESSION_PATTERN.matcher(source).find();

        if (!mathematical) {
            return new PreparedQuestion(
                    true,
                    source.replace("\\\\", "\n")
                            .replaceAll("[ ]+", " ")
                            .trim()
            );
        }

        return new PreparedQuestion(false, prepareQuestionLatex(source));
    }

    private void writePlainQuestionText(
            PdfReviewContext context,
            String text
    ) throws IOException {
        String normalized = text == null || text.isBlank()
                ? "No question text available"
                : text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\\\\", "\n");

        boolean written = false;

        for (String paragraph : normalized.split("\n", -1)) {
            String clean = paragraph.trim().replaceAll("\\s+", " ");

            if (clean.isBlank()) {
                if (written) {
                    context.moveDown(5f);
                }
                continue;
            }

            writeWrappedPdfText(
                    context,
                    clean,
                    REVIEW_MARGIN,
                    REVIEW_CONTENT_WIDTH,
                    11f,
                    15f,
                    context.normalFont(),
                    Color.BLACK
            );
            written = true;
        }

        context.moveDown(12f);
    }

    private boolean isPlainOptionText(String value) {
        return value == null
                || value.isBlank()
                || (!looksLikeLatex(value)
                && !looksLikeMathematicalOption(value)
                && !containsUnescapedDollar(value)
                && !containsUnicodeMathematics(value));
    }

    private boolean containsUnicodeMathematics(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            if (isUnicodeSubscript(character)
                    || isUnicodeSuperscript(character)
                    || "×÷−–≤≥≠∞√πθαβγδλμσφω°→←⇒⇔∠±∓≈∝∑∏∫"
                    .indexOf(character) >= 0) {
                return true;
            }
        }

        return false;
    }

    private String prepareQuestionLatex(String value) {
        if (value == null || value.isBlank()) {
            return "\\text{No question text available}";
        }

        String source = normalizeLatexSource(value);

        if (containsUnescapedDollar(source)) {
            return convertDollarMixedContent(source);
        }

        source = removeLatexDelimiters(source);
        int firstCommand = findFirstLatexCommand(source);

        if (firstCommand > 0) {
            String words = source.substring(0, firstCommand).trim();
            String formula = normalizeLatexForJLatexMath(
                    source.substring(firstCommand).trim()
            );

            return "\\begin{array}{l}\\text{"
                    + escapeLatexText(words)
                    + "}\\\\"
                    + formula
                    + "\\end{array}";
        }

        if (firstCommand == 0 || looksLikeLatex(source)) {
            return normalizeLatexForJLatexMath(source);
        }

        int mathematicalWord = findFirstMathematicalWord(source);

        if (mathematicalWord > 0) {
            String words = source.substring(0, mathematicalWord).trim();
            String formula = normalizeMathWords(
                    source.substring(mathematicalWord).trim()
            );

            return "\\begin{array}{l}\\text{"
                    + escapeLatexText(words)
                    + "}\\\\"
                    + formula
                    + "\\end{array}";
        }

        if (looksLikeMathematicalOption(source)) {
            return normalizeMathWords(source);
        }

        return "\\text{" + escapeLatexText(source) + "}";
    }

    private String prepareOptionLatex(String value) {
        if (value == null || value.isBlank()) {
            return "\\text{}";
        }

        String source = normalizeLatexSource(value);

        if (containsUnescapedDollar(source)) {
            return convertDollarMixedContent(source);
        }

        source = removeLatexDelimiters(source);

        if (looksLikeLatex(source)) {
            return normalizeLatexForJLatexMath(source);
        }

        if (looksLikeMathematicalOption(source)) {
            return normalizeMathWords(source);
        }

        return "\\text{" + escapeLatexText(source) + "}";
    }

    private String normalizeLatexSource(String value) {
        return value == null
                ? ""
                : value.replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private int findFirstLatexCommand(String value) {
        String[] commands = {
                "\\frac", "\\dfrac", "\\tfrac", "\\sqrt", "\\begin",
                "\\left", "\\sin", "\\cos", "\\tan", "\\cot", "\\sec",
                "\\csc", "\\operatorname", "\\mathrm", "\\sum", "\\prod",
                "\\int", "\\lim", "\\vec", "\\overline", "\\underline",
                "\\angle"
        };

        int earliest = -1;

        for (String command : commands) {
            int position = value.indexOf(command);
            if (position >= 0 && (earliest < 0 || position < earliest)) {
                earliest = position;
            }
        }

        return earliest;
    }

    private int findFirstMathematicalWord(String value) {
        Matcher matcher = MATHEMATICAL_WORD_PATTERN.matcher(value);
        return matcher.find() ? matcher.start() : -1;
    }

    private boolean looksLikeLatex(String value) {
        return value != null
                && (value.contains("\\")
                || value.contains("^{")
                || value.contains("_{"));
    }

    private boolean looksLikeMathematicalOption(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String lower = value.toLowerCase(Locale.ROOT);

        return lower.matches(".*\\b(sin|cos|tan|cot|sec|cosec|csc|log|ln)\\b.*")
                || value.matches(".*[=+\\-*/×÷^_].*")
                || value.matches(".*\\d.*")
                || value.contains("(")
                || value.contains(")");
    }

    private String normalizeMathWords(String value) {
        String result = normalizeLatexSource(value);
        result = result.replaceAll("(?i)\\bcosec\\s*", "\\\\mathrm{cosec}\\\\,");
        result = result.replaceAll("(?i)\\bcsc\\s*", "\\\\csc\\\\,");
        result = result.replaceAll("(?i)\\bsec\\s*", "\\\\sec\\\\,");
        result = result.replaceAll("(?i)\\bcot\\s*", "\\\\cot\\\\,");
        result = result.replaceAll("(?i)\\btan\\s*", "\\\\tan\\\\,");
        result = result.replaceAll("(?i)\\bsin\\s*", "\\\\sin\\\\,");
        result = result.replaceAll("(?i)\\bcos\\s*", "\\\\cos\\\\,");
        result = result.replaceAll("(?i)\\blog\\s*", "\\\\log\\\\,");
        result = result.replaceAll("(?i)\\bln\\s*", "\\\\ln\\\\,");
        return normalizeLatexForJLatexMath(result);
    }

    private String normalizeLatexForJLatexMath(String latex) {
        if (latex == null || latex.isBlank()) {
            return "\\text{}";
        }

        String value = removeLatexDelimiters(latex.trim());
        value = convertUnicodeScriptsToLatex(value);
        value = convertUnicodeMathSymbolsToLatex(value);
        value = value.replaceAll(
                "\\\\operatorname\\s*\\{([^{}]*)}",
                "\\\\mathrm{$1}"
        );
        value = value.replaceAll(
                "\\\\arccosec(?![A-Za-z])",
                "\\\\mathrm{arccosec}"
        );
        value = value.replaceAll(
                "\\\\arcsec(?![A-Za-z])",
                "\\\\mathrm{arcsec}"
        );
        value = value.replaceAll(
                "\\\\cosec(?![A-Za-z])",
                "\\\\mathrm{cosec}"
        );
        return value.trim();
    }

    private String convertUnicodeScriptsToLatex(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }

        StringBuilder result = new StringBuilder();
        int index = 0;

        while (index < value.length()) {
            char character = value.charAt(index);

            if (isUnicodeSubscript(character)) {
                StringBuilder subscript = new StringBuilder();
                while (index < value.length()
                        && isUnicodeSubscript(value.charAt(index))) {
                    subscript.append(unicodeSubscriptValue(value.charAt(index)));
                    index++;
                }
                result.append("_{").append(subscript).append('}');
                continue;
            }

            if (isUnicodeSuperscript(character)) {
                StringBuilder superscript = new StringBuilder();
                while (index < value.length()
                        && isUnicodeSuperscript(value.charAt(index))) {
                    superscript.append(unicodeSuperscriptValue(value.charAt(index)));
                    index++;
                }
                result.append("^{").append(superscript).append('}');
                continue;
            }

            result.append(character);
            index++;
        }

        return result.toString();
    }

    private boolean isUnicodeSubscript(char character) {
        return switch (character) {
            case '₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉',
                 '₊', '₋', '₌', '₍', '₎', 'ₐ', 'ₑ', 'ₕ', 'ᵢ', 'ⱼ',
                 'ₖ', 'ₗ', 'ₘ', 'ₙ', 'ₒ', 'ₚ', 'ᵣ', 'ₛ', 'ₜ', 'ᵤ',
                 'ᵥ', 'ₓ' -> true;
            default -> false;
        };
    }

    private String unicodeSubscriptValue(char character) {
        return switch (character) {
            case '₀' -> "0"; case '₁' -> "1"; case '₂' -> "2";
            case '₃' -> "3"; case '₄' -> "4"; case '₅' -> "5";
            case '₆' -> "6"; case '₇' -> "7"; case '₈' -> "8";
            case '₉' -> "9"; case '₊' -> "+"; case '₋' -> "-";
            case '₌' -> "="; case '₍' -> "("; case '₎' -> ")";
            case 'ₐ' -> "a"; case 'ₑ' -> "e"; case 'ₕ' -> "h";
            case 'ᵢ' -> "i"; case 'ⱼ' -> "j"; case 'ₖ' -> "k";
            case 'ₗ' -> "l"; case 'ₘ' -> "m"; case 'ₙ' -> "n";
            case 'ₒ' -> "o"; case 'ₚ' -> "p"; case 'ᵣ' -> "r";
            case 'ₛ' -> "s"; case 'ₜ' -> "t"; case 'ᵤ' -> "u";
            case 'ᵥ' -> "v"; case 'ₓ' -> "x";
            default -> String.valueOf(character);
        };
    }

    private boolean isUnicodeSuperscript(char character) {
        return switch (character) {
            case '⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹',
                 '⁺', '⁻', '⁼', '⁽', '⁾', 'ⁿ', 'ᵃ', 'ᵇ', 'ᶜ', 'ᵈ',
                 'ᵉ', 'ᶠ', 'ᵍ', 'ʰ', 'ⁱ', 'ʲ', 'ᵏ', 'ˡ', 'ᵐ', 'ᵒ',
                 'ᵖ', 'ʳ', 'ˢ', 'ᵗ', 'ᵘ', 'ᵛ', 'ʷ', 'ˣ', 'ʸ', 'ᶻ' -> true;
            default -> false;
        };
    }

    private String unicodeSuperscriptValue(char character) {
        return switch (character) {
            case '⁰' -> "0"; case '¹' -> "1"; case '²' -> "2";
            case '³' -> "3"; case '⁴' -> "4"; case '⁵' -> "5";
            case '⁶' -> "6"; case '⁷' -> "7"; case '⁸' -> "8";
            case '⁹' -> "9"; case '⁺' -> "+"; case '⁻' -> "-";
            case '⁼' -> "="; case '⁽' -> "("; case '⁾' -> ")";
            case 'ⁿ' -> "n"; case 'ᵃ' -> "a"; case 'ᵇ' -> "b";
            case 'ᶜ' -> "c"; case 'ᵈ' -> "d"; case 'ᵉ' -> "e";
            case 'ᶠ' -> "f"; case 'ᵍ' -> "g"; case 'ʰ' -> "h";
            case 'ⁱ' -> "i"; case 'ʲ' -> "j"; case 'ᵏ' -> "k";
            case 'ˡ' -> "l"; case 'ᵐ' -> "m"; case 'ᵒ' -> "o";
            case 'ᵖ' -> "p"; case 'ʳ' -> "r"; case 'ˢ' -> "s";
            case 'ᵗ' -> "t"; case 'ᵘ' -> "u"; case 'ᵛ' -> "v";
            case 'ʷ' -> "w"; case 'ˣ' -> "x"; case 'ʸ' -> "y";
            case 'ᶻ' -> "z";
            default -> String.valueOf(character);
        };
    }

    private String convertUnicodeMathSymbolsToLatex(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }

        return value
                .replace("×", "\\times ")
                .replace("÷", "\\div ")
                .replace("−", "-")
                .replace("–", "-")
                .replace("≤", "\\leq ")
                .replace("≥", "\\geq ")
                .replace("≠", "\\neq ")
                .replace("∞", "\\infty ")
                .replace("√", "\\sqrt{}")
                .replace("π", "\\pi ")
                .replace("θ", "\\theta ")
                .replace("α", "\\alpha ")
                .replace("β", "\\beta ")
                .replace("γ", "\\gamma ")
                .replace("δ", "\\delta ")
                .replace("λ", "\\lambda ")
                .replace("μ", "\\mu ")
                .replace("σ", "\\sigma ")
                .replace("φ", "\\phi ")
                .replace("ω", "\\omega ")
                .replace("°", "^{\\circ}")
                .replace("→", "\\rightarrow ")
                .replace("←", "\\leftarrow ")
                .replace("⇒", "\\Rightarrow ")
                .replace("⇔", "\\Leftrightarrow ")
                .replace("∠", "\\angle ")
                .replace("±", "\\pm ")
                .replace("∓", "\\mp ")
                .replace("≈", "\\approx ")
                .replace("∝", "\\propto ")
                .replace("∑", "\\sum ")
                .replace("∏", "\\prod ")
                .replace("∫", "\\int ");
    }

    private boolean containsUnescapedDollar(String value) {
        for (int index = 0; value != null && index < value.length(); index++) {
            if (value.charAt(index) == '$'
                    && !isEscapedCharacter(value, index)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEscapedCharacter(String value, int index) {
        int count = 0;
        for (int current = index - 1;
             current >= 0 && value.charAt(current) == '\\';
             current--) {
            count++;
        }
        return count % 2 != 0;
    }

    private String convertDollarMixedContent(String source) {
        StringBuilder output = new StringBuilder();
        StringBuilder part = new StringBuilder();
        boolean formula = false;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);

            if (character == '$' && !isEscapedCharacter(source, index)) {
                appendMixedLatexSection(output, part.toString(), formula);
                part.setLength(0);
                formula = !formula;
            } else {
                part.append(character);
            }
        }

        appendMixedLatexSection(output, part.toString(), formula);
        return output.toString().trim();
    }

    private void appendMixedLatexSection(
            StringBuilder output,
            String section,
            boolean formula
    ) {
        if (section == null || section.isEmpty()) {
            return;
        }

        if (formula) {
            output.append(normalizeLatexForJLatexMath(section.trim()));
        } else if (section.isBlank()) {
            output.append("\\;");
        } else {
            output.append("\\text{")
                    .append(escapeLatexText(section))
                    .append('}');
        }
    }

    private String escapeLatexText(String text) {
        StringBuilder result = new StringBuilder();

        if (text == null) {
            return "";
        }

        for (char character : text.toCharArray()) {
            switch (character) {
                case '\\' -> result.append("\\backslash ");
                case '{' -> result.append("\\{");
                case '}' -> result.append("\\}");
                case '%' -> result.append("\\%");
                case '#' -> result.append("\\#");
                case '&' -> result.append("\\&");
                case '_' -> result.append("\\_");
                case '$' -> result.append("\\$");
                case '^' -> result.append("\\hat{}");
                default -> result.append(character);
            }
        }

        return result.toString();
    }

    private String removeLatexDelimiters(String text) {
        if (text == null) {
            return "";
        }

        String value = text.trim();

        if (value.length() >= 4
                && value.startsWith("$$")
                && value.endsWith("$$")) {
            return value.substring(2, value.length() - 2).trim();
        }

        if (value.length() >= 2
                && value.startsWith("$")
                && value.endsWith("$")) {
            return value.substring(1, value.length() - 1).trim();
        }

        if (value.length() >= 4
                && value.startsWith("\\(")
                && value.endsWith("\\)")) {
            return value.substring(2, value.length() - 2).trim();
        }

        if (value.length() >= 4
                && value.startsWith("\\[")
                && value.endsWith("\\]")) {
            return value.substring(2, value.length() - 2).trim();
        }

        return value;
    }

    private float drawLatexOnPdf(
            PdfReviewContext context,
            String formula,
            float x,
            float topY,
            float maxWidth,
            float fontSize,
            Color color
    ) throws IOException {
        BufferedImage image = LatexImageRenderer.render(
                normalizeLatexForJLatexMath(formula),
                fontSize,
                color,
                true
        );

        float quality = (float) LatexImageRenderer.RENDER_QUALITY;
        float width = image.getWidth() / quality;
        float height = image.getHeight() / quality;

        if (maxWidth > 0 && width > maxWidth) {
            float scale = maxWidth / width;
            width *= scale;
            height *= scale;
        }

        float oldY = context.y();
        context.ensureSpace(height + 6f);

        if (Float.compare(oldY, context.y()) != 0) {
            topY = context.y();
        }

        PDImageXObject pdfImage = LosslessFactory.createFromImage(
                context.document(),
                image
        );

        context.stream().drawImage(
                pdfImage,
                x,
                topY - height,
                width,
                height
        );

        return height;
    }

    private void writeQuestionImageIfPresent(
            PdfReviewContext context,
            PDDocument document,
            String imagePath
    ) throws IOException {
        File imageFile = resolveQuestionImageFile(imagePath);

        if (imageFile == null) {
            return;
        }

        PDImageXObject image = PDImageXObject.createFromFileByContent(
                imageFile,
                document
        );

        float scale = Math.min(
                1f,
                Math.min(
                        REVIEW_CONTENT_WIDTH / image.getWidth(),
                        240f / image.getHeight()
                )
        );

        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;

        context.ensureSpace(height + 15f);

        float x = REVIEW_MARGIN + (REVIEW_CONTENT_WIDTH - width) / 2f;
        context.stream().drawImage(
                image,
                x,
                context.y() - height,
                width,
                height
        );
        context.setY(context.y() - height - 12f);
    }

    private File resolveQuestionImageFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        File direct = new File(path);
        if (direct.isFile()) {
            return direct;
        }

        File relative = new File(Main.getRootPath(), path);
        return relative.isFile() ? relative : null;
    }

    private void writePdfText(
            PdfReviewContext context,
            String text,
            float x,
            float y,
            float size,
            PDFont font,
            Color color
    ) throws IOException {
        String value = sanitizePdfText(text);

        if (value.isBlank()) {
            return;
        }

        context.stream().beginText();
        try {
            context.stream().setFont(font, size);
            context.stream().setNonStrokingColor(color);
            context.stream().newLineAtOffset(x, y);
            context.stream().showText(value);
        } finally {
            context.stream().endText();
        }
    }

    private void writeRightAlignedPdfText(
            PdfReviewContext context,
            String text,
            float right,
            float y,
            float size,
            PDFont font,
            Color color
    ) throws IOException {
        String value = sanitizePdfText(text);
        float width = font.getStringWidth(value) / 1000f * size;
        writePdfText(context, value, right - width, y, size, font, color);
    }

    private void writeWrappedPdfText(
            PdfReviewContext context,
            String text,
            float x,
            float maxWidth,
            float size,
            float lineHeight,
            PDFont font,
            Color color
    ) throws IOException {
        for (String line : wrapPdfText(text, font, size, maxWidth)) {
            context.ensureSpace(lineHeight);
            writePdfText(
                    context,
                    line,
                    x,
                    context.y(),
                    size,
                    font,
                    color
            );
            context.moveDown(lineHeight);
        }
    }

    private List<String> wrapPdfText(
            String text,
            PDFont font,
            float size,
            float maxWidth
    ) throws IOException {
        List<String> lines = new ArrayList<>();
        String sanitized = sanitizePdfText(text).trim();

        if (sanitized.isEmpty()) {
            return List.of("");
        }

        StringBuilder current = new StringBuilder();

        for (String word : sanitized.split("\\s+")) {
            String candidate = current.isEmpty()
                    ? word
                    : current + " " + word;

            if (font.getStringWidth(candidate) / 1000f * size <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }

                if (font.getStringWidth(word) / 1000f * size <= maxWidth) {
                    current.setLength(0);
                    current.append(word);
                } else {
                    List<String> fragments = splitLongWord(
                            word,
                            font,
                            size,
                            maxWidth
                    );
                    for (int index = 0; index < fragments.size() - 1; index++) {
                        lines.add(fragments.get(index));
                    }
                    current.setLength(0);
                    current.append(fragments.get(fragments.size() - 1));
                }
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines.isEmpty() ? List.of("") : lines;
    }

    private List<String> splitLongWord(
            String word,
            PDFont font,
            float size,
            float maxWidth
    ) throws IOException {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int index = 0; index < word.length(); index++) {
            char character = word.charAt(index);
            String candidate = current.toString() + character;

            if (!current.isEmpty()
                    && font.getStringWidth(candidate) / 1000f * size > maxWidth) {
                parts.add(current.toString());
                current.setLength(0);
            }

            current.append(character);
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts.isEmpty() ? List.of(word) : parts;
    }

    private void drawPdfLine(
            PdfReviewContext context,
            float x1,
            float y1,
            float x2,
            float y2,
            Color color,
            float width
    ) throws IOException {
        context.stream().setStrokingColor(color);
        context.stream().setLineWidth(width);
        context.stream().moveTo(x1, y1);
        context.stream().lineTo(x2, y2);
        context.stream().stroke();
    }

    private void writePdfFooterNote(PdfReviewContext context) throws IOException {
        context.ensureSpace(35f);
        context.moveDown(10f);

        drawPdfLine(
                context,
                REVIEW_MARGIN,
                context.y(),
                REVIEW_PAGE_SIZE.getWidth() - REVIEW_MARGIN,
                context.y(),
                new Color(210, 218, 228),
                0.7f
        );
        context.moveDown(17f);

        writePdfText(
                context,
                "Generated by Graded Attendance",
                REVIEW_MARGIN,
                context.y(),
                REVIEW_SMALL_FONT_SIZE,
                context.normalFont(),
                Color.GRAY
        );
    }

    private int parseQuestionId(QuestionData question) {
        if (question == null
                || question.question_id() == null
                || question.question_id().isBlank()) {
            throw new IllegalArgumentException("Question ID cannot be blank.");
        }

        try {
            return Integer.parseInt(question.question_id());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid question ID: " + question.question_id(),
                    exception
            );
        }
    }

    private boolean isValidOption(Integer option) {
        return option != null && option >= 1 && option <= 4;
    }

    private String nullSafePdfText(String value) {
        return value == null || value.isBlank()
                ? "Not available"
                : sanitizePdfText(value.trim());
    }

    private String sanitizePdfText(String value) {
        return value == null
                ? ""
                : value.replace('\0', ' ')
                .replace('\t', ' ')
                .replaceAll("[\\p{Cc}&&[^\\n\\r]]", "");
    }

    private String sanitizePdfFileName(String value) {
        String result = value == null || value.isBlank()
                ? "Answer_Review.pdf"
                : value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");

        if (!result.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            result += ".pdf";
        }

        return result.length() > 150
                ? result.substring(0, 146) + ".pdf"
                : result;
    }

    private String optionIndexToLetter(int value) {
        return switch (value) {
            case 1 -> "A";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "D";
            default -> "-";
        };
    }

    public record AnswerReviewPdfResult(
            String studentEd,
            String studentName,
            int obtainedMarks,
            int totalMarks,
            int attempted,
            int correct,
            int wrong,
            int unanswered,
            File pdfFile
    ) {
    }

    private record PreparedQuestion(boolean plainText, String content) {
    }

    private static final class PdfReviewContext implements AutoCloseable {
        private final PDDocument document;
        private final PDFont normalFont;
        private final PDFont boldFont;
        private PDPageContentStream stream;
        private float y;

        private PdfReviewContext(
                PDDocument document,
                PDFont normalFont,
                PDFont boldFont
        ) throws IOException {
            this.document = document;
            this.normalFont = normalFont;
            this.boldFont = boldFont;
            createPage();
        }

        private void createPage() throws IOException {
            closeCurrentStream();

            PDPage page = new PDPage(REVIEW_PAGE_SIZE);
            document.addPage(page);

            stream = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.OVERWRITE,
                    true,
                    true
            );

            y = REVIEW_PAGE_SIZE.getHeight() - REVIEW_TOP_MARGIN;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < REVIEW_BOTTOM_MARGIN) {
                createPage();
            }
        }

        private PDPageContentStream stream() {
            return stream;
        }

        private PDDocument document() {
            return document;
        }

        private PDFont normalFont() {
            return normalFont;
        }

        private PDFont boldFont() {
            return boldFont;
        }

        private float y() {
            return y;
        }

        private void setY(float value) {
            y = value;
        }

        private void moveDown(float value) {
            y -= value;
        }

        private void closeCurrentStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        @Override
        public void close() throws IOException {
            closeCurrentStream();
        }
    }
}
