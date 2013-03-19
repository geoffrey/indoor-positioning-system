/**
 * Settings activity which allows the user to set the hostname and the port of the server
 * @author Geoffrey Tisserand
 */

package fr.utbm.lo53.Android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
	
    /** Called when the activity is first created. */
   
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
