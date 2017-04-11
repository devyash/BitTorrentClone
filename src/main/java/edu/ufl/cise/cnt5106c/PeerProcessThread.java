package edu.ufl.cise.cnt5106c;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.Socket;
import java.util.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;



public class PeerProcessThread implements Runnable, Listener {
    private final static int CHOKE = 0;
    private final static int UNCHOKE = 1;
    private final static int NOTINTERESTED = 3;
    private final static int HAVE = 4;
    private final int peer_Id;
    private final int _port;
    private final boolean has_File;
    private final peerProcess config;
    private final AtomicBoolean terminate_flag = new AtomicBoolean(false);
    private final Collection < ConnectionOrganizer > _connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap < ConnectionOrganizer, Boolean > ());
    private final FileOrganizer fileManager;
    private final PeerOrganizer peerManager;
    private final EventLogger eventLogger;
    private final AtomicBoolean file_completed_flag = new AtomicBoolean(false);
    private final AtomicBoolean peers_file_completed_flag = new AtomicBoolean(false);


    public PeerProcessThread(int peerId, String address, int port, boolean hasFile, Collection < AdjacentPeers > peerInfo, peerProcess conf) {
        this.peer_Id = peerId;
        this._port = port;
        this.has_File = hasFile;
        this.config = conf;
        this.fileManager = new FileOrganizer(peer_Id, config.FileName, config.FileSize, config.PieceSize, config.UnchokingInterval * 1000);
        ArrayList < AdjacentPeers > remotePeers = new ArrayList < > (peerInfo);
        for (AdjacentPeers ri: remotePeers) {
            if (ri.peer_Id == peerId) {
                remotePeers.remove(ri);
                break;
            }
        }
        this.peerManager = new PeerOrganizer(peer_Id, remotePeers, fileManager.getBitmapSize(), config);
        this.eventLogger = new EventLogger(peerId, LogHelper.getLogger());
        this.file_completed_flag.set(has_File);
    }

    void init() {
        fileManager.registerListener(this);
        peerManager.registerListener(this);

        if (has_File) {
            LogHelper.getLogger().debug("The file is being split!");
            fileManager.splitFile();
            fileManager.setAllParts();
        } else {
            LogHelper.getLogger().debug("No file found with the peer!");
        }

        // Start PeerOrganizer Thread
        Thread t = new Thread(peerManager);
        t.setName(peerManager.getClass().getName());
        t.start();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(_port);
            while (!terminate_flag.get()) {
                try {
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + peer_Id + " listening on port " + _port + ".");
                    addConnHandler(new ConnectionOrganizer(peer_Id, false, -1, serverSocket.accept(), fileManager, peerManager));

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

    void connectToPeers(Collection < AdjacentPeers > peersToConnectTo) {
        Iterator < AdjacentPeers > iter = peersToConnectTo.iterator();
        while (iter.hasNext()) {
            do {
                Socket socket = null;
                AdjacentPeers peer = iter.next();
                try {
                    LogHelper.getLogger().debug(" Connecting to peer: " + peer.peer_Id + " (" + peer.peer_Address + ":" + peer.peer_Port + ")");
                    socket = new Socket(peer.peer_Address, peer.peer_Port);
                    if (addConnHandler(new ConnectionOrganizer(peer_Id, true, peer.peer_Id,
                            socket, fileManager, peerManager))) {
                        iter.remove();
                        LogHelper.getLogger().debug(" Connected to peer: " + peer.peer_Id + " (" + peer.peer_Address + ":" + peer.peer_Port + ")");

                    }
                } catch (ConnectException ex) {
                    LogHelper.getLogger().warning("could not connect to peer " + peer.peer_Id + " at address " + peer.peer_Address + ":" + peer.peer_Port);
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1) {}
                    }
                } catch (IOException ex) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1) {}
                    }
                    LogHelper.getLogger().warning(ex);
                }
            }
            while (iter.hasNext());

            // Keep trying until they all connect
            iter = peersToConnectTo.iterator();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {}
        }
    }

    @Override
    public void neighborsCompletedDownload() {
        LogHelper.getLogger().debug("all peers completed download");
        peers_file_completed_flag.set(true);
        if (file_completed_flag.get() && peers_file_completed_flag.get()) {
            // The process can quit
            terminate_flag.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void fileCompleted() {
        LogHelper.getLogger().debug("local peer completed download");
        eventLogger.fileDownloadedMessage();
        file_completed_flag.set(true);
        if (file_completed_flag.get() && peers_file_completed_flag.get()) {
            // The process can quit
            terminate_flag.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionOrganizer connHanlder: _connHandlers) {
            connHanlder.send(new ActualMessage(HAVE, ByteBuffer.allocate(4).putInt(partIdx).array()));
            boolean flag = false;
            AdjacentPeers peer = peerManager.searchPeer(connHanlder.getRemoteNeighborId());
            if (peer != null) {
                BitSet pBitset = (BitSet) peer.received_Parts.clone();
                pBitset.andNot(fileManager.getReceivedParts());
                flag = !pBitset.isEmpty();
            }
            if (!flag) {
                connHanlder.send(new ActualMessage(NOTINTERESTED, null));
            }
        }
    }

    private synchronized boolean addConnHandler(ConnectionOrganizer connHandler) {
        if (!_connHandlers.contains(connHandler)) {
            _connHandlers.add(connHandler);
            new Thread(connHandler).start();
            try {
                wait(10);
            } catch (InterruptedException e) {
                LogHelper.getLogger().warning(e);
            }

        } else {
            LogHelper.getLogger().debug("Peer " + connHandler.getRemoteNeighborId() + " is trying to connect but a connection already exists");
        }
        return true;
    }

    @Override
    public synchronized void chockedPeers(Collection < Integer > chokedPeersIds) {
        for (ConnectionOrganizer ch: _connHandlers) {
            if (chokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Choking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(CHOKE, null));
            }
        }
    }

    @Override
    public synchronized void unchockedPeers(Collection < Integer > unchokedPeersIds) {
        for (ConnectionOrganizer ch: _connHandlers) {
            if (unchokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Unchoking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(UNCHOKE, null));
            }
        }
    }
}
