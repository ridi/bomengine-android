package com.ridi.viewer.reader.bom.engine;


public class TextRenderable extends Renderable {
    private String mText;

    public TextRenderable(String text) {
        mText = text;
    }

    @Override
    public void render(IRenderer renderer) {
        renderer.changeFontSize(mStyleContext.getFontSize(), mStyleContext.getSubScript());
        renderer.changeFontColor(mStyleContext.getFontColor());

        renderer.drawText(mText, mRect.left, mRect.top, mStyleContext);
    }
}