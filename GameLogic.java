package chess;

import java.util.ArrayList;
import java.util.List;

/**
 * Core chess rules engine.
 * Board state: board[row][col], row 0 = rank 8 (black's back rank), row 7 = rank 1 (white's back rank).
 */
public class GameLogic {

    // En-passant target square (the square the capturing pawn lands on), or -1
    private int enPassantRow = -1, enPassantCol = -1;

    /** Returns all LEGAL moves for the piece at (row,col) on the given board. */
    public List<Move> legalMoves(Piece[][] board, int row, int col) {
        Piece p = board[row][col];
        if (p == null) return List.of();
        List<Move> pseudo = pseudoMoves(board, row, col);
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            if (!leavesKingInCheck(board, m, p.isWhite())) {
                legal.add(m);
            }
        }
        return legal;
    }

    /** Returns all legal moves for a side. */
    public List<Move> allLegalMoves(Piece[][] board, boolean white) {
        List<Move> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != null && board[r][c].isWhite() == white)
                    all.addAll(legalMoves(board, r, c));
        return all;
    }

    public boolean isCheckmate(Piece[][] board, boolean white) {
        return isInCheck(board, white) && allLegalMoves(board, white).isEmpty();
    }

    public boolean isStalemate(Piece[][] board, boolean white) {
        return !isInCheck(board, white) && allLegalMoves(board, white).isEmpty();
    }

    public boolean isInCheck(Piece[][] board, boolean white) {
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != null && board[r][c].isWhite() == white
                        && board[r][c].getType() == PieceType.KING) {
                    kr = r; kc = c;
                }
        if (kr == -1) return false;
        return isAttacked(board, kr, kc, !white);
    }

    /** Apply a move to the board (mutates). Returns captured piece (or null). */
    public Piece applyMove(Piece[][] board, Move m) {
        Piece moving = board[m.fromRow][m.fromCol];
        Piece captured = board[m.toRow][m.toCol];

        board[m.toRow][m.toCol] = moving;
        board[m.fromRow][m.fromCol] = null;
        moving.setRow(m.toRow);
        moving.setCol(m.toCol);
        moving.setMoved();

        // En passant capture
        if (m.isEnPassant) {
            int captureRow = m.fromRow; // same rank as moving pawn
            board[captureRow][m.toCol] = null;
            captured = m.captured;
        }

        // Castling – move the rook
        if (m.isCastle) {
            if (m.toCol == 6) { // kingside
                Piece rook = board[m.toRow][7];
                board[m.toRow][5] = rook;
                board[m.toRow][7] = null;
                if (rook != null) { rook.setCol(5); rook.setMoved(); }
            } else { // queenside
                Piece rook = board[m.toRow][0];
                board[m.toRow][3] = rook;
                board[m.toRow][0] = null;
                if (rook != null) { rook.setCol(3); rook.setMoved(); }
            }
        }

        // Promotion
        if (m.promotion != null) {
            board[m.toRow][m.toCol] = new Piece(m.promotion, moving.isWhite(), m.toRow, m.toCol);
            board[m.toRow][m.toCol].setMoved();
        }

        // Update en-passant target
        if (moving.getType() == PieceType.PAWN && Math.abs(m.toRow - m.fromRow) == 2) {
            enPassantRow = (m.fromRow + m.toRow) / 2;
            enPassantCol = m.toCol;
        } else {
            enPassantRow = -1;
            enPassantCol = -1;
        }

        return captured;
    }

    public int getEnPassantRow() { return enPassantRow; }
    public int getEnPassantCol() { return enPassantCol; }
    public void setEnPassant(int r, int c) { enPassantRow = r; enPassantCol = c; }
    public void clearEnPassant() { enPassantRow = -1; enPassantCol = -1; }

    // -------------------------------------------------------------------------
    // Pseudo-move generation (ignores check)
    // -------------------------------------------------------------------------

    private List<Move> pseudoMoves(Piece[][] board, int row, int col) {
        Piece p = board[row][col];
        List<Move> moves = new ArrayList<>();
        switch (p.getType()) {
            case PAWN:   addPawnMoves(board, p, row, col, moves);   break;
            case KNIGHT: addKnightMoves(board, p, row, col, moves); break;
            case BISHOP: addSlidingMoves(board, p, row, col, moves,
                             new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
            case ROOK:   addSlidingMoves(board, p, row, col, moves,
                             new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
            case QUEEN:  addSlidingMoves(board, p, row, col, moves,
                             new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}); break;
            case KING:   addKingMoves(board, p, row, col, moves);   break;
        }
        return moves;
    }

    private void addPawnMoves(Piece[][] board, Piece p, int row, int col, List<Move> moves) {
        int dir = p.isWhite() ? -1 : 1;   // white moves up (decreasing row)
        int startRow = p.isWhite() ? 6 : 1;
        int promRow  = p.isWhite() ? 0 : 7;

        // One forward
        int nr = row + dir;
        if (inBounds(nr, col) && board[nr][col] == null) {
            addPawnMove(moves, p, row, col, nr, col, null, promRow);
            // Two forward from start
            if (row == startRow && board[nr + dir][col] == null) {
                moves.add(Move.of(row, col, nr + dir, col, p, null));
            }
        }
        // Captures
        for (int dc : new int[]{-1, 1}) {
            int nc = col + dc;
            if (inBounds(nr, nc)) {
                Piece target = board[nr][nc];
                if (target != null && target.isWhite() != p.isWhite()) {
                    addPawnMove(moves, p, row, col, nr, nc, target, promRow);
                }
                // En passant
                if (nr == enPassantRow && nc == enPassantCol) {
                    Piece epPawn = board[row][nc];
                    moves.add(new Move(row, col, nr, nc, p, epPawn, false, true, null));
                }
            }
        }
    }

    private void addPawnMove(List<Move> moves, Piece p, int fr, int fc, int tr, int tc, Piece cap, int promRow) {
        if (tr == promRow) {
            for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT})
                moves.add(new Move(fr, fc, tr, tc, p, cap, false, false, pt));
        } else {
            moves.add(Move.of(fr, fc, tr, tc, p, cap));
        }
    }

    private void addKnightMoves(Piece[][] board, Piece p, int row, int col, List<Move> moves) {
        int[][] deltas = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : deltas) {
            int nr = row + d[0], nc = col + d[1];
            if (inBounds(nr, nc)) {
                Piece t = board[nr][nc];
                if (t == null || t.isWhite() != p.isWhite())
                    moves.add(Move.of(row, col, nr, nc, p, t));
            }
        }
    }

    private void addSlidingMoves(Piece[][] board, Piece p, int row, int col,
                                  List<Move> moves, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (inBounds(nr, nc)) {
                Piece t = board[nr][nc];
                if (t == null) {
                    moves.add(Move.of(row, col, nr, nc, p, null));
                } else {
                    if (t.isWhite() != p.isWhite())
                        moves.add(Move.of(row, col, nr, nc, p, t));
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private void addKingMoves(Piece[][] board, Piece p, int row, int col, List<Move> moves) {
        int[][] deltas = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : deltas) {
            int nr = row + d[0], nc = col + d[1];
            if (inBounds(nr, nc)) {
                Piece t = board[nr][nc];
                if (t == null || t.isWhite() != p.isWhite())
                    moves.add(Move.of(row, col, nr, nc, p, t));
            }
        }
        // Castling
        if (!p.hasMoved() && !isInCheck(board, p.isWhite())) {
            // Kingside
            Piece kRook = board[row][7];
            if (kRook != null && kRook.getType() == PieceType.ROOK && !kRook.hasMoved()
                    && board[row][5] == null && board[row][6] == null
                    && !isAttacked(board, row, 5, !p.isWhite())
                    && !isAttacked(board, row, 6, !p.isWhite())) {
                moves.add(new Move(row, col, row, 6, p, null, true, false, null));
            }
            // Queenside
            Piece qRook = board[row][0];
            if (qRook != null && qRook.getType() == PieceType.ROOK && !qRook.hasMoved()
                    && board[row][1] == null && board[row][2] == null && board[row][3] == null
                    && !isAttacked(board, row, 2, !p.isWhite())
                    && !isAttacked(board, row, 3, !p.isWhite())) {
                moves.add(new Move(row, col, row, 2, p, null, true, false, null));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Check / attack detection
    // -------------------------------------------------------------------------

    /** Is square (row,col) attacked by any piece of color `byWhite`? */
    public boolean isAttacked(Piece[][] board, int row, int col, boolean byWhite) {
        // Knight attacks
        int[][] kDeltas = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : kDeltas) {
            int r = row + d[0], c = col + d[1];
            if (inBounds(r, c)) {
                Piece p = board[r][c];
                if (p != null && p.isWhite() == byWhite && p.getType() == PieceType.KNIGHT) return true;
            }
        }
        // Sliding (queen, rook, bishop)
        int[][] straight = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : straight)
            if (slidingAttack(board, row, col, byWhite, d, PieceType.ROOK, PieceType.QUEEN)) return true;
        int[][] diag = {{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] d : diag)
            if (slidingAttack(board, row, col, byWhite, d, PieceType.BISHOP, PieceType.QUEEN)) return true;
        // King
        int[][] kd = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] d : kd) {
            int r = row + d[0], c = col + d[1];
            if (inBounds(r, c)) {
                Piece p = board[r][c];
                if (p != null && p.isWhite() == byWhite && p.getType() == PieceType.KING) return true;
            }
        }
        // Pawns
        int pawnDir = byWhite ? 1 : -1; // white pawns attack upward (lower row index)
        for (int dc : new int[]{-1, 1}) {
            int r = row + pawnDir, c = col + dc;
            if (inBounds(r, c)) {
                Piece p = board[r][c];
                if (p != null && p.isWhite() == byWhite && p.getType() == PieceType.PAWN) return true;
            }
        }
        return false;
    }

    private boolean slidingAttack(Piece[][] board, int row, int col, boolean byWhite,
                                   int[] dir, PieceType... types) {
        int r = row + dir[0], c = col + dir[1];
        while (inBounds(r, c)) {
            Piece p = board[r][c];
            if (p != null) {
                if (p.isWhite() == byWhite) {
                    for (PieceType t : types) if (p.getType() == t) return true;
                }
                break;
            }
            r += dir[0]; c += dir[1];
        }
        return false;
    }

    private boolean leavesKingInCheck(Piece[][] board, Move m, boolean white) {
        Piece[][] copy = copyBoard(board);
        int savedEPR = enPassantRow, savedEPC = enPassantCol;
        applyMove(copy, m);
        boolean inCheck = isInCheck(copy, white);
        enPassantRow = savedEPR;
        enPassantCol = savedEPC;
        return inCheck;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    public static boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    public static Piece[][] copyBoard(Piece[][] board) {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != null)
                    copy[r][c] = board[r][c].copy();
        return copy;
    }

    public static Piece[][] initialBoard() {
        Piece[][] b = new Piece[8][8];
        // Black back rank (row 0)
        PieceType[] backRank = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};
        for (int c = 0; c < 8; c++) {
            b[0][c] = new Piece(backRank[c], false, 0, c);
            b[1][c] = new Piece(PieceType.PAWN, false, 1, c);
            b[6][c] = new Piece(PieceType.PAWN, true,  6, c);
            b[7][c] = new Piece(backRank[c],    true,  7, c);
        }
        return b;
    }
}
