package mnkgame;

public class EvaluationPosition implements Comparable<EvaluationPosition> {
    public int x, y, score;

    public EvaluationPosition(int x, int y, int score) {
        this.x = x;
        this.y = y;
        this.score = score;
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
}
