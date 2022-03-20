package player;

public class EstimatedPosition implements Comparable<EstimatedPosition> {
    public int x, y, score;
    private int blocked;

    public EstimatedPosition(int x, int y, int score) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.blocked = 0;
    }

    public EstimatedPosition(int x, int y, int score, int blocked) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.blocked = blocked;
    }

    @Override
    public int compareTo(EstimatedPosition ep) {
        /*
        * Ordinamento per score e poi per blocked
        * */
        int diff = ep.score - this.score;

        if (diff == 0) { diff = ep.blocked - this.blocked; }

        return diff;
    }


    @Override
    public String toString() {
        return "{ (" + x + ", " + y + ") Score: " + score + " }";
    }
}