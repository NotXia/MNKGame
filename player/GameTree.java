package player;

import java.util.HashMap;
import java.util.PriorityQueue;
import mnkgame.*;

public class GameTree {
    private Node root;
    private int rows, columns, target;
    private boolean first;
    private boolean canExtend; // Indica se è possibile estendere le foglie dell'albero

    private final MNKCellState MY_STATE, OPPONENT_STATE;
    private final MNKGameState WIN_STATE, LOSS_STATE;
    private final int WIN_SCORE, LOSS_SCORE, DRAW_SCORE;

    private int MAX_HEIGHT;     // Altezza dell'albero da mantenere a partire dall'attuale radice
    private int EXTEND_HEIGHT;  // Numero di livelli da generare quando si estende l'albero
    private final int MAX_EVAL; // Numero massimo di mosse da valutare per nodo (nel caso di mosse non critiche)

    private final int PRIORITY_1, PRIORITY_2, PRIORITY_3, PRIORITY_4;

    /**
     * @implNote Costo: Θ(1)
     * */
    public GameTree(int M, int N, int K, boolean first) {
        this.root = null;
        this.rows = M;
        this.columns = N;
        this.target = K;

        this.first = first;
        this.canExtend = false;
        this.MY_STATE = first ? MNKCellState.P1 : MNKCellState.P2;
        this.OPPONENT_STATE = first ? MNKCellState.P2 : MNKCellState.P1;
        this.WIN_STATE = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        this.LOSS_STATE = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

        this.EXTEND_HEIGHT = 2;
        this.MAX_EVAL = 3;

        // Calcolo dell'altezza ottimale
        int height = 1;
        double x;
        do {
            x = Math.pow(MAX_EVAL, height) * height * (M*K + N*K + Math.log10(height));
            height += 2;
        }
        while (x < 10000000);
        this.MAX_HEIGHT = height-2;

        this.WIN_SCORE = 10000000;
        this.LOSS_SCORE = -10000000;
        this.DRAW_SCORE = 0;

        this.PRIORITY_1 = target * 10000;
        this.PRIORITY_2 = target * 1000;
        this.PRIORITY_3 = target * 200;
        this.PRIORITY_4 = target * 100;
    }

    /**
     * @implNote Costo: Θ(1)
     * */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * @implNote Costo: Θ(1)
     * */
    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    /**
     * @implNote Costo (pessimo): O(p^h)         p = numero medio di mosse  |  h = altezza albero
     * @implNote Costo (ottimo): O(sqrt(p^h))
     * */
    private int alphabeta(Node node, boolean myNode, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;
            for (Node child : node.children) { child.alphabeta = false; } // Marca tutti i nodi come non elaborati da alphabeta

            if (myNode) {
                eval = Integer.MAX_VALUE;
                for (Node child : node.children) {
                    eval = Math.min(eval, alphabeta(child, false, alpha, beta));
                    beta = Math.min(eval, beta);
                    child.alphabeta = true;
                    if (beta <= alpha) { break; }
                }
            }
            else {
                eval = Integer.MIN_VALUE;
                for (Node child : node.children) {
                    eval = Math.max(eval, alphabeta(child, true, alpha, beta));
                    alpha = Math.max(eval, alpha);
                    child.alphabeta = true;
                    if (beta <= alpha) { break; }
                }
            }

            node.score = eval;
            return eval;
        }
    }

    /**
     * Imposta il punteggio di un nodo contenente uno stato di gioco terminale
     * @implNote Costo: Θ(1)
     * */
    private void setScoreOf(Node node, MNKGameState gameState) {
        if (gameState == WIN_STATE) {
            node.score = WIN_SCORE;
        }
        else if (gameState == LOSS_STATE) {
            node.score = LOSS_SCORE;
        }
        else {
            node.score = DRAW_SCORE;
        }
        node.endState = true;
    }

    /**
     * Imposta un punteggio euristico alla configurazione di un nodo
     * @implNote Costo: O(M*N*K)
     */
    private void setHeuristicScoreOf(Node node, BoardStatus board, MNKCellState whoHasToPlay) {
        int playerScore = 0, opponentScore = 0;
        int score = 0;

        board.generateGlobalMovesToWin();                                                                       // O(M*N*K)
        int[] playerPossibilities = board.getAllPossibleWinningScenariosCount(MY_STATE);                        // O(M*N)
        int[] opponentPossibilities = board.getAllPossibleWinningScenariosCount(OPPONENT_STATE);                // O(M*N)

        /*
        * Calcola il punteggio considerando il numero di scenari a cui mancano da 1 a 3 mosse per vincere
        * */
        int[] weight = new int[]{ 0, 100000, 1000, 1 };
        for (int i=1; i<=3 && i<playerPossibilities.length; i++) {
            playerScore += playerPossibilities[i] * weight[i];
            opponentScore += opponentPossibilities[i] * weight[i];
        }

        if (whoHasToPlay == MY_STATE && playerPossibilities[1] != 0) { // Vittoria immediata
            score = WIN_SCORE;
        }
        else if (whoHasToPlay == OPPONENT_STATE && opponentPossibilities[1] != 0) { // Sconfitta immediata
            score = LOSS_SCORE;
        }
        else { // Valuazione euristica
            score = playerScore - opponentScore;
        }

        node.score = score;
    }

    /**
     * Restituisce una coda con priorità contenente le celle adiacenti a quelle già piazzate, ordinate per importanza
     * @implNote Costo: O( h(MK + NK) + h*log(h) ) = O( h(MK + NK + log(h)) )
     */
    private PriorityQueue<EstimatedPosition> getAdjacency(Node node, BoardStatus board, MNKCellState state) {
        HashMap<Coord, Boolean> visited = new HashMap<>();
        PriorityQueue<EstimatedPosition> out = new PriorityQueue<>();

        final MNKCellState PLAYING_STATE = state;
        final MNKCellState WAITING_STATE = state == MY_STATE ? OPPONENT_STATE : MY_STATE;

        Node iter = node;

        // Per ogni mossa, valuta le celle circostanti libere
        while (iter != null) {                                                                                                      // -|
            for (int i = -1; i <= 1; i++) {                                                                                         //  | O(8 * h) = O(h)
                for (int j = -1; j <= 1; j++) {                                                                                     //  | h = altezza albero
                    if (i == 0 && j == 0) { continue; }                                                                             // -|

                    int toVisit_x = iter.action.j + j;
                    int toVisit_y = iter.action.i + i;
                                                                                                                                    // O(1) utilizzando il costo medio delle hash table
                    if (!isValidCell(toVisit_x, toVisit_y) || !board.isFreeAt(toVisit_x, toVisit_y) || visited.get(new Coord(toVisit_x, toVisit_y)) != null) { continue; }
                    visited.put(new Coord(toVisit_x, toVisit_y), true);                                                             // O(1) utilizzando il costo medio delle hash table

                    /*
                     * Ordine di priorità:
                     * - Mossa vincente
                     * - Blocco una mossa vincente dell'avversario
                     * - Imposto un vicolo cieco a mio favore
                     * - Blocco un vicolo cieco dell'avversario
                     * - Scelgo la mossa (possibilmente) migliore per me
                     * */

                    board.generateMovesToWinAt(toVisit_x, toVisit_y);                                                               // O(MK + NK)
                    int currentPlayerMovesToWin = board.getMovesToWinAt(toVisit_x, toVisit_y, PLAYING_STATE);
                    int oppositeMovesToWin = board.getMovesToWinAt(toVisit_x, toVisit_y, WAITING_STATE);

                    EstimatedPosition estimation = null;

                    // Mossa vincente per me
                    if (currentPlayerMovesToWin == 1) {
                        estimation = new EstimatedPosition(toVisit_x, toVisit_y, PRIORITY_1);
                    }
                    // Blocca mossa vincente dell'avversario
                    else if (oppositeMovesToWin == 1) {
                        estimation =  new EstimatedPosition(toVisit_x, toVisit_y, PRIORITY_2);
                    }

                    // Cerco un vicolo cieco a mio favore
                    if (currentPlayerMovesToWin == 2) {
                        board.setAt(toVisit_x, toVisit_y, PLAYING_STATE);                                                           // O(M + N)
                        board.generateMovesToWinAt(toVisit_x, toVisit_y);                                                           // O(MK + NK)
                        int[] possibilities = board.getAllPossibleWinningScenariosCountAt(toVisit_x, toVisit_y, PLAYING_STATE);     // O(M + N)
                        board.removeAt(toVisit_x, toVisit_y);                                                                       // O(M + N)

                        if (possibilities[1] > 1) {
                            estimation = new EstimatedPosition(toVisit_x, toVisit_y, PRIORITY_3);
                        }
                    }

                    // Cerco un vicolo cieco a mio sfavore
                    if (estimation == null && oppositeMovesToWin == 2) {
                        board.setAt(toVisit_x, toVisit_y, WAITING_STATE);                                                           // O(M + N)
                        board.generateMovesToWinAt(toVisit_x, toVisit_y);                                                           // O(MK + NK)
                        int[] possibilities = board.getAllPossibleWinningScenariosCountAt(toVisit_x, toVisit_y, WAITING_STATE);     // O(M + N)
                        board.removeAt(toVisit_x, toVisit_y);                                                                       // O(M + N)

                        if (possibilities[1] > 1) {
                            estimation = new EstimatedPosition(toVisit_x, toVisit_y, PRIORITY_4);
                        }
                    }

                    // Valuto la qualità della mossa non critica
                    if (estimation == null) {
                        int aligned = target - currentPlayerMovesToWin + 1;
                        int blocked = target - oppositeMovesToWin;

                        estimation = new EstimatedPosition(toVisit_x, toVisit_y, aligned, blocked);
                    }

                    out.add(estimation);                                                                                            // Costo complessivo: O( log((8h)!) ) = O(log(h!)) = O( h*log(h) )
                }
            }
            iter = iter.parent;
        }

        return out;
    }

    /**
     * Genera l'albero di gioco fino a una determinata profondità
     * @param parentNode Nodo radice
     * @param depth Profondità di generazione
     * @param board Mantiene memorizzata la situazione attuale della griglia
     * @implNote Costo: O( p^depth * h(MK + NK + log(h)) )   h = altezza albero  |  p = Numero di iterazioni
     * */
    private Node createTree(Node parentNode, boolean mePlaying, int depth, BoardStatus board) {
        board.generateMovesToWinAt(parentNode.action.j, parentNode.action.i);                                                   // O(MK + NK)
        MNKGameState gameState = board.statusAt(parentNode.action.j, parentNode.action.i);
        MNKCellState curr_state = mePlaying ? MY_STATE : OPPONENT_STATE;

        if (gameState != MNKGameState.OPEN) {
            setScoreOf(parentNode, gameState);
        }
        else if (depth <= 0) {
            setHeuristicScoreOf(parentNode, board, curr_state);                                                                 // O(M*N*K)
        }
        else {
            PriorityQueue<EstimatedPosition> moves = getAdjacency(parentNode, board, mePlaying ? MY_STATE : OPPONENT_STATE);    // O( h(MK + NK + log(h)) )

            int i=0;
            int score = moves.peek().score;
            while (moves.size() > 0) {                                                                                          // O(p) p = Numero di iterazioni -> p ~ [MAX_EVAL]
                // - Per le mosse critiche valuto tutte quelle che hanno lo stesso score e termino quando ne trovo una diversa
                //   (idea di base: se devo bloccare/vincere non dovrò preoccuparmi di fare altro)
                if (moves.peek().score >= PRIORITY_4 && moves.peek().score != score) { break; }
                // - Per le mosse di altro tipo ne estraggo un paio (tra le più promettenti) e le valuto
                if (moves.peek().score < PRIORITY_4 && i >= MAX_EVAL) { break; }

                EstimatedPosition toVisit = moves.poll();                                                                       // O(log(q)) q = dimensione coda

                MNKCell toEvalCell = new MNKCell(toVisit.y, toVisit.x, curr_state);
                Node child = new Node(parentNode, toEvalCell);

                board.setAt(toVisit.x, toVisit.y, curr_state);                                                                  // O(M + N)
                parentNode.children.add( createTree(child, !mePlaying, depth-1, board) );
                board.removeAt(toVisit.x, toVisit.y);                                                                           // O(M + N)

                i++;
            }
        }

        return parentNode;
    }

    /**
     * Genera l'albero di gioco iniziale
     * @implNote Costo: O( h(MK + NK + log(h)) )
     * */
    public void generate(MNKCell firstMove) {
        root = new Node(null, firstMove);

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);       // Θ(M*N)
        board.setAt(firstMove.j, firstMove.i, firstMove.state);                     // Θ(1) [Dato che board è appena stato istanziato]

        createTree(root, !first, MAX_HEIGHT, board);                                // O( [MAX_EVAL]^[MAX_HEIGHT] * h(MK + NK + log(h)) ) = O( h(MK + NK + log(h)) )
        alphabeta(root, first, LOSS_SCORE, WIN_SCORE);                              // O([MAX_EVAL]^[MAX_HEIGHT]]) = O(c)
    }

    /**
     * Estende di una determinata profodità l'albero radicato nel nodo indicato
     * @param node Nodo da estendere
     * @implNote Costo: O( p^depth * h(MK + NK + log(h)) )
     * */
    private void extendNode(Node node, int depth) {
        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);

        // Riempie board con le mosse piazzate fino alla configurazione corrente del nodo
        Node iter = node;
        while (iter != null) {                                                      // O(h)
            board.setAt(iter.action.j, iter.action.i, iter.action.state);           // O(M+N)
            iter = iter.parent;
        }

        boolean mePlaying = node.action.state == MY_STATE;
        createTree(node, !mePlaying, depth, board);                                 // O( p^depth * h(MK + NK + log(h)) )
    }

    /**
     * Estende tutte le foglie dell'albero radicato nel nodo indicato
     * @param node Nodo di partenza
     * @implNote Costo (pessimo): O( [MAX_EVAL]^[MAX_HEIGHT] * h(MK + NK + log(h)) ) = O( h(MK + NK + log(h)) )
     * */
    private void extendLeaves(Node node) {
        if (node.isLeaf() && !node.endState) {
            extendNode(node, EXTEND_HEIGHT);                                              // O( [MAX_EVAL]^[EXTEND_HEIGHT] * h(MK + NK + log(h)) ) =  O( h(MK + NK + log(h)) )
        }
        else {
            for (Node child : node.children) {
                extendLeaves(child);
            }
        }
    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa dell'avversario corrispondente
     * @param move Mossa dell'avversario
     * @implNote Costo (pessimo): O( h(MK + NK + log(h)) )
     *           Costo (ottimo): O(c)   c = costante
     * */
    public void setOpponentMove(MNKCell move) {
        Node bestChild = null;

        // Cerco il figlio con la mossa dell'avversario
        for (Node child : root.children) {                                                              // O([MAX_EVAL]) = O(c)
            if (child.action.equals(move)) {
                bestChild = child;
                break;
            }
        }

        // Se la mossa dell'avversario non era tra le mie previste
        if (bestChild == null) {
            // Creo un nuovo nodo e genera il sotto-albero radicato
            Node new_root = new Node(root, move);
            root.setAsOnlyChild(new_root);
            root = new_root;
            extendNode(this.root, first ? MAX_HEIGHT+1 : MAX_HEIGHT);                                   // O( [MAX_EVAL]^[MAX_HEIGHT] * h(MK + NK + log(h)) ) = O( h(MK + NK + log(h)) )
            alphabeta(this.root, this.root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);      // O([MAX_EVAL]^[MAX_HEIGHT]]) = O(c)

            canExtend = false;
        }
        else {
            // Sposto la radice
            root.setAsOnlyChild(bestChild);
            root = bestChild;

            // Eventualmente estendo
            if (canExtend) {
                extendLeaves(root);                                                                     // O( h(MK + NK + log(h)) )
                alphabeta(root, root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);            // O([MAX_EVAL]^[MAX_HEIGHT]]) = O(c)
            }
            canExtend = !canExtend;
        }
    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa migliore
     * @return Mossa da eseguire
     * @implNote Costo (pessimo): O( h(MK + NK + log(h)) ) <br/>
     *           Costo (ottimo): O(c)   c = costante
     * */
    public MNKCell nextMove() {
        // Cerco il figlio con il punteggio maggiore
        Node nextChild = root.children.peek();
        for (Node child : root.children) {                                                          // O([MAX_EVAL]) = O(c)
            if (child.score > nextChild.score && child.alphabeta) {
                nextChild = child;
            }
        }

        // Sposto la radice
        root.setAsOnlyChild(nextChild);
        root = nextChild;

        // Eventualmente estendo
        if (canExtend) {
            extendLeaves(root);                                                                     // O( h(MK + NK + log(h)) )
            alphabeta(root, root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);            // O([MAX_EVAL]^[MAX_HEIGHT]]) = O(c)
        }
        canExtend = !canExtend;

        return root.action;
    }
}
