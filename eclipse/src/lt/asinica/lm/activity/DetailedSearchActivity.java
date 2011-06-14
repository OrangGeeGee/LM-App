package lt.asinica.lm.activity;

import java.util.ArrayList;

import lt.asinica.lm.R;
import lt.asinica.lm.objects.Category;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Search;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailedSearchActivity extends Activity {
	public final static int VIEW_FOR_DETAILED_SEARCH = 1;
	private GridView mCategoryGrid;
	private CategoryAdapter mCategoryAdapter;
	private Search mSearch = new Search();
	private EditText mQueryField;
	private CheckBox mInDescriptions;
	private CheckBox mOnlyFreeLeech;
	
	public static void startNew(Activity context, Bundle search, int view) {
    	Intent intent = new Intent(context, DetailedSearchActivity.class);
    	intent.putExtra("search", search);
    	intent.putExtra("view", view);
    	context.startActivityForResult(intent, 1);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detailed_search);
		// set title
		setTitle(getString(R.string.app_name)+" "+getString(R.string.lm_detailed_search));

		mCategoryAdapter = new CategoryAdapter( this, R.layout.category_grid_item, LM.getInstance().getCategoriesList() );
		mCategoryGrid = (GridView) findViewById(R.id.ds_category_grid);
		mCategoryGrid.setAdapter(mCategoryAdapter);
		mCategoryGrid.setOnItemClickListener(new CategoryClickListener());
		
		// asign fields to class vars
		mQueryField = (EditText) findViewById(R.id.ds_query);
		mInDescriptions = (CheckBox) findViewById(R.id.ds_in_descriptions);
		mOnlyFreeLeech = (CheckBox) findViewById(R.id.ds_only_free_leech);
		
    	mQueryField.setOnKeyListener(new OnKeyListener() {
    	    public boolean onKey(View v, int keyCode, KeyEvent event) {
    	        // If the event is a key-down event on the "enter" button
    	        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
    	            (keyCode == KeyEvent.KEYCODE_ENTER)) {
    	          // Perform action on key press
    	          submit();
    	          return true;
    	        }
    	        return false;
    	    }
    	});
		
		// enable buttons at the bottom of detailed search layout
		((Button) findViewById(R.id.ds_cancel)).setOnClickListener(mButtonCancelListener);
		((Button) findViewById(R.id.ds_search)).setOnClickListener(mButtonSearchListener);
		
		// initiating from data 
		Bundle extras = getIntent().getExtras();
		if(extras.containsKey("view")) {
			//int viewArg = extras.getInt("view");
			// TODO implement different views
		}
		
		if(extras.containsKey("search")) {
			mSearch = Search.parseBundle(extras.getBundle("search"));
			mQueryField.setText( mSearch.getQuery() );
			mInDescriptions.setChecked( mSearch.getInDescription() );
			mOnlyFreeLeech.setChecked( mSearch.getOnlyFreeLeech() );
		}
		
	}
	private class CategoryClickListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        	int categoryId = mCategoryAdapter.getItem(position).getId();
        	if(mSearch.hasCategory(categoryId)) {
        		mSearch.removeCategory(categoryId);
        	} else {
        		mSearch.addCategory(categoryId);
        	}
        	mCategoryAdapter.notifyDataSetChanged();
        }
	}
	private class CategoryAdapter extends ArrayAdapter<Category> {

		private ArrayList<Category> items;
		
		public CategoryAdapter(Context context, int textViewResourceId, ArrayList<Category> items) {
			super(context, textViewResourceId, items);
		    this.items = items;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.category_grid_item, null);
			}
			Category cat = items.get(position);
			try {
				ImageView icon = (ImageView) v.findViewById(R.id.category_icon);
				TextView name = (TextView) v.findViewById(R.id.category_name);
				if(mSearch.hasCategory( cat.getId() )) {
					v.setBackgroundResource(R.drawable.selected);
				} else {
					v.setBackgroundResource(0);
				}
				LM.resolveIcon(icon, String.valueOf( cat.getId() ));
				name.setText( cat.getName() );
			} catch (NullPointerException e) {
				Log.e("DEBUG", "Failed to adapt torrent to torrent list. "+e.getMessage());
				e.printStackTrace();
			}
			return v;
		}
	}	
	public void submit() {
	    Intent intent = new Intent();
	    String query = mQueryField.getText().toString();
	    boolean inDescription = mInDescriptions.isChecked();
	    boolean onlyFreeLeech = mOnlyFreeLeech.isChecked();
	    mSearch.setQuery(query);
	    mSearch.setInDescription(inDescription);
	    mSearch.setOnlyFreeLeech(onlyFreeLeech);
	    intent.putExtra("search", mSearch.toBundle());
	    setResult(RESULT_OK, intent);
	    finish();
	}
	private OnClickListener mButtonSearchListener = new OnClickListener() {
	    public void onClick(View v) {
	    	submit();
	    }
	};
	
	private OnClickListener mButtonCancelListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};
}