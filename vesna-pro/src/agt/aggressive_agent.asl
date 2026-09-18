// 1. IDENTITA' aggressive
my_id(0).

{ include("vesna.asl") }

can_eat(Me, T, P, D) :- 
    token(Me, T, P) & P >= 0 & (P + D) <= 50 & 
    token(Enemy, _, EnemyP) & Enemy \== Me & EnemyP >= 0 & EnemyP <= 50 & 
    ((Me * 13 + P + D) mod 52) == ((Enemy * 13 + EnemyP) mod 52).

// Calcola la distanza circolare dal nemico più vicino dopo la mossa.
// Il trucco: se la mossa ti fa superare il nemico, la distanza diventa ~51 (pessima!)
dist_to_enemy(Me, AbsNew, MinDist) :-
    .findall(
        Dist, 
        (
            token(E, _, EP) & E \== Me & EP >= 0 & EP <= 50 &
            EAbs = (E * 13 + EP) mod 52 &
            Dist = (EAbs - AbsNew + 52) mod 52 & 
            Dist > 0 // > 0 perché la distanza 0 significa "mangiare" (già gestito da can_eat)
        ), 
        Dists
    ) &
    ( (Dists \== [] & .min(Dists, MinDist)) | (Dists == [] & MinDist = 999) ).

// Trova la pedina che, dopo essersi mossa, atterra più vicino alle spalle di una preda
best_hunting_token(Me, BestT, D) :-
    .findall(
        [Dist, T], // Salviamo in coppia [Distanza, ID_Pedina] per ordinare dalla minore alla maggiore
        (
            token(Me, T, P) & P >= 0 & (P + D) <= 50 & // Caccia solo nella zona bianca
            AbsNew = (Me * 13 + P + D) mod 52 &
            dist_to_enemy(Me, AbsNew, Dist)
        ),
        Options
    ) &
    Options \== [] &
    .min(Options, [BestDist, BestT]). // .min sceglie automaticamente la distanza più corta!

// INIZIALIZZAZIONE E TURNI
+!start <- joinWorkspace("ludo_game", W); lookupArtifact("board", BoardId); focus(BoardId).
+current_player(Id) : my_id(Id) & not winner(_) <- rollDice.
+dice(D) : my_id(Id) & current_player(Id) & D > 0 <- 
    .print(" I rolled a ", D, ". Looking for the best move...");
    !choose_move(D).

// SCELTA DELLA MOSSA (Valutata dal temper di VEsNA)
// PRIORITÀ 1: MANGIARE
@eat_enemy[
    temper([aggression(1.0), safety(0.0), advance(0.5)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & can_eat(Me, T, P, D) <-
    .print(" Aggressive Move : Use ", D, " to EAT an enemy with token ", T, "!");
    moveToken(T, _).

// PRIORITÀ 2: FAR USCIRE UNA PEDINA (Invasione)
@exit_base[
    temper([aggression(0.8), safety(0.2), advance(0.5)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & D == 6 & token(Me, T, -1) <-
    .print(" Aggressive Move : Use the 6 to deploy the token ", T, " on the board!");
    moveToken(T, _).

// PRIORITÀ 3: LA CACCIA uso la logica per muovere la pedina che si avvicina di più senza superare l'avversario!
@hunt_prey[
    temper([aggression(0.6), safety(0.4), advance(0.8)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & best_hunting_token(Me, T, D) <-
    .print(" Aggressive Move : Move the token ", T, " to get closer to the prey.");
    moveToken(T, _).

// PRIORITÀ 4: ENTRARE IN ZONA SICURA (Se la caccia è finita per quel pedone)
@advance_safe[
    temper([aggression(0.2), safety(0.8), advance(0.9)]),
    effects([])
]
+!choose_move(D) : my_id(Me) & token(Me, T, P) & P >= 0 & (P + D) > 50 & (P + D) <= 57 <-
    .print(" Aggressive Move : Move the token ", T, " to get closer to the safe zone.");
    moveToken(T, _).

// PRIORITÀ 5: FALLBACK (No mosse possibili)
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