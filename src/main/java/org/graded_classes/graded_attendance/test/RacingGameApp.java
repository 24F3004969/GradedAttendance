package org.graded_classes.graded_attendance.test;



import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RacingGameApp extends Application {

    // Window
    private static final int W = 480;
    private static final int H = 720;

    // Road layout
    private static final double ROAD_MARGIN = 80;
    private static final double ROAD_LEFT = ROAD_MARGIN;
    private static final double ROAD_RIGHT = W - ROAD_MARGIN;
    private static final double ROAD_WIDTH = ROAD_RIGHT - ROAD_LEFT;

    // Player
    private final Car player = new Car(W / 2.0 - 18, H - 140, 36, 70, Color.DODGERBLUE);

    // Enemies
    private final List<Car> enemies = new ArrayList<>();
    private final Random rng = new Random();

    // Input
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Game state
    private boolean running = false;
    private boolean gameOver = false;

    // Difficulty / pacing
    private double baseSpeed = 260;         // px/sec
    private double speed = baseSpeed;
    private double spawnTimer = 0;
    private double spawnInterval = 0.9;     // seconds
    private long lastNs = 0;

    // Score
    private double score = 0;

    // Visual
    private double dashOffset = 0;

    // Public API text measurement helper (no com.sun.*)
    private static final Text MEASURE = new Text();

    private static double textWidth(String s, Font font) {
        MEASURE.setFont(font);
        MEASURE.setText(s);
        return MEASURE.getLayoutBounds().getWidth();
    }

    private static double textHeight(String s, Font font) {
        MEASURE.setFont(font);
        MEASURE.setText(s);
        return MEASURE.getLayoutBounds().getHeight();
    }

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();

        Group root = new Group(canvas);
        Scene scene = new Scene(root, W, H, Color.BLACK);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) leftPressed = true;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) rightPressed = true;

            if (e.getCode() == KeyCode.SPACE) {
                if (!running && !gameOver) {
                    running = true; // start
                } else if (gameOver) {
                    resetGame();
                }
            }
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.A) leftPressed = false;
            if (e.getCode() == KeyCode.RIGHT || e.getCode() == KeyCode.D) rightPressed = false;
        });

        stage.setTitle("JavaFX Racing (Canvas + AnimationTimer)");
        stage.setScene(scene);
        stage.show();

        AnimationTimer loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNs == 0) lastNs = now;
                double dt = (now - lastNs) / 1_000_000_000.0;
                lastNs = now;

                update(dt);
                render(g);
            }
        };
        loop.start();
    }

    private void resetGame() {
        enemies.clear();
        score = 0;
        baseSpeed = 260;
        speed = baseSpeed;
        spawnTimer = 0;
        spawnInterval = 0.9;
        dashOffset = 0;

        player.x = W / 2.0 - player.w / 2.0;
        player.y = H - 140;

        running = false;
        gameOver = false;
    }

    private void update(double dt) {
        // Title screen still animates road
        dashOffset += speed * dt * 0.6;

        if (!running || gameOver) return;

        // Difficulty ramp: speed increases and spawn interval decreases
        speed += 12 * dt; // gradually speed up
        spawnInterval = Math.max(0.38, 0.9 - (score / 2500.0)); // more score => faster spawns

        // Score: survival time-based
        score += 110 * dt;

        // Player movement
        double playerSpeed = 420; // px/sec
        if (leftPressed) player.x -= playerSpeed * dt;
        if (rightPressed) player.x += playerSpeed * dt;

        // Clamp within road bounds
        double minX = ROAD_LEFT + 12;
        double maxX = ROAD_RIGHT - player.w - 12;
        player.x = clamp(player.x, minX, maxX);

        // Spawn enemies
        spawnTimer += dt;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0;
            spawnEnemy();
        }

        // Move enemies and remove off-screen
        Iterator<Car> it = enemies.iterator();
        while (it.hasNext()) {
            Car c = it.next();
            c.y += speed * dt;

            if (c.y > H + 100) {
                it.remove();
                continue;
            }

            // Collision
            if (player.intersects(c)) {
                gameOver = true;
                running = false;
                break;
            }
        }
    }

    private void spawnEnemy() {
        // Enemy size similar to player with small variance
        double ew = 34 + rng.nextInt(8);
        double eh = 64 + rng.nextInt(14);

        // Pick a lane-ish x position (3 lanes)
        int lane = rng.nextInt(3); // 0,1,2
        double laneWidth = ROAD_WIDTH / 3.0;

        double x = ROAD_LEFT + lane * laneWidth + (laneWidth - ew) / 2.0;

        // Add slight jitter so it feels less robotic
        x += (rng.nextDouble() - 0.5) * 18;

        // Avoid spawning too close horizontally to existing cars near top (simple check)
        for (Car other : enemies) {
            if (other.y < 140 && Math.abs(other.x - x) < 45) {
                x += laneWidth * (rng.nextBoolean() ? 1 : -1);
                break;
            }
        }

        x = clamp(x, ROAD_LEFT + 12, ROAD_RIGHT - ew - 12);

        Color color = Color.hsb(rng.nextDouble() * 360, 0.9, 0.95);
        enemies.add(new Car(x, -80, ew, eh, color));
    }

    private void render(GraphicsContext g) {
        // Background
        g.setFill(Color.rgb(14, 15, 18));
        g.fillRect(0, 0, W, H);

        // Road
        g.setFill(Color.rgb(40, 42, 48));
        g.fillRect(ROAD_LEFT, 0, ROAD_WIDTH, H);

        // Road edges
        g.setStroke(Color.rgb(220, 220, 220, 0.9));
        g.setLineWidth(5);
        g.strokeLine(ROAD_LEFT, 0, ROAD_LEFT, H);
        g.strokeLine(ROAD_RIGHT, 0, ROAD_RIGHT, H);

        // Center dashed lane separators (3 lanes => 2 separators)
        g.setStroke(Color.rgb(240, 240, 240, 0.75));
        g.setLineWidth(4);
        double laneWidth = ROAD_WIDTH / 3.0;
        double x1 = ROAD_LEFT + laneWidth;
        double x2 = ROAD_LEFT + 2 * laneWidth;

        drawDashes(g, x1, dashOffset);
        drawDashes(g, x2, dashOffset);

        // Enemies
        for (Car e : enemies) e.draw(g);

        // Player
        player.draw(g);

        // HUD
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Consolas", 18));
        g.fillText("Score: " + (int) score, 16, 28);
        g.fillText("Speed: " + (int) speed, 16, 50);

        // Overlays
        if (!running && !gameOver) {
            overlayCentered(g,
                    "JAVA FX RACING",
                    "← / → or A / D to steer\nPress SPACE to start");
        } else if (gameOver) {
            overlayCentered(g,
                    "GAME OVER",
                    "Score: " + (int) score + "\nPress SPACE to restart");
        }
    }

    private void drawDashes(GraphicsContext g, double x, double offset) {
        double dashH = 38;
        double gap = 26;
        double total = dashH + gap;

        // offset downward (mod by total)
        double start = -(offset % total);

        for (double y = start; y < H; y += total) {
            g.strokeLine(x, y, x, y + dashH);
        }
    }

    private void overlayCentered(GraphicsContext g, String title, String subtitle) {
        // translucent panel
        g.setFill(Color.rgb(0, 0, 0, 0.55));
        g.fillRect(0, 0, W, H);

        // Title
        Font titleFont = Font.font("Arial Black", 44);
        g.setFill(Color.WHITE);
        g.setFont(titleFont);

        double titleW = textWidth(title, titleFont);
        double titleY = H * 0.42;
        g.fillText(title, (W - titleW) / 2.0, titleY);

        // Subtitle
        Font subFont = Font.font("Consolas", 20);
        g.setFont(subFont);

        String[] lines = subtitle.split("\n");
        double lineH = Math.max(26, textHeight("Ag", subFont) + 6);
        double y = H * 0.50;

        for (String line : lines) {
            double w = textWidth(line, subFont);
            g.fillText(line, (W - w) / 2.0, y);
            y += lineH;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // Simple rectangular car
    private static class Car {
        double x, y, w, h;
        Color color;

        Car(double x, double y, double w, double h, Color color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
        }

        void draw(GraphicsContext g) {
            // body
            g.setFill(color);
            g.fillRoundRect(x, y, w, h, 16, 16);

            // windshield
            g.setFill(Color.rgb(255, 255, 255, 0.22));
            g.fillRoundRect(x + w * 0.18, y + h * 0.12, w * 0.64, h * 0.24, 10, 10);

            // tires
            g.setFill(Color.rgb(10, 10, 10, 0.9));
            g.fillRoundRect(x - 6, y + 10, 10, h - 20, 6, 6);
            g.fillRoundRect(x + w - 4, y + 10, 10, h - 20, 6, 6);

            // highlight stripe
            g.setFill(Color.rgb(255, 255, 255, 0.18));
            g.fillRoundRect(x + w * 0.46, y + 6, w * 0.08, h - 12, 8, 8);
        }

        boolean intersects(Car other) {
            return x < other.x + other.w &&
                    x + w > other.x &&
                    y < other.y + other.h &&
                    y + h > other.y;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}