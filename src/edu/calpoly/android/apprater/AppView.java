package edu.calpoly.android.apprater;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class is a custom class that is used for visualizing the state of an App object
 * It has three different states:  If the AppView's corresponding App has not been installed
 * yet, then its background color is red.  If it has been installed, but hasn't been rated,
 * then the background color is yellow.  If it has been installed and rated, then its background
 * color is green
 * Makes use of app_view.xml
 * @author Storm
 *
 */
public class AppView extends RelativeLayout{

	/** The data behind this View. Contains the app's information. */
	private App m_app;
	
	/** The container ViewGroup for all other Views in an AppView.
	 * Used to set the view's background color dynamically. */
	private RelativeLayout m_vwContainer;
	
	/** Indicates whether or not the App is installed.
	 * This must be set to non-interactive in the XML layout file. */
	private CheckBox m_vwInstalledCheckBox;
	
	/** Shows the user's current rating for the application. */
	private RatingBar m_vwAppRatingBar;
	
	/** The name of the App. */
	private TextView m_vwAppName;
	
	/** The context this view is in. Used for checking install status. */
	private Context context;

	/** Interface between this AppView and the database it's stored in. */
	private OnAppChangeListener m_onAppChangeListener;
	
	public AppView(Context context, App app) {
		super(context);
		//initialize the context variable
		this.context = this.getContext();
		//inflate the app_view.xml layout
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//true makes this AppView object the root ViewGroup of the inflated layout
		inflater.inflate(R.layout.app_view, this, true);
		//initialize components
		this.m_vwContainer =(RelativeLayout) findViewById(R.id.appLayout);
		this.m_vwInstalledCheckBox = (CheckBox) findViewById(R.id.installedCheckbox);
		this.m_vwAppRatingBar = (RatingBar) findViewById(R.id.appRatingBar);
		this.m_vwAppName = (TextView) findViewById(R.id.appName);
		
		setApp(app);
		//so don't register the initialization of a new app as a change
		this.m_onAppChangeListener = null;
		this.m_vwAppRatingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			
			@Override
			/**
			 * @param ratingBar The RatingBar whose rating has changed
			 * @param rating The current rating.  
			 * @param fromUser True if the rating change was initiated by a user's touch gesture or arrow key/horizontal trackbell movement
			 */
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				//change m_app's rating to the value in the rating parameter
					m_app.setRating(rating);
					//m_app's data changes 
					notifyOnAppChangeListener();					
			}
		});
	}
	
	public App getApp() {
		return m_app;
	}
	/**
	 * Check to see if the app in question is installed on the Android device, 
	 * and change m_app and the CheckBox's checked status
	 * appropriately based on the app's install status 
	 * */
	public void setApp(App app) {
		this.m_app = app;
		//This returns a PackageInfo object containing information about the package 
		PackageManager pmanager = context.getPackageManager();
		//check if the package is installed
		String packageName = m_app.getPackageFromURI();

		//set the name of the app
		this.m_vwAppName.setText(m_app.getName());
		try {
			pmanager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			//app is installed
			this.m_vwInstalledCheckBox.setChecked(true);
			m_app.setInstalled(true);
			this.m_vwAppRatingBar.setIsIndicator(false);
			
			this.m_vwAppRatingBar.setRating(app.getRating());
			float rated = this.m_app.getRating();
			if (rated == 0) {
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.installed_app));			
			}
			else {
				//rated
				this.m_vwContainer.setBackgroundColor(getResources().getColor(R.color.rated_app));				
			}		
	
		} catch (NameNotFoundException e) {
			//app is not installed 
			this.m_vwInstalledCheckBox.setChecked(false);
			int color = this.m_vwContainer.getResources().getColor(R.color.new_app);
			this.m_vwContainer.setBackgroundColor(color);
			m_app.setInstalled(false);
			//disallow rating
			this.m_vwAppRatingBar.setIsIndicator(true);
			//just a precaution
			this.m_vwAppRatingBar.setRating(0); 
			e.printStackTrace();
		}
		//m_app's data changes 
		this.notifyOnAppChangeListener();
	}
	
	/**
	 * Mutator method for changing the OnAppChangeListener object this AppView
	 * notifies when the state its underlying App object changes.
	 * 
	 * It is possible and acceptable for m_onAppChangeListener to be null, you
	 * should allow for this.
	 * 
	 * @param listener
	 *            The OnAppChangeListener object that should be notified when
	 *            the underlying App changes state.
	 */
	public void setOnAppChangeListener(OnAppChangeListener listener) {
		this.m_onAppChangeListener = listener;
	}

	/**
	 * This method should always be called after the state of m_app is changed.
	 * 
	 * It is possible and acceptable for m_onAppChangeListener to be null, you
	 * should test for this.
	 */
	protected void notifyOnAppChangeListener() {
		if (m_onAppChangeListener != null) {
			m_onAppChangeListener.onAppChanged(this, m_app);
		}
	}
	
	/**
	 * Implementing onRaingBarChangeListener
	 * Interface definition for a callback to be invoked when the underlying
	 * App is changed in this AppView object.
	 */
	public static interface OnAppChangeListener {

		/**
		 * Called when the underlying App in an AppView object changes state.
		 * 
		 * @param view
		 *            The AppView in which the App was changed.
		 * @param app
		 *            The App that was changed.
		 */
		public void onAppChanged(AppView view, App app);
	}


	/**
	 * Method for sanity check.
	 */
	public String getName() {
		String name = (String) this.m_vwAppName.getText();
		return name;
	}
}