package edu.calpoly.android.apprater;

import edu.calpoly.android.apprater.AppView.OnAppChangeListener;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Binds a set of AppViews to a set of Apps using Cursors.
 * It is an Adapter that exposes data from a Cursor to a ListView
 */
public class AppCursorAdapter extends CursorAdapter {

	/** The OnAppChangeListener that should be connected to each of the
	 * AppViews created/managed by this Adapter. */
	private OnAppChangeListener m_listener;
	
	/* context = the context
	 * cursor = the cursor from which to get data
	 * flags: used to determine behavior of the adapter, may be combo of 
	 * FLAG_AUTO_REQUERY and FLAG_REGISTER_CONTENT_OBSERVER
	 */
	public AppCursorAdapter(Context context, Cursor cursor, int flags) {
		super(context, cursor, flags);
		//make the adapter, but don't connect the listener to the AppView yet
		this.m_listener = null;
	}

	/**
	 * Mutator method for changing the OnAppChangeListener.
	 * 
	 * @param listener
	 *            The OnAppChangeListener that will be notified when the
	 *            internal state of any App contained in one of this Adapters
	 *            AppViews is changed.
	 */
	public void setOnAppChangeListener(OnAppChangeListener mListener) {
		this.m_listener = mListener;
	}
	/**
	 * Binds an existing view to the data pointed to by cursor
	 * view = existing view, returned earlier by newView
	 * context = interface to application's global information
	 * cursor = the cursor from which to get data 
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		App app = new App(cursor.getString(AppTable.APP_COL_NAME),
			cursor.getString(AppTable.APP_COL_INSTALLURI),
			cursor.getFloat(AppTable.APP_COL_RATING),
			cursor.getLong(AppTable.APP_COL_ID),
			cursor.getInt(AppTable.APP_COL_INSTALLED) > 0);
		//don't connect the listener yet, since initializing it will cause it to change
		((AppView)view).setOnAppChangeListener(null);
		//sets the app for the AppView
		((AppView)view).setApp(app);
		//set the listener to listen for changes in the app
		((AppView)view).setOnAppChangeListener(this.m_listener);
	}

	@Override
	/**
	 * Makes a new app from the cursor, creates a new AppView and sets the listener
	 * of the adapter to listen for changes in App, which is wrapped in an AppView
	 */
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		App app = new App(cursor.getString(AppTable.APP_COL_NAME),
			cursor.getString(AppTable.APP_COL_INSTALLURI),
			cursor.getFloat(AppTable.APP_COL_RATING),
			cursor.getLong(AppTable.APP_COL_ID),
			cursor.getInt(AppTable.APP_COL_INSTALLED) > 0);
		AppView av = new AppView(context, app);
		av.setOnAppChangeListener(this.m_listener);
		return av;
	}
}
