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
                outCell = new MNKCell(rows/2, columns/2, MNKCellState.P1);
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

        p.initPlayer(3, 3, 3, false, 10);

        MNKCell[] mc = new MNKCell[1];
        mc[0] = new MNKCell(2, 0, MNKCellState.P2);
        p.selectCell(new MNKCell[1], mc);

        p.selectCell(new MNKCell[1], mc);

/*        mc = new MNKCell[2];
        mc[0] = new MNKCell(2, 2, MNKCellState.P2);
        mc[1] = new MNKCell(0, 0, MNKCellState.P2);
        p.selectCell(new MNKCell[1], mc);*/

        System.out.println("Creato");
        p.decisionTree.print();

    }

 }
