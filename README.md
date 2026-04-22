# ♟ Chess – Java Swing

A fully playable, single-file Java chess game built with Swing.  
No external libraries required – just a standard JDK.

---

## Features

| Feature | Details |
|---|---|
| **2-Player** | Alternate moves locally on the same machine |
| **vs AI** | Minimax + alpha-beta pruning (depth 3), piece-square table evaluation |
| **Full chess rules** | Castling (both sides), en passant, pawn promotion |
| **Check / Checkmate / Stalemate** | Detected automatically |
| **Threefold repetition** | Triggers an automatic draw |
| **Countdown clocks** | Per-player timer (No clock / 1 / 3 / 5 / 10 / 15 min), chosen at the menu |
| **Move history sidebar** | Scrollable algebraic-notation move list |
| **Evaluation bar** | Live centipawn score bar, toggleable with one click |
| **Legal-move highlighting** | Green dots for empty squares, corner rings for captures |
| **Last-move highlight** | Yellow tint on from/to squares |
| **King-in-check glow** | Red overlay on the king's square |
| **Scalable board** | Resizes with the window |
| **Piece image fallback** | Renders Unicode symbols if images are missing |

---

## Project Structure

```
Chess/
├── ChessGame.java        ← entire game (single file)
└── resources/            ← piece images (PNG, 200×200 recommended)
    ├── whiteking.png
    ├── whitequeen.png
    ├── whiterook.png
    ├── whitebishop.png
    ├── whiteknight.png
    ├── whitepawn.png
    ├── blackking.png
    ├── blackqueen.png
    ├── blackrook.png
    ├── blackbishop.png
    ├── blackknight.png
    └── blackpawn.png
```

> **No images?** The game still works — pieces fall back to Unicode chess symbols (♔♕♖♗♘♙).

---

## Requirements

- **Java 8 or newer** (Swing is built into every standard JDK)
- No Maven, Gradle, or external libraries needed

---

## Running from the Command Line

```bash
# 1. Compile
javac ChessGame.java

# 2. Run
java ChessGame
```

That's it. Both commands must be run from the folder that contains `ChessGame.java`  
and the `resources/` folder.

---

## Running in IntelliJ IDEA

1. Open IntelliJ → **File → Open** → select the folder containing `ChessGame.java`.
2. Right-click `ChessGame.java` in the Project panel → **Run 'ChessGame.main()'**.
3. If IntelliJ asks you to set up a module, choose **"Create module from existing sources"**.
4. Make sure the `resources/` folder sits **next to** `ChessGame.java` (not inside `src/`),  
   or right-click `resources/` → **Mark Directory as → Resources Root**.

---

## Running in VS Code

1. Install the **Extension Pack for Java** from the VS Code Marketplace.
2. Open the folder containing `ChessGame.java` (**File → Open Folder**).
3. Click the **▶ Run** button that appears above the `main` method,  
   or press `F5`.
4. Make sure `resources/` is in the same folder as `ChessGame.java`.

---

## Forking & Contributing

```bash
# Clone (replace URL with your fork if needed)
git clone https://github.com/your-username/chess.git
cd chess

# Compile & run
javac ChessGame.java
java ChessGame
```

Everything lives in `ChessGame.java`, so you can edit it directly —  
no build system to configure.

---

## How to Play

1. Launch the game → choose **Play vs Friend** or **Play vs AI** and a clock setting.
2. **Click** a piece to select it (legal destinations light up).
3. **Click** a highlighted square to move.
4. The **Eval Bar** on the right shows who's winning (toggle with the button below it).
5. The **Move History** panel logs every move in algebraic notation.
6. The game ends on **checkmate**, **stalemate**, **threefold repetition**, or **clock expiry**.

---

## AI Notes

The AI plays as Black and uses:
- **Minimax search** with **alpha-beta pruning** at depth 3
- **Piece-square tables** for positional evaluation
- Auto-promotes pawns to Queen during search

Increase `AI_DEPTH` in the source for a stronger (but slower) engine.