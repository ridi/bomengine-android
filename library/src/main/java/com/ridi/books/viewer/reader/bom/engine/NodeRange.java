package com.ridi.books.viewer.reader.bom.engine;

import java.io.Serializable;


public class NodeRange implements Serializable {
    private static final long serialVersionUID = -775369098081991619L;
    private NodeInfo startNodeInfo;
    private NodeInfo endNodeInfo;
    private int startPos;
    private int endPos;

    public NodeRange(int startPos, int endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public NodeRange(NodeInfo startNodeInfo, NodeInfo endNodeInfo) {
        this.startPos = startNodeInfo.getRawOffset();
        this.endPos = endNodeInfo.getRawOffset();
        this.startNodeInfo = startNodeInfo;
        this.endNodeInfo = endNodeInfo;
    }

    public BomLocation getStartNodeInfo() {
        return new RawLocation(getStartRawOffset());
    }

    public BomLocation getEndNodeInfo() {
        return new RawLocation(getEndRawOffset());
    }

    public int getStartRawOffset() {
        if (startPos == 0 && startNodeInfo != null) {
            startPos = startNodeInfo.getRawOffset();
        }

        return startPos;
    }

    public int getEndRawOffset() {
        if (endPos == 0 && endNodeInfo != null) {
            endPos = endNodeInfo.getRawOffset();
        }

        return endPos;
    }

    public boolean contains(NodeInfo nodeinfo) {
        return (getStartRawOffset() <= nodeinfo.getRawOffset() &&
                nodeinfo.getRawOffset() <= getEndRawOffset());
    }

    public boolean equals(Object o) {
        if (!(o instanceof NodeRange))
            return false;

        NodeRange nr = (NodeRange) o;
        return (getStartRawOffset() == nr.getStartRawOffset())
                && (getEndRawOffset() == nr.getEndRawOffset());
    }
}
