package org.graded_classes.graded_attendance.test;

import atlantafx.base.theme.PrimerLight;
import com.gluonhq.emoji.Emoji;
import com.gluonhq.emoji.EmojiData;
import com.gluonhq.emoji.EmojiLoaderFactory;
import com.gluonhq.emoji.util.EmojiImageUtils;
import com.gluonhq.emoji.util.TextUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class Demo extends Application {

    private StackPane root;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        root = new StackPane();
        Scene scene = new Scene(root, 1230, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        if (EmojiLoaderFactory.getEmojiImageLoader().isInitialized()) {
            addEmojis();
        } else {
            ProgressIndicator progressIndicator = new ProgressIndicator();
            root.getChildren().setAll(progressIndicator);
            EmojiLoaderFactory.getEmojiImageLoader().initialize().thenAccept(aBoolean -> {
                if (aBoolean) {
                    addEmojis();
                }
            });
        }
    }

    private void addEmojis() {
        TextFlow textFlow = new TextFlow();
        Scene scene = root.getScene();
        textFlow.prefWidthProperty().bind(scene.widthProperty().subtract(30));
        List<Node> emojiNodes = processEmojis();
        // Add nodes to textFlow
        Platform.runLater(() -> {
            textFlow.getChildren().addAll(emojiNodes);
            root.getChildren().setAll(new ScrollPane(textFlow));
            ((Stage)scene.getWindow()).setTitle("Emoji nodes: " +
                    textFlow.getChildren().stream().filter(ImageView.class::isInstance).count());
        });
    }

    private List<Node> processEmojis() {
        // Get a single text string with all unicode characters for all emojis
        String text = getAllEmojisAsUnicodeString();
        // Convert text to Text (if delimiter is not empty) and Image nodes (all emojis)
        List<Node> emojiNodes = TextUtils.convertToTextAndImageNodes(text,24);
        emojiNodes.stream()
                .filter(ImageView.class::isInstance)
                .forEach(node -> {
                    String unified = (String) node.getProperties().get(EmojiImageUtils.IMAGE_VIEW_EMOJI_PROPERTY);
                    EmojiData.emojiFromCodepoints(unified).ifPresent(emoji ->
                            Tooltip.install(node, new Tooltip("Emoji: " + emoji.getName() + "\n" + unified)));
                });
        return emojiNodes;
    }

    /**
     * Get a text string with the unicode characters of all emojis, in the same
     * order as the sheet image
     * https://github.com/iamcal/emoji-data/blob/master/sheet_apple_64.png
     *
     * @return a text string
     */
    private static String getAllEmojisAsUnicodeString() {
        return EmojiData.getEmojiCollection().stream()
                .sorted((e1, e2) -> {
                    if (e1.getSheetY() == e2.getSheetY()) {
                        return Integer.compare(e1.getSheetX(), e2.getSheetX());
                    }
                    return Integer.compare(e1.getSheetY(), e2.getSheetY());
                })
                .map(Emoji::character)
                // optional: add a delimiter like `|` to separate emojis
                .collect(Collectors.joining(""));
    }
}