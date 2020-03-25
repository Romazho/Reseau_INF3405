import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class Server {
	private static ServerSocket listener;
	private static Map<String, String> nameAndPasswordMap;

	// source: https://www.techiedelight.com/validate-ip-address-java/
	private static final String IPv4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

	public static void main(String[] args) throws Exception {
		int clientCounter = 0;
		nameAndPasswordMap = new HashMap<String, String>();

		System.out.println("Saisissez votre adresse IP:");

		String serverAddress = "";
		Scanner S = new Scanner(System.in);
		serverAddress = S.next();
		
		// check if the ip address is valid
		while (!isValidInet4Address(serverAddress)) {
			System.out.println("Vous avez sélectionnée une mauvaise adresse IP, veuillez réessayer:");
			serverAddress = S.next();
		}

		int port = 0;
		System.out.println("Saisissez votre port:");
		port = S.nextInt();

		// Check if the port is valid
		while (port < 5000 || port > 5050) {
			System.out.println("Veuillez entrer un port entre 5000 et 5050:");
			port = S.nextInt();
		}

		S.close();

		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		listener.bind(new InetSocketAddress(serverIP, port));
		System.out.format("Le serveur roule sur %s:%d%n", serverAddress, port);

		try {
			while (true) {
				new ClientHandler(listener.accept(), clientCounter++).start();
			}
		} finally {
			listener.close();
		}
	}
	
	// isValidInet4Address is responsible for validating if the provided ip address is a valid ipv4 address
	public static boolean isValidInet4Address(String ip) {
		if (ip == null) {
			return false;
		}

		Matcher matcher = IPv4_PATTERN.matcher(ip);

		return matcher.matches();
	}
	
	// readUsers is responsible for initializing the map containing the usernames as keys and the passwords from the database as values
	private static void readUsers() {
		try {
			nameAndPasswordMap.clear();
			File fileDesc = new File("./users.txt");
			Scanner reader = new Scanner(fileDesc);

			while (reader.hasNextLine() && fileDesc.length() != 0) {
				String line = reader.nextLine();

				String[] nameAndPassword = line.split(" ");

				nameAndPasswordMap.put(nameAndPassword[0], nameAndPassword[1]);
			}

			reader.close();

		} catch (FileNotFoundException error) {
			System.out.println(error);
			error.printStackTrace();
		}
	}

	private static class ClientHandler extends Thread {
		private Socket sock;
		private int clientNumber;
		private DataOutputStream out;
		private DataInputStream in;
		private String userRequest;
		private String username;
		private int clientPort;
		private String adresseIP;
		private String imageName;

		public ClientHandler(Socket socket, int clientNumber) {
			this.sock = socket;
			this.clientNumber = clientNumber;
			try {
				out = new DataOutputStream(sock.getOutputStream());
				in = new DataInputStream(sock.getInputStream());
			} catch (IOException e) {
				System.out.println(e);
			}
			System.out.println("Nouvelle connexion avec le client #" + clientNumber + " à " + socket);
		}
		
		// the client requests are handled inside the run method
		public void run() {
			userRequest = "";
			try {
				setupUser();
			} catch (IOException e) {
				System.out.println("Erreur de gestion du client # " + clientNumber + ": " + e);
			} finally {
				try {
					while (!userRequest.equals(Generals.ClientRequests.EXIT)) {
						userRequest = in.readUTF();
						if (userRequest.equals(Generals.ClientRequests.SOBEL)) {
							imageName = in.readUTF();
							BufferedImage image = receiveImage();
							BufferedImage processedImage = doSobel(image);
							out.writeUTF(imageName.split(Pattern.quote("."))[1]);
							sendImage(processedImage, imageName.split(Pattern.quote("."))[1]);
						}
					}
					out.writeUTF(Generals.ServerResponses.DISCONNECTING);
					sock.close();
				} catch (IOException e) {
					System.out.println(e);
					e.printStackTrace(System.out);
				}
				System.out.println("La connexion avec le client #" + clientNumber + " est fermée.");
			}
		}
		
		// receiveImage is responsible for receiving and returning the image sent from the client 
		private BufferedImage receiveImage() throws IOException {
			// read the size of the image and then read the image into a buffer
			int count = in.readInt();
			byte[] imageDataBuffer = new byte[count];
			in.readFully(imageDataBuffer);
			
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageDataBuffer));
						
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd@HH:mm:ss");
			Date date = new Date();
			System.out.println("[" + username + " - " + adresseIP.substring(1) + ":" + clientPort + " - " + dateFormat.format(date) + "]" + " : Image " + imageName + " recue pour traitement.");
			
			return image;
		}
		
		// doSobel is responsible for applying the Sobel filter on an image and then returning the processed image
		private BufferedImage doSobel(BufferedImage image) throws IOException {
			return Sobel.process(image);
		}
		
		// https://stackoverflow.com/questions/25086868/how-to-send-images-through-sockets-in-java?fbclid=IwAR3naVtKkSJQLKs115olSiQ9tCk_z4gbm-bZZZOsnvQqRikFUWK8BKrv-Zo
		// sendImage is responsible for sending an image file and its size to the client
		private void sendImage(BufferedImage image, String format) throws IOException {
			// Rewrite the image file into a ByteArrayOutputStream to be sent to the server
			ByteArrayOutputStream byteArrOutStr = new ByteArrayOutputStream();
			ImageIO.write(image, format, byteArrOutStr);
			
			// Send the size of the image and the actual image in the ByteArrayOutputStream
			out.writeInt(byteArrOutStr.size());
			out.write(byteArrOutStr.toByteArray());
			out.flush();
		}
		
		// setupUser is responsible for:
		// 1. Registering the user if the username provided is not in the database.
		// 2. Validating the password provided for a given username.
		// 3. Handling cases where the password is incorrect.
		private void setupUser() throws IOException {
			String name = in.readUTF();
			
			// Check if username is in the database
			boolean isExistantUser = isUsernameRegistered(name);
			if (!isExistantUser) {
				registerUsername(name);
				out.writeUTF(Generals.ServerResponses.NEW_USER);
			} else {
				out.writeUTF(Generals.ServerResponses.OK);
			}
			
			String password = in.readUTF();
			// If the username was not registered, add the username.
			// Otherwise, check if the password is the correct one for the povided username
			if (!isExistantUser) {
				registerPassword(password);
				out.writeUTF(Generals.ServerResponses.NEW_USER);
			} else if (!validateUser(name, password)) {
				while(!validateUser(name, password)) {
					out.writeUTF(Generals.ServerResponses.WRONG_PASSWORD);
					password = in.readUTF();
					if(password.equals(Generals.ClientRequests.EXIT)) {
						userRequest = Generals.ClientRequests.EXIT;
						out.writeUTF(Generals.ServerResponses.DISCONNECTING);
						return;
					}
				}
				out.writeUTF(Generals.ServerResponses.OK);
			} else {
				out.writeUTF(Generals.ServerResponses.OK);
			}
			
			out.writeUTF("Bonjour du serveur - vous êtes le client #" + clientNumber);
			clientPort = sock.getLocalPort();
			adresseIP = sock.getInetAddress().toString();
			username = name;
		}
		
		// isUsernameRegistered is responsible for checking if the username is present in the database
		private boolean isUsernameRegistered(String username) {
			readUsers();
			return nameAndPasswordMap.get(username) != null;
		}
		
		// registerUsername is responsible for registering a username in the database
		private void registerUsername(String username) throws IOException {
			File fileDesc = new File("./users.txt");
			FileWriter writer = new FileWriter("./users.txt", true);
			PrintWriter printer = new PrintWriter(writer);
			
			if(fileDesc.length() > 0) {
				printer.println();	
			}
			printer.printf("%s", username);
			printer.close();
			writer.close();
		}
		
		// registerPassword is responsible for registering a password in the database
		private void registerPassword(String password) throws IOException {
			FileWriter writer = new FileWriter("./users.txt", true);
			PrintWriter printer = new PrintWriter(writer);

			printer.printf("%s", " " + password);

			printer.close();
			writer.close();
		}
		
		// validateUser is responsible for checking if the username and password that are provided match the records in the database
		public boolean validateUser(String name, String password) throws IOException {
			if ((nameAndPasswordMap.get(name) != null) && (nameAndPasswordMap.get(name).equals(password))) {
				System.out.println("Utilisateur " + name + " connecté");
				return true;
			} else {
				System.out.println("Mot de passe incorrect pour " + name);
				return false;
			}
		}
	}
}
