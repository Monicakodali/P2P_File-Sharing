package src;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage {
    private String headerOfHandshake;
    private String peerID;
    
    public HandshakeMessage(String pID) {
        this.headerOfHandshake = "P2PFILESHARINGPROJ";
        this.peerID = pID;
    }

    public String getPeerID(){
        return this.peerID;
    }

    public byte[] generateHandShakeMessage() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(this.headerOfHandshake.getBytes(StandardCharsets.UTF_8));
            bos.write(new byte[10]);
            bos.write(this.peerID.getBytes(StandardCharsets.UTF_8));
        } 
        catch(Exception e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    public void readHandShakeMessage(byte[] m){
        String msg = new String(m,StandardCharsets.UTF_8);
        this.peerID = msg.substring(28,32);
    }
}
