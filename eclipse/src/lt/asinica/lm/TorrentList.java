package lt.asinica.lm;

import java.io.IOException;
import java.util.Iterator;

import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.helpers.ListPack;
import lt.asinica.lm.helpers.Torrents;
import lt.asinica.lm.objects.Cache;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Search;
import lt.asinica.lm.objects.Torrent;
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
	// constants
	public static final int CONTEXT_VIEW_TORRENT = 0;
	public static final int CONTEXT_OPEN_TORRENT = 1;
	public static final int CONTEXT_SEND_TO_UTORRENT = 2;

	// class vars
	private ProgressDialog mProgressDialog = null;
	private TorrentAdapter mListAdapter;
	private ListView mListView;
	private Torrents mTorrents = new Torrents();
	private Torrents mLastResult = null;
	private int mPage = 0;
	private Search mSearch;
	private String mTitle;
	private boolean mEverythingLoaded = false;
	private boolean mLoadingMore = false;
	private Thread mTorrentLoaderThread;
	private View mFooterView;
	
	public ListPack pack() {
		return new ListPack(mTorrents, mSearch, mPage, mTitle, mEverythingLoaded, mLoadingMore);
	}
	public void unpack(ListPack p) {
		mTorrents = p.getTorrents();
		mSearch = p.getSearch();
		mPage = p.getPage();
		mTitle = p.getTitle();
		mEverythingLoaded = p.getEverythingLoaded();
		mLoadingMore = false;//p.getLoadingMore();
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
	    mListView = getListView();
		mListView.setOnItemClickListener(clickListener);
	    registerForContextMenu(mListView);
	    // add the footer before adding the adapter, else the footer will not load!
	    
	    final ListPack data = (ListPack) getLastNonConfigurationInstance();
	    if (data != null) {
	    	unpack(data);
	    } else {
		    Bundle extras = getIntent().getExtras();
		    mSearch = Search.parseBundle( extras.getBundle("search") );
		    mTitle = mSearch.getTitle(this);
		    
            getTorrents(mSearch, mPage);
		    mProgressDialog = ProgressDialog.show(TorrentList.this,    
		          getString(R.string.please_wait), getString(R.string.lm_searching), true);
	    }
	    if(!mEverythingLoaded) {
		    mFooterView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.helper, null, false);
		    mListView.addFooterView(mFooterView, null, false);
	    }
	    setTitle(getString(R.string.app_name)+" "+mTitle);
	    mLastResult = new Torrents();
	    mListAdapter = new TorrentAdapter(this, R.layout.torrentlistrow, mLastResult);
	    setListAdapter(mListAdapter);
	    if(data!=null) {
	    	Iterator<Torrent> i = mTorrents.iterator();
	    	while(i.hasNext())
	    		mListAdapter.add(i.next());
	    	mListAdapter.notifyDataSetChanged();
	    }
	    mListView.setOnScrollListener(TorrentList.this);
	}
	private Runnable returnRes(final String error) {
		return new Runnable() {
		    public void run() {
		        if(mLastResult != null && mLastResult.size() > 0){
		        	mTorrents.addAll(mLastResult);
		            mListAdapter.notifyDataSetChanged();
			    	Iterator<Torrent> i = mLastResult.iterator();
			    	while(i.hasNext())
			    		mListAdapter.add(i.next());		            
		        } else {
		        	mEverythingLoaded = true;
		        	mFooterView.setVisibility(View.GONE);
		        }
		        if(mProgressDialog!=null && mProgressDialog.isShowing())
		        	mProgressDialog.hide();
		        if(error != null) {
		        	Toast.makeText(TorrentList.this, error, Toast.LENGTH_SHORT).show();
		        	TorrentList.this.finish();
		        }
		        mLoadingMore = false;
		        mListAdapter.notifyDataSetChanged();
		    }
		};
	}
	private void getTorrents(final Search search, final int page) {
		Runnable load = new Runnable() { public void run() {
			String error = null;
			try {
				mLastResult = LM.getInstance().search(search, page);
				Log.i("DEBUG", "LM Torrents fetched "+mLastResult.size());
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
		if (v == mListView) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Torrent t = mListAdapter.getItem(info.position);
			menu.setHeaderTitle( t.getTitle() );
			
			menu.add(Menu.NONE, CONTEXT_VIEW_TORRENT, CONTEXT_VIEW_TORRENT, getString(R.string.view_torrent));
			menu.add(Menu.NONE, CONTEXT_OPEN_TORRENT, CONTEXT_OPEN_TORRENT, getString(R.string.open_torrent));
			menu.add(Menu.NONE, CONTEXT_SEND_TO_UTORRENT, CONTEXT_SEND_TO_UTORRENT, getString(R.string.send_to_utorrent));
		}
	}
	
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    Torrent t = mListAdapter.getItem(info.position);
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
        	mProgressDialog.show();
        	mListView.setOnScrollListener(null);
        	Cache.getInstance().clean();
        	mPage = 0;
        	mEverythingLoaded = false;
        	if( mTorrentLoaderThread!=null && mTorrentLoaderThread.isAlive() )
        		mTorrentLoaderThread.interrupt();
        	mLoadingMore = true;
        	mListAdapter.clear();
        	mListAdapter.notifyDataSetChanged();
        	mTorrents.clear();
        	mLastResult.clear();
        	getTorrents(mSearch, mPage);
		    mListView.setOnScrollListener(TorrentList.this);
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
		if(!mEverythingLoaded && (lastInScreen == totalItemCount) && !(mLoadingMore)) {
			mLoadingMore = true;
			mPage++;
			getTorrents(mSearch, mPage);
		}
	}	
    public void toast(String text) {
    	Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }	
}