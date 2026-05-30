package org.graded_classes.graded_attendance.done_using_ai;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;       // PDFBox
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChartToPdfWithData extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1) Build a BarChart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Sales by Quarter");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefSize(800, 500);

        // 2) ADD DATA — without this, there is nothing to plot!
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.getData().add(new XYChart.Data<>("Q1", 42));
        s.getData().add(new XYChart.Data<>("Q2", 55));
        s.getData().add(new XYChart.Data<>("Q3", 31));
        s.getData().add(new XYChart.Data<>("Q4", 68));
        chart.getData().add(s);

        // 3) Put chart into a Scene so CSS/layout run, then force them
        StackPane root = new StackPane(chart);
        Scene scene = new Scene(root, 820, 520);
        // Don’t need to show a window to layout:
        root.applyCss();   // ensure CSS applied
        root.layout();     // ensure sizes and plot computed  ← important! [1](https://srclog.com/PDFViewerFX)[2](https://examples.javacodegeeks.com/java-development/desktop-java/javafx/javafx-print-api/)

        // 4) High‑DPI snapshot with white background (improves clarity)
        double scale = 2.5;
        SnapshotParameters sp = new SnapshotParameters();
        sp.setTransform(javafx.scene.transform.Transform.scale(scale, scale));
        sp.setFill(Color.WHITE);

        WritableImage fxImg = chart.snapshot(sp, null);
        BufferedImage awtImg = SwingFXUtils.fromFXImage(fxImg, null);

        // 5) Create a one‑page PDF and place the image properly
        Path pdfPath = Files.createTempFile("node-export-", ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Encode image to PNG in-memory
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(awtImg, "PNG", png);

            float margin = 36f; // 0.5"
            PDRectangle box = page.getMediaBox();
            float availW = box.getWidth() - 2 * margin;
            float availH = box.getHeight() - 2 * margin;

            float imgW = awtImg.getWidth();
            float imgH = awtImg.getHeight();
            float fit = Math.min(availW / imgW, availH / imgH);
            float drawW = imgW * fit;
            float drawH = imgH * fit;
            float x = margin + (availW - drawW) / 2f;
            float y = margin + (availH - drawH) / 2f;

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, png.toByteArray(), "chart");
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, x, y, drawW, drawH);
            }
            doc.save(pdfPath.toFile());
        }

       /* // 6) View it with PDFViewFX (JavaFX PDF viewer)
        PDFView pdfView = new PDFView();
        pdfView.load(new File(pdfPath.toFile().getAbsolutePath())); // "file:///..."
        Scene viewerScene = new Scene(new StackPane(pdfView), 900, 640);*/
        //stage.setScene(viewerScene);
        // stage.setTitle("PDFViewFX – Chart PDF");
        // stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}