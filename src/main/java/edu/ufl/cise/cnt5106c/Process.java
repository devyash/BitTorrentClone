package edu.ufl.cise.cnt5106c;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public class Process implements Runnable, Listener {
    private final static int CHOKE = 0;
    private final static int UNCHOKE = 1;
    private final static int NOTINTERESTED = 3;
    private final static int HAVE = 4;
    private final int id;
    private final int _port;
    private final boolean has_File_flag;
    private final peerProcess _conf;
    private final FileManager file_Manager;
    private final PeerManager _peerMgr;
    private final EventLogger _eventLogger;
    private final AtomicBoolean _fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _peersFileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _terminate = new AtomicBoolean(false);
    private final Collection < ConnectionHandler > _connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap < ConnectionHandler, Boolean > ());

    public Process(int peerId, String address, int port, boolean hasFile, Collection < AdjacentPeers > peerInfo, peerProcess conf) {
        id = peerId;
        _port = port;
        has_File_flag = hasFile;
        _conf = conf;
        //fixed after merging CommonProperties ReadProperties
        //System.out.println("\nFileManager obj "+_conf.FileName +", "+ _conf.FileSize +", "+ _conf.PieceSize +", "+ _conf.UnchokingInterval);
        file_Manager = new FileManager(id, _conf.FileName, _conf.FileSize, _conf.PieceSize, _conf.UnchokingInterval * 1000);
        ArrayList < AdjacentPeers > remotePeers = new ArrayList < > (peerInfo);
        for (AdjacentPeers ri: remotePeers) {
            if (ri.id == peerId) {
                // rmeove myself
                remotePeers.remove(ri);
                break;
            }
        }
        _peerMgr = new PeerManager(id, remotePeers, file_Manager.getBitmapSize(), _conf);
        _eventLogger = new EventLogger(peerId, LogHelper.getLogger());
        _fileCompleted.set(has_File_flag);
    }

    void init() {
        file_Manager.registerListener(this);
        _peerMgr.registerListener(this);

        if (has_File_flag) {
            LogHelper.getLogger().debug("Spltting file");
            file_Manager.splitFile();
            file_Manager.setAllParts();
        } else {
            LogHelper.getLogger().debug("Peer does not event_have file");
        }

        // Start PeerMnager Thread
        Thread t = new Thread(_peerMgr);
        t.setName(_peerMgr.getClass().getName());
        t.start();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(_port);
            while (!_terminate.get()) {
                try {
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + id + " listening on port " + _port + ".");
                    addConnHandler(new ConnectionHandler(id, false, -1, serverSocket.accept(), file_Manager, _peerMgr));

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
                    LogHelper.getLogger().debug(" Connecting to peer: " + peer.id + " (" + peer.address + ":" + peer.port + ")");
                    socket = new Socket(peer.address, peer.port);
                    if (addConnHandler(new ConnectionHandler(id, true, peer.id,
                            socket, file_Manager, _peerMgr))) {
                        iter.remove();
                        LogHelper.getLogger().debug(" Connected to peer: " + peer.id + " (" + peer.address + ":" + peer.port + ")");

                    }
                } catch (ConnectException ex) {
                    LogHelper.getLogger().warning("could not connect to peer " + peer.id + " at address " + peer.address + ":" + peer.port);
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
        _peersFileCompleted.set(true);
        if (_fileCompleted.get() && _peersFileCompleted.get()) {
            // The process can quit
            _terminate.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void fileCompleted() {
        LogHelper.getLogger().debug("local peer completed download");
        _eventLogger.event_File_Downloaded();
        _fileCompleted.set(true);
        if (_fileCompleted.get() && _peersFileCompleted.get()) {
            // The process can quit
            _terminate.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionHandler connHanlder: _connHandlers) {
            connHanlder.send(new ActualMessage(HAVE, ByteBuffer.allocate(4).putInt(partIdx).array()));
            boolean flag = false;
            AdjacentPeers peer = _peerMgr.searchPeer(connHanlder.getRemoteNeighborId());
            if (peer != null) {
                BitSet pBitset = (BitSet) peer.received_Parts.clone();
                pBitset.andNot(file_Manager.getReceivedParts());
                flag = !pBitset.isEmpty();
            }
            if (!flag) {
                connHanlder.send(new ActualMessage(NOTINTERESTED, null));
            }
        }
    }

    private synchronized boolean addConnHandler(ConnectionHandler connHandler) {
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
    public synchronized void chockedPeers(Collection < Integer > event_ChokedPeersIds) {
        for (ConnectionHandler ch: _connHandlers) {
            if (event_ChokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Choking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(CHOKE, null));
            }
        }
    }

    @Override
    public synchronized void unchockedPeers(Collection < Integer > event_UnchokedPeersIds) {
        for (ConnectionHandler ch: _connHandlers) {
            if (event_UnchokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Unchoking " + ch.getRemoteNeighborId());
                ch.send(new ActualMessage(UNCHOKE, null));
            }
        }
    }
}
