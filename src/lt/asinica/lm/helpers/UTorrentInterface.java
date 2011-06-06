package lt.asinica.lm.helpers;

import java.io.IOException;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.InvalidTokenException;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class UTorrentInterface extends AsyncTask<Bundle, Void, String> {
	public static final int ACTION_TEST_CONNECTIVITY = 0;
	public static final int ACTION_UPLOAD = 1;
	
	Activity main = null;
	public void setMain(Activity arg) { main = arg; }
	public UTorrentInterface() { }
	public UTorrentInterface(Activity arg) { main = arg; }
	
    protected String doInBackground(Bundle... args) {
    	String answer = null;
    	int count = args.length;
    	Bundle b = args[0];
    	int i = 0;
    	//for (int i = 0; i < count; i++) {
    	//}
    		
		if (b.containsKey("action")) {
			int action = b.getInt("action");
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main.getBaseContext());
			
			String host = prefs.getString("hostip", "");
			int port = 8080;
			try {
				port = Integer.parseInt(prefs.getString("hostport", ""));
			} catch(Exception e) { }
			String username = prefs.getString("hostusername", "");
			String password = prefs.getString("hostpassword", "");
			UTorrent torrent = UTorrent.getInstance();
			torrent.setServerInfo(host, port, username, password);
			
			switch (action) {
			case ACTION_TEST_CONNECTIVITY:
				try {
					if(torrent.test()) {
						answer = main.getString(R.string.ut_test_success);
					}
				} catch (InvalidTokenException e) {
					Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
					answer = main.getString(R.string.ut_unexpected_response);
					e.printStackTrace();					
				} catch (ClientProtocolException e) {
					Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
					answer = main.getString(R.string.ut_cant_connect)+" "+e.getMessage();
					e.printStackTrace();					
				} catch (IOException e) {
					Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
					answer = main.getString(R.string.ut_cant_connect)+" "+e.getMessage();
					e.printStackTrace();
				}				
				break;
			case ACTION_UPLOAD:
				String query = b.getString("query");				
				break;
			}
		} else {
			answer = "Malformed ASync task request. No action passed.";
		}
    		
		return answer;
    }
    protected void onPreExecute() {
    	//String tmp = main.getResources().getString(R.string.test_connectivity);
    	
    	//dialog = ProgressDialog.show(main.getApplicationContext(), "", tmp, true);
    	
    	main.setProgressBarIndeterminateVisibility(true);
    }

    protected void onPostExecute(String result) {
    	if(result!=null) {
    		Toast.makeText(main.getApplicationContext(), result, Toast.LENGTH_SHORT).show();
    	}
    	//dialog.dismiss();
    	main.setProgressBarIndeterminateVisibility(false);
    }

}