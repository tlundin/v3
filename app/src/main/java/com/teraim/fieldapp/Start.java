package com.teraim.fieldapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.templates.LinjePortalTemplate;
import com.teraim.fieldapp.dynamic.templates.TableDefaultTemplate;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnActivityResult;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.log.CriticalOnlyLogger;
import com.teraim.fieldapp.log.DummyLogger;
import com.teraim.fieldapp.log.Logger;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.DrawerMenu;
import com.teraim.fieldapp.ui.LoginConsoleFragment;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;



/**
 * @author Terje
 *
 */
public class Start extends MenuActivity {

	public static boolean alive = false;

	//	private Map<String,List<String>> menuStructure;

	//	private ArrayList<String> rutItems;
	//	private ArrayList<String> wfItems;
	private LoginConsoleFragment loginFragment;
	private AsyncTask<GlobalState, Integer, LoadResult> histT=null;
	public static Start singleton;
	private DrawerMenu mDrawerMenu;

	private ActionBarDrawerToggle mDrawerToggle;
	private boolean loading = false;

	// Constants
	// The authority for the sync adapter's content provider
	public static final String AUTHORITY = "com.teraim.fieldapp.provider";
	// An account type, in the form of a domain name
	public static final String ACCOUNT_TYPE = "teraim.com";
	// The account name
	public static final String ACCOUNT = "FieldApp";

	public static final long SYNC_INTERVAL = 60;
	// Instance fields
	// Account mAccount;

	private ContentResolver mResolver;




	/**
	 * Program entry point
	 *
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Setup handler for uncaught exceptions.
/*
		Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException (Thread thread, Throwable e)
			{
				Log.e("vortex","Uncaught Exception detected in thread {"+thread+"} Exce: "+ e);
				//e.printStackTrace();
				handleUncaughtException (thread, e);
			}
		});
*/

		Log.d("nils","in START onCreate");
		singleton = this;
		//This is the frame for all pages, defining the Action bar and Navigation menu.
		setContentView(R.layout.naviframe);
		//This combats an issue on the target panasonic platform having to do with http reading.
		System.setProperty("http.keepAlive", "false");
		mDrawerMenu = new DrawerMenu(this);
		mDrawerToggle = mDrawerMenu.getDrawerToggle();
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Create a Sync account
		// mAccount = CreateSyncAccount(this);

		//Determine if program should start or first reload its configuration.
		if (!loading)
			checkStatics();

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if(menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}
		super.onCreate(savedInstanceState);
	}


	public boolean isUIThread(){
		return Looper.getMainLooper().getThread() == Thread.currentThread();
	}

	private void handleUncaughtException(Thread thread, Throwable e) {

		e.printStackTrace(); // not all Android versions will print the stack trace automatically

		//invokeLogActivity();

		if(isUIThread()) {
			invokeLogActivity();

		}else{  //handle non UI thread throw uncaught exception

			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {

					invokeLogActivity();
				}
			});
		}


	}


	




	private void invokeLogActivity(){
		Intent intent = new Intent ();
		if (globalPh!=null) {
			intent.putExtra("program_version", Constants.VORTEX_VERSION);
			intent.putExtra("app_name",globalPh.get(PersistenceHelper.BUNDLE_NAME));
			intent.putExtra("user_name",globalPh.get(PersistenceHelper.USER_ID_KEY));
			intent.putExtra("team_name",globalPh.get(PersistenceHelper.LAG_ID_KEY));
		}
		intent.setAction ("com.teraim.fieldapp.SEND_LOG"); // see step 5.
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
		Log.d("vortex","Sending log file. Starting SendLog.");
		startActivity (intent);
		System.exit(1); // kill off the crashed app
	}

	@Override
	protected void onResume() {
		Log.d("nils","In START onResume");
		//Check if program is already up.
		if (!loading)
			checkStatics();
		else
			loading = false;


		super.onResume();

	}

	@Override
	protected void onStart() {
		Log.d("nils","In START onStart");
		if(GlobalState.getInstance()!=null)
			GlobalState.getInstance().onStart();
		super.onStart();
	}


	/**
	 *
	 */
	private void checkStatics() {
		if (GlobalState.getInstance()==null) {
			loading = true;

			//Create a global logger.


			//Start the login fragment.
			android.app.FragmentManager fm = getFragmentManager();
			for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
				fm.popBackStack();
			}
			loginFragment = new LoginConsoleFragment();
			Log.d("vortex","LoginFragment on stack!");
			fm.beginTransaction()
					.replace(R.id.content_frame, loginFragment)
					.commit();

		} else {
			Log.d("vortex","Globalstate is not null!");

		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d("nils","In oncofigChanged");
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;

		// Handle other action bar items

		return super.onOptionsItemSelected(item);
	}



	//




	@Override
	public void setTitle(CharSequence title) {

		getActionBar().setTitle(title);
	}


	Fragment emptyFragmentToExecute = null ;

	//execute workflow.
	public void changePage(Workflow wf, String statusVar) {
		if (wf==null) {
			debugLogger.addRow("Workflow not defined for button. Check your project XML");
			Log.e("vortex","no wf in changepage");
			return;
		}
		GlobalState gs = GlobalState.getInstance();
		if (gs == null) {
			Log.e("vortex","Global State is null in change pange. App needs to restart");
			Tools.restart(this);
		}

		if (isFinishing()) {
			Log.d("vortex","This activity is finishing! Cannot continue");
			return;
		}

		String label = wf.getLabel();
		String template = wf.getTemplate();

		//Set context.
		Log.d("vortex","CHANGING PAGE TO: xxxxxxxx ["+wf.getName()+"]");
		DB_Context cHash = DB_Context.evaluate(wf.getContext());

		//if Ok err is null.
		if (cHash.isOk()) {

			gs.setDBContext(cHash);

			debugLogger.addRow("Context now [");
			debugLogger.addGreenText(cHash.toString());
			debugLogger.addText("]");
			debugLogger.addText("wf context: "+wf.getContext());

			//gs.setRawHash(r.rawHash);
			//gs.setKeyHash(r.keyHash);
			//No template. This flow does not have a ui. Hand over to Executor.
			Fragment fragmentToExecute;
			Bundle args = new Bundle();
			args.putString("workflow_name", wf.getName());
			args.putString("status_variable", statusVar);


			if (template==null) {
				emptyFragmentToExecute = wf.createFragment("EmptyTemplate");
				emptyFragmentToExecute.setArguments(args);
				FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				//Log.i("vortex", "Adding fragment");
				//ft.add(R.id.lowerContainer, fragment, "AddedFragment");

				ft.add(emptyFragmentToExecute,"EmptyTemplate");
				Log.i("vortex", "Committing Empty transaction");
				ft.commitAllowingStateLoss();
				Log.i("vortex", "Committed transaction");
			} else {
				fragmentToExecute = wf.createFragment(template);
				fragmentToExecute.setArguments(args);
				changePage(fragmentToExecute,label);
			}
			//show error message.
		} else
			showErrorMsg(cHash);
	}
	public void changePage(Fragment newPage, String label) {
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction ft = fragmentManager.beginTransaction();

		ft
				.replace(R.id.content_frame, newPage)
				.addToBackStack("TROLL")
				.commit();
		setTitle(label);

		//If previous was an empty fragment, clean it
		if (emptyFragmentToExecute!=null) {
			Log.d("blax","removing empty fragment");
			ft.remove(emptyFragmentToExecute);
			emptyFragmentToExecute=null;
		}
		//mDrawerLayout.closeDrawer(mDrawerList);

	}



	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("vortex","IN ONACTIVITY RESULT");
		Fragment f = getFragmentManager().findFragmentById(R.id.content_frame);
		if (f!=null)
			((Executor)f).getCurrentContext().registerEvent(new WF_Event_OnActivityResult("Start",EventType.onActivityResult));
		super.onActivityResult(requestCode, resultCode, data);
	}




	@Override
	public void onDestroy() {
		if (histT!=null) {
			Log.d("nils","Trying to cancel histT");
			histT.cancel(true);
		}
		if (GlobalState.getInstance()!=null) {

			GlobalState.getInstance().getDb().closeDatabaseBeforeExit();

			GlobalState.destroy();
		}

		super.onDestroy();
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			Log.d("vortex","gets here key back");


			Fragment currentContentFrameFragment = getFragmentManager().findFragmentById(R.id.content_frame);
			if (currentContentFrameFragment!=null && currentContentFrameFragment instanceof Executor) {

 				final WF_Context wfCtx = ((Executor) currentContentFrameFragment).getCurrentContext();
				Log.d("vortex", "current context: " + wfCtx.toString());
				boolean map = false;

				if (wfCtx!=null) {
					if (wfCtx.getCurrentGis()!=null) {
						map=true;
						if (wfCtx.getCurrentGis().wasShowingPopup()) {
							Log.d("vortex","closed popup, exiting");
							return true;
						}
					}
					Workflow wf = wfCtx.getWorkflow();
					Log.d("vortex","gets here wf is "+wf);
					if (wf!=null) {
						if (!wf.isBackAllowed()) {
							new AlertDialog.Builder(this).setTitle("Warning!")
									.setMessage("This will exit the page.")
									.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {

											wfCtx.upOneMapLevel();
											Log.d("vortex","mapLayer is now "+wfCtx.getMapLayer());
											getFragmentManager().popBackStackImmediate();
											//GlobalState.getInstance().setCurrentWorkflowContext(null);
										}})
									.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {

										}})
									.setCancelable(false)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.show();
						} else {
							if (map)
								wfCtx.upOneMapLevel();

							Log.d("vortex","back was allowed");
						}
					}
				}

				if (currentContentFrameFragment instanceof LinjePortalTemplate) {
					final LinjePortalTemplate lp = (LinjePortalTemplate)getFragmentManager().findFragmentById(R.id.content_frame);
					if (lp.isRunning()) {
						new AlertDialog.Builder(this).setTitle("Linjemätning pågår!")
								.setMessage("Vill du verkligen gå ut från sidan?")
								.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										getFragmentManager().popBackStackImmediate();
									}})
								.setNegativeButton(R.string.no,new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {

									}})
								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				}
			}
			setTitle("");

		}

		return super.onKeyDown(keyCode, event);
	}

	private void showErrorMsg(DB_Context context) {

		String dialogText = "Faulty or incomplete context\nError: "+context.toString();
		new AlertDialog.Builder(this)
				.setTitle("Context problem")
				.setMessage(dialogText)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNeutralButton("Ok",new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {


					}
				} )
				.show();

	}




	public DrawerMenu getDrawerMenu() {
		return mDrawerMenu;
	}




	public LoggerI getLogger() {
		if (debugLogger==null) {
			String logLevel =globalPh.get(PersistenceHelper.LOG_LEVEL);
			if (logLevel == null || logLevel.equals(PersistenceHelper.UNDEFINED) ||
					logLevel.equals("normal"))
				debugLogger = new Logger(this,"DEBUG");
			else if (logLevel.equals("off"))
				debugLogger = new DummyLogger();
			else {
				debugLogger = new CriticalOnlyLogger(this);
				Log.d("vortex","critical only");
			}

		}
		return debugLogger;
	}

	
	
 
    
    


    /*
    public void sayHello() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, SyncService.MSG_SAY_HELLO, 0, 0);
        
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    */




}
