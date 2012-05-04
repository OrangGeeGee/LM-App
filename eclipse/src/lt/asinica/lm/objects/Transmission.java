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
import java.io.UnsupportedEncodingException;
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
	private String SessionId;
	private String sessionHeaderName = "X-Transmission-Session-Id"; 
	
	
	
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
	/**Sets the host port and authentication params for the HttpClient
	 * 
	 * @param host
	 * @param port
	 * @param username
	 * @param password
	 */
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
		
		private String getRPCSessionId(HttpPost httpPost) throws ClientProtocolException, IOException  {
			StringEntity se = new StringEntity("{\"method\": \"session-stats\"}");
			httpPost.setEntity(se);
			HttpResponse response = httpClient.execute(httpPost);
			Header headerSessionId = response.getFirstHeader(sessionHeaderName);
		    if (headerSessionId.getValue() != null && headerSessionId.getValue() != SessionId){
		    	SessionId = headerSessionId.getValue();
		    	return SessionId;
		    }
			return null;
		}
	
		private JSONObject getResponse(HttpPost httpPost, String requestString) throws Exception{
			JSONObject responseObject = null;
			StringEntity se = new StringEntity(requestString);
			
			String currentSessionId = getRPCSessionId(httpPost);
			if (currentSessionId == null)
				throw new Exception("Nerdau Transmission RPC Sessijos");
			
			if (SessionId!=null && SessionId.length() > 0 && SessionId.equals(currentSessionId)){
				httpPost.addHeader(sessionHeaderName, SessionId);
			}
			else
			{
				SessionId = currentSessionId;
				httpPost.addHeader(sessionHeaderName, SessionId);
			}
			httpPost.setEntity(se);
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
	        String result = convertStreamToString(inputStream);
	        inputStream.close();

	        responseObject = new JSONObject(result);
			return responseObject;
		}
		/**
		 * Adds torrent url to the download list of Transmission client
		 * Creates a JSON - RPC call and gets a response, anything else then SUCESS throws an exception
		 * 
		 * @param downloadUrl String 
		 * @param cookie String
		 * @throws ClientProtocolException
		 * @throws InvalidTokenException
		 * @throws IOException
		 * @throws JSONException
		 * @throws Exception
		 */
		public void addTorrent(String downloadUrl, String cookie) throws ClientProtocolException, InvalidTokenException, IOException, JSONException,Exception {
		
		String jsonString = "{\"arguments\":{\"filename\":\"" + downloadUrl + "\",\"cookies\":\"" + cookie + "\"},\"method\": \"torrent-add\"}";
		HttpPost httppost = new HttpPost(protocol + "://" + host + ":" + Integer.toString(port) + basePath);
		JSONObject jsonResponse = new JSONObject();
		jsonResponse = getResponse(httppost,jsonString);
	    String result =jsonResponse.getString("result"); 
	    if (!result.equals(new String("success"))){
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
	
	/**
	 * Wrapper for test of the connection to the Transmission RPC client
	 * @param context
	 * @return
	 */
	public Runnable tester(final Activity context) {
		return new Runnable() { public void run() {
			String answer = null;
			try {
				if(test()) {
					answer = context.getString(R.string.tr_test_success);
				}
			} 
			catch (ClientProtocolException e) {
				Log.e("DEBUG", "ClientProtocolException "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.tr_cant_connect)+" "+e.getMessage();
				e.printStackTrace();					
			}
			catch (IOException e) {
				Log.e("DEBUG", "IOException "+e.getClass().toString()+". "+e.getMessage());
				answer = context.getString(R.string.tr_cant_connect)+" "+e.getMessage();
				e.printStackTrace();					
			}
			catch (Exception e) {
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
	
	/**
	 * Test to see if Transmission client is accessible with given parameters
	 * @return true if connection is OK otherwise FALSE
	 * @throws ClientProtocolException
	 * @throws InvalidTokenException
	 * @throws IOException
	 * @throws Exception
	 */
	public boolean test() throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost(protocol + "://" + host + ":" + Integer.toString(port) + basePath);
		String sessionIdTest = getRPCSessionId(httpPost);
		if (sessionIdTest!=null && sessionIdTest.length() > 0){
			return true;	
		}
		return false;
	}
}
