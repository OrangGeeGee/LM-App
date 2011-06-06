package lt.asinica.lm;

import java.util.Iterator;
import java.util.Map;

import lt.asinica.lm.helpers.LMInterface;
import lt.asinica.lm.helpers.UTorrent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Window;

public class Preferences extends PreferenceActivity {
	
	OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Preference pref = Preferences.this.findPreference(key);
			
			if(pref!=null && !key.contains("password") && pref.getClass() != CheckBoxPreference.class) {
				String val = sharedPreferences.getString(key, "");
				pref.setSummary(val);
			}
			
			
		}
	};
	
	protected void onDestroy() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
        
        // add a listener for preference changes and populate list
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        Map<String, ?> allPrefs = prefs.getAll();
        Iterator<String> i= allPrefs.keySet().iterator();
        String tmp;
        Preference currPref;
        while(i.hasNext()) {
        	tmp = i.next();
        	currPref = (Preference) findPreference(tmp);
        	if(currPref!=null && !tmp.contains("password") && currPref.getClass() != CheckBoxPreference.class) {
        		currPref.setSummary( prefs.getString(tmp, "") );
        	}
        }
        
        // Get the custom preference
        Preference checkConnectivity = (Preference) findPreference("checkConnectivity");
        checkConnectivity.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
    			String host = prefs.getString("hostip", "");
    			int port = 8080;
    			try {
    				port = Integer.parseInt(prefs.getString("hostport", ""));
    			} catch(Exception e) { }
    			String username = prefs.getString("hostusername", "");
    			String password = prefs.getString("hostpassword", "");
    			UTorrent torrent = UTorrent.getInstance();
    			torrent.setServerInfo(host, port, username, password);
    			
    			Thread thread = new Thread(torrent.tester( Preferences.this ), "uTorrent Connectivity Tester Thread");
            	thread.start();
            	
            	return true;
            }
        });
        
        Preference checkLM = (Preference) findPreference("checkLM");
        checkLM.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	LMInterface lm = new LMInterface( Preferences.this );
            	Bundle b = new Bundle();
            	b.putInt("action", LMInterface.ACTION_LOGIN);
            	lm.execute(b);
            	return true;
            }
        });
        
        
        /*Preference instructions = (Preference) findPreference("instructions");
        
        instructions.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
            	AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
            	builder.setMessage("Are you sure you want to exit?")
            		.setView(arg0)
            	       .setCancelable(false)
            	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                MyActivity.this.finish();
            	           }
            	       })
            	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();
            	return true;
            }
        });*/
    }
}