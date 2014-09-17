package cs601.proxy;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientHandler implements Runnable{
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
    
	public ClientHandler(Socket client) throws IOException{ // constructor allowing the main to pass the client
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
				sToC(); //
			}
			if(client != null) client.close();
			System.out.println();
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
    	//debug(request);
    	//getHeaders: name: value pairs, extract the host
    	String headers = getHeaders(cIn);
    	
    	//debug(headers);
    	//Create a server socket to connect to the requested host
        server= new Socket(host, HTTP);
        sOut = server.getOutputStream();
        //send the request to upstream server
        send(request, headers,cIn, sOut);
    }
    //Server to Client, send the data back to the browser
    void sToC() throws IOException{
    	
    	sIn = server.getInputStream();
        String status = readLine(sIn); //connection status, ends with \n
        String headers = getHeaders(sIn);//headers starts with "\r\n"
        String status1 = status.substring(0,status.length()-2);
        send(status1, headers, sIn, cOut);//no blank line in between status and headers
    	
    }
    
    //Send information both ways: request/status+headers+data
	private void send(String request, String headers, InputStream in, OutputStream out) throws IOException{
		String message = request + headers+ "\r\n" + "\r\n"; // status + headers + blank line +data when sToC
		debug(message);
		out.write(message.getBytes()); // finish writing the headers, blank line
		
		if (in != null){ // there is data to send to the other side
            int length = in.available();
            byte[] bArray = new byte[4096];
            int count = 0;
            while(count < length){
                int i = in.read(bArray);
                //System.out.print(new String(bArray));
                out.write(bArray,0,i);
                count += i;
            }
		}
         out.flush();
	}
    
    //Analyze the request, get the first line of request to send to the server
    String getRequest(String firstLine) throws IOException{
        Map<String, String> mRequest = new HashMap<String, String>(); // The table to save all information about the request 
        //debug(firstLine);
        String[] URLProtocol = firstLine.split("[ ]+"); // separate the three components in the first line 
        mRequest.put("method", this.method);
        mRequest.put("URL", URLProtocol[0]);
        mRequest.put("protocol", "HTTP/1.0");// Force it to version 1.0
        //debug(URLProtocol[0]);
        // Get the relative URL in the first line, path
        String[] urlArray = URLProtocol[0].split("//",2);//split URL around "//"
        String[] pathArray = urlArray[1].split("[/]",2);
        String path = pathArray[1];
        path = ("/").concat(path);
        //debug(urlArray[0]); //http:
        //debug(urlArray[1]); //""
        //debug(urlArray[2]); //website, www.cs.usfca.edu/_/xyzabc/123..
/*        
        String path = "/";
        for(int i = 3; i < urlArray.length-1; i++){
        	//System.out.println(path);
        	path = path.concat(urlArray[i]).concat("/"); // Need to test more complicated path
        }
       // path = path.concat(urlArray[i])
        //if (path.length()>1) path = path.substring(0,path.length()-1); // get rid of "/" at the end
         * */
        mRequest.put("path", path);
        debug(path);
   	    String request = mRequest.get("method") + " " + mRequest.get("path") + " " + mRequest.get("protocol");
   	    //debug(request);
        return request;
    }
 
    //Analyze the headers , extract the host
    private String getHeaders(InputStream cIn) throws IOException{
    	Map<String, String> mHeaders = new HashMap<String, String>();
        //Get the header lines, header name as keys and the content as values
    	String header;
    	while((header = readLine(cIn)) != null){
    		//debug(header);
    		if(! header.equals("\r\n") ){
    			String[] hArray =  header.split("[:]",2); // split around the first ":"
    			//debug(hArray[0]);
    			//debug(hArray[1]);
    			
    			if(hArray.length < 2){
    				mHeaders.put(hArray[0].toLowerCase().trim(), null);  
    			}else{
    				mHeaders.put(hArray[0].toLowerCase().trim(), hArray[1].trim());  
    			} 
    		}else break;
    	}
    	host = mHeaders.get("host");
        debug(mHeaders.toString());
  
        String headers = ""; // to save the headers information
        Iterator it = mHeaders.entrySet().iterator(); // iterate through the headers Map 
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next(); // get the header name and value pairs
            //mask
            String headerName = (String) pairs.getKey();
            String headerValue = (String) pairs.getValue();
            //debug (headerName);
            //debug(headerValue);
            if(headerName.contains("user-agent")|| headerName.contains("proxy-connection") 
            		|| headerName.contains("referer") || headerValue.contains("keep-alive") 
            		|| headerValue.contains("Keep-Alive")){
                continue;
            }
            else{
                headers = headers + "\r\n" + headerName +  ": " + headerValue; // one blank line before and after the content
                //debug(headers); // my headers starts with "\r\n" so that it can be concatenated directly to request
                }
            }
        return headers;
    }
    
    
    //read in one line as a String each time
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
    	byte[] bMethod = new byte[4];
    	cIn.read(bMethod);
    	this.method = new String(bMethod).trim();
    	if(method.equals("GET")||method.equals("POST")) return true;
    	else return false;
    }   
    
    //Debug
    public void debug(Object returned){
    	if (debug) System.out.println(returned);
    }
}
  
