// 1. IDENTITÀ balanced_agent.asl
my_id(2).

{ include("vesna.asl") }

can_eat(Me, T, P, D) :- 
    token(Me, T, P) & P >= 0 & (P + D) <= 50 & 
    token(Enemy, _, EnemyP) & Enemy \== Me & EnemyP >= 0 & EnemyP <= 50 & 
    ((Me * 13 + P + D) mod 52) == ((Enemy * 13 + EnemyP) mod 52).

is_safe_move(Me, T, P, D) :- 
    token(Me, T, P) & P >= 0 & (P + D) > 50 & (P + D) <= 57.

// L'agente sa contare quante pedine ha in gioco (non a casa e non in base)
active_tokens(Me, Count) :- 
    .count(token(Me, _, P) & P >= 0 & P < 57, Count).


// INIZIALIZZAZIONE E TURNI
+!start <- joinWorkspace("ludo_game", W); lookupArtifact("board", BoardId); focus(BoardId).
+current_player(Id) : my_id(Id) & not winner(_) <- rollDice.

+dice(D) : my_id(Id) & current_player(Id) & D > 0 <- 
    .print(" I rolled a ", D, ". Looking for the best move...");
    !choose_move(D).

// SCELTA DELLA MOSSA
// PRIORITÀ : MANGIARE UN AVVERSARIO
@eat_enemy[
    temper([aggression(0.7), safety(0.3), advance(0.5)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & can_eat(Me, T, P, D) <-
    .print(" Balanced Move : Use the token ", T, " to eat an enemy.");
    moveToken(T, _).

// PRIORITÀ : METTERSI AL SICURO
@reach_safety[
    temper([aggression(0.2), safety(0.8), advance(0.8)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & is_safe_move(Me, T, P, D) <-
    .print(" Balanced Move: I put the token ", T, " in safety.");
    moveToken(T, _).

// PRIORITÀ ALTA: Uscire dalla base SE si hanno meno di 2 pedine in gioco
// VEsNA ha un match perfetto col carattere Bilanciato (0.5), quindi sceglierà sempre questa.
@exit_base_urgent[
    temper([aggression(0.5), safety(0.5), advance(0.5)]), 
    effects([])
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) & active_tokens(Me, C) & C < 2 <-
    .print(" Balanced Move (Army): I have only ", C, " tokens out! I use the 6 to deploy the token ", T, "!");
    moveToken(T, _).

// PRIORITÀ BASSA: Uscire dalla base se si hanno GIÀ 2 o più pedine in gioco
// Diventa una mossa più aggressiva, quindi VEsNA le darà un peso minore rispetto all'avanzare.
@exit_base_normal[
    temper([aggression(0.8), safety(0.2), advance(0.4)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) <-
    .print(" Balanced Move: I already have a good army, but I use the 6 to get the token ", T, " out of the base.");
    moveToken(T, _).

// PRIORITÀ: AVANZARE COSTANTEMENTE (Preferita quando si hanno già 2 pedine in gioco)
@advance_token[
    temper([aggression(0.4), safety(0.6), advance(0.7)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & token(Me, T, P) & P >= 0 & (P + D) <= 57 <-
    .print(" Balanced Move: I distribute the risks. I advance the token ", T, " by ", D, " steps.");
    moveToken(T, _).

// FALLBACK (Nessuna mossa possibile)
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