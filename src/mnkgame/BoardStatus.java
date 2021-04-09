package mnkgame;

public class BoardStatus {
    private MNKCellState[][] matrix;
    private int size;
    int columns, rows, target;

    public BoardStatus(int columns, int rows, int target) {
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
        matrix[x][y] = MNKCellState.FREE;
        size--;
    }

    public boolean isFreeAt(int x, int y) {
        return matrix[x][y] == MNKCellState.FREE;
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

    public MNKGameState statusAt(int x, int y, MNKCellState playerState) {
        MNKCellState toCheckState = matrix[x][y];
        if (toCheckState == MNKCellState.FREE) { return MNKGameState.OPEN; };

        final MNKGameState WIN_STATE = playerState == MNKCellState.P1 ? MNKGameState.WINP1 : MNKGameState.WINP2;
        final MNKGameState LOSS_STATE = playerState == MNKCellState.P1 ? MNKGameState.WINP2 : MNKGameState.WINP1;
        int aligned;
        MNKGameState result = toCheckState == playerState ? WIN_STATE : LOSS_STATE;

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
