package edu.ufl.cise.cnt5106c; /**
 * Created by Jiya on 4/1/17.
 */


import java.util.BitSet;
import java.util.Timer;
import java.util.TimerTask;

public class RequestPieceFromNeighbors {

    private final long timeoutInMS;
    private final BitSet desiredPiece;

    RequestPieceFromNeighbors(int noOfPiece, long unchokingInterval) {
        desiredPiece = new BitSet (noOfPiece);
        timeoutInMS = unchokingInterval; //while calling multiply by 2 for a roundtrip
    }

    synchronized int getRequestedPiece(BitSet availableParts) {
        availableParts.andNot(desiredPiece);
        if (!availableParts.isEmpty()) {
            if (availableParts.isEmpty())
                throw new RuntimeException ("The bitset is empty, cannot find a set element");
            String set = availableParts.toString();
            String[] indexes = set.substring(1, set.length()-1).split(",");
            int len = indexes.length-1;
            int randomIndex = (int) (Math.random() *  len);
            String ret = indexes[randomIndex].trim();
            final int pieceId =  Integer.parseInt(ret);
            desiredPiece.set(pieceId);

            /*reset the piece so that it can be requested again if not already downloaded in previous time frame from previous unchocked neighbor */
            TimerTask taskNew = new TimerTask() {
                @Override
                public void run() {
                    synchronized (desiredPiece){
                        desiredPiece.clear(pieceId);
                        LogHelper.getLogger().debug("Re-setting the piece: "+ pieceId);
                    }
                }
            };

            Timer t = new Timer();
            t.schedule(taskNew, timeoutInMS);
            return pieceId;
        }
        return -1;
    }
}
