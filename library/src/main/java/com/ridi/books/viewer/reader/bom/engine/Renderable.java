package com.ridi.books.viewer.reader.bom.engine;

import android.graphics.RectF;

public abstract class Renderable {
    protected RectF mRect;
    protected RendererContext mStyleContext;

    public RectF getRect() {
        return mRect;
    }

    public void setRect(RectF mRect) {
        this.mRect = mRect;
    }

    public RendererContext getStyleContext() {
        return mStyleContext;
    }

    public void setStyleContext(RendererContext mStyleContext) {
        this.mStyleContext = mStyleContext;
    }

    abstract public void render(IRenderer renderer);
}
