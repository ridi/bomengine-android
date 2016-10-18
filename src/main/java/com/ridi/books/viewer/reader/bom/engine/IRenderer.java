package com.ridi.books.viewer.reader.bom.engine;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;

public interface IRenderer {
    void drawText(String text, float x, float y, RendererContext context);

    void drawImage(String imageSrc, float left, float top, float right, float bottom);

    SizeF getImageSize(String src);

    float measureText(String text);

    float getFontHeight();

    void changeFontFace(Typeface fontFace);

    void changeFontSize(int fontSize, int subScript);

    void changeFontColor(String color);

    void prepare();

    void render(Canvas canvas);

    void deliverTextRenderable(RendererContext context, String text, float x, float y, NodeInfo nodeinfo);

    void deliverImageRenderable(String imgSrc, RectF rect);

    // inquires
    float getCanvasWidth();

    float getCanvasHeight();

    float getContentWidth();

    float getContentHeight();

    float getMarginLeft();

    float getMarginRight();

    float getMarginTop();

    float getMarginBottom();

    float getLineHeight();
}
