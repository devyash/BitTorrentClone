package edu.ufl.cise.cnt5106c;

import java.io.*;
import java.nio.*;
import java.util.*;

/*
* This is the Actual Message class that has 3 class variables
* @param message length @param message type @param message payload
* This Class handles byte array to (int)piece conversion of size 4 and back
*
* */

public class ActualMessage{
    public byte[] payload;
    public int length;
    public int type;


    ActualMessage(int type, byte[] payload){
        /*Constructor that set the message length according to payload size */
        this.length = (payload == null ? 0 : payload.length) + 1;
        this.payload = payload;
        this.type = type;
    }

    public void read (DataInputStream in) throws IOException {
        /*Takes an DataInputStream Interface variable and
         * reads the byte array(payload) till the message length
         */
        if ((payload != null) && (payload.length) > 0) {
            in.readFully(payload, 0, payload.length);
        }
    }

    public void write (DataOutputStream out) throws IOException {
         /*Takes an DataOutputStream Interface variable
         and writes the byte array(payload) till the message length
         */
        out.writeInt(length);
        out.writeInt(type);
        if ((payload != null) && (payload.length > 0)) {
            out.write(payload, 0, payload.length);
        }
    }

    public int getPieceIndex(){
         /* Reads 4 bytes of the message Payload and converts it into and int and returns   */
        return ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).getInt();
    }

    public byte[] getContent() {
        /*Check if the content of message payload is less than 4, returns the next of*/
        int len = payload.length;
        return ((payload == null) || (len <= 4)) ? null : Arrays.copyOfRange(payload, 4, len);
    }

    public static byte[] merge (int index, byte[] temp) {
        /*
        * Merges the piece with index passed and the byte array*/
        byte[] piece;
        if(temp == null)
            piece = new byte[4];
        else
            piece = new byte[4 + temp.length];
        //converts the piece index into a byte array
        System.arraycopy(ByteBuffer.allocate(4).putInt(index).array(), 0, piece, 0, 4);
        System.arraycopy(temp, 0, piece, 4, temp.length); return piece;
    }


}