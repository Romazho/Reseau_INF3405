import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

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
			String userRequest = "";
			try {
				String name = in.readUTF();
				boolean isExistantUser = isUsernameRegistered(name);
				if(!isExistantUser) {
					registerUsername(name);
					out.writeUTF("usernull");
				} else {
					out.writeUTF("ok");
				}
				String password = in.readUTF();
				if(!isExistantUser) {
					registerPassword(password);
					out.writeUTF("newuser");
					out.writeUTF("Hello from server - you are client# " + clientNumber);
				} else if(validateUser(name, password)) {
					out.writeUTF("Hello from server - you are client# " + clientNumber);	
				} else {
					out.writeUTF("wrongpassword");
					userRequest = "exit";
				}
				
			} catch (IOException e) {
				System.out.println("Error handling client# " + clientNumber + ": " + e);
			} finally {
				try {
					while(!userRequest.equals("exit")) {
						userRequest = in.readUTF();
						if(userRequest.equals("sobel")) {
							out.writeUTF("sobel");
							//recevoir l'image envoyé du client
							String imageName = in.readUTF();
							String resultName = in.readUTF();
							
							///////////////////////////////////////////////////////////////
					        System.out.println("Reading: " + System.currentTimeMillis());

					        byte[] sizeAr = new byte[4];
					        in.read(sizeAr);
					        int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();

					        byte[] imageAr = new byte[size];
					        in.read(imageAr);

					        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));
					        
					    	//sobeliser
							Sobel sobel = new Sobel();
							image = sobel.process(image);

							//envoyer l'image

							
					        System.out.println("Received " + image.getWidth()  + "x" + image.getHeight() + ": " + System.currentTimeMillis());
					        ImageIO.write(image, "jpg", new File("./src/" + resultName));
							///////////////////////////////////////////////////////////////
							
							System.out.println("l'image a été récu");
						
														

						}
						out.writeUTF(userRequest.toUpperCase());
					}
					sock.close();
				} catch (IOException e) {
					System.out.println(e);
				}
				System.out.println("Connection with client# " + clientNumber + " closed.");
			}
		}
		
		private boolean isUsernameRegistered(String username) {
			return nameAndPasswordMap.get(username) != null;
		}
		
		private void registerUsername(String username) throws IOException {
			FileWriter writer = new FileWriter("./src/users.txt", true);
			PrintWriter printer = new PrintWriter(writer);
			
			printer.println();
			printer.printf("%s", username);
			
			printer.close();
			writer.close();
		}
		
		private void registerPassword(String password) throws IOException {
			FileWriter writer = new FileWriter("./src/users.txt", true);
			PrintWriter printer = new PrintWriter(writer);
			
			printer.printf("%s", " " + password);
			
			printer.close();
			writer.close();
		}
		
		public boolean validateUser(String name, String password) throws IOException {
			//System.out.println(password + " " + nameAndPasswordMap.get(name));
			if((nameAndPasswordMap.get(name) != null) && (nameAndPasswordMap.get(name).equals(password))) {
				//on vérifie le password
				System.out.println("Utilisateur " + name + " connecte");
				return true;
			}
			else {
				System.out.println("Mot de passe incorrect pour " + name);
				return false;
			}
		}
	}
}
