package calc_backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		
		try {
			//keeps the server running
			serverSocket = new ServerSocket(8080);
			while (true) {
				System.out.println("Listening on 8080");
				//blocks until a request is passed
				Socket clientSocket = serverSocket.accept();
				
				new RequestProcessor(clientSocket).run();
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
