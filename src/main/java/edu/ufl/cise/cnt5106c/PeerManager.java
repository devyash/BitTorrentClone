package edu.ufl.cise.cnt5106c;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager implements Runnable {

    class OptimisticUnchoker extends Thread {
        private final int _numberOfOptimisticallyUnchokedNeighbors;
        private final int _optimisticUnchokingInterval;
        private final List<AdjacentPeers> _chokedNeighbors = new ArrayList<>();
        final Collection<AdjacentPeers> _optmisticallyUnchokedPeers =
                Collections.newSetFromMap(new ConcurrentHashMap<AdjacentPeers, Boolean>());

        OptimisticUnchoker(peerProcess conf) {
            super("OptimisticUnchoker");
            _numberOfOptimisticallyUnchokedNeighbors = 1;
            _optimisticUnchokingInterval = conf.NumberOfPreferredNeighbors* 1000;
        }

        synchronized void setChokedNeighbors(Collection<AdjacentPeers> chokedNeighbors) {
            _chokedNeighbors.clear();
            _chokedNeighbors.addAll(chokedNeighbors);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(_optimisticUnchokingInterval);
                } catch (InterruptedException ex) {
                }

                synchronized (this) {
                    // Randomly shuffle the remaining neighbors, and select some to optimistically unchoke
                    if (!_chokedNeighbors.isEmpty()) {
                        Collections.shuffle(_chokedNeighbors);
                        _optmisticallyUnchokedPeers.clear();
                        _optmisticallyUnchokedPeers.addAll(_chokedNeighbors.subList(0,
                                Math.min(_numberOfOptimisticallyUnchokedNeighbors, _chokedNeighbors.size())));
                    }
                }

                if (_chokedNeighbors.size() > 0) {
                    LogHelper.getLogger().debug("STATE: OPT UNCHOKED(" + _numberOfOptimisticallyUnchokedNeighbors + "):" + LogHelper.getNeighborsAsString (_optmisticallyUnchokedPeers));
                    _eventLogger.updateOptimisticallyUnchokedNeighbor(LogHelper.getNeighborsAsString (_optmisticallyUnchokedPeers));
                }
                for (Listener listener : _listeners) {
                    Set<Integer> ids = new HashSet<>();
                    for (AdjacentPeers peer : _optmisticallyUnchokedPeers) {
                        ids.add(peer._peerId);
                    }
                    listener.unchockedPeers(ids);
                }
            }
        }
    }

    private final int _numberOfPreferredNeighbors;
    private final int _unchokingInterval;
    private final int _bitmapsize;
    private final EventLogger _eventLogger;
    private final List<AdjacentPeers> _peers = new ArrayList<>();
    private final Collection<AdjacentPeers> _preferredPeers = new HashSet<>();
    private final OptimisticUnchoker _optUnchoker;
    private final Collection<Listener> _listeners = new LinkedList<>();
    private final AtomicBoolean _randomlySelectPreferred = new AtomicBoolean(false);

    PeerManager(int peerId, Collection<AdjacentPeers> peers, int bitmapsize, peerProcess conf) {
        _peers.addAll(peers);
        _numberOfPreferredNeighbors = conf.NumberOfPreferredNeighbors;
        _unchokingInterval = conf.UnchokingInterval * 1000;
        _optUnchoker = new OptimisticUnchoker(conf);
        _bitmapsize = bitmapsize;
        _eventLogger = new EventLogger (peerId, LogHelper.getLogger());
    }

    synchronized void addInterestPeer(int remotePeerId) {
        AdjacentPeers peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer._interested.set(true);
        }
    }

    long getUnchokingInterval() {
        return _unchokingInterval;
    }

    synchronized void removeInterestPeer(int remotePeerId) {
        AdjacentPeers peer = searchPeer(remotePeerId);
        if (peer != null) {
            peer._interested.set(false);
        }
    }

    synchronized List<AdjacentPeers> getInterestedPeers() {
        ArrayList<AdjacentPeers> interestedPeers = new ArrayList<>();
        for (AdjacentPeers peer : _peers){
            if(peer._interested.get()){
                interestedPeers.add(peer);
            }
        }
        return interestedPeers;
    }

    synchronized boolean isInteresting(int peerId, BitSet bitset) {
        AdjacentPeers peer  = searchPeer(peerId);
        if (peer != null) {
            BitSet pBitset = (BitSet) peer._receivedParts.clone();
            pBitset.andNot(bitset);
            return ! pBitset.isEmpty();
        }
        return false;
    }

    synchronized void receivedPart(int peerId, int size) {
        AdjacentPeers peer  = searchPeer(peerId);
        if (peer != null) {
            peer._bytesDownloadedFrom.addAndGet(size);
        }
    }

    synchronized boolean canUploadToPeer(int peerId) {
        AdjacentPeers peerInfo = new AdjacentPeers(peerId, "127.0.0.1",0,false);
        return (_preferredPeers.contains(peerInfo) ||
                _optUnchoker._optmisticallyUnchokedPeers.contains(peerInfo));
    }

    synchronized void fileCompleted() {
        _randomlySelectPreferred.set (true);
    }

    synchronized void bitfieldArrived(int peerId, BitSet bitfield) {
        AdjacentPeers peer  = searchPeer(peerId);
        if (peer != null) {
            peer._receivedParts = bitfield;
        }
        neighborsCompletedDownload();
    }

    synchronized void haveArrived(int peerId, int partId) {
        AdjacentPeers peer  = searchPeer(peerId);
        if (peer != null) {
            peer._receivedParts.set(partId);
        }
        neighborsCompletedDownload();
    }

    synchronized BitSet getReceivedParts(int peerId) {
        AdjacentPeers peer  = searchPeer(peerId);
        if (peer != null) {
            return (BitSet) peer._receivedParts.clone();
        }
        return new BitSet();  // empry bit set
    }

    synchronized private AdjacentPeers searchPeer(int peerId) {
        for (AdjacentPeers peer : _peers) {
            if (peer._peerId == peerId) {
                return peer;
            }
        }
        LogHelper.getLogger().warning("Peer " + peerId + " not found");
        return null;
    }

    synchronized private void neighborsCompletedDownload() {
        for (AdjacentPeers peer : _peers) {
            if (peer._receivedParts.cardinality() < _bitmapsize) {
                // at least one neighbor has not completed
                LogHelper.getLogger().debug("Peer " + peer._peerId + " has not completed yet");
                return;
            }
        }
        for (Listener listener : _listeners) {
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
                Thread.sleep(_unchokingInterval);
            } catch (InterruptedException ex) {
            }

            // 1) GET INTERESTED PEERS AND SORT THEM BY PREFERENCE

            List<AdjacentPeers> interestedPeers = getInterestedPeers();
            if (_randomlySelectPreferred.get()) {
                // Randomly shuffle the neighbors
                LogHelper.getLogger().debug("selecting preferred peers randomly");
                Collections.shuffle(interestedPeers);
            }
            else {
                // Sort the peers in order of preference
                Collections.sort(interestedPeers, new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        AdjacentPeers ri1 = (AdjacentPeers) (o1);
                        AdjacentPeers ri2 = (AdjacentPeers) (o2);
                        // Sort in decreasing order
                        return (ri2._bytesDownloadedFrom.get() - ri1._bytesDownloadedFrom.get());
                    }
                });
            }

            Collection<AdjacentPeers> optUnchokablePeers = null;

            Collection<Integer> chokedPeersIDs = new HashSet<>();
            Collection<Integer> preferredNeighborsIDs = new HashSet<>();
            Map<Integer, Long> downloadedBytes = new HashMap<>();

            synchronized (this) {
                // Reset downloaded bytes, but buffer them for debugging                  
                for (AdjacentPeers peer : _peers) {
                    downloadedBytes.put (peer._peerId, peer._bytesDownloadedFrom.longValue());
                    peer._bytesDownloadedFrom.set(0);
                }

                // 2) SELECT THE PREFERRED PEERS BY SELECTING THE HIGHEST RANKED

                // Select the highest ranked neighbors as "preferred"
                _preferredPeers.clear();
                _preferredPeers.addAll(interestedPeers.subList(0, Math.min(_numberOfPreferredNeighbors, interestedPeers.size())));
                if (_preferredPeers.size() > 0) {
                    _eventLogger.updatePreferredNeighbor(LogHelper.getNeighborsAsString (_preferredPeers));
                }

                // 3) SELECT ALLE THE INTERESTED AND UNINTERESTED PEERS, REMOVE THE PREFERRED. THE RESULTS ARE THE CHOKED PEERS

                Collection<AdjacentPeers> chokedPeers = new LinkedList<>(_peers);
                chokedPeers.removeAll(_preferredPeers);
                Set<Integer> ids = new HashSet<>();
                for (AdjacentPeers peer : _preferredPeers) {
                    ids.add(peer._peerId);
                }
                chokedPeersIDs.addAll(ids);

                // 4) SELECT ALLE THE INTERESTED PEERS, REMOVE THE PREFERRED. THE RESULTS ARE THE CHOKED PEERS THAT ARE "OPTIMISTICALLY-UNCHOKABLE"
                if (_numberOfPreferredNeighbors >= interestedPeers.size()) {
                    optUnchokablePeers = new ArrayList<>();
                }
                else {
                    optUnchokablePeers = interestedPeers.subList(_numberOfPreferredNeighbors, interestedPeers.size());
                }
                preferredNeighborsIDs.addAll (ids);
            }

            // debug
            LogHelper.getLogger().debug("STATE: INTERESTED:" + LogHelper.getNeighborsAsString (interestedPeers));
            LogHelper.getLogger().debug("STATE: UNCHOKED (" + _numberOfPreferredNeighbors + "):" + LogHelper.getNeighborIDsAsString (preferredNeighborsIDs));
            LogHelper.getLogger().debug("STATE: CHOKED:" + LogHelper.getNeighborIDsAsString (chokedPeersIDs));

            for (Entry<Integer,Long> entry : downloadedBytes.entrySet()) {
                String PREFERRED = preferredNeighborsIDs.contains(entry.getKey()) ? " *" : "";
                LogHelper.getLogger().debug("BYTES DOWNLOADED FROM  PEER " + entry.getKey() + ": "
                        + entry.getValue() + " (INTERESTED PEERS: "
                        + interestedPeers.size()+ ": " + LogHelper.getNeighborsAsString (interestedPeers)
                        + ")\t" + PREFERRED);
            }

            // 5) NOTIFY PROCESS, IT WILL TAKE CARE OF SENDING CHOKE AND UNCHOKE MESSAGES

            for (Listener listener : _listeners) {
                listener.chockedPeers(chokedPeersIDs);
                listener.unchockedPeers(preferredNeighborsIDs);
            }

            // 6) NOTIFY THE OPTIMISTICALLY UNCHOKER THREAD WITH THE NEW SET OF UNCHOKABLE PEERS

            if (optUnchokablePeers != null) {
                _optUnchoker.setChokedNeighbors(optUnchokablePeers);
            }
        }

    }
}
