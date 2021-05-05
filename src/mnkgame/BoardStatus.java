package mnkgame;

public class BoardStatus {
    private MNKCellState[][] matrix;
    private int size;
    private int columns, rows, target;
    private final MNKCellState PLAYER_STATE, OPPONENT_STATE;

    public BoardStatus(int columns, int rows, int target, MNKCellState playerState) {
        matrix = new MNKCellState[columns][rows];
        for (int y=0; y<rows; y++) {
            for (int x=0; x<columns; x++) {
                matrix[x][y] = MNKCellState.FREE;
            }
        }
        size = 0;

        this.columns = columns;
        this.rows = rows;
        this.target = target;
        this.PLAYER_STATE = playerState;
        this.OPPONENT_STATE = playerState == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
    };

    public void setAt(int x, int y, MNKCellState state) {
        if (state != null && state != MNKCellState.FREE) {
            matrix[x][y] = state;
            size++;
        }
        else {
            matrix[x][y] = MNKCellState.FREE;
            size--;
        }
    }

    public MNKCellState getAt(int x, int y) {
        return matrix[x][y];
    }

    public void removeAt(int x, int y) {
        if (matrix[x][y] != MNKCellState.FREE) {
            matrix[x][y] = MNKCellState.FREE;
            size--;
        }
    }

    public boolean isFreeAt(int x, int y) {
        return matrix[x][y] == MNKCellState.FREE;
    }

    private int getMovesToWin(MNKCellState board[], MNKCellState toCheckState) {
        MNKCellState oppositeState = toCheckState == MNKCellState.P1 ? MNKCellState.P2 : MNKCellState.P1;
        int best = target+1;
        int[][] s = new int[board.length][2];
        /*
        * s[i][0] = Allineabili all'i-esima cella
        * s[i][1] = Azioni necessarie
        * */


        if (board[0] == toCheckState) {
            s[0][0] = 1;
            s[0][1] = 0;
        }
        else if (board[0] == oppositeState) {
            s[0][0] = 0;
            s[0][1] = 0;
        }
        else { // status[0] == FREE
            s[0][0] = 1;
            s[0][1] = 1;
        }

        for (int i=1; i<board.length; i++) {
            if (board[i] == oppositeState) {
                s[i][0] = 0;
                s[i][1] = 0;
            }
            else if (s[i-1][0] < target) {
                if (board[i] == toCheckState) {
                    s[i][0] = s[i-1][0] + 1;
                    s[i][1] = s[i-1][1];
                }
                else { // board[i] == FREE
                    s[i][0] = s[i-1][0] + 1;
                    s[i][1] = s[i-1][1] + 1;
                }
            }
            else {
                int toIgnoreMoveCost = 0;
                if (board[i-target] == MNKCellState.FREE) {
                    toIgnoreMoveCost = 1;
                }

                if (board[i] == toCheckState) {
                    s[i][0] = s[i-1][0];
                    s[i][1] = s[i-1][1] - toIgnoreMoveCost;
                }
                else { // board[i] == FREE
                    s[i][0] = s[i-1][0];
                    s[i][1] = s[i-1][1] + 1 - toIgnoreMoveCost;
                }
            }

            if (s[i][0] == target && s[i][1] < best) {
                best = s[i][1];
            }
        }

        return best;
    }

    public int getScore(MNKCellState toCheckState) {
        int best = target+1;

        // Righe
        for (int y=0; y<rows; y++) {
            MNKCellState[] row = new MNKCellState[columns];
            for (int x=0; x<columns; x++) {
                row[x] = matrix[x][y];
            }
            int movesToWin = getMovesToWin(row, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }
        }

        // Colonne
        for (int x=0; x<columns; x++) {
            MNKCellState[] column = new MNKCellState[rows];
            for (int y=0; y<rows; y++) {
                column[y] = matrix[x][y];
            }

            int movesToWin = getMovesToWin(column, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }
        }

        // Obliquo alto sx - basso dx
        int maxDiagonalLength = Math.min(columns, rows);

        int currDiagonalLength = 1;
        for (int x=columns-1; x>=0; x--) {
            MNKCellState[] diagonal = new MNKCellState[currDiagonalLength];
            for (int i=0; i<currDiagonalLength; i++) {
                diagonal[i] = matrix[x+i][i];
            }

            int movesToWin = getMovesToWin(diagonal, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }

            if (currDiagonalLength < maxDiagonalLength) {
                currDiagonalLength++;
            }
        }

        currDiagonalLength = 1;
        for (int y=rows-1; y>=1; y--) {
            MNKCellState[] diagonal = new MNKCellState[currDiagonalLength];
            for (int i=0; i<currDiagonalLength; i++) {
                diagonal[i] = matrix[i][y+i];
            }

            int movesToWin = getMovesToWin(diagonal, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }

            if (currDiagonalLength < maxDiagonalLength) {
                currDiagonalLength++;
            }
        }

        // Obliquo alto dx - basso sx
        currDiagonalLength = 1;
        for (int x=0; x<columns; x++) {
            MNKCellState[] diagonal = new MNKCellState[currDiagonalLength];
            for (int i=0; i<currDiagonalLength; i++) {
                diagonal[i] = matrix[x-i][i];
            }

            int movesToWin = getMovesToWin(diagonal, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }

            if (currDiagonalLength < maxDiagonalLength) {
                currDiagonalLength++;
            }
        }

        currDiagonalLength = 1;
        for (int y=rows-1; y>=1; y--) {
            MNKCellState[] diagonal = new MNKCellState[currDiagonalLength];
            for (int i=0; i<currDiagonalLength; i++) {
                diagonal[i] = matrix[columns-1-i][y+i];
            }

            int movesToWin = getMovesToWin(diagonal, toCheckState);
            if (movesToWin < best) {
                best = movesToWin;
                if (best <= 1) { return 1; };
            }

            if (currDiagonalLength < maxDiagonalLength) {
                currDiagonalLength++;
            }
        }

        return best;
    }

    public int getMaxAlignable(MNKCellState toCheckState) {
        int maxAlignable = -1;

        for (int y=0; y<rows; y++) {
            for (int x=0; x<columns; x++) {
                maxAlignable = Math.max(maxAlignable,
                    Math.max(getHorizontallyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }),
                        Math.max(
                            getVerticallyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }),
                            Math.max(
                                getRightLeftObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }),
                                getLeftRightObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE })
                            )
                        )
                    )
                );
            }
        }
        return maxAlignable;
    }

    public int getMaxAligned(MNKCellState toCheckState) {
        int maxAligned = 0;

        for (int y=0; y<rows; y++) {
            for (int x=0; x<columns; x++) {
                if (getHorizontallyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }) >= target) {
                    maxAligned = Math.max(maxAligned, getHorizontallyAlignedAt(x, y, new MNKCellState[]{ toCheckState}));
                }
                if (getVerticallyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }) >= target) {
                    maxAligned = Math.max(maxAligned, getVerticallyAlignedAt(x, y, new MNKCellState[]{ toCheckState}));
                }
                if (getRightLeftObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }) >= target) {
                    maxAligned = Math.max(maxAligned, getRightLeftObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState}));
                }
                if (getLeftRightObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState, MNKCellState.FREE }) >= target) {
                    maxAligned = Math.max(maxAligned, getLeftRightObliquelyAlignedAt(x, y, new MNKCellState[]{ toCheckState}));
                }
            }
        }
        return maxAligned;
    }

    public String toString() {
        String out = "";
        for (int y=0; y<rows-1; y++) {
            for (int x=0; x<columns; x++) {
                out += (matrix[x][y] == MNKCellState.FREE ?
                            "-"
                        :
                            matrix[x][y] == PLAYER_STATE ?
                            "Me"
                        :
                            "Op")
                        + "\t";
            }
            out += "\n";
        }
        for (int x=0; x<columns; x++) {
            out += (matrix[x][rows-1] == MNKCellState.FREE ?
                    "-"
                    :
                    matrix[x][rows-1] == PLAYER_STATE ?
                            "Me"
                            :
                            "Op")
                    + "\t";
        }
        return out;
    }

    private boolean contains(Object[] toCheck, Object toFind) {
        for (int i=0; i<toCheck.length; i++) {
            if (toCheck[i].equals(toFind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restituisce, a partire da una cella, il numero di celle allineate verticalmente che hanno come stato uno di quelli contenuto in toCheck.
     * */
    public int getVerticallyAlignedAt(int x, int y, MNKCellState[] toCheck) {
        if (!contains(toCheck, matrix[x][y])) { return 0; }

        int aligned = 1;
        for (int i=1; i<=target && x-i>=0; i++) { // Sx
            if (contains(toCheck, matrix[x-i][y])) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x+i<columns; i++) { // Dx
            if (contains(toCheck, matrix[x+i][y])) { aligned++; }
            else { break; }
        }
        return aligned;
    }

    /**
     * Restituisce, a partire da una cella, il numero di celle allineate orizzontalmente che hanno come stato uno di quelli contenuto in toCheck.
     * */
    public int getHorizontallyAlignedAt(int x, int y, MNKCellState[] toCheck) {
        if (!contains(toCheck, matrix[x][y])) { return 0; }

        int aligned = 1;
        for (int i=1; i<=target && y-i>=0; i++) { // Alto
            if (contains(toCheck, matrix[x][y-i])) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && y+i<rows; i++) { // Basso
            if (contains(toCheck, matrix[x][y+i])) { aligned++; }
            else { break; }
        }
        return aligned;
    }

    /**
     * Restituisce, a partire da una cella, il numero di celle allineate obliquamente (alto sx verso basso dx) che hanno come stato uno di quelli contenuto in toCheck.
     * */
    public int getLeftRightObliquelyAlignedAt(int x, int y, MNKCellState[] toCheck) {
        if (!contains(toCheck, matrix[x][y])) { return 0; }

        int aligned = 1;
        for (int i=1; i<=target && x-i>=0 && y-i>=0; i++) { // Alto sx
            if (contains(toCheck, matrix[x-i][y-i])) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x+i<columns && y+i<rows; i++) { // Basso dx
            if (contains(toCheck, matrix[x+i][y+i])) { aligned++; }
            else { break; }
        }
        return aligned;
    }

    /**
     * Restituisce, a partire da una cella, il numero di celle allineate obliquamente (alto dx verso basso sx) che hanno come stato uno di quelli contenuto in toCheck.
     * */
    public int getRightLeftObliquelyAlignedAt(int x, int y, MNKCellState[] toCheck) {
        if (!contains(toCheck, matrix[x][y])) { return 0; }

        int aligned = 1;
        for (int i=1; i<=target && x+i<columns && y-i>=0; i++) { // Alto dx
            if (contains(toCheck, matrix[x+i][y-i])) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=target && x-i>=0 && y+i<rows; i++) { // Basso sx
            if (contains(toCheck, matrix[x-i][y+i])) { aligned++; }
            else { break; }
        }
        return aligned;
    }

    public MNKGameState statusAt(int x, int y) {
        MNKCellState toCheckState = matrix[x][y];
        if (toCheckState == MNKCellState.FREE) { return MNKGameState.OPEN; };

        final MNKGameState WIN_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP1 : MNKGameState.WINP2;
        final MNKGameState LOSS_STATE = PLAYER_STATE == MNKCellState.P1 ? MNKGameState.WINP2 : MNKGameState.WINP1;
        MNKGameState result = toCheckState == PLAYER_STATE ? WIN_STATE : LOSS_STATE;

        if (getVerticallyAlignedAt(x, y, new MNKCellState[]{toCheckState}) >= target) { return result; }
        if (getHorizontallyAlignedAt(x, y, new MNKCellState[]{toCheckState}) >= target) { return result; }
        if (getLeftRightObliquelyAlignedAt(x, y, new MNKCellState[]{toCheckState}) >= target) { return result; }
        if (getRightLeftObliquelyAlignedAt(x, y, new MNKCellState[]{toCheckState}) >= target) { return result; }


        if (size == columns*rows) {
            return MNKGameState.DRAW;
        }
        else {
            return MNKGameState.OPEN;
        }
    }

}
