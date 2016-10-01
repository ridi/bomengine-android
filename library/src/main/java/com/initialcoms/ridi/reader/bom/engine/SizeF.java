package com.initialcoms.ridi.reader.bom.engine;

public class SizeF {
    float width;
    float height;

    public SizeF() {
    }

    public SizeF(float w, float h) {
        width = w;
        height = h;
    }

    public SizeF(SizeF s) {
        width = s.width;
        height = s.height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
