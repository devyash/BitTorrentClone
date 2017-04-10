package edu.ufl.cise.cnt5106c;

import java.util.*;
import java.io.*;

public class FileOrganizer {

    private BitSet received_Parts;
    private final Collection < Listener > _listeners = new LinkedList < > ();
    //private Destination _destination;
    private final double _dPartSize;
    private final int _bitsetSize;
    private final RequestPieceFromNeighbors _partsBeingReq;

    //new changes
    private final File _file;
    public final File _partsDir;
    private static final String partsLocation = "files/parts/";

    FileOrganizer(int peerId, String fileName, int fileSize, int partSize, long unchokingInterval) {
        _dPartSize = partSize;
        _bitsetSize = (int) Math.ceil(fileSize / _dPartSize);
        LogHelper.getLogger().debug("File size set to " + fileSize + "\tPart size set to " + _dPartSize + "\tBitset size set to " + _bitsetSize);
        received_Parts = new BitSet(_bitsetSize);
        _partsBeingReq = new RequestPieceFromNeighbors(_bitsetSize, unchokingInterval);

        //new changes
        _partsDir = new File("./peer_" + peerId + "/" + partsLocation + fileName);
        _partsDir.mkdirs();
        _file = new File(_partsDir.getParent() + "/../" + fileName);
        //_destination = new Destination(peerId, fileName);
    }

    public synchronized void addPart(int partIdx, byte[] part) {

        // TODO: write part on file, at the specified directroy
        final boolean isNewPiece = !received_Parts.get(partIdx);
        received_Parts.set(partIdx);

        if (isNewPiece) {

            //new changes
            File currDir = _partsDir;
            //File currDir = _destination._partsDir;

            FileOutputStream fos;
            File outputfile = new File(currDir.getAbsolutePath() + "/" + partIdx);
            try {
                fos = new FileOutputStream(outputfile);
                fos.write(part);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                LogHelper.getLogger().warning(e);
            }

            for (Listener listener: _listeners) {
                listener.pieceArrived(partIdx);
            }
        }
        if (isFileCompleted()) {

            //new changes
            mergeFile(received_Parts.cardinality());
            //_destination.mergeFile(received_Parts.cardinality());
            for (Listener listener: _listeners) {
                listener.fileCompleted();
            }
        }
    }

    public synchronized int getPartToRequest(BitSet availableParts) {
        availableParts.andNot(getReceivedParts());
        return _partsBeingReq.getRequestedPiece(availableParts);
    }

    public synchronized BitSet getReceivedParts() {
        return (BitSet) received_Parts.clone();
    }

    synchronized public boolean hasPart(int pieceIndex) {
        return received_Parts.get(pieceIndex);
    }

    /**
     * Set all parts as received.
     */
    public synchronized void setAllParts() {
        for (int i = 0; i < _bitsetSize; i++) {
            received_Parts.set(i, true);
        }
        LogHelper.getLogger().debug("Received parts set to: " + received_Parts.toString());
    }

    public synchronized int getNumberOfReceivedParts() {
        return received_Parts.cardinality();
    }

    public byte[] getPiece(int partId) {

        //new changes
        byte[] piece = getPartAsByteArray(partId);
        //byte[] piece = _destination.getPartAsByteArray(partId);
        return piece;
    }

    public void registerListener(Listener listener) {
        _listeners.add(listener);
    }

    public void splitFile() {

        //new changes
        splitFile((int) _dPartSize);
        //_destination.splitFile((int) _dPartSize);
    }

    public byte[][] getAllPieces() {

        //new changes
        return getAllPartsAsByteArrays();
        //return _destination.getAllPartsAsByteArrays();
    }

    public int getBitmapSize() {
        return _bitsetSize;
    }

    private boolean isFileCompleted() {
        for (int i = 0; i < _bitsetSize; i++) {
            if (!received_Parts.get(i)) {
                return false;
            }
        }
        return true;
    }

    public byte[][] getAllPartsAsByteArrays() {
        File[] listOfFiles = _partsDir.listFiles();
        byte[][] byteArray = new byte[listOfFiles.length][getPartAsByteArray(1).length];
        for (File file: listOfFiles) {
            int len = (int) file.length();
            byte[] bFile = new byte[len];
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                int currBytes = fis.read(bFile, 0, len);
                fis.close();
                if (currBytes == bFile.length && currBytes == len)
                    byteArray[Integer.parseInt(file.getName())] = bFile;
                break;
            } catch (Exception e) {
                LogHelper.getLogger().warning(e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex) {}
                }
            }
            byteArray[Integer.parseInt(file.getName())] = null;
        }
        return byteArray;
    }

    public byte[] getPartAsByteArray(int pId) {
        File file = new File(_partsDir.getAbsolutePath() + "/" + pId);
        int len = (int) file.length();
        byte[] bFile = new byte[len];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int currBytes = fis.read(bFile, 0, len);
            fis.close();
            if (currBytes == bFile.length && currBytes == len)
                return bFile;
        } catch (Exception e) {
            LogHelper.getLogger().warning(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {}
            }
        }
        return null;
    }

    /*
    splitFile splits the incoming file into parts depending on
    the number of parts given in the argument
    */
    public void splitFile(int partSize) {
        FileInputStream inputStream;
        String newFileName;
        FileOutputStream filePart;
        int fileSize = (int) _file.length();
        int nChunks = 0, read = 0, readLength = partSize;
        byte[] byteChunkPart;
        try {
            inputStream = new FileInputStream(_file);
            while (fileSize > 0) {
                if (fileSize <= 5) {
                    readLength = fileSize;
                }
                byteChunkPart = new byte[readLength];
                read = inputStream.read(byteChunkPart, 0, readLength);
                fileSize -= read;
                assert(read == byteChunkPart.length);
                nChunks++;
                newFileName = _file.getParent() + "/parts/" +
                        _file.getName() + "/" + Integer.toString(nChunks - 1);
                filePart = new FileOutputStream(new File(newFileName));
                filePart.write(byteChunkPart);
                filePart.flush();
                filePart.close();
                byteChunkPart = null;
                filePart = null;
            }
            inputStream.close();
        } catch (IOException e) {
            LogHelper.getLogger().warning(e);
        }
    }

    /*
    MergeFile function merges the files which are being split
    */
    public void mergeFile(int numParts) {
        File ofile = _file;
        FileOutputStream fos;
        FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        List < File > list = new ArrayList < > ();
        for (int i = 0; i < numParts; i++) {
            list.add(new File(_partsDir.getPath() + "/" + i));
        }
        try {
            fos = new FileOutputStream(ofile);
            for (File file: list) {
                fis = new FileInputStream(file);
                fileBytes = new byte[(int) file.length()];
                bytesRead = fis.read(fileBytes, 0, (int) file.length());
                assert(bytesRead == fileBytes.length);
                assert(bytesRead == (int) file.length());
                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;
                fis.close();
                fis = null;
            }
            fos.close();
            fos = null;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
