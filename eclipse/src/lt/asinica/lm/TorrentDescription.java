package lt.asinica.lm;

import java.io.IOException;

import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Torrent;
import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TorrentDescription extends Activity {

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
		    torrent = new Torrent();
		    torrent.unpack( getIntent().getExtras().getBundle("torrent") );	    	
		    progressDialog = ProgressDialog.show(TorrentDescription.this,    
		    		getString(R.string.please_wait), getString(R.string.lm_fetching_details), true);
		    
		    Runnable inspectTorrent = new Runnable() {
		    	@Override
		    	public void run() {
		    		String error = null;
		    		try {
		    			LM.getInstance().getMoreInfo(torrent);
		    		} catch (NotLoggedInException e) {
		    			error = getString(R.string.lm_not_logged_in);
		    			e.printStackTrace();
		    		} catch (IOException e) {
		    			error = getString(R.string.lm_no_connectivity)+" "+e.getMessage();
		    			e.printStackTrace();
		    		} finally {
		    			progressDialog.dismiss();
		    		}
		    		runOnUiThread(afterFetch(error));
		    	}
		    };
		    
		    Thread thread =  new Thread(null, inspectTorrent, "Torrent Inspector Thread");
		    thread.start();
	    }
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
        	torrent.open(this);
            return true;
        case R.id.send_to_utorrent:
        	torrent.sendToUTorrent(this);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }        
	
    private Runnable afterFetch(final String error) {
		return new Runnable() {
			public void run() {
				if(error!=null) {
					toast(error);
				}
				updateUI();
			}
		};
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
    }	
}