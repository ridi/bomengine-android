package com.ridi.viewer.reader.bom.engine;

import android.graphics.Bitmap;
import android.graphics.Typeface;

public interface IResourceProvider {
    Bitmap getBitmap(String imgSrc, int width, int height);

    SizeF getBitmapSize(String imgSrc);

    Typeface getFont(String fontName);
}
