package org.graded_classes.graded_attendance.controller.quiz;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.graded_classes.graded_attendance.components.LatexView;
import org.graded_classes.graded_attendance.data.ExamData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * High-quality compact A4 exam PDF generator for PDFBox 3.x.
 */
public final class ExamPdfGenerator {
    private static final double RENDER_QUALITY = 6.0;
    private static final float PAGE_MARGIN = 16f;
    private static final float BOTTOM_MARGIN = 12f;
    private static final float FIRST_HEADER = 70f;
    private static final float NEXT_HEADER = 27f;
    private static final float QUESTION_FONT = 11f;
    private static final float OPTION_FONT = 9.8f;
    private static final float QUESTION_MIN_H = 11.5f;
    private static final float OPTION_MIN_H = 10f;
    private static final float QUESTION_LINE_GAP = 1.2f;
    private static final float OPTION_LINE_GAP = .8f;
    private static final float QUESTION_GAP = 2.5f;
    private static final float OPTION_GAP = 1.4f;
    private static final float NEXT_QUESTION_GAP = 3.5f;
    private static final float QUESTION_NO_W = 19f;
    private static final float OPTION_LABEL_W = 20f;
    private static final float INLINE_LABEL_W = 17f;
    private static final float INLINE_GAP = 12f;
    private static final float INLINE_PADDING = 6f;
    private static final float LARGE_OPTION_FACTOR = .48f;
    private static final float WRAP_FACTOR = .98f;

    private static final PDFont NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private ExamPdfGenerator() {
    }

    public static File chooseLocationAndGenerate(Window owner, ExamData exam,
                                                 List<QuestionData> questions) throws IOException {
        validate(exam, questions);
        requireFxThread();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Exam Question Paper");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF document", "*.pdf"));
        chooser.setInitialFileName(fileName(exam));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return null;
        if (!file.getName().toLowerCase().endsWith(".pdf"))
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        generate(file, exam, questions);
        return file;
    }

    public static void generate(File file, ExamData exam,
                                List<QuestionData> questions) throws IOException {
        Objects.requireNonNull(file, "output file cannot be null");
        validate(exam, questions);
        requireFxThread();
        writePdf(file, exam, renderQuestions(questions));
    }

    private static void validate(ExamData exam, List<QuestionData> questions) {
        Objects.requireNonNull(exam, "exam cannot be null");
        Objects.requireNonNull(questions, "questions cannot be null");
        if (questions.isEmpty()) throw new IllegalArgumentException("The exam has no questions.");
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread())
            throw new IllegalStateException("Call ExamPdfGenerator on the JavaFX Application Thread.");
    }

    private static List<RenderedQuestion> renderQuestions(List<QuestionData> questions) {
        List<RenderedQuestion> result = new ArrayList<>();
        float qw = questionWidth() * WRAP_FACTOR;
        float ow = optionWidth() * WRAP_FACTOR;
        for (int i = 0; i < questions.size(); i++) {
            QuestionData q = questions.get(i);
            List<RenderedOption> options = new ArrayList<>();
            if (q.option_data() != null && q.option_data().options() != null) {
                int oi = 0;
                for (Map.Entry<Integer, String> e : q.option_data().options().entrySet()) {
                    options.add(new RenderedOption(e.getKey(), label(oi++),
                            renderWrapped(e.getValue(), OPTION_FONT, ow)));
                }
            }
            result.add(new RenderedQuestion(i + 1, q.question_id(),
                    renderWrapped(q.question_txt(), QUESTION_FONT, qw), options));
        }
        return result;
    }

    private static List<BufferedImage> renderWrapped(String value, float size, float maxWidth) {
        String source = normalize(value);
        BufferedImage whole = renderLatex(asLatex(source), size);
        if (naturalW(whole) <= maxWidth) return List.of(whole);
        String plain = extractTextCommand(source);
        if (plain != null) return wrapPlain(plain, size, maxWidth);
        if (!looksLatex(source)) return wrapPlain(source, size, maxWidth);
        List<String> tokens = topLevelTokens(source);
        if (tokens.size() <= 1) return List.of(whole);
        return wrapTokens(tokens, size, maxWidth, whole);
    }

    private static List<BufferedImage> wrapPlain(String text, float size, float maxWidth) {
        String normalized = normalize(text);
        if (normalized.isBlank()) return List.of(renderLatex("\\text{}", size));
        List<String> words = List.of(normalized.split("\\s+"));
        List<String> latexWords = new ArrayList<>();
        for (String word : words) latexWords.add(escapeText(word));
        List<BufferedImage> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : latexWords) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            BufferedImage image = renderLatex("\\text{" + candidate.trim() + "}", size);
            if (naturalW(image) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) lines.add(renderLatex("\\text{" + current.toString().trim() + "}", size));
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) lines.add(renderLatex("\\text{" + current.toString().trim() + "}", size));
        return lines;
    }

    private static List<BufferedImage> wrapTokens(List<String> tokens, float size,
                                                  float maxWidth, BufferedImage fallback) {
        List<BufferedImage> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String token : tokens) {
            String candidate = current.isEmpty() ? token : current + " " + token;
            BufferedImage image = renderLatex(candidate, size);
            if (naturalW(image) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) lines.add(renderLatex(current.toString(), size));
                current.setLength(0);
                BufferedImage tokenImage = renderLatex(token, size);
                if (naturalW(tokenImage) > maxWidth) lines.add(tokenImage);
                else current.append(token);
            }
        }
        if (!current.isEmpty()) lines.add(renderLatex(current.toString(), size));
        return lines.isEmpty() ? List.of(fallback) : lines;
    }

    private static List<String> topLevelTokens(String latex) {
        List<String> out = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        boolean escaped = false;
        for (char c : latex.toCharArray()) {
            if (c == '\\' && !escaped) {
                escaped = true;
                token.append(c);
                continue;
            }
            if (c == '{' && !escaped) depth++;
            if (c == '}' && !escaped) depth = Math.max(0, depth - 1);
            if (Character.isWhitespace(c) && depth == 0) {
                if (!token.isEmpty()) {
                    out.add(token.toString());
                    token.setLength(0);
                }
            } else token.append(c);
            escaped = false;
        }
        if (!token.isEmpty()) out.add(token.toString());
        return out;
    }

    private static String extractTextCommand(String value) {
        String s = value == null ? "" : value.trim();
        String prefix = "\\text{";
        if (!s.startsWith(prefix) || !s.endsWith("}")) return null;
        int depth = 0;
        boolean escaped = false;
        for (int i = prefix.length() - 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if (c == '{' && !escaped) depth++;
            else if (c == '}' && !escaped && --depth == 0 && i != s.length() - 1) return null;
            escaped = false;
        }
        return depth == 0 ? s.substring(prefix.length(), s.length() - 1) : null;
    }

    private static BufferedImage renderLatex(String latex, float logicalSize) {
        LatexView view = new LatexView();
        view.setSize((float) (logicalSize * RENDER_QUALITY));
        view.setFormula(latex == null || latex.isBlank() ? "\\text{}" : "\\text{%s}".formatted(latex.replace("$$", "$")));
        view.applyCss();
        int w = Math.max(1, (int) Math.ceil(view.getWidth()));
        int h = Math.max(1, (int) Math.ceil(view.getHeight()));
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        WritableImage fxImage = new WritableImage(w, h);
        view.snapshot(params, fxImage);
        BufferedImage source = SwingFXUtils.fromFXImage(fxImage, null);
        BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static void writePdf(File file, ExamData exam,
                                 List<RenderedQuestion> questions) throws IOException {
        Map<BufferedImage, PDImageXObject> cache = new IdentityHashMap<>();
        try (PDDocument document = new PDDocument()) {
            PageState state = createPage(document, exam, 1);
            try {
                for (RenderedQuestion q : questions) {
                    if (state.y - questionHeight(q) < BOTTOM_MARGIN) {
                        state.close();
                        state = createPage(document, exam, document.getNumberOfPages() + 1);
                    }
                    state.y = drawQuestion(document, state.stream, q, state.y, cache);
                }
            } finally {
                state.close();
            }
            document.save(file);
        }
    }

    private static PageState createPage(PDDocument doc, ExamData exam, int number) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream s = new PDPageContentStream(doc, page);
        float w = page.getMediaBox().getWidth(), h = page.getMediaBox().getHeight();
        if (number == 1) {
            centered(s, "GRADE ED - EXAMINATION", BOLD, 12.5f, w / 2, h - 18);
            text(s, "Subject: " + safe(exam.subject()), PAGE_MARGIN, h - 35, 8.2f, true);
            right(s, "Class: " + safe(exam.classes()), w - PAGE_MARGIN, h - 35, 8.2f, true);
            text(s, "Topic: " + safe(exam.topic_name()), PAGE_MARGIN, h - 49, 7.7f, false);
            right(s, "Date: " + safe(exam.doe()) + "   Time: " + safe(exam.time()),
                    w - PAGE_MARGIN, h - 49, 7.7f, false);
            line(s, PAGE_MARGIN, h - 57, w - PAGE_MARGIN, h - 57);
            return new PageState(s, h - FIRST_HEADER);
        }
        text(s, safe(exam.subject()) + " | Class " + safe(exam.classes()),
                PAGE_MARGIN, h - 13, 7.5f, true);
        right(s, "Page " + number, w - PAGE_MARGIN, h - 13, 7.5f, false);
        line(s, PAGE_MARGIN, h - 19, w - PAGE_MARGIN, h - 19);
        return new PageState(s, h - NEXT_HEADER);
    }

    private static float drawQuestion(PDDocument doc, PDPageContentStream s,
                                      RenderedQuestion q, float y,
                                      Map<BufferedImage, PDImageXObject> cache) throws IOException {
        float right = PDRectangle.A4.getWidth() - PAGE_MARGIN;
        float qx = PAGE_MARGIN + QUESTION_NO_W;
        text(s, q.number() + ".", PAGE_MARGIN, y - 9, 9, true);
        float qh = drawLines(doc, s, q.questionLines(), qx, y,
                right - qx, QUESTION_LINE_GAP, cache);
        y -= Math.max(qh, QUESTION_MIN_H) + QUESTION_GAP;
        for (OptionRow row : optionRows(q.options(), right - qx)) {
            y = row.fullWidth()
                    ? drawFull(doc, s, row.options().get(0), qx, right, y, cache)
                    : drawInline(doc, s, row.options(), qx, right, y, cache);
        }
        return y - NEXT_QUESTION_GAP;
    }

    private static List<OptionRow> optionRows(List<RenderedOption> options, float available) {
        List<OptionRow> rows = new ArrayList<>();
        List<RenderedOption> current = new ArrayList<>();
        float used = 0;
        for (RenderedOption option : options) {
            float needed = inlineWidth(option);
            boolean full = option.lines().size() > 1 || needed > available * LARGE_OPTION_FACTOR;
            if (full) {
                if (!current.isEmpty()) {
                    rows.add(new OptionRow(List.copyOf(current), false));
                    current.clear();
                    used = 0;
                }
                rows.add(new OptionRow(List.of(option), true));
                continue;
            }
            float add = current.isEmpty() ? needed : INLINE_GAP + needed;
            if (!current.isEmpty() && used + add > available) {
                rows.add(new OptionRow(List.copyOf(current), false));
                current.clear();
                used = 0;
                add = needed;
            }
            current.add(option);
            used += add;
        }
        if (!current.isEmpty()) rows.add(new OptionRow(List.copyOf(current), false));
        return rows;
    }

    private static float inlineWidth(RenderedOption option) {
        return INLINE_LABEL_W + naturalW(option.lines().get(0)) + INLINE_PADDING;
    }

    private static float drawFull(PDDocument doc, PDPageContentStream s, RenderedOption option,
                                  float x, float right, float y,
                                  Map<BufferedImage, PDImageXObject> cache) throws IOException {
        text(s, option.label() + ")", x, y - 8, 8.2f, true);
        float imageX = x + OPTION_LABEL_W;
        float h = drawLines(doc, s, option.lines(), imageX, y,
                right - imageX, OPTION_LINE_GAP, cache);
        return y - Math.max(h, OPTION_MIN_H) - OPTION_GAP;
    }

    private static float drawInline(PDDocument doc, PDPageContentStream s,
                                    List<RenderedOption> options, float x, float right, float y,
                                    Map<BufferedImage, PDImageXObject> cache) throws IOException {
        float rowH = OPTION_MIN_H;
        for (RenderedOption option : options) {
            BufferedImage image = option.lines().get(0);
            text(s, option.label() + ")", x, y - 8, 8.2f, true);
            float imageX = x + INLINE_LABEL_W;
            float h = drawImage(doc, s, image, imageX, y, right - imageX, cache);
            rowH = Math.max(rowH, h);
            x += inlineWidth(option) + INLINE_GAP;
        }
        return y - rowH - OPTION_GAP;
    }

    private static float drawLines(PDDocument doc, PDPageContentStream s,
                                   List<BufferedImage> lines, float x, float y,
                                   float maxWidth, float gap,
                                   Map<BufferedImage, PDImageXObject> cache) throws IOException {
        float total = 0;
        for (int i = 0; i < lines.size(); i++) {
            float h = drawImage(doc, s, lines.get(i), x, y - total, maxWidth, cache);
            total += h + (i < lines.size() - 1 ? gap : 0);
        }
        return total;
    }

    private static float drawImage(PDDocument doc, PDPageContentStream s, BufferedImage image,
                                   float x, float top, float maxWidth,
                                   Map<BufferedImage, PDImageXObject> cache) throws IOException {
        float scale = Math.min(1f, maxWidth / naturalW(image));
        float w = naturalW(image) * scale, h = naturalH(image) * scale;
        PDImageXObject pdfImage = cache.get(image);
        if (pdfImage == null) {
            pdfImage = LosslessFactory.createFromImage(doc, image);
            cache.put(image, pdfImage);
        }
        s.drawImage(pdfImage, x, top - h, w, h);
        return h;
    }

    private static float questionHeight(RenderedQuestion q) {
        float total = Math.max(linesHeight(q.questionLines(), questionWidth(), QUESTION_LINE_GAP),
                QUESTION_MIN_H) + QUESTION_GAP;
        for (OptionRow row : optionRows(q.options(), questionWidth())) {
            if (row.fullWidth()) {
                total += Math.max(linesHeight(row.options().get(0).lines(), optionWidth(), OPTION_LINE_GAP),
                        OPTION_MIN_H) + OPTION_GAP;
            } else {
                float h = OPTION_MIN_H;
                for (RenderedOption o : row.options()) h = Math.max(h, naturalH(o.lines().get(0)));
                total += h + OPTION_GAP;
            }
        }
        return total + NEXT_QUESTION_GAP;
    }

    private static float linesHeight(List<BufferedImage> lines, float maxWidth, float gap) {
        float h = 0;
        for (int i = 0; i < lines.size(); i++) {
            BufferedImage image = lines.get(i);
            h += naturalH(image) * Math.min(1f, maxWidth / naturalW(image));
            if (i < lines.size() - 1) h += gap;
        }
        return h;
    }

    private static float questionWidth() {
        return PDRectangle.A4.getWidth() - PAGE_MARGIN - (PAGE_MARGIN + QUESTION_NO_W);
    }

    private static float optionWidth() {
        return PDRectangle.A4.getWidth() - PAGE_MARGIN - (PAGE_MARGIN + QUESTION_NO_W + OPTION_LABEL_W);
    }

    private static float naturalW(BufferedImage image) {
        return Math.max(1f, (float) (image.getWidth() / RENDER_QUALITY));
    }

    private static float naturalH(BufferedImage image) {
        return Math.max(1f, (float) (image.getHeight() / RENDER_QUALITY));
    }

    private static void text(PDPageContentStream s, String value, float x, float y,
                             float size, boolean bold) throws IOException {
        PDFont font = bold ? BOLD : NORMAL;
        s.beginText();
        s.setFont(font, size);
        s.newLineAtOffset(x, y);
        s.showText(pdfSafe(value));
        s.endText();
    }

    private static void centered(PDPageContentStream s, String value, PDFont font,
                                 float size, float center, float y) throws IOException {
        String v = pdfSafe(value);
        float w = font.getStringWidth(v) / 1000f * size;
        s.beginText();
        s.setFont(font, size);
        s.newLineAtOffset(center - w / 2, y);
        s.showText(v);
        s.endText();
    }

    private static void right(PDPageContentStream s, String value, float right, float y,
                              float size, boolean bold) throws IOException {
        PDFont font = bold ? BOLD : NORMAL;
        String v = pdfSafe(value);
        float w = font.getStringWidth(v) / 1000f * size;
        s.beginText();
        s.setFont(font, size);
        s.newLineAtOffset(right - w, y);
        s.showText(v);
        s.endText();
    }

    private static void line(PDPageContentStream s, float x1, float y1, float x2, float y2) throws IOException {
        s.setLineWidth(.4f);
        s.moveTo(x1, y1);
        s.lineTo(x2, y2);
        s.stroke();
    }

    private static boolean looksLatex(String s) {
        return s != null && (s.contains("\\") || s.contains("^") || s.contains("_") || s.contains("{") || s.contains("}"));
    }

    private static String asLatex(String s) {
        return s == null || s.isBlank() ? "\\text{}" : looksLatex(s) ? s : "\\text{" + escapeText(s) + "}";
    }

    private static String escapeText(String s) {
        return s == null ? "" : s.replace("\\", "\\backslash ").replace("{", "\\{").replace("}", "\\}")
                .replace("%", "\\%").replace("#", "\\#").replace("&", "\\&").replace("_", "\\_").replace("$", "\\$");
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim().replaceAll("\\s+", " ");
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "-" : s.trim();
    }

    private static String label(int i) {
        return i < 26 ? String.valueOf((char) ('A' + i)) : String.valueOf(i + 1);
    }

    private static String pdfSafe(String s) {
        return safe(s).replace('–', '-').replace('—', '-').replace('“', '"').replace('”', '"').replace('’', '\'');
    }

    private static String fileName(ExamData e) {
        return (safe(e.subject()) + "_Class_" + safe(e.classes()) + "_Exam_" + safe(e.id()) + ".pdf").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record RenderedQuestion(int number, String id, List<BufferedImage> questionLines,
                                    List<RenderedOption> options) {
    }

    private record RenderedOption(int id, String label, List<BufferedImage> lines) {
    }

    private record OptionRow(List<RenderedOption> options, boolean fullWidth) {
    }

    private static final class PageState implements AutoCloseable {
        private final PDPageContentStream stream;
        private float y;
        private boolean closed;

        private PageState(PDPageContentStream stream, float y) {
            this.stream = stream;
            this.y = y;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                stream.close();
                closed = true;
            }
        }
    }
}
