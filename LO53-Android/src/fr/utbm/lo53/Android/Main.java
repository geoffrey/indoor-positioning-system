/**
 * Main activity used to launch the tools
 * @author Geoffrey Tisserand
 */
package fr.utbm.lo53.Android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Main extends Activity {
	
	// TODO Define the port, the address and the time of burst first !
	static final int BURST_PORT = 7331;
	static final byte[] BURST_ADDRESS = {(byte)255,(byte)255,(byte)255,(byte)255};
	static final int BURST_TIME = 10; 
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    // Call the settings activity when settings button is hit
   	public void settings(View v){
   		 Intent intent = new Intent(this, Settings.class);
   		 startActivity(intent);
   	}
   	
    // Call the positioning activity when positioning button is hit
   	public void positioning(View v){
   		 Intent intent = new Intent(this, Positioning.class);
   		 startActivity(intent);
   	}
   	
    // Call the setuptool activity when setuptool button is hit
   	public void setuptool(View v){
   		 Intent intent = new Intent(this, Calibration.class);
   		 startActivity(intent);
   	}
}