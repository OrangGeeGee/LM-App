package lt.asinica.lm.activity;

import java.io.IOException;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Torrent;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

public class TorrentDescriptionTabs extends TabActivity {
	private ProgressDialog mProgressDialog = null;
	private Torrent mTorrent = null;
	private TabHost mTabHost;
	private Thread mInspectorThread;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    mTabHost = getTabHost();  // The activity TabHost

	    Bundle torrentBundle = getIntent().getExtras().getBundle("torrent");
	    mTorrent = new Torrent(torrentBundle);
	    Log.v("DEBUG", "Loading torrent "+mTorrent.getId());
	    mProgressDialog = ProgressDialog.show(this,    
	    		getString(R.string.please_wait), getString(R.string.lm_fetching_details),
		          true, // indeterminate
		          true, // cancalable
		          new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						if(mInspectorThread!=null && mInspectorThread.isAlive())
							mInspectorThread.interrupt();
						TorrentDescriptionTabs.this.finish();
					}
		    	});
	    
	    Runnable inspectTorrent = new Runnable() {
	    	@Override
	    	public void run() {
	    		String error = null;
	    		try {
	    			LM.getInstance().getMoreInfo(mTorrent);
	    		} catch (NotLoggedInException e) {
	    			error = getString(R.string.lm_not_logged_in);
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			error = getString(R.string.lm_no_connectivity)+" "+e.getMessage();
	    			e.printStackTrace();
	    		} finally {
	    			mProgressDialog.dismiss();
	    		}
	    		runOnUiThread(afterFetch(error));
	    	}
	    };
	    mInspectorThread =  new Thread(null, inspectTorrent, "Torrent Inspector Thread");
	    mInspectorThread.start();
	}
	@Override
	protected void onPause() {
		mProgressDialog.dismiss();
		super.onPause();
	}
    private Runnable afterFetch(final String error) {
		return new Runnable() {
			public void run() {
				if(error!=null) {
					Toast.makeText(TorrentDescriptionTabs.this, error, Toast.LENGTH_SHORT).show();
				}
				updateUI();
			}
		};
	}	
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.torrent_menu, menu);
        
        return true;
    }   	
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.download:
        	mTorrent.open(this);
            return true;
        case R.id.send_to_utorrent:
        	mTorrent.sendToUTorrent(this);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }     
    private void updateUI() {
    	try {
    		
    	    Resources res = getResources(); // Resource object to get Drawables
    	    TabHost tabHost = mTabHost;
    	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
    	    Intent intent;  // Reusable Intent for each tab
    	    Bundle torrentBundle = mTorrent.toBundle(); // Information bundle

    	    // Create an Intent to launch an Activity for the tab (to be reused)
    	    intent = new Intent()
    	    	.putExtra("torrent", torrentBundle)
    	    	.setClass(this, TorrentInfo.class);

    	    // Initialize a TabSpec for each tab and add it to the TabHost
    	    spec = tabHost.newTabSpec("info").setIndicator( res.getString(R.string.t_information),
    	                      res.getDrawable(R.drawable.ic_tab_info))
    	                  .setContent(intent);
    	    tabHost.addTab(spec);

    	    // Do the same for the other tabs
    	    intent = new Intent().setClass(this, TorrentDescription.class).putExtra("torrent", torrentBundle);
    	    spec = tabHost.newTabSpec("description").setIndicator( res.getString(R.string.t_full_description),
    	                      res.getDrawable(R.drawable.ic_tab_description))
    	                  .setContent(intent);
    	    tabHost.addTab(spec);

    	    intent = new Intent().setClass(this, TorrentComments.class).putExtra("torrent", torrentBundle);
    	    spec = tabHost.newTabSpec("comments").setIndicator( res.getString(R.string.t_comments),
    	                      res.getDrawable(R.drawable.ic_tab_comments))
    	                  .setContent(intent);
    	    tabHost.addTab(spec);
    	    tabHost.setCurrentTab(0);    		
    	} catch(Exception e) {
    		Log.e("DEBUG", "Could not update UI. "+e.getMessage());
    		e.printStackTrace();
    	}
    }

/*
	private ProgressDialog progressDialog = null;
	private Torrent torrent = null;
	
	public static final int CONTEXT_VIEW_TORRENT = 0;
	public static final int CONTEXT_OPEN_TORRENT = 1;
	public static final int CONTEXT_SEND_TO_UTORRENT = 2;

	@Override
	public Object onRetainNonConfigurationInstance() {
	    final Torrent data = torrent;
	    return data;
	}	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.torrentdescription);
	    
	    setTitle( getString(R.string.app_name)+" "+ getString(R.string.lm_single_torrent) );
	    
	    final Torrent data = (Torrent) getLastNonConfigurationInstance();
	    if(data!=null) {
	    	torrent = data;
	    	updateUI();
	    } else {

	    }
	}
       
    private void updateUI() {
    	try {
    		ImageView icon = (ImageView) findViewById(R.id.t_image);
    		LM.resolveIcon(icon, torrent.getCategory());
	    	TextView title = (TextView) findViewById(R.id.t_title);
	    	TextView dateAdded = (TextView) findViewById(R.id.t_date_added);
	    	TextView size = (TextView) findViewById(R.id.t_size);
	    	TextView peers = (TextView) findViewById(R.id.t_peers);
	    	if(torrent.isFreeLeech()) {
	    		ImageView freeLeech = (ImageView) findViewById(R.id.t_free_leech);
	    		freeLeech.setVisibility(View.VISIBLE);
	    	}
	    	//TextView fullDescription = (TextView) findViewById(R.id.t_full_description);
	    	WebView fullDescription = (WebView) findViewById(R.id.t_full_description);
	    	
	    	title.setText(torrent.getTitle());
	    	dateAdded.setText(torrent.getDateAdded());
	    	size.setText(torrent.getSize());
	    	String seeds = torrent.getSeeders();
	    	String leechs = torrent.getLeechers();
	    	//int total = Integer.parseInt(seeds) + Integer.parseInt(leechs); 
	    	String peersTxt = String.format("Seeders: %s, Leechers: %s", seeds, leechs);
	    	peers.setText(peersTxt);
	    	
	    	String data = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /></head><body>"+torrent.getFullDescription()+"</body></html>";
	    	fullDescription.loadData(Uri.encode(data), "text/html", "utf-8");
	    	//fullDescription.setText(Html.fromHtml(torrent.getFullDescription()));
    	} catch(NullPointerException e) {
    		e.printStackTrace();
    	}
    }


	public void toast(String text) {
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }	*/
}