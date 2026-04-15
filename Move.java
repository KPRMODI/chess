package chess;

public class Move {
    public final int fromRow, fromCol, toRow, toCol;
    public final Piece piece;
    public final Piece captured;   // may be null
    public final boolean isCastle;
    public final boolean isEnPassant;
    public final PieceType promotion; // null unless pawn promotion

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                Piece piece, Piece captured,
                boolean isCastle, boolean isEnPassant, PieceType promotion) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.captured = captured;
        this.isCastle = isCastle;
        this.isEnPassant = isEnPassant;
        this.promotion = promotion;
    }

    /** Standard quiet move */
    public static Move of(int fr, int fc, int tr, int tc, Piece p, Piece cap) {
        return new Move(fr, fc, tr, tc, p, cap, false, false, null);
    }
}
