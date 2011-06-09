package lt.asinica.lm.activity;

import java.io.IOException;
import java.util.List;

import lt.asinica.lm.LMApp;
import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.BadPasswordException;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Search;
import lt.asinica.lm.objects.Updater;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
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
    	
    	EditText searchField = (EditText) findViewById(R.id.main_search);
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
    	
		Button searchButton = (Button) findViewById(R.id.main_fetch);
    	searchButton.setOnClickListener(this.btnListener);
    	
    	Button detailedButton = (Button) findViewById(R.id.main_detailed_search);
    	detailedButton.setOnClickListener(this.detailedListener);
    	
    	LM.getInstance().initSearch();
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
        case R.id.menu_report:
        	collectAndSendLog();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    

    public void toast(CharSequence text) {
    	Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }
    
	/**
     * Main search button event listener
     */
    private OnClickListener btnListener = new OnClickListener() {
        public void onClick(View v) {
        	//httpget.setHeader("Cookie", "login=OTI3NDQ6TXpFMVptVXdaamxtWTJZM1pHTTNaV1ZpTURsbVpXVTNNekJsTUdGalpUaz0%3D;");
				//toast(e.getMessage());
        	
        	EditText searchField = (EditText) findViewById(R.id.main_search);
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        	
        	CheckBox searchInDescriptions = (CheckBox) Main.this.findViewById(R.id.search_in_descriptions);
        	
        	// init search object
        	Search search = new Search(searchField.getText().toString(), searchInDescriptions.isChecked());
        	startNewSearch(search);
        }
    };
	/**
     * Detailed search button event listener
     */
    private OnClickListener detailedListener = new OnClickListener() {
        public void onClick(View v) {

        	EditText searchField = (EditText) findViewById(R.id.main_search);
        	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        	imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        	
        	CheckBox searchInDescriptions = (CheckBox) Main.this.findViewById(R.id.search_in_descriptions);
        	
        	// init search object
        	Search search = new Search(searchField.getText().toString(), searchInDescriptions.isChecked());
        	
        	DetailedSearchActivity.startNew(Main.this, search.toBundle(), DetailedSearchActivity.VIEW_FOR_DETAILED_SEARCH);
        }
    };
        
    
    public void startNewSearch(Search search) {
    	startNewSearch(search.toBundle());
    }
    public void startNewSearch(Bundle search) {
    	Intent intent = new Intent(getBaseContext(), TorrentList.class);
    	intent.putExtra("search", search);
    	startActivity(intent);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode == RESULT_OK) {
    		startNewSearch( data.getExtras().getBundle("search") );
    	}
    }    
    
    
    // log collector snippet
    public static final String LOG_COLLECTOR_PACKAGE_NAME = "com.xtralogic.android.logcollector";//$NON-NLS-1$
    public static final String ACTION_SEND_LOG = "com.xtralogic.logcollector.intent.action.SEND_LOG";//$NON-NLS-1$
    public static final String EXTRA_SEND_INTENT_ACTION = "com.xtralogic.logcollector.intent.extra.SEND_INTENT_ACTION";//$NON-NLS-1$
    public static final String EXTRA_DATA = "com.xtralogic.logcollector.intent.extra.DATA";//$NON-NLS-1$
    public static final String EXTRA_ADDITIONAL_INFO = "com.xtralogic.logcollector.intent.extra.ADDITIONAL_INFO";//$NON-NLS-1$
    public static final String EXTRA_SHOW_UI = "com.xtralogic.logcollector.intent.extra.SHOW_UI";//$NON-NLS-1$
    public static final String EXTRA_FILTER_SPECS = "com.xtralogic.logcollector.intent.extra.FILTER_SPECS";//$NON-NLS-1$
    public static final String EXTRA_FORMAT = "com.xtralogic.logcollector.intent.extra.FORMAT";//$NON-NLS-1$
    public static final String EXTRA_BUFFER = "com.xtralogic.logcollector.intent.extra.BUFFER";//$NON-NLS-1$	    
    
    void collectAndSendLog(){
        final PackageManager packageManager = getPackageManager();
        final Intent intent = new Intent(ACTION_SEND_LOG);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        final boolean isInstalled = list.size() > 0;
        
        if (!isInstalled){
            new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage("Ásiraðykite nemokamà open source programëlæ Log Collector, kad galëtumët iðsiøsti informacijà apie savo telefono klaidas ðios aplikacijos kûrëjui.")
            		//"Install the free and open source Log Collector application to collect the device log and send it to the developer.")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + LOG_COLLECTOR_PACKAGE_NAME));
                    marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(marketIntent); 
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
        else{
        	final String email = "antanas.sinica@gmail.com";
            new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage("Paleisti Log Collector aplikacijà.\n" +
            		"Ji surinks info apie jûsø telefone vykusias klaidas ir iðsiøs el. paðtu "+email+".\n"+
            		"Prieð iðsiunèiant bus galimybë perþiûrëti ir pakeisti iðsiunèiamus duomenis.")
	//"Run Log Collector application.\nIt will collect the device log and send it to "+email+".\nYou will have an opportunity to review and modify the data being sent."
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EXTRA_SEND_INTENT_ACTION, Intent.ACTION_SENDTO);
                    intent.putExtra(EXTRA_DATA, Uri.parse("mailto:" + email));
                    Main main = Main.this;
                    String version = "";
                    try {
                    	PackageInfo packageInfo = packageManager.getPackageInfo(main.getPackageName(), 0);
                    	version = packageInfo.versionName;
                    } catch(Exception e) { e.printStackTrace(); }

                    String info =
                    	"LM App version: "+version+"\n" +
                		"Device model: "+Build.MODEL+"\n"+
                		"Firmware version: "+Build.VERSION.RELEASE+" (SDK "+Build.VERSION.SDK+")\n"+
                		"Build number: "+Build.DISPLAY+"\n\n";
                		/*"Kernel version: 2.6.32.39-franco.Kernel.v14.1\n"+
                		"francisco@francisco-linux )\n"+
                		"#1 PREEMPT Tue May 31 01:27:10 WEST 2011\n"+*/
                		intent.putExtra(EXTRA_ADDITIONAL_INFO, info);
                		intent.putExtra(Intent.EXTRA_SUBJECT, "[LM App] Application failure report");
                    
                    intent.putExtra(EXTRA_FORMAT, "time");
                    
                    //The log can be filtered to contain data relevant only to your app
                    /*String[] filterSpecs = new String[3];
                    filterSpecs[0] = "AndroidRuntime:E";
                    filterSpecs[1] = TAG + ":V";
                    filterSpecs[2] = "*:S";
                    intent.putExtra(EXTRA_FILTER_SPECS, filterSpecs);*/
                    
                    startActivity(intent);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
    }    
}