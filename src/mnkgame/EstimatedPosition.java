package mnkgame;

import java.util.Objects;

public class EstimatedPosition implements Comparable<EstimatedPosition> {
    public int x, y, score;
    private int blocked, openDirections;

    public EstimatedPosition(int x, int y, int score) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.blocked = 0;
        this.openDirections = 0;
    }

    public EstimatedPosition(int x, int y, int score, int blocked, int openDirections) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.blocked = blocked;
        this.openDirections = openDirections;
    }

    public int compareTo(EstimatedPosition ep) {
        int diff =  ep.score - this.score;

        if (diff == 0) {
            diff = ep.blocked - this.blocked;
        }

        /*if (diff == 0) {
            diff = ep.openDirections - this.openDirections;
        }*/

        return diff;
    }

    @Override
    public String toString() {
        return "{ (" + x + ", " + y + ") Score: " + score + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EstimatedPosition that = (EstimatedPosition) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}