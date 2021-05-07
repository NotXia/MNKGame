package mnkgame;
import java.util.HashMap;
import java.util.LinkedList;

import java.io.FileWriter;
import java.io.IOException;
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
    private int maxScore, minScore;
    private int maxDepth;
    private final int MAX_EVAL;


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

        this.maxScore = M*N;
        this.minScore = -(M*N);

        this.maxDepth = 5;
        this.MAX_EVAL = 10;
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

            if (myNode) {
                eval = maxScore+1;
                for (Node child : node.children) {
                    eval = Math.min(eval, alphabeta(child, false, alpha, beta));
                    beta = Math.min(eval, beta);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
            else {
                eval = minScore-1;
                for (Node child : node.children) {
                    eval = Math.max(eval, alphabeta(child, true, alpha, beta));
                    alpha = Math.max(eval, alpha);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }

            node.score = eval;
            return eval;
        }
    }

    private Node createTree(Node toEval, boolean mePlaying, int depth, BoardStatus board) {
        MNKGameState gameState = board.statusAt(toEval.action.j, toEval.action.i);

        if (gameState != MNKGameState.OPEN) {
            if (gameState == WIN_STATE) {
                toEval.score = target + 1;
            }
            else if (gameState == LOSS_STATE) {
                toEval.score = -target -1;
            }
            else {
                toEval.score = 0;
            }
            toEval.isEndState = true;
        }
        else if (depth == maxDepth) {
            board.generateScore();

            int playerScore, opponentScore;
            playerScore = board.getGlobalBestMovesToWin(MY_STATE);
            opponentScore = board.getGlobalBestMovesToWin(OPPONENT_STATE);

            playerScore = target - playerScore;
            opponentScore = target - opponentScore;

            if (playerScore > opponentScore) {
                toEval.score = playerScore;
            }
            else {
                toEval.score = -opponentScore;
            }
        }
        else {
            HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();
            board.generateScore();
            PriorityQueue<EvaluationPosition> moves = new PriorityQueue<>((move1, move2) -> move1.score - move2.score);

            for (MNKCell markedCell : toEval.getMarkedCells()) {
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (i == 0 && j == 0) {
                            continue;
                        }

                        int toVisit_x = markedCell.j + j;
                        int toVisit_y = markedCell.i + i;

                        if (isValidCell(toVisit_x, toVisit_y)) {
                            if (board.isFreeAt(toVisit_x, toVisit_y) && hasBeenEvaluated.get(""+toVisit_x+" "+toVisit_y) == null) {
                                moves.add(new EvaluationPosition(toVisit_x, toVisit_y, board.getMovesToWinAt(toVisit_x, toVisit_y, MY_STATE)));
                                moves.add(new EvaluationPosition(toVisit_x, toVisit_y, board.getMovesToWinAt(toVisit_x, toVisit_y, OPPONENT_STATE)));
                                hasBeenEvaluated.put(""+toVisit_x+" "+toVisit_y, true);
                            }
                        }
                    }
                }
            }

            int i=0;

            while (i<MAX_EVAL && moves.size() != 0) {
                int toVisit_x = moves.peek().x;
                int toVisit_y = moves.peek().y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(toEval, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                toEval.children.add( createTree(child, !mePlaying, depth+1, board) );
                board.removeAt(toVisit_x, toVisit_y);
                i++;
            }

            /*board.generateScore();
            PriorityQueue<EvaluationPosition> moves = board.getBestMovesQueue();

            int i=0;

            while (i<MAX_EVAL && moves.size() != 0) {
                int toVisit_x = moves.peek().x;
                int toVisit_y = moves.peek().y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(toEval, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                toEval.children.add( createTree(child, !mePlaying, depth+1, board) );
                board.removeAt(toVisit_x, toVisit_y);
                i++;
            }*/

            //alphabeta(toEval, !mePlaying, -target-1, target+1);
        }

        return toEval;
    }

    public void generate(MNKCell markedCell) {
        root = new Node(null, markedCell);

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        board.setAt(markedCell.j, markedCell.i, markedCell.state);

        createTree(root, !first, 0, board);
        alphabeta(root, first, -target-1, target+1);
    }

    public void generate(Node root) {
        this.root = root;

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        for (MNKCell cell : root.getMarkedCells()) {
            board.setAt(cell.j, cell.i, cell.state);
        }

        boolean mePlaying = root.action.state == MY_STATE;

        createTree(root, !mePlaying, 0, board);
        alphabeta(root, mePlaying, -target-1, target+1);
    }

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
            root.children.add(new_root);
            generate(new_root);
        }
        else {
            root = bestChild;
        }
    }

    public MNKCell nextMove() {
        if (root.children.size() == 0) { // Nessuna mossa prevista
            generate(root);
        }

        root = root.children.poll();

        if (true) {
            if (root.score == 0) {
                System.out.println(root.action + " | Gita in SVIZZERA " + root.score);
            }
            else if (root.score > 0) {
                System.out.println(root.action + " | Verso la VITTORIA " + root.score);
            }
            else {
                System.out.println(root.action + " | Sulla strada verso la DISFATTA " + root.score);
            }
        }

        return root.action;
    }

    public void print() {
        FileWriter myWriter;

        try {
            myWriter = new FileWriter("filename.txt");
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return;
        }
        LinkedList<Node> queue = new LinkedList<>();
        queue.addFirst(root);
        int next_len = 0;
        int curr_len = 1;
        int tree_nodes = 0;

        while (queue.size() != 0) {
            Node x = queue.removeLast();
            tree_nodes++;

            for (Node child : x.children) {
                queue.addFirst(child);
                next_len++;
            }

            try {
                if (x.parent != null) {
                    myWriter.write("(" + x.parent.action.i + " " + x.parent.action.j + ") ");
                }
                myWriter.write(x.action + " " + x.score);
                myWriter.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            curr_len--;

            if (curr_len <= 0) {
                curr_len = next_len;
                next_len = 0;
                try {
                    myWriter.write("\n\n\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(tree_nodes);
        try {
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
