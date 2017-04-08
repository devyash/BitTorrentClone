/**
 * Created by Jiya on 3/30/17.
 */
package edu.ufl.cise.cnt5106c;
import java.nio.ByteBuffer;
import java.util.BitSet;



public class MessageHandler {

    final static int CHOKE = 0;
    final static int UNCHOKE = 1;
    final static int INTERESTED = 2;
    final static int NOTINTERESTED = 3;
    final static int HAVE = 4;
    final static int BITFIELD = 5;
    final static int REQUEST = 6;
    final static int PIECE = 7;

    private boolean isChokedByRemoteNeighbor;
    private final int remoteNeighborID;
    private final FileManager fileMgr;
    private final PeerManager neighborMgr;
    private final EventLogger eventLogger;

    MessageHandler(int remoteNeighborId, FileManager fileMgr, PeerManager neighborMgr, EventLogger eventLogger) {
        isChokedByRemoteNeighbor = true;
        this.fileMgr = fileMgr;
        this.neighborMgr = neighborMgr;
        this.remoteNeighborID = remoteNeighborId;
        this.eventLogger = eventLogger;
    }

    private ActualMessage requestPiece() {
        if (isChokedByRemoteNeighbor)
            LogHelper.getLogger().debug("No parts can be requested to " + remoteNeighborID);
        else {
            int pieceIndex = fileMgr.getPartToRequest(neighborMgr.getReceivedParts(remoteNeighborID)) ;
            if (pieceIndex >= 0) {
                LogHelper.getLogger().debug("Requesting part " + pieceIndex + " to " + remoteNeighborID);
                return new Payload(REQUEST,ByteBuffer.allocate(4).putInt(pieceIndex).array());
            }
        }
        return null;
    }

    public ActualMessage process(HandShakeMessage handshake) {
        BitSet bitset = fileMgr.getReceivedParts();
        return !bitset.isEmpty() ? new Payload(BITFIELD,bitset.toByteArray()) : null;
    }

    public ActualMessage process(ActualMessage message) {
        switch (message.msgType) {
            case CHOKE: {
                isChokedByRemoteNeighbor = true;
                eventLogger.choke(remoteNeighborID);
                return null;
            }
            case UNCHOKE: {
                isChokedByRemoteNeighbor = false;
                eventLogger.unchoke(remoteNeighborID);
                return requestPiece();
            }
            case INTERESTED: {
                eventLogger.interested(remoteNeighborID);
                neighborMgr.addInterestPeer(remoteNeighborID);
                return null;
            }
            case NOTINTERESTED: {
                eventLogger.notInterested(remoteNeighborID);
                neighborMgr.removeInterestPeer(remoteNeighborID);
                return null;
            }
            case HAVE: {
                Payload have = (Payload) message;
                final int pieceId = have.getPieceIndex();
                eventLogger.have(remoteNeighborID, pieceId);
                neighborMgr.haveArrived(remoteNeighborID, pieceId);
                return fileMgr.getReceivedParts().get(pieceId) == true ? new OnlyType(NOTINTERESTED) : new OnlyType(INTERESTED);
            }
            case BITFIELD: {
                Payload bitfield = (Payload) message;
                BitSet bitset =  BitSet.valueOf(bitfield.msgPayload);
                neighborMgr.bitfieldArrived(remoteNeighborID, bitset);
                bitset.andNot(fileMgr.getReceivedParts());
                return bitset.isEmpty() == true ? new OnlyType(NOTINTERESTED) : new OnlyType(INTERESTED);
            }
            case REQUEST: {
                Payload request = (Payload)message;
                if (neighborMgr.canUploadToPeer(remoteNeighborID)) {
                    byte[] piece = fileMgr.getPiece(request.getPieceIndex());
                    if (piece != null) {
                        byte[] mergedPiece = request.merge(request.getPieceIndex(), piece);
                        return new Payload(PIECE,mergedPiece);
                    }
                }
                return null;
            }
            case PIECE: {
                Payload piece = (Payload) message;
                fileMgr.addPart(piece.getPieceIndex(), piece.getContent());
                neighborMgr.receivedPart(remoteNeighborID, piece.getContent().length);
                eventLogger.pieceDownloadedMessage(remoteNeighborID, piece.getPieceIndex(), fileMgr.getNumberOfReceivedParts());
                return requestPiece();
            }
        }
        return null;
    }
}