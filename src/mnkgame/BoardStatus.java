package mnkgame;

public class BoardStatus {
    private class Score {
        public int aligned, moves;
        public int start, end;

        public Score(int aligned, int moves, int start, int end) {
            this.aligned = aligned;
            this.moves = moves;
            this.start = start;
            this.end = end;
        }
        public Score(Score toCopy) {
            this.aligned = toCopy.aligned;
            this.moves = toCopy.moves;
            this.start = toCopy.start;
            this.end = toCopy.end;
        }

        @Override
        public String toString() {
            return String.format("[( %d ~ %d ) %d %d]", start, end, aligned, moves);
        }
    }

    private Matrix matrix;
    private int columns, rows, target;
    private final MNKCellState PLAYER_STATE, OPPONENT_STATE;

    private Score[][] rowScore_player, columnScore_player, mainDiagonalScore_player, secondaryDiagonalScore_player;
    private Score[][] rowScore_opponent, columnScore_opponent, mainDiagonalScore_opponent, secondaryDiagonalScore_opponent;

    private final int NOT_WINNABLE_SCORE;

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
        mainDiagonalScore_player = new Score[columns][rows];
        secondaryDiagonalScore_player = new Score[columns][rows];

        rowScore_opponent = new Score[columns][rows];
        columnScore_opponent = new Score[columns][rows];
        mainDiagonalScore_opponent = new Score[columns][rows];
        secondaryDiagonalScore_opponent = new Score[columns][rows];

        NOT_WINNABLE_SCORE = target+1;
    };

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    /**
     * Imposta lo stato di una determinata cella
     * @implNote Costo (Pessimo): O(max{M, N})<br/>
     *           Costo (ottimo): O(1)
     * */
    public void setAt(int x, int y, MNKCellState state) {
        matrix.setAt(x, y, state);
        clearScores(x, y);
    }

    /**
     * Imposta a FREE lo stato di una determinata cella
     * @implNote Costo (Pessimo): O(max{M, N})<br/>
     *           Costo (ottimo): O(1)
     * */
    public void removeAt(int x, int y) {
        if (matrix.getAt(x, y) != MNKCellState.FREE) {
            matrix.removeAt(x, y);
            clearScores(x, y);
        }
    }

    public MNKCellState getAt(int x, int y) {
        return this.matrix.getAt(x, y);
    }

    /**
     * Indica se una determinata cella è FREE
     * @implNote Costo: O(1)
     * */
    public boolean isFreeAt(int x, int y) {
        return matrix.getAt(x, y) == MNKCellState.FREE;
    }

    /**
     * Imposta a null le celle degli score influenzate dalla posizione x, y
     * @implNote Costo (pessimo): O(max{M, N})<br/>
     *           Costo (ottimo): O(1)
     * */
    private void clearScores(int x, int y) {
        if (rowScore_player[x][y] == null && columnScore_player[x][y] == null && mainDiagonalScore_player[x][y] == null && secondaryDiagonalScore_player[x][y] == null) { return; }

        for (int i=0; i<columns; i++) {
            rowScore_player[i][y] = null;
            rowScore_opponent[i][y] = null;
        }

        for (int i=0; i<rows; i++) {
            columnScore_player[x][i] = null;
            columnScore_opponent[x][i] = null;
        }

        int i=x, j=y;
        while (i >= 0 && j >= 0) {
            mainDiagonalScore_player[i][j] = null;
            mainDiagonalScore_opponent[i][j] = null;
            i--; j--;
        }
        i=x+1; j=y+1;
        while (i < columns && j < rows) {
            mainDiagonalScore_player[i][j] = null;
            mainDiagonalScore_opponent[i][j] = null;
            i++; j++;
        }

        i=x; j=y;
        while (i < columns && j >= 0) {
            secondaryDiagonalScore_player[i][j] = null;
            secondaryDiagonalScore_opponent[i][j] = null;
            i++; j--;
        }
        i=x-1; j=y+1;
        while (i >= 0 && j < rows) {
            secondaryDiagonalScore_player[i][j] = null;
            secondaryDiagonalScore_opponent[i][j] = null;
            i--; j++;
        }
    }

    private Coord getMainDiagonalStart(int x, int y) {
        int min = Math.min(x, y);
        return new Coord(x-min, y-min);
    }

    private Coord getSecondaryDiagonalStart(int x, int y) {
        int i=x, j=y;
        while(isValidCell(i, j)) {
            i++; j--;
        }
        return new Coord(i-1, j+1);
    }

    /**
     * Restituisce un vettore di Score tale che la posizione i-esima indica il numero di mosse mancanti per vincere se si seleziona la mossa in quella posizione
     * @param board Vettore contenente la configurazione di gioco rispetto ad una riga/colonna/diagonale
     * @param toCheckState Lo stato da controllare (giocatore o avversario)
     * @implNote Costo (pessimo): (board.length * K)
     * */
    private Score[] getScoresArray(MNKCellState board[], MNKCellState toCheckState) {
        /*if (board.length < target) {
            Score[] s = new Score[board.length];
            for (int i=0; i<board.length; i++) {
                s[i] = new Score(0, 0);
            }
            return s;
        }*/

        MNKCellState oppositeState = (toCheckState == MNKCellState.P1) ? MNKCellState.P2 : MNKCellState.P1;
        Score[] s = new Score[board.length];

        if (board[0] == toCheckState) {
            s[0] = new Score(1, 0, -1, -1);
        }
        else if (board[0] == oppositeState) {
            s[0] = new Score(0, 0, -1, -1);
        }
        else { // status[0] == FREE
            s[0] = new Score(1, 1, -1, -1);
        }

        for (int i=1; i<board.length; i++) {
            if (board[i] == oppositeState) {
                s[i] = new Score(0, 0, -1, -1);
            }
            else if (s[i-1].aligned < target) {
                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves, -1, -1);
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves + 1, -1, -1);
                }
                if (s[i].aligned == target) {
                    s[i].start = i-(target-1);
                    s[i].end = i;
                }
            }
            else {
                int toIgnoreMoveCost = 0;
                if (board[i-target] == MNKCellState.FREE) { toIgnoreMoveCost = 1; }

                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves - toIgnoreMoveCost, i-(target-1), i);
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves + 1 - toIgnoreMoveCost, i-(target-1), i);
                }
            }


            // Progapazione
            if (s[i].aligned == target) {
                for (int k=1; k<=target-1 && i-k>=0; k++) {
                    if ( (s[i-k].aligned==0 && s[i-k].moves==0) || (s[i-k].aligned == target && s[i-k].moves <= s[i].moves) ) { break; }
                    s[i-k].aligned = s[i].aligned;
                    s[i-k].moves = s[i].moves;
                    s[i-k].start = s[i].start;
                    s[i-k].end = s[i].end;
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
            score[x][y] = new Score(0, NOT_WINNABLE_SCORE, -1, -1);
        }
        else if (value.aligned != target) {
            score[x][y] = new Score(value.aligned, NOT_WINNABLE_SCORE, -1, -1);
        }
        else {
            score[x][y] = new Score(value);
        }
    }

    /**
     * Riempie nella matrice degli Score delle righe, la riga contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(N*K)<br/>
     *           Costo (ottimo): O(1)
     * */
    private void fillRowScoreAt(int x, int y) {
        if (rowScore_player[x][y] != null && rowScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getRowAt(x, y), PLAYER_STATE); // O(N*K + N)
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
     * @implNote Costo (pessimo): O(M*K)<br/>
     *           Costo (ottimo): O(1)
     * */
    private void fillColumnScoreAt(int x, int y) {
        if (columnScore_player[x][y] != null && columnScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(matrix.getColumnAt(x, y), PLAYER_STATE); // O(M*K + M)
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
     * @implNote Costo (pessimo): O(min{M, N}*K)<br/>
     *           Costo (ottimo): O(1)
     * */
    private void fillMainDiagonalScoreAt(int x, int y) {
        if (mainDiagonalScore_player[x][y] != null && mainDiagonalScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // TODO: FORMULA PER INIZIO DIAGONALE
        int diagonalStartX = x, diagonalStartY = y;
        while(diagonalStartX > 0 && diagonalStartY > 0) {
            diagonalStartX--; diagonalStartY--;
        }

        // Giocatore
        moves = getScoresArray(matrix.getMainDiagonalAt(x, y), PLAYER_STATE); // O(min{M, N}*K + min{M, N})
        int i = diagonalStartX, j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {
            setScore(mainDiagonalScore_player, i, j, moves[k]);
            i++; j++;
        }

        // Avversario
        moves = getScoresArray(matrix.getMainDiagonalAt(x, y), OPPONENT_STATE);
        i = diagonalStartX; j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {
            setScore(mainDiagonalScore_opponent, i, j, moves[k]);
            i++; j++;
        }
    }

    /**
     * Riempie nella matrice degli Score delle diagonali secondarie, la diagonale contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(min{M, N}*K)<br/>
     *           Costo (ottimo): O(1)
     * */
    private void fillSecondaryDiagonalScoreAt(int x, int y) {
        if (secondaryDiagonalScore_player[x][y] != null && secondaryDiagonalScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // TODO: FORMULA PER INIZIO DIAGONALE
        int diagonalStartX = x, diagonalStartY = y;
        while (diagonalStartX < columns-1 && diagonalStartY > 0) {
            diagonalStartX++; diagonalStartY--;
        }

        // Giocatore
        moves = getScoresArray(matrix.getSecondaryDiagonalAt(x, y), PLAYER_STATE); // O(min{M, N}*K + min{M, N})
        int i = diagonalStartX, j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {
            setScore(secondaryDiagonalScore_player, i, j, moves[k]);
            i--; j++;
        }

        // Avversario
        moves = getScoresArray(matrix.getSecondaryDiagonalAt(x, y), OPPONENT_STATE);
        i = diagonalStartX; j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {
            setScore(secondaryDiagonalScore_opponent, i, j, moves[k]);
            i--; j++;
        }
    }

    /**
     * Riempie, in tutte le matrici degli score, la posizione indicata (e le celle sulla stessa riga/colonna/diagonale)
     * @implNote Costo (pessimo): (max{M, N}*K)<br/>
     *           Costo (ottimo): O(1)
     * */
    public void generateMovesToWinAt(int x, int y) {
        fillRowScoreAt(x, y);
        fillColumnScoreAt(x, y);
        fillMainDiagonalScoreAt(x, y);
        fillSecondaryDiagonalScoreAt(x, y);
    }

    /**
     * Indica, rispetto ad una determinata posizione, in quante direzioni è ancora possibili vincere
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @implNote Costo: O(1) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int numberOfOpenDirectionsAt(int x, int y, MNKCellState toCheckState) {
        int openDirections = 0;
        Score[][] rowScore, columnScore, mainDiagonalScore, secondaryDiagonalScore;

        if (toCheckState == PLAYER_STATE) {
            rowScore = rowScore_player;
            columnScore = columnScore_player;
            mainDiagonalScore = mainDiagonalScore_player;
            secondaryDiagonalScore = secondaryDiagonalScore_player;
        }
        else {
            rowScore = rowScore_opponent;
            columnScore = columnScore_opponent;
            mainDiagonalScore = mainDiagonalScore_opponent;
            secondaryDiagonalScore = secondaryDiagonalScore_opponent;
        }

        if ( (isValidCell(x-1, y) && rowScore[x-1][y].moves != NOT_WINNABLE_SCORE) || (isValidCell(x+1, y) && rowScore[x+1][y].moves != NOT_WINNABLE_SCORE) ) {
            openDirections += 1;
        }
        if ( (isValidCell(x, y-1) && columnScore[x][y-1].moves != NOT_WINNABLE_SCORE) || (isValidCell(x, y+1) && columnScore[x][y+1].moves != NOT_WINNABLE_SCORE) ) {
            openDirections += 1;
        }
        if ( (isValidCell(x-1, y-1) && mainDiagonalScore[x-1][y-1].moves != NOT_WINNABLE_SCORE) || (isValidCell(x+1, y+1) && mainDiagonalScore[x+1][y+1].moves != NOT_WINNABLE_SCORE) ) {
            openDirections += 1;
        }
        if ( (isValidCell(x+1, y-1) && secondaryDiagonalScore[x+1][y-1].moves != NOT_WINNABLE_SCORE) || (isValidCell(x-1, y+1) && secondaryDiagonalScore[x-1][y+1].moves != NOT_WINNABLE_SCORE) ) {
            openDirections += 1;
        }

        return openDirections;
    }



    /**
     * Riempie tutte le matrici degli score
     * @implNote Costo: O(M*N*K) = O(4*M*N*K) L'idea è che si genera lo score per tutte le celle e bisogna sempre propagare (caso pessimo)
     * */
    public void generateGlobalMovesToWin() {
        for (int x=0; x<columns; x++) { fillColumnScoreAt(x, 0); }                      // O(N * M*K)

        for (int y=0; y<rows; y++) { fillRowScoreAt(0, y); }                            // O(M * N*K)

        for (int y=0; y<rows; y++) { fillMainDiagonalScoreAt(0, y); }
        for (int x=1; x<columns; x++) { fillMainDiagonalScoreAt(x, 0); }                // O((M+N-1) * min{M, N}*K) dove (M+N-1) è il numero di diagonali

        for (int y=0; y<rows; y++) { fillSecondaryDiagonalScoreAt(columns-1, y); }
        for (int x=columns-2; x>=0; x--) { fillSecondaryDiagonalScoreAt(x, 0); }        // O((M+N-1) * min{M, N}*K) dove (M+N-1) è il numero di diagonali
    }

    /**
     * Restituisce il numero minimo di mosse necessarie per vincere a partire da una determinata cella
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @implNote Costo: O(1) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int getMovesToWinAt(int x, int y, MNKCellState toCheckState) {
        if (toCheckState == PLAYER_STATE) {
            return Math.min(
                    columnScore_player[x][y].moves,
                    Math.min(
                            rowScore_player[x][y].moves,
                            Math.min(
                                    mainDiagonalScore_player[x][y].moves,
                                    secondaryDiagonalScore_player[x][y].moves
                            )
                    )
            );
        } else {
            return Math.min(
                    columnScore_opponent[x][y].moves,
                    Math.min(
                            rowScore_opponent[x][y].moves,
                            Math.min(
                                    mainDiagonalScore_opponent[x][y].moves,
                                    secondaryDiagonalScore_opponent[x][y].moves
                            )
                    )
            );
        }
    }


    /**
     * Restituisce un array contenente il numero di possibili modi per vincere
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @return Array di interi dove se v[i] = q allora ci sono q modi per vincere che necessitano un numero di i mosse
     * @implNote Costo: O(M*N) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int[] getAllPossibleWinningScenariosCount(MNKCellState toCheckState) {
        int[] out = new int[target+1];
        int prevStart;
        Score[][] rowScore, columnScore, mainDiagonalScore, secondaryDiagonalScore;
        if (toCheckState == PLAYER_STATE) {
            rowScore = rowScore_player;
            columnScore = columnScore_player;
            mainDiagonalScore = mainDiagonalScore_player;
            secondaryDiagonalScore = secondaryDiagonalScore_player;
        }
        else {
            rowScore = rowScore_opponent;
            columnScore = columnScore_opponent;
            mainDiagonalScore = mainDiagonalScore_opponent;
            secondaryDiagonalScore = secondaryDiagonalScore_opponent;
        }

        for (int y=0; y<rows; y++) {
            prevStart = -1;
            for (int x=0; x<columns; x++) {
                if (rowScore[x][y].aligned != target) { continue; }
                if (isFreeAt(x, y) && prevStart != rowScore[x][y].start) {
                    prevStart = rowScore[x][y].start;
                    out[rowScore[x][y].moves]++;
                }
            }
        }

        for (int x=0; x<columns; x++) {
            prevStart = -1;
            for (int y=0; y<rows; y++) {
                if (columnScore[x][y].aligned != target) { continue; }
                if (isFreeAt(x, y) && prevStart != columnScore[x][y].start) {
                    prevStart = columnScore[x][y].start;
                    out[columnScore[x][y].moves]++;
                }
            }
        }

        for (int y=0; y<rows; y++) {
            int i=0, j=y;
            prevStart = -1;
            while (isValidCell(i, j)) {
                if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                    prevStart = mainDiagonalScore[i][j].start;
                    out[mainDiagonalScore[i][j].moves]++;
                }
                i++; j++;
            }
        }
        for (int x=1; x<columns; x++) {
            int i=x, j=0;
            prevStart = -1;
            while (isValidCell(i, j)) {
                if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                    prevStart = mainDiagonalScore[i][j].start;
                    out[mainDiagonalScore[i][j].moves]++;
                }
                i++; j++;
            }
        }

        for (int y=0; y<rows; y++) {
            int i=columns-1, j=y;
            prevStart = -1;
            while (isValidCell(i, j)) {
                if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                    prevStart = secondaryDiagonalScore[i][j].start;
                    out[secondaryDiagonalScore[i][j].moves]++;
                }
                i--; j++;
            }
        }
        for (int x=columns-2; x>=0; x--) {
            int i=x, j=0;
            prevStart = -1;
            while (isValidCell(i, j)) {
                if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                    prevStart = secondaryDiagonalScore[i][j].start;
                    out[secondaryDiagonalScore[i][j].moves]++;
                }
                i--; j++;
            }
        }

        return out;
    }


    public int[] getAllPossibleWinningScenariosCountAt(int toCheckX, int toCheckY, MNKCellState toCheckState) {
        int[] out = new int[target+1];
        Score[][] rowScore, columnScore, mainDiagonalScore, secondaryDiagonalScore;
        if (toCheckState == PLAYER_STATE) {
            rowScore = rowScore_player;
            columnScore = columnScore_player;
            mainDiagonalScore = mainDiagonalScore_player;
            secondaryDiagonalScore = secondaryDiagonalScore_player;
        }
        else {
            rowScore = rowScore_opponent;
            columnScore = columnScore_opponent;
            mainDiagonalScore = mainDiagonalScore_opponent;
            secondaryDiagonalScore = secondaryDiagonalScore_opponent;
        }
        int prevStart;

        prevStart = -1;
        for (int x=0; x<columns; x++) {
            if (rowScore[x][toCheckY].aligned != target) { continue; }
            if (isFreeAt(x, toCheckY) && prevStart != rowScore[x][toCheckY].start) {
                prevStart = rowScore[x][toCheckY].start;
                out[rowScore[x][toCheckY].moves]++;
            }
        }

        prevStart = -1;
        for (int y=0; y<rows; y++) {
            if (columnScore[toCheckX][y].aligned != target) { continue; }
            if (isFreeAt(toCheckX, y) && prevStart != columnScore[toCheckX][y].start) {
                prevStart = columnScore[toCheckX][y].start;
                out[columnScore[toCheckX][y].moves]++;
            }
        }

        Coord diagonalStart = getMainDiagonalStart(toCheckX, toCheckY);
        int i=diagonalStart.x, j=diagonalStart.y;
        prevStart = -1;
        while (isValidCell(i, j)) {
            if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                prevStart = mainDiagonalScore[i][j].start;
                out[mainDiagonalScore[i][j].moves]++;
            }
            i++; j++;
        }

        diagonalStart = getSecondaryDiagonalStart(toCheckX, toCheckY);
        i=diagonalStart.x; j=diagonalStart.y;
        prevStart = -1;
        while (isValidCell(i, j)) {
            if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                prevStart = secondaryDiagonalScore[i][j].start;
                out[secondaryDiagonalScore[i][j].moves]++;
            }
            i--; j++;
        }

        return out;
    }


    public String toString() {
        return matrix.toString(PLAYER_STATE);
    }

    /**
     * Restituisce lo stato della griglia controllando da una determinata posizione (vittoria, sconfitta, pareggio, partita aperta)
     * @implNote Costo: O(1) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public MNKGameState statusAt(int x, int y) {
        if (matrix.getAt(x, y) == MNKCellState.FREE) { return MNKGameState.OPEN; }

        final MNKGameState WIN_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP1 : MNKGameState.WINP2;
        final MNKGameState LOSS_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP2 : MNKGameState.WINP1;

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
        int M = 5, N = 5, K = 4;
        BoardStatus bs = new BoardStatus(M, N, K, MNKCellState.P1);


        bs.setAt(3, 1, MNKCellState.P2);
        bs.setAt(2, 3, MNKCellState.P2);
        bs.setAt(1, 3, MNKCellState.P2);


        /*bs.setAt(3, 1, MNKCellState.P2);
        bs.setAt(4, 1, MNKCellState.P1);
        bs.setAt(5, 1, MNKCellState.P2);

        bs.setAt(2, 2, MNKCellState.P2);
        bs.setAt(4, 2, MNKCellState.P1);
        bs.setAt(5, 2, MNKCellState.P2);

        bs.setAt(3, 3, MNKCellState.P1);
        bs.setAt(4, 3, MNKCellState.P2);

        bs.setAt(2, 4, MNKCellState.P1);
        bs.setAt(4, 4, MNKCellState.P1);
        bs.setAt(5, 4, MNKCellState.P1);

        bs.setAt(1, 5, MNKCellState.P1);

        bs.setAt(0, 6, MNKCellState.P2);
        bs.setAt(1, 6, MNKCellState.P2);

        bs.setAt(3, 4, MNKCellState.P1);*/

        bs.generateGlobalMovesToWin();

        System.out.println(bs);

        int[] aa = bs.getAllPossibleWinningScenariosCount(MNKCellState.P2);

        for (int i=1; i<aa.length; i++) {
            System.out.println(i + ") " + aa[i]);
        }

        /*int[] aa = bs.getAllPossibleWinningScenariosCountAt(3, 4, MNKCellState.P1);

        for (int i=1; i<aa.length; i++) {
            System.out.println(i + ") " + aa[i]);
        }*/

        /*for (int y=0; y<bs.rows; y++) {
            for (int x=0; x<bs.columns; x++) {
                System.out.print(String.format("%20s", bs.rowScore_player[x][y]));
            }
            System.out.println();
        }*/

        //System.out.println(bs.isVeryDangerous(0, 2, MNKCellState.P2));

    }
}
