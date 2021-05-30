package mnkgame;

import java.util.Objects;

public class PositionEstimation implements Comparable<PositionEstimation> {
    public int x, y, score;
    public int aligned, blocked;
    public int blockedOpenDirections;
    public boolean isDiagonal;

    public PositionEstimation(int x, int y, int score, int aligned, int blocked, int blockedOpenDirections, boolean isDiagonal) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.aligned = aligned;
        this.blocked = blocked;
        this.blockedOpenDirections = blockedOpenDirections;
        this.isDiagonal = isDiagonal;
    }

    public int compareTo(PositionEstimation pe) {
        int diff =  pe.score - this.score;

        if (diff == 0) {
            diff = pe.aligned - this.aligned;
        }
        if (diff == 0) {
            diff = pe.blocked - this.blocked;
        }
        if (diff == 0) {
            diff = pe.blockedOpenDirections - this.blockedOpenDirections;
        }
        if (diff == 0) {
            diff = this.isDiagonal ? -1 : pe.isDiagonal ? 1 : 0;
        }

        return diff;
    }

    @Override
    public String toString() {
        return "{ (" + x + ", " + y + ") Score: " + score + "  Aligned: " + aligned + "  Blocked: " + blocked + "  Open blocked: " + blockedOpenDirections + "  Diagonal: " + isDiagonal + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PositionEstimation that = (PositionEstimation) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
