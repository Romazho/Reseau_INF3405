import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class Client {
	private static Socket sock;
	private static Scanner inputSc;
	private static int port;
	private static String serverAddress;
	private static DataInputStream in;
	private static DataOutputStream out;
	private static String serverResponse;

	// source: https://www.techiedelight.com/validate-ip-address-java/
	private static final String IPv4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

	public static void main(String[] args) throws Exception {
		inputSc = new Scanner(System.in);

		createSocket();

		askUserCredentials();

		if (!sock.isClosed()) {
			processUserRequests();
			sock.close();
			inputSc.close();
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

	private static void createSocket() throws UnknownHostException, IOException {
		System.out.println("Saisissez votre adresse IP:");
		serverAddress = inputSc.nextLine();
		
		// check if the ip address is valid
		while (!isValidInet4Address(serverAddress)) {
			System.out.println("Vous avez saisi une mauvaise adresse IP, veuillez réessayer:");
			serverAddress = inputSc.nextLine();
		}

		System.out.println("Sélectionnez votre port:");
		port = inputSc.nextInt();

		// Check if the port is valid
		while (port < 5000 || port > 5050) {
			System.out.println("Veuillez entrer un port entre 5000 et 5050:");
			port = inputSc.nextInt();
		}

		inputSc.nextLine();

		sock = new Socket(serverAddress, port);
		in = new DataInputStream(sock.getInputStream());
		out = new DataOutputStream(sock.getOutputStream());
	}

	// askUserCredentials is responsible for getting the username and password from the user.
	// It then sends both of these informations to the server and handles the cases where the password is incorrect.
	private static void askUserCredentials() throws IOException {
		serverResponse = "";

		System.out.println("Veuillez entrer votre nom d'utilisateur:");
		String username = inputSc.nextLine();
		out.writeUTF(username);

		serverResponse = in.readUTF();

		if (serverResponse.equals(Generals.ServerResponses.NEW_USER)) {
			System.out
					.println(username + " n'existe pas dans la base de donnees. Creation de l'utilisateur " + username);
		}

		System.out.println("Veuillez entrer votre mot de passe:");
		String password = inputSc.nextLine();
		out.writeUTF(password);

		serverResponse = in.readUTF();

		if (serverResponse.equals(Generals.ServerResponses.NEW_USER)) {
			System.out.println("Mot de passe ajoute");
		} else if (serverResponse.equals(Generals.ServerResponses.WRONG_PASSWORD)) {
			while(serverResponse.equals(Generals.ServerResponses.WRONG_PASSWORD)) {
				System.out.println("Mot de passe incorrect. Essayez d'entrer un autre mot de passe ou sortez (exit).");
				password = inputSc.nextLine();
				out.writeUTF(password);
				serverResponse = in.readUTF();
				if(serverResponse.equals(Generals.ServerResponses.OK)) {
					System.out.println("Connexion reussie");
					return;
				} else if(serverResponse.equals(Generals.ServerResponses.DISCONNECTING)) {
					sock.close();
					inputSc.close();
					return;
				}
			}
			
			sock.close();
			inputSc.close();
		} else if (serverResponse.equals(Generals.ServerResponses.OK)) {
			System.out.println("Connexion reussie");
		}
	}
	
	// processUserRequests is responsible for handling the user requests and sending them to the server and then listenening to the server's responses
	private static void processUserRequests() throws IOException {
		String userRequest = "0";

		String serverResponse = in.readUTF();

		if (!serverResponse.equals(Generals.ServerResponses.WRONG_PASSWORD) || !serverResponse.equals(Generals.ServerResponses.DISCONNECTING)) {
			System.out.println(serverResponse);

			while (!userRequest.equals("1")) {
				System.out.println("Que souhaitez-vous faire (entrer le chiffre associe aux options ci-dessous)?");
				System.out.println("1 -> EXIT");
				System.out.println("2 -> SOBEL");

				userRequest = inputSc.nextLine();
				if(isNumeric(userRequest)) {
					switch(Integer.parseInt(userRequest)) {
					case 1:
						out.writeUTF(Generals.ClientRequests.EXIT);
						break;
					case 2:
						out.writeUTF(Generals.ClientRequests.SOBEL);
						prepareImage();
						String imageFormat = in.readUTF();
						BufferedImage processedImage = receiveImage();
						saveImage(processedImage, imageFormat);
						break;
					default:
						System.out.println("Entrez '1' ou '2'");
						break;
					}
				}
				else {
					System.out.println("Entrez '1' ou '2'");
				}

			}
		}
	}
	
	// prepareImage is responsible for getting the image from the current directory and then passing it to sendImage.
	// It also notifies the server of the images name and file format.
	private static void prepareImage() throws IOException {
		// Query the image from the user
		System.out.println("Quel est le nom de l'image (avec son extension de format) a sobeliser? (p.e. image1.jpg)");
		String imageName = inputSc.nextLine();
		File imageFile = new File("./" + imageName);

		while (!imageFile.canRead()) {
			System.out.println("Impossible de lire le fichier ./" + imageName + ". Veuillez reessayer. L'image doit se trouver dans le repertoire courant.");
			imageName = inputSc.nextLine();
			imageFile = new File("./" + imageName);
		}
		
		out.writeUTF(imageName);
		
		sendImage(imageFile, imageName);
	}
	
	// https://stackoverflow.com/questions/25086868/how-to-send-images-through-sockets-in-java?fbclid=IwAR3naVtKkSJQLKs115olSiQ9tCk_z4gbm-bZZZOsnvQqRikFUWK8BKrv-Zo
	// sendImage is responsible for sending an image file and its size to the server
	private static void sendImage(File imageFile, String imageName) throws IOException {
		// Rewrite the image file into a ByteArrayOutputStream to be sent to the server
		BufferedImage image = ImageIO.read(imageFile);
		ByteArrayOutputStream byteArrOutStr = new ByteArrayOutputStream();
		ImageIO.write(image, imageName.split(Pattern.quote("."))[1], byteArrOutStr);
		
		// Send the size of the image and the actual image in the ByteArrayOutputStream
		out.writeInt(byteArrOutStr.size());
		out.write(byteArrOutStr.toByteArray());
		out.flush();
		
		System.out.println("L’image a été envoyée pour le traitement");
	}
	
	// receiveImage is responsible for receiving and returning the image sent from the server
	private static BufferedImage receiveImage() throws IOException {
		// read the size of the image and then read the image into a buffer
		int count = in.readInt();
		byte[] imageDataBuffer = new byte[count];
		in.readFully(imageDataBuffer);
		
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageDataBuffer));
		
		return image;
	}
	
	// saveImage is responsible for saving the provided image in the current directory
	private static void saveImage(BufferedImage imageToSave, String format) {
		System.out.println("Sous quel nom voulez-vous sauvegarder l'image sobelisee?");
		String newImageName = inputSc.nextLine();
		
		try {
			File newImageFile = new File("./" + newImageName + "." + format);
			ImageIO.write(imageToSave, format, newImageFile);
		} catch(Exception e) {
			System.out.println("Erreur pendant la sauvegarde de la nouvelle image");
			System.out.println(e.getMessage());
		}
		
		System.out.println("Image reçue et sauvegardee sous /" + newImageName + "." + format);
	}

	//source : https://stackoverflow.com/questions/14206768/how-to-check-if-a-string-is-numeric
	private static boolean isNumeric(String s) {  
	    return s != null && s.matches("[-+]?\\d*\\.?\\d+");  
	}  
}
