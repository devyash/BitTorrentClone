package edu.ufl.cise.cnt5106c;

import java.util.Collection;

/**
 * Created by Jiya on 4/8/17.
 */
public interface Listener {
    public void fileCompleted();
    public void pieceArrived (int partIdx);
    public void neighborsCompletedDownload();
    public void chockedPeers (Collection<Integer> event_ChokedPeersIds);
    public void unchockedPeers (Collection<Integer> event_UnchokedPeersIds);
}
