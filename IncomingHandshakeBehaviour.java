

import java.io.*;

/*
After handshaking, actual message contains 1-byte message type defined below.
There are 8 types of messages which are defined from value 0 to 7.
*/
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

    //Message with its specified type is created
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
