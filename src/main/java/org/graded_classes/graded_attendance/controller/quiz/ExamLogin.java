package org.graded_classes.graded_attendance.controller.quiz;

import atlantafx.base.controls.ModalPane;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.R;
import org.graded_classes.graded_attendance.components.KeyHook;
import org.graded_classes.graded_attendance.controller.MainController;
import org.graded_classes.graded_attendance.data.ExamData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExamLogin {
    MainController mainController;
    String rollCode;
    String name;
    String seatNo;
    ExamData examInfo;

    public ExamLogin(MainController mainController, String rollCode, String name, String seatNo, ExamData examInfo) {
        this.mainController = mainController;
        this.rollCode = rollCode;
        this.name = name;
        this.seatNo = seatNo;
        this.examInfo = examInfo;
    }

    private static final int PARTICLE_COUNT = 90;
    private static final double MAX_LINK_DIST = 140.0;
    ModalPane modalPane;
    private final List<Particle> particles = new ArrayList<>(PARTICLE_COUNT);
    Stage stage = new Stage();
    StackPane root;

    public void showLoginScreen() {
        modalPane = new ModalPane();
        StackPane login;
        login = (StackPane)
                mainController.gradedFxmlLoader.
                        createView(R.exam_login,
                                new StudentExamLogin(mainController,
                                        stage, this));
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        root = new StackPane(canvas);
        root.getChildren().add(login);
        root.getChildren().add(modalPane);
        Scene scene = new Scene(root, 1000, 700);

        // IMPORTANT: keep drawing buffer in sync with window size
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        // Create particles once (positions will be clamped when resizing)
        initParticles(canvas.getWidth(), canvas.getHeight());

        // Clamp particles after resize (so they don't end up outside bounds)
        ChangeListener<Number> resizeListener = (obs, oldV, newV) -> clampParticles(canvas.getWidth(), canvas.getHeight());
        canvas.widthProperty().addListener(resizeListener);
        canvas.heightProperty().addListener(resizeListener);

// VERY IMPORTANT: add LAST so it's on top


        // Animation loop (like requestAnimationFrame)
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawFrame(gc, canvas.getWidth(), canvas.getHeight());
            }
        };
        timer.start();
        KeyHook.blockWindowsKey();
        stage.setTitle("Exam Login");
        stage.setFullScreen(true);
        stage.setScene(scene);
        mainController.getStage().close();
        stage.show();
        stage.getScene().setOnKeyPressed(event -> {


            if (event.getCode() == KeyCode.E &&
                    event.isControlDown() &&
                    event.isShiftDown()) {

                showPasswordDialog();
            }


        });

        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });

    }

    private void initParticles(double w, double h) {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(Particle.random(w, h));
        }
    }

    private void clampParticles(double w, double h) {
        for (Particle p : particles) {
            if (p.x < 0) p.x = 0;
            if (p.y < 0) p.y = 0;
            if (p.x > w) p.x = w;
            if (p.y > h) p.y = h;
        }
    }

    private void drawFrame(GraphicsContext gc, double w, double h) {
        // Background (so you ALWAYS see something)
        gc.setFill(Color.web("#0b1220"));
        gc.fillRect(0, 0, w, h);

        // Move + draw particles
        gc.setFill(Color.rgb(255, 255, 255, 0.85));

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            if (p.x < 0 || p.x > w) p.vx *= -1;
            if (p.y < 0 || p.y > h) p.vy *= -1;

            // Draw as circle
            gc.fillOval(p.x - p.r, p.y - p.r, p.r * 2, p.r * 2);
        }

        // Lines between close particles
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);

                double dx = a.x - b.x;
                double dy = a.y - b.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < MAX_LINK_DIST) {
                    double alpha = (1.0 - dist / MAX_LINK_DIST) * 0.22;
                    gc.setStroke(Color.rgb(255, 255, 255, alpha));
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
    }

    private static class Particle {
        double x, y;
        double r;
        double vx, vy;

        static Particle random(double w, double h) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            Particle p = new Particle();
            p.x = rnd.nextDouble(0, Math.max(1, w));
            p.y = rnd.nextDouble(0, Math.max(1, h));
            p.r = 1 + rnd.nextDouble() * 2.2;
            p.vx = -0.6 + rnd.nextDouble() * 1.2;
            p.vy = -0.6 + rnd.nextDouble() * 1.2;
            return p;
        }
    }

    public void showPasswordDialog() {
        Label label = new Label("Please enter your password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        Button submitBtn = new Button("Unlock");
        Label message = new Label();

        VBox content = new VBox(10, label, passwordField, submitBtn, message);

        content.setStyle("""
                    -fx-padding: 20;
                    -fx-background-color: #fafafa;
                    -fx-background-radius: 10;
                """);

        content.setMaxWidth(300);
        content.setMaxHeight(200);

        // ✅ SHOW inside existing modalPane (DON'T CREATE NEW ONE)
        modalPane.show(content);

        passwordField.requestFocus();

        submitBtn.setOnAction(e -> {
            if ("1234".equals(passwordField.getText())) {

                KeyHook.unblockWindowsKey();

                modalPane.hide();
                passwordField.clear();
                message.setText("");

            } else {
                message.setText("Wrong password");
                passwordField.clear();
            }
        });
    }

}