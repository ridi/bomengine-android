package com.initialcoms.ridi.reader.bom.engine;

public class RendererContext {
    public static final int SUBSCRIPT_NONE = 0;
    public static final int SUBSCRIPT_SUP = 1;
    public static final int SUBSCRIPT_SUB = 2;
    protected static final int ALIGN_LEFT = 0;
    protected static final int ALIGN_CENTER = 1;
    protected static final int ALIGN_RIGHT = 2;
    private int mFontSize;
    private int mAlign;
    private int mSubscript;
    private boolean mWrapped;
    private String mFontColor;

    public RendererContext() {
        this.mFontSize = 3;
        this.mAlign = ALIGN_LEFT;
        this.mSubscript = SUBSCRIPT_NONE;
        this.mFontColor = null;
        this.mWrapped = false;
    }

    protected RendererContext(RendererContext baseContext) {
        this();
        this.mFontSize = baseContext.getFontSize();
        this.mFontColor = baseContext.getFontColor();
        this.mSubscript = baseContext.getSubScript();
    }

    public int getFontSize() {
        return mFontSize;
    }

    public void setFontSize(int mFontSize) {
        this.mFontSize = mFontSize;
    }

    public String getFontColor() {
        return mFontColor;
    }

    public void setFontColor(String mFontColor) {
        this.mFontColor = mFontColor;
    }

    public int getAlign() {
        return mAlign;
    }

    public void setAlign(int align) {
        this.mAlign = align;
    }

    public boolean isWrapped() {
        return this.mWrapped;
    }

    public void setWrapped(boolean wrap) {
        this.mWrapped = wrap;
    }

    public int getSubScript() {
        return this.mSubscript;
    }

    public void setSubScript(int subscript) {
        this.mSubscript = subscript;
    }
}
