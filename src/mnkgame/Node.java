package mnkgame;

import java.util.LinkedList;

public class Node {
    public Node parent;
    public LinkedList<Node> children;
    public MNKCell action;
    public int score;
    public boolean alphabeta;

    public Node(Node parent, MNKCell action) {
        this.parent = parent;
        this.children = new LinkedList<>();
        this.action = action;
        this.score = 0;
        this.alphabeta = false;
    }

    /**
     * Restituisce una lista contenente tutte le celle marcate fino alla mossa attuale
     * @implNote Costo: O(h)    h = altezza dell'albero
     * */
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

    /**
     * Svuota la lista di figli e imposta come figlio il nodo in input
     * @implNote Costo: O(1)
     * */
    public void setSelectedChild(Node child) {
        // Idea ottimizzazione: Array di LinkedList (in base al target). [i] contiene una lista con tutte le mosse di quel determinato punteggio
        this.children = new LinkedList<>();
        this.children.add(child);
    }

    /**
     * Indica se il nodo è una foglia
     * @implNote Costo: O(1)
     * */
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