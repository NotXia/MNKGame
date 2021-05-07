package mnkgame;

import java.util.PriorityQueue;
import java.util.LinkedList;

public class Node {
    public Node parent;
    public PriorityQueue<Node> children;
    public MNKCell action;
    public int score;
    public boolean isEndState;

    public Node(Node parent, MNKCell action) {
        this.parent = parent;
        this.children = new PriorityQueue<>((node1, node2) -> node2.score - node1.score);
        this.action = action;
        this.score = 0;
        this.isEndState = false;
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