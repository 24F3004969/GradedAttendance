package org.graded_classes.graded_attendance.components;

import atlantafx.base.theme.PrimerLight;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class LudoGame extends Application {

    @Override
    public void start(Stage stage) {
        BoardView board = new BoardView();
        BorderPane root = new BorderPane(board);
        root.setBottom(board.getControls());
        root.setStyle("-fx-background-color: #f5f2eb;");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Scene scene = new Scene(root, 700, 750);
        stage.setTitle("Ludo - JavaFX");
        stage.setScene(scene);
        stage.setMinWidth(650);
        stage.setMinHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    static final class BoardView extends StackPane {
        private static final int N = 15;
        private static final double LOGICAL_MARGIN = 26;
        private static final double LOGICAL_CELL = 38;
        private static final double LOGICAL_BOARD = N * LOGICAL_CELL;
        private static final double LOGICAL_SIZE = LOGICAL_BOARD + 2 * LOGICAL_MARGIN;

        private static final int[][] PATH = {
                {6,1},{6,2},{6,3},{6,4},{6,5},
                {5,6},{4,6},{3,6},{2,6},{1,6},{0,6},
                {0,7},
                {0,8},{1,8},{2,8},{3,8},{4,8},{5,8},
                {6,9},{6,10},{6,11},{6,12},{6,13},{6,14},
                {7,14},
                {8,14},{8,13},{8,12},{8,11},{8,10},{8,9},
                {9,8},{10,8},{11,8},{12,8},{13,8},{14,8},
                {14,7},
                {14,6},{13,6},{12,6},{11,6},{10,6},{9,6},
                {8,5},{8,4},{8,3},{8,2},{8,1},{8,0},
                {7,0},
                {6,0}
        };

        private static final int[][] HOME_COL = {
                {7,1},{7,2},{7,3},{7,4},{7,5},
                {1,7},{2,7},{3,7},{4,7},{5,7},
                {7,13},{7,12},{7,11},{7,10},{7,9},
                {13,7},{12,7},{11,7},{10,7},{9,7}
        };

        private static final int[] START_IDX = {0, 13, 26, 39};
        private static final Color[] COLORS = {
                Color.rgb(215, 60, 60),
                Color.rgb(60, 175, 90),
                Color.rgb(240, 190, 50),
                Color.rgb(70, 120, 220)
        };
        private static final String[] NAMES = {"Red", "Green", "Yellow", "Blue"};
        private static final Set<Integer> SAFE = new HashSet<>(
                Arrays.asList(0, 8, 13, 21, 26, 34, 39, 47));
        private static final int[][] CORNER = {{0,0},{0,9},{9,9},{9,0}};
        private static final int[][][] YARD = {
                {{2,2},{2,3},{3,2},{3,3}},
                {{2,11},{2,12},{3,11},{3,12}},
                {{11,11},{11,12},{12,11},{12,12}},
                {{11,2},{11,3},{12,2},{12,3}}
        };

        private final Canvas canvas = new Canvas();
        private final int[] tokens = new int[16];
        private final boolean[] isAI = {false, true, true, true};
        private final Random random = new Random();
        private final Button rollButton = new Button("Roll Dice");
        private final Label statusLabel = new Label();
        private final DiceView diceView = new DiceView();
        private final HBox controls;

        private int current;
        private int dice;
        private int sixCount;
        private int gameEpoch;
        private boolean rolled;
        private boolean gameOver;
        private double scale = 1;
        private double offsetX;
        private double offsetY;

        BoardView() {
            setMinSize(0, 0);
            setPrefSize(LOGICAL_SIZE, LOGICAL_SIZE);
            setStyle("-fx-background-color: #f5f2eb;");
            getChildren().add(canvas);

            Button newButton = new Button("New Game");
            String buttonStyle = "-fx-font-size: 15px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 8; -fx-cursor: hand;";
            rollButton.setStyle(buttonStyle);
            newButton.setStyle(buttonStyle);
            rollButton.setOnAction(e -> {
                if (!isAI[current] && !gameOver) rollDice();
            });
            newButton.setOnAction(e -> newGame());

            HBox left = new HBox(8, rollButton, newButton);
            left.setAlignment(Pos.CENTER_LEFT);
            statusLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));
            statusLabel.setAlignment(Pos.CENTER);
            statusLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(statusLabel, Priority.ALWAYS);

            diceView.setMinSize(52, 52);
            diceView.setPrefSize(52, 52);
            diceView.setMaxSize(52, 52);

            controls = new HBox(12, left, statusLabel, diceView);
            controls.setAlignment(Pos.CENTER);
            controls.setPadding(new Insets(8, 12, 10, 12));
            controls.setStyle("-fx-background-color: #f0ece2;");

            canvas.setOnMousePressed(e -> handleClick(e.getX(), e.getY()));
            widthProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
            heightProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
            newGame();
        }

        HBox getControls() {
            return controls;
        }

        private void resizeCanvas() {
            double size = Math.max(1, Math.min(getWidth(), getHeight()));
            canvas.setWidth(size);
            canvas.setHeight(size);
            scale = size / LOGICAL_SIZE;
            offsetX = (canvas.getWidth() - LOGICAL_SIZE * scale) / 2.0;
            offsetY = (canvas.getHeight() - LOGICAL_SIZE * scale) / 2.0;
            draw();
        }

        private void newGame() {
            gameEpoch++;
            Arrays.fill(tokens, -1);
            current = 0;
            dice = 0;
            sixCount = 0;
            rolled = false;
            gameOver = false;
            diceView.setValue(0);
            setStatus("Red's turn. Press Roll.");
            rollButton.setDisable(isAI[current]);
            draw();
        }

        private void setStatus(String text) {
            statusLabel.setText(text);
        }

        private void later(int milliseconds, Runnable action) {
            int scheduledEpoch = gameEpoch;
            PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
            pause.setOnFinished(e -> {
                if (scheduledEpoch == gameEpoch) action.run();
            });
            pause.play();
        }

        private void rollDice() {
            if (gameOver || rolled) return;
            dice = 1 + random.nextInt(6);
            rolled = true;
            diceView.setValue(dice);
            draw();

            List<Move> moves = validMoves(current, dice);
            if (moves.isEmpty()) {
                setStatus(NAMES[current] + " rolled " + dice + " - no valid move.");
                later(900, this::endTurn);
                return;
            }

            if (isAI[current]) {
                int player = current;
                int rolledValue = dice;
                setStatus(NAMES[current] + " rolled " + dice + " ...");
                later(650, () -> {
                    if (gameOver || current != player || dice != rolledValue) return;
                    List<Move> currentMoves = validMoves(current, dice);
                    if (currentMoves.isEmpty()) endTurn();
                    else applyMove(aiChoose(currentMoves));
                });
            } else {
                setStatus(NAMES[current] + " rolled " + dice + ". Click a highlighted token.");
                draw();
            }
        }

        private List<Move> validMoves(int player, int value) {
            List<Move> result = new ArrayList<>();
            for (int token = 0; token < 4; token++) {
                int steps = tokens[player * 4 + token];
                if (steps == -1) {
                    if (value == 6) result.add(new Move(token, 0));
                } else if (steps < 56 && steps + value <= 56) {
                    result.add(new Move(token, steps + value));
                }
            }
            return result;
        }

        private Move aiChoose(List<Move> moves) {
            Move best = moves.get(0);
            int bestScore = Integer.MIN_VALUE;
            for (Move move : moves) {
                int score = 0;
                if (move.target <= 50) {
                    int pathIndex = (START_IDX[current] + move.target) % 52;
                    if (!SAFE.contains(pathIndex)) {
                        for (int opponent = 0; opponent < 4; opponent++) {
                            if (opponent == current) continue;
                            for (int token = 0; token < 4; token++) {
                                int steps = tokens[opponent * 4 + token];
                                if (steps >= 0 && steps <= 50 &&
                                        (START_IDX[opponent] + steps) % 52 == pathIndex) {
                                    score += 200;
                                }
                            }
                        }
                    }
                }
                if (move.target == 56) score += 150;
                else if (move.target >= 51) score += 60;
                if (tokens[current * 4 + move.token] == -1) score += 50;
                score += move.target;
                if (score > bestScore) {
                    bestScore = score;
                    best = move;
                }
            }
            return best;
        }

        private void applyMove(Move move) {
            int player = current;
            tokens[player * 4 + move.token] = move.target;

            if (move.target <= 50) {
                int pathIndex = (START_IDX[player] + move.target) % 52;
                if (!SAFE.contains(pathIndex)) {
                    for (int opponent = 0; opponent < 4; opponent++) {
                        if (opponent == player) continue;
                        for (int token = 0; token < 4; token++) {
                            int steps = tokens[opponent * 4 + token];
                            if (steps >= 0 && steps <= 50 &&
                                    (START_IDX[opponent] + steps) % 52 == pathIndex) {
                                tokens[opponent * 4 + token] = -1;
                            }
                        }
                    }
                }
            }
            draw();

            if (allHome(player)) {
                gameOver = true;
                setStatus(NAMES[player] + " wins! Congratulations!");
                rollButton.setDisable(true);
                draw();
                return;
            }

            int rolledDice = dice;
            later(380, () -> {
                if (gameOver) return;
                if (rolledDice == 6) {
                    sixCount++;
                    if (sixCount >= 3) {
                        setStatus(NAMES[player] + " rolled three 6s - turn forfeited.");
                        later(700, this::endTurn);
                    } else {
                        rolled = false;
                        dice = 0;
                        diceView.setValue(0);
                        setStatus(NAMES[player] + " gets another turn (rolled 6).");
                        draw();
                        if (isAI[player]) later(650, this::rollDice);
                        else rollButton.setDisable(false);
                    }
                } else {
                    endTurn();
                }
            });
        }

        private boolean allHome(int player) {
            for (int token = 0; token < 4; token++) {
                if (tokens[player * 4 + token] != 56) return false;
            }
            return true;
        }

        private void endTurn() {
            if (gameOver) return;
            sixCount = 0;
            rolled = false;
            dice = 0;
            diceView.setValue(0);

            int guard = 0;
            do {
                current = (current + 1) % 4;
                guard++;
            } while (allHome(current) && guard < 8);

            setStatus(NAMES[current] + "'s turn. Press Roll.");
            rollButton.setDisable(isAI[current]);
            draw();
            if (isAI[current]) later(700, this::rollDice);
        }

        private void handleClick(double mouseX, double mouseY) {
            if (gameOver || !rolled || isAI[current]) return;
            List<Move> moves = validMoves(current, dice);
            if (moves.isEmpty()) return;

            double logicalX = (mouseX - offsetX) / scale;
            double logicalY = (mouseY - offsetY) / scale;
            List<TokenPosition> positions = tokenPositions();
            int chosen = -1;
            double bestDistance = LOGICAL_CELL * 0.65;

            for (Move move : moves) {
                for (TokenPosition position : positions) {
                    if (position.player == current && position.token == move.token) {
                        double distance = Math.hypot(position.x - logicalX, position.y - logicalY);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            chosen = move.token;
                        }
                    }
                }
            }
            if (chosen >= 0) {
                for (Move move : moves) {
                    if (move.token == chosen) {
                        applyMove(move);
                        return;
                    }
                }
            }
        }

        private List<TokenPosition> tokenPositions() {
            List<TokenPosition> result = new ArrayList<>();
            for (int player = 0; player < 4; player++) {
                for (int token = 0; token < 4; token++) {
                    int steps = tokens[player * 4 + token];
                    double x;
                    double y;
                    if (steps == -1) {
                        int row = YARD[player][token][0];
                        int col = YARD[player][token][1];
                        x = px(col) + LOGICAL_CELL / 2;
                        y = py(row) + LOGICAL_CELL / 2;
                    } else if (steps <= 50) {
                        int index = (START_IDX[player] + steps) % 52;
                        x = px(PATH[index][1]) + LOGICAL_CELL / 2;
                        y = py(PATH[index][0]) + LOGICAL_CELL / 2;
                    } else if (steps <= 55) {
                        int[] cell = HOME_COL[player * 5 + steps - 51];
                        x = px(cell[1]) + LOGICAL_CELL / 2;
                        y = py(cell[0]) + LOGICAL_CELL / 2;
                    } else {
                        double center = LOGICAL_MARGIN + 7.5 * LOGICAL_CELL;
                        double offset = LOGICAL_CELL * 0.55;
                        if (player == 0) { x = center - offset; y = center; }
                        else if (player == 1) { x = center; y = center - offset; }
                        else if (player == 2) { x = center + offset; y = center; }
                        else { x = center; y = center + offset; }
                    }
                    result.add(new TokenPosition(x, y, player, token));
                }
            }

            Map<String, List<TokenPosition>> groups = new HashMap<>();
            for (TokenPosition position : result) {
                String key = ((int) (position.x / 6)) + ":" + ((int) (position.y / 6));
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(position);
            }
            for (List<TokenPosition> group : groups.values()) {
                int count = group.size();
                if (count <= 1) continue;
                for (int i = 0; i < count; i++) {
                    TokenPosition position = group.get(i);
                    double angle = 2 * Math.PI * i / count - Math.PI / 2;
                    double offset = LOGICAL_CELL * 0.20;
                    position.x += Math.cos(angle) * offset;
                    position.y += Math.sin(angle) * offset;
                }
            }
            return result;
        }

        private double px(int col) {
            return LOGICAL_MARGIN + col * LOGICAL_CELL;
        }

        private double py(int row) {
            return LOGICAL_MARGIN + row * LOGICAL_CELL;
        }

        private void draw() {
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setTransform(1, 0, 0, 1, 0, 0);
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setTransform(scale, 0, 0, scale, offsetX, offsetY);
            drawBoard(g);
            drawTokens(g);
            g.setTransform(1, 0, 0, 1, 0, 0);
        }

        private void drawBoard(GraphicsContext g) {
            g.setFill(Color.WHITE);
            g.fillRect(LOGICAL_MARGIN, LOGICAL_MARGIN, LOGICAL_BOARD, LOGICAL_BOARD);

            for (int player = 0; player < 4; player++) {
                int row = CORNER[player][0];
                int col = CORNER[player][1];
                g.setFill(COLORS[player]);
                g.fillRect(px(col), py(row), 6 * LOGICAL_CELL, 6 * LOGICAL_CELL);
                g.setFill(Color.WHITE);
                g.fillRect(px(col + 1), py(row + 1), 4 * LOGICAL_CELL, 4 * LOGICAL_CELL);
                g.setStroke(COLORS[player].darker());
                g.setLineWidth(2);
                g.strokeRect(px(col), py(row), 6 * LOGICAL_CELL, 6 * LOGICAL_CELL);
            }

            for (int player = 0; player < 4; player++) {
                for (int token = 0; token < 4; token++) {
                    int row = YARD[player][token][0];
                    int col = YARD[player][token][1];
                    double centerX = px(col) + LOGICAL_CELL / 2;
                    double centerY = py(row) + LOGICAL_CELL / 2;
                    double radius = LOGICAL_CELL * 0.38;
                    g.setFill(Color.rgb(255, 255, 255, 0.86));
                    g.fillOval(centerX - radius, centerY - radius, 2 * radius, 2 * radius);
                }
            }

            g.setLineWidth(1);
            for (int[] cell : PATH) {
                g.setFill(Color.WHITE);
                g.fillRect(px(cell[1]), py(cell[0]), LOGICAL_CELL, LOGICAL_CELL);
                g.setStroke(Color.rgb(160, 160, 160));
                g.strokeRect(px(cell[1]), py(cell[0]), LOGICAL_CELL, LOGICAL_CELL);
            }

            for (int player = 0; player < 4; player++) {
                int[] cell = PATH[START_IDX[player]];
                g.setFill(COLORS[player]);
                g.fillRect(px(cell[1]) + 1, py(cell[0]) + 1, LOGICAL_CELL - 1, LOGICAL_CELL - 1);
                g.setStroke(Color.rgb(80, 80, 80));
                g.strokeRect(px(cell[1]), py(cell[0]), LOGICAL_CELL, LOGICAL_CELL);
            }

            for (int player = 0; player < 4; player++) {
                for (int i = 0; i < 5; i++) {
                    int[] cell = HOME_COL[player * 5 + i];
                    g.setFill(COLORS[player]);
                    g.fillRect(px(cell[1]) + 1, py(cell[0]) + 1, LOGICAL_CELL - 1, LOGICAL_CELL - 1);
                    g.setStroke(Color.rgb(255, 255, 255, 0.35));
                    g.strokeRect(px(cell[1]), py(cell[0]), LOGICAL_CELL, LOGICAL_CELL);
                }
            }

            double centerX = LOGICAL_MARGIN + 7.5 * LOGICAL_CELL;
            double centerY = LOGICAL_MARGIN + 7.5 * LOGICAL_CELL;
            double x0 = px(6), y0 = py(6), x1 = px(9), y1 = py(9);
            fillTriangle(g, COLORS[0], x0, y0, x0, y1, centerX, centerY);
            fillTriangle(g, COLORS[1], x0, y0, x1, y0, centerX, centerY);
            fillTriangle(g, COLORS[2], x1, y0, x1, y1, centerX, centerY);
            fillTriangle(g, COLORS[3], x0, y1, x1, y1, centerX, centerY);
            g.setStroke(Color.BLACK);
            g.setLineWidth(2);
            g.strokeRect(x0, y0, 3 * LOGICAL_CELL, 3 * LOGICAL_CELL);

            for (int index : SAFE) {
                int[] cell = PATH[index];
                drawStar(g, px(cell[1]) + LOGICAL_CELL / 2,
                        py(cell[0]) + LOGICAL_CELL / 2, LOGICAL_CELL * 0.32);
            }

            g.setStroke(Color.rgb(60, 40, 20));
            g.setLineWidth(3);
            g.strokeRect(LOGICAL_MARGIN, LOGICAL_MARGIN, LOGICAL_BOARD, LOGICAL_BOARD);
            g.setLineWidth(1);
        }

        private void fillTriangle(GraphicsContext g, Color color,
                                  double x1, double y1, double x2, double y2,
                                  double x3, double y3) {
            g.setFill(color);
            g.fillPolygon(new double[]{x1, x2, x3}, new double[]{y1, y2, y3}, 3);
        }

        private void drawStar(GraphicsContext g, double centerX, double centerY, double radius) {
            double[] xs = new double[10];
            double[] ys = new double[10];
            for (int i = 0; i < 10; i++) {
                double r = i % 2 == 0 ? radius : radius * 0.45;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                xs[i] = centerX + r * Math.cos(angle);
                ys[i] = centerY + r * Math.sin(angle);
            }
            g.setFill(Color.rgb(120, 90, 30, 0.67));
            g.fillPolygon(xs, ys, 10);
        }

        private void drawTokens(GraphicsContext g) {
            Set<Integer> highlighted = new HashSet<>();
            if (!gameOver && rolled && !isAI[current]) {
                for (Move move : validMoves(current, dice)) {
                    highlighted.add(current * 4 + move.token);
                }
            }

            for (TokenPosition position : tokenPositions()) {
                int key = position.player * 4 + position.token;
                double radius = LOGICAL_CELL * 0.36;
                if (highlighted.contains(key)) {
                    g.setFill(Color.rgb(255, 240, 0, 0.72));
                    g.fillOval(position.x - radius - 5, position.y - radius - 5,
                            2 * radius + 10, 2 * radius + 10);
                }
                g.setFill(Color.rgb(0, 0, 0, 0.24));
                g.fillOval(position.x - radius + 2, position.y - radius + 3,
                        2 * radius, 2 * radius);
                g.setFill(COLORS[position.player].darker());
                g.fillOval(position.x - radius, position.y - radius, 2 * radius, 2 * radius);
                g.setFill(COLORS[position.player]);
                g.fillOval(position.x - radius + 2, position.y - radius + 2,
                        2 * radius - 4, 2 * radius - 4);
                g.setFill(Color.rgb(255, 255, 255, 0.78));
                g.fillOval(position.x - radius * 0.45, position.y - radius * 0.45,
                        radius * 0.9, radius * 0.9);
            }
        }

        private record Move(int token, int target) {}

        private static final class TokenPosition {
            double x;
            double y;
            final int player;
            final int token;

            TokenPosition(double x, double y, int player, int token) {
                this.x = x;
                this.y = y;
                this.player = player;
                this.token = token;
            }
        }
    }

    static final class DiceView extends Region {
        private int value;

        void setValue(int value) {
            this.value = value;
            requestLayout();
        }

        @Override
        protected void layoutChildren() {
            draw();
        }

        private void draw() {
            getChildren().clear();
            double width = getWidth();
            double height = getHeight();
            double size = Math.min(width, height) - 4;
            if (size <= 0) return;

            Canvas canvas = new Canvas(width, height);
            GraphicsContext g = canvas.getGraphicsContext2D();
            double x = (width - size) / 2;
            double y = (height - size) / 2;
            g.setFill(Color.WHITE);
            g.fillRoundRect(x, y, size, size, 12, 12);
            g.setStroke(Color.rgb(60, 60, 60));
            g.setLineWidth(2);
            g.strokeRoundRect(x, y, size, size, 12, 12);

            if (value > 0) {
                double radius = Math.max(3, size / 11);
                double centerX = x + size / 2;
                double centerY = y + size / 2;
                double distance = size / 4;
                List<double[]> pips = switch (value) {
                    case 1 -> List.of(new double[]{centerX, centerY});
                    case 2 -> List.of(new double[]{centerX-distance, centerY-distance},
                            new double[]{centerX+distance, centerY+distance});
                    case 3 -> List.of(new double[]{centerX-distance, centerY-distance},
                            new double[]{centerX, centerY},
                            new double[]{centerX+distance, centerY+distance});
                    case 4 -> List.of(new double[]{centerX-distance, centerY-distance},
                            new double[]{centerX+distance, centerY-distance},
                            new double[]{centerX-distance, centerY+distance},
                            new double[]{centerX+distance, centerY+distance});
                    case 5 -> List.of(new double[]{centerX-distance, centerY-distance},
                            new double[]{centerX+distance, centerY-distance},
                            new double[]{centerX, centerY},
                            new double[]{centerX-distance, centerY+distance},
                            new double[]{centerX+distance, centerY+distance});
                    default -> List.of(new double[]{centerX-distance, centerY-distance},
                            new double[]{centerX+distance, centerY-distance},
                            new double[]{centerX-distance, centerY},
                            new double[]{centerX+distance, centerY},
                            new double[]{centerX-distance, centerY+distance},
                            new double[]{centerX+distance, centerY+distance});
                };
                g.setFill(Color.rgb(200, 40, 40));
                for (double[] pip : pips) {
                    g.fillOval(pip[0] - radius, pip[1] - radius, 2 * radius, 2 * radius);
                }
            }
            getChildren().add(canvas);
        }
    }
}
