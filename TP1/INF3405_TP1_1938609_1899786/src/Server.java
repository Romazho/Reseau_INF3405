import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
	private static ServerSocket listener;
	private static Map<String, String> nameAndPasswordMap;
	
	//source: https://www.techiedelight.com/validate-ip-address-java/
	private static final String IPv4_REGEX =
			"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);
	
	public static void main(String[] args) throws Exception {
		int clientCounter = 0;
		nameAndPasswordMap = new HashMap<String, String>();
		
		readUsers();
				
		System.out.println("Séléctionner votre adresse IP:");
		
		String serverAddress = "127.0.0.1";
		Scanner S = new Scanner(System.in);
		serverAddress = S.next();
		
		while (!isValidInet4Address(serverAddress)) {
			System.out.println("Vous avez sélectionné une mauvaise adresse IP, veuillez réessayer:");
			serverAddress = S.next();
		}

		int port = 5000;	
		System.out.println("Séléctionner votre port:");
		port = S.nextInt();

		//vérification de la cohérence du port
		while(port < 5000 || port > 5050) {
			System.out.println("Veuillez entrer un port entre 5000 et 5050:");
			port = S.nextInt();
		}

		S.close();
		
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		listener.bind(new InetSocketAddress(serverIP, port));
		System.out.format("Server running on %s:%d%n", serverAddress, port);
		
		try {
			while(true) {
				new ClientHandler(listener.accept(), clientCounter++).start();
			}
		} finally {
			listener.close();
		}
	}
	
	public static boolean isValidInet4Address(String ip) {
		if (ip == null) {
			return false;
		}

		Matcher matcher = IPv4_PATTERN.matcher(ip);

		return matcher.matches();
	}
	
	private static void readUsers() {
		try {
			File fileDesc = new File("./src/users.txt");
			Scanner reader = new Scanner(fileDesc);
			
			while(reader.hasNextLine()) {
				String line = reader.nextLine();
				
				String[] nameAndPassword = line.split(" ");
				
				nameAndPasswordMap.put(nameAndPassword[0], nameAndPassword[1]);
			}
			
			reader.close();
			
		} catch(FileNotFoundException error) {
			System.out.println(error);
			error.printStackTrace();
		}
		
		// REMOVE WHEN DONE
//		for(Map.Entry<String,String> entry : nameAndPasswordMap.entrySet()) {
//			System.out.println(entry.getKey());
//			System.out.println(entry.getValue());
//		}
	}
	
	private static class ClientHandler extends Thread {
		private Socket sock;
		private int clientNumber;
		private DataOutputStream out;
		private DataInputStream in;
		
		public ClientHandler(Socket socket, int clientNumber) {
			this.sock = socket;
			this.clientNumber = clientNumber;
			try {
				out = new DataOutputStream(sock.getOutputStream());
				in = new DataInputStream(sock.getInputStream());
			}catch(IOException e) {
				System.out.println(e);
			}
			System.out.println("New connection with client# " + clientNumber + " at " + socket);
		}
		
		public void run() {
			try {
				String name = in.readUTF();
				String password = in.readUTF();
				validateUser(name, password);
				out.writeUTF("Hello from server - you are client# " + clientNumber);
				
			} catch (IOException e) {
				System.out.println("Error handling client# " + clientNumber + ": " + e);
			} finally {
				try {
					String userRequest = "";
					
					while(!userRequest.equals("exit")) {
						userRequest = in.readUTF();
						out.writeUTF(userRequest.toUpperCase());
					}
					sock.close();
				} catch (IOException e) {
					System.out.println(e);
				}
				System.out.println("Connection with client# " + clientNumber + " closed.");
			}
		}
		
		public void validateUser(String name, String password) {
			//System.out.println(password + " " + nameAndPasswordMap.get(name));
			if((nameAndPasswordMap.get(name) != null) && (nameAndPasswordMap.get(name).equals(password))) {
				//on vérifie le password
				System.out.println("Correct credentials for " + name);
			}
			else {
				//on ajoute le user
				System.out.println("user doesn't exist");

			}
		}
	}
}
