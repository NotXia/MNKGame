package mnkgame;

import java.util.LinkedList;

public class Node {
    public Node parent;
    public LinkedList<Node> children;
    public MNKCell action;
    public int score;

    public Node(Node parent, MNKCell action, int M, int N, int K) {
        this.parent = parent;
        this.children = new LinkedList<>();
        this.action = action;
        score = 42;
    }

    public LinkedList<MNKCell> getMarkedCells() {
        LinkedList<MNKCell> markedCells = new LinkedList<>();
        Node iter = this.parent;

        markedCells.addFirst(this.action);
        while (iter != null) {
            markedCells.addFirst(iter.action);
            iter = iter.parent;
        }

        return markedCells;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    @Override
    public String toString() {
        return "Node{ " +
                "children=" + children +
                ", action=" + action +
                ", score=" + score +
                " }";
    }
}