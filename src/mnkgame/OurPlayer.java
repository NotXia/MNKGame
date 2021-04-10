package mnkgame;

import java.util.LinkedList;

public class OurPlayer implements MNKPlayer {
    private int rows, columns, target;
    private boolean first;
    public DecisionTree decisionTree; /** <--------------------- DA RIMETTERE PRIVATE **/

    public OurPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.target = K;
        this.first = first;
        this.decisionTree = new DecisionTree(M, N, K, first);
    }

    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        MNKCell outCell;

        if (this.decisionTree.isEmpty()) {
            if (this.first) {
                outCell = new MNKCell(columns/2, rows/2, MNKCellState.P1);
                this.decisionTree.generate(outCell);
            }
            else {
                this.decisionTree.generate(MC[MC.length-1]);
                outCell = this.decisionTree.nextMove();
            }
        }
        else {
            this.decisionTree.setOpponentMove(MC[MC.length-1]);
            outCell = this.decisionTree.nextMove();
        }

        return outCell;
    }

    public String playerName() {
        return "xIA";
    }

    public static void main(String[] args) {
        OurPlayer p = new OurPlayer();

        p.initPlayer(3, 3, 3, true, 10);

        p.selectCell(new MNKCell[1], new MNKCell[1]);


        System.out.println("Creato");
        p.decisionTree.print();
    }

 }
