package lt.asinica.lm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class UpdateService extends Service {
    private NotificationManager mNM;
    private Bundle mIntentExtras;
    private Notification mNotification;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.new_version_avaliable;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    /*public class LocalBinder extends Binder {
        UpdateService getService() {
            return UpdateService.this;
        }
    }*/

    @Override
    public void onCreate() {
        // Display a notification about us starting.  We put an icon in the status bar.
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("DEBUG", "Update Service received start id " + startId + ": " + intent);
        if(intent!=null) {
	        Bundle b = intent.getExtras();
	    	mIntentExtras = b;
	    	showNotification();
        }
    	return START_STICKY;
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Log.v("DEBUG","Service stopped alright");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.new_version_avaliable);
        CharSequence descrip = getString(R.string.new_version_more_info).replace("{version}", mIntentExtras.getString("versionName"));

        // Set the icon, scrolling text and timestamp
        mNotification = new Notification(R.drawable.stat_sys_warning, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        Intent i = new Intent(this, UpdaterActivity.class);
        i.putExtra("details", mIntentExtras);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // Set the info for the views that show in the notification panel.
        mNotification.setLatestEventInfo(this, text, descrip, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, mNotification);
    }
}