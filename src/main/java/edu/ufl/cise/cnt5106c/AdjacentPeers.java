package edu.ufl.cise.cnt5106c;

import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Jiya on 4/8/17.
 */
public class AdjacentPeers {
    public final int _peerId;
    public final String _peerAddress;
    public final int _peerPort;

    //check its usage and see if we can chuck it
    public final boolean _hasFile;
    public AtomicInteger _bytesDownloadedFrom = new AtomicInteger (0);;
    public BitSet _receivedParts;
    public AtomicBoolean _interested  = new AtomicBoolean (false);

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
