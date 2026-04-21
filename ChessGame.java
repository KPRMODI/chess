import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * ChessGame.java  –  Complete single-file Java Swing chess game.
 *
 * HOW TO RUN:
 *   javac ChessGame.java
 *   java  ChessGame
 *
 * Place your piece images in a folder called "resources/" next to ChessGame.java.
 * Expected names: whiteking.png  whitequeen.png  whiterook.png  whitebishop.png
 *                 whiteknight.png  whitepawn.png   (same pattern for black)
 *
 * Supports:
 *   - 2-Player local mode
 *   - vs AI  (minimax + alpha-beta, depth 3)
 *   - Castling, En Passant, Pawn Promotion
 *   - Check / Checkmate / Stalemate detection
 *   - Legal-move highlighting
 *   - Fully scalable / resizable window
 */
public class ChessGame extends JFrame {

    // =========================================================================
    //  CONSTANTS
    // =========================================================================
    private static final int MIN_TILE = 60;
    private static final int DEFAULT_TILE = 80;

    // Piece type constants (kept as int for speed in AI search)
    static final int EMPTY  = 0;
    static final int PAWN   = 1;
    static final int KNIGHT = 2;
    static final int BISHOP = 3;
    static final int ROOK   = 4;
    static final int QUEEN  = 5;
    static final int KING   = 6;

    // Colour constants
    static final int WHITE = 1;
    static final int BLACK = -1;

    // =========================================================================
    //  ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGame::new);
    }

    // =========================================================================
    //  FIELDS
    // =========================================================================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);

    private BoardPanel boardPanel;
    private boolean    vsAI = false;

    // =========================================================================
    //  CONSTRUCTOR
    // =========================================================================
    public ChessGame() {
        super("Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(500, 560));
        setSize(700, 760);
        setLocationRelativeTo(null);

        cardPanel.add(buildMenuPanel(), "MENU");

        add(cardPanel);
        setVisible(true);
        cardLayout.show(cardPanel, "MENU");
    }

    // =========================================================================
    //  MENU SCREEN
    // =========================================================================
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0x2C2C2C));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(14, 40, 14, 40);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.gridx  = 0;

        // Title
        JLabel title = new JLabel("♟  Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 52));
        title.setForeground(new Color(0xF0D9B5));
        gbc.gridy = 0; gbc.insets = new Insets(40, 40, 30, 40);
        panel.add(title, gbc);

        // Subtitle
        JLabel sub = new JLabel("Choose a game mode", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 18));
        sub.setForeground(new Color(0xB0A090));
        gbc.gridy = 1; gbc.insets = new Insets(0, 40, 30, 40);
        panel.add(sub, gbc);

        // Friend button
        JButton btnFriend = menuButton("👥  Play vs Friend", new Color(0x4A7C59));
        gbc.gridy = 2; gbc.insets = new Insets(10, 80, 10, 80);
        panel.add(btnFriend, gbc);

        // AI button
        JButton btnAI = menuButton("🤖  Play vs AI", new Color(0x7C4A4A));
        gbc.gridy = 3;
        panel.add(btnAI, gbc);

        btnFriend.addActionListener(e -> startGame(false));
        btnAI    .addActionListener(e -> startGame(true));

        return panel;
    }

    private JButton menuButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 20));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(260, 60));
        // Hover effect
        Color hover = bg.brighter();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited (MouseEvent e) { btn.setBackground(bg);    }
        });
        return btn;
    }

    // =========================================================================
    //  START / RESTART
    // =========================================================================
    private void startGame(boolean ai) {
        vsAI = ai;
        if (boardPanel != null) cardPanel.remove(boardPanel);
        boardPanel = new BoardPanel(vsAI, this);
        cardPanel.add(boardPanel, "GAME");
        cardLayout.show(cardPanel, "GAME");
        boardPanel.requestFocusInWindow();
    }

    void returnToMenu() {
        cardLayout.show(cardPanel, "MENU");
    }

    // =========================================================================
    // =========================================================================
    //  BOARD PANEL  (the actual game)
    // =========================================================================
    // =========================================================================
    static class BoardPanel extends JPanel {

        // ── Layout ────────────────────────────────────────────────────────────
        private static final int STATUS_H = 44;
        private static final int BTN_H    = 44;

        // ── Colours ───────────────────────────────────────────────────────────
        private static final Color COL_LIGHT   = new Color(0xF0D9B5);
        private static final Color COL_DARK    = new Color(0xB58863);
        private static final Color COL_SEL     = new Color(0x7FC97F);
        private static final Color COL_HINT    = new Color(0xCDD26A);
        private static final Color COL_CHECK   = new Color(0xE74C3C);
        private static final Color COL_LASTFR  = new Color(0xF6F669);
        private static final Color COL_LASTTO  = new Color(0xF6F669);
        private static final Color COL_BG      = new Color(0x2C2C2C);
        private static final Color COL_STATUS  = new Color(0xF0D9B5);

        // ── Game state ────────────────────────────────────────────────────────
        /** board[row][col] – positive = white, negative = black, 0 = empty.
         *  Value magnitude is piece type constant (PAWN=1 … KING=6). */
        private final int[][] board   = new int[8][8];
        private int           turn    = WHITE;  // WHITE=1 or BLACK=-1

        // Castling rights
        private boolean whiteKingMoved  = false;
        private boolean blackKingMoved  = false;
        private boolean whiteRookAMoved = false; // queenside (col 0)
        private boolean whiteRookHMoved = false; // kingside  (col 7)
        private boolean blackRookAMoved = false;
        private boolean blackRookHMoved = false;

        // En passant: column of the pawn that just double-pushed, or -1
        private int enPassantCol = -1;

        // Selection / move hints
        private int        selRow = -1, selCol = -1;
        private List<int[]> hints = new ArrayList<>();

        // Last move (for highlight)
        private int lastFR = -1, lastFC = -1, lastTR = -1, lastTC = -1;

        // Game-over flag
        private boolean gameOver = false;

        // AI
        private final boolean vsAI;
        private final ChessGame parent;
        private static final int AI_DEPTH = 3;

        // Images
        private final Map<Integer, BufferedImage> images = new HashMap<>();

        // Piece-square tables for AI evaluation (from white's perspective)
        // index [row][col]
        private static final int[][] PST_PAWN = {
            { 0,  0,  0,  0,  0,  0,  0,  0},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {10, 10, 20, 30, 30, 20, 10, 10},
            { 5,  5, 10, 25, 25, 10,  5,  5},
            { 0,  0,  0, 20, 20,  0,  0,  0},
            { 5, -5,-10,  0,  0,-10, -5,  5},
            { 5, 10, 10,-20,-20, 10, 10,  5},
            { 0,  0,  0,  0,  0,  0,  0,  0}
        };
        private static final int[][] PST_KNIGHT = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
        };
        private static final int[][] PST_BISHOP = {
            {-20,-10,-10,-10,-10,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5, 10, 10,  5,  0,-10},
            {-10,  5,  5, 10, 10,  5,  5,-10},
            {-10,  0, 10, 10, 10, 10,  0,-10},
            {-10, 10, 10, 10, 10, 10, 10,-10},
            {-10,  5,  0,  0,  0,  0,  5,-10},
            {-20,-10,-10,-10,-10,-10,-10,-20}
        };
        private static final int[][] PST_ROOK = {
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 5, 10, 10, 10, 10, 10, 10,  5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            { 0,  0,  0,  5,  5,  0,  0,  0}
        };
        private static final int[][] PST_QUEEN = {
            {-20,-10,-10, -5, -5,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5,  5,  5,  5,  0,-10},
            { -5,  0,  5,  5,  5,  5,  0, -5},
            {  0,  0,  5,  5,  5,  5,  0, -5},
            {-10,  5,  5,  5,  5,  5,  0,-10},
            {-10,  0,  5,  0,  0,  0,  0,-10},
            {-20,-10,-10, -5, -5,-10,-10,-20}
        };
        private static final int[][] PST_KING_MID = {
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-20,-30,-30,-40,-40,-30,-30,-20},
            {-10,-20,-20,-20,-20,-20,-20,-10},
            { 20, 20,  0,  0,  0,  0, 20, 20},
            { 20, 30, 10,  0,  0, 10, 30, 20}
        };

        // ── Constructor ───────────────────────────────────────────────────────
        BoardPanel(boolean vsAI, ChessGame parent) {
            this.vsAI  = vsAI;
            this.parent = parent;
            setBackground(COL_BG);
            loadImages();
            resetBoard();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onMouseClick(e); }
            });
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { repaint(); }
            });
        }

        // ── Board setup ───────────────────────────────────────────────────────
        private void resetBoard() {
            for (int[] row : board) Arrays.fill(row, EMPTY);
            // Black back rank  (row 0)
            int[] back = {ROOK,KNIGHT,BISHOP,QUEEN,KING,BISHOP,KNIGHT,ROOK};
            for (int c = 0; c < 8; c++) {
                board[0][c] = -back[c];     // black
                board[1][c] = -PAWN;
                board[6][c] =  PAWN;        // white
                board[7][c] =  back[c];
            }
            turn = WHITE; selRow = -1; selCol = -1;
            hints.clear(); gameOver = false; enPassantCol = -1;
            whiteKingMoved=false; blackKingMoved=false;
            whiteRookAMoved=false; whiteRookHMoved=false;
            blackRookAMoved=false; blackRookHMoved=false;
            lastFR=-1; lastFC=-1; lastTR=-1; lastTC=-1;
        }

        // ── Image loading ─────────────────────────────────────────────────────
private void loadImages() {
            // Maps piece code → filename fragment
            int[]    types = { PAWN,   KNIGHT,   BISHOP,   ROOK,   QUEEN,   KING  };
            String[] names = {"pawn","knight","bishop","rook","queen","king"};
            for (int i = 0; i < types.length; i++) {
                images.put( types[i], loadImg("images/white" + names[i] + ".png"));
                images.put(-types[i], loadImg("images/black" + names[i] + ".png"));
            }
        }

        private BufferedImage loadImg(String path) {
            try { return ImageIO.read(new File(path)); }
            catch (IOException ex) { return null; }
        }

        // ── Tile size (responsive) ────────────────────────────────────────────
        private int tileSize() {
            int w = getWidth();
            int h = getHeight() - STATUS_H - BTN_H;
            int t = Math.min(w, h) / 8;
            return Math.max(t, MIN_TILE);
        }

        // ── Mouse handling ────────────────────────────────────────────────────
       private void onMouseClick(MouseEvent e) {
            if (gameOver) return;
            if (vsAI && turn == BLACK) return; // wait for AI

            int ts = tileSize();
            int boardPx = 8 * ts;
            int offsetX = (getWidth() - boardPx) / 2;
            int offsetY = Math.max(0, (getHeight() - STATUS_H - BTN_H - boardPx) / 2);

            // Use integer division to safely floor the coordinates
            int col = (e.getX() - offsetX) / ts;
            int row = (e.getY() - offsetY) / ts;

            // If the user clicks outside the 8x8 board bounds, deselect and return
            if (col < 0 || col > 7 || row < 0 || row > 7) {
                selRow = -1; selCol = -1; hints.clear(); repaint();
                return;
            }

            System.out.println("Click: row=" + row + ", col=" + col + " (pixel x,y=" + e.getX() + "," + e.getY() + ")");

            handleSquareClick(row, col);
        }

        private void handleSquareClick(int row, int col) {
            int piece = board[row][col];
            System.out.println("Handle click: row=" + row + ", col=" + col + ", piece=" + piece + ", turn=" + turn);

            if (selRow == -1) {
                // Select own piece
                if (piece != EMPTY && color(piece) == turn) {
                    selRow = row; selCol = col;
                    hints  = generateLegalMoves(board, row, col,
                                                turn, enPassantCol,
                                                whiteKingMoved, blackKingMoved,
                                                whiteRookAMoved, whiteRookHMoved,
                                                blackRookAMoved, blackRookHMoved);
                    System.out.println("Selected (" + row + "," + col + "), hints: " + hints.size());
                    repaint();
                }
                return;
            }

            // Check if clicked square is a legal destination
            boolean isHint = false;
            System.out.print("Hints: ");
            for (int[] h : hints) {
                System.out.print("[" + h[2] + "," + h[3] + "] ");
                if (h[2] == row && h[3] == col) {
                    isHint = true;
                    System.out.print("(MATCH!) ");
                }
            }
            System.out.println();
            // Re-compute for debug print
            int tsDbg = tileSize();
            int boardPxDbg = 8 * tsDbg;
            int offsetXDbg = (getWidth() - boardPxDbg) / 2;
            int offsetYDbg = Math.max(0, (getHeight() - STATUS_H - BTN_H - boardPxDbg) / 2);
            System.out.println("Target click computed as [" + row + "," + col + "], isHint=" + isHint + 
                               " (offsets: ox=" + offsetXDbg + ", oy=" + offsetYDbg + ", ts=" + tsDbg + ")");
            System.out.println("Clicked [" + row + "," + col + "], isHint=" + isHint);

            if (isHint) {
                System.out.println("Executing move (" + selRow + "," + selCol + ") -> (" + row + "," + col + ")");
                executeMove(selRow, selCol, row, col);
            } else if (piece != EMPTY && color(piece) == turn) {
                // Re-select
                selRow = row; selCol = col;
                hints  = generateLegalMoves(board, row, col,
                                            turn, enPassantCol,
                                            whiteKingMoved, blackKingMoved,
                                            whiteRookAMoved, whiteRookHMoved,
                                            blackRookAMoved, blackRookHMoved);
                System.out.println("Re-selected (" + row + "," + col + "), hints: " + hints.size());
                repaint();
            } else {
                System.out.println("Deselect");
                selRow = -1; selCol = -1; hints.clear(); repaint();
            }
        }

        // ── Execute a move on the live board ──────────────────────────────────
        private void executeMove(int fr, int fc, int tr, int tc) {
            int piece = board[fr][fc];
            int type  = Math.abs(piece);
            int col   = color(piece);

            lastFR = fr; lastFC = fc; lastTR = tr; lastTC = tc;

            // En passant capture
            int newEP = -1;
            if (type == PAWN && fc == enPassantCol && Math.abs(fr - tr) == 1 && Math.abs(fc - tc) == 1
                    && board[tr][tc] == EMPTY) {
                board[fr][tc] = EMPTY;  // remove the captured pawn
            }
            // Double pawn push → set en-passant column
            if (type == PAWN && Math.abs(tr - fr) == 2) newEP = fc;

            // Castling
            if (type == KING && Math.abs(tc - fc) == 2) {
                if (tc == 6) { board[fr][5] = board[fr][7]; board[fr][7] = EMPTY; } // kingside
                else         { board[fr][3] = board[fr][0]; board[fr][0] = EMPTY; } // queenside
            }

            // Move piece
            board[tr][tc] = piece;
            board[fr][fc] = EMPTY;

            // Track moved flags
            if (type == KING) { if (col==WHITE) whiteKingMoved=true; else blackKingMoved=true; }
            if (type == ROOK) {
                if (col==WHITE) { if (fc==0) whiteRookAMoved=true; if (fc==7) whiteRookHMoved=true; }
                else            { if (fc==0) blackRookAMoved=true; if (fc==7) blackRookHMoved=true; }
            }

            enPassantCol = newEP;

            // Pawn promotion
            if (type == PAWN && (tr == 0 || tr == 7)) {
                int promoted = promptPromotion(col);
                board[tr][tc] = col * promoted;
            }

            selRow = -1; selCol = -1; hints.clear();
            turn   = -turn;
            repaint();

            checkGameOver();

            // Trigger AI if needed
            if (!gameOver && vsAI && turn == BLACK) {
                SwingUtilities.invokeLater(this::doAiMove);
            }
        }

        private int promptPromotion(int col) {
            String[] opts = {"Queen", "Rook", "Bishop", "Knight"};
            String pick = (String) JOptionPane.showInputDialog(
                this, "Promote pawn:", "Promotion",
                JOptionPane.PLAIN_MESSAGE, null, opts, "Queen");
            if (pick == null) return QUEEN;
            switch (pick) {
                case "Rook":   return ROOK;
                case "Bishop": return BISHOP;
                case "Knight": return KNIGHT;
                default:       return QUEEN;
            }
        }

        // ── Check / stalemate / game-over ─────────────────────────────────────
        private void checkGameOver() {
            List<int[]> allMoves = generateAllLegalMoves(board, turn, enPassantCol,
                    whiteKingMoved, blackKingMoved,
                    whiteRookAMoved, whiteRookHMoved,
                    blackRookAMoved, blackRookHMoved);
            if (allMoves.isEmpty()) {
                gameOver = true;
                repaint();
                String msg;
                if (isInCheck(board, turn)) {
                    String winner = (turn == WHITE) ? "Black" : "White";
                    msg = "Checkmate! " + winner + " wins!";
                } else {
                    msg = "Stalemate – it's a draw!";
                }
                SwingUtilities.invokeLater(() -> {
                    int opt = JOptionPane.showConfirmDialog(this, msg + "\n\nPlay again?",
                            "Game Over", JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) { resetBoard(); repaint(); }
                    else parent.returnToMenu();
                });
            }
        }

        // ── AI (minimax + alpha-beta) ──────────────────────────────────────────
        private void doAiMove() {
            int[] best = findBestMove(board, AI_DEPTH, enPassantCol,
                    whiteKingMoved, blackKingMoved,
                    whiteRookAMoved, whiteRookHMoved,
                    blackRookAMoved, blackRookHMoved);
            if (best != null) executeMove(best[0], best[1], best[2], best[3]);
        }

        private int[] findBestMove(int[][] b, int depth, int epCol,
                                    boolean wkm, boolean bkm,
                                    boolean wram, boolean wrhm,
                                    boolean bram, boolean brhm) {
            int   bestScore = Integer.MAX_VALUE;
            int[] bestMove  = null;
            List<int[]> moves = generateAllLegalMoves(b, BLACK, epCol, wkm, bkm, wram, wrhm, bram, brhm);
            for (int[] m : moves) {
                int[][] copy = copyBoard(b);
                MoveResult mr = applyMoveToBoard(copy, m[0],m[1],m[2],m[3], BLACK, epCol, wkm,bkm,wram,wrhm,bram,brhm);
                int score = minimax(copy, depth-1, Integer.MIN_VALUE, Integer.MAX_VALUE, true,
                        mr.epCol, mr.wkm, mr.bkm, mr.wram, mr.wrhm, mr.bram, mr.brhm);
                if (score < bestScore) { bestScore = score; bestMove = m; }
            }
            return bestMove;
        }

        private int minimax(int[][] b, int depth, int alpha, int beta, boolean maximising,
                             int epCol, boolean wkm, boolean bkm,
                             boolean wram, boolean wrhm, boolean bram, boolean brhm) {
            if (depth == 0) return evaluate(b);
            int side = maximising ? WHITE : BLACK;
            List<int[]> moves = generateAllLegalMoves(b, side, epCol, wkm,bkm,wram,wrhm,bram,brhm);
            if (moves.isEmpty()) return isInCheck(b, side) ? (maximising ? -30000 : 30000) : 0;

            if (maximising) {
                int best = Integer.MIN_VALUE;
                for (int[] m : moves) {
                    int[][] copy = copyBoard(b);
                    MoveResult mr = applyMoveToBoard(copy,m[0],m[1],m[2],m[3],WHITE,epCol,wkm,bkm,wram,wrhm,bram,brhm);
                    best = Math.max(best, minimax(copy,depth-1,alpha,beta,false,mr.epCol,mr.wkm,mr.bkm,mr.wram,mr.wrhm,mr.bram,mr.brhm));
                    alpha = Math.max(alpha, best);
                    if (beta <= alpha) break;
                }
                return best;
            } else {
                int best = Integer.MAX_VALUE;
                for (int[] m : moves) {
                    int[][] copy = copyBoard(b);
                    MoveResult mr = applyMoveToBoard(copy,m[0],m[1],m[2],m[3],BLACK,epCol,wkm,bkm,wram,wrhm,bram,brhm);
                    best = Math.min(best, minimax(copy,depth-1,alpha,beta,true,mr.epCol,mr.wkm,mr.bkm,mr.wram,mr.wrhm,mr.bram,mr.brhm));
                    beta = Math.min(beta, best);
                    if (beta <= alpha) break;
                }
                return best;
            }
        }

        /** Static evaluation: positive = good for white, negative = good for black. */
        private int evaluate(int[][] b) {
            int score = 0;
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    int p = b[r][c];
                    if (p == EMPTY) continue;
                    int col  = color(p);
                    int type = Math.abs(p);
                    int pstRow = (col == WHITE) ? r : (7 - r); // flip for black
                    score += col * (pieceValue(type) + pst(type, pstRow, c));
                }
            }
            return score;
        }

        private int pieceValue(int type) {
            switch (type) {
                case PAWN:   return 100;
                case KNIGHT: return 320;
                case BISHOP: return 330;
                case ROOK:   return 500;
                case QUEEN:  return 900;
                case KING:   return 20000;
                default:     return 0;
            }
        }

        private int pst(int type, int r, int c) {
            switch (type) {
                case PAWN:   return PST_PAWN[r][c];
                case KNIGHT: return PST_KNIGHT[r][c];
                case BISHOP: return PST_BISHOP[r][c];
                case ROOK:   return PST_ROOK[r][c];
                case QUEEN:  return PST_QUEEN[r][c];
                case KING:   return PST_KING_MID[r][c];
                default:     return 0;
            }
        }

        // ── Move application (for simulation / AI) ────────────────────────────
        static class MoveResult {
            int epCol; boolean wkm,bkm,wram,wrhm,bram,brhm;
        }

        private MoveResult applyMoveToBoard(int[][] b, int fr, int fc, int tr, int tc, int col,
                                             int epCol,
                                             boolean wkm, boolean bkm,
                                             boolean wram, boolean wrhm,
                                             boolean bram, boolean brhm) {
            MoveResult mr = new MoveResult();
            mr.epCol=epCol; mr.wkm=wkm; mr.bkm=bkm;
            mr.wram=wram; mr.wrhm=wrhm; mr.bram=bram; mr.brhm=brhm;

            int piece = b[fr][fc];
            int type  = Math.abs(piece);

            // En passant capture
            if (type == PAWN && fc == epCol && Math.abs(fr-tr)==1 && Math.abs(fc-tc)==1 && b[tr][tc]==EMPTY) {
                b[fr][tc] = EMPTY;
            }
            // New en-passant target
            mr.epCol = (type == PAWN && Math.abs(tr-fr)==2) ? fc : -1;

            // Castling – move rook
            if (type == KING && Math.abs(tc - fc) == 2) {
                if (tc==6) { b[fr][5]=b[fr][7]; b[fr][7]=EMPTY; }
                else       { b[fr][3]=b[fr][0]; b[fr][0]=EMPTY; }
            }

            b[tr][tc] = piece;
            b[fr][fc] = EMPTY;

            // Auto-promote pawns to queen in simulation
            if (type == PAWN && (tr==0 || tr==7)) b[tr][tc] = col * QUEEN;

            // Track moved flags
            if (type==KING) { if (col==WHITE) mr.wkm=true; else mr.bkm=true; }
            if (type==ROOK) {
                if (col==WHITE) { if (fc==0) mr.wram=true; if (fc==7) mr.wrhm=true; }
                else            { if (fc==0) mr.bram=true; if (fc==7) mr.brhm=true; }
            }
            return mr;
        }

        // ── Move generation ───────────────────────────────────────────────────

        /** Returns ALL legal moves for a side as {fr,fc,tr,tc} arrays. */
        static List<int[]> generateAllLegalMoves(int[][] b, int side, int epCol,
                boolean wkm, boolean bkm, boolean wram, boolean wrhm, boolean bram, boolean brhm) {
            List<int[]> all = new ArrayList<>();
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    if (b[r][c] != EMPTY && color(b[r][c]) == side)
                        all.addAll(generateLegalMoves(b,r,c,side,epCol,wkm,bkm,wram,wrhm,bram,brhm));
            return all;
        }

        /** Legal moves for piece at (row,col) – filters moves that leave king in check. */
static List<int[]> generateLegalMoves(int[][] b, int row, int col, int side, int epCol,
                boolean wkm, boolean bkm, boolean wram, boolean wrhm, boolean bram, boolean brhm) {
    // Filter pseudo-moves: keep only those that don't leave own king in check
            List<int[]> pseudo = generatePseudoMoves(b, row, col, epCol, wkm, bkm, wram, wrhm, bram, brhm);
            List<int[]> legal = new ArrayList<>();
            for (int[] move : pseudo) {
                int[][] tempBoard = copyBoard(b);
                applyMoveSimple(tempBoard, row, col, move[2], move[3], epCol);
                if (!isInCheck(tempBoard, side)) {
                    legal.add(move);
                }
            }
            System.out.println("Pseudo: " + pseudo.size() + ", Legal: " + legal.size() + " for (" + row + "," + col + ")");
            return legal;
        }

        private static void applyMoveSimple(int[][] b, int fr, int fc, int tr, int tc, int epCol) {
            int piece = b[fr][fc];
            int type  = Math.abs(piece);
            int col   = color(piece);
            if (type==PAWN && fc==epCol && Math.abs(fr-tr)==1 && Math.abs(fc-tc)==1 && b[tr][tc]==EMPTY)
                b[fr][tc] = EMPTY;
            if (type==KING && Math.abs(tc-fc)==2) {
                if (tc==6) { b[fr][5]=b[fr][7]; b[fr][7]=EMPTY; }
                else       { b[fr][3]=b[fr][0]; b[fr][0]=EMPTY; }
            }
            b[tr][tc] = piece;
            b[fr][fc] = EMPTY;
            if (type==PAWN && (tr==0||tr==7)) b[tr][tc] = col*QUEEN;
        }

        /** Pseudo-legal moves (may leave king in check). */
        private static List<int[]> generatePseudoMoves(int[][] b, int row, int col, int epCol,
                boolean wkm, boolean bkm, boolean wram, boolean wrhm, boolean bram, boolean brhm) {
            List<int[]> moves = new ArrayList<>();
            int piece = b[row][col];
            int type  = Math.abs(piece);
            int side  = color(piece);

            switch (type) {
                case PAWN:   addPawnMoves  (b,row,col,side,epCol,moves); break;
                case KNIGHT: addKnightMoves(b,row,col,side,moves); break;
                case BISHOP: addSliding    (b,row,col,side,moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case ROOK:   addSliding    (b,row,col,side,moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                case QUEEN:  addSliding    (b,row,col,side,moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case KING:   addKingMoves  (b,row,col,side,moves,wkm,bkm,wram,wrhm,bram,brhm); break;
            }
            return moves;
        }

        private static void addPawnMoves(int[][] b, int row, int col, int side, int epCol, List<int[]> moves) {
            int dir      = (side == WHITE) ? -1 : 1;
            int startRow = (side == WHITE) ? 6 : 1;
            int nr = row + dir;

            if (inBounds(nr, col) && b[nr][col] == EMPTY) {
                moves.add(new int[]{row,col,nr,col});
                if (row == startRow && b[nr+dir][col] == EMPTY)
                    moves.add(new int[]{row,col,nr+dir,col});
            }
            for (int dc : new int[]{-1,1}) {
                int nc = col + dc;
                if (!inBounds(nr, nc)) continue;
                // Normal capture
                if (b[nr][nc] != EMPTY && color(b[nr][nc]) != side)
                    moves.add(new int[]{row,col,nr,nc});
                // En passant
                if (nc == epCol && b[nr][nc] == EMPTY && b[row][nc] != EMPTY && color(b[row][nc]) != side
                        && Math.abs(b[row][nc]) == PAWN)
                    moves.add(new int[]{row,col,nr,nc});
            }
        }

        private static void addKnightMoves(int[][] b, int row, int col, int side, List<int[]> moves) {
            for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
                int nr=row+d[0], nc=col+d[1];
                if (inBounds(nr,nc) && (b[nr][nc]==EMPTY || color(b[nr][nc])!=side))
                    moves.add(new int[]{row,col,nr,nc});
            }
        }

        private static void addSliding(int[][] b, int row, int col, int side, List<int[]> moves, int[][] dirs) {
            for (int[] d : dirs) {
                int nr=row+d[0], nc=col+d[1];
                while (inBounds(nr,nc)) {
                    if (b[nr][nc] == EMPTY)  { moves.add(new int[]{row,col,nr,nc}); }
                    else { if (color(b[nr][nc])!=side) moves.add(new int[]{row,col,nr,nc}); break; }
                    nr+=d[0]; nc+=d[1];
                }
            }
        }

        private static void addKingMoves(int[][] b, int row, int col, int side, List<int[]> moves,
                boolean wkm, boolean bkm, boolean wram, boolean wrhm, boolean bram, boolean brhm) {
            for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
                int nr=row+d[0], nc=col+d[1];
                if (inBounds(nr,nc) && (b[nr][nc]==EMPTY || color(b[nr][nc])!=side))
                    moves.add(new int[]{row,col,nr,nc});
            }
            // Castling
            boolean kingMoved = (side==WHITE) ? wkm : bkm;
            boolean rookAMov  = (side==WHITE) ? wram : bram;
            boolean rookHMov  = (side==WHITE) ? wrhm : brhm;
            if (!kingMoved && !isInCheck(b, side)) {
                // Kingside: col 4→6, rook h
                if (!rookHMov && b[row][5]==EMPTY && b[row][6]==EMPTY
                        && !isAttacked(b,row,5,-side) && !isAttacked(b,row,6,-side))
                    moves.add(new int[]{row,col,row,6});
                // Queenside: col 4→2, rook a
                if (!rookAMov && b[row][1]==EMPTY && b[row][2]==EMPTY && b[row][3]==EMPTY
                        && !isAttacked(b,row,3,-side) && !isAttacked(b,row,2,-side))
                    moves.add(new int[]{row,col,row,2});
            }
        }

        // ── Check / attack detection ──────────────────────────────────────────
        static boolean isInCheck(int[][] b, int side) {
            for (int r=0; r<8; r++)
                for (int c=0; c<8; c++)
                    if (b[r][c] == side*KING)
                        return isAttacked(b, r, c, -side);
            return false;
        }

        static boolean isAttacked(int[][] b, int row, int col, int bySide) {
            // Knights
            for (int[] d : new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}) {
                int r=row+d[0], c=col+d[1];
                if (inBounds(r,c) && b[r][c]==bySide*KNIGHT) return true;
            }
            // Straight (rook/queen)
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
                if (slidingAttack(b,row,col,bySide,d,ROOK)) return true;
            // Diagonal (bishop/queen)
            for (int[] d : new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}})
                if (slidingAttack(b,row,col,bySide,d,BISHOP)) return true;
            // King
            for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
                int r=row+d[0], c=col+d[1];
                if (inBounds(r,c) && b[r][c]==bySide*KING) return true;
            }
            // Pawns
            int pDir = (bySide==WHITE) ? 1 : -1;
            for (int dc : new int[]{-1,1}) {
                int r=row+pDir, c=col+dc;
                if (inBounds(r,c) && b[r][c]==bySide*PAWN) return true;
            }
            return false;
        }

        private static boolean slidingAttack(int[][] b, int row, int col, int bySide, int[] dir, int sliderType) {
            int r=row+dir[0], c=col+dir[1];
            while (inBounds(r,c)) {
                int p = b[r][c];
                if (p != EMPTY) {
                    return color(p)==bySide && (Math.abs(p)==sliderType || Math.abs(p)==QUEEN);
                }
                r+=dir[0]; c+=dir[1];
            }
            return false;
        }

        // ── Helpers ───────────────────────────────────────────────────────────
        static int color(int piece) { return piece > 0 ? WHITE : BLACK; }
        static boolean inBounds(int r, int c) { return r>=0&&r<8&&c>=0&&c<8; }
        static int[][] copyBoard(int[][] b) {
            int[][] copy = new int[8][8];
            for (int r=0; r<8; r++) copy[r] = Arrays.copyOf(b[r], 8);
            return copy;
        }

        // ── Rendering ─────────────────────────────────────────────────────────
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            int ts      = tileSize();
            int boardPx = 8 * ts;
            int offsetX = (getWidth()  - boardPx) / 2;
            int offsetY = (getHeight() - STATUS_H - BTN_H - boardPx) / 2;
            offsetY = Math.max(offsetY, 0);

            drawBoard   (g2, ts, offsetX, offsetY);
            drawLastMove(g2, ts, offsetX, offsetY);
            drawHints   (g2, ts, offsetX, offsetY);
            drawSelected(g2, ts, offsetX, offsetY);
            drawCheckGlow(g2, ts, offsetX, offsetY);
            drawPieces  (g2, ts, offsetX, offsetY);
            drawCoords  (g2, ts, offsetX, offsetY);
            drawStatus  (g2, ts, offsetX, offsetY);
            drawButtons (g2, ts, offsetX, offsetY);
        }

        private void drawBoard(Graphics2D g, int ts, int ox, int oy) {
            for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
                g.setColor((r+c)%2==0 ? COL_LIGHT : COL_DARK);
                g.fillRect(ox+c*ts, oy+r*ts, ts, ts);
            }
        }

        private void drawLastMove(Graphics2D g, int ts, int ox, int oy) {
            if (lastFR<0) return;
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f);
            g.setComposite(ac);
            g.setColor(COL_LASTFR);
            g.fillRect(ox+lastFC*ts, oy+lastFR*ts, ts, ts);
            g.fillRect(ox+lastTC*ts, oy+lastTR*ts, ts, ts);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawHints(Graphics2D g, int ts, int ox, int oy) {
            for (int[] h : hints) {
                // CHANGED: Use h[2] and h[3] for the destination row/col
                int r=h[2], c=h[3]; 
                
                if (board[r][c] != EMPTY) {
                    // Capture ring
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                    g.setColor(COL_HINT);
                    g.fillRect(ox+c*ts, oy+r*ts, ts, ts);
                } else {
                    // Movement dot
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                    g.setColor(new Color(0x86A666));
                    int ds = ts/3, dx=ox+c*ts+(ts-ds)/2, dy=oy+r*ts+(ts-ds)/2;
                    g.fillOval(dx, dy, ds, ds);
                }
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
        }

        private void drawSelected(Graphics2D g, int ts, int ox, int oy) {
            if (selRow<0) return;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g.setColor(COL_SEL);
            g.fillRect(ox+selCol*ts, oy+selRow*ts, ts, ts);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        private void drawCheckGlow(Graphics2D g, int ts, int ox, int oy) {
            // Highlight king square if in check
            for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
                if (board[r][c]!=EMPTY && Math.abs(board[r][c])==KING) {
                    int side = color(board[r][c]);
                    if (isInCheck(board, side)) {
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
                        g.setColor(COL_CHECK);
                        g.fillRect(ox+c*ts, oy+r*ts, ts, ts);
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                    }
                }
            }
        }

        private void drawPieces(Graphics2D g, int ts, int ox, int oy) {
            int pad = Math.max(4, ts/14);
            for (int r=0; r<8; r++) for (int c=0; c<8; c++) {
                int p = board[r][c];
                if (p==EMPTY) continue;
                BufferedImage img = images.get(p);
                if (img != null) {
                    g.drawImage(img, ox+c*ts+pad, oy+r*ts+pad, ts-pad*2, ts-pad*2, null);
                } else {
                    drawSymbol(g, p, r, c, ts, ox, oy);
                }
            }
        }

        private void drawSymbol(Graphics2D g, int piece, int row, int col, int ts, int ox, int oy) {
            String sym = unicodeSymbol(piece);
            Font f = new Font("Segoe UI Symbol", Font.PLAIN, ts*6/10);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int x = ox+col*ts+(ts-fm.stringWidth(sym))/2;
            int y = oy+row*ts+(ts+fm.getAscent()-fm.getDescent())/2-1;
            // Shadow
            g.setColor(color(piece)==WHITE ? new Color(60,40,20) : new Color(200,190,170));
            g.drawString(sym, x+1, y+1);
            g.setColor(color(piece)==WHITE ? Color.WHITE : new Color(15,15,15));
            g.drawString(sym, x, y);
        }

        private void drawCoords(Graphics2D g, int ts, int ox, int oy) {
            g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, ts/8)));
            FontMetrics fm = g.getFontMetrics();
            for (int i=0; i<8; i++) {
                // File (a-h) – bottom-left of each square on the last rank
                g.setColor(i%2==0 ? COL_DARK : COL_LIGHT);
                g.drawString(String.valueOf((char)('a'+i)), ox+i*ts+3, oy+8*ts-3);
                // Rank (8-1) – top-left of each square on the first file
                g.setColor(i%2==0 ? COL_LIGHT : COL_DARK);
                g.drawString(String.valueOf(8-i), ox+3, oy+i*ts+fm.getAscent()+2);
            }
        }

        private void drawStatus(Graphics2D g, int ts, int ox, int oy) {
            int boardBottom = oy + 8*ts;
            g.setColor(COL_BG);
            g.fillRect(0, boardBottom, getWidth(), STATUS_H);

            String text;
            if (gameOver) {
                text = "Game Over";
            } else if (isInCheck(board, turn)) {
                text = (turn==WHITE ? "White" : "Black") + " is in CHECK!";
            } else {
                String who = (turn==WHITE ? "White" : "Black");
                String mode = vsAI && turn==BLACK ? " (AI thinking…)" : "'s turn";
                text = who + mode;
            }
            g.setFont(new Font("Serif", Font.BOLD, Math.max(14, ts/5)));
            g.setColor(COL_STATUS);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, ox + (8*ts - fm.stringWidth(text))/2,
                         boardBottom + (STATUS_H + fm.getAscent() - fm.getDescent())/2);
        }

        // Draws a "Menu" button below the status bar
        private JButton menuBtn;
        private void drawButtons(Graphics2D g, int ts, int ox, int oy) {
            // We use a lazy-added JButton so it participates in real Swing layout
            if (menuBtn == null) {
                menuBtn = new JButton("⬅  Main Menu");
                menuBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
                menuBtn.setBackground(new Color(0x4A4A4A));
                menuBtn.setForeground(Color.WHITE);
                menuBtn.setFocusPainted(false);
                menuBtn.setBorderPainted(false);
                menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                menuBtn.addActionListener(e -> parent.returnToMenu());
                setLayout(null);
                add(menuBtn);
            }
            int bw=150, bh=32;
            int bx = ox + (8*ts - bw)/2;
            int by = oy + 8*ts + STATUS_H + (BTN_H - bh)/2;
            menuBtn.setBounds(bx, by, bw, bh);
        }

        private String unicodeSymbol(int piece) {
            int type = Math.abs(piece);
            if (color(piece)==WHITE) {
                switch(type){case KING:return"♔";case QUEEN:return"♕";case ROOK:return"♖";case BISHOP:return"♗";case KNIGHT:return"♘";case PAWN:return"♙";}
            } else {
                switch(type){case KING:return"♚";case QUEEN:return"♛";case ROOK:return"♜";case BISHOP:return"♝";case KNIGHT:return"♞";case PAWN:return"♟";}
            }
            return"?";
        }
    }
}