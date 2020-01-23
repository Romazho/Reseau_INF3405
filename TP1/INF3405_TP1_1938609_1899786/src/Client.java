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

	//source: https://www.techiedelight.com/validate-ip-address-java/
	private static final String IPv4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

	private static final Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

	public static void main(String[] args) throws Exception {
		inputSc = new Scanner(System.in);

		createSocket();
		
		//sendImage();

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
		
		//serverResponse = in.readUTF();
		
		System.out.println(serverResponse);
		
		if(serverResponse.equals("newuser")) {
			System.out.println("Mot de passe ajoute");
		}
	}

	private static void processUserRequests() throws IOException {
		String userRequest = "";

		System.out.println("in processUserRequests");
		
		String serverResponse = in.readUTF();

		if (!serverResponse.equals("wrongpassword")) {
			System.out.println(serverResponse);

			while (!userRequest.equals("exit")) {
				System.out.println("Que souhaitez-vous faire? Sortir (exit) ou 'sobelizer' une image (sobel)");

				userRequest = inputSc.nextLine();

				out.writeUTF(userRequest);

				//serverResponse = in.readUTF();

				if(userRequest.equals("sobel")) {
					sendImage();
					//attendre que le serveur nous envoie l'image sobeliser
					//recvoire l'image et la sauvgarder localement.
				}
				
				System.out.println(serverResponse);
			}
		}
	}
	
	private static void sendImage() {
		try {
			System.out.println("Quel est le nom de l'image que vous souhaitez 'Sobeliser'? (spécifier le format de l'image)");
			String imageName = inputSc.nextLine();
			
			//quoi faire si l'image n'existe pas?
			
			System.out.println("Donnez le nom de l'image 'sobeliser' (spécifier le format de l'image)");
			String resultName = inputSc.nextLine();
			
			BufferedImage image = ImageIO.read(new File( "./src/" + imageName ));
						
		    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	        ImageIO.write(image, "jpg", byteArrayOutputStream);

	        byte[] size = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
	        out.write(size);
	        out.write(byteArrayOutputStream.toByteArray());
	        out.flush();
	        System.out.println("Flushed: " + System.currentTimeMillis());	
	        
			System.out.println("image envoyé au serveur");
	        
			//recevoir l'image et dire où l'image a été stocké
			byte[] sizeAr = new byte[4];
	        in.read(sizeAr);
	        int sizeBuffer = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
			//System.out.println(sizeBuffer);

	        byte[] imageAr = new byte[sizeBuffer];
	        in.read(imageAr);
			//System.out.println(imageAr);
			
			System.out.println(image);
	        BufferedImage newImage = ImageIO.read(new ByteArrayInputStream(imageAr));
			System.out.println(newImage);

	        String imagePlace = "./src/" + resultName;
	        
	        ImageIO.write(newImage, "jpg", new File(imagePlace));
			System.out.println("Image sauvgarder sous: " + imagePlace);

			
	    	//out.close();
			//in.close();
	    	
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		}

	}
	

	
	
	/*try {
    
 	//sock = new Socket("localhost", 8006);
	System.out.println("Image is being sent");

  
 	ObjectOutputStream  oos = new ObjectOutputStream(sock.getOutputStream());
 	//  ois = new ObjectInputStream(socket.getInputStream());
    
  	oos.flush();
   	oos.writeObject(new String("./src/potato.png"));
    
    oos.flush();
    oos.reset();
    //int sz=(Integer )ois.readObject();
    //System.out.println ("Receving "+(sz/1024)+" Bytes From Sever");
     
    byte b[]=new byte [sz];
    int bytesRead = ois.read(b, 0, b.length);
      for (int i = 0; i<sz; i++)
        {
            System.out.print(b[i]);
        }
        FileOutputStream fos=new FileOutputStream(new File("demo.jpg"));
        fos.write(b,0,b.length);
    System.out.println ("From Server : "+ois.readObject());
    oos.close();
    ois.close();
  } catch(Exception e) {
    System.out.println(e.getMessage());
    e.printStackTrace();
  }*/
}
