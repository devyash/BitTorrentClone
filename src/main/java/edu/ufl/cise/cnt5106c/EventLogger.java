package edu.ufl.cise.cnt5106c;


/*
EventLogger provides information about network traffic, usage and other conditions
We can define what is going within the network with the help of event logger
*/
public class EventLogger {

    private final LogHelper logHelper;
    private final String header;

    public EventLogger(int pID, LogHelper logHelper) {
        header = ": Peer ID : " + pID;
        this.logHelper = logHelper;
    }

    private String getLogHeader() {
        String logs = String.format(header);
        return logs;
    }


    public void event_Connect(int pID, boolean isConnectingPeer) {
        final String message;
        if(isConnectingPeer)
            message = getLogHeader() + " establishing connection with peer %d .";
        else
            message = getLogHeader() + " is already connected from peer %d .";
        logHelper.info(String.format(message, pID));
    }

    public void event_Interested (int pID) {
        logHelper.info(String.format(getLogHeader() + " Received the 'INTERESTED' message from %d.", pID));
    }

    public void event_Not_Interested (int pID) {
        logHelper.info (String.format(getLogHeader() + " Received the 'NOT INTERESTED' message from %d.", pID));
    }

    public void event_Choke(int pID) {
        logHelper.info(String.format(getLogHeader() + " is CHOKED by %d.", pID));
    }

    public void event_Unchoke(int pID) {
        logHelper.info(String.format(getLogHeader() + " is UNCHOKED by %d.", pID));
    }

    public void event_have(int pID, int pieceIndex) {
        logHelper.info(String.format(getLogHeader() + " received the 'HAVE' message for the piece %d from %d .", pieceIndex, pID));
    }

    public void event_Preferred_Neighbors (String neighbors) {
        logHelper.info (String.format (getLogHeader() + " has preferred neighbors %s", neighbors));
    }

    public void event_Optimistically_Unchoked_Neighbor (String neighbors) {
        logHelper.info (String.format(getLogHeader() + " has the optimistically Un-choked neighbor %s", neighbors));
    }

    public void event_Piece_Downloaded_Message (int pID, int pieceIdx, int currNumberOfPieces) {
        logHelper.info(String.format(getLogHeader() + " has downloaded the piece %d from peer %d. Total pieces it possess %d.", pieceIdx, pID, currNumberOfPieces));
    }

    public void event_File_Downloaded () {
        logHelper.info(String.format(getLogHeader() + " Complete file has been Downloaded! "));
    }
}
