package edu.ufl.cise.cnt5106c;
/**
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

    public IncomingHandshakeBehaviour(InputStream in ) {
        super( in );
    }

    private boolean handshakeRequest = false;

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        if (!handshakeRequest) {
            HandShakeMessage h = new HandShakeMessage();
            h.read(this);
            handshakeRequest = true;
            return h;
        } else {
            final int length = readInt();
            ActualMessage msg = createMessageWithSpecifiedType(length - 1, readInt());
            msg.read(this);
            return msg;
        }
    }

    public ActualMessage createMessageWithSpecifiedType(int pLength, int type) throws ClassNotFoundException {
        switch (type) {
            case CHOKE:
                return new ActualMessage(CHOKE, null);
            case UNCHOKE:
                return new ActualMessage(UNCHOKE, null);
            case INTERESTED:
                return new ActualMessage(INTERESTED, null);
            case NOTINTERESTED:
                return new ActualMessage(NOTINTERESTED, null);
            case HAVE:
                return new ActualMessage(HAVE, new byte[pLength]);
            case BITFIELD:
                return new ActualMessage(BITFIELD, new byte[pLength]);
            case REQUEST:
                return new ActualMessage(REQUEST, new byte[pLength]);
            case PIECE:
                return new ActualMessage(PIECE, new byte[pLength]);
            default:
                throw new ClassNotFoundException("message type not handled");
        }
    }
}
