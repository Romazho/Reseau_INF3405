import java.io.DataInputStream;
import java.net.Socket;

public class Client {
	private static Socket sock;
	
	public static void main(String[] args) throws Exception {
		String serverAddress = "127.0.0.1";
		int port = 5000;
		
		sock = new Socket(serverAddress, port);
		
		System.out.format("Server running on %s:%d%n", serverAddress, port);
		
		DataInputStream in = new DataInputStream(sock.getInputStream());
		
		String msg = in.readUTF();
		
		System.out.println(msg);
		
		sock.close();
	}
}
