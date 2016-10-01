package com.initialcoms.ridi.reader.bom.engine;

import android.text.TextUtils;

import java.util.ArrayList;

public class TagNode extends Node {
    static final int TAG_STATUS_OPENER = 0;
    static final int TAG_STATUS_CLOSER = 1;
    static final int TAG_STATUS_FINISHED = 2;
    static final int TAG_STATUS_UNKNOWN = 99;

    private final static String[] TAG_NAMES = {"PAGE", "IMG", "FONT", "LINK", "INDEX", "SUP", "SUB"};
    private final static NodeTag[] TAG_VALUES = {NodeTag.NODE_TAG_PAGE, NodeTag.NODE_TAG_IMG, NodeTag.NODE_TAG_FONT,
            NodeTag.NODE_TAG_LINK, NodeTag.NODE_TAG_INDEX, NodeTag.NODE_TAG_SUP, NodeTag.NODE_TAG_SUB};

    private NodeTag tagtype = NodeTag.NODE_NOTHING;
    private int tagstatus = TAG_STATUS_UNKNOWN;
    private String innerText;
    private ArrayList<Attribute> attributes;

    /**
     * 원본 텍스트에서 어느 위치를 파싱하다 생성되었는지 대략적인 위치를 저장
     */
    private int rawOffset;

    protected TagNode(NodeTag tagtype, int rawOffset) {
        this.tagtype = tagtype;
        this.rawOffset = rawOffset;
    }

    protected static NodeTag getTagTypeFromName(String tagName) {
        for (int i = 0; i < TAG_NAMES.length; i++) {
            if (TAG_NAMES[i].compareToIgnoreCase(tagName) == 0) {
                return TAG_VALUES[i];
            }
        }

        return NodeTag.NODE_NOTHING;
    }

    protected static String getTagNameFromType(NodeTag tagType) {
        for (int i = 0; i < TAG_VALUES.length; ++i) {
            if (TAG_VALUES[i] == tagType) {
                return TAG_NAMES[i];
            }
        }
        return "TEXT";
    }

    // 일반 텍스트일 경우
    public void setText(String text) {
        this.innerText = text;
    }

    public int getRawOffset() {
        return rawOffset;
    }

    public void setRawOffset(int rawOffset) {
        this.rawOffset = rawOffset;
    }

    protected void addAttribute(Attribute attr) {
        if (attributes == null) {
            attributes = new ArrayList<Attribute>();
        }

        attributes.add(attr);
    }

    public NodeTag getTagType() {
        return this.tagtype;
    }

    public int getTagStatus() {
        return this.tagstatus;
    }

    public void setTagStatus(int status) {
        if (getTagType() != NodeTag.NODE_TAG_FONT &&
                getTagType() != NodeTag.NODE_TAG_LINK &&
                getTagType() != NodeTag.NODE_TAG_SUP &&
                getTagType() != NodeTag.NODE_TAG_SUB) {

            status = TAG_STATUS_FINISHED;
        }

        this.tagstatus = status;
    }

    public String getInnerText() {
        return (this.innerText != null) ? this.innerText : "";
    }

    private boolean hasAttributes() {
        return (attributes != null);
    }

    /*protected*/
    public String getStringAttr(String key) {
        if (hasAttributes()) {
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attr = attributes.get(i);

                if (attr.getKeyName().compareToIgnoreCase(key) == 0) {
                    return attr.getValue();
                }
            }
        }

        return null;
    }

    protected int getIntAttr(String key, int defaultValue) {
        if (hasAttributes()) {
            String value = getStringAttr(key);

            if (!TextUtils.isEmpty(value)) {
                return Integer.parseInt(value);
            }
        }

        return defaultValue;
    }

    protected boolean getBooleanAttr(String key, boolean defaultValue) {
        if (hasAttributes()) {
            String value = getStringAttr(key);

            if (value == null) {
                return defaultValue;
            }

            if (value.compareToIgnoreCase("true") == 0) {
                return true;
            } else if (value.compareToIgnoreCase("false") == 0) {
                return false;
            }
        }

        return defaultValue;
    }
}
