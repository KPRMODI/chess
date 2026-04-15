# Chess – JavaFX

A fully playable two-player chess game built with JavaFX.

## Features
- Full chess rules (castling, en passant, pawn promotion)
- Check, checkmate & stalemate detection
- Legal-move highlighting (green squares)
- King-in-check highlighting (red square)
- Coordinate labels (a–h / 1–8)
- Play-again prompt on game over

## Project Structure

```
ChessGame/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── module-info.java
    │   └── chess/
    │       ├── ChessApp.java      ← JavaFX Application entry point
    │       ├── ChessBoard.java    ← UI / rendering / input
    │       ├── GameLogic.java     ← Move generation, check detection
    │       ├── Piece.java         ← Piece model
    │       ├── PieceType.java     ← Enum (KING, QUEEN, …)
    │       └── Move.java          ← Move record
    └── resources/
        ├── chess/
        │   └── chess.css
        └── images/               ← ← PUT YOUR PNG FILES HERE
            ├── blackbishop.png
            ├── blackhorse.png      (knight)
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
The code maps piece types to filenames like this:

| Piece  | Filename suffix |
|--------|----------------|
| King   | `king`         |
| Queen  | `queen`        |
| Rook   | `rook`         |
| Bishop | `bishop`       |
| Knight | `horse`        |  ← matches your "blackhorse.png"
| Pawn   | `pawn`         |

Full pattern: `{color}{type}.png`  e.g. `blackhorse.png`, `whiteking.png`

> **Fallback**: If an image file is not found, the piece is drawn using a Unicode chess symbol, so the game still runs without any images.

## Requirements
- Java 17+
- Maven 3.8+
- JavaFX 21 (pulled automatically by Maven)

## Run

```bash
# From the ChessGame/ directory:
mvn javafx:run
```

Or build a fat jar and run:

```bash
mvn package
java --module-path <path-to-javafx-sdk>/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/chess-javafx-1.0-SNAPSHOT-shaded.jar
```

## IDE Setup (IntelliJ / Eclipse)
1. Open as a Maven project.
2. Ensure the JDK is set to Java 17+.
3. Run `chess.ChessApp` as the main class.
4. If JavaFX is not bundled with your JDK, add the JavaFX SDK VM options:
   ```
   --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
   ```
