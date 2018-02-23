package server;

import java.io.IOException;
import java.net.*;

import utility.Connection;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class Server {
	
	static Hashtable <String, String> cars;
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Wrong number of arguments");
			return;
		}
		
		//extract args
		int port = Integer.parseInt(args[0]);
		System.out.print("Port for requests: " + port + "\n\n");
		
		//create connection and database
		Connection connection;
		try {
			connection = new Connection(port);
		} catch (SocketException e) {
			System.out.println("Couldn't create socket on this port");
			return;
		}	
		cars = new Hashtable<String, String>();
		
		//server cycle
		while(true) {
			//get client request
			String message;
			try {
				message = connection.receiveRequest();
			} catch (IOException e) {
				System.out.println("Error receiving request");
				continue;
			}
			System.out.println("Request: " + message);
			
			//execute request operation and prepare response for client
			String response = databaseOperation(message);
			System.out.println("RESPONSE: " + response + "\n");
			
			//send response for client
			try {
				connection.sendRequest(response);
			} catch (IOException e) {
				System.out.println("Error sending response");
				continue;
			}
		}
		//connection.close();
	}

	public static int register(String plateNumber, String ownerName) {
		if(cars.containsKey(plateNumber))
			return -1;
		
		cars.put(plateNumber, ownerName);
		return cars.size();
	}

	public static String lookup(String plateNumber) {
		String owner = cars.get(plateNumber);
		if(owner != null)
			return owner;
		else
			return "NOT_FOUND";
	}
	
	public static String databaseOperation(String message) {
		String messagePattern = "^(LOOKUP [A-Z_0-9]{2}-[A-Z_0-9]{2}-[A-Z_0-9]{2})|(REGISTER [A-Z_0-9]{2}-[A-Z_0-9]{2}-[A-Z_0-9]{2} [a-zA-Z ]{1,256})$";
		if(!message.matches(messagePattern)) { //check if message is in correct format
			return "-1";
		}
		
		//extract request arguments and execute operation
		String operation = message.substring(0, message.indexOf(" "));
		String arguments = message.substring(message.indexOf(" ") + 1);
		String response = "";
		switch(operation){
			case "LOOKUP": {
				String plateNumber = arguments;
				response = lookup(plateNumber);
				break;
			}
			case "REGISTER": {
				String plateNumber = arguments.substring(0, arguments.indexOf(" "));
				String ownerName = arguments.substring(arguments.indexOf(" ") + 1);
				response = Integer.toString(register(plateNumber, ownerName));
				break;
			}
		}
		return response;
	}
}
