package player;

import mnkgame.*;

import java.util.LinkedList;

public class Node {
    public Node parent;
    public LinkedList<Node> children;
    public MNKCell action;
    public int score;
    public boolean alphabeta; // Indica se il nodo è stato elaborato da Alphabeta pruning
    public boolean endState;  // Indica se il nodo contiene una configurazione di gioco finale

    /**
     * @implNote Costo: O(1)
     * */
    public Node(Node parent, MNKCell action) {
        this.parent = parent;
        this.children = new LinkedList<>();
        this.action = action;
        this.score = 0;
        this.alphabeta = false;
        this.endState = false;
    }

    /**
     * Cancella la lista di figli e imposta come figlio il nodo in input
     * @implNote Costo: O(1)
     * */
    public void setAsOnlyChild(Node child) {
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
}