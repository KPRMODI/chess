# Name(s)

Karan Modi

# Project Title

Chess

# What are you building?

I am building an interative chess game in java. The user can either play against the engine (CPU/AI) or against a friend alternating moves. This is just a fun game as I really do enjoy chess.

# Why is this a Java 2 project?

Chess is a very complicated game to program normally, so I will have to use lists to make the chess board, OOP and inheritance for the chess pieces, recursion for the engine/AI, and Hash Maps for the AI too so it can quickly access the position (O(1)). There are other possibly more concepts that I would have to add for the game to be functional that I am not even aware of because of how many custom rules and variations there could be. With my Java I knowledge, I probably could not even do this as I haven't learned algorithms or recursion yet.

# What is your MVP?

a MVP would be the chess board being complete and filled with pieces, the pieces being able to move and being able to capture each other. For simplicity an Engine/AI wouldn't be created yet and you can only alternate moves with yourself or with a friend playing. A win condition of checkmake, stalemate, or a draw would have to occur to prevent the console from running for too long or games being too long (Move limit?). 

# Stretch Goals (optional)

I would add an evaluation bar setting if the user wants to see which side is better in their position, a side bar of the moves played by both sides, and above that would say which opening was played. These are not needed but cool features I would like to add, but would probably require a database or other files which could fit into file I/O.

# Solo or Pair?

Solo.

# AI Use Plan

I will definetely have to use AI for some of the very difficult parts of chess like the Engine, specific interations (en passant, castle). If I had to take a guess, 80% of the code will be AI generated for thing like that, while I can check and program some of the easier things like the chess board itself.