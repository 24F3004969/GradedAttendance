package org.graded_classes.graded_attendance.new_features_and_ai_slop;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * - Human vs Human or Human vs Computer (choose side)
 * - Computer move via minimax + alpha-beta, adjustable depth (difficulty)
 * - Legal move generation: castling, en passant, promotion
 * - Check, checkmate, stalemate, 50-move rule, insufficient material
 * - Improved design: themes, cleaner highlights, coordinates, toolbar
 * <p>
 * Author: Helal Anwar
 * Date: 2026-02-15
 */
public class ChessFX extends Application {

    // ===== Model =====

    enum PieceType {KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN}

    enum Side {WHITE, BLACK}

    static class Piece {
        final PieceType type;
        final Side side;
        boolean moved; // convenience for castling/pawns

        Piece(PieceType type, Side side) {
            this.type = type;
            this.side = side;
        }

        Piece copy() {
            Piece p = new Piece(type, side);
            p.moved = moved;
            return p;
        }

        @Override
        public String toString() {
            return side + " " + type + (moved ? " (m)" : "");
        }
    }

    static class Move {
        final int fromX, fromY, toX, toY;
        PieceType promotion; // if pawn promotion
        boolean isCastleKingSide, isCastleQueenSide;
        boolean isEnPassant;
        boolean isDoublePawnPush;

        // For undo
        Piece capturedPiece;
        int prevEnPassantX = -1, prevEnPassantY = -1;
        boolean prevWKC, prevWQC, prevBKC, prevBQC;
        int prevHalfmoveClock;
        int prevFullmoveNumber;

        Move(int fx, int fy, int tx, int ty) {
            this.fromX = fx;
            this.fromY = fy;
            this.toX = tx;
            this.toY = ty;
        }

        @Override
        public String toString() {
            String base = "" + (char) ('a' + fromX) + (fromY + 1) + "-" + (char) ('a' + toX) + (toY + 1);
            if (promotion != null) base += "=" + promotion.name().charAt(0);
            if (isCastleKingSide) base += " (O-O)";
            if (isCastleQueenSide) base += " (O-O-O)";
            if (isEnPassant) base += " (ep)";
            return base;
        }
    }

    static class Board {
        Piece[][] sq = new Piece[8][8]; // [x][y], y=0 is white back rank (logical)
        Side sideToMove = Side.WHITE;
        boolean whiteCastleKing = true, whiteCastleQueen = true, blackCastleKing = true, blackCastleQueen = true;
        int enPassantX = -1, enPassantY = -1;
        int halfmoveClock = 0;
        int fullmoveNumber = 1;
        Deque<Move> history = new ArrayDeque<>();

        Board() {
            setupInitial();
        }

        void setupInitial() {
            for (int x = 0; x < 8; x++) Arrays.fill(sq[x], null);
            // White
            sq[0][0] = new Piece(PieceType.ROOK, Side.WHITE);
            sq[1][0] = new Piece(PieceType.KNIGHT, Side.WHITE);
            sq[2][0] = new Piece(PieceType.BISHOP, Side.WHITE);
            sq[3][0] = new Piece(PieceType.QUEEN, Side.WHITE);
            sq[4][0] = new Piece(PieceType.KING, Side.WHITE);
            sq[5][0] = new Piece(PieceType.BISHOP, Side.WHITE);
            sq[6][0] = new Piece(PieceType.KNIGHT, Side.WHITE);
            sq[7][0] = new Piece(PieceType.ROOK, Side.WHITE);
            for (int x = 0; x < 8; x++) sq[x][1] = new Piece(PieceType.PAWN, Side.WHITE);
            // Black
            sq[0][7] = new Piece(PieceType.ROOK, Side.BLACK);
            sq[1][7] = new Piece(PieceType.KNIGHT, Side.BLACK);
            sq[2][7] = new Piece(PieceType.BISHOP, Side.BLACK);
            sq[3][7] = new Piece(PieceType.QUEEN, Side.BLACK);
            sq[4][7] = new Piece(PieceType.KING, Side.BLACK);
            sq[5][7] = new Piece(PieceType.BISHOP, Side.BLACK);
            sq[6][7] = new Piece(PieceType.KNIGHT, Side.BLACK);
            sq[7][7] = new Piece(PieceType.ROOK, Side.BLACK);
            for (int x = 0; x < 8; x++) sq[x][6] = new Piece(PieceType.PAWN, Side.BLACK);

            sideToMove = Side.WHITE;
            whiteCastleKing = whiteCastleQueen = blackCastleKing = blackCastleQueen = true;
            enPassantX = enPassantY = -1;
            halfmoveClock = 0;
            fullmoveNumber = 1;
            history.clear();
        }

        Board copy() {
            Board b = new Board();
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) b.sq[x][y] = (sq[x][y] == null ? null : sq[x][y].copy());
            b.sideToMove = sideToMove;
            b.whiteCastleKing = whiteCastleKing;
            b.whiteCastleQueen = whiteCastleQueen;
            b.blackCastleKing = blackCastleKing;
            b.blackCastleQueen = blackCastleQueen;
            b.enPassantX = enPassantX;
            b.enPassantY = enPassantY;
            b.halfmoveClock = halfmoveClock;
            b.fullmoveNumber = fullmoveNumber;
            b.history.clear(); // fresh history
            return b;
        }

        boolean inBounds(int x, int y) {
            return x >= 0 && x < 8 && y >= 0 && y < 8;
        }

        Piece get(int x, int y) {
            return inBounds(x, y) ? sq[x][y] : null;
        }

        void set(int x, int y, Piece p) {
            if (inBounds(x, y)) sq[x][y] = p;
        }

        static Side opposite(Side s) {
            return (s == Side.WHITE) ? Side.BLACK : Side.WHITE;
        }

        int[] kingPos(Side side) {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    Piece p = sq[x][y];
                    if (p != null && p.type == PieceType.KING && p.side == side) return new int[]{x, y};
                }
            return null;
        }

        boolean isSquareAttacked(int x, int y, Side bySide) {
            // Knights
            int[][] kn = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
            for (int[] d : kn) {
                int nx = x + d[0], ny = y + d[1];
                if (inBounds(nx, ny)) {
                    Piece p = sq[nx][ny];
                    if (p != null && p.side == bySide && p.type == PieceType.KNIGHT) return true;
                }
            }
            // Pawns
            int dir = (bySide == Side.WHITE) ? 1 : -1;
            for (int ax : new int[]{x - 1, x + 1}) {
                int py = y - dir;
                if (inBounds(ax, py)) {
                    Piece p = sq[ax][py];
                    if (p != null && p.side == bySide && p.type == PieceType.PAWN) return true;
                }
            }
            // Kings
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (inBounds(nx, ny)) {
                        Piece p = sq[nx][ny];
                        if (p != null && p.side == bySide && p.type == PieceType.KING) return true;
                    }
                }
            // Sliding
            int[][] dirsB = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, dirsR = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirsB) {
                int nx = x + d[0], ny = y + d[1];
                while (inBounds(nx, ny)) {
                    Piece p = sq[nx][ny];
                    if (p != null) {
                        if (p.side == bySide && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true;
                        break;
                    }
                    nx += d[0];
                    ny += d[1];
                }
            }
            for (int[] d : dirsR) {
                int nx = x + d[0], ny = y + d[1];
                while (inBounds(nx, ny)) {
                    Piece p = sq[nx][ny];
                    if (p != null) {
                        if (p.side == bySide && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true;
                        break;
                    }
                    nx += d[0];
                    ny += d[1];
                }
            }
            return false;
        }

        boolean isInCheck(Side side) {
            int[] k = kingPos(side);
            if (k == null) return true;
            return isSquareAttacked(k[0], k[1], opposite(side));
        }

        List<Move> generatePseudoLegalMoves(Side side) {
            List<Move> mv = new ArrayList<>();
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    Piece p = sq[x][y];
                    if (p == null || p.side != side) continue;
                    switch (p.type) {
                        case PAWN -> genPawnMoves(x, y, p, mv);
                        case KNIGHT -> genKnightMoves(x, y, p, mv);
                        case BISHOP -> genSlideMoves(x, y, p, mv, true, false);
                        case ROOK -> genSlideMoves(x, y, p, mv, false, true);
                        case QUEEN -> genSlideMoves(x, y, p, mv, true, true);
                        case KING -> genKingMoves(x, y, p, mv);
                    }
                }
            return mv;
        }

        void genPawnMoves(int x, int y, Piece p, List<Move> out) {
            int dir = (p.side == Side.WHITE) ? 1 : -1;
            int startRank = (p.side == Side.WHITE) ? 1 : 6;
            int promoRank = (p.side == Side.WHITE) ? 7 : 0;

            int ny = y + dir;
            if (inBounds(x, ny) && sq[x][ny] == null) {
                if (ny == promoRank) {
                    for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                        Move pm = new Move(x, y, x, ny);
                        pm.promotion = pt;
                        out.add(pm);
                    }
                } else {
                    out.add(new Move(x, y, x, ny));
                    int ny2 = y + 2 * dir;
                    if (y == startRank && sq[x][ny2] == null) {
                        Move dm = new Move(x, y, x, ny2);
                        dm.isDoublePawnPush = true;
                        out.add(dm);
                    }
                }
            }
            for (int dx : new int[]{-1, 1}) {
                int nx = x + dx, nyc = y + dir;
                if (inBounds(nx, nyc)) {
                    Piece t = sq[nx][nyc];
                    if (t != null && t.side != p.side) {
                        if (nyc == promoRank) {
                            for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
                                Move pm = new Move(x, y, nx, nyc);
                                pm.promotion = pt;
                                out.add(pm);
                            }
                        } else out.add(new Move(x, y, nx, nyc));
                    }
                }
            }
            if (enPassantX != -1) {
                for (int dx : new int[]{-1, 1}) {
                    int nx = x + dx, nyep = y + dir;
                    if (nx == enPassantX && nyep == enPassantY) {
                        Move ep = new Move(x, y, nx, nyep);
                        ep.isEnPassant = true;
                        out.add(ep);
                    }
                }
            }
        }

        void genKnightMoves(int x, int y, Piece p, List<Move> out) {
            int[][] d = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};
            for (int[] v : d) {
                int nx = x + v[0], ny = y + v[1];
                if (!inBounds(nx, ny)) continue;
                Piece t = sq[nx][ny];
                if (t == null || t.side != p.side) out.add(new Move(x, y, nx, ny));
            }
        }

        void genSlideMoves(int x, int y, Piece p, List<Move> out, boolean diag, boolean orth) {
            int[][] dirs = buildDirs(diag, orth);
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                while (inBounds(nx, ny)) {
                    Piece t = sq[nx][ny];
                    if (t == null) {
                        out.add(new Move(x, y, nx, ny));
                    } else {
                        if (t.side != p.side) out.add(new Move(x, y, nx, ny));
                        break;
                    }
                    nx += d[0];
                    ny += d[1];
                }
            }
        }

        int[][] buildDirs(boolean diag, boolean orth) {
            List<int[]> dirs = new ArrayList<>();
            if (diag) dirs.addAll(Arrays.asList(new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}));
            if (orth) dirs.addAll(Arrays.asList(new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}));
            return dirs.toArray(new int[0][]);
        }

        void genKingMoves(int x, int y, Piece p, List<Move> out) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (!inBounds(nx, ny)) continue;
                    Piece t = sq[nx][ny];
                    if (t == null || t.side != p.side) out.add(new Move(x, y, nx, ny));
                }
            if (p.moved) return;
            if (p.side == Side.WHITE) {
                if (whiteCastleKing && sq[5][0] == null && sq[6][0] == null) {
                    if (!isSquareAttacked(4, 0, Side.BLACK) && !isSquareAttacked(5, 0, Side.BLACK) && !isSquareAttacked(6, 0, Side.BLACK)) {
                        Piece rook = sq[7][0];
                        if (rook != null && rook.type == PieceType.ROOK && !rook.moved) {
                            Move m = new Move(4, 0, 6, 0);
                            m.isCastleKingSide = true;
                            out.add(m);
                        }
                    }
                }
                if (whiteCastleQueen && sq[3][0] == null && sq[2][0] == null && sq[1][0] == null) {
                    if (!isSquareAttacked(4, 0, Side.BLACK) && !isSquareAttacked(3, 0, Side.BLACK) && !isSquareAttacked(2, 0, Side.BLACK)) {
                        Piece rook = sq[0][0];
                        if (rook != null && rook.type == PieceType.ROOK && !rook.moved) {
                            Move m = new Move(4, 0, 2, 0);
                            m.isCastleQueenSide = true;
                            out.add(m);
                        }
                    }
                }
            } else {
                if (blackCastleKing && sq[5][7] == null && sq[6][7] == null) {
                    if (!isSquareAttacked(4, 7, Side.WHITE) && !isSquareAttacked(5, 7, Side.WHITE) && !isSquareAttacked(6, 7, Side.WHITE)) {
                        Piece rook = sq[7][7];
                        if (rook != null && rook.type == PieceType.ROOK && !rook.moved) {
                            Move m = new Move(4, 7, 6, 7);
                            m.isCastleKingSide = true;
                            out.add(m);
                        }
                    }
                }
                if (blackCastleQueen && sq[3][7] == null && sq[2][7] == null && sq[1][7] == null) {
                    if (!isSquareAttacked(4, 7, Side.WHITE) && !isSquareAttacked(3, 7, Side.WHITE) && !isSquareAttacked(2, 7, Side.WHITE)) {
                        Piece rook = sq[0][7];
                        if (rook != null && rook.type == PieceType.ROOK && !rook.moved) {
                            Move m = new Move(4, 7, 2, 7);
                            m.isCastleQueenSide = true;
                            out.add(m);
                        }
                    }
                }
            }
        }

        List<Move> generateLegalMoves() {
            List<Move> pseudo = generatePseudoLegalMoves(sideToMove);
            List<Move> legal = new ArrayList<>();
            for (Move m : pseudo) if (wouldBeLegal(m)) legal.add(m);
            return legal;
        }

        boolean wouldBeLegal(Move m) {
            applyMove(m);
            boolean ok = !isInCheck(opposite(sideToMove));
            undoMove();
            return ok;
        }

        void applyMove(Move m) {
            m.prevEnPassantX = enPassantX;
            m.prevEnPassantY = enPassantY;
            m.prevWKC = whiteCastleKing;
            m.prevWQC = whiteCastleQueen;
            m.prevBKC = blackCastleKing;
            m.prevBQC = blackCastleQueen;
            m.prevHalfmoveClock = halfmoveClock;
            m.prevFullmoveNumber = fullmoveNumber;

            Piece moving = sq[m.fromX][m.fromY];
            Piece target = sq[m.toX][m.toY];
            m.capturedPiece = target;

            if (moving.type == PieceType.PAWN || target != null) halfmoveClock = 0;
            else halfmoveClock++;

            if (m.isEnPassant) {
                int dir = (moving.side == Side.WHITE) ? 1 : -1;
                Piece epPawn = sq[m.toX][m.toY - dir];
                m.capturedPiece = epPawn;
                sq[m.toX][m.toY - dir] = null;
            }

            sq[m.fromX][m.fromY] = null;
            sq[m.toX][m.toY] = moving;

            if (moving.type == PieceType.PAWN) {
                int promoRank = (moving.side == Side.WHITE) ? 7 : 0;
                if (m.toY == promoRank) {
                    PieceType pt = (m.promotion != null) ? m.promotion : PieceType.QUEEN; // default queen
                    moving = new Piece(pt, moving.side);
                    moving.moved = true;
                    sq[m.toX][m.toY] = moving;
                }
            }

            if (m.isCastleKingSide) {
                if (moving.side == Side.WHITE) {
                    Piece rook = sq[7][0];
                    sq[7][0] = null;
                    sq[5][0] = rook;
                    if (rook != null) rook.moved = true;
                } else {
                    Piece rook = sq[7][7];
                    sq[7][7] = null;
                    sq[5][7] = rook;
                    if (rook != null) rook.moved = true;
                }
            } else if (m.isCastleQueenSide) {
                if (moving.side == Side.WHITE) {
                    Piece rook = sq[0][0];
                    sq[0][0] = null;
                    sq[3][0] = rook;
                    if (rook != null) rook.moved = true;
                } else {
                    Piece rook = sq[0][7];
                    sq[0][7] = null;
                    sq[3][7] = rook;
                    if (rook != null) rook.moved = true;
                }
            }

            moving.moved = true;
            if (moving.type == PieceType.KING) {
                if (moving.side == Side.WHITE) {
                    whiteCastleKing = false;
                    whiteCastleQueen = false;
                } else {
                    blackCastleKing = false;
                    blackCastleQueen = false;
                }
            }
            if (moving.type == PieceType.ROOK) {
                if (moving.side == Side.WHITE) {
                    if (m.fromX == 0 && m.fromY == 0) whiteCastleQueen = false;
                    if (m.fromX == 7 && m.fromY == 0) whiteCastleKing = false;
                } else {
                    if (m.fromX == 0 && m.fromY == 7) blackCastleQueen = false;
                    if (m.fromX == 7 && m.fromY == 7) blackCastleKing = false;
                }
            }
            if (m.capturedPiece != null && m.capturedPiece.type == PieceType.ROOK) {
                if (m.toX == 0 && m.toY == 0) whiteCastleQueen = false;
                if (m.toX == 7 && m.toY == 0) whiteCastleKing = false;
                if (m.toX == 0 && m.toY == 7) blackCastleQueen = false;
                if (m.toX == 7 && m.toY == 7) blackCastleKing = false;
            }

            enPassantX = enPassantY = -1;
            if (m.isDoublePawnPush) {
                int dir = (moving.side == Side.WHITE) ? 1 : -1;
                enPassantX = m.fromX;
                enPassantY = m.fromY + dir;
            }

            if (sideToMove == Side.BLACK) fullmoveNumber++;
            sideToMove = opposite(sideToMove);
            history.push(m);
        }

        void undoMove() {
            if (history.isEmpty()) return;
            Move m = history.pop();

            sideToMove = opposite(sideToMove);
            fullmoveNumber = m.prevFullmoveNumber;
            halfmoveClock = m.prevHalfmoveClock;
            enPassantX = m.prevEnPassantX;
            enPassantY = m.prevEnPassantY;
            whiteCastleKing = m.prevWKC;
            whiteCastleQueen = m.prevWQC;
            blackCastleKing = m.prevBKC;
            blackCastleQueen = m.prevBQC;

            Piece moving = sq[m.toX][m.toY];
            boolean wasPromotion = (m.promotion != null);
            if (wasPromotion) {
                // revert to pawn of the mover side
                Side side = opposite(sideToMove);
                moving = new Piece(PieceType.PAWN, side);
            }

            if (m.isCastleKingSide) {
                if (moving.side == Side.WHITE) {
                    Piece rook = sq[5][0];
                    sq[5][0] = null;
                    sq[7][0] = rook;
                    if (rook != null) rook.moved = false;
                } else {
                    Piece rook = sq[5][7];
                    sq[5][7] = null;
                    sq[7][7] = rook;
                    if (rook != null) rook.moved = false;
                }
            } else if (m.isCastleQueenSide) {
                if (moving.side == Side.WHITE) {
                    Piece rook = sq[3][0];
                    sq[3][0] = null;
                    sq[0][0] = rook;
                    if (rook != null) rook.moved = false;
                } else {
                    Piece rook = sq[3][7];
                    sq[3][7] = null;
                    sq[0][7] = rook;
                    if (rook != null) rook.moved = false;
                }
            }

            sq[m.toX][m.toY] = null;
            sq[m.fromX][m.fromY] = moving;
            if (moving != null) moving.moved = false;

            if (m.isEnPassant) {
                int dir = (moving.side == Side.WHITE) ? 1 : -1;
                sq[m.toX][m.toY - dir] = m.capturedPiece;
            } else if (m.capturedPiece != null) {
                sq[m.toX][m.toY] = m.capturedPiece;
            }
        }

        boolean hasAnyLegalMoves() {
            return !generateLegalMoves().isEmpty();
        }

        boolean isFiftyMoveDraw() {
            return halfmoveClock >= 100;
        }

        boolean isInsufficientMaterial() {
            List<Piece> pieces = new ArrayList<>();
            for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) if (sq[x][y] != null) pieces.add(sq[x][y]);
            long nonKings = pieces.stream().filter(p -> p.type != PieceType.KING).count();
            if (nonKings == 0) return true;
            if (nonKings == 1) {
                Piece only = pieces.stream().filter(p -> p.type != PieceType.KING).findFirst().orElse(null);
                if (only.type == PieceType.BISHOP || only.type == PieceType.KNIGHT) return true;
            }
            return false;
        }
    }

    // ===== Engine (Minimax + Alpha-Beta) =====
    static class Engine {
        static final int MATE_SCORE = 100000;
        static final int[] VAL = new int[PieceType.values().length];

        static { // material values
            VAL[PieceType.PAWN.ordinal()] = 100;
            VAL[PieceType.KNIGHT.ordinal()] = 320;
            VAL[PieceType.BISHOP.ordinal()] = 330;
            VAL[PieceType.ROOK.ordinal()] = 500;
            VAL[PieceType.QUEEN.ordinal()] = 900;
            VAL[PieceType.KING.ordinal()] = 0;
        }

        // Simple piece-square tables (white perspective). Black is mirrored by rank.
        static final int[][] PST_PAWN = {
                {0, 0, 0, 0, 0, 0, 0, 0},
                {5, 10, 10, -20, -20, 10, 10, 5},
                {5, -5, -10, 0, 0, -10, -5, 5},
                {0, 0, 0, 20, 20, 0, 0, 0},
                {5, 5, 10, 25, 25, 10, 5, 5},
                {10, 10, 20, 30, 30, 20, 10, 10},
                {50, 50, 50, 50, 50, 50, 50, 50},
                {0, 0, 0, 0, 0, 0, 0, 0}
        };
        static final int[][] PST_KNIGHT = {
                {-50, -40, -30, -30, -30, -30, -40, -50},
                {-40, -20, 0, 0, 0, 0, -20, -40},
                {-30, 0, 10, 15, 15, 10, 0, -30},
                {-30, 5, 15, 20, 20, 15, 5, -30},
                {-30, 0, 15, 20, 20, 15, 0, -30},
                {-30, 5, 10, 15, 15, 10, 5, -30},
                {-40, -20, 0, 5, 5, 0, -20, -40},
                {-50, -40, -30, -30, -30, -30, -40, -50}
        };
        static final int[][] PST_BISHOP = {
                {-20, -10, -10, -10, -10, -10, -10, -20},
                {-10, 0, 0, 0, 0, 0, 0, -10},
                {-10, 0, 5, 10, 10, 5, 0, -10},
                {-10, 5, 5, 10, 10, 5, 5, -10},
                {-10, 0, 10, 10, 10, 10, 0, -10},
                {-10, 10, 10, 10, 10, 10, 10, -10},
                {-10, 5, 0, 0, 0, 0, 5, -10},
                {-20, -10, -10, -10, -10, -10, -10, -20}
        };
        static final int[][] PST_ROOK = {
                {0, 0, 5, 10, 10, 5, 0, 0},
                {-5, 0, 0, 0, 0, 0, 0, -5},
                {-5, 0, 0, 0, 0, 0, 0, -5},
                {-5, 0, 0, 0, 0, 0, 0, -5},
                {-5, 0, 0, 0, 0, 0, 0, -5},
                {-5, 0, 0, 0, 0, 0, 0, -5},
                {5, 10, 10, 10, 10, 10, 10, 5},
                {0, 0, 0, 0, 0, 0, 0, 0}
        };
        static final int[][] PST_QUEEN = {
                {-20, -10, -10, -5, -5, -10, -10, -20},
                {-10, 0, 0, 0, 0, 0, 0, -10},
                {-10, 0, 5, 5, 5, 5, 0, -10},
                {-5, 0, 5, 5, 5, 5, 0, -5},
                {-5, 0, 5, 5, 5, 5, 0, -5},
                {-10, 0, 5, 5, 5, 5, 0, -10},
                {-10, 0, 0, 0, 0, 0, 0, -10},
                {-20, -10, -10, -5, -5, -10, -10, -20}
        };
        static final int[][] PST_KING_MID = {
                {-30, -40, -40, -50, -50, -40, -40, -30},
                {-30, -40, -40, -50, -50, -40, -40, -30},
                {-30, -40, -40, -50, -50, -40, -40, -30},
                {-30, -40, -40, -50, -50, -40, -40, -30},
                {-20, -30, -30, -40, -40, -30, -30, -20},
                {-10, -20, -20, -20, -20, -20, -20, -10},
                {20, 20, 0, 0, 0, 0, 20, 20},
                {20, 30, 10, 0, 0, 10, 30, 20}
        };

        static int eval(Board b) {
            int score = 0;
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    Piece p = b.sq[x][y];
                    if (p == null) continue;
                    int base = VAL[p.type.ordinal()];
                    int pst = switch (p.type) {
                        case PAWN -> pstVal(PST_PAWN, x, y, p.side);
                        case KNIGHT -> pstVal(PST_KNIGHT, x, y, p.side);
                        case BISHOP -> pstVal(PST_BISHOP, x, y, p.side);
                        case ROOK -> pstVal(PST_ROOK, x, y, p.side);
                        case QUEEN -> pstVal(PST_QUEEN, x, y, p.side);
                        case KING -> pstVal(PST_KING_MID, x, y, p.side);
                    };
                    int v = base + pst;
                    score += (p.side == Side.WHITE) ? v : -v;
                }
            // small bonus for mobility
            int mob = b.generateLegalMoves().size();
            score += (b.sideToMove == Side.WHITE) ? mob : -mob;
            return score;
        }

        static int pstVal(int[][] pst, int x, int y, Side side) {
            // PSTs defined for white with y up (0 bottom). Our board uses y=0 as white back rank.
            int yy = (side == Side.WHITE) ? y : 7 - y;
            return pst[yy][x];
        }

        static Move findBestMove(Board root, Side computerSide, int depth) {
            List<Move> legal = root.generateLegalMoves();
            if (legal.isEmpty()) return null;
            // simple ordering: captures first (and promotions)
            legal.sort((a, b) -> {
                Piece ta = root.get(a.toX, a.toY);
                Piece tb = root.get(b.toX, b.toY);
                int va = (ta != null ? VAL[ta.type.ordinal()] : 0) + (a.promotion != null ? 50 : 0);
                int vb = (tb != null ? VAL[tb.type.ordinal()] : 0) + (b.promotion != null ? 50 : 0);
                return Integer.compare(vb, va);
            });

            int bestScore = (computerSide == Side.WHITE) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            Move best = legal.get(0);

            Board work = root.copy(); // search on a copy to avoid touching UI board
            for (Move m : legal) {
                work.applyMove(m);
                int s = search(work, depth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
                work.undoMove();
                if (computerSide == Side.WHITE) {
                    if (s > bestScore) {
                        bestScore = s;
                        best = m;
                    }
                } else {
                    if (s < bestScore) {
                        bestScore = s;
                        best = m;
                    }
                }
            }
            // default queen promotion if needed
            if (best.promotion == null) {
                Piece moving = root.get(best.fromX, best.fromY);
                if (moving != null && moving.type == PieceType.PAWN) {
                    int promoRank = (moving.side == Side.WHITE) ? 7 : 0;
                    if (best.toY == promoRank) best.promotion = PieceType.QUEEN;
                }
            }
            return best;
        }

        static int search(Board b, int depth, int alpha, int beta) {
            List<Move> legal = b.generateLegalMoves();

            // terminal
            if (depth <= 0 || legal.isEmpty()) {
                if (legal.isEmpty()) {
                    if (b.isInCheck(b.sideToMove)) {
                        // side to move is checkmated
                        return (b.sideToMove == Side.WHITE) ? -MATE_SCORE : MATE_SCORE;
                    } else {
                        // stalemate
                        return 0;
                    }
                }
                return eval(b);
            }

            boolean maximizing = (b.sideToMove == Side.WHITE);
            if (maximizing) {
                int best = Integer.MIN_VALUE + 1;
                for (Move m : legal) {
                    b.applyMove(m);
                    int val = search(b, depth - 1, alpha, beta);
                    b.undoMove();
                    best = Math.max(best, val);
                    alpha = Math.max(alpha, val);
                    if (alpha >= beta) break;
                }
                return best;
            } else {
                int best = Integer.MAX_VALUE - 1;
                for (Move m : legal) {
                    b.applyMove(m);
                    int val = search(b, depth - 1, alpha, beta);
                    b.undoMove();
                    best = Math.min(best, val);
                    beta = Math.min(beta, val);
                    if (alpha >= beta) break;
                }
                return best;
            }
        }
    }

    // ===== UI =====

    private final GridPane boardUI = new GridPane();
    private final Label status = new Label();
    private final Button btnNew = new Button("New Game");
    private final Button btnUndo = new Button("Undo");
    private final ComboBox<String> modeCombo = new ComboBox<>();
    private final ComboBox<String> themeCombo = new ComboBox<>();
    private final Slider difficultySlider = new Slider(1, 4, 2);
    private final Label difficultyLabel = new Label("Difficulty: 2");

    private final StackPane centerStack = new StackPane();
    private final ProgressIndicator thinkingIndicator = new ProgressIndicator();

    private Board board = new Board();

    // Selection state
    private int selX = -1, selY = -1;
    private List<Move> selMoves = Collections.emptyList();

    // Computer settings
    private Side computerSide = Side.BLACK;

    // Themes
    static class Theme {
        final Color LIGHT, DARK, H_FROM, H_MOVE, H_CAPTURE, COORD, BG;

        Theme(Color LIGHT, Color DARK, Color H_FROM, Color H_MOVE, Color H_CAPTURE, Color COORD, Color BG) {
            this.LIGHT = LIGHT;
            this.DARK = DARK;
            this.H_FROM = H_FROM;
            this.H_MOVE = H_MOVE;
            this.H_CAPTURE = H_CAPTURE;
            this.COORD = COORD;
            this.BG = BG;
        }
    }

    private Theme theme = new Theme(Color.web("#EEEED2"), Color.web("#769656"),
            Color.web("#FFE082"), Color.web("#A7FFEB"), Color.web("#FFABAB"),
            Color.web("#455A64"), Color.web("#fafafa"));

    private static String pieceToUnicode(Piece p) {
        if (p == null) return "";
        return switch (p.type) {
            case KING -> (p.side == Side.WHITE) ? "♔" : "♚";
            case QUEEN -> (p.side == Side.WHITE) ? "♕" : "♛";
            case ROOK -> (p.side == Side.WHITE) ? "♖" : "♜";
            case BISHOP -> (p.side == Side.WHITE) ? "♗" : "♝";
            case KNIGHT -> (p.side == Side.WHITE) ? "♘" : "♞";
            case PAWN -> (p.side == Side.WHITE) ? "♙" : "♟";
        };
    }

    private static Font pieceFont() {
        return Font.font("Segoe UI Symbol", FontWeight.BOLD, 48);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("ChessFX");

        thinkingIndicator.setVisible(false);
        thinkingIndicator.setMaxSize(80, 80);

        configToolbar();
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        boardUI.setGridLinesVisible(false);
        boardUI.setPadding(new Insets(8));
        centerStack.getChildren().add(boardUI);
        centerStack.getChildren().add(thinkingIndicator);

        BorderPane root = new BorderPane();
        root.setCenter(centerStack);
        VBox controls = buildControls();
        root.setRight(controls);
        BorderPane.setAlignment(boardUI, Pos.CENTER);
        BorderPane.setAlignment(centerStack, Pos.CENTER);

        Scene scene = new Scene(root,940,820);
        root.setStyle("-fx-background-color: " + toRgb(theme.BG) + ";");
        stage.setScene(scene);
        stage.show();
        redrawBoard();
        updateStatus();
    }

    private void configToolbar() {
        modeCombo.getItems().addAll(
                "Human vs Human",
                "Human vs Computer (You: White)",
                "Human vs Computer (You: Black)"
        );
        modeCombo.getSelectionModel().select("Human vs Computer (You: White)");

        themeCombo.getItems().addAll("Classic", "Blue", "Monochrome", "Wood");
        themeCombo.getSelectionModel().select("Classic");

        difficultySlider.setShowTickLabels(true);
        difficultySlider.setShowTickMarks(true);
        difficultySlider.setMajorTickUnit(1);
        difficultySlider.setMinorTickCount(0);
        difficultySlider.setSnapToTicks(true);

        difficultySlider.valueProperty().addListener((obs, oldV, newV) -> {
            int d = newV.intValue();
            difficultyLabel.setText("Difficulty: " + d);
        });

        modeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            switch (newV) {
                case "Human vs Human" -> computerSide = null;
                case "Human vs Computer (You: White)" -> computerSide = Side.BLACK;
                case "Human vs Computer (You: Black)" -> computerSide = Side.WHITE;
            }
            // If it's computer's turn immediately, trigger move
            triggerComputerIfNeeded();
        });

        themeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            switch (newV) {
                case "Classic" -> theme = new Theme(Color.web("#EEEED2"), Color.web("#769656"),
                        Color.web("#FFE082"), Color.web("#A7FFEB"), Color.web("#FFABAB"),
                        Color.web("#37474F"), Color.web("#fafafa"));
                case "Blue" -> theme = new Theme(Color.web("#DCE6F2"), Color.web("#5B7DB1"),
                        Color.web("#FFF59D"), Color.web("#C5E1A5"), Color.web("#EF9A9A"),
                        Color.web("#263238"), Color.web("#ECEFF1"));
                case "Monochrome" -> theme = new Theme(Color.web("#FAFAFA"), Color.web("#CFCFCF"),
                        Color.web("#FFF176"), Color.web("#B2DFDB"), Color.web("#E57373"),
                        Color.web("#424242"), Color.web("#FFFFFF"));
                case "Wood" -> theme = new Theme(Color.web("#EBD5B3"), Color.web("#8B5A2B"),
                        Color.web("#FFD54F"), Color.web("#A5D6A7"), Color.web("#EF9A9A"),
                        Color.web("#3E2723"), Color.web("#F2E1C2"));
            }
            centerStack.getParent().setStyle("-fx-background-color: " + toRgb(theme.BG) + ";");
            redrawBoard();
        });
    }

    private VBox buildControls() {
        btnNew.setOnAction(e -> {
            board = new Board();
            clearSelection();
            updateStatus();
            redrawBoard();
            triggerComputerIfNeeded();
        });
        btnUndo.setOnAction(e -> {
            if (computerSide != null) {
                // undo both computer + human moves if available
                if (!board.history.isEmpty()) board.undoMove();
                if (!board.history.isEmpty()) board.undoMove();
            } else {
                if (!board.history.isEmpty()) board.undoMove();
            }
            clearSelection();
            updateStatus();
            redrawBoard();
        });

        VBox controls = new VBox(12,
                btnNew, btnUndo,
                new Separator(),
                new Label("Mode:"), modeCombo,
                new Separator(),
                new Label("Theme:"), themeCombo,
                new Separator(),
                difficultyLabel, difficultySlider,
                new Separator(),
                status
        );
        controls.setAlignment(Pos.TOP_LEFT);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-border-color: rgba(0,0,0,0.1); -fx-border-width: 0 0 1 0;");
        return controls;
    }

    private void redrawBoard() {
        boardUI.getChildren().clear();
        boardUI.getColumnConstraints().clear();
        boardUI.getRowConstraints().clear();
        for (int i = 0; i < 8; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(12.5);
            boardUI.getColumnConstraints().add(cc);
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(12.5);
            boardUI.getRowConstraints().add(rc);
        }

        for (int rx = 0; rx < 8; rx++) {
            for (int ry = 0; ry < 8; ry++) {
                int x = rx;
                int y = 7 - ry;

                StackPane cell = new StackPane();
                cell.setMinSize(80, 80);
                cell.setPrefSize(80, 80);

                Color base = ((x + y) % 2 == 0) ? theme.LIGHT : theme.DARK;
                BackgroundFill bf = new BackgroundFill(base, new CornerRadii(6), new Insets(2));
                cell.setBackground(new Background(bf));
                cell.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);");

                // Coordinate labels (files on bottom row, ranks on left column)
                if (y == 0) {
                    Label fileLbl = new Label("" + (char) ('a' + x));
                    fileLbl.setTextFill(theme.COORD);
                    fileLbl.setStyle("-fx-font-size: 12px;");
                    StackPane.setAlignment(fileLbl, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(fileLbl, new Insets(0, 0, 6, 8));
                    cell.getChildren().add(fileLbl);
                }
                if (x == 0) {
                    Label rankLbl = new Label("" + (y + 1));
                    rankLbl.setTextFill(theme.COORD);
                    rankLbl.setStyle("-fx-font-size: 12px;");
                    StackPane.setAlignment(rankLbl, Pos.TOP_RIGHT);
                    StackPane.setMargin(rankLbl, new Insets(8, 8, 0, 0));
                    cell.getChildren().add(rankLbl);
                }

                // Highlights
                boolean isFrom = (x == selX && y == selY);
                if (isFrom) {
                    cell.setBackground(new Background(new BackgroundFill(theme.H_FROM, new CornerRadii(6), new Insets(2))));
                } else {
                    for (Move m : selMoves) {
                        if (m.toX == x && m.toY == y) {
                            Piece target = board.get(x, y);
                            Color hc = (target != null || m.isEnPassant) ? theme.H_CAPTURE : theme.H_MOVE;
                            cell.setBackground(new Background(new BackgroundFill(hc, new CornerRadii(6), new Insets(2))));
                            // Add small dot for quiet moves, ring for capture
                            Circle dot = new Circle((target != null || m.isEnPassant) ? 16 : 10,
                                    (target != null || m.isEnPassant) ? Color.TRANSPARENT : Color.web("#2E7D32"));
                            if (target != null || m.isEnPassant) {
                                dot.setStroke(Color.web("#B71C1C"));
                                dot.setStrokeWidth(3);
                            }
                            cell.getChildren().add(dot);
                            break;
                        }
                    }
                }

                Label lbl = new Label(pieceToUnicode(board.get(x, y)));
                lbl.setFont(pieceFont());
                lbl.setTextFill(((x + y) % 2 == 0) ? Color.web("#222") : Color.web("#111"));
                cell.getChildren().add(lbl);

                final int fx = x, fy = y;
                cell.setOnMouseClicked(ev -> {
                    if (ev.getButton() != MouseButton.PRIMARY) return;
                    onCellClicked(fx, fy);
                });

                boardUI.add(cell, rx, ry);
            }
        }
    }

    private void onCellClicked(int x, int y) {
        if (isComputerTurn()) return; // ignore clicks while computer is moving

        Piece p = board.get(x, y);

        // Fresh selection
        if (selX == -1) {
            if (p != null && isHumanSide(p.side)) {
                selX = x;
                selY = y;
                selMoves = legalMovesFrom(x, y);
            } else clearSelection();
            redrawBoard();
            return;
        }

        // Clicking own piece again -> change selection
        if (p != null && isHumanSide(p.side)) {
            selX = x;
            selY = y;
            selMoves = legalMovesFrom(x, y);
            redrawBoard();
            return;
        }

        // Try move
        Optional<Move> chosen = selMoves.stream().filter(m -> m.toX == x && m.toY == y).findFirst();
        if (chosen.isPresent()) {
            Move m = chosen.get();
            Piece moving = board.get(selX, selY);
            if (moving != null && moving.type == PieceType.PAWN) {
                int promoRank = (moving.side == Side.WHITE) ? 7 : 0;
                if (m.toY == promoRank && m.promotion == null) {
                    m.promotion = askPromotion(moving.side);
                }
            }
            board.applyMove(m);
            clearSelection();
            updateStatus();
            redrawBoard();
            triggerComputerIfNeeded();
        } else {
            clearSelection();
            redrawBoard();
        }
    }

    private boolean isHumanSide(Side s) {
        if (computerSide == null) return true; // Human vs Human
        return s != computerSide;
    }

    private boolean isComputerTurn() {
        return computerSide != null && board.sideToMove == computerSide;
    }

    private void triggerComputerIfNeeded() {
        if (!isComputerTurn()) return;
        if (isGameOver()) return;

        thinkingIndicator.setVisible(true);

        int depth = (int) difficultySlider.getValue();
        Side comp = computerSide;
        Board snapshot = board.copy(); // compute on copy

        Task<Move> task = new Task<>() {
            @Override
            protected Move call() {
                return Engine.findBestMove(snapshot, comp, Math.max(1, depth));
            }
        };
        task.setOnSucceeded(ev -> {
            Move best = task.getValue();
            thinkingIndicator.setVisible(false);
            if (best != null) {
                // Ensure promotion default for AI
                Piece mv = board.get(best.fromX, best.fromY);
                if (mv != null && mv.type == PieceType.PAWN) {
                    int promoRank = (mv.side == Side.WHITE) ? 7 : 0;
                    if (best.toY == promoRank && best.promotion == null) best.promotion = PieceType.QUEEN;
                }
                board.applyMove(best);
                updateStatus();
                redrawBoard();
                // If still computer turn (e.g., after illegal human state), chain again
                if (isComputerTurn() && !isGameOver()) triggerComputerIfNeeded();
            } else {
                updateStatus();
            }
        });
        task.setOnFailed(ev -> {
            thinkingIndicator.setVisible(false);
            updateStatus();
            ev.getSource().getException().printStackTrace();
        });

        Thread th = new Thread(task, "AI-Thread");
        th.setDaemon(true);
        th.start();
    }

    private boolean isGameOver() {
        boolean inCheck = board.isInCheck(board.sideToMove);
        boolean noMoves = !board.hasAnyLegalMoves();
        return noMoves || board.isFiftyMoveDraw() || board.isInsufficientMaterial();
    }

    private void clearSelection() {
        selX = selY = -1;
        selMoves = Collections.emptyList();
    }

    private List<Move> legalMovesFrom(int x, int y) {
        Piece p = board.get(x, y);
        if (p == null || p.side != board.sideToMove) return Collections.emptyList();
        return board.generateLegalMoves().stream().filter(m -> m.fromX == x && m.fromY == y).collect(Collectors.toList());
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        Side stm = board.sideToMove;
        boolean inCheck = board.isInCheck(stm);
        List<Move> legal = board.generateLegalMoves();

        if (legal.isEmpty()) {
            if (inCheck) sb.append("Checkmate! ").append((stm == Side.WHITE) ? "Black" : "White").append(" wins.");
            else sb.append("Stalemate. Draw.");
        } else if (board.isFiftyMoveDraw()) {
            sb.append("Draw by 50-move rule.");
        } else if (board.isInsufficientMaterial()) {
            sb.append("Draw by insufficient material.");
        } else {
            sb.append((stm == Side.WHITE) ? "White" : "Black").append(" to move");
            if (inCheck) sb.append(" — Check!");
            if (isComputerTurn()) sb.append(" (Computer thinking...)");
        }
        status.setText(sb.toString());
    }

    private PieceType askPromotion(Side side) {
        List<String> choices = Arrays.asList("Queen", "Rook", "Bishop", "Knight");
        ChoiceDialog<String> dlg = new ChoiceDialog<>("Queen", choices);
        dlg.setTitle("Pawn Promotion");
        dlg.setHeaderText((side == Side.WHITE ? "White" : "Black") + " pawn promotion");
        dlg.setContentText("Promote to:");
        Optional<String> result = dlg.showAndWait();
        String sel = result.orElse("Queen");
        return switch (sel) {
            case "Rook" -> PieceType.ROOK;
            case "Bishop" -> PieceType.BISHOP;
            case "Knight" -> PieceType.KNIGHT;
            default -> PieceType.QUEEN;
        };
    }

    private static String toRgb(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}