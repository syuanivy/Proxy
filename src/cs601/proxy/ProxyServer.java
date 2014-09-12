package cs601.proxy;
import java.io.*;
import java.net.*;


public class ProxyServer {
	public static final int listeningPort= 8888;  // Port used to listen to requests
	public static boolean openForService = true;
	public static final boolean SingleThreaded = false;
	
	public static void main(String[] args) throws Exception {   

		ServerSocket sSocket = new ServerSocket(listeningPort); //Open a server socket
		while (openForService) {
            try { 
                Socket client = sSocket.accept(); //Wait for a socket connection
                if (SingleThreaded){
                	new ClientHandler(client).run(); // pass the client to ClientHandler and run
                }
                else{
                   new Thread(new ClientHandler(client)).start();
                }
            }catch (IOException ioe) { 
    			System.err.println("Can't complete the service!");
    			ioe.printStackTrace(System.err);
    		}
        }
		sSocket.close();
	}
}

