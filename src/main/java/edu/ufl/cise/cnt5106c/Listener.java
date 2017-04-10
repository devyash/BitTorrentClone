package edu.ufl.cise.cnt5106c;
import java.util.Collection;

/*
*This interface  is used to register various events in th code.
 *  */

public interface Listener {
    void fileCompleted();
    void pieceArrived (int partIdx);
    void neighborsCompletedDownload();
    void chockedPeers (Collection<Integer> chokedPeersIds);
    void unchockedPeers (Collection<Integer> unchokedPeersIds);
}
