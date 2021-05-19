package mnkgame;

public class BoardStatus {
    private class Score {
        public int aligned, moves;

        public Score(int aligned, int moves) {
            this.aligned = aligned;
            this.moves = moves;
        }
        public Score(Score toCopy) {
            this.aligned = toCopy.aligned;
            this.moves = toCopy.moves;
        }

        @Override
        public String toString() {
            return "(" + aligned + ", " + moves + ")";
        }
    }

    private Matrix matrix;
    private int columns, rows, target;
    private final MNKCellState PLAYER_STATE, OPPONENT_STATE;

    private Score[][] rowScore_player, columnScore_player, diagonalLeftRightScore_player, diagonalRightLeftScore_player;
    private Score[][] rowScore_opponent, columnScore_opponent, diagonalLeftRightScore_opponent, diagonalRightLeftScore_opponent;

    /**
     * @implNote Costo: O(M*N)
     * */
    public BoardStatus(int columns, int rows, int target, MNKCellState playerState) {
        matrix = new Matrix(columns, rows, target);

        this.columns = columns;
        this.rows = rows;
        this.target = target;

        this.PLAYER_STATE = playerState;
        this.OPPONENT_STATE = playerState == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;

        rowScore_player = new Score[columns][rows];
        columnScore_player = new Score[columns][rows];
        diagonalLeftRightScore_player = new Score[columns][rows];
        diagonalRightLeftScore_player = new Score[columns][rows];
        rowScore_opponent = new Score[columns][rows];
        columnScore_opponent = new Score[columns][rows];
        diagonalLeftRightScore_opponent = new Score[columns][rows];
        diagonalRightLeftScore_opponent = new Score[columns][rows];
    };

    /**
     * Imposta lo stato di una determinata cella
     * @implNote Costo: O(1)
     * */
    public void setAt(int x, int y, MNKCellState state) {
        matrix.setAt(x, y, state);
        clearScores(x, y);
    }

    /**
     * Imposta a FREE lo stato di una determinata cella
     * @implNote Costo: O(1)
     * */
    public void removeAt(int x, int y) {
        if (matrix.getAt(x, y) != MNKCellState.FREE) {
            matrix.removeAt(x, y);
            clearScores(x, y);
        }
    }

    /**
     * Indica se una determinata cella è FREE
     * @implNote Costo: O(1)
     * */
    public boolean isFreeAt(int x, int y) {
        return matrix.getAt(x, y) == MNKCellState.FREE;
    }

    private void clearScores(int x, int y) {
        for (int i=0; i<columns; i++) {
            rowScore_player[i][y] = null;
            rowScore_opponent[i][y] = null;
        }

        for (int i=0; i<rows; i++) {
            columnScore_player[x][i] = null;
            columnScore_opponent[x][i] = null;
        }

        int i=x, j=y;
        while(i >= 0 && j >= 0) {
            diagonalLeftRightScore_player[i][j] = null;
            diagonalLeftRightScore_opponent[i][j] = null;
            i--; j--;
        }
        i=x+1; j=y+1;
        while(i < columns && j < rows) {
            diagonalLeftRightScore_player[i][j] = null;
            diagonalLeftRightScore_opponent[i][j] = null;
            i++; j++;
        }

        i=x; j=y;
        while(i < columns && j >= 0) {
            diagonalRightLeftScore_player[i][j] = null;
            diagonalRightLeftScore_opponent[i][j] = null;
            i++; j--;
        }
        i=x-1; j=y+1;
        while(i >= 0 && j < rows) {
            diagonalRightLeftScore_player[i][j] = null;
            diagonalRightLeftScore_opponent[i][j] = null;
            i--; j++;
        }
    }

    /**
     * Restituisce un vettore di Score tale che la posizione i-esima indica il numero di mosse mancanti per vincere se si seleziona la mossa in quella posizione
     * @param board Vettore contenente la configurazione di gioco rispetto ad una riga/colonna/diagonale
     * @param toCheckState Lo stato da controllare (giocatore o avversario)
     * @implNote Costo: (max{M, N} * K)
     * */
    private Score[] getScoresArray(MNKCellState board[], MNKCellState toCheckState) {
        if (board.length < target) {
            Score[] s = new Score[board.length];
            for (int i=0; i<board.length; i++) {
                s[i] = new Score(0, 0);
            }
            return s;
        }

        MNKCellState oppositeState = toCheckState == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        Score[] s = new Score[board.length];

        if (board[0] == toCheckState) {
            s[0] = new Score(1, 0);
        }
        else if (board[0] == oppositeState) {
            s[0] = new Score(0, 0);
        }
        else { // status[0] == FREE
            s[0] = new Score(1, 1);
        }

        for (int i=1; i<board.length; i++) {
            if (board[i] == oppositeState) {
                s[i] = new Score(0, 0);
            }
            else if (s[i-1].aligned < target) {
                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves);
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves + 1);
                }
            }
            else {
                int toIgnoreMoveCost = 0;
                if (board[i-target] == MNKCellState.FREE) {
                    toIgnoreMoveCost = 1;
                }

                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves - toIgnoreMoveCost);
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves + 1 - toIgnoreMoveCost);
                }
            }

            // Progapazione
            if (s[i].aligned == target) {
                for (int k=1; k<=target-1 && i-k>=0; k++) {
                    if ( (s[i-k].aligned==0 && s[i-k].moves==0) || (s[i-k].aligned == target && s[i-k].moves < s[i].moves) ) { break; }
                    s[i-k] = s[i];
                }
            }
        }

        return s;
    }

    /**
     * Imposta il punteggio di una determinata cella rispetto ad una determinata direzione.
     * @implNote Costo: O(1)
     * */
    private void setScore(Score[][] score, int x, int y, Score value) {
        if (value.aligned == 0 && value.moves == 0) {
            score[x][y] = new Score(0, target);
        }
        else if(value.aligned != target) {
            score[x][y] = new Score(value.aligned, target);
        }
        else {
            score[x][y] = new Score(value);
        }
        /*score[x][y] = new Score(value);*/
    }

    /**
     * Riempie nella matrice degli Score delle righe, la riga contenente la cella nella posizione indicata
     * @implNote Costo:
     * */
    private void fillRowScoreAt(int x, int y) {
        if (rowScore_player[x][y] != null && rowScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getRowAt(x, y), PLAYER_STATE);
        for (int j=0; j<columns; j++) {
            setScore(rowScore_player, j, y, moves[j]);
        }

        // Avversario
        moves = getScoresArray(matrix.getRowAt(x, y), OPPONENT_STATE);
        for (int j=0; j<columns; j++) {
            setScore(rowScore_opponent, j, y, moves[j]);
        }
    }

    /**
     * Riempie nella matrice degli Score delle colonne, la colonna contenente la cella nella posizione indicata
     * @implNote Costo:
     * */
    private void fillColumnScoreAt(int x, int y) {
        if (columnScore_player[x][y] != null && columnScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getColumnAt(x, y), PLAYER_STATE);
        for (int j=0; j<rows; j++) {
            setScore(columnScore_player, x, j, moves[j]);
        }

        // Avversario
        moves = getScoresArray(matrix.getColumnAt(x, y), OPPONENT_STATE);
        for (int j=0; j<rows; j++) {
            setScore(columnScore_opponent, x, j, moves[j]);
        }
    }

    /**
     * Riempie nella matrice degli Score delle diagonali principali, la diagonale contenente la cella nella posizione indicata
     * @implNote Costo:
     * */
    private void fillDiagonalLeftRightScoreAt(int x, int y) {
        if (diagonalLeftRightScore_player[x][y] != null && diagonalLeftRightScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getDiagonalLeftRightAt(x, y), PLAYER_STATE);
        int i = x, j = y;
        while(i > 0 && j > 0) {
            i--; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalLeftRightScore_player, i, j, moves[k]);
            i++; j++;
        }

        // Avversario
        moves = getScoresArray(matrix.getDiagonalLeftRightAt(x, y), OPPONENT_STATE);
        i = x; j = y;
        while(i > 0 && j > 0) {
            i--; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalLeftRightScore_opponent, i, j, moves[k]);
            i++; j++;
        }
    }

    /**
     * Riempie nella matrice degli Score delle diagonali secondarie, la diagonale contenente la cella nella posizione indicata
     * @implNote Costo:
     * */
    private void fillDiagonalRightLeftScoreAt(int x, int y) {
        if (diagonalRightLeftScore_player[x][y] != null && diagonalRightLeftScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getDiagonalRightLeftAt(x, y), PLAYER_STATE);
        int i = x, j = y;
        while (i < columns-1 && j > 0) {
            i++; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalRightLeftScore_player, i, j, moves[k]);
            i--; j++;
        }

        // Avversario
        moves = getScoresArray(matrix.getDiagonalRightLeftAt(x, y), OPPONENT_STATE);
        i = x; j = y;
        while (i < columns-1 && j > 0) {
            i++; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalRightLeftScore_opponent, i, j, moves[k]);
            i--; j++;
        }
    }

    /**
     * Riempie, in tutte le matrici degli score, la posizione indicata (e le celle sulla stessa riga/colonna/diagonale)
     * @implNote Costo:
     * */
    public void generateScoreAt(int x, int y) {
        fillRowScoreAt(x, y);
        fillColumnScoreAt(x, y);
        fillDiagonalLeftRightScoreAt(x, y);
        fillDiagonalRightLeftScoreAt(x, y);
    }

    /**
     * Restituisce il numero minimo di mosse necessarie per vincere a partireda una determinata cella
     * @param toCheckStatus Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @implNote Costo:
     * */
    public int getMovesToWinAt(int x, int y, MNKCellState toCheckStatus) {
        if (toCheckStatus == PLAYER_STATE) {
            return Math.min(
                columnScore_player[x][y].moves,
                Math.min(
                    rowScore_player[x][y].moves,
                    Math.min(
                        diagonalLeftRightScore_player[x][y].moves,
                        diagonalRightLeftScore_player[x][y].moves
                    )
                )
            );
        }
        else {
            return Math.min(
                columnScore_opponent[x][y].moves,
                Math.min(
                    rowScore_opponent[x][y].moves,
                    Math.min(
                        diagonalLeftRightScore_opponent[x][y].moves,
                        diagonalRightLeftScore_opponent[x][y].moves
                    )
                )
            );
        }
    }

    public String toString() {
        return matrix.toString(PLAYER_STATE);
    }

    /**
     * Restituisce lo stato della griglia controllando da una determinata posizione (vittoria, sconfitta, pareggio, partita aperta)
     * @implNote Costo:
     * */
    public MNKGameState statusAt(int x, int y) {
        if (matrix.getAt(x, y) == MNKCellState.FREE) { return MNKGameState.OPEN; }

        final MNKGameState WIN_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP1 : MNKGameState.WINP2;
        final MNKGameState LOSS_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP2 : MNKGameState.WINP1;

        generateScoreAt(x, y);
        if (matrix.size() == columns * rows) {
            return MNKGameState.DRAW;
        }
        else if (getMovesToWinAt(x, y, PLAYER_STATE) == 0) {
            return WIN_STATE;
        }
        else if (getMovesToWinAt(x, y, OPPONENT_STATE) == 0) {
            return LOSS_STATE;
        }
        else {
            return MNKGameState.OPEN;
        }
    }

    public static void main(String[] args) {
        BoardStatus bs = new BoardStatus(5, 5, 4, MNKCellState.P1);

        bs.setAt(0, 0, MNKCellState.P2);
        bs.setAt(1, 0, MNKCellState.P2);
        bs.setAt(2, 0, MNKCellState.P2);

        bs.setAt(1, 1, MNKCellState.P1);
        bs.setAt(2, 2, MNKCellState.P1);
        bs.setAt(1, 3, MNKCellState.P1);

        bs.generateScoreAt(3, 0);
        System.out.println(bs.getMovesToWinAt(3, 0, MNKCellState.P1));
        System.out.println(bs.getMovesToWinAt(3, 0, MNKCellState.P2));

        /*bs.generateScore();
        System.out.println(bs.getGlobalBestMovesToWin(MNKCellState.P1));
        System.out.println(bs.getGlobalBestMovesToWin(MNKCellState.P2));*/

    }
}
