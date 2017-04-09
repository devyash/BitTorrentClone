package edu.ufl.cise.cnt5106c;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Jiya on 4/8/17.
 */
public class ReadConfig {
    ReadConfig() {
        this.readCommonCfgFile();
    }
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
}