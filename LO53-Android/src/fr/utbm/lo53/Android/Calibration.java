/**
 * SetUpTool activity which allow the user to calibrate the system by building a rssi map
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Calibration extends Activity implements OnTouchListener{
	
	// The graphic components of the app
	ToggleButton setPoint;
	ImageView map;
	ImageView map_pin;
	Button measure;
	Button cancel;
	Boolean activeMode;
	
	// Location x, y on the map
	int mapx;
	int mapy;
	
	// Map dimension in pixel
	int mapwidth;
	int mapheight;
	
	// Location x, y in the actual room in cm
	int roomx;
	int roomy;
	
	// Room dimension in cm
	int roomwidth = 300;
	int roomheight = 600;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setuptool);

        // Initializing variables 
        setPoint = (ToggleButton) findViewById(R.id.setPointButton);
        map = (ImageView) findViewById(R.id.map);
        map.setOnTouchListener(this);

        map_pin = (ImageView) findViewById(R.id.map_pin);
        map_pin.setVisibility(View.INVISIBLE);
        
        measure = (Button) findViewById(R.id.measureButton);
        cancel = (Button) findViewById(R.id.cancelButton);
        
        // Hide active mode buttons
        measure.setVisibility(View.INVISIBLE);
        cancel.setVisibility(View.INVISIBLE);
        
        // Turn off active mode  
        activeMode = false;       
    }
    
    public void setPointToggleSwitch(View v){
    	// When mode switches
    	if (setPoint.isChecked()){
    		// If turning on
    		measure.setVisibility(View.VISIBLE);
    		cancel.setVisibility(View.VISIBLE);
    		activeMode = true;
    	}else{
    		// If turning off
    		measure.setVisibility(View.INVISIBLE);
    		cancel.setVisibility(View.INVISIBLE);
    		activeMode = false;
    	}
    }
    
	// When touching Cancel button
    public void cancelActiveMode(View v){
    	// turn off active mode
    	activeMode = false;
    	
    	// hide active mode buttons
    	measure.setVisibility(View.INVISIBLE);
		cancel.setVisibility(View.INVISIBLE);
		
		// turn off toggle switch
		setPoint.setChecked(false);
    	
    }

    // When touching the map
    public boolean onTouch(View v, MotionEvent event) {
    	// If we are in active mode
    	if (activeMode){
	    	// Get the dimensions of the map
	        mapwidth = map.getMeasuredWidth();
	        mapheight = map.getMeasuredHeight();
			
			// retrieve event coordinates
			mapx = (int)event.getX();
			mapy = (int)event.getY();
			
			// Display the map_pin to the event location (substract map_pin dimensions)
			map_pin.setPadding(mapx-8, mapy-28,0,0);
			map_pin.setVisibility(View.VISIBLE);
			map_pin.bringToFront();
			
			// Compute actual location in the room in cm
			roomx = mapwidth * mapx / roomwidth;
			roomy = mapheight * mapy / roomheight;
						
			Toast.makeText(this, "("+mapx+","+mapy+")", Toast.LENGTH_SHORT).show();
		}
		return false;
	}
	
    // When touching setting button
	public void settings(View v){
		// Start new activity settings.java
		Intent intent = new Intent(this, Settings.class);
		startActivity(intent);
	}
	
	// When touching measure button
	public void measure(View v){
		// If a pin is set on the map
		if (mapx != 0 || mapy != 0){
			
			// Retrieve preferences
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			String h = sp.getString("hostname", "default");
			int p = Integer.valueOf(sp.getString("port", "default"));
			
			// If preferences are set correctly
			if (h != "" || p != 0){
				// Burst data to all APs
		    	DatagramSocket socket = null;
				String m = "[SetupToolApp] BURSTFROMHELL";
				try {
					socket = new DatagramSocket(Main.BURST_PORT);
					long begin = System.currentTimeMillis();
					// Burst during BURST_TIME ms
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
				
				// Send location to the server using http get 
				try {
					HttpClient httpClient = new DefaultHttpClient();
					HttpGet request = new HttpGet("http://"+h+":"+p+"/LO53-Server/Calibration?x="+mapx+"&y="+mapy);
					HttpResponse response = httpClient.execute(request);
		    		HttpEntity entity = response.getEntity();

		    		Toast.makeText(this, "\"MEASURE;"+mapx+";"+mapy+";1\" sent to "+h+":"+p+"...", Toast.LENGTH_SHORT).show();

		    		if (entity != null){
		    			// Ensure coordinates are correctly received by looking into server response
		    			String responseString = EntityUtils.toString(entity);
		    			System.out.println(responseString);
		    			responseString = responseString.trim();
		    			String[] coord = responseString.split(",");
		    			
		    			// If the server response is correct
		    			if(coord.length == 2){
		    				// Retrieve coordinates in the response 
		    				int x = Integer.valueOf(coord[0]);
			    			int y = Integer.valueOf(coord[1]);
					    	Toast.makeText(this, "Server received ("+x+","+y+")", Toast.LENGTH_SHORT).show();
		    			}else{
		    				Toast.makeText(this, "Server did not received coordinates !", Toast.LENGTH_SHORT).show();
		    			}
		    		}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "Protocol exception", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					e.printStackTrace();
			    	Toast.makeText(this, "IOException", Toast.LENGTH_SHORT).show();
				}
			}else{	
				Toast.makeText(this, "Setup the server information first !", Toast.LENGTH_SHORT).show();
			}
		}else{
			Toast.makeText(this, "Select a position on the map first !", Toast.LENGTH_SHORT).show();
		}		
	}
}