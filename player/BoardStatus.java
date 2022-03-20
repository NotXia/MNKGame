package player;

import mnkgame.*;

public class BoardStatus {
    private class Score {
        public int aligned, moves;
        public int start;

        public Score(int aligned, int moves, int start) {
            this.aligned = aligned;
            this.moves = moves;
            this.start = start;
        }
        public Score(Score toCopy) {
            this.aligned = toCopy.aligned;
            this.moves = toCopy.moves;
            this.start = toCopy.start;
        }

        @Override
        public String toString() {
            return String.format("[( %d ) %d %d]", start, aligned, moves);
        }
    }

    private Matrix matrix;
    private int columns, rows, target;
    private final MNKCellState PLAYER_STATE, OPPONENT_STATE;

    private Score[][] rowScore_player, columnScore_player, mainDiagonalScore_player, secondaryDiagonalScore_player;
    private Score[][] rowScore_opponent, columnScore_opponent, mainDiagonalScore_opponent, secondaryDiagonalScore_opponent;

    private final int NOT_WINNABLE_SCORE;

    /**
     * @implNote Costo: Θ(M*N)
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
    }

    /**
     * Indica se una coordinata è valida
     * @implNote Costo: Θ(1)
     * */
    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    /**
     * Imposta lo stato di una determinata cella
     * @implNote Costo (Pessimo): O(max{M, N}) = O(M+N)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    public void setAt(int x, int y, MNKCellState state) {
                                        // Ottimo   Pessimo
        matrix.setAt(x, y, state);      // Θ(1)     Θ(1)
        clearScores(x, y);              // Θ(1)     O(max{M, N})
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

    /**
     * Indica se una determinata cella è FREE
     * @implNote Costo: Θ(1)
     * */
    public boolean isFreeAt(int x, int y) {
        return matrix.getAt(x, y) == MNKCellState.FREE;
    }

    /**
     * Imposta a null la riga, colonna e diagonali che passano per (x, y)
     * @implNote Costo (pessimo): O(max{M, N}) = O(M+N)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    private void clearScores(int x, int y) {
        if (rowScore_player[x][y] == null && columnScore_player[x][y] == null && mainDiagonalScore_player[x][y] == null && secondaryDiagonalScore_player[x][y] == null) { return; }

        // Riga                                         Θ(N)
        for (int i=0; i<columns; i++) {
            rowScore_player[i][y] = null; rowScore_opponent[i][y] = null;
        }

        // Colonna                                      Θ(M)
        for (int i=0; i<rows; i++) {
            columnScore_player[x][i] = null; columnScore_opponent[x][i] = null;
        }

        // Diagonali principali                         O(min {M, N})
        int i=x, j=y;
        while (i >= 0 && j >= 0) {
            mainDiagonalScore_player[i][j] = null; mainDiagonalScore_opponent[i][j] = null;
            i--; j--;
        }
        i=x+1; j=y+1;
        while (i < columns && j < rows) {
            mainDiagonalScore_player[i][j] = null; mainDiagonalScore_opponent[i][j] = null;
            i++; j++;
        }

        // Diagonali secondarie                         O(min {M, N})
        i=x; j=y;
        while (i < columns && j >= 0) {
            secondaryDiagonalScore_player[i][j] = null; secondaryDiagonalScore_opponent[i][j] = null;
            i++; j--;
        }
        i=x-1; j=y+1;
        while (i >= 0 && j < rows) {
            secondaryDiagonalScore_player[i][j] = null; secondaryDiagonalScore_opponent[i][j] = null;
            i--; j++;
        }
    }

    /**
     * Restituisce un vettore di Score tale che la posizione i-esima indichi il numero di mosse mancanti per vincere se si selezionasse la mossa in quella posizione
     * @param board Vettore contenente la configurazione di gioco rispetto ad una riga/colonna/diagonale
     * @param toCheckState Lo stato da controllare (giocatore o avversario)
     * @implNote Costo (pessimo): O(board.length * K)
     * */
    private Score[] getScoresArray(MNKCellState[] board, MNKCellState toCheckState) {
        MNKCellState oppositeState = (toCheckState == MNKCellState.P1) ? MNKCellState.P2 : MNKCellState.P1;
        Score[] s = new Score[board.length];

        if (board[0] == toCheckState)       { s[0] = new Score(1, 0, -1); }
        else if (board[0] == oppositeState) { s[0] = new Score(0, 0, -1); }
        else /* status[0] == FREE */        { s[0] = new Score(1, 1, -1); }

        for (int i=1; i<board.length; i++) {                                                            // O(board.length)
            if (board[i] == oppositeState) {
                s[i] = new Score(0, 0, -1);
            }
            else if (s[i-1].aligned < target) {
                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves, -1);
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned + 1, s[i-1].moves + 1, -1);
                }

                if (s[i].aligned == target) { s[i].start = i-(target-1); } // Se sono in grado di allinearne fino al target, imposto l'inizio dell'allineamento
            }
            else {
                int toIgnoreMoveCost = 0;
                if (board[i-target] == MNKCellState.FREE) { toIgnoreMoveCost = 1; }

                if (board[i] == toCheckState) {
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves - toIgnoreMoveCost, i-(target-1));
                }
                else { // board[i] == FREE
                    s[i] = new Score(s[i-1].aligned, s[i-1].moves + 1 - toIgnoreMoveCost, i-(target-1));
                }
            }

            // Progapazione
            if (s[i].aligned == target) {
                for (int k=1; k<=target-1 && i-k>=0; k++) {                                             // O(K)
                    // Interrompo se incontro una mossa migliore della i-esima (necessita di meno mosse per vincere)
                    if (s[i-k].aligned == target && s[i-k].moves <= s[i].moves) { break; }

                    s[i-k] = new Score(s[i]);
                }
            }
        }

        return s;
    }

    /**
     * Imposta il punteggio di una determinata cella rispetto ad una determinata direzione.
     * @implNote Costo: Θ(1)
     * */
    private void setScore(Score[][] score, int x, int y, Score value) {
        if (value.aligned == 0 && value.moves == 0) {
            score[x][y] = new Score(0, NOT_WINNABLE_SCORE, -1);
        }
        else if (value.aligned != target) {
            score[x][y] = new Score(value.aligned, NOT_WINNABLE_SCORE, -1);
        }
        else {
            score[x][y] = new Score(value);
        }
    }

    /**
     * Riempie, nella matrice degli Score delle righe, la riga contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(N*K)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    private void fillRowScoreAt(int x, int y) {
        if (rowScore_player[x][y] != null && rowScore_opponent[x][y] != null) { return; }

        Score[] moves;
        final MNKCellState[] row = matrix.getRowAt(x, y);       // Θ(N)

        // Giocatore
        moves = getScoresArray(row, PLAYER_STATE);              // O(N*K)
        for (int j=0; j<columns; j++) {                         // Θ(N)
            setScore(rowScore_player, j, y, moves[j]);
        }

        // Avversario
        moves = getScoresArray(row, OPPONENT_STATE);            // O(N*K)
        for (int j=0; j<columns; j++) {                         // Θ(N)
            setScore(rowScore_opponent, j, y, moves[j]);
        }
    }

    /**
     * Riempie, nella matrice degli Score delle colonne, la colonna contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(M*K)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    private void fillColumnScoreAt(int x, int y) {
        if (columnScore_player[x][y] != null && columnScore_opponent[x][y] != null) { return; }

        Score[] moves;
        final MNKCellState[] column = matrix.getColumnAt(x, y);     // Θ(M)

        // Giocatore
        moves = getScoresArray(column, PLAYER_STATE);               // O(M*K)
        for (int j=0; j<rows; j++) {                                // Θ(M)
            setScore(columnScore_player, x, j, moves[j]);
        }

        // Avversario
        moves = getScoresArray(column, OPPONENT_STATE);             // O(M*K)
        for (int j=0; j<rows; j++) {                                // Θ(M)
            setScore(columnScore_opponent, x, j, moves[j]);
        }
    }

    /**
     * Riempie, nella matrice degli Score delle diagonali principali, la diagonale contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(min{M, N}*K)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    private void fillMainDiagonalScoreAt(int x, int y) {
        if (mainDiagonalScore_player[x][y] != null && mainDiagonalScore_opponent[x][y] != null) { return; }

        Score[] moves;
        final MNKCellState[] diagonal = matrix.getMainDiagonalAt(x, y);             // O(min{M, N})

        Coord diagonalStart = matrix.getMainDiagonalStart(x, y);
        int diagonalStartX = diagonalStart.x, diagonalStartY = diagonalStart.y;

        // Giocatore
        moves = getScoresArray(diagonal, PLAYER_STATE);                             // O(min{M, N}*K)
        int i = diagonalStartX, j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {                                        // O(min{M, N})
            setScore(mainDiagonalScore_player, i, j, moves[k]);
            i++; j++;
        }

        // Avversario
        moves = getScoresArray(diagonal, OPPONENT_STATE);                           // O(min{M, N}*K)
        i = diagonalStartX; j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {                                        // O(min{M, N})
            setScore(mainDiagonalScore_opponent, i, j, moves[k]);
            i++; j++;
        }
    }

    /**
     * Riempie, nella matrice degli Score delle diagonali secondarie, la diagonale contenente la cella nella posizione indicata
     * @implNote Costo (pessimo): O(min{M, N}*K)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    private void fillSecondaryDiagonalScoreAt(int x, int y) {
        if (secondaryDiagonalScore_player[x][y] != null && secondaryDiagonalScore_opponent[x][y] != null) { return; }

        Score[] moves;
        final MNKCellState[] diagonal = matrix.getSecondaryDiagonalAt(x, y);            // O(min{M, N})

        Coord diagonalStart = matrix.getSecondaryDiagonalStart(x, y);
        int diagonalStartX = diagonalStart.x, diagonalStartY = diagonalStart.y;

        // Giocatore
        moves = getScoresArray(diagonal, PLAYER_STATE);                                 // O(min{M, N}*K)
        int i = diagonalStartX, j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {                                            // O(min{M, N})
            setScore(secondaryDiagonalScore_player, i, j, moves[k]);
            i--; j++;
        }

        // Avversario
        moves = getScoresArray(diagonal, OPPONENT_STATE);                               // O(min{M, N}*K)
        i = diagonalStartX; j = diagonalStartY;
        for (int k=0; k<moves.length; k++) {                                            // O(min{M, N})
            setScore(secondaryDiagonalScore_opponent, i, j, moves[k]);
            i--; j++;
        }
    }

    /**
     * Riempie in tutte le matrici degli score la riga/colonna/diagonale che include la posizione indicata
     * @implNote Costo (pessimo): O(max{M, N}*K) = O(MK + NK)<br/>
     *           Costo (ottimo): Θ(1)
     * */
    public void generateMovesToWinAt(int x, int y) {
                                                // Ottimo       Pessimo
        fillRowScoreAt(x, y);                   // Θ(1)         O(N*K)
        fillColumnScoreAt(x, y);                // Θ(1)         O(M*K)
        fillMainDiagonalScoreAt(x, y);          // Θ(1)         O(min{M, N}*K)
        fillSecondaryDiagonalScoreAt(x, y);     // Θ(1)         O(min{M, N}*K)
    }

    /**
     * Riempie tutte le matrici degli score
     * @implNote Costo (pessimo): O(M*N*K) = O(2*4*M*N*K) L'idea è che per generare tutti gli score, bisogna iterare su tutte le celle disponibili<br/>
     *           Costo (ottimo): Θ(1)
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
     * Restituisce la matrice degli score della tipologia richiesta
     * @implNote Costo: Θ(1)
     * */
    private Score[][] rowScoreFilter(MNKCellState toCheckState) {
        if (toCheckState == PLAYER_STATE) { return rowScore_player; }
        else { return rowScore_opponent; }
    }
    private Score[][] columnScoreFilter(MNKCellState toCheckState) {
        if (toCheckState == PLAYER_STATE) { return columnScore_player; }
        else { return columnScore_opponent; }
    }
    private Score[][] mainDiagonalScoreFilter(MNKCellState toCheckState) {
        if (toCheckState == PLAYER_STATE) { return mainDiagonalScore_player; }
        else { return mainDiagonalScore_opponent; }
    }
    private Score[][] secondaryDiagonalScoreFilter(MNKCellState toCheckState) {
        if (toCheckState == PLAYER_STATE) { return secondaryDiagonalScore_player; }
        else { return secondaryDiagonalScore_opponent; }
    }

    /**
     * Restituisce il numero minimo di mosse necessarie per vincere ad una determinata cella
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @implNote Costo: Θ(1) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int getMovesToWinAt(int x, int y, MNKCellState toCheckState) {
        Score[][] rowScore = rowScoreFilter(toCheckState);
        Score[][] columnScore = columnScoreFilter(toCheckState);
        Score[][] mainDiagonalScore = mainDiagonalScoreFilter(toCheckState);
        Score[][] secondaryDiagonalScore = secondaryDiagonalScoreFilter(toCheckState);

        return Math.min(
            rowScore[x][y].moves,
            Math.min(
                columnScore[x][y].moves,
                Math.min(
                    mainDiagonalScore[x][y].moves,
                    secondaryDiagonalScore[x][y].moves
                )
            )
        );
    }


    /**
     * Restituisce un array contenente il numero di possibili modi per vincere
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @return Array di interi dove se v[i] = q allora ci sono q modi per vincere che necessitano di un numero di i mosse
     * @implNote Costo: O(M*N) = O(4*M*N)
     *           [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int[] getAllPossibleWinningScenariosCount(MNKCellState toCheckState) {
        int[] out = new int[target+1];
        Score[][] rowScore = rowScoreFilter(toCheckState);
        Score[][] columnScore = columnScoreFilter(toCheckState);
        Score[][] mainDiagonalScore = mainDiagonalScoreFilter(toCheckState);
        Score[][] secondaryDiagonalScore = secondaryDiagonalScoreFilter(toCheckState);
        int prevStart; // Memorizza l'inizio dell'allineamento precedentemente elaborato

        // Righe                                                             // Θ(M*N)
        for (int y=0; y<rows; y++) { // Θ(M)
            prevStart = -1;
            for (int x=0; x<columns; x++) { // Θ(N)
                if (rowScore[x][y].aligned != target) { continue; }
                if (isFreeAt(x, y) && prevStart != rowScore[x][y].start) {
                    prevStart = rowScore[x][y].start;
                    out[rowScore[x][y].moves]++;
                }
            }
        }

        // Colonne                                                           // Θ(M*N)
        for (int x=0; x<columns; x++) { // Θ(N)
            prevStart = -1;
            for (int y=0; y<rows; y++) { // Θ(M)
                if (columnScore[x][y].aligned != target) { continue; }
                if (isFreeAt(x, y) && prevStart != columnScore[x][y].start) {
                    prevStart = columnScore[x][y].start;
                    out[columnScore[x][y].moves]++;
                }
            }
        }

        // Diagonali principali                                              // O((M+N) * min{M, N})
        for (int y=0; y<rows; y++) { // Θ(M)
            int i=0, j=y;
            prevStart = -1;
            while (isValidCell(i, j)) { // O(min{M, N}) [Lunghezza max diagonale]
                if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                    prevStart = mainDiagonalScore[i][j].start;
                    out[mainDiagonalScore[i][j].moves]++;
                }
                i++; j++;
            }
        }
        for (int x=1; x<columns; x++) { // Θ(N)
            int i=x, j=0;
            prevStart = -1;
            while (isValidCell(i, j)) { // O(min{M, N}) [Lunghezza max diagonale]
                if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                    prevStart = mainDiagonalScore[i][j].start;
                    out[mainDiagonalScore[i][j].moves]++;
                }
                i++; j++;
            }
        }

        // Diagonali secondaria                                              // O((M+N) * min{M, N})
        for (int y=0; y<rows; y++) { // Θ(M)
            int i=columns-1, j=y;
            prevStart = -1;
            while (isValidCell(i, j)) { // O(min{M, N}) [Lunghezza max diagonale]
                if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                    prevStart = secondaryDiagonalScore[i][j].start;
                    out[secondaryDiagonalScore[i][j].moves]++;
                }
                i--; j++;
            }
        }
        for (int x=columns-2; x>=0; x--) { // Θ(N)
            int i=x, j=0;
            prevStart = -1;
            while (isValidCell(i, j)) { // O(min{M, N}) [Lunghezza max diagonale]
                if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                    prevStart = secondaryDiagonalScore[i][j].start;
                    out[secondaryDiagonalScore[i][j].moves]++;
                }
                i--; j++;
            }
        }

        return out;
    }

    /**
     * Restituisce un array contenente il numero di possibili modi per vincere rispetto ad una determinata posizione
     * @param toCheckState Indica lo stato della cella che si vuole controllare (giocatore o avversario)
     * @return Array di interi dove se v[i] = q allora ci sono q modi per vincere che necessitano di un numero di i mosse
     * @implNote Costo: O(max{M, N}) = O(M + N)
     *           [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public int[] getAllPossibleWinningScenariosCountAt(int toCheckX, int toCheckY, MNKCellState toCheckState) {
        int[] out = new int[target+1];
        Score[][] rowScore = rowScoreFilter(toCheckState);
        Score[][] columnScore = columnScoreFilter(toCheckState);
        Score[][] mainDiagonalScore = mainDiagonalScoreFilter(toCheckState);
        Score[][] secondaryDiagonalScore = secondaryDiagonalScoreFilter(toCheckState);
        int prevStart; // Memorizza l'inizio dell'allineamento precedentemente elaborato

        prevStart = -1;
        for (int x=0; x<columns; x++) {                                                     // Θ(N)
            if (rowScore[x][toCheckY].aligned != target) { continue; }
            if (isFreeAt(x, toCheckY) && prevStart != rowScore[x][toCheckY].start) {
                prevStart = rowScore[x][toCheckY].start;
                out[rowScore[x][toCheckY].moves]++;
            }
        }

        prevStart = -1;
        for (int y=0; y<rows; y++) {                                                        // Θ(M)
            if (columnScore[toCheckX][y].aligned != target) { continue; }
            if (isFreeAt(toCheckX, y) && prevStart != columnScore[toCheckX][y].start) {
                prevStart = columnScore[toCheckX][y].start;
                out[columnScore[toCheckX][y].moves]++;
            }
        }

        Coord diagonalStart = matrix.getMainDiagonalStart(toCheckX, toCheckY);
        int i=diagonalStart.x, j=diagonalStart.y;
        prevStart = -1;
        while (isValidCell(i, j)) {                                                         // O(min{M, N})
            if (mainDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != mainDiagonalScore[i][j].start) {
                prevStart = mainDiagonalScore[i][j].start;
                out[mainDiagonalScore[i][j].moves]++;
            }
            i++; j++;
        }

        diagonalStart = matrix.getSecondaryDiagonalStart(toCheckX, toCheckY);
        i=diagonalStart.x; j=diagonalStart.y;
        prevStart = -1;
        while (isValidCell(i, j)) {                                                         // O(min{M, N})
            if (secondaryDiagonalScore[i][j].aligned == target && isFreeAt(i, j) && prevStart != secondaryDiagonalScore[i][j].start) {
                prevStart = secondaryDiagonalScore[i][j].start;
                out[secondaryDiagonalScore[i][j].moves]++;
            }
            i--; j++;
        }

        return out;
    }

    /**
     * Restituisce lo stato della griglia controllando da una determinata posizione (vittoria, sconfitta, pareggio, partita aperta)
     * @implNote Costo: Θ(1) [GLI SCORE DEVONO ESSERE STATI GENERATI]
     * */
    public MNKGameState statusAt(int x, int y) {
        if (matrix.getAt(x, y) == MNKCellState.FREE) { return MNKGameState.OPEN; }

        final MNKGameState WIN_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP1 : MNKGameState.WINP2;
        final MNKGameState LOSS_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP2 : MNKGameState.WINP1;

        if (getMovesToWinAt(x, y, PLAYER_STATE) == 0) {
            return WIN_STATE;
        }
        else if (getMovesToWinAt(x, y, OPPONENT_STATE) == 0) {
            return LOSS_STATE;
        }
        else if (matrix.size() == columns * rows) {
            return MNKGameState.DRAW;
        }
        else {
            return MNKGameState.OPEN;
        }
    }

    @Override
    public String toString() {
        return matrix.toString(PLAYER_STATE);
    }
}
