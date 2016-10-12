package com.ridi.books.viewer.reader.bom.engine;

public class ImageRenderable extends Renderable {
    private String mImgSrc;

    public ImageRenderable(String src) {
        this.mImgSrc = src;
    }

    @Override
    public void render(IRenderer renderer) {
        if (mImgSrc != null)
            renderer.drawImage(mImgSrc, mRect.left, mRect.top, mRect.right, mRect.bottom);
    }
}
