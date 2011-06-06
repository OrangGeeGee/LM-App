package lt.asinica.lm;

import java.io.File;

import lt.asinica.lm.helpers.DownloadProgressUpdater;
import lt.asinica.lm.helpers.LM;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class Updater {
	private static String UPDATE_SERVER_URL = "http://asinica.lt/lmapp/updateServer.php";
	private static Updater instance;
	public static Updater getInstance() {
		if(instance==null)
			instance = new Updater();
		return instance;
	}
	private Updater() {
		mCurrentVersion = getSoftwareVersion();
	}
	
    private int mCurrentVersion;
    private boolean mCheckedForNewVersion = false;
    private boolean mCheckingInProgress = false;
    
    private int getSoftwareVersion() {
        try {
        	LMApp app = LMApp.getInstance();
            PackageInfo packageInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("DEBUG", "Package name not found ", e);
            e.printStackTrace();
        };
        return 0;
    }
    
    public void forceCheck() {
    	mCheckedForNewVersion = false;
    	checkForNewVersion();
    }
    public void checkForNewVersion() {
    	if(!mCheckedForNewVersion && !mCheckingInProgress) {
    		mCheckingInProgress = true;
    		Thread t = new Thread(checker(), "Version Checker Thread");
    		t.start();
    	}
    }    
    private Runnable checker() { return new Runnable () { public void run() {
    	HttpGet httpGet = new HttpGet(UPDATE_SERVER_URL);
    	HttpParams httpParameters = new BasicHttpParams();
    	
    	int timeoutConnection = 5000;
    	HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
    	int timeoutSocket = 5000;
    	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

    	DefaultHttpClient client = new DefaultHttpClient(httpParameters);    	
    	try {
    		HttpResponse response = client.execute(httpGet);
    		JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
    		int versionCode = json.getInt("versionCode");
    		String versionName = json.getString("versionName");
    		String downloadUrl = json.getString("downloadUrl"); 
    		String title = json.getString("title");
    		String changelog = json.getString("changelog");
    		if(versionCode > mCurrentVersion) {
    			Context c = LMApp.getDefaultContext();
    			Intent i = new Intent(c, UpdateService.class);
    			i.putExtra("title", title);
    			i.putExtra("changelog", changelog);
    			i.putExtra("downloadUrl", downloadUrl);
    			i.putExtra("versionName", versionName);
    			c.startService(i);
    			Log.i("DEBUG", "you need some updates bro");
    		}
    		mCheckedForNewVersion = true;
    	} catch(Exception e) {
    		Log.e("DEBUG", "Could not connect get update from update server. "+e.getMessage());
    		e.printStackTrace();
    	}
    	mCheckingInProgress = false;
	}}; }
    
    public Runnable downloader(final String path, final String filename, final String downloadURL, final Activity c, final ProgressDialog dialog) {
    	return new Runnable() { public void run() {
		    
	    	dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	
	    	DownloadProgressUpdater ui = new DownloadProgressUpdater() {
				public void run() {
					int progress = (int) (sizeDownloaded / sizeTotal * 10000) % 1;
					dialog.setProgress(progress);
				}
			};
	    	
			String error = null;
			File downloaded = null;
	    	try {
	    		Log.v("DEBUG", "Starting download from "+downloadURL+" to "+path+filename);
				downloaded = LM.downloadFile(path, filename, downloadURL, null, ui);
				Log.v("DEBUG", "Download finished, stopping activity");
			} catch (Exception e) {
				Log.e("DEBUG", "Couldn't download update file. "+e.getMessage());
				error = c.getString(R.string.updater_failed_to_download)+" "+e.getMessage();
				e.printStackTrace();
			}
			
			final String err = error;
			final File f = downloaded;
			c.runOnUiThread(new Runnable() {
				public void run() {
					dialog.dismiss();
					if(err!=null) {
						Toast.makeText(c, err, Toast.LENGTH_LONG);
					} else {
						Uri uri = Uri.fromFile(f);
						String type = "application/vnd.android.package-archive";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setDataAndType(uri, type);
			    		//i.setData(u);
			    		//i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    		c.startActivity(i);
					}
				}
			});
    	}
    }; }
}
