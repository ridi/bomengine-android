package com.ridi.viewer.reader.bom.engine;


public class RawLocation extends BomLocation {
    private static final long serialVersionUID = 7755365806059425486L;
    private int offset;

    public RawLocation(int offset) {
        this.offset = offset;
    }

    public static RawLocation fromString(String locationString) {
        try {
            return new RawLocation(Integer.parseInt(locationString));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public int getRawOffset() {
        return offset;
    }
}
