package cs601.proxy;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientHandlerT implements Runnable{
	Socket client;  // socket to the client passed by the main in ProxyServer.java
	Socket server; // socket to the server
	InputStream cIn; // input stream from the client
	OutputStream cOut; //output stream to the client
	InputStream sIn;  //input stream from the server
	OutputStream sOut;  //output stream to the server 
	String method;  // the request method
	String host;  // the quested host
	public static final int HTTP = 80; // default port at the server
    protected boolean debug = true;  // debug flag
    
	public ClientHandlerT(Socket client) throws IOException{ // constructor allowing the main to pass the client
		this.client = client;
		cIn = client.getInputStream();
		cOut = client.getOutputStream();
	}
	
    @Override
    public void run(){
    	try {
    		//if request valid
    		if (valid()){
    			//send the request to server
    			cToS();
        		//send the response back to client
        		sToC(server); //
    		}
    		if(client != null) client.close();
    		System.out.println("Client closed.");
    		if(server != null) server.close();
            System.out.println("server closed.");
           } catch (IOException ioe) {
        	   ioe.printStackTrace();
           } 
    }
    
    //Client to Server, send the request out
    private void cToS() throws IOException{
    	//getRequest: method path protocol
    	String request = getRequest(readLine(cIn));
    	//getHeaders: name: value pairs, extract the host
    	String headers = getHeaders(cIn);
    	//Create a server socket to connect to the requested host
        server= new Socket(host, HTTP);
        sOut = server.getOutputStream();
        //send the request to upstream server
        send(request, headers, cIn, sOut);
    }
    //Server to Client, send the data back to the browser
    void sToC(Socket server) throws IOException{
    	
    	sIn = server.getInputStream();
        String status = readLine(sIn); //connection status
        String headers = getHeaders(sIn);
        send(status, headers, sIn, cOut);
    	
    }
    
    //Send client's request to upstream server
	private void send(String request, String headers, InputStream in, OutputStream out) throws IOException{
		String message = request + headers;
		//if (debug) System.out.println(message);
		out.write(message.getBytes());
        if (in.available() != 0){
            int length = in.available();
            byte[] bArray = new byte[4096];
            int count = 0;
            while(count < length){
                int i = in.read(bArray);
                out.write(bArray,0,i);
                count += i;
            }
        }
        out.flush();
	}
    
    //Analyze the request, get the first line of request to send to the server
    String getRequest(String firstLine) throws IOException{
        Map<String, String> mRequest = new HashMap<String, String>(); // The table to save all information about the request 
        //if (debug) System.out.println(firstLine);
        String[] methodURLProtocol = firstLine.split("[ ]+"); // separate the three components in the first line 
        mRequest.put("method", methodURLProtocol[0]);
        mRequest.put("URL", methodURLProtocol[1]);
        mRequest.put("protocol", "HTTP/1.0");// Force it to version 1.0
        this.method = mRequest.get("method");
        // Get the relative URL in the first line, path
        String[] urlArray = methodURLProtocol[1].split("[/]");//split URL around "/"
        //if (debug) System.out.println(urlArray[2]);
        String path = "/";
        for(int i = 3; i < urlArray.length; i++){
        	path ="";
        	path = path.concat("/").concat(urlArray[i]); // Need to test more complicated path
        }
        mRequest.put("path", path);
   	    String request = mRequest.get("method") + " " + mRequest.get("path") + " " + mRequest.get("protocol")  + "\r\n";
   	    //if (debug) System.out.println(request.toString());
        return request;
    }
 
    //Analyze the headers , extract the host
    private String getHeaders(InputStream cIn) throws IOException{
    	Map<String, String> mHeaders = new HashMap<String, String>();
        //Get the header lines, header name as keys and the content as values
    	String header;
    	while((header = readLine(cIn)) != null){
    		if(header != "\r\n"){
    			String[] hArray =  header.split("[:]",2); // split around the first ":"           
    			mHeaders.put(hArray[0].toLowerCase().trim(), hArray[1].trim());   
    		}else break;
    	}
        this.host = mHeaders.get("host");
        String headers = ""; // to save the headers information
        Iterator it = mHeaders.entrySet().iterator(); // iterate through the headers Map 
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next(); // get the header name and value pairs
            //mask
            if(!(((String) pairs.getKey()).contains("connection")) && ((((String) pairs.getValue()).contains("keep-alive"))) && !((String) pairs.getKey()).contains("user-agent") && !((String) pairs.getKey()).contains("proxy-connection") && !((String) pairs.getKey()).contains("referer")){ // mask the user-agent header
                headers = headers + pairs.getKey() +  ": " + pairs.getValue() + "\r\n";
            }
        }
       
        //if (debug) System.out.println(headers.toString());
        return headers;
    }
    
    //get data from the request
    public String getData() { // maybe split after the second blank line??
        String data = "";
        return data;
    }
    
    //read in one line as a String  each time
    private String readLine(InputStream cIn) throws IOException{
    	String oneLine = "";
        int count;
        while((count = cIn.read()) != -1){ //while available
            char c = (char)count;  // type cast to character
            oneLine = oneLine.concat(String.valueOf(c)); // convert to String and add to the growing line
            if (c == '\n'){  // return if line finishes
                return oneLine;
            }
        }
        return null;
    }
    
    //To see if it is a valid request
    private boolean valid() throws IOException{
    	byte[] method = new byte[4];
    	cIn.read(method);
    	String sMethod = new String(method);
    	if(sMethod.equals("GET ")||sMethod.equals("POST")) return true;
    	else return false;
    }   
}
  
