package lt.asinica.lm.objects;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import android.os.Bundle;

public class Search {
	// class vars
	private String mQuery = "";
	private boolean mInDescription = false;
	private boolean mOnlyFreeLeech = false;
	private Categories mCategories = new Categories();
	
	// getters
	public String getQuery() { return mQuery; }
	public boolean getInDescription() { return mInDescription; }
	public boolean getOnlyFreeLeech() { return mOnlyFreeLeech; }
	public Categories getCategories() { return mCategories; }
	
	// setters
	public void setQuery(String query) { mQuery = query; }
	public void setInDescription(boolean inDescription) { mInDescription = inDescription; }
	public void setOnlyFreeLeech(boolean onlyFreeLeech) { mOnlyFreeLeech = onlyFreeLeech; }
	public void setCategories(Categories categories) { mCategories = categories; }
	
	public Search() { }
	public Search(String query, boolean inDescription) {
		mQuery = query;
		mInDescription = inDescription;
	}
	
	
	// TODO static method realization
	// static methods
	public static Search parseBundle(Bundle bundle) {
		Search s = new Search();
		s.setQuery( bundle.getString("query") );
		s.setInDescription( bundle.getBoolean("inDescription") );
		s.setOnlyFreeLeech( bundle.getBoolean("onlyFreeLeech") );
		
		Categories all = LM.getInstance().getCategories();
		Categories current = s.getCategories();
		Iterator<Integer> iter = bundle.getIntegerArrayList("categories").iterator();
		int currKey;
		while(iter.hasNext()) {
			currKey = iter.next();
			current.put(currKey, all.get(currKey));
		}
		return s;
	}
	public Bundle toBundle() {
		Bundle bundle = new Bundle();
		bundle.putString("query", mQuery);
		bundle.putBoolean("inDescription", mInDescription);
		bundle.putBoolean("onlyFreeLeech", mOnlyFreeLeech);
		
		// TODO Check if this val is okay
		ArrayList<Integer> array = new ArrayList<Integer>(mCategories.keySet());
		bundle.putIntegerArrayList("categories", array);
		
		return bundle;
	}
	public String toGetParameter() {
		String result = "search=" + URLEncoder.encode( mQuery ) + (mInDescription ? "&searchindesc=1" : "");
		return result;
	}
}
