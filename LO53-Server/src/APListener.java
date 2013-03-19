/**
 * Listener used to receive the APs responses
 * @author Geoffrey Tisserand
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application Lifecycle Listener implementation class APListener
 *
 */
@WebListener
public class APListener extends Thread implements ServletContextListener {
	
	// TODO First, set up the port used to receive UDP packet from APs
	static final int LISTENING_PORT = 9999;
	
	// TODO Secondly, set up the database informations
	static final String DB_HOST = "jdbc:postgresql://localhost:5432/lo53";
	static final String DB_USER = "lo53";
	static final String DB_PASSWORD = "lo53";
	
	// TODO Thirdly, set up the PORT and the IPs of the APs
	static final int AP_PORT = 7777;
	static final byte[][] AP_IPS = {
									{(byte)192,(byte)168,(byte)1,(byte)1}
									};
	
	
    /**
     * Default constructor. 
     */
    public APListener() {
    }

    // When the thread is started
    public void run(){
		DatagramSocket socket = null;
    	try {
    		// We look for every packet on the PORT port
    		socket = new DatagramSocket(LISTENING_PORT);
    		while(true){
    			byte[] receiveData = new byte[1024];
    			DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
    			// Receive the packet
    			socket.receive(packet);
    			// Retrieve the data
    			String sentence = new String(packet.getData());
    			System.out.println("************APListener Reached***************");
    			System.out.println("PACKET RECEIVED : " + sentence);

    			// Split the data with ; 
    			String[] data = sentence.split(";");
    			
    			// If there is 7 items, this is the response for calibration
    			if (data.length == 7){
    				String x = data[1];
    				String y = data[2];
    				String mid = data[3];
    				String devicemacaddr = data[4];
    				String apmacaddr = data[5];
    				String srssival = data[6];
    				double rssival = Double.valueOf(srssival);
    				
        			System.out.println("IT'S A CALIBRATION PACKET !");
        			System.out.println("[X] = " + x);
        			System.out.println("[Y] = " + y);
        			System.out.println("[MID] = " + mid);
        			System.out.println("[DEVICE MAC ADDR] = " + devicemacaddr);
        			System.out.println("[AP MAC ADDR] = " + apmacaddr);
        			System.out.println("[RSSI VAL] = " + rssival);
    			
        			// Connection to DB to store RSSI values      			        			
        			Connection connection = null;
        			Statement statement;
        			ResultSet rs, rs2, rs3, rsk, rsk2;

        			try {
        				Class.forName("org.postgresql.Driver");
        				connection = DriverManager.getConnection(DB_HOST,DB_USER,DB_PASSWORD);
            			System.out.println("Connected to database...");
        				
            			statement = connection.createStatement();

        				// Look for the AP in the table with the apmacaddr 
        				rs = statement.executeQuery("select * from accesspoint where mac_addr='"+apmacaddr.toUpperCase()+"'");
            			System.out.println("Looking for AP in database...");
        				int apid = -1;
        				while(rs.next()){
        					apid = rs.getInt("id");
        				}
        				
        				// If no AP found add the AP to the acesspoint table
        				if (apid == -1){
            				statement.executeUpdate("insert into accesspoint(mac_addr) values('"+apmacaddr.toUpperCase()+"')", Statement.RETURN_GENERATED_KEYS);
            				rsk = statement.getGeneratedKeys();
            				while(rsk.next()){
            					apid = rsk.getInt(1);
            				}
                			System.out.println("AP not in database, added now...");
        				}else{
                			System.out.println("AP already in database...");
        				}
        				System.out.println("accesspoint(id) : " + apid);     	
        				
        				// Look for the location in the table with the coordinates 
        				rs2 = statement.executeQuery("select * from location where x='"+x+"' and y='"+y+"' and map_id='1'");
            			System.out.println("Looking for location in database...");
        				int locid = -1;
        				while(rs2.next()){
        					locid = rs2.getInt("id");
        				}
   
        				// If no location found add the coordinates to the location table
        				if (locid == -1){
            				statement.executeUpdate("insert into location(x, y, map_id) values('"+x+"', '"+y+"' , '1')", Statement.RETURN_GENERATED_KEYS);
            				rsk2 = statement.getGeneratedKeys();
            				while(rsk2.next()){
            					locid = rsk2.getInt(1);
            				}
                			System.out.println("Location not in database, added now...");
        				}else{
                			System.out.println("Location already in database");
        				}
        				System.out.println("location(id) : " + locid);     	
      
        				
        				// Look if a rssi val already exist for this AP at this location
        				rs3 = statement.executeQuery("select * from rssi where id_loc='"+locid+"' and id_ap='"+apid+"'");
            			System.out.println("Looking if rssi value exist for this AP and this location in database...");
        				int rssifound = -1;
        				while(rs3.next()){
        					rssifound = rs3.getInt("id_loc");
        				}
   
        				// If no rssival found add the rssi val to the location table
        				if (rssifound == -1){
            				statement.executeUpdate("Insert into rssi(id_loc, id_ap, avg_val, std_dev) values('"+locid+"', '"+apid+"', '"+rssival+"', '1')");
                			System.out.println("Rssi value for this AP and this location not in database, added now...");
        				}else{
        					// Else update the existing rssival
            				statement.executeUpdate("Update rssi set avg_val='"+rssival+"' where id_loc = '"+locid+"' and id_ap = '"+apid+"'");
                			System.out.println("Rssi value for this AP and this location already in database, updated now...");
        				}
        							
        			    connection.close();
        				System.out.println("Database connection closed...");

        			} catch (ClassNotFoundException e) {
        				System.out.println(e);
        			    System.out.println("[APListener] Driver Error");
        			} catch (SQLException e) {
        				System.out.println("[APListener] SQLException: " + e.getMessage());
        			}
        			
  
        		// Else if there is 4 items, this is a positioning response
    			}else if(data.length == 4){
    				String devicemacaddr = data[1];
    				String apmacaddr = data[2];
    				String srssival = data[3];
    				double rssival = Double.valueOf(srssival);

    				System.out.println("IT'S A POSITIONING PACKET !");
        			System.out.println("[DEVICE MAC ADDR] = " + data[1]);
        			System.out.println("[AP MAC ADDR] = " + data[2]);
        			System.out.println("[RSSI VAL] = " + data[3]);
        			
        			// Connection to DB to store tempRSSI values      			        			
        			Connection connection = null;
        			Statement statement;
        			ResultSet rs, rsk;

        			try {
        				Class.forName("org.postgresql.Driver");
        				connection = DriverManager.getConnection(DB_HOST,DB_USER,DB_PASSWORD);
        				statement = connection.createStatement();
        				System.out.println("Connected to database...");

        				// Look for the AP in the table with the apmacaddr
        				rs = statement.executeQuery("select * from accesspoint where mac_addr='"+apmacaddr.toUpperCase()+"'");
            			System.out.println("Looking for AP in database...");
        				int apid = -1;
        				while(rs.next()){
        					apid = rs.getInt("id");
        				}
        				
        				// If no AP found add the AP to the acesspoint table
        				if (apid == -1){
            				statement.executeUpdate("insert into accesspoint(mac_addr) values('"+apmacaddr.toUpperCase()+"')", Statement.RETURN_GENERATED_KEYS);
            				rsk = statement.getGeneratedKeys();
            				while(rsk.next()){
            					apid = rsk.getInt(1);
            				}
                   			System.out.println("AP not in database, added now...");
        				}else{
                			System.out.println("AP already in database...");
        				}
        	            		
            			System.out.println("accesspoint(id) : "+apid);

        				statement.executeUpdate("insert into temprssi(ap_id, mac_addr, avg_val) values('"+apid+"', '"+devicemacaddr.toUpperCase()+"', '"+rssival+"')");
            			System.out.println("Inserting temprssi in database...");

        			    connection.close();
        				System.out.println("Database connection closed...");

        			} catch (ClassNotFoundException e) {
        				System.out.println(e);
        			    System.out.println("[APListener] Driver Error");
        			} catch (SQLException e) {
        				System.out.println("[APListener] SQLException: " + e.getMessage());
        			}
        			
    			}else{
    				System.out.println("IT'S A SPAM PACKET ! IGNORED");
    			}
    			
    			System.out.println("***********************************************");

    		}
    	} catch (SocketException e) {
    		System.out.println("[APListener] SocketException");
    	} catch (IOException e) {
    		System.out.println("[APListener] IOException");
    	}finally{
			if (socket != null ) 	socket.close();
    	}
    	
    }       
    
   
	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0) {
    	System.out.println("APListener launched...");
    	// Starting the thread to get UDP responses
    	this.start();
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("APListener destroyed");
    }
	
}