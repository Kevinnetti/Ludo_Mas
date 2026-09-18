// 3. IDENTITÀ prudent_agent.asl
my_id(1).

{ include("vesna.asl") }

// Controlla se la mossa porta la pedina nella zona sicura colorata (da 51 a 57)
is_safe_move(Me, T, P, D) :- 
    token(Me, T, P) & P >= 0 & (P + D) > 50 & (P + D) <= 57.


// INIZIALIZZAZIONE E TURNI
+!start <- joinWorkspace("ludo_game", W); lookupArtifact("board", BoardId); focus(BoardId).
+current_player(Id) : my_id(Id) & not winner(_) <- rollDice.
+dice(D) : my_id(Id) & current_player(Id) & D > 0 <- 
    .print("I rolled a ", D, ". Looking for the best move...");
    !choose_move(D).

// SCELTA DELLA MOSSA (Valutata dal temper di VEsNA)
// PRIORITÀ 1: METTERSI AL SICURO (Entrare nel corridoio o vincere) Sicurezza MAx (1.0). Se c'è questa mossa, il Prudente la fa subito.
@reach_safety[
    temper([aggression(0.0), safety(1.0), advance(0.9)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & is_safe_move(Me, T, P, D) <-
    .print(" Prudent Move: I use the ", D, " to put the token ", T, " in safety.");
    moveToken(T, _).

// PRIORITÀ 2: AVANZARE UNA PEDINA GIÀ IN GIOCO Preferisce far correre le pedine già esposte piuttosto che tirarne fuori di nuove.
// Sicurezza alta (0.8).
@advance_token[
    temper([aggression(0.1), safety(0.8), advance(0.8)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & token(Me, T, P) & P >= 0 & (P + D) <= 50 <-
    .print(" Prudent Move: I move the token ", T, " to be safe from the enemies.");
    moveToken(T, _).

// PRIORITÀ 3: FAR USCIRE UNA PEDINA NUOVA
// Sicurezza bassissima (0.2). Lo fa solo se fa 6 e non ha nessuna pedina in gioco.
@exit_base[
    temper([aggression(0.3), safety(0.2), advance(0.4)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) <-
    .print("Prudent Move: I use the 6 to get the token ", T, " out of the base because I have no other options.");
    moveToken(T, _).

// PRIORITÀ 4: FALLBACK (Nessuna mossa possibile)
@fallback_move[
    temper([aggression(0.0), safety(0.0), advance(0.0)]),
    effects([])
]
+!choose_move(D) : true <-
    .print("I pass this turn because I have no valid moves.");
    moveToken(0, _).

// FINE GIOCO
+winner(W) : my_id(Id) & W == Id <- .print(" I WON!").
+winner(W) : my_id(Id) & W \== Id <- .print( W, " WON... ").