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
            BitSet b = new BitSet();
            AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
            if (peer != null) {
                b = (BitSet) peer._receivedParts.clone();
            }
            int pieceIndex = fileMgr.getPartToRequest(b);
            if (pieceIndex >= 0) {
                LogHelper.getLogger().debug("Requesting part " + pieceIndex + " to " + remoteNeighborID);
                return new ActualMessage(REQUEST, ByteBuffer.allocate(4).putInt(pieceIndex).array());
            }
        }
        return null;
    }

    public ActualMessage process(HandShakeMessage handshake) {
        BitSet bitset = fileMgr.getReceivedParts();
        return !bitset.isEmpty() ? new ActualMessage(BITFIELD, bitset.toByteArray()) : null;
    }

    public ActualMessage process(ActualMessage message) {
        switch (message.type) {
            case CHOKE:
            {
                isChokedByRemoteNeighbor = true;
                eventLogger.choke(remoteNeighborID);
                return null;
            }
            case UNCHOKE:
            {
                isChokedByRemoteNeighbor = false;
                eventLogger.unchoke(remoteNeighborID);
                return requestPiece();
            }
            case INTERESTED:
            {
                eventLogger.interested(remoteNeighborID);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null)
                    peer._interested.set(true);
                return null;
            }
            case NOTINTERESTED:
            {
                eventLogger.notInterested(remoteNeighborID);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null)
                    peer._interested.set(false);
                return null;
            }
            case HAVE:
            {
                ActualMessage have = (ActualMessage) message;
                final int pieceId = have.getPieceIndex();
                eventLogger.have(remoteNeighborID, pieceId);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer._receivedParts.set(pieceId);
                }
                neighborMgr.neighborsCompletedDownload();
                return fileMgr.getReceivedParts().get(pieceId) == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case BITFIELD:
            {
                ActualMessage bitfield = (ActualMessage) message;
                BitSet bitset = BitSet.valueOf(bitfield.payload);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer._receivedParts = bitset;
                }
                neighborMgr.neighborsCompletedDownload();
                bitset.andNot(fileMgr.getReceivedParts());
                return bitset.isEmpty() == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case REQUEST:
            {
                ActualMessage request = (ActualMessage) message;
                if (neighborMgr.canUploadToPeer(remoteNeighborID)) {
                    byte[] piece = fileMgr.getPiece(request.getPieceIndex());
                    if (piece != null) {
                        byte[] mergedPiece = request.merge(request.getPieceIndex(), piece);
                        return new ActualMessage(PIECE, mergedPiece);
                    }
                }
                return null;
            }
            case PIECE:
            {
                ActualMessage piece = (ActualMessage) message;
                fileMgr.addPart(piece.getPieceIndex(), piece.getContent());
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer._bytesDownloadedFrom.addAndGet(piece.getContent().length);
                }
                eventLogger.pieceDownloadedMessage(remoteNeighborID, piece.getPieceIndex(), fileMgr.getNumberOfReceivedParts());
                return requestPiece();
            }
        }
        return null;
    }
}
