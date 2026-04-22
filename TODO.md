# En Passant Fix - TODO List

## Plan Status: ✅ APPROVED

**Step 1: [PENDING]** Add debug logging to `addPawnMoves()` and `handleSquareClick()`  
**Step 2: [PENDING]** Enhance `executeMove()` with en passant tracking and visual fix  
**Step 3: [PENDING]** Test en passant sequence  
**Step 4: [PENDING]** Cleanup debug logs if working  
**Step 5: [DONE]** attempt_completion

**Test Sequence:**
1. White: e2→e4  
2. Black: d7→d5 (sets enPassantCol=3)  
3. White: f2 pawn should show move to e3  
4. Execute → d5 pawn should disappear, f-pawn moves to e3  
5. Console should log "EN PASSANT CAPTURE!"

**Current Progress:** 0/5 completed

