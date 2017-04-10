package edu.ufl.cise.cnt5106c;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerOrganizer implements Runnable {
    private final int _bitmapsize;
    private final EventLogger _eventLogger;
    private final List < AdjacentPeers > _peers = new ArrayList < > ();
    private final Collection < AdjacentPeers > _preferredPeers = new HashSet < > ();
    private final OptimisticUnchoker _optUnchoker;
    private final Collection < Listener > _listeners = new LinkedList < > ();
    public peerProcess pProcess;
    public final AtomicBoolean _randomlySelectPreferred = new AtomicBoolean(false);

    PeerOrganizer(int peerId, Collection < AdjacentPeers > peers, int bitmapsize, peerProcess conf) {
        _peers.addAll(peers);
        pProcess = conf;
        _optUnchoker = new OptimisticUnchoker(pProcess.NumberOfPreferredNeighbors * 1000);
        _bitmapsize = bitmapsize;
        _eventLogger = new EventLogger(peerId, LogHelper.getLogger());
    }

    //cant replace this
    synchronized boolean canUploadToPeer(int peerId) {
        AdjacentPeers peerInfo = new AdjacentPeers(peerId, "127.0.0.1", 0, false);
        return (_preferredPeers.contains(peerInfo) ||
                _optUnchoker._optmisticallyUnchokedPeers.contains(peerInfo));
    }

    //cant replace this
    synchronized void fileCompleted() {
        _randomlySelectPreferred.set(true);
    }

    //TODO: TRY REPLACING IT
    synchronized public AdjacentPeers searchPeer(int peerId) {
        for (AdjacentPeers peer: _peers) {
            if (peer.peer_Id == peerId) {
                return peer;
            }
        }
        LogHelper.getLogger().warning("Peer " + peerId + " not found");
        return null;
    }

    synchronized public void neighborsCompletedDownload() {
        for (AdjacentPeers peer: _peers) {
            if (peer.received_Parts.cardinality() < _bitmapsize) {
                LogHelper.getLogger().debug("Peer " + peer.peer_Id + " has not completed yet");
                return;
            }
        }
        for (Listener listener: _listeners) {
            listener.neighborsCompletedDownload();
        }
    }

    public synchronized void registerListener(Listener listener) {
        _listeners.add(listener);
    }

    @Override
    public void run() {
        _optUnchoker.start();
        while (true) {
            try {
                Thread.sleep(pProcess.UnchokingInterval * 1000);
            } catch (InterruptedException ex) {}
            ArrayList < AdjacentPeers > interestedPeers = new ArrayList < > ();
            for (AdjacentPeers peer: _peers) {
                if (peer.interested.get()) {
                    interestedPeers.add(peer);
                }
            }
            if (_randomlySelectPreferred.get()) {
                LogHelper.getLogger().debug("selecting preferred peers randomly");
                Collections.shuffle(interestedPeers);
            } else {
                Collections.sort(interestedPeers, new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        AdjacentPeers ri1 = (AdjacentPeers)(o1);
                        AdjacentPeers ri2 = (AdjacentPeers)(o2);
                        return (ri2.download_Bytes.get() - ri1.download_Bytes.get());
                    }
                });
            }
            //TODO change names
            Collection < AdjacentPeers > optUnchokablePeers = null;
            HashSet < Integer > chokedPeersIDs = new HashSet < > ();
            HashSet < Integer > preferredNeighborsIDs = new HashSet < > ();
            HashMap < Integer, Long > downloadedBytes = new HashMap < > ();

            synchronized(this) {
                for (AdjacentPeers peer: _peers) {
                    downloadedBytes.put(peer.peer_Id, peer.download_Bytes.longValue());
                    peer.download_Bytes.set(0);
                }
                _preferredPeers.clear();
                _preferredPeers.addAll(interestedPeers.subList(0, Math.min(pProcess.NumberOfPreferredNeighbors, interestedPeers.size())));
                if (_preferredPeers.size() > 0) {
                    _eventLogger.updatePreferredNeighbor(LogHelper.getNeighborsAsString(_preferredPeers));
                }
                List < AdjacentPeers > chokedPeers = new LinkedList < > (_peers);
                chokedPeers.removeAll(_preferredPeers);
                Set < Integer > ids = new HashSet < > ();
                for (AdjacentPeers peer: _preferredPeers) {
                    ids.add(peer.peer_Id);
                }
                chokedPeersIDs.addAll(ids);
                if (pProcess.NumberOfPreferredNeighbors >= interestedPeers.size()) {
                    optUnchokablePeers = new ArrayList < > ();
                } else {
                    optUnchokablePeers = interestedPeers.subList(pProcess.NumberOfPreferredNeighbors, interestedPeers.size());
                }
                preferredNeighborsIDs.addAll(ids);
            }
            for (Entry < Integer, Long > entry: downloadedBytes.entrySet()) {
                String PREFERRED = preferredNeighborsIDs.contains(entry.getKey()) ? " *" : "";
                LogHelper.getLogger().debug("BYTES DOWNLOADED FROM  PEER " + entry.getKey() + ": " + entry.getValue() + " (INTERESTED PEERS: " + interestedPeers.size() + ": " + LogHelper.getNeighborsAsString(interestedPeers) + ")\t" + PREFERRED);
            }
            for (Listener listener: _listeners) {
                listener.chockedPeers(chokedPeersIDs);
                listener.unchockedPeers(preferredNeighborsIDs);
            }
            if (optUnchokablePeers != null) {
                _optUnchoker._chokedNeighbors.clear();
                _optUnchoker._chokedNeighbors.addAll(optUnchokablePeers);
            }
        }
    }

    class OptimisticUnchoker extends Thread {
        public int _optimisticUnchokingInterval;
        private final List < AdjacentPeers > _chokedNeighbors = new ArrayList < > ();
        final Collection < AdjacentPeers > _optmisticallyUnchokedPeers = Collections.newSetFromMap(new ConcurrentHashMap < AdjacentPeers, Boolean > ());

        OptimisticUnchoker(int interval) {
            super("OptimisticUnchoker");
            _optimisticUnchokingInterval = interval;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(_optimisticUnchokingInterval);
                } catch (InterruptedException ex) {}
                synchronized(this) {
                    //TODO: CHANGE COMMENT: Randomly shuffle the remaining neighbors, and select some to optimistically unchoke
                    if (!_chokedNeighbors.isEmpty()) {
                        Collections.shuffle(_chokedNeighbors);
                        _optmisticallyUnchokedPeers.clear();
                        _optmisticallyUnchokedPeers.addAll(_chokedNeighbors.subList(0,
                                Math.min(1, _chokedNeighbors.size())));
                    }
                }
                if (_chokedNeighbors.size() > 0) {
                    LogHelper.getLogger().debug("STATE: OPT UNCHOKED(1):" + LogHelper.getNeighborsAsString(_optmisticallyUnchokedPeers));
                    _eventLogger.updateOptimisticallyUnchokedNeighbor(LogHelper.getNeighborsAsString(_optmisticallyUnchokedPeers));
                }
                //already simplified
                for (Listener listener: _listeners) {
                    Set < Integer > ids = new HashSet < > ();
                    for (AdjacentPeers peer: _optmisticallyUnchokedPeers) {
                        ids.add(peer.peer_Id);
                    }
                    listener.unchockedPeers(ids);
                }
            }
        }
    }
}
