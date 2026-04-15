package chess;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Parent;

import java.util.*;

public class ChessBoard {

    private static final int TILE = 80;
    private static final Color LIGHT = Color.web("#F0D9B5");
    private static final Color DARK  = Color.web("#B58863");
    private static final Color SEL   = Color.web("#7FC97F", 0.75);
    private static final Color HINT  = Color.web("#CDD26A", 0.75);
    private static final Color CHECK = Color.web("#E74C3C", 0.65);

    private final BorderPane root   = new BorderPane();
    private final Pane        board  = new Pane();
    private final Label       status = new Label("White's turn");

    private Piece[][]  pieces;
    private final GameLogic logic = new GameLogic();

    private Piece    selected     = null;
    private List<Move> legalCache = new ArrayList<>();
    private boolean  whiteTurn    = true;

    // Image cache
    private final Map<String, Image> imageCache = new HashMap<>();

    public ChessBoard() {
        pieces = GameLogic.initialBoard();
        board.setPrefSize(8 * TILE, 8 * TILE);
        board.setMaxSize(8 * TILE, 8 * TILE);

        status.getStyleClass().add("status-label");
        HBox statusBar = new HBox(status);
        statusBar.setAlignment(Pos.CENTER);
        statusBar.setStyle("-fx-padding: 8 0 4 0;");

        root.setCenter(board);
        root.setBottom(statusBar);
        root.setStyle("-fx-background-color: #2C2C2C;");

        render();
    }

    public Parent getRoot() { return root; }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void render() {
        board.getChildren().clear();

        // Draw squares
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Rectangle sq = new Rectangle(c * TILE, r * TILE, TILE, TILE);
                sq.setFill((r + c) % 2 == 0 ? LIGHT : DARK);
                board.getChildren().add(sq);

                // Selection highlight
                if (selected != null && selected.getRow() == r && selected.getCol() == c) {
                    Rectangle overlay = overlay(r, c, SEL);
                    board.getChildren().add(overlay);
                }

                // King in check highlight
                if (pieces[r][c] != null && pieces[r][c].getType() == PieceType.KING) {
                    boolean kingWhite = pieces[r][c].isWhite();
                    if (logic.isInCheck(pieces, kingWhite)) {
                        board.getChildren().add(overlay(r, c, CHECK));
                    }
                }
            }
        }

        // Highlight legal move targets
        for (Move m : legalCache) {
            Rectangle hint = overlay(m.toRow, m.toCol, HINT);
            board.getChildren().add(hint);
        }

        // Draw coordinate labels
        for (int i = 0; i < 8; i++) {
            // File letters a-h
            Label file = new Label(String.valueOf((char)('a' + i)));
            file.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(0,0,0,0.45);");
            file.setLayoutX(i * TILE + 2);
            file.setLayoutY(7 * TILE + TILE - 14);
            board.getChildren().add(file);

            // Rank numbers 8-1
            Label rank = new Label(String.valueOf(8 - i));
            rank.setStyle("-fx-font-size: 10; -fx-text-fill: rgba(0,0,0,0.45);");
            rank.setLayoutX(2);
            rank.setLayoutY(i * TILE + 2);
            board.getChildren().add(rank);
        }

        // Draw pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = pieces[r][c];
                if (p == null) continue;
                Image img = loadImage(p.getImageName());
                if (img != null) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(TILE - 8);
                    iv.setFitHeight(TILE - 8);
                    iv.setLayoutX(c * TILE + 4);
                    iv.setLayoutY(r * TILE + 4);
                    iv.setSmooth(true);
                    final int fr = r, fc = c;
                    iv.setOnMouseClicked(e -> handleClick(fr, fc));
                    board.getChildren().add(iv);
                } else {
                    // Fallback: text if image not found
                    Label lbl = new Label(pieceChar(p));
                    lbl.setLayoutX(c * TILE + 18);
                    lbl.setLayoutY(r * TILE + 12);
                    lbl.setStyle("-fx-font-size: 40; -fx-text-fill: " +
                            (p.isWhite() ? "white" : "#1a1a1a") + ";");
                    final int fr = r, fc = c;
                    lbl.setOnMouseClicked(e -> handleClick(fr, fc));
                    board.getChildren().add(lbl);
                }
            }
        }

        // Click handler on squares (for moving to empty squares)
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // We add a transparent click rectangle to catch clicks on empty tiles
                Rectangle clickZone = new Rectangle(c * TILE, r * TILE, TILE, TILE);
                clickZone.setFill(Color.TRANSPARENT);
                final int fr = r, fc = c;
                clickZone.setOnMouseClicked(e -> handleClick(fr, fc));
                board.getChildren().add(clickZone);
            }
        }
    }

    private Rectangle overlay(int row, int col, Color color) {
        Rectangle r = new Rectangle(col * TILE, row * TILE, TILE, TILE);
        r.setFill(color);
        return r;
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    private void handleClick(int row, int col) {
        Piece clicked = pieces[row][col];

        if (selected == null) {
            // Select a piece of the current player
            if (clicked != null && clicked.isWhite() == whiteTurn) {
                selected = clicked;
                legalCache = logic.legalMoves(pieces, row, col);
                render();
            }
            return;
        }

        // Check if clicked square is a legal destination
        Move chosen = null;
        for (Move m : legalCache) {
            if (m.toRow == row && m.toCol == col) {
                chosen = m;
                break;
            }
        }

        if (chosen != null) {
            // Handle promotion choice
            if (chosen.promotion != null) {
                chosen = promptPromotion(chosen);
            }
            executeMove(chosen);
        } else if (clicked != null && clicked.isWhite() == whiteTurn) {
            // Re-select own piece
            selected = clicked;
            legalCache = logic.legalMoves(pieces, row, col);
            render();
        } else {
            // Deselect
            selected = null;
            legalCache = new ArrayList<>();
            render();
        }
    }

    private void executeMove(Move m) {
        logic.applyMove(pieces, m);
        selected = null;
        legalCache = new ArrayList<>();
        whiteTurn = !whiteTurn;
        render();
        checkGameOver();
    }

    private void checkGameOver() {
        if (logic.isCheckmate(pieces, whiteTurn)) {
            String winner = whiteTurn ? "Black" : "White";
            status.setText("Checkmate! " + winner + " wins!");
            showAlert("Checkmate!", winner + " wins by checkmate.");
        } else if (logic.isStalemate(pieces, whiteTurn)) {
            status.setText("Stalemate – Draw!");
            showAlert("Stalemate", "The game is a draw by stalemate.");
        } else if (logic.isInCheck(pieces, whiteTurn)) {
            status.setText((whiteTurn ? "White" : "Black") + " is in CHECK!");
        } else {
            status.setText((whiteTurn ? "White" : "Black") + "'s turn");
        }
    }

    // -------------------------------------------------------------------------
    // Promotion dialog
    // -------------------------------------------------------------------------

    private Move promptPromotion(Move m) {
        List<String> choices = List.of("Queen", "Rook", "Bishop", "Knight");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", choices);
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Promote your pawn!");
        dialog.setContentText("Choose piece:");
        Optional<String> result = dialog.showAndWait();
        PieceType pt = PieceType.QUEEN;
        if (result.isPresent()) {
            switch (result.get()) {
                case "Rook":   pt = PieceType.ROOK;   break;
                case "Bishop": pt = PieceType.BISHOP; break;
                case "Knight": pt = PieceType.KNIGHT; break;
            }
        }
        return new Move(m.fromRow, m.fromCol, m.toRow, m.toCol,
                        m.piece, m.captured, false, false, pt);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Image loadImage(String name) {
        return imageCache.computeIfAbsent(name, n -> {
            try {
                var stream = getClass().getResourceAsStream("/images/" + n);
                if (stream == null) return null;
                return new Image(stream);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /** Unicode fallback if images are missing */
    private String pieceChar(Piece p) {
        if (p.isWhite()) {
            switch (p.getType()) {
                case KING:   return "♔";
                case QUEEN:  return "♕";
                case ROOK:   return "♖";
                case BISHOP: return "♗";
                case KNIGHT: return "♘";
                case PAWN:   return "♙";
            }
        } else {
            switch (p.getType()) {
                case KING:   return "♚";
                case QUEEN:  return "♛";
                case ROOK:   return "♜";
                case BISHOP: return "♝";
                case KNIGHT: return "♞";
                case PAWN:   return "♟";
            }
        }
        return "?";
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg + "\n\nPlay again?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                pieces = GameLogic.initialBoard();
                whiteTurn = true;
                selected = null;
                legalCache = new ArrayList<>();
                logic.clearEnPassant();
                render();
                status.setText("White's turn");
            }
        });
    }
}
