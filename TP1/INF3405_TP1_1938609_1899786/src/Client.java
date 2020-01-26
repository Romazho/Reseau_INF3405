import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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

	public static boolean isValidInet4Address(String ip) {
		if (ip == null) {
			return false;
		}

		Matcher matcher = IPv4_PATTERN.matcher(ip);

		return matcher.matches();
	}

	private static void createSocket() throws UnknownHostException, IOException {
		System.out.println("S�l�ctionnez votre adresse IP:");
		// serverAddress = inputSc.nextLine();
		serverAddress = "127.0.0.1";

		while (!isValidInet4Address(serverAddress)) {
			System.out.println("Vous avez s�lectionn� une mauvaise adresse IP, veuillez r�essayer:");
			serverAddress = inputSc.nextLine();
		}

		port = 5000;
		System.out.println("S�lectionnez votre port:");
		// port = inputSc.nextInt();

		// v�rification de la coh�rence du port
		while (port < 5000 || port > 5050) {
			System.out.println("Veuillez entrer un port entre 5000 et 5050:");
			port = inputSc.nextInt();
		}

		// inputSc.nextLine();

		sock = new Socket(serverAddress, port);
		in = new DataInputStream(sock.getInputStream());
		out = new DataOutputStream(sock.getOutputStream());
	}

	private static void askUserCredentials() throws IOException {
		serverResponse = "";

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

		if (serverResponse.equals("newuser")) {
			System.out.println("Mot de passe ajoute");
		} else if (serverResponse.equals("wrongpassword")) {
			System.out.println("Mot de passe incorrect, deconnexion");
			sock.close();
			inputSc.close();
		} else if (serverResponse.equals("ok")) {
			System.out.println("Connexion reussie");
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

				if (userRequest.equals("sobel")) {
					prepareImage();
					String imageFormat = in.readUTF();
					System.out.println("Image format " + imageFormat);
					BufferedImage processedImage = receiveImage();
					saveImage(processedImage, imageFormat);
				}

				System.out.println(serverResponse);
			}
		}
	}
	
	private static void prepareImage() throws IOException {
		System.out.println("Quel est le nom de l'image (son extension de format) a sobeliser? (p.e. image1.jpg)");
		String imageName = inputSc.nextLine();
		File imageFile = new File("./src/" + imageName);

		while (!imageFile.canRead()) {
			System.out.println("Impossible de lire le fichier ./src/" + imageName + ". Veuillez reessayer. L'image doit se trouver dans le repertoire courant.");
			imageName = inputSc.nextLine();
			imageFile = new File("./src/" + imageName);
		}
		
		out.writeUTF(imageName);
		
		sendImage(imageFile, imageName);
	}
	
	//https://stackoverflow.com/questions/25086868/how-to-send-images-through-sockets-in-java?fbclid=IwAR3naVtKkSJQLKs115olSiQ9tCk_z4gbm-bZZZOsnvQqRikFUWK8BKrv-Zo
	private static void sendImage(File imageFile, String imageName) throws IOException {
		BufferedImage image = ImageIO.read(imageFile);
		ByteArrayOutputStream byteArrOutStr = new ByteArrayOutputStream();
		ImageIO.write(image, imageName.split(Pattern.quote("."))[1], byteArrOutStr);
		
		// Send the image size as a byte array.
		byte[] size = ByteBuffer.allocate(Integer.BYTES).putInt(byteArrOutStr.size()).array();
		out.write(size);
		
		// Send the actual image
		out.write(byteArrOutStr.toByteArray());
		out.flush();
	}
	
	//https://stackoverflow.com/questions/25086868/how-to-send-images-through-sockets-in-java?fbclid=IwAR3naVtKkSJQLKs115olSiQ9tCk_z4gbm-bZZZOsnvQqRikFUWK8BKrv-Zo
	private static BufferedImage receiveImage() throws IOException {
		byte[] size = new byte[Integer.BYTES];
		in.read(size);
		
		int imageSize = ByteBuffer.wrap(size).asIntBuffer().get();
		byte[] streamImage = new byte[imageSize];
		in.read(streamImage);
		
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(streamImage));
		return image;
	}
	
	private static void saveImage(BufferedImage imageToSave, String format) {
		System.out.println("Sous quel nom voulez-vous sauvegarder l'image sobelisee?");
		String newImageName = inputSc.nextLine();
		
		try {
			File newImageFile = new File("./src/" + newImageName + "." + format);
			ImageIO.write(imageToSave, format, newImageFile);
		} catch(Exception e) {
			System.out.println("Erreur pendant la sauvegarde de la nouvelle image");
			System.out.println(e.getMessage());
		}
		
		System.out.println("Image sauvegardee sous ./src/" + newImageName + "." + format);
	}

//	private static void sendImage() {
//		try {
//			System.out.println("Quel est le nom de l'image que vous souhaitez 'Sobeliser'? (sp�cifier le format de l'image)");
//			String imageName = inputSc.nextLine();
//			File imageFile = new File( "./src/" + imageName );
//			
//			while(!imageFile.exists()) {
//				System.out.println("L'image n'existe pas dans le repertoire courant. Veuillez essayer de nouveau.");
//				imageName = "./src/" + inputSc.nextLine();
//				imageFile = new File(imageName);
//			}
//			
//			System.out.println("Donnez le nom de l'image 'sobeliser' (sp�cifier le format de l'image)");
//			String resultName = inputSc.nextLine();
//			
//			BufferedImage image = ImageIO.read(imageFile);
//						
//		    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//	        ImageIO.write(image, "jpg", byteArrayOutputStream);
//
//	        byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
//	        out.write(size);
//	        out.write(byteArrayOutputStream.toByteArray());
//	        out.flush();
//	        System.out.println("Flushed: " + System.currentTimeMillis());	
//	        
//			System.out.println("image envoy� au serveur");
//	        
//			//recevoir l'image et dire o� l'image a �t� stock�
//			byte[] sizeAr = new byte[4];
//	        in.read(sizeAr);
//	        int sizeBuffer = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
//			//System.out.println(sizeBuffer);
//
//	        byte[] imageAr = new byte[sizeBuffer];
//	        in.read(imageAr);
//			//System.out.println(imageAr);
//			
//			System.out.println(image);
//	        BufferedImage newImage = ImageIO.read(new ByteArrayInputStream(imageAr));
//			System.out.println(newImage);
//
//	        String imagePlace = "./src/" + resultName;
//	        
//	        ImageIO.write(newImage, "jpg", new File(imagePlace));
//			System.out.println("Image sauvgarder sous: " + imagePlace);
//			
//			
//	    	//out.close();
//			//in.close();
//	    	
//		} catch ( FileNotFoundException e ) {
//			e.printStackTrace();
//		} catch ( IOException e ) {
//			e.printStackTrace();
//		}
//
//	}

	/*
	 * try {
	 * 
	 * //sock = new Socket("localhost", 8006);
	 * System.out.println("Image is being sent");
	 * 
	 * 
	 * ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream()); //
	 * ois = new ObjectInputStream(socket.getInputStream());
	 * 
	 * oos.flush(); oos.writeObject(new String("./src/potato.png"));
	 * 
	 * oos.flush(); oos.reset(); //int sz=(Integer )ois.readObject();
	 * //System.out.println ("Receving "+(sz/1024)+" Bytes From Sever");
	 * 
	 * byte b[]=new byte [sz]; int bytesRead = ois.read(b, 0, b.length); for (int i
	 * = 0; i<sz; i++) { System.out.print(b[i]); } FileOutputStream fos=new
	 * FileOutputStream(new File("demo.jpg")); fos.write(b,0,b.length);
	 * System.out.println ("From Server : "+ois.readObject()); oos.close();
	 * ois.close(); } catch(Exception e) { System.out.println(e.getMessage());
	 * e.printStackTrace(); }
	 */
}
