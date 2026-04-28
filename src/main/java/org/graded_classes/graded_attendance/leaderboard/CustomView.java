package org.graded_classes.graded_attendance.leaderboard;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.graded_classes.graded_attendance.GradedResourceLoader;

public class CustomView extends HBox {
    final private String text1, text2, text3;
    private final String image_src;
    private final VBox layer1;
    private final HBox layer2;
    Text t1 = new Text(), t2 = new Text(), t3 = new Text();

    public Text getText1() {
        return t1;
    }

    public Text getText2() {
        return t2;
    }

    public Text getText3() {
        return t3;
    }

    private final HBox image_layer;
    private final HBox numPad_Back;
    private final StackPane numPad;
    private final int id;

    public CustomView(int id, String image_src, String text1,
                      String text2, String text3, String bg_color) {
        this.text1 = text1;
        this.text2 = text2;
        this.text3 = text3;
        this.id = id;
        this.image_src = image_src;
        layer1 = new VBox();
        layer2 = new HBox();
        numPad_Back = new HBox();
        numPad = new StackPane();
        image_layer = new HBox();
        this.getStyleClass().add("custom-view");
        this.setStyle("-fx-background-color: " + bg_color + ";");
        String substring = bg_color.substring(0, bg_color.length() - 2);
        numPad_Back.setStyle("-fx-background-color :" + substring + ";" + "-fx-background-radius: 10;");
        numPad.setStyle("-fx-background-color :#fafafa;-fx-background-radius: 5;");
        init(substring);
    }

    public CustomView(int id, String text1, String text2, String text3, String bg_color) {
        this(id, null, text1, text2, text3, bg_color);
    }

    private void init(String code) {
        this.setSpacing(15);

        this.setPadding(new Insets(10, 10, 10, 10));
        this.setFillHeight(true);
        this.setMaxSize(Double.MAX_VALUE, Region.USE_COMPUTED_SIZE);
        ImageView imageViewer = new ImageView();
        if (image_src != null)
            imageViewer = getImageView();
        t1.setText(text1);
        t2.setText(text2);
        t3.setText(text3);
        t2.setWrappingWidth(250);
        t1.setWrappingWidth(250);
        t3.setStyle("-fx-fill:" + code + ";");
        if (image_src != null)
            image_layer.getChildren().add(imageViewer);
        image_layer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setMargin(t1, new Insets(5, 5, 5, 5));
        HBox.setMargin(t2, new Insets(5, 5, 5, 5));
        HBox.setMargin(t3, new Insets(5, 5, 5, 5));
        HBox.setMargin(t3, new Insets(10, 10, 10, 10));
        HBox.setHgrow(t1, Priority.ALWAYS);
        HBox.setHgrow(t2, Priority.ALWAYS);
        HBox.setHgrow(t3, Priority.ALWAYS);
        HBox.setHgrow(image_layer, Priority.ALWAYS);
        layer1.setFillWidth(true);
        layer2.setFillHeight(true);
        HBox.setHgrow(layer1, Priority.ALWAYS);
        //HBox.setHgrow(layer2, Priority.ALWAYS);
        layer1.setMaxWidth(Double.MAX_VALUE);
        layer1.getChildren().addAll(t1, t2);
        layer2.setMaxWidth(Double.MAX_VALUE);
        layer2.setAlignment(Pos.TOP_RIGHT);
        numPad_Back.setMinSize(40, 30);
        numPad.setMinSize(25, 25);
        numPad.setAlignment(Pos.CENTER);
        numPad.getChildren().add(new Text("" + this.id));
        layer2.getChildren().addAll(t3);
        numPad_Back.setAlignment(Pos.CENTER);
        numPad_Back.getChildren().add(numPad);
        image_layer.setPadding(new Insets(0, 50, 0, 0));
        numPad_Back.setPadding(new Insets(10, 5, 10, 5));
        this.getChildren().addAll(numPad_Back, layer1, image_layer, layer2);

    }

    private ImageView getImageView() {
        ImageView imageViewer = new ImageView(new Image(GradedResourceLoader.loadURL(image_src).toString()));
        imageViewer.setFitWidth(100);
        imageViewer.setFitHeight(100);
        imageViewer.setPreserveRatio(true);
        imageViewer.setSmooth(true);
        return imageViewer;
    }
}
