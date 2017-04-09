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
    private final int remote_Id;
    private final FileManager file_Manager;
    private final PeerManager neighbor_Manager;
    private final EventLogger eventLogger;

    MessageHandler(int remoteNeighborId, FileManager file_Manager, PeerManager neighbor_Manager, EventLogger eventLogger) {
        isChokedByRemoteNeighbor = true;
        this.file_Manager = file_Manager;
        this.neighbor_Manager = neighbor_Manager;
        this.remote_Id = remoteNeighborId;
        this.eventLogger = eventLogger;
    }

    private ActualMessage requestPiece() {
        if (isChokedByRemoteNeighbor)
            LogHelper.getLogger().debug("No parts can be requested to " + remote_Id);
        else {
            BitSet b = new BitSet();
            AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
            if (peer != null) {
                b = (BitSet) peer._receivedParts.clone();
            }
            int pieceIndex = file_Manager.getPartToRequest(b);
            if (pieceIndex >= 0) {
                LogHelper.getLogger().debug("Requesting part " + pieceIndex + " to " + remote_Id);
                return new ActualMessage(REQUEST, ByteBuffer.allocate(4).putInt(pieceIndex).array());
            }
        }
        return null;
    }

    public ActualMessage process(HandShakeMessage handshake) {
        BitSet bitset = file_Manager.getReceivedParts();
        return !bitset.isEmpty() ? new ActualMessage(BITFIELD, bitset.toByteArray()) : null;
    }

    public ActualMessage process(ActualMessage message) {
        switch (message.type) {
            case CHOKE:
            {
                isChokedByRemoteNeighbor = true;
                eventLogger.choke(remote_Id);
                return null;
            }
            case UNCHOKE:
            {
                isChokedByRemoteNeighbor = false;
                eventLogger.unchoke(remote_Id);
                return requestPiece();
            }
            case INTERESTED:
            {
                eventLogger.interested(remote_Id);
                AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
                if (peer != null)
                    peer._interested.set(true);
                return null;
            }
            case NOTINTERESTED:
            {
                eventLogger.notInterested(remote_Id);
                AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
                if (peer != null)
                    peer._interested.set(false);
                return null;
            }
            case HAVE:
            {
                ActualMessage have =  message;
                final int pieceId = have.getPieceIndex();
                eventLogger.have(remote_Id, pieceId);
                AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
                if (peer != null) {
                    peer._receivedParts.set(pieceId);
                }
                neighbor_Manager.neighborsCompletedDownload();
                return file_Manager.getReceivedParts().get(pieceId) == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case BITFIELD:
            {
                ActualMessage bitfield = (ActualMessage) message;
                BitSet bitset = BitSet.valueOf(bitfield.payload);
                AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
                if (peer != null) {
                    peer._receivedParts = bitset;
                }
                neighbor_Manager.neighborsCompletedDownload();
                bitset.andNot(file_Manager.getReceivedParts());
                return bitset.isEmpty() == true ? new ActualMessage(NOTINTERESTED, null) : new ActualMessage(INTERESTED, null);
            }
            case REQUEST:
            {
                ActualMessage request = (ActualMessage) message;
                if (neighbor_Manager.canUploadToPeer(remote_Id)) {
                    byte[] piece = file_Manager.getPiece(request.getPieceIndex());
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
                file_Manager.addPart(piece.getPieceIndex(), piece.getContent());
                AdjacentPeers peer = neighbor_Manager.searchPeer(remote_Id);
                if (peer != null) {
                    peer._bytesDownloadedFrom.addAndGet(piece.getContent().length);
                }
                eventLogger.pieceDownloadedMessage(remote_Id, piece.getPieceIndex(), file_Manager.getNumberOfReceivedParts());
                return requestPiece();
            }
        }
        return null;
    }
}
