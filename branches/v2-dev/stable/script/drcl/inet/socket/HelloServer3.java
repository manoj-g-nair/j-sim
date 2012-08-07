
import java.net.*;
import java.io.*;
import java.util.Vector;
import drcl.comp.*;
import drcl.util.queue.FIFOQueue;
import drcl.inet.socket.SocketMaster;
import drcl.inet.socket.InetSocket;

// testing nonblocking accept
public class HelloServer3 extends drcl.inet.socket.SocketApplication
    implements ActiveComponent
{
  public static int END = 3;

  boolean stop = false;
  long localAddress;
  int localPort = 0;
  Port sessionPort = addPort(".session"); //where new session is processed
  Port stopPort = addPort(".stop"); //to stop a session

  public HelloServer3()
  { super(); }

  public HelloServer3(String id_)
  { super(id_); }

  public void reset()
  {
    super.reset();
    stop = false;
  }

  public String info()
  {
    return "local port = " + localPort
      + "\nstopped: " + stop + "\n" + socketMaster.info();
  }

  public void setup(int localAddress_, int localPort_)
  {
    localAddress = localAddress_;
    localPort = localPort_;
  }

  InetSocket serverSocket;
  Vector vSockets = new Vector();

  protected void _start()
  {
    try {
      serverSocket = socketMaster.newSocket();
      socketMaster.bind(serverSocket, localAddress, localPort);
      //socketMaster.listen(serverSocket, 1);

      System.out.println("Server starts at port " + localPort);

      socketMaster.aAccept(serverSocket, this);
    }
    catch (Exception e_) {
      e_.printStackTrace();
    }
  }

  public void acceptFinished(InetSocket serverSocket_, InetSocket new_)
  {
    try {
      if (new_ != null) fork(sessionPort, new_, 0.0);
      if (isMultiSessionEnabled() && !stop)
        socketMaster.aAccept(serverSocket, this);
      else
        System.out.println("Server stops.");
    }
    catch (Exception e_) {
      e_.printStackTrace();
    }
  }

  public void closeFinished(InetSocket socket_)
  {
    synchronized (vSockets) {
      vSockets.removeElement(socket_);
    }
    System.out.println("End with client: "
            + socket_.getRemoteAddress()
            + "/" + socket_.getRemotePort());
  }
  
  protected void _stop()
  {
    stop = true;
    try {
      socketMaster.close(serverSocket);
    }
    catch (Exception e_) {
      e_.printStackTrace();
    }
  }

  protected void process(Object data_, Port inPort_)
  {
    if (inPort_ == stopPort) {
      if (data_ == null) {
        // stop all sessions
        FIFOQueue qTasks_ = new FIFOQueue();
        synchronized (vSockets) {
          for (int i=0; i<vSockets.size(); i++)
            qTasks_.enqueue(vSockets.elementAt(i));
        }

        while (!qTasks_.isEmpty())
          fork(stopPort, qTasks_.dequeue(), 0.0);
        return;
      }
      else {
        try {
          socketMaster.close((InetSocket)data_);
        } 
        catch (Exception e_) {
          e_.printStackTrace();
        }
      }
    }
    else if (inPort_ == sessionPort) {
      try {
        InetSocket socket_ = (InetSocket)data_;
        synchronized (vSockets) {
          vSockets.addElement(socket_);
        }
        BufferedReader is_ = new BufferedReader(
          new InputStreamReader(socket_.getInputStream()));
        OutputStream os_ = socket_.getOutputStream();
        for (int i=0; i<END; i++) {
          os_.write(("Hello" + i + "!\n").getBytes());
          String line_ = is_.readLine();
          System.out.println(line_ + " from "
                  + socket_.getRemoteAddress() + "/"
                  + socket_.getRemotePort());
          sleepFor(1.0); // second
        }
        socketMaster.aClose(socket_, this);
      }
      catch (Exception e_) {
        e_.printStackTrace();
      }
    }
    else 
      super.process(data_, inPort_);
  }

  public void stopAllSessions()
  {
    fork(stopPort, null, 0.0);
  }
}
