package edu.ufl.cise.cnt5106c;

import java.io.*;
import java.nio.ByteBuffer;

/*
Handshake message is established between peer before file sharing starts.
Handshake message is 32 bits with first 18 bits as "P2PFILESHARINGPROJ"
and next 10 bits as zero and next 4 bits describing te peer id
*/
public class HandShakeMessage{

    public static final String header_Name = "P2PFILESHARINGPROJ";
    public static final int ZERO_BIT_SIZE = 10;
    public static final int PEER_ID_BYTE = 4;

    private final byte[] zeroBits = new byte[ZERO_BIT_SIZE];
    byte[] peerId = new byte[PEER_ID_BYTE];
    byte[] messageHeader = header_Name.getBytes();

    public HandShakeMessage(){

    }

    /*public HandShakeMessage(byte[] pId) {
        if (pId.length > PEER_ID_BYTE)
            throw new IndexOutOfBoundsException("Max length permitted is " + PEER_ID_BYTE);
        else
            peerId = pId;           //check for failure
    }*/

    public HandShakeMessage(int pId)
    {
        peerId = (ByteBuffer.allocate(4).putInt(pId).array());
    }

    public void write(DataOutputStream outputStream) throws IOException {
        outputStream.write(messageHeader, 0, messageHeader.length);
        outputStream.write(zeroBits, 0, zeroBits.length);
        outputStream.write(peerId, 0, peerId.length);
    }

    public void read(DataInputStream inputStream) throws IOException {
        if (inputStream.read(messageHeader, 0, messageHeader.length) < messageHeader.length) {;
        }
        if (inputStream.read(zeroBits, 0, zeroBits.length) < zeroBits.length) {;
        }
        if (inputStream.read(peerId, 0, peerId.length) < peerId.length) {;
        }
    }

}