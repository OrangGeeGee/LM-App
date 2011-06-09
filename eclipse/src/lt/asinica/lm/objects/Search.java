package lt.asinica.lm.objects;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import lt.asinica.lm.R;
import android.content.Context;
import android.os.Bundle;

public class Search {
	// class vars
	private String mQuery = "";
	private boolean mInDescription = false;
	private boolean mOnlyFreeLeech = false;
	private ArrayList<Integer> mCategoriesList = new ArrayList<Integer>();
	
	// getters
	public String getQuery() { return mQuery; }
	public boolean getInDescription() { return mInDescription; }
	public boolean getOnlyFreeLeech() { return mOnlyFreeLeech; }
	public ArrayList<Integer> getCategoriesList() { return mCategoriesList; }
	
	// setters
	public void setQuery(String query) { mQuery = query; }
	public void setInDescription(boolean inDescription) { mInDescription = inDescription; }
	public void setOnlyFreeLeech(boolean onlyFreeLeech) { mOnlyFreeLeech = onlyFreeLeech; }
	public void setCategoriesList(ArrayList<Integer> categories) { mCategoriesList = categories; }
	
	// constructors
	public Search() { }
	public Search(String query, boolean inDescription) {
		mQuery = query;
		mInDescription = inDescription;
	}
	
	// misc
	public boolean hasCategory(Integer id) {
		return mCategoriesList.contains(id);
	}
	public void addCategory(Integer id) {
		mCategoriesList.add(id);
	}
	public void removeCategory(Integer id) {
		mCategoriesList.remove(id);		
	}
	
	public String getTitle(Context context) {
		if(!mCategoriesList.isEmpty()) {
			return context.getString(R.string.lm_detailed_search);
		} else if(mQuery!=null && mQuery.length()>0) {
			return String.format( context.getString(R.string.lm_search_results), mQuery );
		} else {
			return context.getString(R.string.lm_all_torrents);
		}
	}
	
	// static methods
	public static Search parseBundle(Bundle bundle) {
		Search s = new Search();
		s.setQuery( bundle.getString("query") );
		s.setInDescription( bundle.getBoolean("inDescription") );
		s.setOnlyFreeLeech( bundle.getBoolean("onlyFreeLeech") );
		s.setCategoriesList( bundle.getIntegerArrayList("categories") );
		return s;
	}
	public Bundle toBundle() {
		Bundle bundle = new Bundle();
		bundle.putString("query", mQuery);
		bundle.putBoolean("inDescription", mInDescription);
		bundle.putBoolean("onlyFreeLeech", mOnlyFreeLeech);
		bundle.putIntegerArrayList("categories", mCategoriesList);
		return bundle;
	}
	public String toGetParameter() {
		String result = "search=";
		if(mQuery!=null)
			result += URLEncoder.encode( mQuery );
		if(mInDescription)
			result += "&searchindesc=1";
		if(mOnlyFreeLeech)
			result += "&freeleech=1";
		
		Categories allCats = LM.getInstance().getCategories();
		Iterator<Integer> iter = mCategoriesList.iterator();
		while(iter.hasNext())
			result += "&"+allCats.get(iter.next()).getQueryParam();
		
		return result;
	}
}
