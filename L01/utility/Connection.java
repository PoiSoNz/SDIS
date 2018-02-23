package utility;

import java.io.IOException;
import java.net.*;
import utility.Side;


public class Connection {
	private DatagramSocket socket;
	private InetAddress destinationAddress;
	private int destinationPort;
	private Side type;
	
	public Connection(String hostname, int destPortNumber) throws SocketException, UnknownHostException {//Constructor called by client
		this.socket = new DatagramSocket();
	
		/*this.destinationAddress = InetAddress.getLocalHost();
		System.out.println("Local hostname: "+this.destinationAddress.getHostName());*/
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
		
		//extract response
		String response = new String(packet.getData(), 0, packet.getLength());
		return response;
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
	
	public InetAddress getDestAddress() {
		return this.destinationAddress;
	}
	
	public int getDestPort() {
		return this.destinationPort;
	}
	
	public void close() {
		this.socket.close();
	}
}
