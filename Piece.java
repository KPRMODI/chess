package chess;

public class Piece {
    private PieceType type;
    private boolean white;
    private int row, col;
    private boolean hasMoved;

    public Piece(PieceType type, boolean white, int row, int col) {
        this.type = type;
        this.white = white;
        this.row = row;
        this.col = col;
        this.hasMoved = false;
    }

    public PieceType getType()    { return type; }
    public boolean isWhite()      { return white; }
    public int getRow()           { return row; }
    public int getCol()           { return col; }
    public boolean hasMoved()     { return hasMoved; }

    public void setRow(int row)   { this.row = row; }
    public void setCol(int col)   { this.col = col; }
    public void setMoved()        { this.hasMoved = true; }

    /** Returns the image filename for this piece, e.g. "whiteking.png" */
    public String getImageName() {
        String color = white ? "white" : "black";
        String name;
        switch (type) {
            case KING:   name = "king";   break;
            case QUEEN:  name = "queen";  break;
            case ROOK:   name = "rook";   break;
            case BISHOP: name = "bishop"; break;
            case KNIGHT: name = "horse";  break;  // matches your "blackhorse.png" naming
            case PAWN:   name = "pawn";   break;
            default:     name = "";
        }
        return color + name + ".png";
    }

    public Piece copy() {
        Piece p = new Piece(type, white, row, col);
        if (hasMoved) p.setMoved();
        return p;
    }
}
