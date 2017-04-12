
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.ServerSocket;

public class PeerProcessThread implements Runnable, Listener {
    private final static int CHOKE = 0;
    private final static int UNCHOKE = 1;
    private final static int NOTINTERESTED = 3;
    private final static int HAVE = 4;
    private final int peerId;
    private final int port;
    private final boolean hasFile;
    private final peerProcess config;
    private final AtomicBoolean terminateFlag = new AtomicBoolean(false);
    public final Collection < ConnectionOrganizer > connOrganizer =
            Collections.newSetFromMap(new ConcurrentHashMap < ConnectionOrganizer, Boolean > ());
    public final FileOrganizer fileOrganizer;
    public final PeerOrganizer peerOrganizer;
    private final EventLogger eventLogger;
    private final AtomicBoolean fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean peersFileCompleted = new AtomicBoolean(false);

    public PeerProcessThread(int peerId, String address, int port, boolean hasFile, Collection < AdjacentPeers > peerInfo, peerProcess conf) {
        this.peerId = peerId;
        this.port = port;
        this.hasFile = hasFile;
        this.config = conf;
        this.fileOrganizer = new FileOrganizer(peerId, config.FileName, config.FileSize, config.PieceSize, config.UnchokingInterval * 1000);
        ArrayList < AdjacentPeers > remotePeers = new ArrayList < > (peerInfo);
        for (AdjacentPeers ri: remotePeers) {
            if (ri.peer_Id == peerId) {
                remotePeers.remove(ri);
                break;
            }
        }
        this.peerOrganizer = new PeerOrganizer(peerId, remotePeers, fileOrganizer.getBitmapSize(), config);
        this.eventLogger = new EventLogger(peerId, LogHelper.getLogger());
        this.fileCompleted.set(hasFile);
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (!terminateFlag.get()) {
                try {
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + peerId + " listening on port " + port + ".");
                    ConnectionOrganizer con = new ConnectionOrganizer(peerId, false, -1, serverSocket.accept(), fileOrganizer, peerOrganizer);

                    if (!connOrganizer.contains(con)) {
                        connOrganizer.add(con);
                        new Thread(con).start();
                        try {
                            wait(10);
                        } catch (InterruptedException e) {
                            LogHelper.getLogger().warning(e);
                        }

                    }
                    else {
                        LogHelper.getLogger().debug("Peer " + con.getRemoteNeighborId() + " is trying to connect but a connection already exists");
                    }

                } catch (Exception e) {
                    LogHelper.getLogger().warning(e);
                }
            }
        } catch (IOException ex) {
            LogHelper.getLogger().warning(ex);
        } finally {
            LogHelper.getLogger().warning(Thread.currentThread().getName() + " terminating, TCP connections will no longer be accepted.");
        }
    }

    @Override
    public void neighborsCompletedDownload() {
        LogHelper.getLogger().debug("all peers completed download");
        peersFileCompleted.set(true);
        if (fileCompleted.get() && peersFileCompleted.get()) {
            // The process can quit
            terminateFlag.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void fileCompleted() {
        LogHelper.getLogger().debug("local peer completed download");
        eventLogger.fileDownloadedMessage();
        fileCompleted.set(true);
        if (fileCompleted.get() && peersFileCompleted.get()) {
            // The process can quit
            terminateFlag.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionOrganizer connHanlder: connOrganizer) {
            connHanlder.send(new ActualMessage(HAVE, ByteBuffer.allocate(4).putInt(partIdx).array()));
            boolean flag  = false;
            AdjacentPeers peer = peerOrganizer.searchPeer(connHanlder.getRemoteNeighborId());
            if (peer != null) {
                BitSet pBitset = (BitSet) peer.received_Parts.clone();
                pBitset.andNot(fileOrganizer.getReceivedParts());
                 flag = !pBitset.isEmpty();
            }
            if (!flag) {
                connHanlder.send(new ActualMessage(NOTINTERESTED, null));
            }
        }
    }

    @Override
    public synchronized void chockedPeers(Collection < Integer > chokedPeersIds) {
        for (ConnectionOrganizer ch: connOrganizer) {
            if (chokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Choking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(CHOKE, null));
            }
        }
    }

    @Override
    public synchronized void unchockedPeers(Collection < Integer > unchokedPeersIds) {
        for (ConnectionOrganizer ch: connOrganizer) {
            if (unchokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Unchoking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(UNCHOKE, null));
            }
        }
    }
}
