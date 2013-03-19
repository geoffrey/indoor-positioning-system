/**
 * Positioning activity which allow to locate the user device
 * @author Geoffrey Tisserand
 */

package fr.utbm.lo53.Android;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Positioning extends Activity{
	
	// The graphic components of the app
	ImageView map;
	static ImageView map_pin;
	ToggleButton startLocatingButton;
	Button settingsButton;

    /** Called when the activity is first created. */
    @Override		
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.positioning);

        // Initializing variables 
        map = (ImageView) findViewById(R.id.map);
        
        map_pin = (ImageView) findViewById(R.id.map_pin);
        map_pin.setVisibility(View.INVISIBLE);
        
        settingsButton = (Button) findViewById(R.id.settingButton);
        
        startLocatingButton = (ToggleButton) findViewById(R.id.startLocatingButton);
        
    }
    

    // Called when toggle switch button hit
    public void startLocatingToggleSwitch(View v){
    	// If we locate
    	if (startLocatingButton.isChecked()){
			startLocatingButton.setChecked(true);

    		// Retrieve the preferences
    		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			String h = sp.getString("hostname", "default");
			int p = Integer.valueOf(sp.getString("port", "default"));
						
			// If host and port are invalid
			if (h == "" || p == 0){
				Toast.makeText(this, "Setup the server information first !", Toast.LENGTH_SHORT).show();
				startLocatingButton.setChecked(false);
				
			}else{	
				// If they are valid
		    	Toast.makeText(this, "Locating with "+h+":"+p+"...", Toast.LENGTH_SHORT).show();
				
		    	// Burst packets to APs by broadcast
		    	DatagramSocket socket = null;
				String m = "[PositioningApp] BURSTFROMHELL";
				try {
					socket = new DatagramSocket(Main.BURST_PORT);
					long begin = System.currentTimeMillis();
					while((System.currentTimeMillis() - begin) < Main.BURST_TIME){
						InetAddress address = InetAddress.getByAddress(Main.BURST_ADDRESS);
						DatagramPacket packet = new DatagramPacket(m.getBytes(), m.length(), address, Main.BURST_PORT);
						socket.send(packet);
					}
			    	Toast.makeText(this, "Burst to APs", Toast.LENGTH_SHORT).show();
				} catch (SocketException e1) {
					e1.printStackTrace();
			    	Toast.makeText(this, "Socket Exception !", Toast.LENGTH_SHORT).show();
				} catch (UnknownHostException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "Unknown Host Exception !", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "Burst IOException", Toast.LENGTH_SHORT).show();
				}finally{
					if (socket != null) socket.close();
				}
				
				// Locate request
				try {
					HttpClient httpClient = new DefaultHttpClient();
		    		HttpGet request = new HttpGet("http://"+h+":"+p+"/LO53-Server/Positioning");
					// Retrieve the response
		    		HttpResponse response = httpClient.execute(request);
		    		HttpEntity entity = response.getEntity();
		    		if (entity != null){
		    			String responseString = EntityUtils.toString(entity);
		    			System.out.println(responseString);
		    			responseString = responseString.trim();
		    			String[] coord = responseString.split(",");
		    		
		    			// Find the coordinates inside the response
		    			if(coord.length ==  2){
		    				int x = Integer.valueOf(coord[0]);
			    			int y = Integer.valueOf(coord[1]);
				    		
			    			// Display the location on the map
			    			Positioning.setLocation(x,y);
					    	Toast.makeText(this, "Location found @("+x+","+y+")", Toast.LENGTH_SHORT).show();

		    			}else{
		    				Toast.makeText(this, "No location found !", Toast.LENGTH_SHORT).show();
		    			}
		    		}
	    			
				} catch (ClientProtocolException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "Protocol exception", Toast.LENGTH_SHORT).show();

				} catch (IOException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "IOException", Toast.LENGTH_SHORT).show();
				}
				
    			startLocatingButton.setChecked(false);
			}
    	}else{
    		Toast.makeText(this, "End locating", Toast.LENGTH_SHORT).show();
    	}
    }
    
    // To set the pin on the map with it's coordinates
    public static void setLocation(int x, int y){
    	map_pin.setPadding(x-8, y-28,0,0);
		map_pin.setVisibility(View.VISIBLE);
		map_pin.bringToFront();
    }
	
    // Call the settings activity when settings button is hit
	public void settings(View v){
		 Intent intent = new Intent(this, Settings.class);
		 startActivity(intent);
	}
}