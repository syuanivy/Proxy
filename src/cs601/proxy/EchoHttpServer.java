package cs601.proxy;

import java.io.*;
import java.net.*;

// http://java.sun.com/j2se/1.3/docs/api/java/net/Socket.html
// http://java.sun.com/j2se/1.3/docs/api/java/net/ServerSocket.html

// Print incoming headers to stdout, echo any POST data back to browser
public class EchoHttpServer {
	// $ java cs601.proxy.EchoServer 8081
	public static void main(String[] args) throws IOException {
		int port = 8081;
		if ( args.length>0 ) {
			port = Integer.valueOf(args[0]);
		}
		ServerSocket s = new ServerSocket(port);
		while ( true ) {
			Socket channel = s.accept();
			System.out.println("\nListening via port "+channel.getPort());
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

//			String input = read(in);
//			System.out.println(input);

			DataInputStream din = new DataInputStream(in);
			String line = din.readLine();
			System.out.println(line);
			String firstLine = line;
			int content_length = 0;
			while ( line!=null && line.length()>0 ) {
				if ( line.toLowerCase().startsWith("content-length") ) {
					int colon = line.indexOf(":");
					String opnd = line.substring(colon+1);
					content_length = Integer.valueOf(opnd.trim());
				}
				line = din.readLine();
				System.out.println(line);
			}
			String postData = null;
			if ( firstLine.startsWith("POST") ) {
				postData = read(din, content_length);
				System.out.println(postData);
			}

			out.write("HTTP/1.1 200 OK\r\n".getBytes());
//			out.write("content-type=text/html\r\n".getBytes());
//			out.write("connection=close\r\n".getBytes());
			if ( postData!=null ) {
				out.write("\r\n".getBytes()); // blank after response headers
				out.write(postData.getBytes());
			}
			out.flush();
			out.close();
			in.close();
			System.out.println("Closing port "+channel.getPort());
			channel.close();
		}
	}

	public static String read(InputStream in, int n) throws IOException {
		StringBuilder buf = new StringBuilder();
		int c = in.read();
//		System.out.println("read "+(char)c);
		int i = 1;
		while ( c!=-1 && i < n) {
			buf.append((char)c);
			c = in.read();
//			System.out.println("read "+(char)c);
			i++;
		}
//		System.out.println("Done");
		return buf.toString();
	}
}
