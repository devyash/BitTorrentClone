package edu.ufl.cise.cnt5106c; /**
 * Created by Jiya on 2/16/17.
 */

//import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ActualMessage{
    public int msgLength;
    public int msgType;
    public byte[] msgPayload;

    ActualMessage(){
        msgLength = 0;
        msgType = -1;
        msgPayload = null;
    }

    ActualMessage(int type, byte[] payload){
        msgLength = (payload == null ? 0 : payload.length)
                + 1; // for the _type
        //if(payload.length > 0){
            msgPayload = payload;
          //  msgLength = payload.length;
        //}
        msgType = type;
    }

    public void read (DataInputStream in) throws IOException {
        if ((msgPayload != null) && (msgPayload.length) > 0) {
            in.readFully(msgPayload, 0, msgPayload.length);
        }
    }

    public void write (DataOutputStream out) throws IOException {
        out.writeInt(msgLength);
        out.writeInt(msgType);
        if ((msgPayload != null) && (msgPayload.length > 0)) {
            out.write(msgPayload, 0, msgPayload.length);
        }
    }

    public int getPieceIndex(){
        return ByteBuffer.wrap(Arrays.copyOfRange(msgPayload, 0, 4)).getInt();
    }

    public byte[] getContent() {
        int len = msgPayload.length;
        return ((msgPayload == null) || (len <= 4)) ? null : Arrays.copyOfRange(msgPayload, 4, len);
    }

    public static byte[] merge (int index, byte[] temp) {
        byte[] piece = null;
        if(temp == null)
            piece = new byte[4];
        else
            piece = new byte[4 + temp.length];

        System.arraycopy(ByteBuffer.allocate(4).putInt(index).array(), 0, piece, 0, 4);

        System.arraycopy(temp, 0, piece, 4, temp.length);

        return piece;
    }
//
//    @NotNull
    public static byte[] getIndexBytes (int pieceIndex) {
        return ByteBuffer.allocate(4).putInt(pieceIndex).array();
    }

}