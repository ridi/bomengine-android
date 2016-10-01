package com.initialcoms.ridi.reader.bom.engine;

public class NodeInfo extends BomLocation {
    private static final long serialVersionUID = -7708016617938783103L;

    private int mNodeIndex;
    private int mOffset;
    private int mRawOffset;

    public NodeInfo(int index, int offset, int rawOffset) {
        mNodeIndex = index;
        mOffset = offset;
        mRawOffset = rawOffset;
    }

    public NodeInfo(NodeInfo baseNodeInfo) {
        mNodeIndex = baseNodeInfo.getNodeIndex();
        mOffset = baseNodeInfo.getOffset();
        mRawOffset = baseNodeInfo.getRawOffset();
    }

    public NodeInfo(TagNode tagNode) {
        mNodeIndex = tagNode.getNodeIndex();
        mOffset = 0;
        mRawOffset = tagNode.getRawOffset();
    }

    public int getNodeIndex() {
        return mNodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        mNodeIndex = nodeIndex;
    }

    public int getOffset() {
        return mOffset;
    }

    public int getRawOffset() {
        return mRawOffset;
    }

    public boolean equals(Object o) {
        if (!(o instanceof NodeInfo))
            return false;

        NodeInfo ni = (NodeInfo) o;

        return mNodeIndex == ni.getNodeIndex() && mOffset == ni.getOffset() &&
                mRawOffset == ni.getRawOffset();
    }

    public void moveOffset(int delta) {
        mOffset += delta;
        mRawOffset += delta;
    }
}
