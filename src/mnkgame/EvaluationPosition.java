package mnkgame;

import java.util.Objects;

public class EvaluationPosition implements Comparable<EvaluationPosition> {
    public int x, y, score, frequency;
    public MNKCellState state;

    public EvaluationPosition(int x, int y, int score, MNKCellState state, int frequency) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.state = state;
        this.frequency = frequency;
    }

    public EvaluationPosition(EvaluationPosition ev) {
        this.x = ev.x;
        this.y = ev.y;
        this.score = ev.score;
        this.state = ev.state;
        this.frequency = ev.frequency;
    }

    public int compareTo(EvaluationPosition ev) {
        int diff = this.score - ev.score;
        return diff == 0 ? (ev.frequency - this.frequency) : diff;
    }

    @Override
    public String toString() {
        return "{ (" + x + ", " + y + ") " + score + " " + frequency + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationPosition that = (EvaluationPosition) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
