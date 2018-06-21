package com.ridi.books.viewer.reader.bom.engine;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;

public class LayoutManager {
    private static final String TAG = "LayoutManager";

    public static final int PURPOSE_RENDERING = 0;
    public static final int PURPOSE_PAGING = 1;

    private NodeManager nodeMgr;
    private IRenderer mRenderer;

    private RendererContext mCurrentContext;
    private ArrayList<RendererContext> mContextStack;

    private float pos_x;
    private float pos_y;
    private int offset;
    private float rowHeight;

    private int mPurpose;

    public LayoutManager() {
        mContextStack = new ArrayList<RendererContext>();
        initContext();

        pos_x = pos_y = 0;
        offset = 0;

        mPurpose = PURPOSE_RENDERING;
    }

    private void initContext() {
        mContextStack.clear();

        mCurrentContext = new RendererContext();
        mContextStack.add(mCurrentContext);
    }

    public void restoreContext(NodeInfo from) {
        TagNode node = (TagNode) nodeMgr.getNodeAtIndex(from.getNodeIndex());
        if (node != null) {
            node = (TagNode) node.getParent();
        }
        ArrayList<TagNode> nodes = new ArrayList<TagNode>();
        RendererContext context;

        initContext();

        // 역순으로 노드 쌓기
        while (node != null) {
            // CLOSER 태그나 FINISHED된 태그들은 컨텍스트에 영향이 없으므로
            // OPENER만 컨텍스트에 반영
            if (node.getTagStatus() == TagNode.TAG_STATUS_OPENER) {
                switch (node.getTagType()) {
                    case NODE_TAG_FONT:
                    case NODE_TAG_SUP:
                    case NODE_TAG_SUB:
                        nodes.add(0, node);
                        break;
                    default:
                }
            }

            node = (TagNode) node.getParent();
        }

        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);

            switch (node.getTagType()) {
                case NODE_TAG_FONT:
                    context = makeContextFromFontTagNode(node);
                    pushContext(context);
                    break;

                case NODE_TAG_SUP:
                    context = makeContextFromSupTagNode(node);
                    pushContext(context);
                    break;

                case NODE_TAG_SUB:
                    context = makeContextFromSubTagNode(node);
                    pushContext(context);
                    break;

                default:
            }
        }

        applyContext();
    }

    public NodeInfo layoutPage(NodeInfo from) {
        initPosition(); // 좌표 초기화
        rowHeight = 0;

        if (nodeMgr == null) {
            return null;
        }

        TagNode node = (TagNode) nodeMgr.getNodeAtIndex(from.getNodeIndex());

        offset = from.getOffset();

        // node 순회
        while (node != null) {
            // 닫는 태그이면 context POP하고 다음 노드 탐색
            if (node.getTagStatus() == TagNode.TAG_STATUS_CLOSER) {
                popContext();
                node = (TagNode) node.getNextNode();
                offset = 0;
                continue;
            }

            NodeInfo nextNode = null;
            RendererContext rendContext = null;

            switch (node.getTagType()) {
                case NODE_TAG_FONT:
                    // 스타일 컨텍스트 적용
                    rendContext = makeContextFromFontTagNode(node);
                    pushContext(rendContext);

                    break;

                case NODE_TAG_IMG:
                    boolean beginningOfPage = (node.getNodeIndex() == from.getNodeIndex());
                    nextNode = layoutImage(node, new NodeInfo(node.getNodeIndex(), offset, node.getRawOffset() + offset),
                            beginningOfPage);
                    if (nextNode != null)
                        return nextNode;

                    break;

                case NODE_TAG_PAGE:
                    // 페이지 종료시킴
                    if (node.getNextNode() != null)
                        return new NodeInfo((TagNode) node.getNextNode());

                    break;

                case NODE_TEXT:
                    nextNode = layoutText(node, new NodeInfo(node.getNodeIndex(), offset, node.getRawOffset() + offset));
                    if (nextNode != null)
                        return nextNode;
                    break;

                case NODE_TAG_SUP:
                    // 윗첨자
                    rendContext = makeContextFromSupTagNode(node);
                    pushContext(rendContext);

                    break;

                case NODE_TAG_SUB:
                    // 아랫첨자
                    rendContext = makeContextFromSubTagNode(node);
                    pushContext(rendContext);

                    break;

                default:
            }

            node = (TagNode) node.getNextNode();
            offset = 0;
        }

        return null;
    }

    private NodeInfo layoutText(TagNode node, NodeInfo from) {
        String innerText = node.getInnerText();
        int textOffset = offset;

        while (textOffset < innerText.length()) {
            if (pos_y >= mRenderer.getCanvasHeight() - mRenderer.getMarginBottom() - mRenderer.getFontHeight()) {
                // 끝난 위치의 노드정보 전달
                offset = textOffset;

                return new NodeInfo(node.getNodeIndex(), offset, node.getRawOffset() + offset);
            }

            float segment_x = pos_x;
            float segment_y = pos_y;

            TextSegment segment = layoutTextSegment(innerText, textOffset);

            if (segment.getSegmentText().length() == 0) {
                textOffset += segment.getOffsetLengthToSkip();
                continue;
            }

            PointF addPoint = applySubscriptPoint(new PointF(segment_x, segment_y));
            NodeInfo addNodeInfo = new NodeInfo(from.getNodeIndex(), textOffset, node.getRawOffset() + textOffset);

            RendererContext segmentContext;

            // 왼쪽 정렬(기본)일 때만 양쪽정렬 처리
            if (segment.isWrapped() && mCurrentContext.getAlign() == RendererContext.ALIGN_LEFT) {
                segmentContext = getBaseContextFromCurrentContext();
                segmentContext.setWrapped(true);
            } else {
                segmentContext = mCurrentContext;
            }

            deliverAlignedTextRenderable(segmentContext, segment.getSegmentText(), addPoint.x, addPoint.y,
                    mRenderer.measureText(segment.getSegmentText()), addNodeInfo);

            textOffset += segment.getOffsetLengthToSkip();
        }

        offset = textOffset;

        return null;
    }

    private void updateRowHeight(float height) {
        if (rowHeight < height)
            rowHeight = height;
    }

    private void lineBreak() {
        // line feed
        moveY(rowHeight);
        rowHeight = 0;

        // kelly return
        initPositionX();
    }

    /**
     * 이름이 애매한데, 이미 wrap이 결정된 상태에서 어디를 기준으로 자를지를 계산한다.
     * 1) 마침표같은 구둣점으로 잘리지 않게, 2) 영단어와 숫자는 잘리지 않게 하는 기준이 있다.
     *
     * @param sb
     * @return 호출한 쪽에서는 리턴된 곳을 기준으로 자르면 됨.
     */
    private int wordWrap(StringBuilder sb) {
        int index = sb.length() - 1;

        // 마지막이 구둣점으로 끝나면, 공백까지 자름.
        if (isEndingSpecialChar(sb.charAt(index))) {
            --index;
            while (index >= 0 && (isAlphaOrNumber(sb.charAt(index)) || isSpecialChar(sb.charAt(index)))) {
                --index;
            }
            return index;
        }

        if (index >= 0 && (isAlphaOrNumber(sb.charAt(index)) || isSpecialChar(sb.charAt(index)))) {
            // word wrap 모드
            --index;
            while (index >= 0 && (isAlphaOrNumber(sb.charAt(index)) || isSpecialChar(sb.charAt(index)))) {
                --index;
            }
            ++index;
        }

        return index;
    }

    /*
     * 주어진 환경에서 한 줄에 들어갈만큼만 세그먼트로 뽑아냄 여기서 좌표(pos_x, pos_y) 핸들링함
     */
    private TextSegment layoutTextSegment(String innerText, int textOffset) {
        StringBuilder sb = new StringBuilder("");
        float textWidth = pos_x - mRenderer.getMarginLeft();
        int skipCharacterCount = 0;

        updateRowHeight(mRenderer.getFontHeight() + mRenderer.getLineHeight());

        while (textOffset < innerText.length()) {
            char ch = innerText.charAt(textOffset++);

            // \r 무시
            if (ch == '\r') {
                skipCharacterCount++;
                continue;
            }

            if (ch == '\n') {
                // 줄넘김이면
                lineBreak();
                updateRowHeight(mRenderer.getFontHeight() + mRenderer.getLineHeight());

                skipCharacterCount++;

                String segmentText = sb.toString();
                return new TextSegment(segmentText, segmentText.length() + skipCharacterCount, false);
            }

            sb.append(ch);

            float charWidth = mRenderer.measureText(String.valueOf(ch));
            textWidth += charWidth;

            // 한 줄 넘어가면
            if (textWidth > mRenderer.getContentWidth()) {
                int index = wordWrap(sb);

                if (index > 0) {
                    // wrapped된 단어가 화면에 들어가는 길이면
                    sb.setLength(index);

                    // 양쪽 정렬 상태에서 맨 오른쪽 단어가 공백이면 트리밍
                    if (sb.charAt(sb.length() - 1) == ' ') {
                        sb.setLength(sb.length() - 1);
                        skipCharacterCount++;
                    }
                } else {
                    // wrapped된 단어가 content width 보다 길면
                    sb.setLength(sb.length() - 1);
                }

                // 다음줄의 시작(넣어서 넘친 문자)이 스페이스는 노노
                if (ch == ' ') {
                    skipCharacterCount++;
                }

                lineBreak();
                updateRowHeight(mRenderer.getFontHeight() + mRenderer.getLineHeight());

                String segmentText = sb.toString();
                return new TextSegment(segmentText, segmentText.length() + skipCharacterCount, true);
            }
        }

        // while 다 돌고 버퍼에 남아있는 텍스트 있으면 렌더러블 추가
        if (sb.length() > 0) {
            float bufWidth = mRenderer.measureText(sb.toString());
            moveX(bufWidth);
        }

        String segmentText = sb.toString();

        updateRowHeight(mRenderer.getFontHeight() + mRenderer.getLineHeight());

        return new TextSegment(segmentText, segmentText.length() + skipCharacterCount, false);
    }

    private void deliverAlignedTextRenderable(RendererContext context, String text, float x, float y,
                                              float measuredLength, NodeInfo nodeinfo) {
        float text_x = x;

        switch (context.getAlign()) {
            case RendererContext.ALIGN_LEFT:
                text_x = x;
                break;

            case RendererContext.ALIGN_CENTER:
                text_x = (mRenderer.getCanvasWidth() - measuredLength) / 2;
                break;

            case RendererContext.ALIGN_RIGHT:
                text_x = mRenderer.getCanvasWidth() - mRenderer.getMarginRight() - measuredLength;
                break;
        }

        mRenderer.deliverTextRenderable(context, text, text_x, y, nodeinfo);
    }

    private NodeInfo layoutImage(TagNode node, NodeInfo from, boolean beginningOfPage) {
        String imgSrc = node.getStringAttr("src");
        String imgCaption = node.getStringAttr("caption");
        int imgAlign = getAlign(node.getStringAttr("align"), RendererContext.ALIGN_CENTER);
        int imgWidth = node.getIntAttr("width", -1);
        int imgHeight = node.getIntAttr("height", -1);
        boolean imgFullscreen = node.getBooleanAttr("fullscreen", false);
        // boolean imgZoom = node.getBooleanAttr("zoom", false);
        // boolean imgFloat = node.getBooleanAttr("float", false);


        if (imgSrc == null) {
            // 이미지 경로 데이터가 없으면
            return null;
        }

        // 태그에 이미지 크기가 없을 경우
        if (imgWidth == -1 || imgHeight == -1) {
            // performance trick
            if (mPurpose == PURPOSE_PAGING && imgFullscreen) {
                imgWidth = 1;
                imgHeight = 1;
            } else {
                SizeF imgSize = mRenderer.getImageSize(imgSrc);

                if (imgSize == null)
                    return null;

                imgWidth = (int) imgSize.width;
                imgHeight = (int) imgSize.height;
            }
        }


        float captionWidth = 0;
        float captionHeight = 0;

        // 캡션 전처리
        if (imgCaption != null) {
            // 캡션이 추가될 공간 계산
            captionHeight = mRenderer.getLineHeight() / 2 + mRenderer.getFontHeight();

            for (int i = 0; i < imgCaption.length(); i++) {
                captionWidth = mRenderer.measureText(imgCaption.substring(0, i + 1));

                if (captionWidth > mRenderer.getContentWidth()) {
                    // 캡션이 Content 가로넓이를 넘어가면 ... 으로 처리
                    if (imgCaption.length() > 3) {
                        imgCaption = imgCaption.substring(0, i - 1) + "...";
                        break;
                    }
                }
            }
        }

        // 풀스크린일 경우
        if (imgFullscreen) {
            if (beginningOfPage) {
                SizeF contentSize = new SizeF(mRenderer.getContentWidth(), mRenderer.getContentHeight() - captionHeight);
                float display_height = contentSize.height;

                final SizeF actualSize = new SizeF(imgWidth, imgHeight);
                SizeF scaledSize = new SizeF(imgWidth, imgHeight);

                // 1. 2번 단계에서 어느 한 축으로 잘라도 실 이미즈가 너무 크면 문제가 있기 때문에 좀 줄임
                if (contentSize.width / display_height >= actualSize.width / actualSize.height) {
                    // 세로로 먼저 클리핑
                    if (actualSize.height > display_height) {
                        scaledSize.width = display_height * actualSize.width / actualSize.height;
                        scaledSize.height = display_height;
                    }
                } else {
                    // 가로로 클리핑
                    if (actualSize.width > contentSize.width) {
                        scaledSize.width = contentSize.width;
                        scaledSize.height = contentSize.width * actualSize.height / actualSize.width;
                    }
                }

                SizeF rect_size = new SizeF(scaledSize);

                // 2. contentSize.width x display_height 에 들어갈 수 있도록 clip
                if (scaledSize.width > contentSize.width) {
                    // 세로로 잘랐는데 가로가 넘치면
                    rect_size.height = scaledSize.height * contentSize.width / scaledSize.width;
                    rect_size.width = contentSize.width;
                }
                if (scaledSize.height > display_height) {
                    // 가로로 잘랐는데 세로가 넘치면
                    rect_size.width = scaledSize.width * display_height / scaledSize.height;
                    rect_size.height = display_height;
                }

                pos_x = (contentSize.width - rect_size.width) / 2 + mRenderer.getMarginLeft();
                pos_y = (display_height - rect_size.height) / 2 + mRenderer.getMarginTop();

                mRenderer.deliverImageRenderable(imgSrc, new RectF(pos_x, pos_y, pos_x + rect_size.width, pos_y
                        + rect_size.height));

                // 캡션이 있으면 딜리버
                if (imgCaption != null) {
                    RendererContext centerContext = new RendererContext();
                    centerContext.setAlign(RendererContext.ALIGN_CENTER);

                    deliverAlignedTextRenderable(centerContext, imgCaption, pos_x, pos_y + rect_size.height
                            + (mRenderer.getLineHeight() / 2), captionWidth, from);
                }

                if (node.getNextNode() != null)
                    return new NodeInfo((TagNode) node.getNextNode());

                // 다음 노드가 없으면 끝냄
                return null;
            } else {
                // 다음 페이지에 풀스크린으로 그리도록
                return new NodeInfo((TagNode) node);
            }

        } else {

            // fullscreen이 아닐경우, 이미지 크기가 화면보다 크면 줄인다.
            final int MAX_NONFULLSCREEN_IMAGE_WIDTH = (int) mRenderer.getContentWidth();
            final int MAX_NONFULLSCREEN_IMAGE_HEIGHT = (int) (mRenderer.getContentHeight() - mRenderer.getLineHeight() - captionHeight);

            if (imgWidth > MAX_NONFULLSCREEN_IMAGE_WIDTH) {
                imgHeight = imgHeight * MAX_NONFULLSCREEN_IMAGE_WIDTH / imgWidth;
                imgWidth = MAX_NONFULLSCREEN_IMAGE_WIDTH;
            }
            if (imgHeight > MAX_NONFULLSCREEN_IMAGE_HEIGHT) {
                imgWidth = imgWidth * MAX_NONFULLSCREEN_IMAGE_HEIGHT / imgHeight;
                imgHeight = MAX_NONFULLSCREEN_IMAGE_HEIGHT;
            }

            if (pos_y + imgHeight + captionHeight > mRenderer.getCanvasHeight() - mRenderer.getMarginBottom()) {
                // 현재 페이지에 넣을 수 없는 경우 다음페이지로 넘긴다.
                return new NodeInfo((TagNode) node);
            } else {
                // 이미지 Renderable 추가
                float img_x = pos_x;

                switch (imgAlign) {
                    case RendererContext.ALIGN_LEFT:
                        img_x = pos_x;
                        break;
                    case RendererContext.ALIGN_CENTER:
                        img_x = (mRenderer.getCanvasWidth() - imgWidth) / 2;
                        break;

                    case RendererContext.ALIGN_RIGHT:
                        img_x = mRenderer.getCanvasWidth() - mRenderer.getMarginRight() - imgWidth;
                        break;
                }

                // 이미지 딜리버
                mRenderer.deliverImageRenderable(imgSrc, new RectF(img_x, pos_y, img_x + imgWidth, pos_y + imgHeight));
                moveY(imgHeight + mRenderer.getLineHeight());

                // 캡션이 있으면 딜리버
                if (imgCaption != null) {
                    mRenderer.deliverTextRenderable(new RendererContext(), imgCaption, img_x, pos_y, from);
                    moveY(captionHeight);

                    initPositionX();
                }
            }
        }

        return null;
    }

    private PointF applySubscriptPoint(PointF point) {
        // 안드로이드는 point의 좌표 아래쪽으로 글자가 렌더링 됨
        if (mCurrentContext.getSubScript() == RendererContext.SUBSCRIPT_SUP) {
            return new PointF(point.x, point.y - mRenderer.getFontHeight() / 6f);
        } else if (mCurrentContext.getSubScript() == RendererContext.SUBSCRIPT_SUB) {
            return new PointF(point.x, point.y + mRenderer.getFontHeight() / 1.8f);
        }

        // SUBSCRIPT_NONE
        return point;
    }

    private RendererContext makeContextFromSupTagNode(TagNode node) {
        RendererContext rendContext;

        rendContext = getBaseContextFromCurrentContext();
        rendContext.setSubScript(RendererContext.SUBSCRIPT_SUP);

        return rendContext;
    }

    private RendererContext makeContextFromSubTagNode(TagNode node) {
        RendererContext rendContext;

        rendContext = getBaseContextFromCurrentContext();
        rendContext.setSubScript(RendererContext.SUBSCRIPT_SUB);

        return rendContext;
    }

    // FONT 태그에서 렌더러 컨텍스트 뽑기
    private RendererContext makeContextFromFontTagNode(TagNode node) {
        RendererContext rendContext;

        rendContext = getBaseContextFromCurrentContext();

        // String fontFace = node.getStringAttr("face");
        String fontColor = node.getStringAttr("color"); // use
        int fontSize = node.getIntAttr("size", -1); // use
        int fontAlign = getAlign(node.getStringAttr("align"), rendContext.getAlign());
        // boolean fontBold = node.getBooleanAttr("bold", false);
        // boolean fontItalic = node.getBooleanAttr("italic", false);

        // if (fontFace != null)
        // readContext.setFontFace(fontFace);

        if (fontSize != -1)
            rendContext.setFontSize(fontSize);

        if (fontColor != null)
            rendContext.setFontColor(fontColor);

        rendContext.setAlign(fontAlign);

        return rendContext;
    }

    private RendererContext getBaseContextFromCurrentContext() {
        if (mCurrentContext == null) {
            return new RendererContext();
        } else {
            return new RendererContext(mCurrentContext);
        }
    }

    private void initPosition() {
        initPositionX();
        initPositionY();
    }

    private void initPositionX() {
        pos_x = mRenderer.getMarginLeft();
    }

    private void initPositionY() {
        pos_y = mRenderer.getMarginTop();
    }

    private void moveX(float length) {
        pos_x += length;
    }

    private void moveY(float length) {
        pos_y += length;
    }

    private void applyContext() {
        if (mContextStack.size() > 0) {
            mCurrentContext = mContextStack.get(mContextStack.size() - 1);

            mRenderer.changeFontSize(mCurrentContext.getFontSize(), mCurrentContext.getSubScript());
            mRenderer.changeFontColor(mCurrentContext.getFontColor());
        } else {
            mCurrentContext = null;
        }
    }

    private void pushContext(RendererContext rc) {
        mContextStack.add(rc);
        applyContext();
    }

    private void popContext() {
        if (mContextStack.size() > 1)
            mContextStack.remove(mContextStack.size() - 1);
        else {
            Log.d(TAG, "context stack is empty.");
        }

        applyContext();
    }

    private int getAlign(String value, int defaultValue) {
        if (value == null)
            return defaultValue;

        if (value.compareToIgnoreCase("left") == 0)
            return RendererContext.ALIGN_LEFT;
        else if (value.compareToIgnoreCase("right") == 0)
            return RendererContext.ALIGN_RIGHT;
        else if (value.compareToIgnoreCase("center") == 0)
            return RendererContext.ALIGN_CENTER;

        return defaultValue;
    }

    public NodeManager getNodeManager() {
        return nodeMgr;
    }

    public void setNodeManager(NodeManager nodeMgr) {
        this.nodeMgr = nodeMgr;
    }

    public IRenderer getRenderer() {
        return mRenderer;
    }

    public void setRenderer(IRenderer renderer) {
        this.mRenderer = renderer;
    }

    private boolean isEndingSpecialChar(char ch) {
        switch (ch) {
            case ')':
            case '"':
            case '\'':
            case '!':
            case ',':
            case '.':
            case '>':
            case '?':
            case '}':
            case '\u201D': // ”
            case '\u2019': // ’
                return true;
            default:
                return false;
        }
    }

    private boolean isSpecialChar(char ch) {
        switch (ch) {
            case '(':
            case ')':
            case '"':
            case '\'':
            case '!':
            case ',':
            case '.':
            case '<':
            case '>':
            case '?':
            case '{':
            case '}':
                return true;
            default:
                return false;
        }
    }

    private boolean isAlphaOrNumber(char ch) {
        if (ch >= '0' && ch <= '9')
            return true;
        if (ch >= 'A' && ch <= 'Z')
            return true;
        if (ch >= 'a' && ch <= 'z')
            return true;

        return false;
    }

    public String getImgSrc(NodeInfo from) {
        if (nodeMgr == null || from == null) {
            return null;
        }

        TagNode node = (TagNode) nodeMgr.getNodeAtIndex(from.getNodeIndex());

        if (node.getTagType() == NodeTag.NODE_TAG_IMG) {
            return node.getStringAttr("src");
        }

        return null;
    }

    public void setPurpose(int purpose) {
        mPurpose = purpose;
    }

    private class TextSegment {
        private String segmentText;
        private int offsetLengthToSkip = 0;
        private boolean wrapped = false;

        public TextSegment(String segmentText, int offsetLengthToSkip, boolean wrapped) {
            this.segmentText = segmentText;
            this.offsetLengthToSkip = offsetLengthToSkip;
            this.wrapped = wrapped;
        }

        public String getSegmentText() {
            return segmentText;
        }

        public int getOffsetLengthToSkip() {
            return offsetLengthToSkip;
        }

        public boolean isWrapped() {
            return wrapped;
        }
    }
}
