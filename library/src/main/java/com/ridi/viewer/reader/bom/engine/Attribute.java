package com.ridi.viewer.reader.bom.engine;

class Attribute {
    private String key;
    private String value;

    protected Attribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKeyName() {
        return key;
    }

    public void setKeyName(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
