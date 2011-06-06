package lt.asinica.lm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class UpdaterActivity extends Activity {
	private Bundle mUpdateDetails;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
		stopService(new Intent(this, UpdateService.class));
		} catch(Exception e) {
			Log.e("DEBUG", "Something went wrong when stopping service. "+e.getMessage());
			e.printStackTrace();
		}
		
		setContentView(R.layout.updater);
		
		Bundle extras = getIntent().getExtras().getBundle("details");
		mUpdateDetails = extras;
		
		TextView info = (TextView) findViewById(R.id.updater_info);//
		String html = getString(R.string.updater_info).replace("{version}", extras.getString("versionName"));
		html = html.replace("{changelog}", extras.getString("changelog"));
		info.setText(html);
		
		Button cancel = (Button) findViewById(R.id.updater_cancel);
		cancel.setOnClickListener(mListenToCancel);
		
		Button update = (Button) findViewById(R.id.updater_update);
		update.setOnClickListener(mListenToUpdate);
	}
	
	private OnClickListener mListenToCancel = new OnClickListener() { public void onClick(View v) {
		UpdaterActivity.this.finish();
	} };
	
	private OnClickListener mListenToUpdate = new OnClickListener() { public void onClick(View v) {
		String path = getExternalCacheDir().getAbsolutePath()+"/";
		
		String url = mUpdateDetails.getString("downloadUrl");
		
		
		ProgressDialog dialog = new ProgressDialog(UpdaterActivity.this);
		dialog.setTitle(getString(R.string.please_wait));
		dialog.setMessage(getString(R.string.updater_update_downloading));
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setIndeterminate(true);
		dialog.show();
		
		Runnable dl = Updater.getInstance().downloader(path, "LM App.apk", url, UpdaterActivity.this, dialog);
		
		Thread t = new Thread(dl, "New Version Downloader Thread");
		t.start();
	} };	
}
