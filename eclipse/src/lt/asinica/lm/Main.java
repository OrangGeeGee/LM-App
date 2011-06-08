package lt.asinica.lm;

import java.io.IOException;

import lt.asinica.lm.exceptions.BadPasswordException;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Updater;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class Main extends Activity {
	private boolean mMenuEnabled = true;
	private SharedPreferences mPreferences;
	private ProgressDialog mProgressDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	
    	mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    	// if not logged in
    	if(!mPreferences.contains("lmsecret") || mPreferences.getString("lmsecret", "").length() == 0 ) {
    		initLogin();
    	} else {//if logged in
    		initSearch();
    	}
    	
    	if(mPreferences.getBoolean("autoupdatecheck", true))
    		 Updater.getInstance().checkForNewVersion();
    }
    private void initSearch() {
    	findViewById(R.id.main_logged_in).setVisibility(View.VISIBLE);
    	findViewById(R.id.main_logged_out).setVisibility(View.GONE);
    	
    	EditText searchField = (EditText) findViewById(R.id.search);
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
    	
		Button myButton = (Button) findViewById(R.id.fetch);
    	myButton.setOnClickListener(this.btnListener);
    	
    	// TODO Informacijos sekcijos perdarymas á naujas funkcijas arba prasminæ info. 
    	
    	// display button at bottom to open prefs if uTorrent host is unspecified
    	if( !mPreferences.contains("hostip") || mPreferences.getString("hostip", "").length() == 0 ) {
    		// make layout part with the button and text visible
    		((View) findViewById(R.id.main_utorrent_not_set_up)).setVisibility(View.VISIBLE);
    		
			Button oSettings = (Button) findViewById(R.id.open_settings);
			oSettings.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent(Main.this, Preferences.class);
					startActivity(i);
				}
			});
    	}
    }
    private void initLogin() {
    	// disable menu inflater
    	mMenuEnabled = false;
    	// display the logged out frame and hide the logged in frame
    	findViewById(R.id.main_logged_out).setVisibility(View.VISIBLE);
    	findViewById(R.id.main_logged_in).setVisibility(View.GONE);
    	
    	Button loginButton = (Button) findViewById(R.id.login_action);
    	final EditText username = (EditText) findViewById(R.id.login_username);
    	final EditText password = (EditText) findViewById(R.id.login_password);
    	
    	OnClickListener loginListener = new OnClickListener() {
			public void onClick(View v) {
				String u = username.getText().toString();
				String p = password.getText().toString();
				Thread loginThread = new Thread(new Login(u, p), "LM Login Thread");
				mProgressDialog = ProgressDialog.show(Main.this, getString(R.string.please_wait), getString(R.string.lm_logging_in), true);
				loginThread.start();
			}
		};
		
    	loginButton.setOnClickListener(loginListener);
    }
    
    private class Login implements Runnable {
    	String mUsername;
    	String mPassword;
    	public Login(String username, String password) {
    		mUsername = username;
    		mPassword = password;
    	}
		public void run() {
			String msg = null;
			boolean success = false;
			try {
				String lmsecret = LM.getInstance().login(mUsername, mPassword);
				Editor editor = mPreferences.edit();
				editor.putString("lmsecret", lmsecret);
				editor.commit();
				success = true;
			} catch (BadPasswordException e) {
				msg = getString(R.string.lm_bad_password);
				Log.e("DEBUG", msg);
				e.printStackTrace();
			} catch (IOException e) {
				msg = getString(R.string.lm_no_connectivity)+" "+e.getMessage();
				Log.e("DEBUG", msg);
				e.printStackTrace();
			}
			runOnUiThread(new AfterLogin(msg, success));
		}
    }
    
    private class AfterLogin implements Runnable {
    	private String mMessage;
    	private boolean mSuccess;
    	public AfterLogin(String msg, boolean suc) {
    		mMessage = msg;
    		mSuccess = suc;
    	}
    	
		public void run() {
			mProgressDialog.dismiss();
			if(mMessage!=null) {
				toast(mMessage);
			}
			if(mSuccess) {
				LMApp.restart();
			}
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if(mMenuEnabled) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.menu, menu);
    	}
    	return true;
    }   
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_settings:
        	Intent settingsActivity = new Intent(getBaseContext(),
        			Preferences.class);
        	startActivity(settingsActivity);
            return true;
        case R.id.menu_update:
        	Updater.getInstance().forceCheck();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    

    
    
    public void toast(CharSequence text) {
    	Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }
    
	/**
     * button event listener
     */
    private OnClickListener btnListener = new OnClickListener() {
        public void onClick(View v) {
        	//httpget.setHeader("Cookie", "login=OTI3NDQ6TXpFMVptVXdaamxtWTJZM1pHTTNaV1ZpTURsbVpXVTNNekJsTUdGalpUaz0%3D;");
				//toast(e.getMessage());
        	
        	EditText searchField = (EditText) findViewById(R.id.search);
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        	
        	CheckBox searchInDescriptions = (CheckBox) Main.this.findViewById(R.id.search_in_descriptions);
        	//search_in_descriptions
        	Intent intent = new Intent(getBaseContext(), TorrentList.class);
        	intent.putExtra("searchInDescriptions", searchInDescriptions.isChecked());
        	intent.putExtra("query", searchField.getText().toString());
        	startActivity(intent);
        }
    };
    
}