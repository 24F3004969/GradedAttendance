package org.graded_classes.graded_attendance.components;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

public class OMRGeneratorApp extends Application {

    // Sheet size
    public static final double SHEET_WIDTH = 800;
    public static final double SHEET_HEIGHT = 1200;

    // Marker settings
    public static final double MARKER_SIZE = 40;
    public static final double MARKER_LEFT = 30;
    public static final double MARKER_TOP = 30;
    public static final double MARKER_RIGHT = 730;
    public static final double MARKER_BOTTOM = 1130;

    // Bubble settings
    public static final double BUBBLE_RADIUS = 8;
    public static final double BUBBLE_STROKE_WIDTH = 1.5;

    // Layout settings
    public static final int TOTAL_QUESTIONS = 70;
    public static final int QUESTIONS_PER_COLUMN = 35;

    public static final double LEFT_COLUMN_X = 90;
    public static final double RIGHT_COLUMN_X = 420;

    public static final double FIRST_ROW_Y = 250;
    public static final double QUESTION_GAP_Y = 22;

    public static final double QUESTION_NUMBER_GAP = 45;
    public static final double OPTION_GAP_X = 50;

    private Pane currentOMRPane;

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        Button generateBtn = new Button("Generate OMR");
        Button savePdfBtn = new Button("Save PDF");

        ScrollPane scroll = new ScrollPane();

        generateBtn.setOnAction(e -> {
            currentOMRPane = createOMRSheet();
            scroll.setContent(currentOMRPane);
        });

        savePdfBtn.setOnAction(e -> {
            if (currentOMRPane == null) {
                currentOMRPane = createOMRSheet();
                scroll.setContent(currentOMRPane);
            }

            savePaneAsPdf(currentOMRPane, "omr-sheet.pdf");
        });

        BorderPane topBar = new BorderPane();
        topBar.setLeft(generateBtn);
        topBar.setRight(savePdfBtn);

        root.setTop(topBar);
        root.setCenter(scroll);

        Scene scene = new Scene(root, 1000, 800);

        stage.setTitle("OMR Generator");
        stage.setScene(scene);
        stage.show();
    }

    private Pane createOMRSheet() {

        Pane pane = new Pane();

        pane.setPrefSize(SHEET_WIDTH, SHEET_HEIGHT);
        pane.setMinSize(SHEET_WIDTH, SHEET_HEIGHT);
        pane.setMaxSize(SHEET_WIDTH, SHEET_HEIGHT);

        // Use red border only for debugging/preview.
        // Remove -fx-border-color:red before final printing if you want.

        /*
         * All actual OMR elements go inside this content pane.
         * Then we center this content pane inside the main sheet pane.
         */
        Pane content = new Pane();

        // Corner markers
        content.getChildren().add(createMarker(MARKER_LEFT, MARKER_TOP));
        content.getChildren().add(createMarker(MARKER_RIGHT, MARKER_TOP));
        content.getChildren().add(createMarker(MARKER_LEFT, MARKER_BOTTOM));
        content.getChildren().add(createMarker(MARKER_RIGHT, MARKER_BOTTOM));

        // Title
        Text title = new Text(
                260,
                70,
                "GradeEd Coaching Classes"
        );
        title.setFont(Font.font(18));
        content.getChildren().add(title);

        Text subTitle = new Text(
                310,
                100,
                "OMR ANSWER SHEET"
        );
        subTitle.setFont(Font.font(14));
        content.getChildren().add(subTitle);

        // Student info
        Text nameText = new Text(
                90,
                145,
                "Student Name: __________________________"
        );
        nameText.setFont(Font.font(13));

        Text rollText = new Text(
                430,
                145,
                "Roll No: ______________"
        );
        rollText.setFont(Font.font(13));

        content.getChildren().add(nameText);
        content.getChildren().add(rollText);

        // Instructions
        Text instruction = new Text(
                90,
                180,
                "Instructions: Fill the bubble completely using black/blue pen. Do not tick, cross, or mark outside bubbles."
        );
        instruction.setFont(Font.font(11));
        content.getChildren().add(instruction);

        // Column headers
        addHeader(
                content,
                LEFT_COLUMN_X,
                FIRST_ROW_Y - 30
        );

        addHeader(
                content,
                RIGHT_COLUMN_X,
                FIRST_ROW_Y - 30
        );

        // Questions
        for (int q = 1; q <= TOTAL_QUESTIONS; q++) {

            boolean rightColumn =
                    q > QUESTIONS_PER_COLUMN;

            int row;
            double columnX;

            if (rightColumn) {
                row = q - QUESTIONS_PER_COLUMN - 1;
                columnX = RIGHT_COLUMN_X;
            } else {
                row = q - 1;
                columnX = LEFT_COLUMN_X;
            }

            double y =
                    FIRST_ROW_Y + row * QUESTION_GAP_Y;

            addQuestionRow(
                    content,
                    q,
                    columnX,
                    y
            );
        }

        // Add content first
        pane.getChildren().add(content);

        // Center all OMR content inside the page
        centerContent(
                pane,
                content
        );

        return pane;
    }
    private void centerContent(
            Pane sheet,
            Pane content
    ) {

        content.applyCss();
        content.layout();

        double contentMinX =
                content.getBoundsInLocal().getMinX();

        double contentMinY =
                content.getBoundsInLocal().getMinY();

        double contentWidth =
                content.getBoundsInLocal().getWidth();

        double contentHeight =
                content.getBoundsInLocal().getHeight();

        double offsetX =
                (SHEET_WIDTH - contentWidth) / 2
                        - contentMinX;

        double offsetY =
                (SHEET_HEIGHT - contentHeight) / 2
                        - contentMinY;

        content.setLayoutX(offsetX);
        content.setLayoutY(offsetY);
    }
    private void addHeader(Pane pane, double columnX, double y) {

        Text qNo = new Text(columnX, y, "Q.No");
        qNo.setFont(Font.font(12));

        Text a = new Text(columnX + QUESTION_NUMBER_GAP, y, "A");
        Text b = new Text(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X, y, "B");
        Text c = new Text(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X * 2, y, "C");
        Text d = new Text(columnX + QUESTION_NUMBER_GAP + OPTION_GAP_X * 3, y, "D");

        a.setFont(Font.font(12));
        b.setFont(Font.font(12));
        c.setFont(Font.font(12));
        d.setFont(Font.font(12));

        pane.getChildren().addAll(qNo, a, b, c, d);
    }

    private void addQuestionRow(
            Pane pane,
            int questionNo,
            double columnX,
            double y
    ) {

        Text qText = new Text(
                columnX,
                y + 5,
                String.valueOf(questionNo)
        );

        qText.setFont(Font.font(13));
        pane.getChildren().add(qText);

        for (int option = 0; option < 4; option++) {

            double bubbleX =
                    columnX
                            + QUESTION_NUMBER_GAP
                            + option * OPTION_GAP_X;

            Circle circle = new Circle(
                    bubbleX,
                    y,
                    BUBBLE_RADIUS
            );

            circle.setFill(Color.WHITE);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(BUBBLE_STROKE_WIDTH);

            pane.getChildren().add(circle);
        }
    }

    private Rectangle createMarker(
            double x,
            double y
    ) {

        Rectangle marker = new Rectangle(
                x,
                y,
                MARKER_SIZE,
                MARKER_SIZE
        );

        marker.setFill(Color.BLACK);

        return marker;
    }

    private void savePaneAsPdf(
            Pane pane,
            String outputFile
    ) {

        try {

            SnapshotParameters params =
                    new SnapshotParameters();

            params.setTransform(new Scale(3, 3));
            params.setFill(Color.WHITE);

            WritableImage image =
                    pane.snapshot(params, null);

            PDDocument document =
                    new PDDocument();

            PDPage page =
                    new PDPage(PDRectangle.A4);

            document.addPage(page);

            PDImageXObject pdfImage =
                    LosslessFactory.createFromImage(
                            document,
                            SwingFXUtils.fromFXImage(
                                    image,
                                    null
                            )
                    );

            PDPageContentStream contentStream =
                    new PDPageContentStream(
                            document,
                            page
                    );

            contentStream.drawImage(
                    pdfImage,
                    0,
                    0,
                    page.getMediaBox().getWidth(),
                    page.getMediaBox().getHeight()
            );

            contentStream.close();

            document.save(outputFile);
            document.close();

            System.out.println("High quality PDF saved: " + outputFile);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save PDF", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}