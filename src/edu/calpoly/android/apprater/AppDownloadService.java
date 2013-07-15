package edu.calpoly.android.apprater;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * The Service that performs app information downloading, performing the check again
 * occasionally over time. It adds an application to the list of apps and is also
 * responsible for telling the BroadcastReceiver when it has done so.
 */
public class AppDownloadService extends IntentService{
	/** The ID for the Notification that is generated when a new App is added. */
	public static final int NEW_APP_NOTIFICATION_ID = 1;
	
	/** The Timer thread which will execute the check for new Apps. Acts like a Thread
	 * that can be told to start at a specific time and/or at specific time intervals. */
	private Timer m_updateTimer;
	
	/** The TimerTask which encapsulates the logic that will check for new Apps. This ends
	 * up getting run by the Timer in the same way that a Thread runs a Runnable. */
	private TimerTask m_updateTask;

	/** The time frequency at which the service should check the server for new Apps. */
	private static final long UPDATE_FREQUENCY = 10000L;

	/** A String containing the URL from which to download the list of all Apps. */
	public static final String GET_APPS_URL = "http://www.simexusa.com/aac/getAll.php";

	/**
	 * Note that the constructor that takes a String will NOT be properly instantiated.
	 * Use the constructor that takes no parameters instead, and pass in a String that
	 * contains the name of the service to the super() call.
	 */
	public AppDownloadService() {
		super("AppDownloadService");
	}

	/**
	 * This method downloads all of the Apps from the App server. For each App,
	 * it checks the AppContentProvider to see if it has already been downloaded
	 * before. If it is new, then it adds it to the AppContentProvider by
	 * calling addNewApp.
	 */
	private void getAppsFromServer() {
		try {
			URL url = new URL(AppDownloadService.GET_APPS_URL);
			/* Get all the apps from the URL and put them in HashMap with the name of the app
			 * as the name, and the URI of the app as the value */
			Scanner in;
			try {
				in = new Scanner(url.openStream());
				in = in.useDelimiter(";");
				while(in.hasNext()){
					String appInfo = in.next();
					//don't include the whitespace
					if (appInfo.length() == 1) {
						break;
					}
					else{
						String[] split = appInfo.split(",");
						String name = split[0];
						String uri = split[1];
						//convert the info to an app add it
						App app = new App(name,uri);
						Log.e("ADS", "app name from file:  " + name);
						addNewApp(app);
					}
				}
			} 
			catch (IOException e) {
				Log.e("AppRater", e.getMessage());
			} 

		} catch (MalformedURLException e) {
			Log.e("AppRater", e.getMessage());
		}
	}

	/**
	 * This method adds a new App to the AppContentProvider.
	 * 
	 * @param app
	 *            The new App object to add to the ContentProvider.
	 */
	private void addNewApp(App app) {
		Log.e("ADS", "adding a new app");
		Log.e("ADS", "app name:  " + app.getName());
		/* get the ContentResolver for AppRater
		 * the contentResolver obtained here allows access to the operations in the
		 * AppContentProvider.  For example, calling query() from the ContentResolver object
		 * will invoke AppContentProvider.query().  There's no need to set up a Cursor 
		 * managing system to perform operations on the database. */
		ContentResolver contentResolver = this.getContentResolver();
		/* perform a query on the ContentResolver for an app in the database with the passed in
		 * app's name*/
		String [] projection = {AppTable.APP_KEY_NAME, AppTable.APP_KEY_INSTALLED};
		Uri uri;
		uri = Uri.withAppendedPath(AppContentProvider.CONTENT_URI, "apps/" + app.getName());
		Log.e("AppDownload", "uri created for single app:  " + uri.toString());
		/* projection is columns want to return
		 * selection is filter of which rows to return, formatted as where clause (without where) 
		 * WHERE <column name><operator value>*/
		Cursor cursor = contentResolver.query(uri, projection, null, null, null);
		Log.e("AppContent", "after the query");
		//if cursor contains 0 rows, app doesn't exist, so add it
		if (cursor.getCount() == 0) {
			Log.e("AppDownload", "cursor is 0");
			ContentValues contentValues = new ContentValues();
			//put the app's data into contentValues (column name for the table, data)
			contentValues.put(AppTable.APP_KEY_NAME, app.getName());
			contentValues.put(AppTable.APP_KEY_RATING, app.getRating());
			contentValues.put(AppTable.APP_KEY_INSTALLURI, app.getInstallURI());
			//can't put booleans in SQLite, so use 1 for true and 0 for false
			if (app.isInstalled()) {
				Log.e("AppDownloadService", "app is downloaded");
				contentValues.put(AppTable.APP_KEY_INSTALLED, 1);
			}
			else {
				Log.e("AppDownloadService", "app is not downloaded");
				contentValues.put(AppTable.APP_KEY_INSTALLED, 0);
			}
			//parse a new Uri for insertion into the database
			Uri insertUri = Uri.withAppendedPath(AppContentProvider.CONTENT_URI, "apps/" + app.getID());
			Uri newUri = contentResolver.insert(insertUri, contentValues);
			Long automated_id = Long.valueOf(newUri.getLastPathSegment());
			//set the app id to this id
			app.setID(automated_id);
			Log.e("AppDownloadService", "new id:  " + app.getID());
			
			//tell application that there is a new App to rate
			announceNewApp();
		}
		/* close the cursor object.  Since not using CursorLoader in this class, we are 
		 * responsible for closing an Cursors that we open */
		cursor.close();
	}

	/**
	 * This method broadcasts an intent with a specific Action String. This method should be
	 * called when a new App has been downloaded and added successfully.
	 * This is where Intent broadcasting happens
	 */
	private void announceNewApp() {
		/* create an intent with ACTION_NEW_APP_TO_REVIEW as the action
		 * this string can be found in the DownloadCompleteReceiver subclass inside the 
		 * AppRater class */
		Intent announce = new Intent(AppRater.DownloadCompleteReceiver.ACTION_NEW_APP_TO_REVIEW);
		/*set the category. This is necessary for matching the soon-to-be broadcast Intent with the
		 * broadcast receiver*/
		announce.addCategory(Intent.CATEGORY_DEFAULT);
		/* If you know there will only be local broadcasts of INtents within your app's 
		 * process, there is an optional LocalBroadcastManager you can use */
		this.sendBroadcast(announce);
		
	}
	/* This method is invoked on the worker thread with a request to process. Only one Intent is processed at a time, 
	 * but the processing happens on a worker thread that runs independently from other application logic. 
	 * So, if this code takes a long time, it will hold up other requests to the same IntentService, 
	 * but it will not hold up anything else. When all requests have been handled, 
	 * the IntentService stops itself, so you should not call stopSelf().
	 * Note: onCreate() is called before onHandleIntent(), therefore initialization in onCreate() was ideal
	 */
	@Override
	protected void onHandleIntent(Intent arg0) {
		Log.e("ADS", "onHandleIntent");
		/*Schedule a task for repeated fixed-rate execution after a specific delay has passed.
		 * Parameters
		 * task  the task to schedule. 
		 * delay  amount of time in milliseconds before first execution. 
		 * period  amount of time in milliseconds between subsequent executions.
		 * Using these parameters will cause the TimerTask, which calls getAppsFromServer(), to be run
		 * every so often */
		this.m_updateTimer.scheduleAtFixedRate(this.m_updateTask, 0, AppDownloadService.UPDATE_FREQUENCY);
	}
	
	@Override
	public void onCreate(){
		//initialize m_updateTimer
		this.m_updateTimer = new Timer();
		//initialize m_updateTask
		this.m_updateTask = new TimerTask() {
			
			@Override
			//The task to run
			public void run() {
				getAppsFromServer();
			}
		};
		super.onCreate();
	}
	/*Called by the system to notify a Service that it is no longer used and is being removed. 
	 *The service should clean up any resources it holds (threads, registered receivers, etc) at this point. 
	 *Upon return, there will be no more calls in to this Service object and it is effectively dead. 
	 *Do not call this method directly. */
	@Override
	public void onDestroy() {
		//cancel the timer
		this.m_updateTimer.cancel();
		//also do the other stuff in the default implementation of onDestroy
		super.onDestroy();
	}
}
