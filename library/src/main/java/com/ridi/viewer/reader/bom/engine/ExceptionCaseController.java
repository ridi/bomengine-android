package com.ridi.viewer.reader.bom.engine;

/*
 * 리디북스 뷰어엔진 V2에서의 예외처리
 * 
 * 1) 태그와 태그 사이의 엔터(\n) 제거
 * 2) IMG 태그가 fullScreen 속성을 가지고 있고 바로 다음에 PAGE태그가 올 경우 PAGE 태그 제거
 * 
 */

import android.util.Log;

public class ExceptionCaseController {
    private static final String TAG = "ExceptionCaseController";

    public static void removeExceptionCase(NodeManager nodeMgr) {
        TagNode startNode = (TagNode) nodeMgr.getRootNode();
        TagNode node = (TagNode) startNode.getNextNode();

        while (node != null) {
            if (isTagNode(node)) {
                // 예외 케이스 1 처리
                removeReturnCharacterNodeBetweenTagNodes(node);

                // 예외 케이스 2 처리
                removePageTagAfterFullscreenImage(node);
            }

            node = (TagNode) node.getNextNode();
        }
    }

    // 케이스1 : 태그 노드 사이의 엔터(\n) 제거
    private static void removeReturnCharacterNodeBetweenTagNodes(TagNode node) {
        TagNode nextNode = (TagNode) node.getNextNode();
        if (nextNode == null)
            return;

        TagNode nextOfNextNode = (TagNode) nextNode.getNextNode();
        if (nextOfNextNode == null)
            return;

        // 풀스크린이 아닌 경우는 처리하지 않음
        if (nextOfNextNode.getTagType() == NodeTag.NODE_TAG_IMG && nextOfNextNode.getBooleanAttr("fullscreen", false) == false) {
            return;
        }

        if (isReturnCharacterNode(nextNode) && isTagNode(nextOfNextNode)) {
            Log.d(TAG, "case1 cleared!!");
            // 다음 노드를 잘라냄
            removeNextNode(node);
        }
    }

    // 케이스2 : IMG 태그가 fullScreen 속성을 가지고 있고 바로 다음에 PAGE태그가 올 경우 PAGE 태그 제거
    private static void removePageTagAfterFullscreenImage(TagNode node) {
        TagNode nextNode = (TagNode) node.getNextNode();
        if (nextNode == null)
            return;

        if (isTagToFillWholePage(node) && nextNode.getTagType() == NodeTag.NODE_TAG_PAGE) {
            Log.d(TAG, "case2 cleared!!");
            // 다음 노드를 잘라냄
            removeNextNode(node);
        }
    }

    // node 다음 노드를 제거하고 node의 nextNode를 다다음 node로 연결시킴
    private static void removeNextNode(TagNode node) {
        TagNode nextNode = (TagNode) node.getNextNode();
        TagNode nextOfNextNode = (TagNode) nextNode.getNextNode();

        node.setNextNode(nextOfNextNode);

        if (nextNode.getParent() != null)
            nextNode.getParent().removeChild(nextNode);

        nextNode = null;
    }

    private static boolean isReturnCharacterNode(TagNode node) {
        // TEXT 노드만 취급
        if (node == null || isTagNode(node))
            return false;

        String innerText = node.getInnerText();

        return (innerText.length() == 1 && innerText.charAt(0) == '\n');
    }

    // PAGE 태그이거나 IMG 태그이면서 fullscreen 태그이면
    private static boolean isTagToFillWholePage(TagNode node) {
        if (node.getTagType() == NodeTag.NODE_TAG_PAGE)
            return true;

        return node.getTagType() == NodeTag.NODE_TAG_IMG && node.getBooleanAttr("fullscreen", false);
    }

    private static boolean isTagNode(TagNode node) {
        return (node != null && node.getTagType() != NodeTag.NODE_TEXT);
    }
}
