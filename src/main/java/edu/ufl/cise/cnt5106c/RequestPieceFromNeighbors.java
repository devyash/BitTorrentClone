package edu.ufl.cise.cnt5106c;
/*
* This class is used to set the timer for un-choking Interval.
* As well as, Request for pieces.
* */


import java.util.*;

public class RequestPieceFromNeighbors {

    private final long timeoutInMS;
    private final BitSet desiredPiece;
    /*Constructor for creating an object of RequestPieceFrom Neighbors,
     Sets the required piece number, and the unchoking interval */
    RequestPieceFromNeighbors(int noOfPiece, long unchokingInterval) {
        desiredPiece = new BitSet (noOfPiece);
        timeoutInMS = unchokingInterval; //while calling multiply by 2 for a roundtrip
    }

    /*Synchronized class to request a piece from neighbor */
    synchronized int getRequestedPiece(BitSet availableParts) {
        availableParts.andNot(desiredPiece); //clears all the bits in available parts
        if (!availableParts.isEmpty()) {
            if (availableParts.isEmpty())
                throw new RuntimeException ("Empty BitSet Found!");
            String set = availableParts.toString();
            String[] indexes = set.substring(1, set.length()-1).split(",");
            int len = indexes.length-1;
            int randomIndex = (int) (Math.random() *  len);
            String ret = indexes[randomIndex].trim();
            final int pieceId =  Integer.parseInt(ret);
            desiredPiece.set(pieceId);

            /*reset the piece so that it can be requested again
            if not already downloaded in previous time frame from previous unchocked neighbor */
            TimerTask taskNew = new TimerTask() {
                @Override
                public void run() {
                    synchronized (desiredPiece){
                        desiredPiece.clear(pieceId);
                        LogHelper.getLogger().debug("The piece is being reset! :"+ pieceId);
                    }
                }
            };

            Timer t = new Timer();
            t.schedule(taskNew, timeoutInMS);// triggers the timer task defined above
            return pieceId;
        }
        return -1;
    }
}
