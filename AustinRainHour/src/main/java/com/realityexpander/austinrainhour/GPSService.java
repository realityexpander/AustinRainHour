/* 
 * LocationService
 * A service running in the background, reading GPS, and notifying the main app if updates are needed.
 */

package com.realityexpander.austinrainhour;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class GPSService extends Service {

	private LocationManager lm;
	private LocationListener locationListener;

	private static long minTimeMillischange = 60000; //Minimum time to timer 
	private static long minTimeMillis = 10000;		//Minimum time and distance 
	private static long minDistanceMeters = 100;    //before checking location changes

	private int lastStatus = 0;	 //GPS provider status
	private static boolean showingDebugToast = true; //Debug toasts

	private NotificationManager mNM;

	private static final String tag = "GPSService: ";

	private static final int DISTANCE = 200; //Distance on which to notify the user if he's close to a location

	private Handler handler;
	private Runnable r;
	private boolean runrunnable = true;
    // BAD public static MainActivity mMainActivity; // TODO Need a better way to pass location back to MainActivity
	
	/** Called when the activity is first created. */
	private void startLocationService() {

		lm = (LocationManager) getSystemService(LOCATION_SERVICE);

		locationListener = new MyLocationListener();

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				minTimeMillis, 
				minDistanceMeters,
				locationListener);

		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null)
		    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null ) {
            Intent i = new Intent();
            i.setAction("LatLong");
            i.putExtra("lat", location.getLatitude());
            i.putExtra("lng", location.getLongitude());
            sendBroadcast(i);
        }

        // BAD mMainActivity = MainActivity.mMainActivity;
        // BAD mMainActivity.setLocation(location);
		
		handler=new Handler();
		Runnable r = new Runnable()
		{
		    public void run() 
		    {
		        // Not used checkTasks(MainActivity.currentLocation);  // TODO this needs to update the forecast for current GPS
		        Log.d(tag,"Runnable");
		        if (runrunnable)
		        	handler.postDelayed(this, minTimeMillischange);
		    }
		};

		handler.postDelayed(r, minTimeMillischange);
	}

	
	/* Disable location service (on service close) */ 
	private void shutdownLocationService() {
		handler.removeCallbacks(r); 
		runrunnable = false;
		lm.removeUpdates(locationListener);
	}

	
	public class MyLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {

			//if location changes (except first time) more than DISTANCE_CHANGE meters - update
			if (location != null) {

                // Update the forecast and geolocation here
                Intent i = new Intent();
                i.setAction("LatLong");
                i.putExtra("lat", location.getLatitude());
                i.putExtra("lng", location.getLongitude());
                sendBroadcast(i);

				if (showingDebugToast) Toast.makeText(getBaseContext(),
						"Location stored: \nLat: " + location.getLatitude() + 
						"\nLon: " + location.getLongitude() +
						"\nAlt: " + (location.hasAltitude() ? location.getAltitude()+"m":"?") + 
						"\nAcc: " + (location.hasAccuracy() ? location.getAccuracy()+"m":"?"), 
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			Toast.makeText(getBaseContext(), "GPS provider disabled", Toast.LENGTH_SHORT).show();		
		}

		public void onProviderEnabled(String provider) {
			Toast.makeText(getBaseContext(), "GPS provider enabled", Toast.LENGTH_SHORT).show();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			String showStatus = null;
			if (status == LocationProvider.AVAILABLE)
				showStatus = "Available";
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				showStatus = "Temporarily Unavailable";
			if (status == LocationProvider.OUT_OF_SERVICE)
				showStatus = "Out of Service";
			if (status != lastStatus && showingDebugToast) {
				Toast.makeText(getBaseContext(),
						"new status: " + showStatus,
						Toast.LENGTH_SHORT).show();
			}
			lastStatus = status;
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(tag, "created taskService");
	}


	// TODO do the inclement weather notification here?
	/* Check distances to all locations with tasks. */
	public void checkTasks(Location location) {
		
		if (location == null ) {
			return;
		}
		runrunnable = false; // stop timer

		runrunnable = true;
		handler.postDelayed(r, minTimeMillischange); // start timer
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if (showingDebugToast) {
            Toast.makeText(this, "Location service started",
                    Toast.LENGTH_SHORT).show();
        }

        Log.d(tag, "started service");
        startLocationService();

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {		
		mNM.cancel(0);

		shutdownLocationService();

		// Tell the user we stopped.
		if (showingDebugToast) {
			Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static void setShowingDebugToast(boolean showingDebugToast) {
		GPSService.showingDebugToast = showingDebugToast;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GPSService getService() {
			return GPSService.this;
		}
	}

    // TODO change this to notify when weather is inclement for the next hour
//	private void notifyNearTask(String locname, TaskAlert task,Boolean wg) {
//		CharSequence text = "";
//		if (wg){
//			text = getString(R.string.location_notify, locname, task.getName());
//		}else{
//			text = getString(R.string.task_notify,task.getName());
//		}
//		// Create the notification w/ icon, text and time
//		Notification notification = new Notification(R.drawable.icon, text,
//				System.currentTimeMillis());
//		notification.defaults = Notification.DEFAULT_ALL;
//		notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
//
//		//Create an Intent to open a TaskInfo activity if notification is clicked.
//		Intent intent = new Intent(this, TaskInfo.class);
//		intent.putExtra(TasksDbAdapter.KEY_ROWID, task.getId());
//
//		// The PendingIntent to launch our activity if the user selects this notification
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, task.getId());
//		notification.contentIntent = contentIntent;
//		// Set the info for the views that show in the notification panel.
//		notification.setLatestEventInfo(this, getText(R.string.app_name),
//				text, contentIntent);
//
//		// Send the notification.
//		// We use the task id as a unique identifier (can cancel the notification later)
//		mNM.notify(task.getId(), notification);
//	}
}