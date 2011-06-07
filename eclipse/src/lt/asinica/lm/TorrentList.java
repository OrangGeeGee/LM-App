package lt.asinica.lm;

import java.io.IOException;
import java.util.Iterator;

import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.helpers.LM;
import lt.asinica.lm.helpers.ListPack;
import lt.asinica.lm.helpers.Torrents;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TorrentList extends ListActivity implements OnScrollListener {

	private ProgressDialog progressDialog = null;
	private TorrentAdapter listAdapter;
	private ListView listView;
	private Torrents torrents = new Torrents();
	private Torrents lastResult = null;
	private int page = 0;
	private String query;
	private boolean inDescriptions;
	private String title;
	private boolean everythingLoaded = false;
	private boolean loadingMore = false;
	private Thread mTorrentLoaderThread;
	View footerView;
	
	public static final int CONTEXT_VIEW_TORRENT = 0;
	public static final int CONTEXT_OPEN_TORRENT = 1;
	public static final int CONTEXT_SEND_TO_UTORRENT = 2;

	public ListPack pack() {
		return new ListPack(torrents, query, page, title, inDescriptions, everythingLoaded, loadingMore);
	}
	public void unpack(ListPack p) {
		torrents = p.getTorrents();
		query = p.getQuery();
		page = p.getPage();
		title = p.getTitle();
		inDescriptions = p.getInDescriptions();
		everythingLoaded = p.getEverythingLoaded();
		loadingMore = false;//p.getLoadingMore();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	    final ListPack data = pack();
	    return data;
	}	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.torrentlist);
	    listView = getListView();
		listView.setOnItemClickListener(clickListener);
	    registerForContextMenu(listView);
	    // add the footer before adding the adapter, else the footer will not load!
	    
	    final ListPack data = (ListPack) getLastNonConfigurationInstance();
	    if (data != null) {
	    	unpack(data);
	    } else {
		    Bundle extras = getIntent().getExtras();
		    query = extras.getString("query");
		    inDescriptions = extras.getBoolean("searchInDescriptions",false);
		    title = (
		    	query.length()>0 ?
		    			String.format( getString(R.string.lm_search_results), query ) :
		    			getString(R.string.lm_all_torrents) );
		    
            getTorrents(query, page);
		    progressDialog = ProgressDialog.show(TorrentList.this,    
		          getString(R.string.please_wait), getString(R.string.lm_searching), true);
	    }
	    if(!everythingLoaded) {
		    footerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.helper, null, false);
		    listView.addFooterView(footerView, null, false);
	    }
	    setTitle(getString(R.string.app_name)+" "+title);
	    lastResult = new Torrents();
	    listAdapter = new TorrentAdapter(this, R.layout.torrentlistrow, lastResult);
	    setListAdapter(listAdapter);
	    if(data!=null) {
	    	Iterator<Torrent> i = torrents.iterator();
	    	while(i.hasNext())
	    		listAdapter.add(i.next());
	    	listAdapter.notifyDataSetChanged();
	    }
	    listView.setOnScrollListener(TorrentList.this);
	}
	private Runnable returnRes(final String error) {
		return new Runnable() {
		    public void run() {
		        if(lastResult != null && lastResult.size() > 0){
		        	torrents.addAll(lastResult);
		            listAdapter.notifyDataSetChanged();
			    	Iterator<Torrent> i = lastResult.iterator();
			    	while(i.hasNext())
			    		listAdapter.add(i.next());		            
		        } else {
		        	everythingLoaded = true;
		        	footerView.setVisibility(View.GONE);
		        }
		        if(progressDialog!=null && progressDialog.isShowing())
		        	progressDialog.hide();
		        if(error != null) {
		        	Toast.makeText(TorrentList.this, error, Toast.LENGTH_SHORT).show();
		        	TorrentList.this.finish();
		        }
		        loadingMore = false;
		        listAdapter.notifyDataSetChanged();
		    }
		};
	}
	private void getTorrents(final String query, final int page) {
		Runnable load = new Runnable() { public void run() {
			String error = null;
			try {
				lastResult = LM.search(query, inDescriptions, page);
				Log.i("DEBUG", "LM Torrents fetched "+lastResult.size());
			} catch (NotLoggedInException e) {
				error = getString(R.string.lm_not_logged_in);
				e.printStackTrace();
			} catch (IOException e) {
				error = getString(R.string.lm_no_connectivity)+" "+e.getMessage();
				e.printStackTrace();
			}
	        runOnUiThread(returnRes(error));
		} };
	    mTorrentLoaderThread =  new Thread(null, load, "Torrent Loader Thread");
	    mTorrentLoaderThread.start();
    }

	private class TorrentAdapter extends ArrayAdapter<Torrent> {

		private Torrents items;
		
		public TorrentAdapter(Context context, int textViewResourceId, Torrents items) {
			super(context, textViewResourceId, items);
		    this.items = items;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.torrentlistrow, null);
			}
			Torrent t = items.get(position);
			try {
				ImageView icon = (ImageView) v.findViewById(R.id.torrent_image);
				LM.resolveIcon(icon, t.getCategory());
				
				TextView title = (TextView) v.findViewById(R.id.torrent_title);
				TextView date = (TextView) v.findViewById(R.id.torrent_date);
				TextView size = (TextView) v.findViewById(R.id.torrent_size);
				TextView peers = (TextView) v.findViewById(R.id.torrent_peers);
				
				ImageView freeLeech = (ImageView) v.findViewById(R.id.torrent_free_leech);
				freeLeech.setVisibility( t.isFreeLeech() ? View.VISIBLE : View.GONE );
				
	    		TextView newT = (TextView) v.findViewById(R.id.torrent_new);
		    	newT.setVisibility( t.isNew() ? View.VISIBLE : View.GONE );
		    	
				title.setText(t.getTitle());
				date.setText(t.getDateAdded());
				size.setText(t.getSize());
				peers.setText( String.format("S: %s, L: %s", t.getSeeders(), t.getLeechers()) );
			} catch (NullPointerException e) {
				Log.e("DEBUG", "Failed to adapt torrent to torrent list. "+e.getMessage());
				e.printStackTrace();
			}
			return v;
		}
	}
	
	
	private OnItemClickListener clickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Torrent t = (Torrent) parent.getAdapter().getItem(position);
			t.view(TorrentList.this);
		}
	};		
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v == listView) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Torrent t = listAdapter.getItem(info.position);
			menu.setHeaderTitle( t.getTitle() );
			
			menu.add(Menu.NONE, CONTEXT_VIEW_TORRENT, CONTEXT_VIEW_TORRENT, getString(R.string.view_torrent));
			menu.add(Menu.NONE, CONTEXT_OPEN_TORRENT, CONTEXT_OPEN_TORRENT, getString(R.string.open_torrent));
			menu.add(Menu.NONE, CONTEXT_SEND_TO_UTORRENT, CONTEXT_SEND_TO_UTORRENT, getString(R.string.send_to_utorrent));
		}
	}
	
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    Torrent t = listAdapter.getItem(info.position);
	    int menuItemIndex = item.getItemId();
	  
	    switch(menuItemIndex) {
	    case CONTEXT_VIEW_TORRENT:
	    	t.view(this);
	    	break;
	    case CONTEXT_OPEN_TORRENT:
		    t.open(this);
	    	break;
	    case CONTEXT_SEND_TO_UTORRENT:
	    	t.sendToUTorrent(this);
	    	break;
	    }
	    return true;
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.torrent_list_menu, menu);
        
        return true;
    }   
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_refresh:
        	progressDialog.show();
        	listView.setOnScrollListener(null);
        	Cache.getInstance().clean();
        	page = 0;
        	everythingLoaded = false;
        	if( mTorrentLoaderThread!=null && mTorrentLoaderThread.isAlive() )
        		mTorrentLoaderThread.interrupt();
        	loadingMore = true;
        	listAdapter.clear();
        	listAdapter.notifyDataSetChanged();
        	torrents.clear();
        	lastResult.clear();
        	getTorrents(query, page);
		    listView.setOnScrollListener(TorrentList.this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }	
	public void onScrollStateChanged(AbsListView view, int scrollState) {}
	public void onScroll(AbsListView view, int firstVisibleItem,
		int visibleItemCount, int totalItemCount) {

		//what is the bottom iten that is visible
		int lastInScreen = firstVisibleItem + visibleItemCount;				

		//is the bottom item visible & not loading more already ? Load more !
		if(!everythingLoaded && (lastInScreen == totalItemCount) && !(loadingMore)) {
			loadingMore = true;
			page++;
			getTorrents(query, page);
		}
	}	
    public void toast(String text) {
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }	
}