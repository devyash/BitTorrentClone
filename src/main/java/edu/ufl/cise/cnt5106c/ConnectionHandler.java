/**
 * Created by Jiya on 4/3/17.
 */

package edu.ufl.cise.cnt5106c;


import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionHandler implements Runnable {

 class ThreadImplementer extends Thread {
  private boolean remoteIsChoked = true;
  public void run() {
   this.setName(getClass().getName() + "-" + remoteNeighborID + "-sending thread");
   while (true) {
    try {
     final ActualMessage message = queue.take();
     if (message == null) {
      continue;
     }
     if (remoteNeighborID != PEERIDUNSET) {
      if (message.msgType == CHOKE) {
       if (!remoteIsChoked) {
        remoteIsChoked = true;
        sendInternal(message);
       }
      } else if (message.msgType == UNCHOKE) {
       if (remoteIsChoked) {
        remoteIsChoked = false;
        sendInternal(message);
       }
      } else
       sendInternal(message);
     } else {
      LogHelper.getLogger().debug("cannot send message of type " + message.msgType + " because the remote peer has not handshaked yet.");
     }
    } catch (IOException ex) {
     LogHelper.getLogger().warning(ex);
    } catch (Exception ex) {}
   }
  }
 }

 class ReqTask extends TimerTask {

  private final ActualMessage _request;
  private final FileManager _fileMgr;
  private final OutgoingMessage _out;
  private final int _remotePeerId;
  private final ActualMessage _message;

  ReqTask(ActualMessage request, FileManager fileMgr, OutgoingMessage out, ActualMessage message, int remotePeerId) {
   super();
   _request = request;
   _fileMgr = fileMgr;
   _out = out;
   _remotePeerId = remotePeerId;
   _message = message;
  }

  public void run() {
   if (_fileMgr.hasPart(_request.getPieceIndex())) {
    LogHelper.getLogger().debug("Not rerequesting piece " + _request.getPieceIndex() + " to peer " + _remotePeerId);
   } else {
    LogHelper.getLogger().debug("Rerequesting piece " + _request.getPieceIndex() + " to peer " + _remotePeerId);
    try {
     _out.writeObject(_message);
    } catch (IOException e) {
     e.printStackTrace();
    }

   }
  }
 }

 //constants to be used
 private final static int CHOKE = 0;
 private final static int UNCHOKE = 1;
 private final static int REQUEST = 6;
 private static final int PEERIDUNSET = -1;

 private final int myLocalID;
 private final Socket socket;
 private final OutgoingMessage out;
 private final FileManager fileMgr;
 private final PeerManager neighborMgr;
 private final boolean isConnectingNeighbor;
 private final int expectedRemoteNeighborID;
 private volatile int remoteNeighborID;
 private final BlockingQueue < ActualMessage > queue = new LinkedBlockingQueue < > ();

 public ConnectionHandler(int myLocalID, boolean isConnectingNeighbor, int expectedRemoteNeighborID, Socket socket, FileManager fileMgr, PeerManager neighborMgr) throws IOException {
  this.socket = socket;
  this.myLocalID = myLocalID;
  this.isConnectingNeighbor = isConnectingNeighbor; //false by default
  this.expectedRemoteNeighborID = expectedRemoteNeighborID; //-1
  this.fileMgr = fileMgr;
  this.neighborMgr = neighborMgr;
  out = new OutgoingMessage(socket.getOutputStream());
  remoteNeighborID = PEERIDUNSET;
 }

 public int getRemoteNeighborId() {
  return remoteNeighborID;
 }

 public void send(final ActualMessage message) {
  queue.add(message);
 }

 private synchronized void sendInternal(ActualMessage message) throws IOException {
  if (message != null) {
   out.writeObject(message);
   if (message.msgType == REQUEST) {
    Timer t = new Timer();
    TimerTask taskNew = new ReqTask((ActualMessage) message, fileMgr, out, message, remoteNeighborID);
    t.schedule(taskNew, neighborMgr.pProcess.UnchokingInterval * 1000 * 2);
   }
  }
 }

 @Override
 public void run() {
  ThreadImplementer t = new ThreadImplementer();
  t.start();
  try {
   final IncomingHandshakeBehaviour in = new IncomingHandshakeBehaviour(socket.getInputStream());
   // Send handshake
   out.writeObject(new HandShakeMessage(myLocalID));
   // Receive and check handshake
   HandShakeMessage rcvdHandshake = (HandShakeMessage) in .readObject();
   remoteNeighborID = ByteBuffer.wrap(rcvdHandshake.peerId).getInt();
   Thread.currentThread().setName(getClass().getName() + "-" + remoteNeighborID);
   final EventLogger eventLogger = new EventLogger(myLocalID, LogHelper.getLogger());
   final MessageHandler msgHandler = new MessageHandler(remoteNeighborID, fileMgr, neighborMgr, eventLogger);
   if (isConnectingNeighbor && (remoteNeighborID != expectedRemoteNeighborID)) {
    throw new Exception("Remote peer id " + remoteNeighborID + " does not match with the expected id: " + expectedRemoteNeighborID);
   }
   // Handshake successful
   eventLogger.ConnectWithPeer(remoteNeighborID, isConnectingNeighbor);

   sendInternal(msgHandler.process(rcvdHandshake));
   while (true) {
    try {
     sendInternal(msgHandler.process((ActualMessage) in .readObject()));
    } catch (Exception ex) {
     LogHelper.getLogger().warning(ex);
     break;
    }
   }
  } catch (Exception ex) {
   LogHelper.getLogger().warning(ex);
  } finally {
   try {
    socket.close();
   } catch (Exception e) {}
  }
  LogHelper.getLogger().warning(Thread.currentThread().getName() + " terminating, messages will no longer be accepted.");
 }
}
