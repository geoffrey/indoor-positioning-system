/**
 * Servlet used to calibrate our positioning system
 * @author Geoffrey Tisserand
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Calibration
 */
@WebServlet("/Calibration")
public class Calibration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Calibration() {

        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter outresponse = response.getWriter();
		
		// Retrieve the request IP
		String requestip = request.getRemoteAddr();
		System.out.println("************Calibration Servlet Reached***************");
		System.out.println("Request from " + requestip + " received");
		
		// Extract coordinates from GET request
		int x = -1; int y = -1;
		try{
			x = Integer.valueOf(request.getParameter("x"));
			y = Integer.valueOf(request.getParameter("y"));
		}catch (NumberFormatException nfe){
			System.out.println("Wrong parameters. You must use x and y as int parameters to request this servlet");
		}
		
		// If we received the coordinates and found the mac addr
		if (x != -1 && y != -1){
			System.out.println("Coordinates found : ("+x+","+y+")");	
		
			// Open the ARP table too look up for this IP
			System.out.println("Looking inside arp table for ip "+requestip+"...");
			BufferedReader in = new BufferedReader(new FileReader("/proc/net/arp"));
			String strLine;
			String macAddr = null;
			
			// Read File Line By Line
			while ((strLine = in.readLine()) != null){
				// Find the matching IP
				System.out.println(strLine);
				if (strLine.contains(requestip)){
					// Split the line
					String[] table = strLine.split(" ");	
					// Retrieve the MAC Address
					//macAddr = table[24];
					for (int i = 0 ; i < table.length ; i++){
						if (table[i].length() == 17)
							macAddr = table[i];
						//System.out.println("Table["+i+"] : "+table[i]);
					}
				}
			}
			
			
			// TODO fake macaddr if not found
			//if (macAddr == null)macAddr = "toto";
			
			if (macAddr != null){
				System.out.println("MAC Address found : "+macAddr);
				
				// Send get message to all APs
				String msg = "GET;"+x+";"+y+";1;"+macAddr;
				DatagramSocket socket = null;
				try{
					for (int i = 0 ; i < APListener.AP_IPS.length ; i++){
						InetAddress address = InetAddress.getByAddress(APListener.AP_IPS[i]);
						socket = new DatagramSocket(APListener.AP_PORT);
						DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), address, APListener.AP_PORT);
						socket.send(packet);
						System.out.println("Sending "+msg+" to AP...");
					}
					socket.close();
				} catch (IOException iox){
				    System.out.println(iox+" [Calibration Servlet] IOException");
				}finally{
					if (socket != null) socket.close();
				}
			}else{
				System.out.println("MAC Address not found !");
			}
			
		}else{
			System.out.println("Coordinates not found");
		}
		outresponse.println(x+","+y);
		System.out.println("********************************************");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

}