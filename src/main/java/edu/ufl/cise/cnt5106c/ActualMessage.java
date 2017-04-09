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
    public int message_Length;
    public int message_Type;
    public byte[] message_Payload;

    ActualMessage(int type, byte[] payload){
        /*Constructor that set the message length according to payload size */
        message_Length = (payload == null ? 0 : payload.length) + 1;
        message_Payload = payload;
        message_Type = type;
    }

    public void read (DataInputStream in) throws IOException {
        /*Takes an DataInputStream Interface variable and
         * reads the byte array(payload) till the message length
         */
        if ((message_Payload != null) && (message_Payload.length) > 0) {
            in.readFully(message_Payload, 0, message_Payload.length);
        }
    }

    public void write (DataOutputStream out) throws IOException {
         /*Takes an DataOutputStream Interface variable
         and writes the byte array(payload) till the message length
         */
        out.writeInt(message_Length);
        out.writeInt(message_Type);
        if ((message_Payload != null) && (message_Payload.length > 0)) {
            out.write(message_Payload, 0, message_Payload.length);
        }
    }

    public int getPieceIndex(){
         /* Reads 4 bytes of the message Payload and converts it into and int and returns   */
        return ByteBuffer.wrap(Arrays.copyOfRange(message_Payload, 0, 4)).getInt();
    }

    public byte[] getContent() {
        /*Check if the content of messagepayload is less than 4, returns the next of*/
        int len = message_Payload.length;
        return ((message_Payload == null) || (len <= 4)) ? null : Arrays.copyOfRange(message_Payload, 4, len);
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