package mnkgame;

import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 *
 * java -cp ".." mnkgame.MNKGame 4 4 4 mnkgame.OurPlayer
 *
 * **/

public class GameTree {
    private Node root;
    private int rows, columns, target;
    private boolean first;
    private final MNKCellState MY_STATE, OPPONENT_STATE;
    private final MNKGameState WIN_STATE, LOSS_STATE;
    private final int WIN_SCORE, LOSS_SCORE, DRAW_SCORE;

    private static int MAX_DEPTH = 6;

    final int WIN_MOVE, BLOCK_MOVE1, BLOCK_MOVE2_DANGEROUS, BLOCK_MOVE2;

    public GameTree(int M, int N, int K, boolean first) {
        this.root = null;
        this.rows = M;
        this.columns = N;
        this.target = K;

        this.first = first;
        this.MY_STATE = first ? MNKCellState.P1 : MNKCellState.P2;
        this.OPPONENT_STATE = first ? MNKCellState.P2 : MNKCellState.P1;
        this.WIN_STATE = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        this.LOSS_STATE = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

        this.WIN_SCORE = 1000000;
        this.LOSS_SCORE = -1000000;
        this.DRAW_SCORE = 0;

        this.WIN_MOVE = target * 10000;
        this.BLOCK_MOVE1 = target * 1000;
        this.BLOCK_MOVE2_DANGEROUS = target * 200; // Mosse che portano a vicoli ciechi
        this.BLOCK_MOVE2 = target * 100;
    }

    public boolean isEmpty() {
        return root == null;
    }

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    private int alphabeta(Node node, boolean myNode, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;
            for (Node child : node.children) { child.alphabeta = false; }

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
     * Restituisce il punteggio di un nodo contenente uno stato di gioco terminale
     * @implNote Costo: O(1)
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
    }

    /**
     * Imposta un punteggio euristico al nodo
     * @implNote Costo: O(M*N*K)
     */
    private void setHeuristicScoreOf(Node node, BoardStatus board, MNKCellState whoHasToPlay) {
        int playerScore=0, opponentScore=0;
        int score = 0;

        board.generateGlobalMovesToWin();                                                                       // O(M*N*K)
        int[] playerPossibilities = board.getAllPossibleWinningScenariosCount(MY_STATE);                        // O(M*N)
        int[] opponentPossibilities = board.getAllPossibleWinningScenariosCount(OPPONENT_STATE);                // O(M*N)

        int weight = 10000;
        for (int i=1; i<=2 && i<playerPossibilities.length; i++) {
            playerScore += playerPossibilities[i] * weight;
            opponentScore += opponentPossibilities[i] * weight;
            weight = weight / 10;
        }

        if (playerScore == 0 && opponentScore == 0 && playerPossibilities.length > 3) {
            for (int i=3; i<=4 && i<playerPossibilities.length; i++) {
                playerScore += playerPossibilities[i] * weight;
                opponentScore += opponentPossibilities[i] * weight;
                weight = weight / 10;
            }
        }

        /**
         * Rivedere coso ^^^^^^^^^^^^^^^^
         * */

        if (whoHasToPlay == MY_STATE && playerPossibilities[1] != 0) { score = WIN_SCORE; }
        else if (whoHasToPlay == OPPONENT_STATE && opponentPossibilities[1] != 0) { score = LOSS_SCORE; }
        else {
            score = playerScore - opponentScore;
            /*if (playerScore > opponentScore) { score = playerScore; }
            else if (opponentScore > playerScore) { score = -opponentScore; }
            else {
                if (whoHasToPlay == MY_STATE) {
                    score = playerScore;
                }
                else {
                    score = -opponentScore;
                }
            }*/
        }

        node.score = score;
    }

    /**
     * Restituisce una coda con priorità contenente le celle adiacenti a quelle già piazzate, ordinate per importanza
     * @implNote Costo: O(8*h*(max{M, N} * K))
     */
    public PriorityQueue<EstimatedPosition> getAdjacency(Node node, BoardStatus board, MNKCellState state) {
        boolean[][] visited = new boolean[columns][rows];
        PriorityQueue<EstimatedPosition> out = new PriorityQueue<>();

        final MNKCellState PLAYING_STATE = state;
        final MNKCellState WAITING_STATE = state == MY_STATE ? OPPONENT_STATE : MY_STATE;

        Node iter = node;

        while (iter != null) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) { continue; }

                    int toVisit_x = iter.action.j + j;
                    int toVisit_y = iter.action.i + i;

                    if (!isValidCell(toVisit_x, toVisit_y) || !board.isFreeAt(toVisit_x, toVisit_y) || visited[toVisit_x][toVisit_y]) { continue; }
                    visited[toVisit_x][toVisit_y] = true;

                    /**
                     *
                     * - Se posso vincere, vinco
                     * - Se ci sono minacce (fino a 2 mosse mancanti), blocco
                     * - Prendo gli allineamenti maggiori
                     *
                     * */

                    board.generateMovesToWinAt(toVisit_x, toVisit_y);
                    int playerMovesToWin = board.getMovesToWinAt(toVisit_x, toVisit_y, PLAYING_STATE);
                    int oppositeMovesToWin = board.getMovesToWinAt(toVisit_x, toVisit_y, WAITING_STATE);
                    EstimatedPosition estimation = null;

                    if (playerMovesToWin == 1) {
                        estimation = new EstimatedPosition(toVisit_x, toVisit_y, WIN_MOVE);
                    } else if (oppositeMovesToWin == 1) {
                        estimation =  new EstimatedPosition(toVisit_x, toVisit_y, BLOCK_MOVE1);
                    } else if (oppositeMovesToWin == 2) {
                        // Controllo la situazione se l'altro mette la mossa in questa posizione
                        board.setAt(toVisit_x, toVisit_y, WAITING_STATE);
                        board.generateMovesToWinAt(toVisit_x, toVisit_y);
                        int[] possibilities = board.getAllPossibleWinningScenariosCountAt(toVisit_x, toVisit_y, WAITING_STATE);
                        board.removeAt(toVisit_x, toVisit_y);

                        if (possibilities[1] > 1) {
                            estimation = new EstimatedPosition(toVisit_x, toVisit_y, BLOCK_MOVE2_DANGEROUS);
                        } else {
                            //estimation = new EstimatedPosition(toVisit_x, toVisit_y, BLOCK_MOVE2);
                        }
                    }

                    if (estimation == null) {
                        int aligned = target - playerMovesToWin + 1;
                        //int blocked = target - oppositeMovesToWin;
                        estimation = new EstimatedPosition(toVisit_x, toVisit_y, aligned);
                    }

                    out.add(estimation);
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
     * @implNote Costo:
     * */
    private Node createTree(Node parentNode, boolean mePlaying, int depth, BoardStatus board) {
        board.generateMovesToWinAt(parentNode.action.j, parentNode.action.i);                               // O(max{M, N}*K)
        MNKGameState gameState = board.statusAt(parentNode.action.j, parentNode.action.i);

        if (gameState != MNKGameState.OPEN) {
            setScoreOf(parentNode, gameState);
            parentNode.endState = true;
        }
        else if (depth <= 0) {
            setHeuristicScoreOf(parentNode, board, mePlaying ? MY_STATE : OPPONENT_STATE);              // O(M*N*K)
        }
        else {
            LinkedList<EstimatedPosition> list = new LinkedList<>();
            PriorityQueue<EstimatedPosition> moves = getAdjacency(parentNode, board, mePlaying ? MY_STATE : OPPONENT_STATE);

            int i=0;
            int score = moves.peek().score;
            while (moves.size() > 0) {
                if (moves.peek().score < BLOCK_MOVE2_DANGEROUS && i >= 5) { break; }
                /*if (moves.peek().score != score) {
                    if (moves.peek().score < target * 20 && i >= 4) { break; }
                }*/
                //if (moves.peek().score != score || (moves.peek().score < target * 20 && i>=3)) { break; }
                //if (moves.peek().score != score) { break; }

                list.add(moves.poll());
                i++;
            }

            /*System.out.println(board);
            System.out.println(mePlaying ? "ME" : "OPP");
            for (EstimatedPosition pe : list) {
                System.out.print(pe + " ");
            }
            System.out.println();
            System.out.println();*/

            for (EstimatedPosition toVisit : list) {
                int toVisit_x = toVisit.x;
                int toVisit_y = toVisit.y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(parentNode, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                parentNode.children.add( createTree(child, !mePlaying, depth-1, board) );
                board.removeAt(toVisit_x, toVisit_y);
            }
        }


        return parentNode;
    }

    /**
     * Genera l'albero di gioco iniziale
     * @implNote Costo:
     * */
    public void generate(MNKCell firstMove) {
        root = new Node(null, firstMove);

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        board.setAt(firstMove.j, firstMove.i, firstMove.state);

        createTree(root, !first, MAX_DEPTH, board);
        alphabeta(root, first, LOSS_SCORE, WIN_SCORE);
    }

    /**
     * Estende di una determinata profodità l'albero radicato nel nodo indicato
     * @param node Nodo da estendere
     * @implNote Costo:
     * */
    public void extendNode(Node node, int depth) {
        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        for (MNKCell cell : node.getMarkedCells()) {
            board.setAt(cell.j, cell.i, cell.state);
        }

        boolean mePlaying = node.action.state == MY_STATE;
        createTree(node, !mePlaying, depth, board);
    }

    /**
     * Estende di un livello di profondità tutte le foglie dell'albero radicato nel nodo indicato
     * @param node Nodo di partenza
     * @implNote Costo:
     * */
    private void extendLeaves(Node node) {
        if (node.isLeaf() && !node.endState) {
            extendNode(node, 1);
        }
        else {
            for (Node child : node.children) {
                extendLeaves(child);
            }
        }
    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa corrispondente
     * @param move Mossa dell'avversario
     * @implNote Costo:
     * */
    public void setOpponentMove(MNKCell move) {
        Node bestChild = null;

        for (Node child : root.children) {
            if (child.action.equals(move)) {
                bestChild = child;
                break;
            }
        }

        if (bestChild == null) {
            Node new_root = new Node(root, move);
            root.setSelectedChild(new_root);
            root = new_root;
            extendNode(this.root, MAX_DEPTH);
            alphabeta(this.root, this.root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        }
        else {
            root.setSelectedChild(bestChild);
            root = bestChild;
            extendLeaves(root);
            alphabeta(root, root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        }

    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa migliore
     * @return Mossa da eseguire
     * @implNote Costo:
     * */
    public MNKCell nextMove() {
        Node nextChild = root.children.peek(); /*** TODO RIMETTERE POLL **/

        for (Node child : root.children) {
            System.out.println(child.action + " " + child.score + " " + child.alphabeta);
            if (child.score > nextChild.score && child.alphabeta) {
                nextChild = child;
            }
        }
        System.out.println();

        root.setSelectedChild(nextChild);
        root = nextChild;

        extendLeaves(root);
        alphabeta(root, root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        return root.action;
    }
}
