import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private static ServerSocket listener;
	
	public static void main(String[] args) throws Exception {
		int clientCounter = 0;
		String serverAddress = "127.0.0.1";
		int port = 5000;	
		
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
	
	private static class ClientHandler extends Thread {
		private Socket sock;
		private int clientNumber;
		
		public ClientHandler(Socket socket, int clientNumber) {
			this.sock = socket;
			this.clientNumber = clientNumber;
			System.out.println("New connection with client# " + clientNumber + " at " + socket);
		}
		
		public void run() {
			try {
				DataOutputStream out = new DataOutputStream(sock.getOutputStream());
				out.writeUTF("Hello from server - you are client# " + clientNumber);
			} catch (IOException e) {
				System.out.println("Error handling client# " + clientNumber + ": " + e);
			} finally {
				try {
					sock.close();
				} catch (IOException e) {
					System.out.println("Can't close a socket, wtf?");
				}
				System.out.println("Connection with client# " + clientNumber + " closed.");
			}
		}
	}
}
