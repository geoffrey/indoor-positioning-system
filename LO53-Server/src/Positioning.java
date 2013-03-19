/**
 * Servlet used to get the location of the device in the room
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class Positioning
 */
@WebServlet("/Positioning")
public class Positioning extends HttpServlet {
	private static final long serialVersionUID = 1L;
		
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Positioning() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter outresponse = response.getWriter();
		boolean coordsent = false;

		// Retrieve the IP address of the request device
		String requestip = request.getRemoteAddr();
		System.out.println("************Positioning Servlet Reached***************");
		System.out.println("Request from " + requestip + " received");
		System.out.println("Looking inside arp table for ip "+requestip+"...");

		// Open the ARP table
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
		
		// If we found a MAC Address we request APs for RSSI
		if (macAddr != null){
			System.out.println ("MAC Address found : "+macAddr);
			
			// Clean up the tempRSSI table
			Connection connection = null;
			Statement statement;
			ResultSet rs;

			try {
				Class.forName("org.postgresql.Driver");
				connection = DriverManager.getConnection(APListener.DB_HOST,APListener.DB_USER,APListener.DB_PASSWORD);
				System.out.println("Connected to database...");
				System.out.println("Cleaning temprssi table...");
				statement = connection.createStatement();

				statement.executeUpdate("delete from temprssi");
				System.out.println("Tempsrssi table clean...");

			    connection.close();
				System.out.println("Database connection closed...");

			} catch (ClassNotFoundException e) {
			    System.out.println("[Positioning Servlet:Cleaning tempsrssi table] Driver Error");
			} catch (SQLException e) {
				System.out.println("[Positioning Servlet:Cleaning tempsrssi table] SQLException: " + e.getMessage());
			}
			
			
			// Send Get request to all APs	
			String msg = "GET;"+macAddr;
			DatagramSocket socket = null;
			try{
				for (int i = 0 ; i < APListener.AP_IPS.length ; i++){
					InetAddress address = InetAddress.getByAddress(APListener.AP_IPS[i]) ;
					socket = new DatagramSocket(APListener.AP_PORT);
					DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), address, APListener.AP_PORT);
					socket.send(packet);
					System.out.println("Sending "+msg+" to AP...");
				}
				socket.close();
			} catch (IOException iox){
			    System.out.println("[Positioning Servlet:Sending GET message to APs] IOException");
			}finally{
				if (socket != null) socket.close();
			}

			// Sleep during 500ms
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			// Connection to DB to retrieve tempRSSI values stored by the APListener 	
			ResultSet rs2, rs3, rs4;
			try {
				Class.forName("org.postgresql.Driver");
				connection = DriverManager.getConnection(APListener.DB_HOST,APListener.DB_USER,APListener.DB_PASSWORD);
				System.out.println("Connected to database...");
				statement = connection.createStatement();
				
				rs2 = statement.executeQuery("Select * from temprssi where mac_addr='"+macAddr.toUpperCase()+"'");
				System.out.println("Retrieve new temprssi...");
				int nbtemprssi = 0;
				while (rs2.next()) {
					nbtemprssi++;
					System.out.println("RSSI found !");
					System.out.println("APID : "+rs2.getInt("ap_id"));
					System.out.println("CLIENTMAC : "+rs2.getString("mac_addr"));
					System.out.println("AVGVAL : "+rs2.getDouble("avg_val"));
				}
				
				// If there is enough data in temprssi we can try to locate the device assuming there is also data in rssi data
				if (nbtemprssi != 0){
					rs3 = statement.executeQuery("Select id_loc from temprssi inner join rssi on temprssi.ap_id = rssi.id_ap where temprssi.mac_addr='"+macAddr.toUpperCase()+"' group by 1 order by (avg(abs(rssi.avg_val - temprssi.avg_val))) asc limit 1");
					System.out.println("Retrieve nearest loc...");
					int locid = -1;
					while (rs3.next()){
						locid = rs3.getInt("id_loc");
					}
					
					// If we found a location
					if (locid != -1){
						System.out.println("Nearest loc retrieved !");
						
						// We retrieve the coordinates of this location
						rs4 = statement.executeQuery("Select x, y from location where id='"+locid+"'");
						int foundx = -1;
						int foundy = -1;
						while (rs4.next()){
							foundx = rs4.getInt("x");
							foundy = rs4.getInt("y");

						}
						
						if (foundx != -1 && foundy != -1){
							System.out.println("location found @("+foundx+","+foundy+")");
							outresponse.println(foundx+","+foundy);
							coordsent = true;

						}
					}else{
						System.out.println("Nearest loc not found...");
					}
					
				}else{
					System.out.println("Not enough data to locate !");
				}
				
	
			    connection.close();
				System.out.println("Database connection closed...");

			} catch (ClassNotFoundException e) {
			    System.out.println("[Positioning Servlet:Retrieving new temprssi values] Driver Error");
			} catch (SQLException e) {
				System.out.println("[Positioning Servlet:Retrieving new temprssi values] SQLException: " + e.getMessage());
			}
			
		}else{
			System.out.println("MAC Address not found !");
		}
		
		// If we did not manage to locate the device we send random locaton
		if (!coordsent){
			int x = (int) (Math.random()*500) ;
			int y = (int) (Math.random()*500) ;
	
			outresponse.println(x+","+y);
			System.out.println("Random coordinates sent !");
		}
		System.out.println("********************************************");


	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

}