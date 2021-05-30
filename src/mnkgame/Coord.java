package mnkgame;

import java.util.Objects;

public class Coord {
    public int x, y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + " " + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Coord coord = (Coord) o;
        return x == coord.x && y == coord.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public boolean isOnTheSameRowOf(Coord c) {
        return this.y == c.y;
    }

    public boolean isOnTheSameColumnOf(Coord c) {
        return this.x == c.x;
    }

    public boolean isOnTheSameMainDiagonalOf(Coord c) {
        return (this.x - c.x) == (this.y - c.y);
    }

    public boolean isOnTheSameSecondaryDiagonalOf(Coord c) {
        return (this.x - c.x) == -(this.y - c.y);
    }

    public double distance(Coord c) {
        return Math.sqrt(Math.pow(Math.abs(x-c.x), 2) + Math.pow(Math.abs(y-c.y), 2));
    }
}
