package lt.asinica.lm.activity;

import lt.asinica.lm.R;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Torrent;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TorrentInfo extends Activity {
	private Torrent mTorrent; 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.torrentinfo);
        
        // get info from intent
        Bundle b = getIntent().getExtras().getBundle("torrent");
        Torrent t = new Torrent( b );
        mTorrent = t;
        
    	try {
    		// update category icon
    		String catId = t.getCategory();
    		ImageView icon = (ImageView) findViewById(R.id.t_image);
    		LM.resolveIcon(icon, catId);
	    	
	    	String seeds = t.getSeeders();
	    	String leechs = t.getLeechers();
	    	//int total = Integer.parseInt(seeds) + Integer.parseInt(leechs); 
	    	String peersTxt = String.format("Seeders: %s, Leechers: %s", seeds, leechs);    		
    		
    		// short version of setting field values
    		((TextView) findViewById(R.id.t_title)).setText(t.getTitle());
	    	((TextView) findViewById(R.id.t_date_added)).setText(t.getDateAdded());
	    	((TextView) findViewById(R.id.t_size)).setText(t.getSize());
	    	((TextView) findViewById(R.id.t_peers)).setText(peersTxt);
	    	((TextView) findViewById(R.id.t_uploaded_by)).setText(t.getUploadedBy());
	    	((TextView) findViewById(R.id.t_category)).setText(t.getCategoryName());
	    	((TextView) findViewById(R.id.t_file_count)).setText(t.getFileCount());
	    	String tys = t.getThankYous();
	    	TextView tyField = (TextView) findViewById(R.id.t_thank_yous);
	    	if(tys!=null && tys.length() > 0) {
	    		tyField.setText(tys);
	    		tyField.setVisibility(View.VISIBLE);
	    	}
	    	
	    	// show or hide thank you blocks
	    	final TextView thankedField = (TextView) findViewById(R.id.t_thanked);
	    	if(t.getThanked()) {
	    		// show the text
	    		thankedField.setVisibility(View.VISIBLE);
	    	} else {
	    		// show the button
	    		final Button tyButton = (Button) findViewById(R.id.t_thank);
	    		tyButton.setVisibility(View.VISIBLE);
	    		tyButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Toast.makeText(TorrentInfo.this, R.string.t_thanks, Toast.LENGTH_SHORT).show();
						tyButton.setVisibility(View.GONE);
						thankedField.setVisibility(View.VISIBLE);
						LM.getInstance().thank(mTorrent, TorrentInfo.this);
					}
				});
	    	}
	    	
	    	
	    	// show free leech block if the torrent is freeLeech
	    	if(t.isFreeLeech()) {
	    		View freeLeech = (View) findViewById(R.id.t_free_leech);
	    		freeLeech.setVisibility(View.VISIBLE);
	    	}
	    	
	    	
	    	/*t_category 
	    	t_uploaded_by
	    	t_file_count
	    	t_download_count
	    	t_thank_yous
	    	t_thanked*/
    	} catch(Exception e) {
    		Log.e("DEBUG", "Can't load torrent info. "+e.getMessage());
    		e.printStackTrace();
    	}        
    }
}