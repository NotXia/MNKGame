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
    }

    public boolean isEmpty() {
        return root == null;
    }

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    private int alphabeta(Node node, boolean mePlaying, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;

            if (mePlaying) {
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

    private boolean isProbablyAGoodIdea(BoardStatus board, int toVisit_x, int toVisit_y, int xAxisVariation, int yAxisVariation) {
        int alignable = 0;

        if (yAxisVariation == 0 && xAxisVariation != 0) { // Cella orizzontale
            alignable = board.getHorizontallyAlignedAt(toVisit_x, toVisit_y, new MNKCellState[]{ MY_STATE, MNKCellState.FREE });
        }
        else if (xAxisVariation == 0 && yAxisVariation != 0) { // Cella verticale
            alignable = board.getVerticallyAlignedAt(toVisit_x, toVisit_y, new MNKCellState[]{ MY_STATE, MNKCellState.FREE });
        }
        else if (xAxisVariation > 0 && yAxisVariation < 0 || xAxisVariation < 0 && yAxisVariation > 0) { // Cella in alto a destra / Cella in basso a sinistra
            alignable = board.getRightLeftObliquelyAlignedAt(toVisit_x, toVisit_y, new MNKCellState[]{ MY_STATE, MNKCellState.FREE });
        }
        else { // Cella in alto a sinistra / Cella in basso a destra
            alignable = board.getLeftRightObliquelyAlignedAt(toVisit_x, toVisit_y, new MNKCellState[]{ MY_STATE, MNKCellState.FREE });
        }

        return alignable >= target;
    }

    private Node createTree(Node toEval, boolean mePlaying, int depth, BoardStatus board) {
        MNKGameState gameState = board.statusAt(toEval.action.j, toEval.action.i);

        if (gameState != MNKGameState.OPEN) {
            if (gameState == WIN_STATE) {
                toEval.score = maxScore - depth;
            }
            else if (gameState == LOSS_STATE) {
                toEval.score = -1 - depth;
            }
            else {
                toEval.score = 0;
            }
            toEval.isEndState = true;
        }
        else {
            // Tiene traccia delle celle già elaborate
            HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();

            // Valuta tutte le celle adiacenti a quelle già marcate
            for (MNKCell markedCell : toEval.getMarkedCells()) {
                for (int i=-1; i<=1; i++) {
                    for (int j=-1; j<=1; j++) {
                        if (i == 0 && j == 0) { continue; }

                        int toVisit_x = markedCell.j+j;
                        int toVisit_y = markedCell.i+i;

                        if (isValidCell(toVisit_x, toVisit_y)) {
                            if (board.isFreeAt(toVisit_x, toVisit_y) && hasBeenEvaluated.get(""+toVisit_x+" "+toVisit_y) == null) {
                                /*if (markedCell.state == OPPONENT_STATE || isProbablyAGoodIdea(board, toVisit_x, toVisit_y, j, i)) {*/
                                    MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                                    MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                                    Node child = new Node(toEval, toEvalCell);

                                    board.setAt(toVisit_x, toVisit_y, state);
                                    toEval.children.add( createTree(child, !mePlaying, depth+1, board) );
                                    board.removeAt(toVisit_x, toVisit_y);

                                    hasBeenEvaluated.put(""+toVisit_x+" "+toVisit_y, true);
                                /*}*/
                            }
                        }
                    }
                }
            }
            alphabeta(toEval, !mePlaying, minScore, maxScore);
        }

        return toEval;
    }

    public void generate(MNKCell markedCell) {
        root = new Node(null, markedCell);

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        board.setAt(markedCell.j, markedCell.i, markedCell.state);

        createTree(root, !first, 0, board);
    }

    public void generate(Node root) {
        this.root = root;

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        for (MNKCell cell : root.getMarkedCells()) {
            board.setAt(cell.j, cell.i, cell.state);
        }

        boolean mePlaying = root.action.state == MY_STATE;

        createTree(root, !mePlaying, 0, board);
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
        root = root.children.poll();

        if (root.score == 0) {
            System.out.println(root.action + " | Gita in SVIZZERA");
        }
        else if (root.score > 0) {
            System.out.println(root.action + " | Verso la VITTORIA");
        }
        else {
            System.out.println(root.action + " | Sulla strada verso la DISFATTA");
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
