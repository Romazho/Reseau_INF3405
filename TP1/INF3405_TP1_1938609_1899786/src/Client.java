import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
	private static Socket sock;
	private static Scanner inputSc;
	private static int port;
	private static String serverAddress;
	private static DataInputStream in;
	private static DataOutputStream out;

	// partie qui vérifie la validité de l'adresse IP en utilisant la librairie
	// Regex
	// MENTIONER LA SOURCE
	private static final String IPv4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

	public static void main(String[] args) throws Exception {
		inputSc = new Scanner(System.in);

		createSocket();

		askUserCredentials();

		processUserRequests();

		sock.close();
		inputSc.close();
	}

	public static boolean isValidInet4Address(String ip) {
		if (ip == null) {
			return false;
		}

		Matcher matcher = IPv4_PATTERN.matcher(ip);

		return matcher.matches();
	}

	private static void createSocket() throws UnknownHostException, IOException {
		System.out.println("Séléctionnez votre adresse IP:");
		serverAddress = inputSc.nextLine();

		while (!isValidInet4Address(serverAddress)) {
			System.out.println("Vous avez sélectionné une mauvaise adresse IP, veuillez réessayer:");
			serverAddress = inputSc.nextLine();
		}

		port = 5000;
		System.out.println("Sélectionnez votre port:");
		port = inputSc.nextInt();

		// vérification de la cohérence du port
		while (port < 5000 || port > 5050) {
			System.out.println("Veuillez entrer un port entre 5000 et 5050:");
			port = inputSc.nextInt();
		}

		inputSc.nextLine();

		sock = new Socket(serverAddress, port);
		in = new DataInputStream(sock.getInputStream());
		out = new DataOutputStream(sock.getOutputStream());
	}

	private static void askUserCredentials() throws IOException {
		String serverResponse = "";
		
		System.out.println("Veuillez entrer votre nom d'utilisateur:");
		String username = inputSc.nextLine();
		out.writeUTF(username);
		
		serverResponse = in.readUTF();
		
		if (serverResponse.equals("usernull")) {
			System.out
					.println(username + " n'existe pas dans la base de donnees. Creation de l'utilisateur " + username);
		}

		System.out.println("Veuillez entrer votre mot de passe:");
		String password = inputSc.nextLine();
		out.writeUTF(password);
		
		serverResponse = in.readUTF();
		
		if(serverResponse.equals("newuser")) {
			System.out.println("Mot de passe ajoute");
		}
	}

	private static void processUserRequests() throws IOException {
		String userRequest = "";

		String serverResponse = in.readUTF();

		if (!serverResponse.equals("wrongpassword")) {
			System.out.println(serverResponse);

			while (!userRequest.equals("exit")) {
				System.out.println("Que souhaitez-vous faire? Sortir (exit) ou 'sobelizer' une image (sobel)");

				userRequest = inputSc.nextLine();

				out.writeUTF(userRequest);

				serverResponse = in.readUTF();

				System.out.println(serverResponse);
			}
		}
	}
}
