
package edu.ufl.cise.cnt5106c;
import java.nio.ByteBuffer;
import java.util.BitSet;

/*
 * This class is used to organize various kinds of message.
 * Based on the message type the class performs and organizes/handles various task
 */

public class MessageOrganizer {
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
    private final FileOrganizer fileMgr;
    private final PeerOrganizer neighborMgr;
    private final EventLogger eventLogger;

    /*Constructor to initialize the organizer class with the different organizer class*/
    MessageOrganizer(int remoteNeighborId, FileOrganizer fileMgr, PeerOrganizer neighborMgr, EventLogger eventLogger) {
        this.isChokedByRemoteNeighbor = true;
        this.fileMgr = fileMgr;
        this.neighborMgr = neighborMgr;
        this.remoteNeighborID = remoteNeighborId;
        this.eventLogger = eventLogger;
    }

/*This method is used toe request a piece from a neighbour.
 It returns the message object */
    private ActualMessage requestPiece() {
        if (isChokedByRemoteNeighbor)
            LogHelper.getLogger().debug("Since the peer: " + remoteNeighborID+", No parts are allowed to be queried.");
        else {
            BitSet b = new BitSet();
            AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
            if (peer != null) {
                b = (BitSet) peer.received_Parts.clone();
            }
            int pieceIndex = fileMgr.getPartToRequest(b);
            if (pieceIndex >= 0) {
                LogHelper.getLogger().debug("Requesting part " + pieceIndex + " to " + remoteNeighborID);
                return new ActualMessage(REQUEST, ByteBuffer.allocate(4).putInt(pieceIndex).array());
            }
        }
        return null;
    }
    /*This method is used to process the handshake message.
    Since there is nothing to be done with the handshake message.
    The handshake message here passed is used only to differentiate the function(overloading)*/
    //TODO: Unused Handshake Parameter
    public ActualMessage process(HandShakeMessage handshake) {
        BitSet bitset = fileMgr.getReceivedParts();
        return !bitset.isEmpty() ? new ActualMessage(BITFIELD, bitset.toByteArray()) : null;
    }
    /*This method is used to process the actual message.
    * Based on the various message types different parts
    * of logic is processed as described in project description*/
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
                    peer.interested.set(true);
                return null;
            }
            case NOTINTERESTED:
            {
                eventLogger.notInterested(remoteNeighborID);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null)
                    peer.interested.set(false);
                return null;
            }
            case HAVE:
            {
                ActualMessage have = message;
                final int pieceId = have.getPieceIndex();
                eventLogger.have(remoteNeighborID, pieceId);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer.received_Parts.set(pieceId);
                }
                neighborMgr.neighborsCompletedDownload();
                return fileMgr.getReceivedParts().get(pieceId) == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case BITFIELD:
            {
                ActualMessage bitfield = message;
                BitSet bitset = BitSet.valueOf(bitfield.payload);
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer.received_Parts = bitset;
                }
                neighborMgr.neighborsCompletedDownload();
                bitset.andNot(fileMgr.getReceivedParts());
                return bitset.isEmpty() == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case REQUEST:
            {
                ActualMessage request =  message;
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
                ActualMessage piece =  message;
                fileMgr.addPart(piece.getPieceIndex(), piece.getContent());
                AdjacentPeers peer = neighborMgr.searchPeer(remoteNeighborID);
                if (peer != null) {
                    peer.download_Bytes.addAndGet(piece.getContent().length);
                }
                eventLogger.pieceDownloadedMessage(remoteNeighborID, piece.getPieceIndex(), fileMgr.getNumberOfReceivedParts());
                return requestPiece();
            }
        }
        return null;
    }
}
