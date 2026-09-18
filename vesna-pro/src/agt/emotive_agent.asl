// 1. IDENTITA' emotive_agent
my_id(3).

{ include("vesna.asl") }

// ==========================================================
// REGOLE LOGICHE
// ==========================================================
can_eat(Me, T, P, D) :- 
    token(Me, T, P) & P >= 0 & (P + D) <= 50 & 
    token(Enemy, _, EnemyP) & Enemy \== Me & EnemyP >= 0 & EnemyP <= 50 & 
    ((Me * 13 + P + D) mod 52) == ((Enemy * 13 + EnemyP) mod 52).

// ==========================================================
// INIZIALIZZAZIONE E TURNI
// ==========================================================
+!start <- 
    joinWorkspace("ludo_game", W); 
    lookupArtifact("board", BoardId); 
    focus(BoardId).

+current_player(Id) : my_id(Id) & not winner(_) <- rollDice.

+dice(D) : my_id(Id) & current_player(Id) & D > 0 <- 
    .print(" I rolled a ", D, ". Looking for the best move...");
    !choose_move(D).

// ==========================================================
// REAZIONI EMOTIVE (Il Trauma)
// ==========================================================
+token(Me, T, P) : my_id(Me) & P >= 0 <- 
    +in_play(T).

+token(Me, T, -1) : my_id(Me) & in_play(T) <- 
    -in_play(T); 
    !react_to_death(T).

// ==========================================================
// SCELTA DELLA MOSSA (Doppia Personalita')
// ==========================================================

// --- LA CATARSI: VENDETTA! ---
@got_eaten[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.5), joy(0.5)]),
    effects([anger(1.0), joy(0.0)]) 
]
+!react_to_death(T) : true <-
    .print(" Emotive Event: Token ", T, " was captured! Anger level spiked to maximum.").

@vendetta[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.9), joy(0.1)]),
    effects([anger(0.0), joy(1.0)]) 
]
+!choose_move(D) : my_id(Me) & can_eat(Me, T, P, D) <-
    .print(" Emotive Move (Catharsis): Token ", T, " captured an enemy! Retaliation successful, calm state restored.");
    moveToken(T, _).

// --- STATO ARRABBIATO ---
// All static traits perfectly locked to 0.5. Decision relies 100% on anger(0.9).
@exit_base_angry[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.9), joy(0.1)]),
    effects([]) 
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) <-
    .print(" Emotive Move (Angry): Driven by anger, aggressively deploying token ", T, " to seek retaliation.");
    moveToken(T, _).

@advance_angry[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.9), joy(0.1)]),
    effects([]) 
]
+!choose_move(D) : my_id(Me) & token(Me, T, P) & P >= 0 & (P + D) <= 57 <-
    .print(" Emotive Move (Angry): Aggressively advancing token ", T, " by ", D, " steps toward the enemies.");
    moveToken(T, _).


// --- STATO CALMO ---
// All static traits perfectly locked to 0.5. Decision relies 100% on joy(0.9).
@exit_base_calm[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.1), joy(0.9)]),
    effects([]) 
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) <-
    .print(" Emotive Move (Calm): Peacefully deploying token ", T, " from the base.");
    moveToken(T, _).

@advance_calm[
    temper([aggression(0.5), safety(0.5), advance(0.5), anger(0.1), joy(0.9)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & token(Me, T, P) & P >= 0 & (P + D) <= 57 <-
    .print(" Emotive Move (Calm): Cautiously advancing token ", T, " by ", D, " steps.");
    moveToken(T, _).


// --- FALLBACK ---
// Restored to 0.0 so VEsNA explicitly ignores this in normal similarity calculations.
@fallback_move[
    temper([aggression(0.0), safety(0.0), advance(0.0), anger(0.0), joy(0.0)]),
    effects([])
]
+!choose_move(D) : true <-
    .print("I pass this turn because I have no valid moves.");
    moveToken(0, _).

// ==========================================================
// FINE GIOCO
// ==========================================================
+winner(W) : my_id(Id) & W == Id <- .print(" I WON!").
+winner(W) : my_id(Id) & W \== Id <- .print( W, " WON... ").