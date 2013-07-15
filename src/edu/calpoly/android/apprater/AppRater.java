package edu.calpoly.android.apprater;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.calpoly.android.apprater.AppView.OnAppChangeListener;

/**
 * Contains definition for the main AppRater Activity class
 * This class will display the list of applications the user is supposed to test and rate.
 * It starts the AppDownloadService in order to add new Apps to the underlying database. 
 * The new apps are retrieved from a website through this Service.
 * Makes use of app_list.xml
 * @author Storm
 *
 */
public class AppRater extends SherlockFragmentActivity implements OnAppChangeListener, OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
	
	/** The ListView that contains the List of AppViews. */
	private ListView m_vwAppList;

	/** The CursorAdapter used to bind the Cursor to AppViews. */
	private AppCursorAdapter m_appAdapter;

	/** The BroadcastReceiver used to listen for new Apps that have been added by
	 * the AppDownloadService. */
	private DownloadCompleteReceiver m_receiver; 
	
	/** The ID of the CursorLoader to be initialized in the LoaderManager and used to load a Cursor. */
	private static final int LOADER_ID = 1;
	
	/** Code used to launch market install and to handle the install */
	private static final int MARKET_REQUEST_CODE = 2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list);
        
        // Initialize cursor adapter
        this.m_appAdapter = new AppCursorAdapter(this, null, 0);
        
        //Initialize the LoaderManager, causing it to set up a CursorLoader.
		this.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        
        //Initialize View Components, similar to how initLayout() was done in the past.
        this.m_vwAppList = (ListView)findViewById(R.id.m_vwAppList);
        this.m_vwAppList.setOnItemClickListener(this);
        this.m_vwAppList.setAdapter(this.m_appAdapter);
    }
    /**
     * This method is called when the user resumes activity from the Paused state
     * System calls this method every time activity comes into the foreground, including
     * when it's created for the first time
     */
    @Override
    public void onResume() {
    	//create IntentFilter so match with ACTION_NEW_APP_TO_REVIEW action
    	IntentFilter intentFilter = new IntentFilter(AppRater.DownloadCompleteReceiver.ACTION_NEW_APP_TO_REVIEW);
    	//broadcast an Intent with CATEGORY_DEFAULT, so add this as category to Intent Filter
    	intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
    	//initialize m_receiver
    	this.m_receiver = new DownloadCompleteReceiver();
    	/* Register a BroadcastReceiver to be run in the main activity thread. 
    	 * The receiver will be called with any broadcast Intent that matches filter, 
    	 * in the main application thread. 
    	 */
    	this.registerReceiver(m_receiver, intentFilter);
    	super.onResume();
    }
    /**
     * This is called when the activity is going into the background, but has not (yet) been
     * killed.  It is the counterpart ot onResume
     */
    @Override
    public void onPause() {
    	//unregister the receiver
    	this.unregisterReceiver(m_receiver);
    	super.onPause();
    }
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		App app = ((AppView) view).getApp();
		String name = app.getName();
		Toast.makeText(getBaseContext(), name, Toast.LENGTH_SHORT).show();
		Toast.makeText(getBaseContext(), "installed status:  "+ app.isInstalled(), Toast.LENGTH_SHORT).show();
		/* create an intent that opens a Google Play URL
		 * ACTION_VIEW: Display the data to the user. 
		 * This is the most common action performed on data -- it is the generic action you 
		 * can use on a piece of data to get the most reasonable thing to occur. 
		 * For example, when used on a contacts entry it will view the entry; 
		 * when used on a mailto: URI it will bring up a compose window filled with 
		 * the information supplied by the URI; when used with a tel: URI it will invoke 
		 * the dialer. 
		 */
		Toast.makeText(getBaseContext(), "Check rating on item click:  " + app.getRating(), Toast.LENGTH_SHORT).show();
		if (!app.isInstalled()) {
			Toast.makeText(getBaseContext(), "App is not installed", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(Intent.ACTION_VIEW);
			//get the install uri of the app represented by the view
			String uri = app.getInstallURI();
			//set the data this intent is operating on
			intent.setData(Uri.parse(uri));
			//use this so can identify the call and handle it in onActivityResult
			startActivityForResult(intent, AppRater.MARKET_REQUEST_CODE);	
		}
	}
	/**
	 * Called when activity you launched exits
	 * @param requestCode The integer request code originally supplied to startActivityForResult(), 
	 * 		  allowing you to identify where it came from
	 * @param resultCode The integer result code returned by the child activity through 
	 *        its setResult()
	 * @param data An Intent, which can return result data to the caller
	 */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//make sure market install launches the request
    	if (requestCode == AppRater.MARKET_REQUEST_CODE) {
    		Toast.makeText(getBaseContext(), "Handling market request", Toast.LENGTH_SHORT).show();
    		//refresh the view (makes new AppViews)
    		fillData();
    	}
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = this.getSupportMenuInflater();
    	/* options from this menu will allow users of the app to start/stop the service that 
    	 * downloads the apps from the server to the list and rechecks the server periodically
    	 * for more, or remove all apps from the list*/
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	int button = item.getItemId();
    	switch (button) {
	    	case(R.id.menu_startDownload):{
	    		/* getIntent() = get the intent that started this service */
	    		Log.w("AppRater", "starting download");
	    		//since service has a an action, we're using it
	    		Intent intent = new Intent(this,AppDownloadService.class);
	    		Log.w("AppRater", intent.toString());
	    		this.startService(intent);
	    		return true;
	    	}
	    	case (R.id.menu_stopDownload):{
	    		Log.w("AppRater", "stopping download");
	    		this.stopService(getIntent());
	    		return true;
	    	}
	    	case (R.id.menu_removeAll):{
	    		Log.w("AppRater", "removing all apps from the list");
	    		Uri removeUri = Uri.withAppendedPath(AppContentProvider.CONTENT_URI, "apps");
	    		Log.w("AppRater", "remove uri:  " + removeUri);
	    		//use the ContentResolver to delete
	    		this.getContentResolver().delete(removeUri, null, null);
	    		return true;
	    	}
    	}
    	return false;
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = { AppTable.APP_KEY_ID, AppTable.APP_KEY_NAME, AppTable.APP_KEY_RATING,
			AppTable.APP_KEY_INSTALLURI, AppTable.APP_KEY_INSTALLED };
		
		Uri uri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps");
		
		CursorLoader cursorLoader = new CursorLoader(this, uri, projection, null, null, AppTable.ORDER_BY_STRING);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		this.m_appAdapter.swapCursor(cursor);
		this.m_appAdapter.setOnAppChangeListener(this);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		this.m_appAdapter.swapCursor(null);
	}

	/**
	 * The event handler for managing App changes. Updates the database app table and the
	 * AppView bound to the underlying App that got changed.<br><br>
	 * 
	 * Specified by <b>onAppChanged(...)</b> in AppView.<br><br>
	 */
	@Override
	public void onAppChanged(AppView view, App app) {
		Uri uri = Uri.parse(AppContentProvider.CONTENT_URI + "/apps/" + app.getID());
		
		ContentValues cv = new ContentValues();
		cv.put(AppTable.APP_KEY_NAME, app.getName());
		cv.put(AppTable.APP_KEY_RATING, app.getRating());
		cv.put(AppTable.APP_KEY_INSTALLURI, app.getInstallURI());
		cv.put(AppTable.APP_KEY_INSTALLED, app.isInstalled() ? 1 : 0);
		
		this.getContentResolver().update(uri, cv, null, null);
		
		this.m_appAdapter.setOnAppChangeListener(null);
		
		fillData();
	}
	
	/**
	 * Update and refresh all list data in the application: The Cursor from the CursorLoader,
	 * the adapter (through <b>restartLoader()</b>) and sets the latter as the ListView's new
	 * adapter.
	 */
	public void fillData() {
		this.getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
		/* instead of reassigning the adapter to the ListView after restarting the CursorLoader
		 * like in Lab 4, just call adapter's notifyDataSetChanged() method.  This prevents the
		 * ListView from always scrolling back up to the top of the list on every change, but 
		 * preserves its refresh */
		this.m_appAdapter.notifyDataSetChanged();
	}
	
	/**
	 * BroadcastReceiver that checks for broadcasted Intents with a specific action as
	 * specified in AppDownloadService. Also responsible for producing and showing
	 * Notifications in the Notification Bar when an app is added.
	 */
	public class DownloadCompleteReceiver extends BroadcastReceiver {

		public static final String ACTION_NEW_APP_TO_REVIEW = "edu.calpoly.android.apprater.MESSAGE_PROCESSED";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//if the Intent's action matches ACITON_NEW_APP_TO_REVIEW
			if (intent.getAction().equals(ACTION_NEW_APP_TO_REVIEW)) {
				Toast.makeText(getBaseContext(), R.string.newAppToast, Toast.LENGTH_SHORT).show();
				//update list of apps
				fillData();
				//post notification to notification bar
				showNotification(context);
			}
		}

		/**
		 * Creates, initializes and sends a Notification to the Notification Bar.
		 * 
		 * @param context The context this receiver runs in. 
		 */
		private void showNotification(Context context) {
			//get instance of app's resources from the context
			Resources resources = context.getResources();
			//set the first line of text in the platform notification template
			String title = resources.getString(R.string.newAppNotificationOriginName);
			//set the second line of text in the platform notification template 
			String text = resources.getString(R.string.newAppNotificationText);
			/* text that appears briefly in the minimized Notification area when the
			 * Notification is first added to it before disappearing after several seconds*/
			String tickerText = resources.getString(R.string.newAppNotificationTicker);
			/* create an intent for a specific component.  This provides a convenient way
			 * to create an intent that is intended to execute a hard-coded class name,
			 * rather than relying on the system to find an appropriate class for you */
			Intent appRaterIntent = new Intent(context,AppRater.class);
			/* create a new PendingIntent
			 * intent = intent of the activity to be launched (points to AppRater class*/
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appRaterIntent, 0);
			Builder notificationBuilder = new NotificationCompat.Builder(context)
										  .setContentTitle(title)
										  .setContentText(text)
										  .setSmallIcon(R.drawable.icon)
										  .setTicker(tickerText)
										  .setLights(Color.RED, 1000, 1000);
			//supply a PendingIntent to be sent when the notification is clicked
			notificationBuilder.setContentIntent(pendingIntent);
			//make the Notification disappear from the Notification area when it is pressed
			notificationBuilder.setAutoCancel(true);
			//get a reference to NotificationManger
			NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			/* don't know what to put for the id
			 * build() combines all of the options that have been set and returns a new Notification
			 * object */
			nManager.notify(1, notificationBuilder.build());
		}
	}
}