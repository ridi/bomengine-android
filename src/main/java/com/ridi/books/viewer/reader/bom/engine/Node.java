package com.ridi.books.viewer.reader.bom.engine;

import java.util.ArrayList;

public class Node {
    private int nodeIndex = -1;
    private Node parent = null;
    private Node nextNode = null;
    private ArrayList<Node> children;

    public Node() {
        children = new ArrayList<Node>();
    }

    boolean addChild(Node n) {
        if (n == null) {
            return false;
        }

        n.setParent(this);

        return children.add(n);
    }

    boolean removeChild(Node n) {
        return children.remove(n);
    }

    protected boolean hasChild() {
        return !children.isEmpty();
    }

    protected int numberOfChildren() {
        return children.size();
    }

    // node index로 노드 검색
    protected Node getNodeAtIndex(int index) {
        Node node = this;

        while (node != null) {
            if (node.getNodeIndex() == index)
                return node;

            node = node.getNextNode();
        }

        return null;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node n) {
        this.parent = n;
    }

    public Node getNextNode() {
        return this.nextNode;
    }

    public void setNextNode(Node n) {
        this.nextNode = n;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }
}