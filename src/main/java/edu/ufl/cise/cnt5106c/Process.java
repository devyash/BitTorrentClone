package edu.ufl.cise.cnt5106c;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public class Process implements Runnable, FileManagerListener, PeerManagerListener {
    private final static int CHOKE = 0;
    private final static int UNCHOKE = 1;
    private final static int NOTINTERESTED = 3;
    private final static int HAVE = 4;
    private final int _peerId;
    private final int _port;
    private final boolean _hasFile;
    private final ReadConfig _conf;
    private final FileManager _fileMgr;
    private final PeerManager _peerMgr;
    private final EventLogger _eventLogger;
    private final AtomicBoolean _fileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _peersFileCompleted = new AtomicBoolean(false);
    private final AtomicBoolean _terminate = new AtomicBoolean(false);
    private final Collection<ConnectionHandler> _connHandlers =
            Collections.newSetFromMap(new ConcurrentHashMap<ConnectionHandler, Boolean>());

    public Process(int peerId, String address, int port, boolean hasFile, Collection<AdjacentPeers> peerInfo, ReadConfig conf) {
        _peerId = peerId;
        _port = port;
        _hasFile = hasFile;
        _conf = conf;    
        //fixed after merging CommonProperties ReadProperties
        _fileMgr = new FileManager(_peerId, _conf.FileName, _conf.FileSize, _conf.PieceSize, _conf.UnchokingInterval * 1000);
        ArrayList<AdjacentPeers> remotePeers = new ArrayList<>(peerInfo);
        for (AdjacentPeers ri : remotePeers) {
            if (ri._peerId == peerId) {
                // rmeove myself
                remotePeers.remove(ri);
                break;
            }
        }
        _peerMgr = new PeerManager(_peerId, remotePeers, _fileMgr.getBitmapSize(), _conf);
        _eventLogger = new EventLogger(peerId,LogHelper.getLogger());
        _fileCompleted.set(_hasFile);
    }

    void init() {
        _fileMgr.registerListener(this);
        _peerMgr.registerListener(this);

        if (_hasFile) {
            LogHelper.getLogger().debug("Spltting file");
            _fileMgr.splitFile();
            _fileMgr.setAllParts();
        }
        else {
            LogHelper.getLogger().debug("Peer does not have file");
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
                    LogHelper.getLogger().debug(Thread.currentThread().getName() + ": Peer " + _peerId + " listening on port " + _port + ".");
                    addConnHandler(new ConnectionHandler(_peerId,false,-1, serverSocket.accept(), _fileMgr, _peerMgr));

                } catch (Exception e) {
                    LogHelper.getLogger().warning(e);
                }
            }
        } catch (IOException ex) {
            LogHelper.getLogger().warning(ex);
        } finally {
            LogHelper.getLogger().warning(Thread.currentThread().getName()
                    + " terminating, TCP connections will no longer be accepted.");
        }
    }

    void connectToPeers(Collection<AdjacentPeers> peersToConnectTo) {
        Iterator<AdjacentPeers> iter = peersToConnectTo.iterator();
        while (iter.hasNext()) {
            do {
                Socket socket = null;
                AdjacentPeers peer = iter.next();
                try {
                    LogHelper.getLogger().debug(" Connecting to peer: " + peer._peerId
                            + " (" + peer._peerAddress + ":" + peer._peerPort + ")");
                    socket = new Socket(peer._peerAddress, peer._peerPort);
                    if (addConnHandler(new ConnectionHandler(_peerId, true, peer._peerId,
                            socket, _fileMgr, _peerMgr))) {
                        iter.remove();
                        LogHelper.getLogger().debug(" Connected to peer: " + peer._peerId
                                + " (" + peer._peerAddress + ":" + peer._peerPort + ")");

                    }
                }
                catch (ConnectException ex) {
                    LogHelper.getLogger().warning("could not connect to peer " + peer._peerId
                            + " at address " + peer._peerAddress + ":" + peer._peerPort);
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                }
                catch (IOException ex) {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex1)
                        {}
                    }
                    LogHelper.getLogger().warning(ex);
                }
            }
            while (iter.hasNext());

            // Keep trying until they all connect
            iter = peersToConnectTo.iterator();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
            }
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
        _eventLogger.fileDownloadedMessage();
        _fileCompleted.set(true);
        if (_fileCompleted.get() && _peersFileCompleted.get()) {
            // The process can quit
            _terminate.set(true);
            System.exit(0);
        }
    }

    @Override
    public synchronized void pieceArrived(int partIdx) {
        for (ConnectionHandler connHanlder : _connHandlers) {
            connHanlder.send(new Payload(HAVE,ByteBuffer.allocate(4).putInt(partIdx).array()));
            if (!_peerMgr.isInteresting(connHanlder.getRemoteNeighborId(), _fileMgr.getReceivedParts())) {
                connHanlder.send(new OnlyType(NOTINTERESTED));
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

        }
        else {
            LogHelper.getLogger().debug("Peer " + connHandler.getRemoteNeighborId() + " is trying to connect but a connection already exists");
        }
        return true;
    }

    @Override
    public synchronized void chockedPeers(Collection<Integer> chokedPeersIds) {
        for (ConnectionHandler ch : _connHandlers) {
            if (chokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Choking " + ch.getRemoteNeighborId());
                ch.send(new OnlyType(CHOKE));
            }
        }
    }

    @Override
    public synchronized void unchockedPeers(Collection<Integer> unchokedPeersIds) {
        for (ConnectionHandler ch : _connHandlers) {
            if (unchokedPeersIds.contains(ch.getRemoteNeighborId())) {
                LogHelper.getLogger().debug("Unchoking " + ch.getRemoteNeighborId());
                ch.send(new OnlyType(UNCHOKE));
            }
        }
    }
}
