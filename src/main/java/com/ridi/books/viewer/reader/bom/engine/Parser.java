package com.ridi.books.viewer.reader.bom.engine;

import android.util.Log;

public class Parser {
    private static final String TAG = "Parser";

    private static final int CHARACTER_TYPE_TEXT = 0;
    private static final int CHARACTER_TYPE_TAG = 1;
    private static final int PARSE_ATTR_INVALID_FORMAT = -1;
    private static final int PARSE_ATTR_OK = 0;
    private static final int PARSE_ATTR_END_OPNER = 1;
    private static final int PARSE_ATTR_END_FINISHED = 2;

    private Reader reader = null;
    private TagNode rootNode = null;
    private TagNode workingNode = null;
    private TagNode lastAddedNode = null;
    private int nodeIndex = 0;

    public Parser(Reader reader) {
        rootNode = new TagNode(NodeTag.NODE_NOTHING, 0);
        rootNode.setNodeIndex(0);
        workingNode = rootNode;
        lastAddedNode = rootNode;
        this.reader = reader;
    }

    public static String removeTag(String plainText) {
        String removedText = "";
        boolean inTag = false;
        boolean firstClosure = true;

        if (plainText == null)
            return null;

        for (int i = 0; i < plainText.length(); i++) {
            if (inTag == false && plainText.charAt(i) == '{')
                inTag = true;

            if (inTag == false)
                removedText += plainText.charAt(i);

            if (plainText.charAt(i) == '}') {
                if (inTag == false && firstClosure == true) {
                    removedText = "";
                    firstClosure = false;
                }

                inTag = false;
            }
        }

        return removedText;
    }

    // 일반 텍스트 노드 생성
    private TagNode makeTextNode(String text, int offset) {
        TagNode textnode = new TagNode(NodeTag.NODE_TEXT, offset);
        textnode.setText(text);

        return textnode;
    }

    public void parse() {
        parse(false);
    }

    // 파싱
    public void parse(boolean escapeTags) {
        int lastOffset = 0;
        StringBuilder parsedText = new StringBuilder("");

        while (reader.isReadable()) {
            char ch = reader.getCurrPosChar();

            if (!escapeTags && guessNextCharacterType(ch) == CHARACTER_TYPE_TAG) {
                // 태그가 아닐 경우 돌아갈 위치 기억
                reader.rememberCurrPos();

                // 태그 파싱
                TagNode tagnode = parseTag();

                // null이 아니면 정상적인 태그 형식인 것으로 보면 됨
                if (tagnode != null) {
                    // 태그 정보를 추가 전에 이미 파싱된 텍스트가 있으면 TEXT 태그로 먼저 추가
                    if (parsedText.length() > 0) {
                        if (tagnode.getTagType() != NodeTag.NODE_TAG_PAGE ||
                                !"gift".equals(tagnode.getStringAttr("class"))) {
                            addNodeToWorkingNode(makeTextNode(parsedText.toString(), lastOffset));
                        }
                        parsedText.setLength(0);

                        lastOffset = reader.getRememberedPos();
                    }

                    tagnode.setRawOffset(lastOffset);
                    procNodeWithWorkingNode(tagnode);

                    lastOffset = reader.getPos();
                    continue;
                } else {
                    // 제대로 된 형식의 태그가 아니기 때문에 원래 위치로 돌리고 { 는 일반 텍스트 형태로 더함
                    reader.backToRememberedPos();
                }
            }

            parsedText.append(ch);
            reader.goForward(1);
        }

        // 내용 끝까지 파싱이 끝난 후 남아있는 텍스트가 있으면 TEXT 태그로 만들어 추가
        if (parsedText.length() > 0) {
            addNodeToWorkingNode(makeTextNode(parsedText.toString(), lastOffset));
        }

        // 노드 찍어보기
        //printNode2(rootNode);
    }

    // 파싱한 노드를 적절한 처리
    private void procNodeWithWorkingNode(TagNode tagnode) {
        if (tagnode.getTagType() == NodeTag.NODE_TAG_PAGE &&
                "gift".equals(tagnode.getStringAttr("class"))) {
            return;
        }
        addNodeToWorkingNode(tagnode);

        // FONT, LINK 등 중첩되는 노드중 닫히지 않은 것(not FINISHED)이면 워킹노드 변경
        if (tagnode.getTagStatus() == TagNode.TAG_STATUS_OPENER) {
            workingNode = tagnode;
        } else if (tagnode.getTagStatus() == TagNode.TAG_STATUS_CLOSER) {
            TagNode parentNode = (TagNode) workingNode.getParent();

            // 같은 태그여야만 워킹노드를 부모로 옮김
            if (parentNode != null && workingNode.getTagType() == tagnode.getTagType()) {
                workingNode = parentNode;
            }
        } else if (tagnode.getTagStatus() == TagNode.TAG_STATUS_FINISHED) {
            // DO NOTHING
        }
    }

    private void addNodeToWorkingNode(TagNode n) {
        n.setNodeIndex(++nodeIndex);
        workingNode.addChild(n);

        lastAddedNode.setNextNode(n);
        lastAddedNode = n;
    }

    private int guessNextCharacterType(char ch) {
        switch (ch) {
            case '{':
                return CHARACTER_TYPE_TAG;

            default:
                return CHARACTER_TYPE_TEXT;
        }
    }

    @SuppressWarnings("unused")
    private void printNode2(TagNode n) {
        int index = 0;
        TagNode node;

        node = (TagNode) n.getNextNode();

        while (node != null) {
            Log.d(TAG, "index: " + node.getNodeIndex() + ", " + node.getRawOffset() + " : " + reader.getWholeText().substring(node.getRawOffset(), node.getRawOffset() + 10));

            node = (TagNode) node.getNextNode();
        }
    }

    @SuppressWarnings("unused")
    private void printNode(TagNode n, int depth) {
        String tab = "";

        for (int i = 0; i < depth; i++) {
            tab += "\t";
        }

        Log.d(TAG, tab + TagNode.getTagNameFromType(n.getTagType())
                + ", " + n.getInnerText());

        for (int i = 0; i < n.numberOfChildren(); i++) {
            //printNode((TagNode) n.get(i), depth + 1);
        }
    }

    // 태그 파싱
    private TagNode parseTag() {
        int tagBeginPos = reader.getPos();
        boolean closerTag = false;

        // skip '{'
        reader.goForward(1);

        // Closer 태그인지 확인
        if (reader.getCurrPosChar() == '/') {
            closerTag = true;
            reader.goForward(1);
        }

        // 태그명 파싱
        String tagName = parseTagName();
        NodeTag tagType = TagNode.getTagTypeFromName(tagName);

        // Closer 태그인지 확인
        if (reader.getCurrPosChar() == '/') {
            closerTag = true;
            reader.goForward(1);
        }

        if (tagType != NodeTag.NODE_NOTHING) {
            // 맞는 태그명이면 태그노드 생성
            TagNode tagnode = new TagNode(tagType, tagBeginPos);

            if (!closerTag) {
                // 닫는 태그가 아니면 attribute 파싱
                if (parseAttributes(tagnode) == false) {
                    // 비정상적인 구문으로 되어있으면
                    return null;
                }
            } else {
                // 닫는 태그일 경우 처리 } 까지 읽음
                reader.readUntil('}');
                reader.goForward(1);

                tagnode.setTagStatus(TagNode.TAG_STATUS_CLOSER);
            }

            return tagnode;
        } else {
            // 태그명에 해당하는게 아니면 태그가 아님
            return null;
        }
    }

    // 태그명 파싱 : 특수문자 혹은 화이트 스페이스까지의 문자열
    private String parseTagName() {
        return reader.getNextToken();
    }

    // attribute 파싱
    private boolean parseAttributes(TagNode tagnode) {
        // attribute 이름 파싱
        StringBuilder name = new StringBuilder("");
        String value;

        while (reader.isReadable()) {
            name.setLength(0);
            int ret = parseAttrName(tagnode, name);

            if (ret == PARSE_ATTR_INVALID_FORMAT) {
                return false;
            } else if (ret == PARSE_ATTR_END_OPNER) {
                tagnode.setTagStatus(TagNode.TAG_STATUS_OPENER);
                return true;
            } else if (ret == PARSE_ATTR_END_FINISHED) {
                tagnode.setTagStatus(TagNode.TAG_STATUS_FINISHED);
                return true;
            }

            if (parseValueSyntax() == false) {
                // =" 가 안나오면 실패
                return false;
            }

            // attribute 값 파싱
            value = parseAttrValue();

            if (value == null) {
                // 내용 끝까지 가도 "가 안나오면
                return false;
            }

            // 파싱된 attribute 한 개 추가
            Attribute attr = new Attribute(name.toString(), value);
            tagnode.addAttribute(attr);
        }

        return true;
    }

    // attribute name 파싱
    private int parseAttrName(TagNode tagnode, StringBuilder name) {
        char ch;

        while (reader.isReadable()) {
            // 화이트 스페이스들은 모두 건너띄고 나오는 첫 문자
            ch = reader.getCharSkipWhite();

            if (ch == '{') {
                // 또 다른 {가 나오면 잘못된 태그
                return PARSE_ATTR_INVALID_FORMAT;
            }
            if (ch == '}') {
                // 그냥 태그가 닫히면 OPENER TAG
                // 하지만 FONT, LINK가 아니면 FINISH 된 것으로 처리함
                tagnode.setTagStatus(TagNode.TAG_STATUS_OPENER);
                return PARSE_ATTR_END_OPNER;
            } else if (ch == '/' && reader.getCharSkipWhite() == '}') {
                // /} 형태로 닫히면 FINISHED
                tagnode.setTagStatus(TagNode.TAG_STATUS_FINISHED);
                return PARSE_ATTR_END_FINISHED;
            }

            // attribute name이 끝나고 = 가 나오면 끝
            if (ch == '=') {
                break;
            }

            name.append(ch);
        }

        return PARSE_ATTR_OK;
    }

    private boolean parseValueSyntax() {
        // 화이트 스페이스를 무시하고 "가 바로 나오는지 확인(attribute= 다음 부분 체크)
        if (reader.getCharSkipWhite() == '"') {
            return true;
        }

        return false;
    }

    // attribute_name=" 까지 파싱되어 있으므로 다음 "가 나올 때까지 파싱
    private String parseAttrValue() {
        StringBuilder value = new StringBuilder("");

        reader.readUntil('"', value);
        reader.goForward(1);

        return value.toString();
    }

    public Node getRootNode() {
        return rootNode;
    }
}
