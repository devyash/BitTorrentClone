package edu.ufl.cise.cnt5106c;

import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;

//new changes
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private BitSet _receivedParts;
    private final Collection < Listener > _listeners = new LinkedList < > ();
    private final double _dPartSize;
    private final int _bitsetSize;
    private final RequestPieceFromNeighbors _partsBeingReq;

    private final File _file;
    public final File _partsDir;
    private static final String partsLocation = "files/parts/";

    FileManager(int peerId, String fileName, int fileSize, int partSize, long unchokingInterval) {
        _dPartSize = partSize;
        _bitsetSize = (int) Math.ceil(fileSize / _dPartSize);
        LogHelper.getLogger().debug("File size set to " + fileSize + "\tPart size set to " + _dPartSize + "\tBitset size set to " + _bitsetSize);
        _receivedParts = new BitSet(_bitsetSize);
        _partsBeingReq = new RequestPieceFromNeighbors(_bitsetSize, unchokingInterval);

        //new changes
        _partsDir = new File("./peer_" + peerId + "/" + partsLocation + fileName);
        _partsDir.mkdirs();
        _file = new File(_partsDir.getParent() + "/../" + fileName);
        //_destination = new Destination(peerId, fileName);
    }

    
    /*
    The addPart function adds parts to the outputstream
    */
    public synchronized void addPart(int partIdx, byte[] part) {

        final boolean isNewPiece = !_receivedParts.get(partIdx);
        _receivedParts.set(partIdx);

        if (isNewPiece) {

            File currDir = _partsDir;

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
            mergeFile(_receivedParts.cardinality());
            //_destination.mergeFile(_receivedParts.cardinality());
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
        return (BitSet) _receivedParts.clone();
    }

    synchronized public boolean hasPart(int pieceIndex) {
        return _receivedParts.get(pieceIndex);
    }

    /**
     * Set all parts as received.
     */
    public synchronized void setAllParts() {
        for (int i = 0; i < _bitsetSize; i++) {
            _receivedParts.set(i, true);
        }
        LogHelper.getLogger().debug("Received parts set to: " + _receivedParts.toString());
    }

    public synchronized int getNumberOfReceivedParts() {
        return _receivedParts.cardinality();
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
            if (!_receivedParts.get(i)) {
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
