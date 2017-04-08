package edu.ufl.cise.cnt5106c; /**
 * Created by Jiya on 3/30/17.
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

public class IncomingHandshakeBehaviour extends DataInputStream implements ObjectInput {

    final static int CHOKE = 0;
    final static int UNCHOKE = 1;
    final static int INTERESTED = 2;
    final static int NOTINTERESTED = 3;
    final static int HAVE = 4;
    final static int BITFIELD = 5;
    final static int REQUEST = 6;
    final static int PIECE = 7;

    public IncomingHandshakeBehaviour(InputStream in) {
        super(in);
    }

    private boolean handshakeRequest = false;

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        if(!handshakeRequest){
            HandShakeMessage h = new HandShakeMessage();
            h.read(this);
            handshakeRequest = true;
            return h;
        }
        else{
            final int length = readInt();
            ActualMessage msg = createMessageWithSpecifiedType(length - 1, readInt());
            msg.read(this);
            return msg;
        }
    }

    public ActualMessage createMessageWithSpecifiedType(int pLength, int type) throws ClassNotFoundException {
        switch (type) {
            case CHOKE:
                return new OnlyType(CHOKE);
            case UNCHOKE:
                return new OnlyType(UNCHOKE);
            case INTERESTED:
                return new OnlyType(INTERESTED);
            case NOTINTERESTED:
                return new OnlyType(NOTINTERESTED);
            case HAVE:
                return new Payload(HAVE,new byte[pLength]);
            case BITFIELD:
                return new Payload(BITFIELD,new byte[pLength]);
            case REQUEST:
                return new Payload(REQUEST,new byte[pLength]);
            case PIECE:
                return new Payload(PIECE,new byte[pLength]);
            default:
                throw new ClassNotFoundException ("message type not handled");
        }
    }
}
