import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * ChessGame.java  –  Complete single-file Java Swing chess game.
 *
 * HOW TO RUN:
 *   javac ChessGame.java
 *   java  ChessGame
 *
 * Place your piece images in a folder called "images/" next to ChessGame.java:
 *   whiteking.png  whitequeen.png  whiterook.png  whitebishop.png
 *   whiteknight.png  whitepawn.png  (same pattern for "black")
 *
 * Features:
 *   - 2-Player local OR vs AI (minimax + alpha-beta, depth 3)
 *   - Castling, En Passant, Pawn Promotion
 *   - Check / Checkmate / Stalemate
 *   - Threefold-repetition draw detection
 *   - Per-side countdown clock (configurable at menu)
 *   - Move history sidebar (algebraic-style)
 *   - Toggleable evaluation bar
 *   - Legal-move dots + capture corner-rings
 *   - Fully scalable / resizable window
 */
public class ChessGame extends JFrame {

    // =========================================================================
    //  PIECE / COLOUR CONSTANTS
    // =========================================================================
    static final int EMPTY  = 0;
    static final int PAWN   = 1;
    static final int KNIGHT = 2;
    static final int BISHOP = 3;
    static final int ROOK   = 4;
    static final int QUEEN  = 5;
    static final int KING   = 6;

    static final int WHITE =  1;
    static final int BLACK = -1;
    static final int MIN_TILE = 50;

    // =========================================================================
    //  ENTRY POINT
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGame::new);
    }

    // =========================================================================
    //  FRAME FIELDS
    // =========================================================================
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);
    private BoardPanel boardPanel;

    // =========================================================================
    //  CONSTRUCTOR
    // =========================================================================
    public ChessGame() {
        super("Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(640, 600));
        setSize(1060, 780);
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
        gbc.fill  = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel title = new JLabel("♟  Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 56));
        title.setForeground(new Color(0xF0D9B5));
        gbc.gridy = 0; gbc.insets = new Insets(50, 80, 10, 80);
        panel.add(title, gbc);

        JLabel sub = new JLabel("Choose a game mode", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 17));
        sub.setForeground(new Color(0x9A8A7A));
        gbc.gridy = 1; gbc.insets = new Insets(0, 80, 30, 80);
        panel.add(sub, gbc);

        JLabel timeLabel = new JLabel("Clock per player:", SwingConstants.CENTER);
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        timeLabel.setForeground(new Color(0xC0B0A0));
        gbc.gridy = 2; gbc.insets = new Insets(0, 80, 4, 80);
        panel.add(timeLabel, gbc);

        String[] timeOpts = {"No clock", "1 min", "3 min", "5 min", "10 min", "15 min"};
        JComboBox<String> timePicker = new JComboBox<>(timeOpts);
        timePicker.setSelectedIndex(2);
        timePicker.setFont(new Font("SansSerif", Font.PLAIN, 15));
        timePicker.setBackground(new Color(0x3A3A3A));
        timePicker.setForeground(Color.WHITE);
        gbc.gridy = 3; gbc.insets = new Insets(0, 120, 24, 120);
        panel.add(timePicker, gbc);

        JButton btnFriend = menuButton("👥  Play vs Friend", new Color(0x3D6B4F));
        gbc.gridy = 4; gbc.insets = new Insets(8, 100, 8, 100);
        panel.add(btnFriend, gbc);

        JButton btnAI = menuButton("🤖  Play vs AI", new Color(0x6B3D3D));
        gbc.gridy = 5;
        panel.add(btnAI, gbc);

        btnFriend.addActionListener(e -> startGame(false, parseTime(timePicker)));
        btnAI    .addActionListener(e -> startGame(true,  parseTime(timePicker)));
        return panel;
    }

    private int parseTime(JComboBox<String> cb) {
        String s = (String) cb.getSelectedItem();
        if (s == null || s.equals("No clock")) return 0;
        return Integer.parseInt(s.split(" ")[0]) * 60;
    }

    private JButton menuButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 19));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(260, 56));
        Color hover = bg.brighter();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited (MouseEvent e) { btn.setBackground(bg);    }
        });
        return btn;
    }

    // =========================================================================
    //  START / RETURN
    // =========================================================================
    private void startGame(boolean ai, int secondsPerSide) {
        if (boardPanel != null) { boardPanel.stopClocks(); cardPanel.remove(boardPanel); }
        boardPanel = new BoardPanel(ai, secondsPerSide, this);
        cardPanel.add(boardPanel, "GAME");
        cardLayout.show(cardPanel, "GAME");
        boardPanel.requestFocusInWindow();
    }

    void returnToMenu() {
        if (boardPanel != null) boardPanel.stopClocks();
        cardLayout.show(cardPanel, "MENU");
    }

    // =========================================================================
    // =========================================================================
    //  BOARD PANEL
    // =========================================================================
    // =========================================================================
    static class BoardPanel extends JPanel {

        // ── Layout ────────────────────────────────────────────────────────────
        private static final int SIDEBAR_W = 210;
        private static final int STATUS_H  = 42;
        private static final int CLOCK_H   = 38;

        // ── Colours ───────────────────────────────────────────────────────────
        private static final Color COL_LIGHT  = new Color(0xF0D9B5);
        private static final Color COL_DARK   = new Color(0xB58863);
        private static final Color COL_SEL    = new Color(0x7FC97F);
        private static final Color COL_HINT   = new Color(0xCDD26A);
        private static final Color COL_CHECK  = new Color(0xE74C3C);
        private static final Color COL_LAST   = new Color(0xF6F669);
        private static final Color COL_BG     = new Color(0x2C2C2C);
        private static final Color COL_PANEL  = new Color(0x1E1E1E);
        private static final Color COL_TEXT   = new Color(0xF0D9B5);
        private static final Color COL_SUB    = new Color(0x9A8878);

        // ── Board state ───────────────────────────────────────────────────────
        private final int[][] board = new int[8][8];
        private int turn = WHITE;
        private boolean whiteKingMoved=false, blackKingMoved=false;
        private boolean whiteRookAMoved=false, whiteRookHMoved=false;
        private boolean blackRookAMoved=false, blackRookHMoved=false;
        private int enPassantCol = -1;

        // ── Selection / highlights ────────────────────────────────────────────
        private int selRow=-1, selCol=-1;
        private List<int[]> hints = new ArrayList<>();
        private int lastFR=-1, lastFC=-1, lastTR=-1, lastTC=-1;
        private boolean gameOver = false;

        // ── Threefold repetition ──────────────────────────────────────────────
        private final Map<String,Integer> positionCounts = new HashMap<>();

        // ── Move history ──────────────────────────────────────────────────────
        private final List<String> moveHistory = new ArrayList<>();
        private int moveNumber = 1;

        // ── Clocks ────────────────────────────────────────────────────────────
        private int whiteSeconds, blackSeconds;
        private final int initialSeconds;
        private final boolean clockEnabled;
        private javax.swing.Timer clockTimer;

        // ── Eval bar toggle ───────────────────────────────────────────────────
        private boolean showEval = true;

        // ── AI ────────────────────────────────────────────────────────────────
        private final boolean vsAI;
        private final ChessGame parent;
        private static final int AI_DEPTH = 3;

        // ── Images ────────────────────────────────────────────────────────────
        private final Map<Integer,BufferedImage> images = new HashMap<>();

        // ── Piece-square tables (from white's perspective) ────────────────────
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
        private static final int[][] PST_KING = {
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-30,-40,-40,-50,-50,-40,-40,-30},
            {-20,-30,-30,-40,-40,-30,-30,-20},
            {-10,-20,-20,-20,-20,-20,-20,-10},
            { 20, 20,  0,  0,  0,  0, 20, 20},
            { 20, 30, 10,  0,  0, 10, 30, 20}
        };

        // ── Sidebar Swing components ──────────────────────────────────────────
        private JTextArea  moveListArea;
        private JPanel     evalBarPanel;
        private JButton    evalToggleBtn;

        // =====================================================================
        //  CONSTRUCTOR
        // =====================================================================
        BoardPanel(boolean vsAI, int secondsPerSide, ChessGame parent) {
            this.vsAI           = vsAI;
            this.parent         = parent;
            this.initialSeconds = secondsPerSide;
            this.clockEnabled   = secondsPerSide > 0;
            this.whiteSeconds   = secondsPerSide;
            this.blackSeconds   = secondsPerSide;

            setLayout(new BorderLayout(0, 0));
            setBackground(COL_BG);
            loadImages();

            add(new BoardCanvas(), BorderLayout.CENTER);
            add(buildSidebar(),    BorderLayout.EAST);

            resetBoard();
            startClockTimer();
        }

        // =====================================================================
        //  SIDEBAR
        // =====================================================================
        private JPanel buildSidebar() {
            JPanel side = new JPanel();
            side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
            side.setBackground(COL_PANEL);
            side.setPreferredSize(new Dimension(SIDEBAR_W, 0));
            side.setBorder(new EmptyBorder(12, 10, 12, 10));

            // ── Eval bar ─────────────────────────────────────────────────────
            evalBarPanel = new EvalBarPanel();
            evalBarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            evalBarPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
            evalBarPanel.setPreferredSize(new Dimension(SIDEBAR_W - 20, 240));
            side.add(evalBarPanel);
            side.add(Box.createVerticalStrut(6));

            // ── Eval toggle button ────────────────────────────────────────────
            evalToggleBtn = sideButton("Hide Eval Bar");
            evalToggleBtn.addActionListener(e -> {
                showEval = !showEval;
                evalToggleBtn.setText(showEval ? "Hide Eval Bar" : "Show Eval Bar");
                evalBarPanel.setVisible(showEval);
                side.revalidate(); side.repaint();
            });
            side.add(evalToggleBtn);
            side.add(Box.createVerticalStrut(14));

            // ── Clocks ────────────────────────────────────────────────────────
            if (clockEnabled) {
                side.add(sectionLabel("⏱  Clocks"));
                side.add(Box.createVerticalStrut(5));
                side.add(clockRow("⬛ Black", blackSeconds, false));
                side.add(Box.createVerticalStrut(3));
                side.add(clockRow("⬜ White", whiteSeconds, true));
                side.add(Box.createVerticalStrut(14));
            }

            // ── Move list ─────────────────────────────────────────────────────
            side.add(sectionLabel("📋  Move History"));
            side.add(Box.createVerticalStrut(5));

            moveListArea = new JTextArea();
            moveListArea.setEditable(false);
            moveListArea.setBackground(new Color(0x161616));
            moveListArea.setForeground(new Color(0xD8C8B8));
            moveListArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            moveListArea.setLineWrap(false);
            moveListArea.setBorder(new EmptyBorder(6, 6, 6, 6));
            JScrollPane scroll = new JScrollPane(moveListArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
            scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 99999));
            side.add(scroll);
            side.add(Box.createVerticalStrut(10));

            // ── Menu button ───────────────────────────────────────────────────
            JButton menuBtn = sideButton("⬅  Main Menu");
            menuBtn.addActionListener(e -> parent.returnToMenu());
            side.add(menuBtn);

            return side;
        }

        private JLabel sectionLabel(String text) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setForeground(COL_SUB);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            return lbl;
        }

        private JLabel clockRow(String label, int secs, boolean isWhite) {
            JLabel lbl = new JLabel(label + "  " + formatTime(secs), SwingConstants.CENTER);
            lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
            lbl.setForeground(COL_TEXT);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            // We tag the label so we can update it by name later
            lbl.setName(isWhite ? "whiteClock" : "blackClock");
            return lbl;
        }

        private JButton sideButton(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("SansSerif", Font.BOLD, 12));
            btn.setBackground(new Color(0x3A3A3A));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            Color hov = new Color(0x555555);
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.setBackground(hov); }
                public void mouseExited (MouseEvent e) { btn.setBackground(new Color(0x3A3A3A)); }
            });
            return btn;
        }

        // =====================================================================
        //  EVAL BAR  (custom painted panel)
        // =====================================================================
        class EvalBarPanel extends JPanel {
            EvalBarPanel() {
                setBackground(COL_PANEL);
                setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(), h=getHeight();
                int score = evaluate(board);
                int clamped = Math.max(-1500, Math.min(1500, score));
                double frac = (clamped + 1500.0) / 3000.0;
                int whiteH = (int)(h * frac);
                // Black section (top)
                g2.setColor(new Color(0x1A1A1A));
                g2.fillRect(0, 0, w, h - whiteH);
                // White section (bottom)
                g2.setColor(new Color(0xEEEEEE));
                g2.fillRect(0, h - whiteH, w, whiteH);
                // Centre line
                g2.setColor(new Color(0x666666));
                g2.drawLine(0, h/2, w, h/2);
                // Score text
                String lbl;
                if (Math.abs(score) > 1400) lbl = score > 0 ? "+M" : "-M";
                else {
                    double p = score / 100.0;
                    lbl = (p >= 0 ? "+" : "") + String.format("%.1f", p);
                }
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(lbl)) / 2;
                int ty = h - whiteH - 4;
                if (ty < 14) ty = 14;
                if (ty > h - 4) ty = h - 4;
                g2.setColor(score >= 0 ? Color.BLACK : Color.WHITE);
                g2.drawString(lbl, tx, ty);
                // Labels
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(new Color(0xAAAAAA));
                g2.drawString("White", 3, h - 5);
                g2.drawString("Black", 3, 12);
            }
        }

        // =====================================================================
        //  BOARD CANVAS  (inner JPanel that draws the actual board)
        // =====================================================================
        class BoardCanvas extends JPanel {
            BoardCanvas() {
                setBackground(COL_BG);
                addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) { onMouseClick(e); }
                });
                addComponentListener(new ComponentAdapter() {
                    @Override public void componentResized(ComponentEvent e) { repaint(); }
                });
            }

            // ── Tile size & board offset ──────────────────────────────────────
            private int tileSize() {
                int avail = Math.min(getWidth(), getHeight() - STATUS_H - CLOCK_H * 2);
                return Math.max(avail / 8, MIN_TILE);
            }

            private int[] offset(int ts) {
                int ox = (getWidth()  - 8 * ts) / 2;
                int oy = CLOCK_H + Math.max(0, (getHeight() - STATUS_H - CLOCK_H * 2 - 8 * ts) / 2);
                return new int[]{ox, oy};
            }

            // ── Mouse click → board square (precise integer arithmetic) ───────
            private void onMouseClick(MouseEvent e) {
                if (gameOver) return;
                if (vsAI && turn == BLACK) return;
                int ts = tileSize();
                int[] off = offset(ts);
                int px = e.getX() - off[0];
                int py = e.getY() - off[1];
                if (px < 0 || py < 0 || px >= 8 * ts || py >= 8 * ts) {
                    selRow=-1; selCol=-1; hints.clear(); repaint(); return;
                }
                handleSquareClick(py / ts, px / ts);
            }

            // ── paintComponent ───────────────────────────────────────────────
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

                int ts = tileSize();
                int[] off = offset(ts);
                int ox = off[0], oy = off[1];

                drawBoard    (g2, ts, ox, oy);
                drawLastMove (g2, ts, ox, oy);
                drawHints    (g2, ts, ox, oy);
                drawSelected (g2, ts, ox, oy);
                drawCheckGlow(g2, ts, ox, oy);
                drawPieces   (g2, ts, ox, oy);
                drawCoords   (g2, ts, ox, oy);
                drawClocks   (g2, ts, ox, oy);
                drawStatus   (g2, ts, ox, oy);
            }

            // ── Board squares ─────────────────────────────────────────────────
            private void drawBoard(Graphics2D g, int ts, int ox, int oy) {
                for (int r=0; r<8; r++)
                    for (int c=0; c<8; c++) {
                        g.setColor((r+c)%2==0 ? COL_LIGHT : COL_DARK);
                        g.fillRect(ox+c*ts, oy+r*ts, ts, ts);
                    }
            }

            // ── Last-move highlight ───────────────────────────────────────────
            private void drawLastMove(Graphics2D g, int ts, int ox, int oy) {
                if (lastFR < 0) return;
                alpha(g, 0.42f);
                g.setColor(COL_LAST);
                g.fillRect(ox+lastFC*ts, oy+lastFR*ts, ts, ts);
                g.fillRect(ox+lastTC*ts, oy+lastTR*ts, ts, ts);
                alpha(g, 1f);
            }

            // ── Move hints: dot for empty squares, corner rings for captures ──
            private void drawHints(Graphics2D g, int ts, int ox, int oy) {
                for (int[] h : hints) {
                    int r=h[2], c=h[3];
                    if (board[r][c] != EMPTY) {
                        // Capture target: draw L-shaped corners
                        alpha(g, 0.70f);
                        g.setColor(COL_HINT);
                        int t = Math.max(3, ts/7);   // corner thickness
                        int arc = ts / 3;             // corner length
                        int x = ox+c*ts, y = oy+r*ts;
                        g.fillRect(x,           y,           t,   arc);
                        g.fillRect(x,           y,           arc, t  );
                        g.fillRect(x+ts-t,      y,           t,   arc);
                        g.fillRect(x+ts-arc,    y,           arc, t  );
                        g.fillRect(x,           y+ts-arc,    t,   arc);
                        g.fillRect(x,           y+ts-t,      arc, t  );
                        g.fillRect(x+ts-t,      y+ts-arc,    t,   arc);
                        g.fillRect(x+ts-arc,    y+ts-t,      arc, t  );
                    } else {
                        // Empty square: centred dot
                        alpha(g, 0.55f);
                        g.setColor(new Color(0x3A6A3A));
                        int ds = ts/3, dx=ox+c*ts+(ts-ds)/2, dy=oy+r*ts+(ts-ds)/2;
                        g.fillOval(dx, dy, ds, ds);
                    }
                    alpha(g, 1f);
                }
            }

            // ── Selected-square highlight ─────────────────────────────────────
            private void drawSelected(Graphics2D g, int ts, int ox, int oy) {
                if (selRow < 0) return;
                alpha(g, 0.55f);
                g.setColor(COL_SEL);
                g.fillRect(ox+selCol*ts, oy+selRow*ts, ts, ts);
                alpha(g, 1f);
            }

            // ── King-in-check glow ────────────────────────────────────────────
            private void drawCheckGlow(Graphics2D g, int ts, int ox, int oy) {
                for (int r=0; r<8; r++)
                    for (int c=0; c<8; c++)
                        if (Math.abs(board[r][c])==KING && isInCheck(board, color(board[r][c]))) {
                            alpha(g, 0.52f);
                            g.setColor(COL_CHECK);
                            g.fillRect(ox+c*ts, oy+r*ts, ts, ts);
                            alpha(g, 1f);
                        }
            }

            // ── Pieces ────────────────────────────────────────────────────────
            private void drawPieces(Graphics2D g, int ts, int ox, int oy) {
                int pad = Math.max(3, ts/12);
                for (int r=0; r<8; r++)
                    for (int c=0; c<8; c++) {
                        int p = board[r][c];
                        if (p==EMPTY) continue;
                        BufferedImage img = images.get(p);
                        if (img != null)
                            g.drawImage(img, ox+c*ts+pad, oy+r*ts+pad, ts-pad*2, ts-pad*2, null);
                        else
                            drawSymbol(g, p, r, c, ts, ox, oy);
                    }
            }

            private void drawSymbol(Graphics2D g, int piece, int r, int c, int ts, int ox, int oy) {
                String sym = unicodeFor(piece);
                g.setFont(new Font("Segoe UI Symbol", Font.PLAIN, ts*6/10));
                FontMetrics fm = g.getFontMetrics();
                int x = ox+c*ts+(ts-fm.stringWidth(sym))/2;
                int y = oy+r*ts+(ts+fm.getAscent()-fm.getDescent())/2;
                g.setColor(color(piece)==WHITE ? new Color(50,30,10) : new Color(210,200,180));
                g.drawString(sym, x+1, y+1);
                g.setColor(color(piece)==WHITE ? Color.WHITE : new Color(10,10,10));
                g.drawString(sym, x, y);
            }

            // ── Coordinate labels ─────────────────────────────────────────────
            private void drawCoords(Graphics2D g, int ts, int ox, int oy) {
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, ts/8)));
                FontMetrics fm = g.getFontMetrics();
                for (int i=0; i<8; i++) {
                    g.setColor(i%2==0 ? COL_DARK : COL_LIGHT);
                    g.drawString(String.valueOf((char)('a'+i)), ox+i*ts+3, oy+8*ts-3);
                    g.setColor(i%2==0 ? COL_LIGHT : COL_DARK);
                    g.drawString(String.valueOf(8-i), ox+3, oy+i*ts+fm.getAscent()+2);
                }
            }

            // ── Clock boxes painted on the canvas ────────────────────────────
            private void drawClocks(Graphics2D g, int ts, int ox, int oy) {
                if (!clockEnabled) return;
                int bw = 8*ts;
                drawClockBox(g, ox, oy-CLOCK_H+2, bw/2-2, CLOCK_H-4,
                             "⬛  " + formatTime(blackSeconds), turn==BLACK && !gameOver);
                drawClockBox(g, ox, oy+8*ts+2, bw/2-2, CLOCK_H-4,
                             "⬜  " + formatTime(whiteSeconds), turn==WHITE && !gameOver);
            }

            private void drawClockBox(Graphics2D g, int x, int y, int w, int h,
                                       String text, boolean active) {
                g.setColor(active ? new Color(0x2E5A2E) : new Color(0x333333));
                g.fillRoundRect(x, y, w, h, 8, 8);
                g.setColor(active ? new Color(0xAAFFAA) : COL_TEXT);
                g.setFont(new Font("Monospaced", Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, x+(w-fm.stringWidth(text))/2,
                             y+(h+fm.getAscent()-fm.getDescent())/2);
            }

            // ── Status bar ───────────────────────────────────────────────────
            private void drawStatus(Graphics2D g, int ts, int ox, int oy) {
                int sy = oy + 8*ts + CLOCK_H;
                g.setColor(COL_BG);
                g.fillRect(0, sy, getWidth(), STATUS_H);
                String text;
                if (gameOver) {
                    text = "Game Over";
                } else if (isInCheck(board, turn)) {
                    text = (turn==WHITE?"White":"Black") + " is in CHECK!";
                } else {
                    text = (turn==WHITE?"White":"Black")
                         + (vsAI && turn==BLACK ? " (AI thinking…)" : "'s turn");
                }
                g.setFont(new Font("Serif", Font.BOLD, Math.max(14, ts/5)));
                g.setColor(COL_TEXT);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, ox+(8*ts-fm.stringWidth(text))/2,
                             sy+(STATUS_H+fm.getAscent()-fm.getDescent())/2);
            }

            private void alpha(Graphics2D g, float a) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            }
        } // end BoardCanvas

        // =====================================================================
        //  CLICK HANDLING
        // =====================================================================
        private void handleSquareClick(int row, int col) {
            int piece = board[row][col];
            if (selRow == -1) {
                if (piece != EMPTY && color(piece) == turn) {
                    selRow=row; selCol=col;
                    hints = generateLegalMoves(board,row,col,turn,enPassantCol,
                                whiteKingMoved,blackKingMoved,
                                whiteRookAMoved,whiteRookHMoved,
                                blackRookAMoved,blackRookHMoved);
                    repaint();
                }
                return;
            }
            int[] chosen = null;
            for (int[] h : hints) if (h[2]==row && h[3]==col) { chosen=h; break; }
            if (chosen != null) {
                executeMove(selRow, selCol, row, col);
            } else if (piece != EMPTY && color(piece) == turn) {
                selRow=row; selCol=col;
                hints = generateLegalMoves(board,row,col,turn,enPassantCol,
                            whiteKingMoved,blackKingMoved,
                            whiteRookAMoved,whiteRookHMoved,
                            blackRookAMoved,blackRookHMoved);
                repaint();
            } else {
                selRow=-1; selCol=-1; hints.clear(); repaint();
            }
        }

        // =====================================================================
        //  EXECUTE MOVE
        // =====================================================================
        private void executeMove(int fr, int fc, int tr, int tc) {
            int piece=board[fr][fc], type=Math.abs(piece), col=color(piece);
            lastFR=fr; lastFC=fc; lastTR=tr; lastTC=tc;

        // En passant
        int newEP = -1;
        boolean ep = type==PAWN && tc==enPassantCol
            && Math.abs(fr-tr)==1 && Math.abs(fc-tc)==1 && board[tr][tc]==EMPTY;
            if (ep) board[fr][tc]=EMPTY;
            if (type==PAWN && Math.abs(tr-fr)==2) newEP=fc;

            // Castling
            boolean castle = type==KING && Math.abs(tc-fc)==2;
            if (castle) {
                if(tc==6){ board[fr][5]=board[fr][7]; board[fr][7]=EMPTY; }
                else     { board[fr][3]=board[fr][0]; board[fr][0]=EMPTY; }
            }

            // Build move label before modifying board
            String label = buildLabel(fr,fc,tr,tc,piece,board[tr][tc],castle,ep);

            board[tr][tc]=piece; board[fr][fc]=EMPTY;

            // Castling/king tracking
            if (type==KING){ if(col==WHITE) whiteKingMoved=true; else blackKingMoved=true; }
            if (type==ROOK){
                if(col==WHITE){ if(fc==0) whiteRookAMoved=true; if(fc==7) whiteRookHMoved=true; }
                else          { if(fc==0) blackRookAMoved=true; if(fc==7) blackRookHMoved=true; }
            }
            enPassantCol=newEP;

            // Promotion
            if (type==PAWN && (tr==0||tr==7)) {
                int promo=promptPromotion(col);
                board[tr][tc]=col*promo;
                label += "="+pieceChar(promo);
            }

            selRow=-1; selCol=-1; hints.clear();

            // Record move in history
            if (turn==WHITE) {
                moveHistory.add(moveNumber+". "+label);
            } else {
                if (!moveHistory.isEmpty())
                    moveHistory.set(moveHistory.size()-1,
                        moveHistory.get(moveHistory.size()-1)+"   "+label);
                else moveHistory.add("… "+label);
                moveNumber++;
            }
            refreshSidebar();

            turn=-turn;
            repaint();

            // Threefold repetition
            String key=boardKey();
            int cnt=positionCounts.getOrDefault(key,0)+1;
            positionCounts.put(key,cnt);
            if (cnt>=3) { triggerDraw("Threefold repetition – draw!"); return; }

            checkGameOver();
            if (!gameOver && vsAI && turn==BLACK)
                SwingUtilities.invokeLater(this::doAiMove);
        }

        // =====================================================================
        //  MOVE LABEL BUILDER  (simplified algebraic notation)
        // =====================================================================
        private String buildLabel(int fr, int fc, int tr, int tc,
                                   int piece, int captured, boolean castle, boolean ep) {
            if (castle) return tc==6 ? "O-O" : "O-O-O";
            int type=Math.abs(piece);
            String to=""+(char)('a'+tc)+(8-tr);
            if (type==PAWN) {
                return (captured!=EMPTY||ep) ? (char)('a'+fc)+"x"+to : to;
            }
            String from=""+(char)('a'+fc)+(8-fr);
            String cap=(captured!=EMPTY)?"x":"-";
            return pieceChar(type)+from+cap+to;
        }

        private String pieceChar(int type) {
            switch(type){
                case KNIGHT:return"N"; case BISHOP:return"B";
                case ROOK:  return"R"; case QUEEN: return"Q"; case KING:return"K";
                default: return"";
            }
        }

        private void refreshSidebar() {
            if (moveListArea == null) return;
            StringBuilder sb = new StringBuilder();
            for (String m : moveHistory) sb.append(m).append("\n");
            moveListArea.setText(sb.toString());
            moveListArea.setCaretPosition(moveListArea.getDocument().getLength());
            if (evalBarPanel != null) evalBarPanel.repaint();
        }

        // =====================================================================
        //  PROMOTION DIALOG
        // =====================================================================
        private int promptPromotion(int col) {
            String[] opts={"Queen","Rook","Bishop","Knight"};
            String pick=(String)JOptionPane.showInputDialog(this,"Promote pawn:","Pawn Promotion",
                    JOptionPane.PLAIN_MESSAGE,null,opts,"Queen");
            if (pick==null) return QUEEN;
            switch(pick){case"Rook":return ROOK;case"Bishop":return BISHOP;case"Knight":return KNIGHT;}
            return QUEEN;
        }

        // =====================================================================
        //  GAME OVER
        // =====================================================================
        private void checkGameOver() {
            List<int[]> all=generateAllLegalMoves(board,turn,enPassantCol,
                    whiteKingMoved,blackKingMoved,whiteRookAMoved,whiteRookHMoved,
                    blackRookAMoved,blackRookHMoved);
            if (!all.isEmpty()) return;
            gameOver=true; repaint();
            String msg=isInCheck(board,turn)
                ? "Checkmate! "+(turn==WHITE?"Black":"White")+" wins!"
                : "Stalemate – draw!";
            showEndDialog(msg);
        }

        private void triggerDraw(String msg) {
            if (gameOver) return;
            gameOver=true; repaint(); showEndDialog(msg);
        }

        private void showEndDialog(String msg) {
            SwingUtilities.invokeLater(()->{
                stopClocks();
                int opt=JOptionPane.showConfirmDialog(this,msg+"\n\nPlay again?",
                        "Game Over",JOptionPane.YES_NO_OPTION);
                if(opt==JOptionPane.YES_OPTION){ resetBoard(); repaint(); }
                else parent.returnToMenu();
            });
        }

        // =====================================================================
        //  CLOCKS
        // =====================================================================
        private void startClockTimer() {
            if (!clockEnabled) return;
            clockTimer=new javax.swing.Timer(1000, e->{
                if (gameOver) return;
                if (turn==WHITE){ whiteSeconds--; if(whiteSeconds<=0){ whiteSeconds=0; triggerDraw("White ran out of time – Black wins!"); } }
                else            { blackSeconds--; if(blackSeconds<=0){ blackSeconds=0; triggerDraw("Black ran out of time – White wins!"); } }
                repaint();
                if(evalBarPanel!=null) evalBarPanel.repaint();
            });
            clockTimer.start();
        }

        void stopClocks() { if(clockTimer!=null) clockTimer.stop(); }
        private static String formatTime(int s){ s=Math.max(0,s); return String.format("%d:%02d",s/60,s%60); }

        // =====================================================================
        //  BOARD RESET
        // =====================================================================
        private void resetBoard() {
            for (int[] row:board) Arrays.fill(row,EMPTY);
            int[] back={ROOK,KNIGHT,BISHOP,QUEEN,KING,BISHOP,KNIGHT,ROOK};
            for (int c=0;c<8;c++){
                board[0][c]=-back[c]; board[1][c]=-PAWN;
                board[6][c]= PAWN;   board[7][c]= back[c];
            }
            turn=WHITE; selRow=-1; selCol=-1; hints.clear(); gameOver=false; enPassantCol=-1;
            whiteKingMoved=blackKingMoved=false;
            whiteRookAMoved=whiteRookHMoved=blackRookAMoved=blackRookHMoved=false;
            lastFR=lastFC=lastTR=lastTC=-1;
            positionCounts.clear();
            moveHistory.clear(); moveNumber=1;
            whiteSeconds=blackSeconds=initialSeconds;
            if(moveListArea!=null) moveListArea.setText("");
            if(evalBarPanel!=null) evalBarPanel.repaint();
            stopClocks(); startClockTimer();
        }

        // =====================================================================
        //  POSITION KEY  (for threefold repetition)
        // =====================================================================
        private String boardKey() {
            StringBuilder sb=new StringBuilder();
            for(int[] row:board) for(int p:row){ sb.append(p); sb.append(','); }
            sb.append(turn).append(':').append(enPassantCol).append(':');
            sb.append(whiteKingMoved?1:0).append(blackKingMoved?1:0);
            sb.append(whiteRookAMoved?1:0).append(whiteRookHMoved?1:0);
            sb.append(blackRookAMoved?1:0).append(blackRookHMoved?1:0);
            return sb.toString();
        }

        // =====================================================================
        //  IMAGE LOADING
        // =====================================================================
        private void loadImages() {
            int[]    types=  {PAWN,KNIGHT,BISHOP,ROOK,QUEEN,KING};
            String[] names=  {"pawn","knight","bishop","rook","queen","king"};
            for(int i=0;i<types.length;i++){
                images.put( types[i], loadImg("images/white"+names[i]+".png"));
                images.put(-types[i], loadImg("images/black"+names[i]+".png"));
            }
        }
        private BufferedImage loadImg(String p){ try{ return ImageIO.read(new File(p)); }catch(IOException e){ return null; } }

        // =====================================================================
        //  AI  (minimax + alpha-beta pruning)
        // =====================================================================
        private void doAiMove(){
            int[] best=findBestMove(board,AI_DEPTH,enPassantCol,
                    whiteKingMoved,blackKingMoved,whiteRookAMoved,whiteRookHMoved,blackRookAMoved,blackRookHMoved);
            if(best!=null) executeMove(best[0],best[1],best[2],best[3]);
        }

        private int[] findBestMove(int[][] b,int depth,int epCol,
                boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            int best=Integer.MAX_VALUE; int[] bestMove=null;
            for(int[] m:generateAllLegalMoves(b,BLACK,epCol,wkm,bkm,wram,wrhm,bram,brhm)){
                int[][] cp=copyBoard(b);
                MoveResult mr=applyMoveToBoard(cp,m[0],m[1],m[2],m[3],BLACK,epCol,wkm,bkm,wram,wrhm,bram,brhm);
                int score=minimax(cp,depth-1,Integer.MIN_VALUE,Integer.MAX_VALUE,true,mr.epCol,mr.wkm,mr.bkm,mr.wram,mr.wrhm,mr.bram,mr.brhm);
                if(score<best){ best=score; bestMove=m; }
            }
            return bestMove;
        }

        private int minimax(int[][] b,int depth,int alpha,int beta,boolean max,
                int epCol,boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            if(depth==0) return evaluate(b);
            int side=max?WHITE:BLACK;
            List<int[]> moves=generateAllLegalMoves(b,side,epCol,wkm,bkm,wram,wrhm,bram,brhm);
            if(moves.isEmpty()) return isInCheck(b,side)?(max?-30000:30000):0;
            if(max){
                int best=Integer.MIN_VALUE;
                for(int[] m:moves){
                    int[][] cp=copyBoard(b);
                    MoveResult mr=applyMoveToBoard(cp,m[0],m[1],m[2],m[3],WHITE,epCol,wkm,bkm,wram,wrhm,bram,brhm);
                    best=Math.max(best,minimax(cp,depth-1,alpha,beta,false,mr.epCol,mr.wkm,mr.bkm,mr.wram,mr.wrhm,mr.bram,mr.brhm));
                    alpha=Math.max(alpha,best); if(beta<=alpha) break;
                }
                return best;
            } else {
                int best=Integer.MAX_VALUE;
                for(int[] m:moves){
                    int[][] cp=copyBoard(b);
                    MoveResult mr=applyMoveToBoard(cp,m[0],m[1],m[2],m[3],BLACK,epCol,wkm,bkm,wram,wrhm,bram,brhm);
                    best=Math.min(best,minimax(cp,depth-1,alpha,beta,true,mr.epCol,mr.wkm,mr.bkm,mr.wram,mr.wrhm,mr.bram,mr.brhm));
                    beta=Math.min(beta,best); if(beta<=alpha) break;
                }
                return best;
            }
        }

        // =====================================================================
        //  EVALUATION  (material + piece-square tables)
        // =====================================================================
        int evaluate(int[][] b){
            int score=0;
            for(int r=0;r<8;r++) for(int c=0;c<8;c++){
                int p=b[r][c]; if(p==EMPTY) continue;
                int col=color(p), type=Math.abs(p), pr=(col==WHITE)?r:(7-r);
                score+=col*(pieceValue(type)+pst(type,pr,c));
            }
            return score;
        }

        private int pieceValue(int t){
            switch(t){case PAWN:return 100;case KNIGHT:return 320;case BISHOP:return 330;
                       case ROOK:return 500;case QUEEN:return 900;case KING:return 20000;} return 0;
        }
        private int pst(int t,int r,int c){
            switch(t){case PAWN:return PST_PAWN[r][c];case KNIGHT:return PST_KNIGHT[r][c];
                       case BISHOP:return PST_BISHOP[r][c];case ROOK:return PST_ROOK[r][c];
                       case QUEEN:return PST_QUEEN[r][c];case KING:return PST_KING[r][c];} return 0;
        }

        // =====================================================================
        //  MOVE RESULT  (carries updated state flags out of simulation)
        // =====================================================================
        static class MoveResult {
            int epCol; boolean wkm,bkm,wram,wrhm,bram,brhm;
        }

        private MoveResult applyMoveToBoard(int[][] b,int fr,int fc,int tr,int tc,int col,
                int epCol,boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            MoveResult mr=new MoveResult();
            mr.epCol=epCol; mr.wkm=wkm; mr.bkm=bkm; mr.wram=wram; mr.wrhm=wrhm; mr.bram=bram; mr.brhm=brhm;
            int piece=b[fr][fc], type=Math.abs(piece);
            if(type==PAWN&&tc==epCol&&Math.abs(fr-tr)==1&&Math.abs(fc-tc)==1&&b[tr][tc]==EMPTY) b[fr][tc]=EMPTY;
            mr.epCol=(type==PAWN && Math.abs(tr-fr)==2)?fc:-1;
            if(type==KING && Math.abs(tc-fc)==2){ if(tc==6){b[fr][5]=b[fr][7];b[fr][7]=EMPTY;}else{b[fr][3]=b[fr][0];b[fr][0]=EMPTY;} }
            b[tr][tc]=piece; b[fr][fc]=EMPTY;
            if(type==PAWN&&(tr==0||tr==7)) b[tr][tc]=col*QUEEN;
            if(type==KING){if(col==WHITE)mr.wkm=true;else mr.bkm=true;}
            if(type==ROOK){if(col==WHITE){if(fc==0)mr.wram=true;if(fc==7)mr.wrhm=true;}else{if(fc==0)mr.bram=true;if(fc==7)mr.brhm=true;}}
            return mr;
        }

        // =====================================================================
        //  MOVE GENERATION
        // =====================================================================
        static List<int[]> generateAllLegalMoves(int[][] b,int side,int epCol,
                boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            List<int[]> all=new ArrayList<>();
            for(int r=0;r<8;r++) for(int c=0;c<8;c++)
                if(b[r][c]!=EMPTY && color(b[r][c])==side)
                    all.addAll(generateLegalMoves(b,r,c,side,epCol,wkm,bkm,wram,wrhm,bram,brhm));
            return all;
        }

        static List<int[]> generateLegalMoves(int[][] b,int row,int col,int side,int epCol,
                boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            List<int[]> pseudo=generatePseudoMoves(b,row,col,epCol,wkm,bkm,wram,wrhm,bram,brhm);
            List<int[]> legal=new ArrayList<>();
            for(int[] m:pseudo){
                int[][] tmp=copyBoard(b);
                applyMoveSimple(tmp,row,col,m[2],m[3],epCol);
                if(!isInCheck(tmp,side)) legal.add(m);
            }
            return legal;
        }

        private static void applyMoveSimple(int[][] b,int fr,int fc,int tr,int tc,int epCol){
            int piece=b[fr][fc],type=Math.abs(piece),col=color(piece);
            if(type==PAWN&&fc==epCol&&Math.abs(fr-tr)==1&&Math.abs(fc-tc)==1&&b[tr][tc]==EMPTY) b[fr][tc]=EMPTY;
            if(type==KING&&Math.abs(tc-fc)==2){if(tc==6){b[fr][5]=b[fr][7];b[fr][7]=EMPTY;}else{b[fr][3]=b[fr][0];b[fr][0]=EMPTY;}}
            b[tr][tc]=piece; b[fr][fc]=EMPTY;
            if(type==PAWN&&(tr==0||tr==7)) b[tr][tc]=col*QUEEN;
        }

        private static List<int[]> generatePseudoMoves(int[][] b,int row,int col,int epCol,
                boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            List<int[]> mv=new ArrayList<>();
            int piece=b[row][col],type=Math.abs(piece),side=color(piece);
            switch(type){
                case PAWN:  addPawnMoves  (b,row,col,side,epCol,mv); break;
                case KNIGHT:addKnightMoves(b,row,col,side,mv); break;
                case BISHOP:addSliding(b,row,col,side,mv,new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case ROOK:  addSliding(b,row,col,side,mv,new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                case QUEEN: addSliding(b,row,col,side,mv,new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case KING:  addKingMoves(b,row,col,side,mv,wkm,bkm,wram,wrhm,bram,brhm); break;
            }
            return mv;
        }

        private static void addPawnMoves(int[][] b,int row,int col,int side,int epCol,List<int[]> mv){
            int dir=(side==WHITE)?-1:1, start=(side==WHITE)?6:1, nr=row+dir;
            if(inBounds(nr,col)&&b[nr][col]==EMPTY){
                mv.add(new int[]{row,col,nr,col});
                if(row==start&&b[nr+dir][col]==EMPTY) mv.add(new int[]{row,col,nr+dir,col});
            }
            for(int dc:new int[]{-1,1}){
                int nc=col+dc; if(!inBounds(nr,nc)) continue;
                if(b[nr][nc]!=EMPTY&&color(b[nr][nc])!=side) mv.add(new int[]{row,col,nr,nc});
                if(nc==epCol&&b[nr][nc]==EMPTY&&b[row][nc]!=EMPTY&&color(b[row][nc])!=side&&Math.abs(b[row][nc])==PAWN)
                    mv.add(new int[]{row,col,nr,nc});
            }
        }

        private static void addKnightMoves(int[][] b,int row,int col,int side,List<int[]> mv){
            for(int[] d:new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}){
                int nr=row+d[0],nc=col+d[1];
                if(inBounds(nr,nc)&&(b[nr][nc]==EMPTY||color(b[nr][nc])!=side)) mv.add(new int[]{row,col,nr,nc});
            }
        }

        private static void addSliding(int[][] b,int row,int col,int side,List<int[]> mv,int[][] dirs){
            for(int[] d:dirs){
                int nr=row+d[0],nc=col+d[1];
                while(inBounds(nr,nc)){
                    if(b[nr][nc]==EMPTY) mv.add(new int[]{row,col,nr,nc});
                    else{if(color(b[nr][nc])!=side) mv.add(new int[]{row,col,nr,nc}); break;}
                    nr+=d[0]; nc+=d[1];
                }
            }
        }

        private static void addKingMoves(int[][] b,int row,int col,int side,List<int[]> mv,
                boolean wkm,boolean bkm,boolean wram,boolean wrhm,boolean bram,boolean brhm){
            for(int[] d:new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}){
                int nr=row+d[0],nc=col+d[1];
                if(inBounds(nr,nc)&&(b[nr][nc]==EMPTY||color(b[nr][nc])!=side)) mv.add(new int[]{row,col,nr,nc});
            }
            boolean km=(side==WHITE)?wkm:bkm, ram=(side==WHITE)?wram:bram, rhm=(side==WHITE)?wrhm:brhm;
            if(!km&&!isInCheck(b,side)){
                if(!rhm&&b[row][5]==EMPTY&&b[row][6]==EMPTY&&!isAttacked(b,row,5,-side)&&!isAttacked(b,row,6,-side))
                    mv.add(new int[]{row,col,row,6});
                if(!ram&&b[row][1]==EMPTY&&b[row][2]==EMPTY&&b[row][3]==EMPTY&&!isAttacked(b,row,3,-side)&&!isAttacked(b,row,2,-side))
                    mv.add(new int[]{row,col,row,2});
            }
        }

        // =====================================================================
        //  CHECK / ATTACK DETECTION
        // =====================================================================
        static boolean isInCheck(int[][] b,int side){
            for(int r=0;r<8;r++) for(int c=0;c<8;c++)
                if(b[r][c]==side*KING) return isAttacked(b,r,c,-side);
            return false;
        }

        static boolean isAttacked(int[][] b,int row,int col,int by){
            for(int[] d:new int[][]{{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}){
                int r=row+d[0],c=col+d[1]; if(inBounds(r,c)&&b[r][c]==by*KNIGHT) return true;
            }
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) if(slidingAttack(b,row,col,by,d,ROOK)) return true;
            for(int[] d:new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}) if(slidingAttack(b,row,col,by,d,BISHOP)) return true;
            for(int[] d:new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}){
                int r=row+d[0],c=col+d[1]; if(inBounds(r,c)&&b[r][c]==by*KING) return true;
            }
            int pd=(by==WHITE)?1:-1;
            for(int dc:new int[]{-1,1}){ int r=row+pd,c=col+dc; if(inBounds(r,c)&&b[r][c]==by*PAWN) return true; }
            return false;
        }

        private static boolean slidingAttack(int[][] b,int row,int col,int by,int[] dir,int st){
            int r=row+dir[0],c=col+dir[1];
            while(inBounds(r,c)){
                int p=b[r][c];
                if(p!=EMPTY) return color(p)==by&&(Math.abs(p)==st||Math.abs(p)==QUEEN);
                r+=dir[0]; c+=dir[1];
            }
            return false;
        }

        // =====================================================================
        //  UTILITIES
        // =====================================================================
        static int color(int p){ return p>0?WHITE:BLACK; }
        static boolean inBounds(int r,int c){ return r>=0&&r<8&&c>=0&&c<8; }
        static int[][] copyBoard(int[][] b){
            int[][] cp=new int[8][8]; for(int r=0;r<8;r++) cp[r]=Arrays.copyOf(b[r],8); return cp;
        }
        private static String unicodeFor(int piece){
            int t=Math.abs(piece);
            if(color(piece)==WHITE) switch(t){case KING:return"♔";case QUEEN:return"♕";case ROOK:return"♖";case BISHOP:return"♗";case KNIGHT:return"♘";case PAWN:return"♙";}
            else                    switch(t){case KING:return"♚";case QUEEN:return"♛";case ROOK:return"♜";case BISHOP:return"♝";case KNIGHT:return"♞";case PAWN:return"♟";}
            return"?";
        }
    }
}