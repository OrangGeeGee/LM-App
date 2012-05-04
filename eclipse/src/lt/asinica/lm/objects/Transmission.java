/**
 * 
 */
package lt.asinica.lm.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.InvalidTokenException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;



/**
 * @author nial
 * This class handles communication between software and transmission client using JASON-RPC 1.6 
 *
 */
public class Transmission {
	private String protocol = "http";
	private String host;
	private int port = 9091;
	private String username;
	private String password;
	private String basePath = "/transmission/rpc";
	private DefaultHttpClient httpClient;
	private String sessionId= null;
	public String getLabel() {
		return host;
	}
	
	private static Transmission instance;
	
	/**	public static getInstance()
	 * 	Creates an instance if not already present
	 * @returns instance
	 **/
	public static Transmission getInstance() {
		if(Transmission.instance == null) {
			instance = new Transmission();
			
		}
		return instance;
	}
	
	/**
	 * private Transmission()
	 *	Initialises HTTPClient
	 **/
	private Transmission() {
		httpClient = new DefaultHttpClient();
	}
	
	public void setServerInfo(String host, int port, String username, String password) {

		if(this.host != host) {
			this.host = host;
		}
		if(this.port != port) {
			this.port = port;
		}
		boolean up = false;
		if(this.username != username) {
			up = true;
			this.username = username;
		}
		if(this.password != password) {
			up = true;
			this.password = password;
		}
		if(up) {
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
			httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
		}
	}
	public void addTorrent(String downloadUrl, String cookie) throws ClientProtocolException, InvalidTokenException, IOException, JSONException,Exception {
		//http://[USERNAME]:[PASSWORD]@[IP]:[PORT]/gui/
		//	String suffix = "?action=add-url&"+ "{token}" +"&s="+ URLEncoder.encode(downloadUrl) +":COOKIE:"+cookie;
		//	request(suffix);
		//	//request();
		String sessionHeaderName = "X-Transmission-Session-Id"; 
		JSONObject  jsonAddTorrent = null;
		String jsonString = "{\"arguments\":{\"filename\":\"" + downloadUrl + "\",\"cookies\":\"" + cookie + "\"},\"method\": \"torrent-add\"}";
		try {
			jsonAddTorrent = new JSONObject (jsonString);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (jsonAddTorrent == null){
			throw new Exception("JsonString creation failed");
		}
		
		 HttpPost httppost = new HttpPost(protocol + "://" + host + ":" + Integer.toString(port) + basePath);
		 StringEntity se = new StringEntity(jsonAddTorrent.toString());
		 httppost.setEntity(se);
		// Execute
	     HttpResponse response = httpClient.execute(httppost);
	     Header headerSessionId = response.getFirstHeader(sessionHeaderName);
	     if (headerSessionId.getValue() != null && headerSessionId.getValue() != sessionId){
	    	 sessionId = headerSessionId.getValue();
	    	 httppost.addHeader(sessionHeaderName, sessionId);
	    	 response = httpClient.execute(httppost);
	     }
	    HttpEntity entity = response.getEntity();
	    JSONObject json = null;
	    if (entity != null) {
	    	// Read JSON response
	        InputStream inputStream = entity.getContent();
	        String result = convertStreamToString(inputStream);
	        json = new JSONObject(result);
	        inputStream.close();
	    }
	    if (json == null){
	    	throw new Exception("No response from server");
	    }
	    String result =json.getString("result"); 
	    if (result.equals(new String("success"))){
	    	throw new Exception(result);
	    }
	    
	}
	
	/**
	 * Utility to convert Stream To String
	 * @param inputStream
	 * @return String , if empty returns ""
	 * @throws IOException
	 */
	private String convertStreamToString( InputStream inputStream)  throws IOException{
		if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
            	inputStream.close();
            }
            return writer.toString();
        } else {        
            return "";
        }
	}
	public Runnable tester(final Activity context) {
		return new Runnable() { public void run() {
			String answer = null;
			try {
				if(test()) {
					answer = context.getString(R.string.tr_test_success);
				}
			} catch (InvalidTokenException e) {
				Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.tr_unexpected_response);
				e.printStackTrace();					
			} catch (Exception e) {
				Log.e("DEBUG", "Exception "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.tr_cant_connect)+" "+e.getMessage();
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
	
	public boolean test() throws ClientProtocolException, InvalidTokenException, IOException, Exception {
		 return true;
		
	}
}
