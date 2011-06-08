package lt.asinica.lm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class DetailedSearchActivity extends Activity {
	public final static int VIEW_FOR_DETAILED_SEARCH = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		
		// initiating from data 
		Bundle extras = getIntent().getExtras();
		
		if(extras.containsKey("view")) {
			int viewArg = extras.getInt("view");
			// TODO implement different views
		}
		
		if(extras.containsKey("search")) {
			// TODO init form fields
			extras.getBundle("search");
		}
	}
	
	private OnClickListener buttonSearchListener = new OnClickListener() {
	    public void onClick(View v) {
		     // TODO Auto-generated method stub
		    Intent intent=new Intent();
		    
		    Bundle searchBundle = new Bundle();
		    intent.putExtra("search", searchBundle);
		    setResult(RESULT_OK, intent);
		    finish();
	    }
	};
	
	private OnClickListener buttonCancelListener = new OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};
}