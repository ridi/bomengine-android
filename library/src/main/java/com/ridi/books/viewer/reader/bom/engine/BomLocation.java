package com.ridi.books.viewer.reader.bom.engine;

import java.io.Serializable;

public abstract class BomLocation implements Comparable<BomLocation>, Serializable {
    private static final long serialVersionUID = -6779870542595559252L;

    public abstract int getRawOffset();

    @Override
    public int compareTo(BomLocation another) {
        return getRawOffset() - another.getRawOffset();
    }

    @Override
    public String toString() {
        return String.valueOf(getRawOffset());
    }
}
