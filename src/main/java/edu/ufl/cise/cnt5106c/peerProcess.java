package edu.ufl.cise.cnt5106c;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

public class peerProcess {

    public static void main (String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (args.length != 1) {
            LogHelper.getLogger().severe("the number of arguments passed to the program is " + args.length + " while it should be 1.\nUsage: java peerProcess peerId");
        }
        final int peerId = Integer.parseInt(args[0]);
        LogHelper.configure(peerId);
        String address = "localhost";
        int port = 6008;
        boolean hasFile = false;
        ReadConfig peerInfo = new ReadConfig();
        Collection<AdjacentPeers> peersToConnectTo = new LinkedList<>();
        try {
            peerInfo.readPeerInfoCfgFile();
            for (AdjacentPeers peer : peerInfo.neighbors) {
                if (peerId == peer._peerId) {
                    address = peer._peerAddress;
                    port = peer._peerPort;
                    hasFile = peer._hasFile;
                    break;
                }
                else { 
                    peersToConnectTo.add (peer);
                    LogHelper.getLogger().conf ("Read configuration for peer: " + peer);
                }
            }
        }
        catch (Exception ex) {
            LogHelper.getLogger().severe (ex);
            return;
        }

        Process peerProc = new Process (peerId, address, port, hasFile, peerInfo.neighbors, peerInfo);
        peerProc.init();
        Thread t = new Thread (peerProc);
        t.setName ("peerProcess-" + peerId);
        t.start();

        LogHelper.getLogger().debug ("Connecting to " + peersToConnectTo.size() + " peers.");
        peerProc.connectToPeers (peersToConnectTo);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
