I've reorganized the documentation for you. It now prioritizes getting the project onto your local machine first, followed by the specific setup for IDEs, and finally the command-line execution.

---

# ♟ Chess – Java Swing

A fully playable, single-file Java chess game built with Swing.  
No external libraries required – just a standard JDK.

## 1. Getting the Project

Before running the game, you need to bring the code onto your machine.

**Option A: Using Git (Recommended)**
```bash
# Clone the repository
git clone https://github.com/your-username/chess.git

# Enter the directory
cd chess
```

**Option B: Manual Download**
1. Download the ZIP file from the repository.
2. Extract the contents to a folder of your choice.
3. Ensure the `ChessGame.java` file and the `resources/` folder are in the same directory.

---

## 2. Setting Up Your IDE

### **IntelliJ IDEA**
1. Open IntelliJ → **File → Open** → select the folder you just cloned/extracted.
2. If IntelliJ asks to set up a module, choose **"Create module from existing sources"**.
3. **Important:** Right-click the `resources/` folder in the Project panel → **Mark Directory as → Resources Root**.
4. Right-click `ChessGame.java` → **Run 'ChessGame.main()'**.

### **VS Code**
1. Install the **Extension Pack for Java** from the Marketplace.
2. Open the project folder (**File → Open Folder**).
3. Open `ChessGame.java`.
4. Click the **▶ Run** button that appears above the `main` method, or press `F5`.

---

## 3. Running from the Command Line

If you prefer to run without an IDE, use these commands from the root of the project folder:

```bash
# 1. Compile
javac ChessGame.java

# 2. Run
java ChessGame
```

---

## Features & UI

| Feature | Details |
|---|---|
| **2-Player / AI** | Play locally or against a Minimax AI (Depth 3) |
| **Full Rules** | Castling, en passant, pawn promotion, and threefold repetition |
| **Clocks** | Selectable timers (1, 3, 5, 10, 15 min) or no clock |
| **Analysis** | Live evaluation bar and algebraic move history sidebar |
| **Visuals** | Legal move highlighting, last-move tint, and king-in-check glow |



### **Project Structure**
```
Chess/
├── ChessGame.java        ← entire game (single file)
└── resources/            ← piece images (PNG)
```
> **Note:** If the `resources/` folder is missing, the game automatically falls back to Unicode chess symbols (♔♕♖♗).

---

## AI & Mechanics

The AI (playing as Black) evaluates positions using **Piece-Square Tables**. These tables tell the AI which squares are strategically better for specific pieces (e.g., Knights are better in the center than on the rim).

* **Minimax & Alpha-Beta:** The engine calculates the best move by "pruning" branches of the move tree that it knows are suboptimal, saving significant processing time.
* **Difficulty:** You can increase the `AI_DEPTH` constant in the `ChessGame.java` source code for a stronger challenge, though depth 4+ may cause slight delays in move calculation.