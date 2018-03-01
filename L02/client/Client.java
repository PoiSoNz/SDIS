package client;

import java.io.IOException;
import java.net.*;
import utility.Connection;

public class Client {
	
	public static void main(String[] args) {
		if(args.length != 4) {
			System.out.println("Wrong number of arguments");
			return;
		}
		
		//extract and verify args
		String multicastHostname = args[0];
		int multicastPort = Integer.parseInt(args[1]);	
		String operation = args[2];
		String operationArgs = args[3];
		System.out.println("Operation: " + operation);
		System.out.println("Arguments: " + operationArgs);
		if(!checkMessage(operation, operationArgs))
			return;
		
		//create connection
		Connection connection = null;
		String multicastMessage = "";
		try {
			connection = new Connection(multicastHostname, multicastPort);
		} catch (IOException e1) {
			System.out.println("Error creating sockets");
			return;
		}
		
		//receive multicast packet
		try {
			multicastMessage = connection.receiveMulticastPacket();
			System.out.println("Received multicast message: " + multicastMessage);
		} catch (IOException e) {
			System.out.println("Error receiving multicast packet");
			return;
		}	
		
		//extract service info
		try {
			extractServerInfo(multicastMessage, connection);
		} catch (UnknownHostException e1) {
			System.out.println("Error establishing connection with server from multicast");
			return;
		}
		
		//send request for server
		String message = operation + " " + operationArgs;
		try {
			connection.sendRequest(message);
			System.out.println("Request sent");
		} catch (IOException e) {
			System.out.println("Error sending request");
			return;
		}
		
		//get server response
		try {
			String response = connection.receiveRequest();
			System.out.println("Server response: " + response);
		} catch (Exception e) {
			System.out.println("Too much time without receiving server response");
		}
		
		//end connection
		try {
			connection.close();
		} catch (IOException e) {
			System.out.println("Error ending connection");
		}
	}
	
	public static boolean checkMessage(String operation, String operationArgs) {
		String operationPattern = "^(LOOKUP)|(REGISTER)$";
		if(!operation.matches(operationPattern)) {
			System.out.println("Operation not recognized");
			return false;
		}
		
		String argumentsPattern = "";
		switch(operation) {
			case "REGISTER":
				argumentsPattern = "^[A-Z_0-9]{2}-[A-Z_0-9]{2}-[A-Z_0-9]{2} [a-zA-Z ]{1,256}$";
				break;
			case "LOOKUP":
				argumentsPattern = "^[A-Z_0-9]{2}-[A-Z_0-9]{2}-[A-Z_0-9]{2}$";
		}
		
		if(!operationArgs.matches(argumentsPattern)) {
			System.out.println("Operation arguments are in wrong format");
			return false;
		}
		return true;
	}
	
	public static void extractServerInfo(String multicastMessage, Connection con) throws UnknownHostException {
		//Check multicast message format
		String multicastPattern = "^multicast: ([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}) ([0-9]{1,5}): ([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}) ([0-9]{1,5})$";
		if(!multicastMessage.matches(multicastPattern)) {
			System.out.println("Multicast message wrong format");
			throw new UnknownHostException();
		}
		String[] divisions = multicastMessage.split(" ");
		
		//extract arguments from message
		String multicastHostname = divisions[1];
		int multicastPort = Integer.parseInt(divisions[2].substring(0, divisions[2].length() - 1));
		String serverHostname = divisions[3];
		int serverPort = Integer.parseInt(divisions[4]); 
		
		//verify multicast information
		if(!con.verifyMulticastInfo(multicastHostname, multicastPort)) {
			System.out.println("Multicast message info doesnt match multicast channel");
			throw new UnknownHostException();
		}
		
		//set connection properly
		con.setConnectionDestination(serverHostname, serverPort);
	}
}
