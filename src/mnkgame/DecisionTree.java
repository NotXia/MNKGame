package mnkgame;
import java.util.HashMap;
import java.util.LinkedList;

import java.io.FileWriter;
import java.io.IOException;

public class DecisionTree {
    private Node root;
    private int rows, columns, target;
    private boolean first;
    private MNKCellState my_state, opponent_state;
    private int maxScore, minScore;


    public DecisionTree(int M, int N, int K, boolean first) {
        this.root = null;
        this.rows = M;
        this.columns = N;
        this.target = K;
        this.first = first;
        this.my_state = first ? MNKCellState.P1 : MNKCellState.P2;
        this.opponent_state = first ? MNKCellState.P2 : MNKCellState.P1;
        this.maxScore = M*N;
        this.minScore = -(M*N);
    }

    public boolean isEmpty() {
        return root == null;
    }

    private boolean isValidCoord(int x, int y) {
        return (x >= 0 && x < rows) && (y >= 0 && y < columns);
    }

    private MNKGameState boardStatus(MNKCellState[][] board, int boardSize, int x, int y) {
        MNKCellState toCheckState = board[x][y];
        MNKCellState me = first ? MNKCellState.P1 : MNKCellState.P2;

        MNKGameState win = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        MNKGameState loss = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
        int aligned;
        MNKGameState result;

        if (toCheckState == me) {
            result = win;
        }
        else {
            result = loss;
        }

        // VERTICALE
        aligned = 1;
        for (int i=1; i<=target && x-i>=0; i++) { // Sx
            if (board[x-i][y] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x+i<rows; i++) { // Dx
            if (board[x+i][y] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= target) { return result; }

        // ORIZZONTALE
        aligned = 1;
        for (int i=1; i<=target && y-i>=0; i++) { // Alto
            if (board[x][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && y+i<columns; i++) { // Basso
            if (board[x][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= target) { return result; }

        // OBLIQUO SX a DX
        aligned = 1;
        for (int i=1; i<=target && x-i>=0 && y-i>=0; i++) { // Alto sx
            if (board[x-i][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x+i<rows && y+i<columns; i++) { // Basso dx
            if (board[x+i][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= target) { return result; }

        // OBLIQUO DX a SX
        aligned = 1;
        for (int i=1; i<=target && x+i<rows && y-i>=0; i++) { // Alto dx
            if (board[x+i][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x-i>=0 && y+i<columns; i++) { // Basso sx
            if (board[x-i][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= target) { return result; }

        if (boardSize == rows*columns) {
            return MNKGameState.DRAW;
        }
        else {
            return MNKGameState.OPEN;
        }
    }

    private int alphabeta(Node node, boolean mePlaying, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;

            if (mePlaying) {
                eval = 10;
                int i = 0;
                for (Node child : node.children) {
                    eval = Math.min(eval, alphabeta(child, false, alpha, beta));
                    beta = Math.min(eval, beta);
                    if (beta <= alpha) {
                        node.children.subList(i+1, node.children.size()).clear();
                        break;
                    }
                    i++;
                }
            }
            else {
                eval = -10;
                int i = 0;
                for (Node child : node.children) {
                    eval = Math.max(eval, alphabeta(child, true, alpha, beta));
                    alpha = Math.max(eval, alpha);
                    if (beta <= alpha) {
                        node.children.subList(i+1, node.children.size()).clear();
                        break;
                    }
                    i++;
                }
            }

            node.score = eval;
            return eval;
        }
    }

    private Node createTree(Node toEval, boolean mePlaying, int depth, MNKCellState[][] board, int boardSize) {
        /*System.out.println(toEval);*/
        MNKGameState status = boardStatus(board, boardSize, toEval.action.i, toEval.action.j);
        /*for (int a=0; a<rows; a++) {
            for (int b=0; b<columns; b++) {
                System.out.print(board[a][b] + " ");
            }
            System.out.println();
        }
        System.out.println(status);
        System.out.println();*/


        if (status == MNKGameState.OPEN) {
            HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();

            /**
             *
             * TODO: Manca il nodo della mossa del nemico fuori dal nostro raggio di valutazione
             * (Serve veramente?)
             *
             * **/

            for (MNKCell markedCell : toEval.getMarkedCells()) {
                for (int i=-1; i<=1; i++) {
                    for (int j=-1; j<=1; j++) {
                        if (i == 0 && j == 0) { continue; }

                        int toVisit_x = markedCell.i+i;
                        int toVisit_y = markedCell.j+j;

                        if (isValidCoord(toVisit_x, toVisit_y) && hasBeenEvaluated.get(""+toVisit_x+" "+toVisit_y) == null) {
                            if (board[toVisit_x][toVisit_y] == null) {
                                MNKCellState state = mePlaying ? my_state : opponent_state;
                                MNKCell toEvalCell = new MNKCell(toVisit_x, toVisit_y, state);

                                Node child = new Node(toEval, toEvalCell, rows, columns, target);

                                board[toVisit_x][toVisit_y] = state;
                                toEval.children.addFirst( createTree(child, !mePlaying, depth+1, board, boardSize+1) );
                                board[toVisit_x][toVisit_y] = null;

                                hasBeenEvaluated.put(""+toVisit_x+" "+toVisit_y, true);
                            }
                        }
                    }
                }
            }

            alphabeta(toEval, !mePlaying, minScore, maxScore); /** <-- CONTROLLARE i parametri di chiamata **/
        }
        else {
            MNKGameState win = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
            MNKGameState loss = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
            if (status == win) {
                toEval.score = maxScore - depth;
            }
            else if (status == loss) {
                toEval.score = minScore + depth;
            }
            else {
                toEval.score = 0;
            }
        }

        return toEval;
    }

    public void generate(MNKCell markedCell) {
        root = new Node(null, markedCell, rows, columns, target);

        MNKCellState[][] board = new MNKCellState[rows][columns];
        board[markedCell.i][markedCell.j] = markedCell.state;

        createTree(root, !first, 0, board, 1);
    }

    public void setOpponentMove(MNKCell move) {
        Node bestChild = root.children.getFirst();

        for (Node child : root.children) {
            if (child.action.equals(move)) {
                bestChild = child;
                break;
            }
            if (child.score > bestChild.score) {
                bestChild = child;
            }
        }

        root = bestChild;
        root.parent = null;
    }

    public MNKCell nextMove() {
        Node bestChild = root.children.getFirst();

        for (Node child : root.children) {
            if (child.score > bestChild.score) {
                bestChild = child;
            }
        }

        root = bestChild;
        root.parent = null;

        return bestChild.action;
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
