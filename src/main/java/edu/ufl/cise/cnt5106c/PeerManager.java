package edu.ufl.cise.cnt5106c;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager implements Runnable {
    private final int _bitmapsize;
    private final EventLogger _eventLogger;
    private final List < AdjacentPeers > _peers = new ArrayList < > ();
    private final Collection < AdjacentPeers > _preferredPeers = new HashSet < > ();
    private final OptimisticUnevent_Choker _optUnevent_Choker;
    private final Collection < Listener > _listeners = new LinkedList < > ();
    public peerProcess pProcess;
    public final AtomicBoolean _randomlySelectPreferred = new AtomicBoolean(false);

    PeerManager(int peerId, Collection < AdjacentPeers > peers, int bitmapsize, peerProcess conf) {
        _peers.addAll(peers);
        pProcess = conf;
        _optUnevent_Choker = new OptimisticUnevent_Choker(pProcess.NumberOfPreferredNeighbors * 1000);
        _bitmapsize = bitmapsize;
        _eventLogger = new EventLogger(peerId, LogHelper.getLogger());
    }

    //cant replace this
    synchronized boolean canUploadToPeer(int peerId) {
        AdjacentPeers peerInfo = new AdjacentPeers(peerId, "127.0.0.1", 0, false);
        return (_preferredPeers.contains(peerInfo) ||
                _optUnevent_Choker._optmisticallyUnevent_ChokedPeers.contains(peerInfo));
    }

    //cant replace this
    synchronized void fileCompleted() {
        _randomlySelectPreferred.set(true);
    }

    //TODO: TRY REPLACING IT
    synchronized public AdjacentPeers searchPeer(int peerId) {
        for (AdjacentPeers peer: _peers) {
            if (peer.id == peerId) {
                return peer;
            }
        }
        LogHelper.getLogger().warning("Peer " + peerId + " not found");
        return null;
    }

    synchronized public void neighborsCompletedDownload() {
        for (AdjacentPeers peer: _peers) {
            if (peer.received_Parts.cardinality() < _bitmapsize) {
                LogHelper.getLogger().debug("Peer " + peer.id + " has not completed yet");
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
        _optUnevent_Choker.start();
        while (true) {
            try {
                Thread.sleep(pProcess.UnchokingInterval * 1000);
            } catch (InterruptedException ex) {}
            ArrayList < AdjacentPeers > event_InterestedPeers = new ArrayList < > ();
            for (AdjacentPeers peer: _peers) {
                if (peer.event_Interested.get()) {
                    event_InterestedPeers.add(peer);
                }
            }
            if (_randomlySelectPreferred.get()) {
                LogHelper.getLogger().debug("selecting preferred peers randomly");
                Collections.shuffle(event_InterestedPeers);
            } else {
                Collections.sort(event_InterestedPeers, new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        AdjacentPeers ri1 = (AdjacentPeers)(o1);
                        AdjacentPeers ri2 = (AdjacentPeers)(o2);
                        return (ri2.bytes_Downloaded_From.get() - ri1.bytes_Downloaded_From.get());
                    }
                });
            }
            //TODO change names
            Collection < AdjacentPeers > optUnchokablePeers = null;
            HashSet < Integer > event_ChokedPeersIDs = new HashSet < > ();
            HashSet < Integer > preferredNeighborsIDs = new HashSet < > ();
            HashMap < Integer, Long > downloadedBytes = new HashMap < > ();

            synchronized(this) {
                for (AdjacentPeers peer: _peers) {
                    downloadedBytes.put(peer.id, peer.bytes_Downloaded_From.longValue());
                    peer.bytes_Downloaded_From.set(0);
                }
                _preferredPeers.clear();
                _preferredPeers.addAll(event_InterestedPeers.subList(0, Math.min(pProcess.NumberOfPreferredNeighbors, event_InterestedPeers.size())));
                if (_preferredPeers.size() > 0) {
                    _eventLogger.event_Preferred_Neighbors(LogHelper.getNeighborsAsString(_preferredPeers));
                }
                List < AdjacentPeers > event_ChokedPeers = new LinkedList < > (_peers);
                event_ChokedPeers.removeAll(_preferredPeers);
                Set < Integer > ids = new HashSet < > ();
                for (AdjacentPeers peer: _preferredPeers) {
                    ids.add(peer.id);
                }
                event_ChokedPeersIDs.addAll(ids);
                if (pProcess.NumberOfPreferredNeighbors >= event_InterestedPeers.size()) {
                    optUnchokablePeers = new ArrayList < > ();
                } else {
                    optUnchokablePeers = event_InterestedPeers.subList(pProcess.NumberOfPreferredNeighbors, event_InterestedPeers.size());
                }
                preferredNeighborsIDs.addAll(ids);
            }
            for (Entry < Integer, Long > entry: downloadedBytes.entrySet()) {
                String PREFERRED = preferredNeighborsIDs.contains(entry.getKey()) ? " *" : "";
                LogHelper.getLogger().debug("BYTES DOWNLOADED FROM  PEER " + entry.getKey() + ": " + entry.getValue() + " (INTERESTED PEERS: " + event_InterestedPeers.size() + ": " + LogHelper.getNeighborsAsString(event_InterestedPeers) + ")\t" + PREFERRED);
            }
            for (Listener listener: _listeners) {
                listener.chockedPeers(event_ChokedPeersIDs);
                listener.unchockedPeers(preferredNeighborsIDs);
            }
            if (optUnchokablePeers != null) {
                _optUnevent_Choker._event_ChokedNeighbors.clear();
                _optUnevent_Choker._event_ChokedNeighbors.addAll(optUnchokablePeers);
            }
        }
    }

    class OptimisticUnevent_Choker extends Thread {
        public int _optimisticUnchokingInterval;
        private final List < AdjacentPeers > _event_ChokedNeighbors = new ArrayList < > ();
        final Collection < AdjacentPeers > _optmisticallyUnevent_ChokedPeers = Collections.newSetFromMap(new ConcurrentHashMap < AdjacentPeers, Boolean > ());

        OptimisticUnevent_Choker(int interval) {
            super("OptimisticUnevent_Choker");
            _optimisticUnchokingInterval = interval;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(_optimisticUnchokingInterval);
                } catch (InterruptedException ex) {}
                synchronized(this) {
                    //TODO: CHANGE COMMENT: Randomly shuffle the remaining neighbors, and select some to optimistically event_Unchoke
                    if (!_event_ChokedNeighbors.isEmpty()) {
                        Collections.shuffle(_event_ChokedNeighbors);
                        _optmisticallyUnevent_ChokedPeers.clear();
                        _optmisticallyUnevent_ChokedPeers.addAll(_event_ChokedNeighbors.subList(0,
                                Math.min(1, _event_ChokedNeighbors.size())));
                    }
                }
                if (_event_ChokedNeighbors.size() > 0) {
                    LogHelper.getLogger().debug("STATE: OPT UNCHOKED(1):" + LogHelper.getNeighborsAsString(_optmisticallyUnevent_ChokedPeers));
                    _eventLogger.event_Optimistically_Unchoked_Neighbor(LogHelper.getNeighborsAsString(_optmisticallyUnevent_ChokedPeers));
                }
                //already simplified
                for (Listener listener: _listeners) {
                    Set < Integer > ids = new HashSet < > ();
                    for (AdjacentPeers peer: _optmisticallyUnevent_ChokedPeers) {
                        ids.add(peer.id);
                    }
                    listener.unchockedPeers(ids);
                }
            }
        }
    }
}
