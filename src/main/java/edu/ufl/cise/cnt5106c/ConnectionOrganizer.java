/*
* The Connection Handler Class has 2 inner classes ImplementingThread and RequestTask
* This Class manages the connection of a peer
* */

package edu.ufl.cise.cnt5106c;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Connection Organizer returns a queue of connections.
 * It runs all the task one by one from the queue
 *
 */


public class ConnectionOrganizer implements Runnable {

 class ImplementingThread extends Thread {
  private boolean is_Remote_Choked_Flag = true;

  public void run() {
   this.setName(getClass().getName() + ":" + remote_Id + ": This is Sending thread");
   while (true) {
    try {
     final ActualMessage message = queue.take();
     if (message == null) {
      continue;
     }
     if (remote_Id != PEERIDUNSET) {
      if (message.type == CHOKE) {
       if (!is_Remote_Choked_Flag) {
        is_Remote_Choked_Flag = true;
        sendInternal(message);
       }
      } else if (message.type == UNCHOKE) {
       if (is_Remote_Choked_Flag) {
        is_Remote_Choked_Flag = false;
        sendInternal(message);
       }
      } else
       sendInternal(message);
     } else {
      LogHelper.getLogger().debug("The message handler was not able to send " + message.type + " as the remote peer has not been Hand shaked");
     }
    } catch (IOException ex) {
     LogHelper.getLogger().warning(ex);
    } catch (Exception ex) {
    }
   }
  }
 }

 class RequestTask extends TimerTask {

  private final ActualMessage request;
  private final FileOrganizer file_Manager;
  private final OutgoingMessage out;
  private final int remote_Id;
  private final ActualMessage message;

  RequestTask(ActualMessage request, FileOrganizer file_Manager, OutgoingMessage out, ActualMessage message, int remote_Id) {
   super();
   this.request = request;
   this.file_Manager = file_Manager;
   this.out = out;
   this.remote_Id = remote_Id;
   this.message = message;
  }

  public void run() {
   if (file_Manager.hasPart(request.getPieceIndex())) {
    LogHelper.getLogger().debug("Not Re-requesting piece " + request.getPieceIndex() + " to peer " + remote_Id);
   } else {
    LogHelper.getLogger().debug("Re-requesting piece " + request.getPieceIndex() + " to peer " + remote_Id);
    try {
     out.writeObject(message);
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

 private final int my_Id;
 private final Socket socket;
 private final OutgoingMessage out;
 private final FileOrganizer file_Manager;
 private final PeerOrganizer neighbor_Manager;
 private final boolean is_Conn_Neighbor_Flag;
 private final int expected_Neighbor_Id;
 private volatile int remote_Id;
 private final BlockingQueue<ActualMessage> queue = new LinkedBlockingQueue<>();

 public ConnectionOrganizer(int my_Id, boolean is_Conn_Neighbor_Flag, int expected_Neighbor_Id, Socket socket, FileOrganizer file_Manager, PeerOrganizer neighbor_Manager) throws IOException {
  this.socket = socket;
  this.my_Id = my_Id;
  this.is_Conn_Neighbor_Flag = is_Conn_Neighbor_Flag; //false by default
  this.expected_Neighbor_Id = expected_Neighbor_Id; //-1
  this.file_Manager = file_Manager;
  this.neighbor_Manager = neighbor_Manager;
  out = new OutgoingMessage(socket.getOutputStream());
  remote_Id = PEERIDUNSET;
 }

 public int getRemoteNeighborId() {
  return remote_Id;
 }

 public void send(final ActualMessage message) {
  queue.add(message);
 }

 private synchronized void sendInternal(ActualMessage message) throws IOException {
  if (message != null) {
   out.writeObject(message);
   if (message.type == REQUEST) {
    Timer t = new Timer();
    TimerTask taskNew = new RequestTask(message, file_Manager, out, message, remote_Id);
    t.schedule(taskNew, neighbor_Manager.pProcess.UnchokingInterval * 1000 * 2);
   }
  }
 }


 public void run() {
  ImplementingThread t = new ImplementingThread();
  t.start();
  try {
   final IncomingHandshakeBehaviour in = new IncomingHandshakeBehaviour(socket.getInputStream());
   // Send handshake
   out.writeObject(new HandShakeMessage(my_Id));
   // Receive and check handshake
   HandShakeMessage received_Handshake = (HandShakeMessage) in.readObject();
   remote_Id = ByteBuffer.wrap(received_Handshake.peerId).getInt();
   Thread.currentThread().setName(getClass().getName() + "-" + remote_Id);
   final EventLogger eventLogger = new EventLogger(my_Id, LogHelper.getLogger());
   final MessageHandler msgHandler = new MessageHandler(remote_Id, file_Manager, neighbor_Manager, eventLogger);
   if (is_Conn_Neighbor_Flag && (remote_Id != expected_Neighbor_Id)) {
    throw new Exception("Remote peer id " + remote_Id + " is different from the expected id: " + expected_Neighbor_Id);
   }
   // Handshake successful
   eventLogger.ConnectWithPeer(remote_Id, is_Conn_Neighbor_Flag);

   sendInternal(msgHandler.process(received_Handshake));
   while (true) {
    try {
     sendInternal(msgHandler.process((ActualMessage) in.readObject()));
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
   } catch (Exception e) {
   }
  }
  LogHelper.getLogger().warning(Thread.currentThread().getName() + "Terminated!");
 }
}
