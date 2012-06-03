package lt.asinica.lm.activity;

import lt.asinica.lm.R;
import lt.asinica.lm.objects.Torrent;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

public class TorrentDescription extends Activity {
	private Torrent mTorrent; 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.torrentdescription);
        
        // get info from intent
        mTorrent = new Torrent( getIntent().getExtras().getBundle("torrent") );
        
        
        // find the description webview
    	WebView fullDescription = (WebView) findViewById(R.id.t_full_description);
    	
    	// assign it
    	String data = 
    		"<html>" +
    			"<head>" +
    			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
    			"</head>" +
    			"<body>"+mTorrent.getFullDescription()+"</body>" +
			"</html>";
    	
    	fullDescription.loadData(Uri.encode(data), "text/html", "utf-8");
    }
}