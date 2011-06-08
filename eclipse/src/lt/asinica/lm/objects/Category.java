package lt.asinica.lm.objects;

import org.jsoup.nodes.Element;

public class Category {
	public final static String PREFIX_FOR_LINKS = "http://www.linkomanija.net";
	public final static String SEPERATOR_IN_FILES = "||"; 
	
	private int mId;
	private String mQueryParam;
	private String mName;
	private String mUrl;
	
	public int getId() { return mId; }
	public String getQueryParam() { return mQueryParam; }
	public String getName() { return mName; }
	public String getUrl() { return mUrl; }
	
	public Category(int id, String queryParam, String name, String url) {
		mId = id;
		mQueryParam = queryParam;
		mName = name;
		mUrl = url;
	}
	
	public String toString() {
		String sep = SEPERATOR_IN_FILES;
		return mId + sep + mQueryParam + sep + mName + sep + mUrl;
	}
	
	/**
	 * Parses a table cell and returns category object 
	 * @param tableCell An Element object of the cell that contains this category from site linkomanija.net 
	 * @return Parsed object
	 */	
	public static Category parse(Element tableCell) {
		// parse the input[type='checkbox'] object
		Element checkbox = tableCell.select("input").get(0);
		String queryParam = checkbox.attr("name") + "=" + checkbox.attr("value");
		
		// parse the anchor
		Element anchor = tableCell.select("a.catlink").get(0);
		String name = anchor.text();
		String url = anchor.attr("href");
		int id = Integer.parseInt(url.substring( url.lastIndexOf("=")+1 )); 
		
		return new Category(id, queryParam, name, PREFIX_FOR_LINKS + url);
	}
	/**
	 * Parses line and returns category object 
	 * @param line A string returned by toString function of this object 
	 * @return Parsed object
	 */
	public static Category parse(String line) {
		String sep = SEPERATOR_IN_FILES;
		int sepLen = sep.length();
		int firstSepPos = line.indexOf(sep);
		int secondSepPos = line.indexOf(sep, firstSepPos+sepLen);
		int thirdSepPos = line.lastIndexOf(sep);
		
		int id = Integer.parseInt(line.substring( 0, firstSepPos ));
		String queryParam = line.substring( firstSepPos+sepLen, secondSepPos );
		String name = line.substring( secondSepPos+sepLen, thirdSepPos );
		String url = line.substring( thirdSepPos+sepLen );
		
		return new Category(id, queryParam, name, url);
	}
}
