package edu.ufl.cise.cnt5106c;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class peerProcess {

    private static final String commonCfgFileName = "Common.cfg";
    public int NumberOfPreferredNeighbors;
    public int UnchokingInterval;
    public int OptimisticUnchokingInterval;
    public String FileName;
    public int FileSize;
    public int PieceSize;

    public void readCommonCfgFile() {
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
    public void readPeerInfoCfgFile() {
        //TODO Put status in logger - reading peerconfig started
        num_wait = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(peerInfoFileName));
            String in = reader.readLine();
            String[] split = in .split(" ");
            if ( in == null)
                System.out.print("\nYo\n");
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
     /*// All neighbors start out as choked
     this.neighborStatus.put(id, PeerStatus.Choked);
     // and uninterested
     this.neighborInterestedStatus.put(id,
             PeerInterestedStatus.NotInterested);*/
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
            LogHelper.getLogger().severe("the number of arguments passed to the program is " + args.length + " while it should be 1.\nUsage: java peerProcess peerId");
        }
        final int peerId = Integer.parseInt(args[0]);
        LogHelper.configure(peerId);
        String address = "localhost";
        int port = 6008;
        boolean hasFile = false;
        peerProcess peerInfo = new peerProcess();
        Collection < AdjacentPeers > peersToConnectTo = new LinkedList < > ();
        try {
            peerInfo.readCommonCfgFile();
            peerInfo.readPeerInfoCfgFile();
            for (AdjacentPeers peer: peerInfo.neighbors) {
                if (peerId == peer._peerId) {
                    address = peer._peerAddress;
                    port = peer._peerPort;
                    hasFile = peer._hasFile;
                    break;
                } else {
                    peersToConnectTo.add(peer);
                    LogHelper.getLogger().conf("Read configuration for peer: " + peer);
                }
            }
        } catch (Exception ex) {
            LogHelper.getLogger().severe(ex);
            return;
        }

        Process peerProc = new Process(peerId, address, port, hasFile, peerInfo.neighbors, peerInfo);
        peerProc.init();
        Thread t = new Thread(peerProc);
        t.setName("peerProcess-" + peerId);
        t.start();

        LogHelper.getLogger().debug("Connecting to " + peersToConnectTo.size() + " peers.");
        peerProc.connectToPeers(peersToConnectTo);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
