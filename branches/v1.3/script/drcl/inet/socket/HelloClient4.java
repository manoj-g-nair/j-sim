
import java.net.*;
import java.io.*;
import drcl.comp.*;
import drcl.inet.socket.SocketMaster;
import drcl.inet.socket.InetSocket;

// same as HelloClient3 except now is RestartableComponent
public class HelloClient4 extends drcl.inet.socket.SocketApplication
	implements RestartableComponent
{
	int remoteport;
	long localAddress, remoteAddress;

	public HelloClient4()
	{ super(); }

	public HelloClient4(String id_)
	{ super(id_); }

	public String info()
	{ return "peer: " + remoteAddress + "/" + remoteport
			+ "\n" + super.info(); }

	public void setup(long localAddr_, long remoteAddr_, int remotePort_)
	{
		localAddress = localAddr_;
		remoteAddress = remoteAddr_;
		remoteport = remotePort_;
	}

	protected void _start()
	{
		try {
			InetSocket s_ = socketMaster.newSocket();
			socketMaster.bind(s_, localAddress, 0);
			socketMaster.aConnect(s_, remoteAddress, remoteport, this);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	public void connectFinished(InetSocket socket_)
	{
		try {
			BufferedReader is_ = new BufferedReader(
				new InputStreamReader(socket_.getInputStream()));
			OutputStream os_ = socket_.getOutputStream();

			for (int i=0; i<HelloServer3.END; i++) {
				String line_ = is_.readLine();
				System.out.println(line_ + " from " + socket_.getRemoteAddress()
								+ "/" + socket_.getRemotePort());

				// uncomment the following line blocks the thread at the server
				//   from receiving the last line of messages, used to test
				//   cancelling blocked receiving
				//if (i < HelloServer3.END-1)
				os_.write((getTime() + ": Hello Back" + i + "!\n").getBytes());
			}
			socketMaster.aClose(socket_, this);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	public void closeFinished(InetSocket socket_)
	{
		//System.out.println("End with server: " + socket_.getRemoteAddress()
		//				+ "/" + socket_.getRemotePort());
		System.out.println("End with server: " + socket_);
	}
}
