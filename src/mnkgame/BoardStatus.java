package mnkgame;

import java.util.LinkedList;
import java.util.PriorityQueue;

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

    private MNKCellState[][] matrix;
    private int size;
    private int columns, rows, target;
    private final MNKCellState PLAYER_STATE, OPPONENT_STATE;

    private Score[][] rowScore_player, columnScore_player, diagonalLeftRightScore_player, diagonalRightLeftScore_player;
    private Score[][] rowScore_opponent, columnScore_opponent, diagonalLeftRightScore_opponent, diagonalRightLeftScore_opponent;


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

        rowScore_player = new Score[columns][rows];
        columnScore_player = new Score[columns][rows];
        diagonalLeftRightScore_player = new Score[columns][rows];
        diagonalRightLeftScore_player = new Score[columns][rows];
        rowScore_opponent = new Score[columns][rows];
        columnScore_opponent = new Score[columns][rows];
        diagonalLeftRightScore_opponent = new Score[columns][rows];
        diagonalRightLeftScore_opponent = new Score[columns][rows];
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

    private Score[] getScoresArray(MNKCellState board[], MNKCellState toCheckState) {
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
        }

        // Propagazione
        for (int i=0; i<s.length; i++) {
            if (s[i].aligned == target) {
                for (int j=1; j<=target-1; j++) {
                    if ( (s[i-j].aligned == 0 && s[i-j].moves == 0) || (s[i-j].aligned == target && s[i-j].moves < s[i].moves) ) {
                        continue;
                    }
                    s[i-j].aligned = s[i].aligned;
                    s[i-j].moves = s[i].moves;
                }
            }
        }

        return s;
    }

    private MNKCellState[] getRowAt(int start_x, int start_y) {
        MNKCellState[] row = new MNKCellState[columns];
        for (int x=0; x<columns; x++) {
            row[x] = matrix[x][start_y];
        }
        return row;
    }

    private MNKCellState[] getColumnAt(int start_x, int start_y) {
        MNKCellState[] column = new MNKCellState[rows];
        for (int y=0; y<rows; y++) {
            column[y] = matrix[start_x][y];
        }
        return column;
    }

    private MNKCellState[] getDiagonalLeftRightAt(int start_x, int start_y) {
        LinkedList<MNKCellState> buffer = new LinkedList<>();

        int i = start_x, j = start_y;
        while (i < columns && j < rows) {
            buffer.addLast(matrix[i][j]);
            i++; j++;
        }

        i = start_x-1;
        j = start_y-1;
        while (i >= 0 && j >= 0) {
            buffer.addFirst(matrix[i][j]);
            i--; j--;
        }

        return buffer.toArray(new MNKCellState[buffer.size()]);
    }

    private MNKCellState[] getDiagonalRightLeftAt(int start_x, int start_y) {
        LinkedList<MNKCellState> buffer = new LinkedList<>();

        int i = start_x, j = start_y;
        while (i < columns && j >= 0) {
            buffer.addFirst(matrix[i][j]);
            i++; j--;
        }

        i = start_x-1;
        j = start_y+1;
        while (i >= 0 && j < rows) {
            buffer.addLast(matrix[i][j]);
            i--; j++;
        }

        return buffer.toArray(new MNKCellState[buffer.size()]);
    }

    private void setScore(Score[][] score, int x, int y, Score value) {
        if (value.aligned == 0 && value.moves == 0) {
            score[x][y] = new Score(0, target*10);
        }
        else if(value.aligned != target) {
            score[x][y] = new Score(0, target + value.moves);
        }
        else {
            score[x][y] = new Score(value);
        }
    }

    private void fillRowScoreAt(int x, int y) {
        if (rowScore_player[x][y] != null && rowScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(getRowAt(x, y), PLAYER_STATE);
        for (int j=0; j<columns; j++) {
            setScore(rowScore_player, j, y, moves[j]);
        }

        // Avversario
        moves = getScoresArray(getRowAt(x, y), OPPONENT_STATE);
        for (int j=0; j<columns; j++) {
            setScore(rowScore_opponent, j, y, moves[j]);
        }
    }

    private void fillRowsScore() {
        for (int i=0; i<rows; i++) {
            fillRowScoreAt(0, i);
        }
    }

    private void fillColumnScoreAt(int x, int y) {
        if (columnScore_player[x][y] != null && columnScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(getColumnAt(x, y), PLAYER_STATE);
        for (int j=0; j<rows; j++) {
            setScore(columnScore_player, x, j, moves[j]);
        }

        // Avversario
        moves = getScoresArray(getColumnAt(x, y), OPPONENT_STATE);
        for (int j=0; j<rows; j++) {
            setScore(columnScore_opponent, x, j, moves[j]);
        }
    }

    private void fillColumnsScore() {
        for (int i=0; i<columns; i++) {
            fillColumnScoreAt(i, 0);
        }
    }

    private void fillDiagonalLeftRightScoreAt(int x, int y) {
        if (diagonalLeftRightScore_player[x][y] != null && diagonalLeftRightScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(getDiagonalLeftRightAt(x, y), PLAYER_STATE);
        int i = x, j = y;
        while(i > 0 && j > 0) {
            i--; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalLeftRightScore_player, i, j, moves[k]);
            i++; j++;
        }

        // Avversario
        moves = getScoresArray(getDiagonalLeftRightAt(x, y), OPPONENT_STATE);
        i = x; j = y;
        while(i > 0 && j > 0) {
            i--; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalLeftRightScore_opponent, i, j, moves[k]);
            i++; j++;
        }
    }

    private void fillDiagonalLeftRightScore() {
        for (int i=0; i<rows; i++) {
            fillDiagonalLeftRightScoreAt(0, i);
        }
        for (int i=1; i<columns; i++) {
            fillDiagonalLeftRightScoreAt(i, 0);
        }
    }

    private void fillDiagonalRightLeftScoreAt(int x, int y) {
        if (diagonalRightLeftScore_player[x][y] != null && diagonalRightLeftScore_opponent[x][y] != null) { return; }
        Score[] moves;

        // Giocatore
        moves = getScoresArray(getDiagonalRightLeftAt(x, y), PLAYER_STATE);
        int i = x, j = y;
        while (i < columns-1 && j > 0) {
            i++; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalRightLeftScore_player, i, j, moves[k]);
            i--; j++;
        }

        // Avversario
        moves = getScoresArray(getDiagonalRightLeftAt(x, y), OPPONENT_STATE);
        i = x; j = y;
        while (i < columns-1 && j > 0) {
            i++; j--;
        }
        for (int k=0; k<moves.length; k++) {
            setScore(diagonalRightLeftScore_opponent, i, j, moves[k]);
            i--; j++;
        }
    }

    private void fillDiagonalRightLeftScore() {
        for (int i=0; i<rows; i++) {
            fillDiagonalRightLeftScoreAt(columns-1, i);
        }
        for (int i=columns-2; i>=0; i--) {
            fillDiagonalRightLeftScoreAt(i, 0);
        }
    }

    public void generateScore() {
        fillRowsScore();
        fillColumnsScore();
        fillDiagonalLeftRightScore();
        fillDiagonalRightLeftScore();
    }

    public void generateScoreAt(int x, int y) {
        fillRowScoreAt(x, y);
        fillColumnScoreAt(x, y);
        fillDiagonalLeftRightScoreAt(x, y);
        fillDiagonalRightLeftScoreAt(x, y);
    }

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

    public int getGlobalBestMovesToWin(MNKCellState toCheckStatus) {
        int best = target*10;

        for (int i=0; i<rows; i++) {
            for (int j=0; j<columns; j++) {
                int s = getMovesToWinAt(j, i, toCheckStatus);
                if (s < best) best = s;
            }
        }

        return best;
    }

    public PriorityQueue<EvaluationPosition> getBestMovesQueue() {
        PriorityQueue<EvaluationPosition> moves = new PriorityQueue<>((move1, move2) -> move1.score - move2.score);

        for (int y=0; y<rows; y++) {
            for (int x=0; x<columns; x++) {
                if (matrix[x][y] == MNKCellState.FREE) {
                    moves.add(new EvaluationPosition(x, y, columnScore_player[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, rowScore_player[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, diagonalLeftRightScore_player[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, diagonalRightLeftScore_player[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, columnScore_opponent[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, rowScore_opponent[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, diagonalLeftRightScore_opponent[x][y].moves));
                    moves.add(new EvaluationPosition(x, y, diagonalRightLeftScore_opponent[x][y].moves));
                }
            }
        }

        return moves;
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
