package org.graded_classes.graded_attendance.test;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

public class LudoApp extends Application {

    // ---------- Config ----------
    private static final int GRID = 13;          // logical grid size
    private static final int CELL = 48;          // pixel size of each cell
    private static final int MARGIN = 20;        // canvas margin
    private static final int HOME_LEN = 6;       // home lane length
    private static final int TOKENS_PER_PLAYER = 4;

    // ---------- Model ----------
    enum PColor {
        RED(Color.web("#E53935")),
        GREEN(Color.web("#43A047")),
        YELLOW(Color.web("#FDD835")),
        BLUE(Color.web("#1E88E5"));

        final Color fx;
        PColor(Color fx) { this.fx = fx; }
    }

    static class Token {
        final int id;           // 0..3
        int progress = -1;      // -1 = base, 0..pathLen-1 along player's path
        Token(int id) { this.id = id; }

        boolean inBase() { return progress < 0; }
        boolean finished(int pathLen) { return progress == pathLen - 1; }
    }

    static class Player {
        final PColor color;
        final List<Token> tokens = new ArrayList<>();

        // ✅ Rule tracking: first time this player rolls a 6 -> move 12 instead of 6
        boolean firstSixBonusUsed = false;

        Player(PColor color) {
            this.color = color;
            for (int i=0;i<TOKENS_PER_PLAYER;i++) tokens.add(new Token(i));
        }
        boolean allFinished(int pathLen) {
            return tokens.stream().allMatch(t -> t.finished(pathLen));
        }
    }

    static class Pos {
        final int r, c;
        Pos(int r, int c) { this.r = r; this.c = c; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pos)) return false;
            Pos pos = (Pos) o;
            return r == pos.r && c == pos.c;
        }
        @Override public int hashCode() { return Objects.hash(r, c); }
    }

    // ---------- Board Layout ----------
    static class Layout {
        final List<Pos> track;                     // shared loop
        final EnumMap<PColor, Integer> startIndex; // starting index on shared track
        final EnumMap<PColor, List<Pos>> homeLane; // private lane (length HOME_LEN)
        final EnumMap<PColor, List<Pos>> baseSpots;// where tokens sit in base
        final Set<Integer> safeIndices;            // safe squares on track

        Layout(List<Pos> track,
               EnumMap<PColor,Integer> startIndex,
               EnumMap<PColor,List<Pos>> homeLane,
               EnumMap<PColor,List<Pos>> baseSpots,
               Set<Integer> safeIndices) {
            this.track = track;
            this.startIndex = startIndex;
            this.homeLane = homeLane;
            this.baseSpots = baseSpots;
            this.safeIndices = safeIndices;
        }
    }

    // ---------- Game State ----------
    private final Random rng = new Random();
    private Layout layout;
    private List<Player> players;

    private int currentPlayerIdx = 0;

    // dice = move steps (could be 12 when first 6 bonus happens)
    private Integer dice = null;

    // ✅ Raw dice result (always 1..6) and flag for "rolled 6"
    private int rawDice = -1;
    private boolean rolledSixThisTurn = false;

    private boolean mustRoll = true;
    private boolean gameOver = false;

    // ---------- UI ----------
    private Canvas canvas;
    private Label lblTurn;
    private Label lblDice;
    private Label lblInfo;
    private Button btnRoll;
    private Button btnNew;
    private ComboBox<Integer> cmbPlayers;

    // cached paths
    private EnumMap<PColor, List<Pos>> playerPath; // track rotated + home lane
    private int pathLen;                            // same length for all

    @Override
    public void start(Stage stage) {
        layout = buildLayout();

        canvas = new Canvas(MARGIN*2 + GRID*CELL, MARGIN*2 + GRID*CELL);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onCanvasClick);

        lblTurn = new Label();
        lblTurn.setFont(Font.font(16));
        lblDice = new Label("Dice: -");
        lblDice.setFont(Font.font(16));
        lblInfo = new Label("Roll to start.");
        lblInfo.setWrapText(true);

        btnRoll = new Button("Roll");
        btnRoll.setMaxWidth(Double.MAX_VALUE);
        btnRoll.setOnAction(e -> rollDice());

        btnNew = new Button("New Game");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> newGame());

        cmbPlayers = new ComboBox<>();
        cmbPlayers.getItems().addAll(2,3,4);
        cmbPlayers.setValue(4);

        VBox right = new VBox(10,
                new Label("Players:"), cmbPlayers,
                new Separator(),
                lblTurn, lblDice,
                btnRoll,
                new Separator(),
                lblInfo,
                new Separator(),
                btnNew
        );
        right.setPadding(new Insets(12));
        right.setPrefWidth(220);

        BorderPane root = new BorderPane(canvas);
        root.setRight(right);

        newGame();
        draw();

        Scene scene = new Scene(root);
        stage.setTitle("Ludo (JavaFX) - First 6 = 12 Moves");
        stage.setScene(scene);
        stage.show();
    }

    // ---------- New Game ----------
    private void newGame() {
        int n = cmbPlayers.getValue();
        players = new ArrayList<>();
        PColor[] all = PColor.values();
        for (int i=0;i<n;i++) players.add(new Player(all[i]));

        buildPlayerPaths();
        currentPlayerIdx = 0;

        dice = null;
        rawDice = -1;
        rolledSixThisTurn = false;

        mustRoll = true;
        gameOver = false;

        lblInfo.setText("Roll to start. Need 6 to leave base. First 6 = 12 moves!");
        updateLabels();
        draw();
    }

    private void buildPlayerPaths() {
        playerPath = new EnumMap<>(PColor.class);
        int trackLen = layout.track.size();
        for (PColor c : PColor.values()) {
            int start = layout.startIndex.get(c);
            List<Pos> rotated = new ArrayList<>(trackLen);
            for (int i=0;i<trackLen;i++) rotated.add(layout.track.get((start + i) % trackLen));
            List<Pos> path = new ArrayList<>(trackLen + HOME_LEN);
            path.addAll(rotated);
            path.addAll(layout.homeLane.get(c));
            playerPath.put(c, path);
        }
        pathLen = layout.track.size() + HOME_LEN;
    }

    // ---------- Core Mechanics ----------
    private void rollDice() {
        if (gameOver) return;
        if (!mustRoll) {
            lblInfo.setText("You already rolled. Click a token to move.");
            return;
        }

        Player p = players.get(currentPlayerIdx);

        rawDice = rng.nextInt(6) + 1;            // 1..6
        rolledSixThisTurn = (rawDice == 6);

        // ✅ Move steps: first time rolling 6 => 12 moves
        int moveSteps = rawDice;
        if (rolledSixThisTurn && !p.firstSixBonusUsed) {
            moveSteps = 12;
            p.firstSixBonusUsed = true;
        }

        dice = moveSteps;         // dice now means "move steps"
        mustRoll = false;

        if (moveSteps != rawDice) {
            lblDice.setText("Dice: " + rawDice + " (bonus → " + moveSteps + ")");
            lblInfo.setText("First 6 bonus! Move 12 steps. Click a token.");
        } else {
            lblDice.setText("Dice: " + rawDice);
            lblInfo.setText("Click one of your movable tokens.");
        }

        List<Token> movable = movableTokens(p, dice);
        if (movable.isEmpty()) {
            lblInfo.setText("No moves possible. Turn passes.");
            endTurn(false);
        }

        updateLabels();
        draw();
    }

    private List<Token> movableTokens(Player p, int diceVal) {
        List<Pos> path = playerPath.get(p.color);
        return p.tokens.stream().filter(t -> canMoveToken(p, t, diceVal, path)).collect(Collectors.toList());
    }

    private boolean canMoveToken(Player p, Token t, int diceVal, List<Pos> path) {
        if (t.finished(pathLen)) return false;

        // ✅ leaving base depends on RAW dice being 6, not move steps
        if (t.inBase()) {
            if (!rolledSixThisTurn) return false;
            // entering at progress 0 then move remaining (diceVal-1) steps
            return (diceVal - 1) < path.size();
        }

        int newProg = t.progress + diceVal;
        return newProg < path.size(); // must not overshoot finish
    }

    private void endTurn(boolean extraTurn) {
        dice = null;
        rawDice = -1;
        rolledSixThisTurn = false;

        lblDice.setText("Dice: -");
        mustRoll = true;

        if (!extraTurn && !gameOver) {
            currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
        }
        updateLabels();
        draw();
    }

    // ---------- Input ----------
    private void onCanvasClick(MouseEvent e) {
        if (gameOver) return;
        if (mustRoll || dice == null) {
            lblInfo.setText("Roll first!");
            return;
        }

        Player p = players.get(currentPlayerIdx);
        int diceVal = dice;
        List<Token> movable = movableTokens(p, diceVal);
        if (movable.isEmpty()) return;

        Token clicked = findClickedToken(p, e.getX(), e.getY());
        if (clicked == null) {
            lblInfo.setText("Click one of your tokens.");
            return;
        }
        if (!movable.contains(clicked)) {
            lblInfo.setText("That token can't move with move=" + diceVal + " (raw=" + rawDice + ")");
            return;
        }

        animateMove(p, clicked, diceVal);
    }

    private Token findClickedToken(Player p, double x, double y) {
        double radius = CELL * 0.35;
        Token best = null;
        double bestDist = Double.MAX_VALUE;

        for (Token t : p.tokens) {
            Point2D center = tokenCenter(p.color, t);
            double d = center.distance(x, y);
            if (d < radius && d < bestDist) {
                best = t;
                bestDist = d;
            }
        }
        return best;
    }

    private void animateMove(Player p, Token t, int diceVal) {
        btnRoll.setDisable(true);

        List<Pos> path = playerPath.get(p.color);
        int trackLen = layout.track.size();

        // overshoot checks
        if (t.inBase()) {
            // final progress will be (diceVal - 1)
            if ((diceVal - 1) >= path.size()) {
                btnRoll.setDisable(false);
                lblInfo.setText("Move too large to enter (rare). Turn passes.");
                endTurn(false);
                return;
            }
        } else {
            if (t.progress + diceVal >= path.size()) {
                btnRoll.setDisable(false);
                lblInfo.setText("Exact roll needed to finish.");
                endTurn(false);
                return;
            }
        }

        // build animation steps
        List<Integer> steps = new ArrayList<>();
        if (t.inBase()) {
            // entering counts as 1 step -> progress 0
            steps.add(0);
            // remaining moves -> 1..diceVal-1
            for (int i = 1; i < diceVal; i++) steps.add(i);
        } else {
            int start = t.progress;
            for (int i = 1; i <= diceVal; i++) steps.add(start + i);
        }

        Timeline tl = new Timeline();
        for (int i=0;i<steps.size();i++) {
            int prog = steps.get(i);
            KeyFrame kf = new KeyFrame(Duration.millis(120L * (i + 1)), ev -> {
                t.progress = Math.min(prog, path.size()-1);
                draw();
            });
            tl.getKeyFrames().add(kf);
        }

        tl.setOnFinished(ev -> {
            applyPostMoveEffects(p, t, trackLen);

            // ✅ extra turn depends on RAW roll being 6
            boolean extra = rolledSixThisTurn && !gameOver;

            if (!gameOver) {
                lblInfo.setText(extra ? "Rolled 6 → Extra turn! Roll again." : "Turn over. Next player.");
            }

            btnRoll.setDisable(false);
            endTurn(extra);
        });

        tl.play();
    }

    private void applyPostMoveEffects(Player p, Token t, int trackLen) {
        // capture logic only if on shared track and not safe
        if (t.progress >= 0 && t.progress < trackLen) {
            int gIndex = globalTrackIndex(p.color, t.progress);
            if (!layout.safeIndices.contains(gIndex)) {
                Pos landed = layout.track.get(gIndex);
                boolean capturedAny = false;

                for (Player op : players) {
                    if (op == p) continue;
                    for (Token ot : op.tokens) {
                        if (ot.inBase() || ot.finished(pathLen)) continue;
                        if (ot.progress < trackLen) {
                            int og = globalTrackIndex(op.color, ot.progress);
                            if (layout.track.get(og).equals(landed)) {
                                ot.progress = -1;
                                capturedAny = true;
                            }
                        }
                    }
                }
                if (capturedAny) lblInfo.setText("Captured opponent token!");
            }
        }

        // win check
        if (p.allFinished(pathLen)) {
            gameOver = true;
            lblInfo.setText("🎉 " + p.color + " wins! (All tokens finished)");
        }
    }

    private int globalTrackIndex(PColor color, int progressOnTrack) {
        int start = layout.startIndex.get(color);
        return (start + progressOnTrack) % layout.track.size();
    }

    // ---------- Drawing ----------
    private void updateLabels() {
        Player p = players.get(currentPlayerIdx);
        lblTurn.setText("Turn: " + p.color);
        lblTurn.setTextFill(p.color.fx);
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#FAFAFA"));
        g.fillRect(0,0,canvas.getWidth(), canvas.getHeight());

        // draw grid faint
        g.setStroke(Color.web("#E0E0E0"));
        for (int r=0;r<=GRID;r++) {
            double y = MARGIN + r*CELL;
            g.strokeLine(MARGIN, y, MARGIN + GRID*CELL, y);
        }
        for (int c=0;c<=GRID;c++) {
            double x = MARGIN + c*CELL;
            g.strokeLine(x, MARGIN, x, MARGIN + GRID*CELL);
        }

        drawTrack(g);
        drawHomeLanes(g);
        drawBases(g);
        drawTokens(g);
        drawCenter(g);
    }

    private void drawTrack(GraphicsContext g) {
        for (int i=0;i<layout.track.size();i++) {
            Pos p = layout.track.get(i);
            drawCell(g, p, layout.safeIndices.contains(i) ? Color.web("#FFF3E0") : Color.WHITE, Color.web("#9E9E9E"));
        }

        for (PColor c : PColor.values()) {
            Integer si = layout.startIndex.get(c);
            if (si == null) continue;
            Pos p = layout.track.get(si);
            drawCell(g, p, c.fx.deriveColor(0,1,1,0.25), c.fx);
        }
    }

    private void drawHomeLanes(GraphicsContext g) {
        for (PColor c : PColor.values()) {
            List<Pos> lane = layout.homeLane.get(c);
            for (int i=0;i<lane.size();i++) {
                Pos p = lane.get(i);
                drawCell(g, p, c.fx.deriveColor(0,1,1,0.20), c.fx.deriveColor(0,1,1,0.65));
            }
        }
    }

    private void drawBases(GraphicsContext g) {
        for (PColor c : PColor.values()) {
            List<Pos> spots = layout.baseSpots.get(c);
            if (spots == null) continue;
            for (Pos p : spots) {
                drawCell(g, p, c.fx.deriveColor(0,1,1,0.12), c.fx.deriveColor(0,1,1,0.55));
            }
        }
    }

    private void drawTokens(GraphicsContext g) {
        for (Player pl : players) {
            for (Token t : pl.tokens) {
                Point2D center = tokenCenter(pl.color, t);
                double r = CELL * 0.18;

                g.setFill(pl.color.fx);
                g.fillOval(center.getX()-r, center.getY()-r, 2*r, 2*r);

                g.setStroke(Color.web("#212121"));
                g.strokeOval(center.getX()-r, center.getY()-r, 2*r, 2*r);

                g.setFill(Color.WHITE);
                g.setFont(Font.font(12));
                g.fillText("" + (t.id+1), center.getX()-4, center.getY()+4);
            }
        }
    }

    private void drawCenter(GraphicsContext g) {
        int mid = GRID/2;
        drawCell(g, new Pos(mid, mid), Color.web("#ECEFF1"), Color.web("#90A4AE"));
        g.setFill(Color.web("#455A64"));
        g.setFont(Font.font(12));
        double x = MARGIN + mid*CELL + CELL*0.2;
        double y = MARGIN + mid*CELL + CELL*0.55;
        g.fillText("FIN", x, y);
    }

    private void drawCell(GraphicsContext g, Pos p, Color fill, Color stroke) {
        double x = MARGIN + p.c * CELL;
        double y = MARGIN + p.r * CELL;
        g.setFill(fill);
        g.fillRect(x, y, CELL, CELL);
        g.setStroke(stroke);
        g.strokeRect(x, y, CELL, CELL);
    }

    private Point2D tokenCenter(PColor color, Token t) {
        if (t.inBase()) {
            Pos p = layout.baseSpots.get(color).get(t.id);
            return cellCenter(p);
        }

        List<Pos> path = playerPath.get(color);
        int prog = Math.min(t.progress, path.size()-1);
        Pos p = path.get(prog);
        return cellCenter(p);
    }

    private Point2D cellCenter(Pos p) {
        double x = MARGIN + p.c * CELL + CELL/2.0;
        double y = MARGIN + p.r * CELL + CELL/2.0;
        return new Point2D(x, y);
    }

    // ---------- Layout Builder (simple square-loop “Ludo-like”) ----------
    private Layout buildLayout() {
        int ringMin = 2;
        int ringMax = GRID - 3;

        List<Pos> track = new ArrayList<>();

        // top edge (left->right)
        for (int c = ringMin; c <= ringMax; c++) track.add(new Pos(ringMin, c));
        // right edge (top+1->bottom)
        for (int r = ringMin+1; r <= ringMax; r++) track.add(new Pos(r, ringMax));
        // bottom edge (right-1->left)
        for (int c = ringMax-1; c >= ringMin; c--) track.add(new Pos(ringMax, c));
        // left edge (bottom-1->top+1)
        for (int r = ringMax-1; r >= ringMin+1; r--) track.add(new Pos(r, ringMin));

        int trackLen = track.size();

        EnumMap<PColor,Integer> start = new EnumMap<>(PColor.class);
        start.put(PColor.RED, 0);
        start.put(PColor.GREEN, trackLen/4);
        start.put(PColor.YELLOW, trackLen/2);
        start.put(PColor.BLUE, 3*trackLen/4);

        Set<Integer> safe = new HashSet<>();
        safe.addAll(start.values());
        safe.add((start.get(PColor.RED) + 4) % trackLen);
        safe.add((start.get(PColor.GREEN) + 4) % trackLen);
        safe.add((start.get(PColor.YELLOW) + 4) % trackLen);
        safe.add((start.get(PColor.BLUE) + 4) % trackLen);

        int mid = GRID/2;
        EnumMap<PColor,List<Pos>> home = new EnumMap<>(PColor.class);
        home.put(PColor.RED,    makeLine(mid+1, mid, -1, 0, HOME_LEN));
        home.put(PColor.GREEN,  makeLine(mid, mid-1, 0, 1, HOME_LEN));
        home.put(PColor.YELLOW, makeLine(mid-1, mid, 1, 0, HOME_LEN));
        home.put(PColor.BLUE,   makeLine(mid, mid+1, 0, -1, HOME_LEN));

        EnumMap<PColor,List<Pos>> base = new EnumMap<>(PColor.class);
        base.put(PColor.RED, Arrays.asList(
                new Pos(GRID-2, 1), new Pos(GRID-2, 2),
                new Pos(GRID-3, 1), new Pos(GRID-3, 2)
        ));
        base.put(PColor.GREEN, Arrays.asList(
                new Pos(1, 1), new Pos(1, 2),
                new Pos(2, 1), new Pos(2, 2)
        ));
        base.put(PColor.YELLOW, Arrays.asList(
                new Pos(1, GRID-3), new Pos(1, GRID-2),
                new Pos(2, GRID-3), new Pos(2, GRID-2)
        ));
        base.put(PColor.BLUE, Arrays.asList(
                new Pos(GRID-2, GRID-3), new Pos(GRID-2, GRID-2),
                new Pos(GRID-3, GRID-3), new Pos(GRID-3, GRID-2)
        ));

        return new Layout(track, start, home, base, safe);
    }

    private List<Pos> makeLine(int sr, int sc, int dr, int dc, int len) {
        List<Pos> out = new ArrayList<>(len);
        int r = sr, c = sc;
        for (int i=0;i<len;i++) {
            out.add(new Pos(r, c));
            r += dr; c += dc;
        }
        return out;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

