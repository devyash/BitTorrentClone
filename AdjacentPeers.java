

import java.util.*;
import java.util.concurrent.atomic.*;

/*
his class identifies all the peers and checks if the peer has the file required
*/

public class AdjacentPeers {

    public final String peer_Address;
    public AtomicInteger download_Bytes = new AtomicInteger (0);;
    public final boolean has_File;
    public BitSet received_Parts;
    public final int peer_Port;
    public AtomicBoolean interested  = new AtomicBoolean (false);
    public final int peer_Id;

    /*
    Every adjacent peer has peer id, peer address and its own port
    */
    public AdjacentPeers(int peer_Id, String peer_Address, int peer_Port, boolean has_File) {
        this.peer_Id = peer_Id;
        this.peer_Address = peer_Address;
        this.peer_Port = peer_Port;
        this.has_File = has_File;
        this.received_Parts = new BitSet();
    }
    //Used to print the Adjacent peer object
    public String toString() {
        return new StringBuilder (peer_Id)
                .append (" peer address:").append (peer_Address)
                .append(" peer port: ").append(peer_Port).toString();
    }

    //Used to compare to objects
    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof AdjacentPeers) {
            return (((AdjacentPeers) obj).peer_Id == peer_Id);
        }
        return false;
    }

//This functions needs to be overridden when over-riding the equals file
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.peer_Id);
        return hash;
    }


}
