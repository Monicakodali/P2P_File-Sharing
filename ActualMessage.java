package src;

import java.util.*;

import java.io.*;
import java.nio.*;
import java.math.BigInteger;

public class ActualMessage {
    private int mLength;
    private char mType;
    private byte[] mPayload;

    public ActualMessage() {

    }

    public ActualMessage(char mType) {
        this.mType = mType;
        this.mLength = 1;
        this.mPayload = new byte[0];
    }

    public ActualMessage(char mType, byte[] mPayload) {
        this.mType = mType;
        this.mPayload = mPayload;
        this.mLength = this.mPayload.length + 1;
    }

    public byte[] generateActualMessage() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] bb = ByteBuffer.allocate(4).putInt(this.mLength).array();
            bos.write(bb);
            bos.write((byte) this.mType);
            bos.write(this.mPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    public void readActualMessage(int length, byte[] m) {
        this.mLength = length;
        this.mType = extractMessageType(m, 0);
        this.mPayload = extractPayload(m, 1);
    }

    public int extractIntFromByteArray(byte[] message, int beg) {
        byte[] length = new byte[4];
        int p=0;
        while(p<4)
        {
            length[p] = message[p + beg];
            p++;
        }
        ByteBuffer bb = ByteBuffer.wrap(length);
        return bb.getInt();
    }

    public char extractMessageType(byte[] message, int index) {
        return (char) message[index];
    }

    public byte[] extractPayload(byte[] message, int index) {
        byte[] resp = new byte[this.mLength - 1];
        System.arraycopy(message, index, resp, 0, this.mLength - 1);
        return resp;
    }

    public BitSet getBitFieldMessage() {
        BitSet bs = new BitSet();
        bs = BitSet.valueOf(this.mPayload);
        return bs;
    }

    public int getPieceIndexFromPayload() {
        return extractIntFromByteArray(this.mPayload, 0);
    }

    public byte[] getPieceFromPayload() {
        int size = this.mLength - 5;
        byte[] piece = new byte[size];
        int i=0;
        while(i<size)
        {
            piece[i] = this.mPayload[i + 4];
            i++;
        }

        return piece;
    }

    public char getMessageType() {
        return this.mType;
    }

}
