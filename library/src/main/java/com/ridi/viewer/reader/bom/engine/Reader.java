package com.ridi.viewer.reader.bom.engine;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Reader {
    private static final String TAG = "Reader";

    private String text;
    private int pos = 0;
    private int remember = 0;
    private int mTotalLength = 0;

    public Reader(String text) {
        this.text = text;
        mTotalLength = text.length();
    }

    public Reader(File file) {
        try {
            byte[] filebody = getBytesFromFile(file);
            this.text = new String(filebody);
            mTotalLength = text.length();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Close the input stream
        is.close();


        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        return bytes;
    }

    // 현재 위치 리턴
    public int getPos() {
        return pos;
    }

    // 화이트스페이스, 특수문자 등이 나올 때까지의 문자열 리턴
    public String getNextToken() {
        StringBuilder sb = new StringBuilder();

        while (isReadable()) {
            char ch = getChar();

            if (Character.isWhitespace(ch) ||
                    !Character.isLetterOrDigit(ch)) {
                goBackward(1);
                return sb.toString();
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    // 현재 위치의 문자 리턴 후 위치 하나 이동
    public char getChar() {
        char ch = getCurrPosChar();
        goForward(1);

        return ch;
    }

    public char getCharSkipWhite() {
        while (isReadable()) {
            char ch = getCurrPosChar();
            goForward(1);

            // 화이트 스페이스 건너띄는 처리
            if (Character.isWhitespace(ch))
                continue;

            return ch;
        }

        return 0;
    }

    // 현재 위치의 문자 리턴
    public char getCurrPosChar() {
        return text.charAt(pos);
    }

    // 다음 위치의 글자 리턴
    public char getNextPosChar() {
        return text.charAt(pos + 1);
    }

	/*
     * readUntil - 원하는 문자가 나올 때까지 읽어옴(pos는 해당 문자 위치 가르키고 있음)
	 * 
	 * 리턴값
	 *   1 : 성공적으로 리턴
	 *   0 : 읽어들여온 문자 없음
	 *  -1 : stop 문자열에 걸림
	 */

    protected int readUntil(char c) {
        return readUntil(c, null, null);
    }

    protected int readUntil(char c, StringBuilder dest) {
        return readUntil(c, dest, null);
    }

    private int readUntil(char c, StringBuilder dest, String stopChars) {
        while (isReadable()) {
            char ch = getChar();

            // 파싱시 멈춰야하는 문자가 나타났을 경우 처리
            if (stopChars != null && stopChars.indexOf(ch) != -1) {
                goBackward(1);
                return -1;
            }

            if (ch == c) {
                goBackward(1);
                return 1;
            } else {
                if (dest != null) {
                    dest.append(ch);
                }
            }
        }

        return 0;
    }

    // 읽을 수 있는 상태인지
    public boolean isReadable() {
        return pos < mTotalLength;
    }

    // 인자만큼 앞으로 위치 이동(skip)
    private void goBackward(int length) {
        pos--;
    }

    // 인자만큼 앞으로 위치 이동(skip)
    protected void goForward(int length) {
        pos++;
    }

    protected void backToRememberedPos() {
        this.pos = remember;
    }

    protected void rememberCurrPos() {
        this.remember = pos;
    }

    public String getWholeText() {
        return this.text;
    }

    public int getRememberedPos() {
        return this.remember;
    }
}
