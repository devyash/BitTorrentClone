

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;

import static java.lang.Thread.sleep;

/*
* This is the main class that is required as per the description which calls the peerProcessThread.
*
* */
public class peerProcess {

    private static final String commonCfgFileName = "Common.cfg";
    public int NumberOfPreferredNeighbors;
    public int UnchokingInterval;
    public int OptimisticUnchokingInterval;
    public String FileName;
    public int FileSize;
    public int PieceSize;

    public void readCommonCfg() {
            //TODO Put status in logger - reading started
            try {
                BufferedReader reader = new BufferedReader(new FileReader(commonCfgFileName));
                String in = reader.readLine();
                while ( in != null) {
                    String[] split = in .split(" ");
                    switch (split[0]) {
                        case "NumberOfPreferredNeighbors":
                            NumberOfPreferredNeighbors = Integer.parseInt(split[1]);
                            break;
                        case "UnchokingInterval":
                            UnchokingInterval = Integer.parseInt(split[1]);
                            break;
                        case "OptimisticUnchokingInterval":
                            OptimisticUnchokingInterval = Integer.parseInt(split[1]);
                            break;
                        case "FileName":
                            FileName = split[1];
                            break;
                        case "FileSize":
                            FileSize = Integer.parseInt(split[1]);
                            break;
                        case "PieceSize":
                            PieceSize = Integer.parseInt(split[1]);
                            break;
                    } in = reader.readLine();
                }
                reader.close();
            } catch (Exception e) {
                //TODO Put status in logger - reading error
                e.printStackTrace();
                System.exit(-1);
            }
            //TODO Put status in logger - reading done
        }


    private static final String peerInfoFileName = "PeerInfo.cfg";
    private int num_wait;
    private int myid;
    private int port;
    public final List < AdjacentPeers > neighbors = new LinkedList < > ();

    /**
     * Read in variables from config file, set all appropriate variables
     */
    public void readPeerInfoCfg() {
        //TODO Put status in logger - reading peerconfig started

        num_wait = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(peerInfoFileName));
            String in = reader.readLine();
            String[] split;
            while ( in != null) {
                split = in .split(" ");
                int id = Integer.parseInt(split[0]);
                String hostname = split[1];
                int port = Integer.parseInt(split[2]);
                boolean hasFile = Integer.parseInt(split[3]) == 1;
                AdjacentPeers nbr;
                if (id != this.myid) {
                    nbr = new AdjacentPeers(id, hostname, port, hasFile);
                    neighbors.add(nbr);
                    if (id > this.myid)
                        num_wait++;
                } else
                    nbr = new AdjacentPeers(myid, hostname, port, hasFile); in = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            //TODO Put status in logger - reading error
            e.printStackTrace();
            System.exit(-1);
        }
        //TODO Put status in logger - reading done
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (args.length != 1) {
            LogHelper.getLogger().error("Incorrect format! Run like this 'java peerProcess 1001'. ");
        }
        final int peerId = Integer.parseInt(args[0]);
        LogHelper.configure(peerId);
        String address = "localhost";
        int port = 6008;
        boolean hasFile = false;
        peerProcess peerInfo = new peerProcess();
        Collection < AdjacentPeers > peersToConnectTo = new LinkedList < > ();
        try {
            peerInfo.readCommonCfg();
            peerInfo.readPeerInfoCfg();
            for (AdjacentPeers peer: peerInfo.neighbors) {
                if (peerId == peer.peer_Id) {
                    address = peer.peer_Address;
                    port = peer.peer_Port;
                    hasFile = peer.has_File;
                    break;
                } else {
                    peersToConnectTo.add(peer);
                }
            }
        } catch (Exception ex) {
            LogHelper.getLogger().error(ex);
            return;
        }

        //initialize peerProcessThread and register it to listeners
        PeerProcessThread peerProc = new PeerProcessThread(peerId, address, port, hasFile, peerInfo.neighbors, peerInfo);
        peerProc.fileOrganizer.registerListener(peerProc);
        peerProc.peerOrganizer.registerListener(peerProc);

        if (hasFile) {
            LogHelper.getLogger().debug("The file is being split!");
            peerProc.fileOrganizer.splitFile();
            peerProc.fileOrganizer.setAllParts();
        } else {
            LogHelper.getLogger().debug("No file found with the peer!");
        }
        Thread t = new Thread(peerProc.peerOrganizer);
        t.setName(peerProc.peerOrganizer.getClass().getName());
        t.start();

        Thread curr = new Thread(peerProc);
        curr.setName("peerProcess-" + peerId);
        curr.start();

        LogHelper.getLogger().debug("Connecting to " + peersToConnectTo.size() + " peers.");

        //Connect to peers

        Iterator < AdjacentPeers > iter = peersToConnectTo.iterator();
        while (iter.hasNext()) {
            do {
                Socket socket = null;
                AdjacentPeers peer = iter.next();
                try {
                    LogHelper.getLogger().debug(" Connecting to peer: " + peer.peer_Id + " (" + peer.peer_Address + ":" + peer.peer_Port + ")");
                    socket = new Socket(peer.peer_Address, peer.peer_Port);
                    ConnectionOrganizer con = new ConnectionOrganizer(peerId, true, peer.peer_Id,
                            socket, peerProc.fileOrganizer, peerProc.peerOrganizer);

                    if (!peerProc.connOrganizer.contains(con)) {
                        peerProc.connOrganizer.add(con);
                        new Thread(con).start();
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            LogHelper.getLogger().warning(e);
                        }
                    } else {
                        LogHelper.getLogger().debug("Peer " + con.getRemoteNeighborId() + " is trying to connect but a connection already exists");
                    }
                    iter.remove();
                    LogHelper.getLogger().debug(" Connected to peer: " + peer.peer_Id + " (" + peer.peer_Address + ":" + peer.peer_Port + ")");
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
                sleep(5);
            } catch (InterruptedException ex) {}
        }

        try {
            sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
