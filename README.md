# Chess – Java Swing

A fully playable two-player chess game using only Java Swing (no JavaFX required).

## Features
- Full chess rules — castling, en passant, pawn promotion
- Check, checkmate & stalemate detection
- Legal-move highlighting (green dots / yellow overlay)
- King-in-check red glow
- Board coordinates (a–h / 1–8)
- Unicode piece fallback if images are missing
- Play-again prompt on game over

## Project Structure

```
ChessGameSwing/
├── src/
│   └── chess/
│       ├── ChessApp.java          ← main() entry point
│       ├── ChessFrame.java        ← JFrame wrapper
│       ├── ChessBoardPanel.java   ← JPanel – all rendering & input
│       ├── GameLogic.java         ← Rules engine
│       ├── Piece.java             ← Piece model
│       ├── PieceType.java         ← Enum
│       └── Move.java              ← Move record
└── resources/
    └── images/                    ← ← PUT YOUR PNG FILES HERE
        ├── blackbishop.png
        ├── blackhorse.png          (knight)
        ├── blackking.png
        ├── blackqueen.png
        ├── blackrook.png
        ├── blackpawn.png
        ├── whitebishop.png
        ├── whitehorse.png
        ├── whiteking.png
        ├── whitequeen.png
        ├── whiterook.png
        └── whitepawn.png
```

## Image Naming Convention
| Piece  | Name used |
|--------|-----------|
| King   | `king`    |
| Queen  | `queen`   |
| Rook   | `rook`    |
| Bishop | `bishop`  |
| Knight | `horse`   |  ← matches your blackhorse.png
| Pawn   | `pawn`    |

Full pattern: `{color}{piece}.png`

> **No images?** The game still runs — pieces are drawn as Unicode chess symbols (♔♕♖♗♘♙).

## Requirements
- Java 8 or newer (Swing is built-in — no extra dependencies)

## Compile & Run (command line)

```bash
# From ChessGameSwing/
mkdir -p out

# Compile (include resources on classpath)
javac -d out -sourcepath src src/chess/ChessApp.java

# Copy resources
cp -r resources/* out/

# Run
java -cp out chess.ChessApp
```

## IntelliJ IDEA Setup
1. Open `ChessGameSwing/` as a new project.
2. Mark `src/` as **Sources Root**.
3. Mark `resources/` as **Resources Root**.
4. Run `chess.ChessApp`.

## Eclipse Setup
1. Create a new Java Project.
2. Add `src/` as a source folder.
3. Add `resources/` as a source folder (so images land on the classpath).
4. Run `chess.ChessApp`.