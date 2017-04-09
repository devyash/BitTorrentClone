package edu.ufl.cise.cnt5106c;

import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/*
the following class recognizes adjancent peers and checks if the adjacent peers has the files needed.
Interested message is sent by the peer to another peer if the other peer has the file needed by that peer.
*/

public class AdjacentPeers {
    public final int _peerId;
    public final String _peerAddress;
    public final int _peerPort;

    public final boolean _hasFile;
    public AtomicInteger _bytesDownloadedFrom = new AtomicInteger (0);;
    public BitSet _receivedParts;
    public AtomicBoolean _interested  = new AtomicBoolean (false);

    /*
	The djacent peers have their own peer id, peer address and port where they are located.
	Hasfile defines if the adjacent peer has the file needed by the peer
    */
	public AdjacentPeers(int pId, String pAddress, int pPort, boolean hasFile) {
        _peerId = pId;
        _peerAddress = pAddress;
        _peerPort = pPort;
        _hasFile = hasFile;
        _receivedParts = new BitSet();
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof AdjacentPeers) {
            return (((AdjacentPeers) obj)._peerId == _peerId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this._peerId);
        return hash;
    }

    @Override
    public String toString() {
        return new StringBuilder (_peerId)
                .append (" address:").append (_peerAddress)
                .append(" port: ").append(_peerPort).toString();
    }

}
