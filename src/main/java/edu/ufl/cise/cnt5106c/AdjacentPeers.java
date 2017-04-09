package edu.ufl.cise.cnt5106c;

import java.util.*;
import java.util.concurrent.atomic.*;

/*
the following class recognizes adjacent peers and checks if the adjacent peers has the files needed.
Interested message is sent by the peer to another peer if the other peer has the file needed by that peer.
*/

public class AdjacentPeers {
    public final int id;
    public final String address;
    public final int port;

    public final boolean has_File_flag;
    public AtomicInteger bytes_Downloaded_From = new AtomicInteger (0);;
    public BitSet received_Parts;
    public AtomicBoolean event_Interested  = new AtomicBoolean (false);

    /*
	The djacent peers event_have their own peer id, peer address and port where they are located.
	Hasfile defines if the adjacent peer has the file needed by the peer
    */
	public AdjacentPeers(int pId, String pAddress, int pPort, boolean hasFile) {
        this.id = pId;
        this.address = pAddress;
        this.port = pPort;
        this.has_File_flag = hasFile;
        this.received_Parts = new BitSet();
    }

    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof AdjacentPeers) {
            return (((AdjacentPeers) obj).id == id);
        }
        return false;
    }
/*
This function needs to be overwritten as the equals function has been overwritten.
*/
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.id);
        return hash;
    }
/*
This function overrides the String class's to String method.
Used to print the details of the Adjacent Peer Class
 */
    public String toString() {
        return new StringBuilder (id)
                .append (" The address:").append (address)
                .append(" The port number: ").append(port).toString();
    }

}
