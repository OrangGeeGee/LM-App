package lt.asinica.lm;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import lt.asinica.lm.helpers.DownloadProgressUpdater;
import lt.asinica.lm.helpers.LM;
import lt.asinica.lm.helpers.UTorrent;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class LMApp extends Application{

	private static LMApp instance;
	public static LMApp getInstance() {
		if(instance == null)
			instance = new LMApp();
		return instance;
	}
	
    private static Context context;

    public LMApp() {
    	super();
    	instance = this; 
    }
    
    public void onCreate(){
    	super.onCreate();
        LMApp.context=getApplicationContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
        String host = prefs.getString("hostip", "");
		int port = 8080;
		try {
			port = Integer.parseInt(prefs.getString("hostport", ""));
		} catch (Exception e) {
			
		}
		String username = prefs.getString("hostusername", "");
		String password = prefs.getString("hostpassword", "");
		UTorrent torrent = UTorrent.getInstance();
		torrent.setServerInfo(host, port, username, password);
    }

    public static Context getDefaultContext() {
    	return context;
    }
}