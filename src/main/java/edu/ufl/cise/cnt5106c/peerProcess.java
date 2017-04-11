package edu.ufl.cise.cnt5106c;

import java.io.*;
import java.util.*;
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

    public void read_Common() {
        //TODO Put status in logger - reading started
        try {
            //Reads the Common.cfg
            //Common.cfg
            //NumberOfPreferredNeighbors 2
            // UnchokingInterval 5
            // OptimisticUnchokingInterval 15
            // FileName TheFile.dat
            // FileSize 10000232
            // PieceSize 32768
            Properties commonProp = new Properties();
            String  thisLine = null;
            BufferedReader br = new BufferedReader(new FileReader(commonCfgFileName));
            while ((thisLine = br.readLine()) != null) {
                //System.out.println(thisLine);
                String[] hm=thisLine.split(" ");
                commonProp.put(hm[0],hm[1]);
            }
            br.close();
            this.FileName=commonProp.getProperty("FileName");
            this.FileSize=Integer.parseInt(commonProp.getProperty("FileSize"));
            this.PieceSize=Integer.parseInt(commonProp.getProperty("PieceSize"));
            this.OptimisticUnchokingInterval=Integer.parseInt(commonProp.getProperty("OptimisticUnchokingInterval"));
            this.UnchokingInterval=Integer.parseInt(commonProp.getProperty("UnchokingInterval"));
            this.NumberOfPreferredNeighbors=Integer.parseInt(commonProp.getProperty("NumberOfPreferredNeighbors"));
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
            LogHelper.getLogger().severe("Incorrect format! Run like this 'java peerProcess 1001'. ");
        }
        final int peerId = Integer.parseInt(args[0]);
        LogHelper.configure(peerId);
        String address = "localhost";
        int port = 6008;
        boolean hasFile = false;
        peerProcess peerInfo = new peerProcess();
        Collection < AdjacentPeers > peersToConnectTo = new LinkedList < > ();
        try {
            peerInfo.read_Common();
            peerInfo.readPeerInfoCfgFile();
            for (AdjacentPeers peer: peerInfo.neighbors) {
                if (peerId == peer.peer_Id) {
                    address = peer.peer_Address;
                    port = peer.peer_Port;
                    hasFile = peer.has_File;
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

        PeerProcessThread peerProc = new PeerProcessThread(peerId, address, port, hasFile, peerInfo.neighbors, peerInfo);
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
