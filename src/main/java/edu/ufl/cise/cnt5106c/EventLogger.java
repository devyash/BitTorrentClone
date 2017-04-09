package edu.ufl.cise.cnt5106c;


/*
EventLogger provides information about network traffic, usage and other conditions
We can define what is going within the network with the help of event looger
*/
public class EventLogger {

    private final LogHelper logHelper;
    private final String header;

    public EventLogger(int pID, LogHelper logHelper) {
        header = ": Peer " + pID;
        this.logHelper = logHelper;
    }

    private String getLogHeader() {
        String logs = String.format(header);
        return logs;
    }


    public void ConnectWithPeer(int pID, boolean isConnectingPeer) {
        final String message;
        if(isConnectingPeer)
            message = getLogHeader() + " establishing connection with peer %d .";
        else
            message = getLogHeader() + " is already connected from peer %d .";
        logHelper.info(String.format(message, pID));
    }

    public void interested (int pID) {
        logHelper.info(String.format(getLogHeader() + " received the 'interested' message from %d.", pID));
    }

    public void notInterested (int pID) {
        logHelper.info (String.format(getLogHeader() + " received the 'not interested' message from %d.", pID));
    }

    public void choke(int pID) {
        logHelper.info(String.format(getLogHeader() + " is choked by %d.", pID));
    }

    public void unchoke(int pID) {
        logHelper.info(String.format(getLogHeader() + " is unchoked by %d.", pID));
    }

    public void have(int pID, int pieceIndex) {
        logHelper.info(String.format(getLogHeader() + " received the 'have' message for the piece %d from %d .", pieceIndex, pID));
    }

    public void updatePreferredNeighbor (String neighbors) {
        logHelper.info (String.format (getLogHeader() + " has preferred neighbors %s", neighbors));
    }

    public void updateOptimisticallyUnchokedNeighbor (String neighbors) {
        logHelper.info (String.format(getLogHeader() + " has the optimistically unchoked neighbor %s", neighbors));
    }

    public void pieceDownloadedMessage (int pID, int pieceIdx, int currNumberOfPieces) {
        logHelper.info(String.format(getLogHeader() + " has downloaded the piece %d from peer %d. Total pieces it possess %d.", pieceIdx, pID, currNumberOfPieces));
    }

    public void fileDownloadedMessage () {
        logHelper.info(String.format(getLogHeader() + " complete file has been downloaded. "));
    }
}
