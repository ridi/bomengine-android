package com.ridi.books.viewer.reader.bom.engine;


public class NodeManager {
    private Node rootNode;

    public NodeManager(Node rootNode) {
        this.rootNode = rootNode;
    }

    public Node getNodeAtIndex(int nodeIndex) {
        return rootNode.getNodeAtIndex(nodeIndex);
    }

    public Node getRootNode() {
        return rootNode;
    }

    public boolean hasChildAtRootNode() {
        return rootNode.hasChild();
    }

    public NodeInfo convertRawOffsetToNodeInfo(int rawOffset) {
        TagNode node = (TagNode) rootNode.getNextNode();
        TagNode lastNode = node;
        NodeInfo nodeInfo = new NodeInfo(node.getNodeIndex(), rawOffset - node.getRawOffset(), rawOffset);

        while (node != null) {
            if (node.getRawOffset() > rawOffset) {
                break;
            }

            lastNode = node;
            nodeInfo = new NodeInfo(lastNode.getNodeIndex(), rawOffset - lastNode.getRawOffset(), rawOffset);
            node = (TagNode) node.getNextNode();
        }

        return nodeInfo;
    }

    public NodeRange getWordNodeRangeAt(NodeInfo nodeinfo) {
        NodeInfo startNodeInfo = new NodeInfo(nodeinfo);
        NodeInfo endNodeInfo = new NodeInfo(nodeinfo);

        TagNode node = (TagNode) rootNode.getNodeAtIndex(nodeinfo.getNodeIndex());
        String innerText = node.getInnerText();
        int index;

        // 앞 단어 찾기
        index = nodeinfo.getOffset();
        while (index > 0) {
            --index;
            if (innerText.charAt(index) == ' ' ||
                    innerText.charAt(index) == '\n') {
                ++index;
                break;
            }
        }

        startNodeInfo.moveOffset(index - nodeinfo.getOffset());

        // 뒷 단어 찾기
        index = nodeinfo.getOffset();
        while (index < innerText.length() - 1) {
            ++index;
            if (innerText.charAt(index) == ' ' ||
                    innerText.charAt(index) == '\n') {
                --index;
                break;
            }
        }

        endNodeInfo.moveOffset(index - nodeinfo.getOffset());

        return new NodeRange(startNodeInfo, endNodeInfo);
    }

    // NodeRange에 해당하는 텍스트들 리턴
    public String getTextInRange(NodeRange range) {
        if (rootNode.numberOfChildren() == 0)
            return null;

        StringBuilder texts = new StringBuilder("");

        NodeInfo startNodeInfo = (NodeInfo) convertRawOffsetToNodeInfo(range.getStartRawOffset());
        NodeInfo endNodeInfo = (NodeInfo) convertRawOffsetToNodeInfo(range.getEndRawOffset());

        TagNode node = (TagNode) rootNode.getNodeAtIndex(startNodeInfo.getNodeIndex());

        while (node != null) {
            if (node.getNodeIndex() > endNodeInfo.getNodeIndex())
                break;

            if (node.getNodeIndex() == startNodeInfo.getNodeIndex()) {
                if (startNodeInfo.getNodeIndex() != endNodeInfo.getNodeIndex()) {
                    texts.append(node.getInnerText().substring(startNodeInfo.getOffset()));
                } else {
                    texts.append(node.getInnerText().substring(startNodeInfo.getOffset(), endNodeInfo.getOffset() + 1));
                }
            } else if (node.getNodeIndex() > startNodeInfo.getNodeIndex() &&
                    node.getNodeIndex() < endNodeInfo.getNodeIndex()) {
                texts.append(node.getInnerText());
            } else if (node.getNodeIndex() == endNodeInfo.getNodeIndex()) {
                texts.append(node.getInnerText().substring(0, endNodeInfo.getOffset() + 1));
            }

            node = (TagNode) node.getNextNode();
        }

        return texts.toString();
    }
}
