package utility;

import java.io.IOException;
import java.net.*;
import utility.Side;


public class Connection {
	private MulticastSocket mulitcastSocket;
	private InetAddress multicastAddress;
	private DatagramSocket socket;
	private InetAddress destinationAddress = null;
	private int destinationPort = -1;
	private Side type;
	
	/*public Connection(String hostname, int destPortNumber) throws SocketException, UnknownHostException {//Constructor called by client
		this.socket = new DatagramSocket();
	
		//this.destinationAddress = InetAddress.getLocalHost();
		//System.out.println("Local hostname: "+this.destinationAddress.getHostName());
		this.destinationAddress = InetAddress.getByName(hostname);
		this.destinationPort = destPortNumber;
		this.type = Side.CLIENT;
	}

	public Connection(int seflPortNumber) throws SocketException {//Constructor called by server
		this.socket = new DatagramSocket(seflPortNumber);
		//this.socket.setSoTimeout(5000);
		this.destinationAddress = null;
		this.destinationPort = -1;
		this.type = Side.SERVER;
	}*/

	public Connection(String mulitcastHostname, int multicastPort) {//Constructor called by client
		this.multicastAddress = InetAddress.getByName(mulitcastHostname);
		this.multicastPort = multicastPort;
		
		//create mulitcast socket
		this.multicastSocket = new MulticastSocket(this.multicastPort);
		this.multicastSocket.setTimeToLive(1);
		
		//join multicast address
		this.multicastSocket.joinGroup(this.multicastAddress);		
		
		this.socket = new DatagramSocket();
		this.type = Side.CLIENT;
	}
	
	public Connection(String mulitcastHostname, int multicastPort, int servicePort) {//Constructor called by server
		this.multicastAddress = InetAddress.getByName(multicastHostname);
		this.multicastPort = multicastPort;
		
		//create multicast socket
		this.multicastSocket = new MulticastSocket();
		this.multicastSocket.setTimeToLive(1);
		
		this.socket = new DatagramSocket(servicePort);
		this.type = Side.SERVER;
	}
	
	public String receiveRequest() throws IOException {
		// operation message format "<operation>[8] <plate number>[8] <owner name>[256]"
		final int maxRequestSize = 274;
		
		// packet
		byte[] rbuf = new byte[maxRequestSize];
		DatagramPacket packet = new DatagramPacket(rbuf, maxRequestSize);
		
		// receive request
		this.socket.receive(packet);
		
		if(this.type == Side.SERVER) { // extract client's address and port for later response
			this.destinationAddress = packet.getAddress();
			this.destinationPort = packet.getPort();
			System.out.println("Request received from client: ip - " + this.destinationAddress.getHostAddress() + "    port - " + this.destinationPort);
		}
		
		//extract content
		String content = new String(packet.getData(), 0, packet.getLength());
		return content;
	}

	public void sendRequest(String message) throws IOException {
		// packet
		byte[] sbuf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, this.destinationAddress, this.destinationPort);
		
		// send request
		this.socket.send(packet);
		
		//set socket timeout
		if(this.type == Side.CLIENT)
			this.socket.setSoTimeout(5000);
	}
	
	public void sendMulticastPacket(String message) {
		// packet
		byte[] sbuf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, this.multicastAddress, this.multicastPort);
		
		// send request
		this.multicastSocket.send(packet);
		System.out.println("Server sent packet to multicast with msg: " + message);

	}
	
	public String receiveMulticastPacket() {
		// multicast message format "multicast: <mcast_addr> <mcast_port>: <srvc_addr> <srvc_port>"
		final int maxPacketSize = 999;
		
		// packet
		byte[] rbuf = new byte[maxPacketSize];
		DatagramPacket packet = new DatagramPacket(rbuf, maxPacketSize);
		
		// receive multicast packet
		this.multicastSocket.receive(packet);
		
		//extract response
		String content = new String(packet.getData(), 0, packet.getLength());
		return content;
	}
	
	public InetAddress getDestAddress() {
		return this.destinationAddress;
	}
	
	public int getDestPort() {
		return this.destinationPort;
	}
	
	public void leaveMulticastGroup() {
		this.multicastSocket.leaveGroup(this.multicastAddress);
	}
	public void close() {
		this.socket.close();
	}
	
	
	public class MulticastMessageSender {
	    String message;
	    Timer timer;

	    public MulticastMessageSender(String message) {
	        this.message = message;
	        this.timer = new Timer();
	        this.timer.schedule(new MulticastTask(), 0, 1*1000);  //subsequent rate
	        
	    }

	    class MulticastTask extends TimerTask {
	        public void run() {
	        	sendMulticastPacket(message);
	        }
	    }
	}
}
