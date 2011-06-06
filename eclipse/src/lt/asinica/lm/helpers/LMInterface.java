package lt.asinica.lm.helpers;

import java.io.IOException;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.BadPasswordException;
import lt.asinica.lm.exceptions.NotLoggedInException;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class LMInterface extends AsyncTask<Bundle, Void, String> {
	public static final int ACTION_LOGIN = 0;
	public static final int ACTION_BROWSE = 1;
	
	Activity main = null;
	public void setMain(Activity arg) { main = arg; }
	public LMInterface() { }
	public LMInterface(Activity arg) { main = arg; }
	
    protected String doInBackground(Bundle... args) {
    	String answer = null;
    	int count = args.length;
    	Bundle b = args[0];
    	int i = 0;
		
		if (b.containsKey("action")) {
			int action = b.getInt("action");

			switch (action) {
			case ACTION_LOGIN:
				try {
					if(LM.login()) {
						Log.v("DEBUG", "LM Login SUCCESS");		
						return main.getString(R.string.lm_login_success);
					}
				} catch (BadPasswordException e) {
					Log.v("DEBUG", "LM Login Bad Password");
					return main.getString(R.string.lm_bad_password);
				} catch(IOException e) {
					Log.v("DEBUG", "LM Login Can't connect to LM");
					return main.getString(R.string.lm_no_connectivity)+" "+e.getMessage();
				}
				break;
			case ACTION_BROWSE:
				try {
					LM.search( b.getString("query"), b.getBoolean("searchInDescriptions") );
				} catch (NotLoggedInException e) {
					return main.getString(R.string.lm_not_logged_in);						
				} catch (IOException e) {
					return main.getString(R.string.lm_no_connectivity)+" "+e.getMessage();
				}
				
				break;
			}
		}
    		
		return answer;
    	//http://[USERNAME]:[PASSWORD]@[IP]:[PORT]/gui/?action=add-url
    }
    protected void onPreExecute() {
    	//String tmp = main.getResources().getString(R.string.test_connectivity);
    	
    	//dialog = ProgressDialog.show(main.getApplicationContext(), "", tmp, true);
    	main.setProgressBarIndeterminateVisibility(true);
    }

    protected void onPostExecute(String result) {
    	if(result!=null) {
    		Toast.makeText(main, result, Toast.LENGTH_SHORT).show();
    	}
    	//dialog.dismiss();
    	main.setProgressBarIndeterminateVisibility(false);
    }

}