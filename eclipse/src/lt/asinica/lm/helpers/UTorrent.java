package lt.asinica.lm.helpers;

import java.io.IOException;
import java.net.URLEncoder;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.InvalidTokenException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

public class UTorrent {
	
	private String protocol = "http";
	private String host;
	private int port;
	private String username;
	private String password;
	private boolean infoChanged = false;
	private String basePath = "/gui/";
	private String tokenUrlSuffix = "token.html"; 
	private String token;
	private DefaultHttpClient httpClient;
	public String getLabel() {
		return host;
	}
	
	private static UTorrent instance;
	public static UTorrent getInstance() {
		if(UTorrent.instance == null) {
			instance = new UTorrent();
		}
		return instance;
	}
	
	private UTorrent() {
		httpClient = new DefaultHttpClient();
	}
	
	public void setServerInfo(String host, int port, String username, String password) {
		if(this.host != host) {
			this.host = host;
			infoChanged = true;
		}
		if(this.port != port) {
			this.port = port;
			infoChanged = true;
		}
		boolean up = false;
		if(this.username != username) {
			up = true;
			this.username = username;
			infoChanged = true;
		}
		if(this.password != password) {
			up = true;
			this.password = password;
			infoChanged = true;
		}
		if(up) {
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
			httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
		}
	}
	
	public void addTorrent(String downloadUrl, String cookie) throws ClientProtocolException, InvalidTokenException, IOException {
		//http://[USERNAME]:[PASSWORD]@[IP]:[PORT]/gui/
		String suffix = "?action=add-url&"+ "%$1s" +"&s="+ URLEncoder.encode(downloadUrl) +":COOKIE:"+cookie;
		request(suffix);
		//request();
	}
	public Runnable tester(final Activity context) {
		return new Runnable() { public void run() {
			String answer = null;
			try {
				if(test()) {
					answer = context.getString(R.string.ut_test_success);
				}
			} catch (InvalidTokenException e) {
				Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.ut_unexpected_response);
				e.printStackTrace();					
			} catch (ClientProtocolException e) {
				Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.ut_cant_connect)+" "+e.getMessage();
				e.printStackTrace();					
			} catch (IOException e) {
				Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.ut_cant_connect)+" "+e.getMessage();
				e.printStackTrace();
			}		
			
			if(answer!=null) {
				final String tmp = answer;
				context.runOnUiThread(new Runnable() { public void run() {
					Toast.makeText(context, tmp, Toast.LENGTH_LONG).show();
				} });
			}
		} };
	}
	
	public boolean test() throws ClientProtocolException, InvalidTokenException, IOException {
		String resp = request("?action=getsettings&"+ "%$1s");
		if(resp.length()>0) return true;
		else return false;
	}
	
	private void fetchToken() throws InvalidTokenException, ClientProtocolException, IOException {
		// if token not yet fetched
		if(token==null || infoChanged) {
			
			String page = rawRequest(tokenUrlSuffix);
			
			Document doc = Jsoup.parse(page);
			Elements tokenElem = doc.select("#token");
			if(!tokenElem.isEmpty()) {
				token = tokenElem.get(0).text();
			} else {
				throw new InvalidTokenException();
			}
		}
	}
	private String request(String urlSuffix) throws ClientProtocolException, InvalidTokenException, IOException {
		fetchToken();
		return rawRequest(urlSuffix);
	}
	private String rawRequest(String urlSuffix) throws ClientProtocolException, IOException {
		String tokenUrl = protocol + "://" + host + ":" + Integer.toString(port) + basePath + String.format(urlSuffix, "token="+token);
		HttpGet httpGet = new HttpGet(tokenUrl);
		HttpResponse response = httpClient.execute(httpGet);
		String page = EntityUtils.toString(response.getEntity());
		//httpClient.getConnectionManager().shutdown();
		
		return page;
	}
}
