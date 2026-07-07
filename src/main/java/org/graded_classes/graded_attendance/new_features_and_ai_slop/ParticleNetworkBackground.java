package org.graded_classes.graded_attendance.new_features_and_ai_slop;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ParticleNetworkBackground extends Application {

    private static final int PARTICLE_COUNT = 90;
    private static final double MAX_LINK_DIST = 140.0;

    private final List<Particle> particles = new ArrayList<>(PARTICLE_COUNT);

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
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

        // Animation loop (like requestAnimationFrame)
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawFrame(gc, canvas.getWidth(), canvas.getHeight());
            }
        };
        timer.start();

        stage.setTitle("JavaFX Particle Network Background");
        stage.setScene(scene);
        stage.show();
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

    public static void main(String[] args) {
        launch(args);
    }
}