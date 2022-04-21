package calc_backend;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.Socket;

import com.google.gson.JsonObject;

/*
 * @author: Roberto A. Ruiz de la Cruz - github.com/rardlc
 * @author: grey-box project - grey-box.ca
 * 
 * Class is responsible for handling HTTP requests via its run method.
 * 
 * Specifically, the first line will be parsed for parameters to do an arithmetic calculation
 * @param Socket Socket which holds the whole HTTP request string
 * @return Sends a response to the client with the expression, calculation, and an optional error as a JSON for display 
 */

public class RequestProcessor implements Runnable {

	private Socket socket = null;
	private OutputStream os = null;
	private BufferedReader in = null;
	private String msgToClient = "HTTP/1.1 200 OK\n" + "Server: HTTP server/0.1\n"
			+ "Access-Control-Allow-Origin: *\n\n";
	private JsonObject jsonObject = new JsonObject();

	public RequestProcessor(Socket Socket) {
		super();
		try {
			socket = Socket;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			os = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		};
	}
	
	//parses the first line for the calculation parameters
	//returns an array of strings with leftOperand at 0, operation at 1 and rightOperand at 2
	private String[] parseURLParams(String httpFirstLine) throws IOException {

		// search parameters are in index 1 after splitting, remove the trailing '/'
		httpFirstLine = httpFirstLine.split(" ")[1].substring(1);
		String[] httpParams = httpFirstLine.split("&");
		
		// store the operation parameters here
		String[] opArr = new String[3];
		boolean allParamsFound = false;
		
		for (int i=0; i <= httpParams.length; i++) {
			String[] keyValue = httpParams[i].split("=");
			
			if (keyValue[0].equals("leftOperand")) {
				opArr[0] = keyValue[1];
				
			} 
			else if (keyValue[0].equals("operation")) {
				
				if( keyValue[1].equals("%2B")) {
					opArr[1] = "+";
				}
				else if( keyValue[1].equals("%25")) {
					opArr[1] = "%";
				} 
				else if( keyValue[1].equals("%25")) {
					opArr[1] = "%";
				} 
				else if( keyValue[1].equals("%2F")) {
					opArr[1] = "/";
				}
				else {
					opArr[1] = keyValue[1];
				}
				
			} 
			else if (keyValue[0].equals("rightOperand")) {
				opArr[2] = keyValue[1];
			}
			
			if(opArr[0] != null && opArr[1] != null && opArr[2] != null ) {
				allParamsFound = true;
				break;
			}
		}
		
		if(allParamsFound) {
			return opArr;
		} else {
			throw new IOException();
		}
	}

	//calculates using the BigDecimal objects in java.math 
	// to allow for a suitable number of decimal places
	private BigDecimal calculate(String[] args) throws IOException {
		BigDecimal opResult = null;
		
		try {
			if( args[1].equals("+") ) {
				opResult = new BigDecimal(args[0]).add(new BigDecimal(args[2]));
				
			} else if( args[1].equals("-") ) {
				opResult = new BigDecimal(args[0]).subtract(new BigDecimal(args[2]));
				
			} else if( args[1].equals("*") ) {
				opResult = new BigDecimal(args[0]).multiply(new BigDecimal(args[2]));
				
			} else if( args[1].equals("/") ) {
				//decimal with a precision of at most 34 digits
				opResult = new BigDecimal(args[0]).divide(new BigDecimal(args[2]), MathContext.DECIMAL128);
				
			} else if( args[1].equals("%") ) {
				opResult = new BigDecimal(args[0]).remainder(new BigDecimal(args[2]));;
				
			} else {
				throw new IOException("ERROR - Invalid operator: " + args[1]);
			}
			
		} catch (ArithmeticException e) {
			throw new IOException( e.getMessage() );
		}
		
		return opResult;
	}
	
	//main function of the RequestProcessor, 
	//called when the socket receives an HTTP request
	public void run() {
		String httpFirstLine;

		try {
			httpFirstLine = in.readLine();
			
			//throws an IOException if all parameters are not found
			String[] opArr = parseURLParams(httpFirstLine);
			
			String expr = opArr[0] + " " + opArr[1] + " " + opArr[2];
			jsonObject.addProperty("expression", expr);
			
			//throws an IOException if the operator is not supported
			BigDecimal opResult = calculate(opArr);
			jsonObject.addProperty("result", opResult);
			
		} catch (IOException e1) {
			//implicit don't take down the server for errors
			e1.printStackTrace();
			jsonObject.addProperty("error", e1.getMessage() );
		}
		
		String res = msgToClient + jsonObject.toString();
		
		try {
			os.write(res.getBytes());
			os.flush();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
