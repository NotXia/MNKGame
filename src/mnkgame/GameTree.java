package mnkgame;
import javax.swing.text.Position;
import java.util.HashMap;

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

    private static int MAX_DEPTH = 7;
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

        this.WIN_SCORE = 100000;
        this.LOSS_SCORE = -100000;
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

    private void setHeuristicScoreOf(Node node, BoardStatus board, MNKCellState whoHasToPlaying) {
        int playerScore=0, opponentScore=0;

        /*board.removeAt(node.action.j, node.action.i);
        board.generateMovesToWinAt(node.action.j, node.action.i);
        playerScore = board.getMovesToWinAt(node.action.j, node.action.i, MY_STATE);
        opponentScore = board.getMovesToWinAt(node.action.j, node.action.i, OPPONENT_STATE);
        board.setAt(node.action.j, node.action.i, node.action.state);

        playerScore = target - playerScore;
        opponentScore = target - opponentScore;*/

        /*board.generateGlobalMovesToWin();
        playerScore = target - board.getGlobalScoreOf(MY_STATE);
        opponentScore = target - board.getGlobalScoreOf(OPPONENT_STATE);

        //node.score = playerScore - opponentScore;

        if (playerScore == target-1 && whoHasToPlaying == MY_STATE) {
            node.score = WIN_SCORE;
        }
        else if (opponentScore == target-1 && whoHasToPlaying == OPPONENT_STATE) {
            node.score = LOSS_SCORE;
        }

        else if (playerScore > opponentScore) {
            node.score = playerScore;
        }
        else if (playerScore < opponentScore) {
            node.score = -opponentScore;
        }
        else if (whoHasToPlaying == MY_STATE) {
            node.score = -opponentScore;
        }
        else {
            node.score = playerScore;
        }*/

        board.generateGlobalMovesToWin();
        int[] playerPossibilities = board.getAllPossibleWinningScenariosCount(MY_STATE);
        int[] opponentPossibilities = board.getAllPossibleWinningScenariosCount(OPPONENT_STATE);

        int weight = 100;
        for (int i=1; i<=3 && i<playerPossibilities.length; i++) {
            playerScore += playerPossibilities[i] * weight;
            opponentScore += opponentPossibilities[i] * weight;
            weight = weight / 10;
        }

        /*System.out.println(board);
        for (int i=1; i<playerPossibilities.length; i++) {
            System.out.print("[" + i + ") " + playerPossibilities[i] + "] ");
        }
        System.out.println();
        for (int i=1; i<playerPossibilities.length; i++) {
            System.out.print("[" + i + ") " + opponentPossibilities[i] + "] ");
        }
        System.out.println();
        System.out.println(playerScore + " " + opponentScore);
        System.out.println();*/

        if (board.getGlobalScoreOf(MY_STATE) == target-1 && whoHasToPlaying == MY_STATE) {
            node.score = WIN_SCORE;
        }
        else if (board.getGlobalScoreOf(OPPONENT_STATE) == target-1 && whoHasToPlaying == OPPONENT_STATE) {
            node.score = LOSS_SCORE;
        }
        else if (playerScore == 0 && opponentScore == 0) {
            node.score = DRAW_SCORE;
        }
        else {
            node.score = playerScore - opponentScore;

            /*if (playerScore > opponentScore) {
                node.score = playerScore;
            }
            else {
                node.score = -opponentScore;
            }*/
        }

        /*else if (playerScore > opponentScore) {
            node.score = playerScore;
        }
        else if (playerScore < opponentScore) {
            node.score = -opponentScore;
        }
        else if (whoHasToPlaying == MY_STATE) {
            node.score = -opponentScore;
        }
        else {
            node.score = playerScore;
        }*/

/*        System.out.println(board);
        System.out.println(node.score + " " + whoHasToPlaying + " " + node.action);
        System.out.println();*/

        /*if (playerScore > opponentScore) {
            node.score = playerScore;
        }
        else if (playerScore < opponentScore) {
            node.score = -opponentScore;
        }
        else if (playerScore < target/2) {
            node.score = 0;
        }
        else if (state == MY_STATE) {
            node.score = -opponentScore;
        }
        else {
            node.score = playerScore;
        }*/
    }

    public PriorityQueue<PositionEstimation> getAdj(Node node, BoardStatus board, MNKCellState state) {
        HashMap<Coord, PositionEstimation> hm = new HashMap<>();
        PriorityQueue<PositionEstimation> out = new PriorityQueue<>();

        Node iter = node;

        while (iter != null) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) { continue; }

                    int toVisit_x = iter.action.j + j;
                    int toVisit_y = iter.action.i + i;
                    boolean isDiagonal = (i!=0 && j!=0);

                    Coord currCoord = new Coord(toVisit_x, toVisit_y);

                    if (isValidCell(toVisit_x, toVisit_y) && board.isFreeAt(toVisit_x, toVisit_y) && hm.get(currCoord) == null) {
                        int score = 0;
                        int beforePlayerScore, beforeOpponentScore;
                        int afterPlayerScore, afterOpponentScore;
                        int aligned = 0, blocked = 0;
                        int beforePlayerOpenDirections, beforeOpponentOpenDirections;
                        int afterPlayerOpenDirections, afterOpponentOpenDirections;
                        int closedDirections = 0;

                        board.generateMovesToWinAt(toVisit_x, toVisit_y);
                        beforePlayerScore = board.getMovesToWinAt(toVisit_x, toVisit_y, MY_STATE);
                        beforeOpponentScore = board.getMovesToWinAt(toVisit_x, toVisit_y, OPPONENT_STATE);
                        beforePlayerOpenDirections = board.numberOfOpenDirectionsAt(toVisit_x, toVisit_y, MY_STATE);
                        beforeOpponentOpenDirections = board.numberOfOpenDirectionsAt(toVisit_x, toVisit_y, OPPONENT_STATE);

                        board.setAt(toVisit_x, toVisit_y, state);
                        board.generateMovesToWinAt(toVisit_x, toVisit_y);
                        afterPlayerScore = board.getMovesToWinAt(toVisit_x, toVisit_y, MY_STATE);
                        afterOpponentScore = board.getMovesToWinAt(toVisit_x, toVisit_y, OPPONENT_STATE);
                        afterPlayerOpenDirections = board.numberOfOpenDirectionsAt(toVisit_x, toVisit_y, MY_STATE);
                        afterOpponentOpenDirections = board.numberOfOpenDirectionsAt(toVisit_x, toVisit_y, OPPONENT_STATE);

                        /*int playerScore=0, opponentScore=0;
                        board.generateGlobalMovesToWin();
                        int[] playerPossibilities = board.getAllPossibleWinningScenariosCount(MY_STATE);
                        int[] opponentPossibilities = board.getAllPossibleWinningScenariosCount(OPPONENT_STATE);
                        int weight = 100;
                        for (int k=1; k<=3 && k<playerPossibilities.length; k++) {
                            playerScore += playerPossibilities[k] * weight;
                            opponentScore += opponentPossibilities[k] * weight;
                            weight = weight / 10;
                        }*/

                        if (state == MY_STATE) {
                            if (afterPlayerScore == 0) { score = WIN_SCORE; }
                            else if (beforeOpponentScore == 1) { score = WIN_SCORE; }
                            //else if (playerScore == 0 && opponentScore == 0) { score = DRAW_SCORE; }
                            else {
                                aligned = target - afterPlayerScore;
                                blocked = target - beforeOpponentScore;
                                //score = Math.abs(beforePlayerScore - afterPlayerScore) + Math.abs(target - beforeOpponentScore);

                                /*if (playerScore > opponentScore) {
                                    score = playerScore;
                                }
                                else {
                                    score = -opponentScore;
                                }*/


                                score = aligned+blocked;
                                closedDirections = beforeOpponentOpenDirections - afterOpponentOpenDirections;
                            }

                        }
                        else {
                            if (afterOpponentScore == 0) { score = WIN_SCORE; }
                            else if (beforePlayerScore == 1) { score = WIN_SCORE; }
                            //else if (playerScore == 0 && opponentScore == 0) { score = DRAW_SCORE; }
                            else {
                                aligned = target - afterOpponentScore;
                                blocked = target - beforePlayerScore;
                                //score = Math.abs(beforeOpponentScore - afterOpponentScore) + Math.abs(target - beforePlayerScore);

                                /*if (opponentScore > playerScore) {
                                    score = opponentScore;
                                }
                                else {
                                    score = -playerScore;
                                }*/

                                score = aligned+blocked;
                                closedDirections = beforePlayerOpenDirections - afterPlayerOpenDirections;
                            }
                        }
                        board.removeAt(toVisit_x, toVisit_y);

                        hm.put(currCoord, new PositionEstimation(toVisit_x, toVisit_y, score, aligned, blocked, closedDirections,  isDiagonal));
                    }
                }
            }
            iter = iter.parent;
        }


        for (PositionEstimation ev : hm.values()) {
            out.add(ev);
        }

        return out;
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
        else if (depth <= 0) {
            setHeuristicScoreOf(toEval, board, mePlaying ? MY_STATE : OPPONENT_STATE);
        }
        else {
            LinkedList<PositionEstimation> list = new LinkedList<>();
            PriorityQueue<PositionEstimation> moves = getAdj(toEval, board, mePlaying ? MY_STATE : OPPONENT_STATE);

            int i=0;
            int score = moves.peek().score;
            while (moves.size() > 0) {
                //if (moves.peek().score < score) { break; }
                if (i >= 5) { break; }

                list.add(moves.poll());
                i++;
            }

            /*System.out.println(board);
            System.out.println(toEval.action);
            for (PositionEstimation toVisit : list) {
                System.out.print(toVisit + " ");
            }
            System.out.println();
            System.out.println();*/

            for (PositionEstimation toVisit : list) {
                int toVisit_x = toVisit.x;
                int toVisit_y = toVisit.y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(toEval, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                toEval.children.add( createTree(child, !mePlaying, depth-1, board) );
                board.removeAt(toVisit_x, toVisit_y);
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
