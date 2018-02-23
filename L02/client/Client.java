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
		//extract args
		String multicastHostname = args[0];
		int multicastPort = Integer.parseInt(args[1]);	
		
		//create connection
		Connection connection;
		try {
			connection = new Connection(multicastHostname, multicastPort);
		} catch (SocketException | UnknownHostException e) {
			if(e instanceof SocketException)
				System.out.println("Error creating socket");
			if(e instanceof UnknownHostException)
				System.out.println("Unknown hostname");
			return;
		}
		
		//create request string
		String operation = args[2];
		String operationArgs = args[3];
		System.out.println("Operation: " + operation);
		System.out.println("Arguments: " + operationArgs);
		if(!checkMessage(operation, operationArgs))
			return;	
		
		//get multicast message
		try {
			String multicastMessage = connection.receiveMulticastPacket();
			System.out.println("Received multicast message: " + multicastMessage);
		} catch (IOException e) {
			System.out.println("Error sending request");
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
		connection.close();
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
}
