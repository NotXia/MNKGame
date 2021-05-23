package mnkgame;

import java.util.Objects;

public class EvaluationPosition implements Comparable<EvaluationPosition> {
    public int x, y, score;

    public EvaluationPosition(int x, int y, int score) {
        this.x = x;
        this.y = y;
        this.score = score;
    }

    public EvaluationPosition(EvaluationPosition ev) {
        this.x = ev.x;
        this.y = ev.y;
        this.score = ev.score;
    }

    public int compareTo(EvaluationPosition ev) {
        return this.score - ev.score;
    }

    @Override
    public String toString() {
        return "{" +
                "(" + x +
                ", " + y +
                ") " + score +
                '}';
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
