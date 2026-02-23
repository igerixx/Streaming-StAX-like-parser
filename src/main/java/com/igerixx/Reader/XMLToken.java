package com.igerixx.Reader;

import java.nio.charset.StandardCharsets;

public class XMLToken {
    private int type;
    private byte[] stringBuffer;
    private char[] charStringBuffer;
    private int length;

    public XMLToken() {}

    public XMLToken(int type, byte[] stringBuffer, int length) {
        this.type = type;
        this.stringBuffer = stringBuffer;
        this.charStringBuffer = null;
        this.length = length;
    }

    public XMLToken(int type, char[] stringBuffer, int length) {
        this.type = type;
        this.stringBuffer = null;
        this.charStringBuffer = stringBuffer;
        this.length = length;
    }

    public XMLToken(int type, char[] stringBuffer) {
        this.type = type;
        this.stringBuffer = null;
        this.charStringBuffer = stringBuffer;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        if (stringBuffer != null)
            return new String(stringBuffer, 0, length, StandardCharsets.UTF_8);
        return new String(charStringBuffer, 0, length);
    }
}
