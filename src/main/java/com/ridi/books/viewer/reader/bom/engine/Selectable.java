package com.ridi.books.viewer.reader.bom.engine;

import android.graphics.RectF;


public class Selectable {
    private RectF rect;
    private NodeInfo nodeinfo;

    public Selectable(RectF rect, NodeInfo nodeinfo) {
        this.rect = rect;
        this.nodeinfo = nodeinfo;
    }

    public RectF getRect() {
        return new RectF(rect);
    }

    public NodeInfo getNodeInfo() {
        return nodeinfo;
    }
}
