package utility;

import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

import utility.Side;


public class Connection {
	private MulticastSocket multicastSocket;
	private InetAddress multicastAddress;
	private int multicastPort;
	private DatagramSocket socket;
	private InetAddress destinationAddress = null;
	private int destinationPort = -1;
	private MulticastMessageSender multicastSender = null;
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

	public Connection(String multicastHostname, int multicastPort) throws IOException {//Constructor called by client
		//check if multicast address is valid
		this.multicastAddress = InetAddress.getByName(multicastHostname);
		if(!this.multicastAddress.isMulticastAddress()) {
			System.out.println("Error, this is not a multicast address");
			throw new IOException();
		}
		
		this.multicastPort = multicastPort;

		//create mulitcast socket
		this.multicastSocket = new MulticastSocket(this.multicastPort);
		this.multicastSocket.setTimeToLive(1);

		//join multicast address
		this.multicastSocket.joinGroup(this.multicastAddress);	

		this.socket = new DatagramSocket();
		this.type = Side.CLIENT;
	}
	
	public Connection(String multicastHostname, int multicastPort, int servicePort) throws IOException {//Constructor called by server
		//check if multicast address is valid
		this.multicastAddress = InetAddress.getByName(multicastHostname);
		if(!this.multicastAddress.isMulticastAddress()) {
			System.out.println("Error, this is not a multicast address");
			throw new IOException();
		}
		
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
	
	public void sendMulticastPacket(String message) throws IOException {
		// packet
		byte[] sbuf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, this.multicastAddress, this.multicastPort);
		
		// send request
		this.multicastSocket.send(packet);
		System.out.print("Server sent packet to multicast with msg: " + message);
	}
	
	public String receiveMulticastPacket() throws IOException {
		// multicast message format "multicast: <mcast_addr> <mcast_port>: <srvc_addr> <srvc_port>"
		final int maxPacketSize = 100;
		
		// packet
		byte[] rbuf = new byte[maxPacketSize];
		DatagramPacket packet = new DatagramPacket(rbuf, maxPacketSize);

		// receive multicast packet
		this.multicastSocket.receive(packet);

		//extract response
		String content = new String(packet.getData(), 0, packet.getLength());	
		return content;
	}
	
	public void setMulticastSender(String message) {
		this.multicastSender = new MulticastMessageSender(message);
	}
	
	public void setConnectionDestination(String hostname, int destinationPort) throws UnknownHostException {
		this.destinationAddress = InetAddress.getByName(hostname);
		this.destinationPort = destinationPort;
	}
	
	public InetAddress getDestAddress() {
		return this.destinationAddress;
	}
	
	public int getDestPort() {
		return this.destinationPort;
	}
	
	public void leaveMulticastGroup() throws IOException {
		this.multicastSocket.leaveGroup(this.multicastAddress);
	}
	public void close() throws IOException {
		if(this.type == Side.CLIENT)
			this.multicastSocket.leaveGroup(this.multicastAddress);
		
		this.socket.close();
	}
	
	
	public class MulticastMessageSender {
	    String message;
	    Timer timer;

	    public MulticastMessageSender(String message) {
	        this.message = message;
	        this.timer = new Timer();
	        this.timer.schedule(new MulticastTask(), 0, 1*1000);   
	    }

	    class MulticastTask extends TimerTask {
	        public void run() {
	        	try {
					sendMulticastPacket(message);
				} catch (IOException e) {
					System.out.println("Error sending multicast packet");
				}
	        }
	    }
	}


	public boolean verifyMulticastInfo(String mcastHostname, int mcastPort) {
		return mcastHostname == this.multicastAddress.getHostAddress() && mcastPort == this.multicastPort;
	}

	public String getMulticastHostname() {
		return this.multicastAddress.getHostAddress();
	}

	public int getMulticastPort() {
		return this.multicastPort;
	}

	public String getSelfHostname() {
		return this.socket.getLocalAddress().getHostAddress();
	}

	public int getSelfPort() {
		return this.socket.getLocalPort();
	}
}
