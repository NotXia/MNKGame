package player;

import mnkgame.*;

public class OurPlayer implements MNKPlayer {
    private int rows, columns, target;
    private boolean first;
    private GameTree gameTree;

    public OurPlayer() {
    }

    /**
     * @implNote Costo: Θ(1)
     * */
    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.target = K;
        this.first = first;
        this.gameTree = new GameTree(M, N, K, first);
    }

    /**
     * @implNote Costo (pessimo): O( h(MK + NK + log(h)) )
     * */
    public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
        MNKCell outCell;

        if (this.gameTree.isEmpty()) {
            if (this.first) {
                // Se gioco per primo piazzo la mossa al centro della griglia
                outCell = new MNKCell(rows/2, columns/2, MNKCellState.P1);
                this.gameTree.generate(outCell);                                        // O( h(MK + NK + log(h)) )
            }
            else {
                this.gameTree.generate(MC[MC.length-1]);                                // O( h(MK + NK + log(h)) )
                outCell = this.gameTree.nextMove();                                     // O( h(MK + NK + log(h)) )
            }
        }
        else {
            this.gameTree.setOpponentMove(MC[MC.length-1]);                             // O( h(MK + NK + log(h)) )
            outCell = this.gameTree.nextMove();                                         // O( h(MK + NK + log(h)) )
        }

        return outCell;
    }

    public String playerName() {
        return "Boneless";
    }
 }
