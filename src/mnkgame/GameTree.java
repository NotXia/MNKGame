package mnkgame;
import java.util.HashMap;

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
    private static final int MIN_EVAL = 5;
    public static final int SCORE_THRESHOLD = 2;

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

        this.WIN_SCORE = target + 1;
        this.LOSS_SCORE = -target - 1;
        this.DRAW_SCORE = 0;
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
            for (Node child : node.children) {
                child.alphabeta = false;
            }

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

    private void setHeuristicScoreOf(Node node, BoardStatus board) {
        int playerScore, opponentScore;

        board.removeAt(node.action.j, node.action.i);
        board.generateScoreAt(node.action.j, node.action.i);

        playerScore = board.getMovesToWinAt(node.action.j, node.action.i, MY_STATE);
        opponentScore = board.getMovesToWinAt(node.action.j, node.action.i, OPPONENT_STATE);

        board.setAt(node.action.j, node.action.i, node.action.state);

        playerScore = target - playerScore;
        opponentScore = target - opponentScore;

        if (playerScore > opponentScore) {
            node.score = playerScore;
        }
        else {
            node.score = -opponentScore;
        }
    }

    public HashMap<Coord, Integer> setAdjacencyOf(Node node, BoardStatus board) {
        HashMap<Coord, Integer> newAdjacency = new HashMap<>();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) { continue; }
                int toVisit_x = node.action.j + j;
                int toVisit_y = node.action.i + i;

                if (isValidCell(toVisit_x, toVisit_y) && board.isFreeAt(toVisit_x, toVisit_y)) {
                    board.generateScoreAt(toVisit_x, toVisit_y);

                    int best = Math.min(board.getMovesToWinAt(toVisit_x, toVisit_y, MY_STATE), board.getMovesToWinAt(toVisit_x, toVisit_y, OPPONENT_STATE));
                    node.adjacency.add( new EvaluationPosition(toVisit_x, toVisit_y, best) );

                    newAdjacency.put( new Coord(toVisit_x, toVisit_y), best );
                }
            }
        }

        return newAdjacency;
    }

    public void getParentAdjacencyOf(Node node, BoardStatus board, HashMap<Coord, Integer> newAdjacency) {
        Coord currCoord = new Coord(node.action.j, node.action.i);

        for (EvaluationPosition position : node.parent.adjacency) {
            Coord positionCoord = new Coord(position.x, position.y);

            if (newAdjacency.get(positionCoord) == null && board.isFreeAt(position.x, position.y)) { // Le nuove adiacenze non collidono con quelle del padre
                EvaluationPosition toAdd = new EvaluationPosition(position);
                int updatedScore = toAdd.score;

                if (currCoord.isOnTheSameRowOf(positionCoord)) {
                    updatedScore = Math.min(board.getRowMovesToWinAt(position.x, position.y, MY_STATE), board.getRowMovesToWinAt(position.x, position.y, OPPONENT_STATE));
                }
                else if (currCoord.isOnTheSameColumnOf(positionCoord)) {
                    updatedScore = Math.min(board.getColumnMovesToWinAt(position.x, position.y, MY_STATE), board.getColumnMovesToWinAt(position.x, position.y, OPPONENT_STATE));
                }
                else if (currCoord.isOnTheSameMainDiagonalOf(positionCoord)) {
                    updatedScore = Math.min(board.getMainDiagonalMovesToWinAt(position.x, position.y, MY_STATE), board.getMainDiagonalMovesToWinAt(position.x, position.y, OPPONENT_STATE));
                }
                else if (currCoord.isOnTheSameSecondaryDiagonalOf(positionCoord)) {
                    updatedScore = Math.min(board.getSecondaryDiagonalMovesToWinAt(position.x, position.y, MY_STATE), board.getSecondaryDiagonalMovesToWinAt(position.x, position.y, OPPONENT_STATE));
                }

                toAdd.score = Math.min(toAdd.score, updatedScore);
                node.adjacency.add(toAdd);
            }
        }
    }

    /**
     * Genera l'albero di gioco fino a una determinata profondità
     * @param toEval Nodo radice
     * @param depth Profondità di generazione
     * @param board Mantiene memorizzata la situazione attuale della griglia
     * @implNote Costo:
     * */
    private Node createTree(Node toEval, boolean mePlaying, int depth, BoardStatus board) {
        MNKGameState gameState = board.statusAt(toEval.action.j, toEval.action.i);

        if (gameState != MNKGameState.OPEN) {
            setScoreOf(toEval, gameState);
            toEval.endState = true;
        }
        else if (depth == 0) {
            setHeuristicScoreOf(toEval, board);
        }
        else {
            HashMap<Coord, Integer> newAdjacency = setAdjacencyOf(toEval, board);
            if (toEval.parent != null) {
                getParentAdjacencyOf(toEval, board, newAdjacency);
            }

            PriorityQueue<EvaluationPosition> moves = new PriorityQueue<>();
            for (EvaluationPosition position : toEval.adjacency) {
                moves.add(position);
            }

            int i=0;
            while (moves.size() > 0) {
                if (i >= MIN_EVAL && moves.peek().score > SCORE_THRESHOLD) { break; }

                EvaluationPosition toVisit = moves.poll();
                int toVisit_x = toVisit.x;
                int toVisit_y = toVisit.y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(toEval, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                toEval.children.add( createTree(child, !mePlaying, depth-1, board) );
                board.removeAt(toVisit_x, toVisit_y);
                i++;
            }
        }

        return toEval;
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
            //System.out.println(child.action + " " + child.score + " " + child.alphabeta);
            if (child.score > nextChild.score && child.alphabeta) {
                nextChild = child;
            }
        }
        //System.out.println();

        root.setSelectedChild(nextChild);
        root = nextChild;

        extendLeaves(root);
        alphabeta(root, root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        return root.action;
    }
}
