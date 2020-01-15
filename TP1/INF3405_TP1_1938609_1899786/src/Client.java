import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
	private static Socket sock;
	
	//partie qui vérifie la validité de l'adresse IP en utilisant la librairie Regex 
	//MENTIONER LA SOURCE
	private static final String IPv4_REGEX =
			"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
			"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

	public static boolean isValidInet4Address(String ip) {
		if (ip == null) {
			return false;
		}

		Matcher matcher = IPv4_PATTERN.matcher(ip);

		return matcher.matches();
	}
	
	public static void main(String[] args) throws Exception {
	
		//demander à l'utilisateur ces 4 attributs
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
		
		sock = new Socket(serverAddress, port);
		
		DataOutputStream out = new DataOutputStream(sock.getOutputStream());
		
		System.out.println("Veuillez entrer votre nom d'utilisateur:");
		String nomUtilisateur = S.next();
		out.writeUTF(nomUtilisateur);
		
		System.out.println("Veuillez entrer votre mot de passe:");
		String motPasse = S.next();
		out.writeUTF(motPasse);
		
		System.out.format("Server running on %s:%d%n", serverAddress, port);
		
		DataInputStream in = new DataInputStream(sock.getInputStream());
		
		String msg = in.readUTF();
		
		System.out.println(msg);
		
		sock.close();
	}
}
