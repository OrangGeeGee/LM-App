package lt.asinica.lm.helpers;

public class ListPack {
	Torrents torrents;
	String query;
	int page;
	String title;
	boolean inDescriptions;
	boolean everythingLoaded;
	boolean loadingMore;
	
	public Torrents getTorrents() { return torrents; }
	public String getQuery() { return query; }
	public int getPage() { return page; }
	public String getTitle() { return title; }
	public boolean getInDescriptions() { return inDescriptions; }
	public boolean getEverythingLoaded() { return everythingLoaded; }
	public boolean getLoadingMore() { return loadingMore; }
	
	public ListPack(Torrents torrents, String query, int page, String title, boolean inDescriptions, boolean everythingLoaded, boolean loadingMore) {
		this.torrents = torrents;
		this.query = query;
		this.page = page;
		this.title = title;
		this.inDescriptions = inDescriptions;
		this.everythingLoaded = everythingLoaded;
		this.loadingMore = loadingMore;
	}
	
}