package lt.asinica.lm.objects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import lt.asinica.lm.LMApp;
import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.BadPasswordException;
import lt.asinica.lm.exceptions.ExternalStorageNotAvaliableException;
import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.helpers.DownloadProgressUpdater;
import lt.asinica.lm.helpers.Torrents;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class LM {
	// constants
	public final static String BASE_URL = "http://www.linkomanija.net"; //  
	public final static String BROWSE_URL = BASE_URL+"/browse.php?incldead=0";
	public final static String THANK_URL = BASE_URL+"/ajax/thank.php";
	public final static String LOGIN_URL = BASE_URL+"/takelogin.php";
	
	
	// singleton apratus
	private static LM instance;
	public static LM getInstance() {
		if(LM.instance == null) {
			instance = new LM();
		}
		return instance;
	}
	private LM() {}
	
	private Categories mCategories = new Categories();
	private ArrayList<Category> mCategoriesList;
	private boolean mInitSuccessful = false;
	private boolean mInitInProgress = false;
	private Thread mInitThread;
	
	/**
	 * Returns LM categories. Warning, if categories are not yet initialized it pauses the current thread.
	 * @return
	 */
	
	public Thread getInitThread() {
		return mInitThread;
	}
	public Categories getCategories() {
		return mCategories;
	}
	public ArrayList<Category> getCategoriesList() {
		return mCategoriesList;
	}
	
	/**
	 * Initiation of search categories
	 */
	public void initSearch() {
		if( !mInitSuccessful && !mInitInProgress ) {
			mInitInProgress = true;
			Runnable init = new Runnable() {
				public void run() {
					mCategories = Categories.restore();
					if(mCategories.isEmpty()) {
						mInitSuccessful = refetchCategories();
						mCategories.save();
					} else {
						mInitSuccessful = true;
					}
					if(!mCategories.isEmpty()) {
						mCategoriesList = mCategories.toArrayList();
					}
						
					mInitInProgress = false;
				}
			};
			mInitThread = new Thread(init, "LM Category Initiation thread");
			mInitThread.start();
		}
	}
	private boolean refetchCategories() {
		String url = BROWSE_URL + "&search=randomabcnevergonefindme";
		try {
			Document doc = performQuery(url);
			mCategories = Categories.parse(doc);
			boolean notEmpty = !mCategories.isEmpty();
			return notEmpty;
		} catch (NotLoggedInException e) {
			Log.e("DEBUG", "Couldn't refetch categories, because not logged in.");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("DEBUG", "Couldn't refetch categories. "+e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	public String login(String username, String password) throws BadPasswordException, IOException {
		//String secret  = prefs.getString("lmsecret", "");
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(LOGIN_URL);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("commit", "Prisijungti"));
        nameValuePairs.add(new BasicNameValuePair("login_cookie", "1"));
        
    	BasicHttpContext httpContext = new BasicHttpContext();
    	CookieStore cookieStore      = new BasicCookieStore();        
    	httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    	
		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		HttpResponse response = httpClient.execute(post, httpContext);
		Log.v("DEBUG", "LM Login response status line "+response.getStatusLine());
		Log.v("DEBUG", "LM Login headers "+response.getAllHeaders().toString());
		List<Cookie> cookies = cookieStore.getCookies();
		
		if(!cookies.isEmpty()) {
			Iterator<Cookie> i = cookies.iterator();
			Cookie tmp;
			String nTmp;
			while(i.hasNext()) {
				tmp = i.next();
				nTmp = tmp.getName();
				if(nTmp.equals("login")) {
					return tmp.getValue();
				}
			}
			throw new BadPasswordException();
		} else {
			throw new BadPasswordException();
		}
	}
	public Torrents search(Search search, int page) throws NotLoggedInException, IOException {
		String url = BROWSE_URL + "&" + search.toGetParameter() + "&page="+Integer.toString(page);
		Document doc = performQuery(url);
    	Torrents list = new Torrents();
    	try {
	    	Elements rows = doc.select("form table[border] tr");
    		rows.remove(0);
    		ListIterator<Element> iterator = rows.listIterator();
    		Element row;
    		while(iterator.hasNext()) {
    			row = iterator.next();
    			list.add(new Torrent(row));
    		}
    	} catch (Exception e) {
    		Log.e("DEBUG", "Can not parse document to torrent list. "+e.getMessage());
    		e.printStackTrace();
    	}
    	return list;
	}
	
	/**
	 * Creates it's own thread. Notifies if something went wrong
	 */
	public void thank(final Torrent t, final Activity context) {
		Runnable sayThanks = new Runnable() { public void run() {
			String url = THANK_URL;
			String id = t.getId();
			String error = null;
			try {
				Response resp = Jsoup.connect(url)
				  .cookie("login", getSecret())
				  .method(Method.POST)
				  .data("id", id)
				  .execute();
				Log.v("DEBUG", "Said thanks to "+id+", URL "+url+", response "+resp.body());
			} catch (Exception e) {
				error = context.getString(R.string.t_thanks_failed) + " " + e.getMessage();
				Log.e("DEBUG", error);
				e.printStackTrace();
			}
			if(error != null) {
				final String err = error;
				context.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(context, err, Toast.LENGTH_SHORT).show();
					}
				});
			}
		}};
		Thread thanker = new Thread(sayThanks, "Thank you Thread");
		thanker.start();
		Cache.getInstance().cleanSingle(t.getDescriptionUrl());
	}
	
	public Torrent getMoreInfo(Torrent t) throws NotLoggedInException, IOException {
		String url = t.getDescriptionUrl();
		Document doc = performQuery(url);
		t.parseTorrentInfo(doc);
    	return t;		
	}
	
	public Document performQuery(String url) throws NotLoggedInException, IOException {
		Log.v("DEBUG", "LM navigating to "+url);
		Cache cacheObj = Cache.getInstance();
		String cache = cacheObj.fetch(url);
		Document doc = null;
		if(cache!=null) {
			doc = Jsoup.parse(cache);
		} else {
			String loginSecret = getSecret();
			doc = Jsoup.connect(url)
			  //.data("query", "Java")
			  .userAgent("Mozilla")
			  .cookie("login", loginSecret)
			  .timeout(10000)
			  .get();
			
			if(doc.select("#username").isEmpty()) {
				Editor editor = PreferenceManager.getDefaultSharedPreferences(LMApp.getDefaultContext()).edit();
				editor.remove("lmsecret").commit();
				LMApp.restart();
				throw new NotLoggedInException();
			}
			int cacheForXMinutes = 30;
			if(url.contains("search="))
				cacheForXMinutes = 5;
			cacheObj.put(url, doc.html(), cacheForXMinutes);
		}
		return doc;
	}
	
	/**
	 * Used to fetch LM Cookie `login` value, so you could crawl the website
	 * @return String with the cookie's 'login' value
	 * @throws NotLoggedInException
	 */
	public String getSecret() throws NotLoggedInException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LMApp.getDefaultContext());
		if(prefs.getAll().containsKey("lmsecret")) {
			return prefs.getString("lmsecret", "");
		} else {
			throw new NotLoggedInException();
		}
	}
	

	/**
	 * Downloads a torrent file with all the default options
	 * @param t Torrent type object from which all required params are fetched
	 * @return File handle of the downloaded file
	 * @throws ExternalStorageNotAvaliableException
	 * @throws MalformedURLException
	 * @throws NotLoggedInException
	 * @throws IOException
	 */
	public File downloadFile(Torrent t)
			throws ExternalStorageNotAvaliableException, MalformedURLException, NotLoggedInException, IOException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LMApp.getDefaultContext());
		
		// parse download path so there's alwayas a trailing slash
		String dir = prefs.getString("downloadpath", "");
		int dirlen = dir.length();
		if(dirlen>0 && dir.lastIndexOf("/")!=dir.length()-1) dir = dir + "/";
		
		// get SD Card path
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+dir;
		
		// get login secret and pass to worker method 
		String loginSecret = getSecret();
		return downloadFile(path, t.getFileName(), t.getDownloadUrl(), "login="+loginSecret, null);
		
	}
	public File downloadFile(String path, String filename, String downloadFromURL, String cookie, DownloadProgressUpdater updater)
			throws ExternalStorageNotAvaliableException, MalformedURLException, NotLoggedInException, IOException {

		// check if SDCard is mounted
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state)) {
			throw new ExternalStorageNotAvaliableException();
		}
		
        File pathHandle = new File(path );
        pathHandle.mkdirs();
        if(!pathHandle.isDirectory()) { throw new IOException("Directory " + pathHandle.getAbsolutePath() + " could not be created"); }
        //create a new file, specifying the path, and the filename
        //which we want to save the file as.
        File file = new File(pathHandle,filename);
        //this will be used to write the downloaded data into the file we created
        FileOutputStream fileOutput = new FileOutputStream(file);

        URL url = new URL(downloadFromURL);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        //set up some things on the connection
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setUseCaches(false);
        if(cookie!=null) {
        	urlConnection.addRequestProperty("Cookie", cookie);
        }
        //and connect!
        urlConnection.connect();        
        
        //this will be used in reading the data from the internet
        InputStream inputStream = urlConnection.getInputStream();

        //this is the total size of the file
        int totalSize = urlConnection.getContentLength();
        //variable to store total downloaded bytes
        int downloadedSize = 0;

        //create a buffer...
        byte[] buffer = new byte[1024];
        int bufferLength = 0; //used to store a temporary size of the buffer
        
        if(updater!=null)
        	updater.updateProgress(totalSize, downloadedSize);
        
        //now, read through the input buffer and write the contents to the file
        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
                //add up the size so we know how much is downloaded
                downloadedSize += bufferLength;
                
                //this is where you would do something to report the prgress, like this maybe
                if(updater!=null)
                	updater.updateProgress(totalSize, downloadedSize);
        }
        //close the output stream when done
        fileOutput.close();
        return file;
	}
	
	/**
	 * Resolves icon image from the specified category
	 * @param icon ImageView to be resolved
	 * @param category String indicating the category id of the image
	 */
	public static void resolveIcon(ImageView icon, String category) {
		try {
			Integer i = (Integer) R.drawable.class.getField("ic_"+category).get(null);
			icon.setImageResource(i);
			icon.setVisibility(View.VISIBLE);
		} catch (Exception e) {
			Log.e("DEBUG", "Couldn't fetch torrent icon. "+e.getMessage());
			icon.setVisibility(View.INVISIBLE);
			e.printStackTrace();
		}
	}
}