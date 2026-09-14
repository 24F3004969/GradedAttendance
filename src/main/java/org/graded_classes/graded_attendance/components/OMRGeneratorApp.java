package org.graded_classes.graded_attendance.components;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Generates the fixed 90-question OMR sheet used by OmrReader2.
 *
 * The sheet contains:
 * - Four black registration markers
 * - Six-digit roll/enrollment number OMR grid
 * - Ninety questions in three columns of 30, each having A, B, C and D
 */
public final class OMRGeneratorApp extends Application {

    public static final double SHEET_WIDTH = 800;
    public static final double SHEET_HEIGHT = 1200;

    public static final double MARKER_SIZE = 40;
    public static final double MARKER_LEFT = 30;
    public static final double MARKER_TOP = 30;
    public static final double MARKER_RIGHT = 730;
    public static final double MARKER_BOTTOM = 1130;

    public static final double BUBBLE_RADIUS = 8;
    public static final double BUBBLE_STROKE_WIDTH = 1.5;

    public static final int TOTAL_QUESTIONS = 90;
    public static final int QUESTIONS_PER_COLUMN = 30;

    public static final int QUESTION_COLUMNS = 3;
    public static final double[] QUESTION_COLUMN_X = {55, 310, 565};

    // Questions were moved down slightly to make room for roll-number bubbles.
    public static final double FIRST_ROW_Y = 350;
    public static final double QUESTION_GAP_Y = 25;

    public static final double QUESTION_NUMBER_GAP = 35;
    public static final double OPTION_GAP_X = 36;

    // Six-column roll/enrollment number grid.
    public static final int ROLL_DIGITS = 6;
    public static final int ROLL_VALUES = 7;
    public static final double ROLL_LABEL_X = 482;
    public static final double ROLL_FIRST_COLUMN_X = 515;
    public static final double ROLL_COLUMN_GAP_X = 33;
    public static final double ROLL_FIRST_ROW_Y = 148;
    public static final double ROLL_ROW_GAP_Y = 20;
    public static final double ROLL_BUBBLE_RADIUS = 8.0;
    public static final double ROLL_BUBBLE_STROKE_WIDTH = 1.3;

    private Pane currentOMRPane;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Button generateButton = new Button("Generate OMR");
        Button savePdfButton = new Button("Save PDF");

        HBox toolbar = new HBox(10, generateButton, savePdfButton);
        toolbar.setPadding(new Insets(8));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        generateButton.setOnAction(event -> {
            currentOMRPane = createOMRSheet();
            scrollPane.setContent(currentOMRPane);
        });

        savePdfButton.setOnAction(event -> {
            if (currentOMRPane == null) {
                currentOMRPane = createOMRSheet();
                scrollPane.setContent(currentOMRPane);
            }
            savePaneAsPdf(currentOMRPane, "omr-sheet.pdf");
        });

        currentOMRPane = createOMRSheet();
        scrollPane.setContent(currentOMRPane);

        root.setTop(toolbar);
        root.setCenter(scrollPane);

        stage.setTitle("OMR Generator With Roll Number");
        stage.setScene(new Scene(root, 1000, 800));
        stage.show();
    }

    public Pane createOMRSheet() {
        Pane sheet = new Pane();
        sheet.setPrefSize(SHEET_WIDTH, SHEET_HEIGHT);
        sheet.setMinSize(SHEET_WIDTH, SHEET_HEIGHT);
        sheet.setMaxSize(SHEET_WIDTH, SHEET_HEIGHT);
        sheet.setStyle("-fx-background-color: white;");

        addRegistrationMarkers(sheet);
        addTitle(sheet);
        addStudentInformation(sheet);
        addInstructions(sheet);

        addRollNumberGrid(sheet);

        for (double columnX : QUESTION_COLUMN_X) {
            addHeader(sheet, columnX, FIRST_ROW_Y - 27);
        }

        for (int question = 1; question <= TOTAL_QUESTIONS; question++) {
            int zeroBasedQuestion = question - 1;
            int columnIndex = zeroBasedQuestion / QUESTIONS_PER_COLUMN;
            int rowIndex = zeroBasedQuestion % QUESTIONS_PER_COLUMN;

            double columnX = QUESTION_COLUMN_X[columnIndex];
            double y = FIRST_ROW_Y + rowIndex * QUESTION_GAP_Y;

            addQuestionRow(sheet, question, columnX, y);
        }

        return sheet;
    }

    private void addRegistrationMarkers(Pane sheet) {
        sheet.getChildren().addAll(
                createMarker(MARKER_LEFT, MARKER_TOP),
                createMarker(MARKER_RIGHT, MARKER_TOP),
                createMarker(MARKER_LEFT, MARKER_BOTTOM),
                createMarker(MARKER_RIGHT, MARKER_BOTTOM)
        );
    }

    private void addTitle(Pane sheet) {
        Text title = new Text(274, 68, "GradeEd Coaching Classes");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Text subtitle = new Text(285, 95, "OMR ANSWER SHEET - 90 QUESTIONS");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 13));

        sheet.getChildren().addAll(title, subtitle);
    }

    private void addStudentInformation(Pane sheet) {
        Text name = new Text(90, 130, "Student Name: __________________________");
        name.setFont(Font.font(12));
        sheet.getChildren().addAll(name);
    }

    private void addInstructions(Pane sheet) {
        Text instruction = new Text(
                90,
                292,
                "Instructions: Fill one bubble in every roll-number column and one option per question using black/blue pen."
        );

        instruction.setFont(Font.font(10));
        sheet.getChildren().add(instruction);
    }

    /**
     * Draws six large, clearly separated digit columns. Each column contains digits 0 through 9.
     * Example: for 123456, fill 1 in column 1, 2 in column 2, etc.
     */
    private void addRollNumberGrid(Pane sheet) {
        // Position headers: 1, 2, 3, 4, 5, 6.
        for (int position = 0; position < ROLL_DIGITS; position++) {
            double x = rollColumnX(position);
            Text positionLabel = new Text(x - 3, ROLL_FIRST_ROW_Y - 13,
                    String.valueOf(position ));
            positionLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            sheet.getChildren().add(positionLabel);
        }

        // Digit labels and bubbles.
        for (int digit = 0; digit < ROLL_VALUES; digit++) {
            double y = rollDigitY(digit);

            Text digitLabel = new Text(ROLL_LABEL_X, y + 3, String.valueOf(digit));
            digitLabel.setFont(Font.font(12));
            sheet.getChildren().add(digitLabel);

            for (int position = 0; position < ROLL_DIGITS; position++) {
                Circle bubble = new Circle(
                        rollColumnX(position),
                        y,
                        ROLL_BUBBLE_RADIUS
                );
                bubble.setFill(Color.WHITE);
                bubble.setStroke(Color.BLACK);
                bubble.setStrokeWidth(ROLL_BUBBLE_STROKE_WIDTH);
                sheet.getChildren().add(bubble);
            }
        }
    }

    public static double rollColumnX(int zeroBasedPosition) {
        return ROLL_FIRST_COLUMN_X + zeroBasedPosition * ROLL_COLUMN_GAP_X;
    }

    public static double rollDigitY(int digit) {
        return ROLL_FIRST_ROW_Y + digit * ROLL_ROW_GAP_Y;
    }

    private void addHeader(Pane sheet, double columnX, double y) {
        Text questionNumber = createText(columnX, y, "Q.No", 12);
        Text a = createText(columnX + QUESTION_NUMBER_GAP, y, "A", 12);
        Text b = createText(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X, y, "B", 12);
        Text c = createText(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X * 2, y, "C", 12);
        Text d = createText(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X * 3, y, "D", 12);
        sheet.getChildren().addAll(questionNumber, a, b, c, d);
    }

    private void addQuestionRow(
            Pane sheet,
            int questionNumber,
            double columnX,
            double y
    ) {
        Text number = createText(columnX, y + 5, String.valueOf(questionNumber), 12);
        sheet.getChildren().add(number);

        for (int option = 0; option < 4; option++) {
            double bubbleX = columnX
                    + QUESTION_NUMBER_GAP
                    + option * OPTION_GAP_X;

            Circle bubble = new Circle(bubbleX, y, BUBBLE_RADIUS);
            bubble.setFill(Color.WHITE);
            bubble.setStroke(Color.BLACK);
            bubble.setStrokeWidth(BUBBLE_STROKE_WIDTH);
            sheet.getChildren().add(bubble);
        }
    }

    private Text createText(double x, double y, String value, double size) {
        Text text = new Text(x, y, value);
        text.setFont(Font.font(size));
        return text;
    }

    private Rectangle createMarker(double x, double y) {
        Rectangle marker = new Rectangle(x, y, MARKER_SIZE, MARKER_SIZE);
        marker.setFill(Color.BLACK);
        return marker;
    }

    private void savePaneAsPdf(Pane pane, String outputFile) {
        pane.applyCss();
        pane.layout();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setTransform(new Scale(3, 3));
        parameters.setFill(Color.WHITE);

        WritableImage writableImage = pane.snapshot(parameters, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject pdfImage = LosslessFactory.createFromImage(
                    document,
                    bufferedImage
            );

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.drawImage(
                        pdfImage,
                        0,
                        0,
                        page.getMediaBox().getWidth(),
                        page.getMediaBox().getHeight()
                );
            }

            document.save(outputFile);
            System.out.println("High-quality OMR PDF saved: " + outputFile);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to save OMR PDF: " + outputFile,
                    exception
            );
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
