package org.graded_classes.graded_attendance.done_using_ai;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.concurrent.ThreadLocalRandom;

public class FastTextParticles extends Application {

    private static final int COUNT = 10_000;
    private static final double W = 1200, H = 700;

    // Tune these
    private static final double MAX_SPEED = 90;        // px/sec
    private static final double TURN_RATE = 1.2;       // direction change/sec
    private static final double MAX_ANG_SPEED = 180;   // deg/sec
    private static final int FPS_CAP = 60;             // 30 if still heavy

    private final WritableImage[] sprite = new WritableImage[COUNT];
    private final double[] x = new double[COUNT];
    private final double[] y = new double[COUNT];
    private final double[] vx = new double[COUNT];
    private final double[] vy = new double[COUNT];
    private final double[] angle = new double[COUNT];
    private final double[] angVel = new double[COUNT];

    // Used for drawing centered
    private final double[] halfW = new double[COUNT];
    private final double[] halfH = new double[COUNT];

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        initSpritesAndParticles();

        final long frameIntervalNanos = 1_000_000_000L / FPS_CAP;

        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            long accumulator = 0;

            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                long deltaNanos = now - last;
                last = now;

                // FPS cap (prevents rendering too often)
                accumulator += deltaNanos;
                if (accumulator < frameIntervalNanos) return;
                double dt = accumulator / 1_000_000_000.0;
                accumulator = 0;

                // Clear
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, W, H);

                var rnd = ThreadLocalRandom.current();

                for (int i = 0; i < COUNT; i++) {
                    // Smooth random movement: slightly rotate velocity direction instead of heavy jitter
                    // Add small random turn (cheap)
                    double turn = (rnd.nextDouble() * 2 - 1) * TURN_RATE * dt;

                    double cos = Math.cos(turn);
                    double sin = Math.sin(turn);

                    // rotate velocity vector by 'turn'
                    double nvx = vx[i] * cos - vy[i] * sin;
                    double nvy = vx[i] * sin + vy[i] * cos;
                    vx[i] = nvx;
                    vy[i] = nvy;

                    // Move
                    x[i] += vx[i] * dt;
                    y[i] += vy[i] * dt;

                    // Wrap around edges (faster + looks smooth)
                    if (x[i] < -50) x[i] += (W + 100);
                    else if (x[i] > W + 50) x[i] -= (W + 100);

                    if (y[i] < -50) y[i] += (H + 100);
                    else if (y[i] > H + 50) y[i] -= (H + 100);

                    // Rotate
                    angle[i] += angVel[i] * dt;

                    // Draw rotated image around its center
                    WritableImage img = sprite[i];

                    gc.save();
                    gc.translate(x[i], y[i]);
                    gc.rotate(angle[i]);
                    gc.drawImage(img, -halfW[i], -halfH[i]);
                    gc.restore();
                }
            }
        };

        timer.start();
        stage.setScene(new Scene(new Group(canvas), W, H, Color.BLACK));
        stage.setTitle("Fast 10,000 Text Particles (Pre-rendered)");
        stage.show();
    }

    private void initSpritesAndParticles() {
        var rnd = ThreadLocalRandom.current();
        Font font = Font.font("Consolas", 12);

        // A tiny offscreen canvas to render each string once
        Canvas off = new Canvas(160, 40); // enough for short words
        GraphicsContext og = off.getGraphicsContext2D();
        og.setFont(font);
        og.setTextAlign(TextAlignment.CENTER);
        og.setTextBaseline(VPos.CENTER);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        for (int i = 0; i < COUNT; i++) {
            String s = randomWord(rnd, 3 + rnd.nextInt(8));
            Color c = Color.color(
                    0.3 + 0.7 * rnd.nextDouble(),
                    0.3 + 0.7 * rnd.nextDouble(),
                    0.3 + 0.7 * rnd.nextDouble(),
                    0.95
            );

            // Render once
            og.clearRect(0, 0, off.getWidth(), off.getHeight());
            og.setFill(c);
            og.fillText(s, off.getWidth() / 2, off.getHeight() / 2);

            WritableImage img = new WritableImage((int) off.getWidth(), (int) off.getHeight());
            off.snapshot(params, img);

            sprite[i] = img;
            halfW[i] = img.getWidth() / 2.0;
            halfH[i] = img.getHeight() / 2.0;

            // Initial state
            x[i] = rnd.nextDouble(W);
            y[i] = rnd.nextDouble(H);

            // Random velocity at constant speed
            double a = rnd.nextDouble(Math.PI * 2);
            double sp = 20 + rnd.nextDouble(MAX_SPEED);
            vx[i] = Math.cos(a) * sp;
            vy[i] = Math.sin(a) * sp;

            angle[i] = rnd.nextDouble(360);
            angVel[i] = (rnd.nextDouble() * 2 - 1) * MAX_ANG_SPEED;
        }
    }

    private static String randomWord(ThreadLocalRandom rnd, int len) {
        char[] c = new char[len];
        for (int i = 0; i < len; i++) c[i] = (char) ('A' + rnd.nextInt(26));
        return new String(c);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
