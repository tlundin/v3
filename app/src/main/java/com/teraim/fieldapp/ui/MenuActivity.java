package com.teraim.fieldapp.ui;

import java.util.Map;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.synchronization.framework.SyncService;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends Activity   {


	private BroadcastReceiver brr;
	private GlobalState gs;
	protected PersistenceHelper globalPh;
	protected LoggerI debugLogger;
	private boolean initdone=false,initfailed=false;
	private MenuActivity me;
	private Account mAccount;

	public final static String REDRAW = "com.teraim.fieldapp.menu_redraw";
	public static final String INITDONE = "com.teraim.fieldapp.init_done";
	public static final String INITSTARTS = "com.teraim.fieldapp.init_done";
	public static final String INITFAILED = "com.teraim.fieldapp.init_done_but_failed";	
	public static final String SYNC_REQUIRED = "com.teraim.fieldapp.sync_required";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		me = this;

		globalPh = new PersistenceHelper(getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS));

		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("nils", "Broadcast: "+intent.getAction());

				if (intent.getAction().equals(INITDONE)) {
					initdone=true;
					//Now sync can start.
					Message msg = Message.obtain(null, SyncService.MSG_REGISTER_CLIENT);
					msg.replyTo = mMessenger;
					try {
						mService.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				else if (intent.getAction().equals(INITSTARTS)) {
					initdone=false;
					initfailed=false;
				}
				else if (intent.getAction().equals(INITFAILED)) {
					initfailed=true;
				}
				else if (intent.getAction().equals(SYNC_REQUIRED)) {
					 new AlertDialog.Builder(MenuActivity.this)
						.setTitle("Synchronize")
						.setMessage("The action you just performed mandates a synchronisation. Please synchronise with your partner before continuing.") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								uiLock=null;			
							}
						})
						.show();
				}
					

				me.refreshStatusRow();
			}


		};
		//Listen for bluetooth events.
		IntentFilter filter = new IntentFilter();
		filter.addAction(INITDONE);		
		filter.addAction(INITSTARTS);	
		filter.addAction(REDRAW);
		filter.addAction(INITFAILED);

		this.registerReceiver(brr, filter);
		//Listen for Service started/stopped event.


		configureSynk();
	}




	@Override
	public void onDestroy()
	{
		Log.d("NILS", "In the onDestroy() event");

		//Stop listening for bluetooth events.
		this.unregisterReceiver(brr);
		super.onDestroy();

	}

	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
	@Override
	protected void onStart() {
		super.onStart();
		//Bind to synk service
		Intent myIntent = new Intent(MenuActivity.this, SyncService.class);
		myIntent.setAction(MESSAGE_ACTION);
		bindService(myIntent, mConnection,
				Context.BIND_AUTO_CREATE);
	}

	/** Flag indicating whether we have called bind on the service. */
	boolean mBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service.  We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			mService = new Messenger(service);
			mBound = true;

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			mBound = false;
			syncError=true;
			syncActive=false;
			me.refreshStatusRow();
		}
	};

	public boolean isSynkServiceRunning() {
		return mBound;
	}


	Messenger mService = null;
	boolean syncActive=false, syncError = false;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	AlertDialog uiLock=null;
	Message reply=null;
	private boolean inSync;
	class IncomingHandler extends Handler {


		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case SyncService.MSG_SYNC_STARTED:
				Log.d("vortex","MSG -->SYNC STARTED");
				syncActive = true;
				syncError=false;
				break;

			case SyncService.MSG_SYNC_ERROR_STATE:
				String toastMsg = "";
				Log.d("vortex","MSG -->SYNC ERROR STATE");
				switch(msg.arg1) {
				case SyncService.ERR_NOT_INTERNET_SYNC:				
					Log.d("vortex","ERR NOT INTERNET SYNC...");
					//Send a config changed msg...reload!				
					Log.d("vortex","Turning sync off.");
					ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);					
					break;
				case SyncService.ERR_SETTINGS:
					Log.d("vortex","ERR WRONG SETTINGS...");				
					if (uiLock == null) {
						uiLock = new AlertDialog.Builder(MenuActivity.this)
						.setTitle("Synchronize")
						.setMessage("Please enter a team and a user name under Settings.") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								uiLock=null;			
							}
						})
						.show();
					}

					break;

				case SyncService.ERR_SERVER_NOT_REACHABLE:
					Log.d("vortex","Synkserver is currently not reachable.");
					toastMsg = "Me --> Sync Server. No route";
					break;
				case SyncService.ERR_SERVER_CONN_TIMEOUT:
					toastMsg = "Me-->Sync Server. Connection timeout";
					break;
				default:
					Log.d("vortex","Any other error!");
					toastMsg = "Me-->Sync Server. Connection failure.";
					break;
				}
				if (toastMsg !=null && toastMsg.length()>0) {
					Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
				}
				syncActive=false;
				syncError=true;				
				break;



			case SyncService.MSG_SYNC_REQUEST_DB_LOCK:
				Log.d("vortex","MSG -->SYNC REQUEST DB LOCK");
				inSync = false;
				/*
				uiLock = new AlertDialog.Builder(MenuActivity.this)
				.setTitle("Synchronizing")
				.setMessage("Inserting data from team..please wait") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.d("Vortex","User closed lock window.");			
					}
				})
				.show();
				 */
				reply = Message.obtain(null, SyncService.MSG_DATABASE_LOCK_GRANTED);
				try {
					mService.send(reply);
				} catch (RemoteException e) {
					e.printStackTrace();
					syncActive=false;
					syncError=true;
				}
				break;

			case SyncService.MSG_DEVICES_IN_SYNC:
				Log.d("vortex","Oh glorious days!");
				inSync = true;
				syncActive=false;
				syncError = false;
				break;

			case SyncService.MSG_SYNC_RELEASE_DB:
				Log.d("vortex","MSG -->SYNC RELEASE DB LOCK");
				if (uiLock != null)
					uiLock.cancel();
				uiLock=null;
				//Check if something needs to be saved. If so, do it on its own thread.
				Thread thread = new Thread() {
					@Override
					public void run() {
						if (GlobalState.getInstance().getDb().scanSyncEntries()) {
							Log.d("vortex","Changes made by sync. Requesting page redraw.");
							gs.sendEvent(Executor.REDRAW_PAGE);
						}
						reply = Message.obtain(null, SyncService.MSG_DATA_SAFELY_STORED);
						syncError=false;
						syncActive=false;
						try {
							mService.send(reply);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {

									refreshStatusRow();

								}
							});

						} catch (RemoteException e) {
							e.printStackTrace();
							syncError=true;syncActive=false;
						}
					}

				};

				thread.start();
				syncActive=true;
				syncError=false;
				break;
			}
			Log.d("Vortex","Refreshing status row. status is se: "+syncError+" sA: "+syncActive);
			refreshStatusRow();
		}

	}




	public static final String MESSAGE_ACTION = "Massage_Massage";


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		createMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		refreshStatusRow();
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return menuChoice(item);
	}

	private final static int NO_OF_MENU_ITEMS = 4;

	MenuItem mnu[] = new MenuItem[NO_OF_MENU_ITEMS];
	ImageView animView = null;

	private void createMenu(Menu menu)
	{
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		animView = (ImageView)inflater.inflate(R.layout.refresh_load_icon, null);

		for(int c=0;c<mnu.length;c++) 
			mnu[c]=menu.add(0,c,c,"");



		mnu[0].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mnu[1].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[2].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[2].setIcon(null);
		mnu[mnu.length-1].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[1].setTitle(R.string.context);
		mnu[2].setTitle(R.string.log);
		mnu[mnu.length-1].setTitle(R.string.settings);
		mnu[mnu.length-1].setIcon(android.R.drawable.ic_menu_preferences);


	}

	protected void refreshStatusRow() {
		if (initfailed) {
			mnu[2].setVisible(true);
			mnu[mnu.length-1].setVisible(true);
			mnu[mnu.length-1].setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		} else 	if (GlobalState.getInstance()!=null && initdone) { 
			gs = GlobalState.getInstance();
			if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE") || gs.isSolo()) 
				mnu[0].setVisible(false);
			else 
				setSyncState(mnu[0]);

			mnu[1].setVisible(true);		
			mnu[2].setVisible(!globalPh.get(PersistenceHelper.LOG_LEVEL).equals("off"));
			if (debugLogger.hasRed()) {
				mnu[2].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				mnu[2].setIcon(R.drawable.warning);
			}
			mnu[mnu.length-1].setVisible(true);
		}
	}

	boolean animationRunning = false;

	private void setSyncState(final MenuItem menuItem) {
		//Log.d("vortex","Entering setsyncstate");
		menuItem.setTitle(gs.getDb().getNumberOfUnsyncedEntries()+"");
		boolean internetSync = globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet");
		Integer ret = R.drawable.syncoff;
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth"))
			ret = R.drawable.bt;
		else if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE"))
			ret = null;

		else if (internetSync) {
			if (syncError) {
				//menuItem.setTitle("!ERROR!");
				ret = R.drawable.syncerr;
			} 
			else if (!ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY))
				ret = R.drawable.syncoff;
			else if (inSync) {
				ret = R.drawable.insync;
			}
			else if (syncActive) {
				if (!animationRunning) {
					Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
					rotation.setRepeatCount(Animation.INFINITE);
					animView.startAnimation(rotation);
					menuItem.setActionView(animView);
					animView.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							animView.clearAnimation();
							menuItem.setActionView(null); 
							menuItem.setOnMenuItemClickListener(null);
							animView.setOnClickListener(null);
							animationRunning = false;
						}
					});
				}
				return;
			} else 
				ret = R.drawable.syncon;

		}
		animationRunning = false;
		animView.clearAnimation();
		menuItem.setActionView(null); 
		menuItem.setOnMenuItemClickListener(null);
		animView.setOnClickListener(null);
		menuItem.setIcon(ret);
		menuItem.setVisible(true);
		//Log.d("vortex","Exiting setsyncstate");
	}


	private boolean menuChoice(MenuItem item) {

		int selection = item.getItemId();
		//case must be constant..
		if (selection == mnu.length-1)
			selection = 99;

		switch (selection) {
		case 0:
			toggleSyncOnOff();
			break;
		case 1:			
			if (gs!=null && gs.getVariableCache()!=null) {
			new AlertDialog.Builder(this)
			.setTitle("Context")
			.setMessage(gs.getVariableCache().getContext().toString()) 
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
			}
			break;

		case 2:
			mnu[2].setIcon(null);
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.log_dialog_popup);
			dialog.setTitle("Session Log");
			final TextView tv=(TextView)dialog.findViewById(R.id.logger);
			final ScrollView sv=(ScrollView)dialog.findViewById(R.id.logScroll);
			Typeface type=Typeface.createFromAsset(getAssets(),
					"clacon.ttf");
			tv.setTypeface(type);
			final LoggerI log = Start.singleton.getLogger();
			log.setOutputView(tv);
			//trigger redraw.
			log.draw();
			Button close=(Button)dialog.findViewById(R.id.log_close);
			dialog.show();
			close.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					log.setOutputView(null);
				}
			});
			Button clear = (Button)dialog.findViewById(R.id.log_clear);
			clear.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					log.clear();
				}
			});
			Button scrollD = (Button)dialog.findViewById(R.id.scrollDown);
			scrollD.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					sv.post(new Runnable() {
						@Override
						public void run() {
							sv.fullScroll(ScrollView.FOCUS_DOWN);
						}
					});
				}
			});
			Button print = (Button)dialog.findViewById(R.id.printdb);
			print.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					gs.getBackupManager().backupDatabase("dump");
				}
			});

			break;

		case 99:
			//close drawer menu if open
			if (Start.singleton.getDrawerMenu()!=null)
				Start.singleton.getDrawerMenu().closeDrawer();
			Intent intent = new Intent(getBaseContext(),ConfigMenu.class);
			startActivity(intent);
			return true;
		}
		return false;
	}


	private void toggleSyncOnOff() {
		String syncMethod = globalPh.get(PersistenceHelper.SYNC_METHOD);
		if (syncMethod.equals("Bluetooth")) {
			DataSyncSessionManager.start(MenuActivity.this, new UIProvider(this) {
				@Override
				public void onClose() {
					me.onCloseSync();

				};
			});
		} else {
			if (syncMethod.equals("Internet")) {
				if (!ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY)) {
					Log.d("vortex", "Trying to start Internet sync");
					//Check there is name and team.
					String user = globalPh.get(PersistenceHelper.USER_ID_KEY);
					String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
					if (user==null || user.length()==0 || team==null || team.length()==0) {
						new AlertDialog.Builder(this)
						.setTitle("Sync cannot start")
						.setMessage("Missing team ["+team+"] or user name ["+user+"]. Please add under the Settings menu") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								syncError=false;
								syncActive=false;
							}
						})
						.show();
					}

					/*
					if (!globalPh.getB(PersistenceHelper.SYNC_ON_FIRST_TIME_KEY)) {
						globalPh.put(PersistenceHelper.SYNC_ON_FIRST_TIME_KEY,true);

						new AlertDialog.Builder(this)
						.setTitle("Sync starting up")
						.setMessage("The sync symbol turns green only when data succesfully reaches the server. Your sync interval is set to: "+Start.SYNC_INTERVAL+" seconds. If the symbol is not green after this time, you likely have network issues.") 
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						})
						.show();
					}
					 */
					ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);		
				} else {
					Log.d("vortex", "Trying to stop Internet sync");
					ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);
				}

				refreshStatusRow();


			}
		}
	}
	private void configureSynk() {
		mAccount = GlobalState.getmAccount(getApplicationContext());

		ContentResolver.addPeriodicSync(
				mAccount,
				Start.AUTHORITY,
				Bundle.EMPTY,
				Start.SYNC_INTERVAL);
		Log.d("vortex","added periodic sync to account.");
	}
	/*
	public void stopSynk() {
		if (!isSynkServiceRunning()) {
			Log.d("vortex","Cannot stop synk. It is not running");
			return;
		} 

		Account mAccount = GlobalState.getmAccount(getApplicationContext());
		ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);

		if (mService!=null) {
			Message msg = Message.obtain(null, SyncService.MSG_STOP_REQUESTED);

			msg.replyTo = mMessenger;
			try {
				mService.send(msg);
				if (mBound) {
					unbindService(mConnection);
					mBound = false;

				}
			} catch (RemoteException e) {

				new AlertDialog.Builder(getApplicationContext())
				.setTitle("Stop failed")
				.setMessage("The Sync Service did not stop properly. It is strongly recommended you restart the App.") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.show();
				e.printStackTrace();
			}
		}
	}
	 */



	protected void onCloseSync() {
		Log.d("vortex","IN on close SYNC!!");
		refreshStatusRow();
		DataSyncSessionManager.stop();
	}


	/**
	 * 
	 * @author Terje
	 *
	 * Helper class that allows other threads to interact with the UI main thread. 
	 * Caller must override onClose for specific actions to happen when aborting sync.
	 */

	public class UIProvider {

		public static final int LOCK =1, UNLOCK=2, ALERT = 3, UPDATE_SUB = 4, CONFIRM = 5, UPDATE = 6; 
		private String row1="",row2="";
		private AlertDialog uiBlockerWindow=null;  
		public static final int Destroy_Sync = 1;

		Handler mHandler= new Handler(Looper.getMainLooper()) {
			boolean twoButton=false;

			private void oneButton() {
				dismiss();
				uiBlockerWindow =  new AlertDialog.Builder(mContext)
				.setTitle("Synchronizing")
				.setMessage("Receiving data..standby") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						onClose();
					}
				})
				.show();
				twoButton=false;
			}

			private void twoButton(final ConfirmCallBack cb) {
				dismiss();
				uiBlockerWindow =  new AlertDialog.Builder(mContext)
				.setTitle("Synchronizing")
				.setMessage("Receiving data..standby") 
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						//onClose must be overridden. 
						onClose();				
					}
				})
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						cb.confirm();

					}
				})
				.show();
				twoButton=true;
			}


			private void dismiss() {
				if (uiBlockerWindow!=null)
					uiBlockerWindow.dismiss();
			}

			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case LOCK:
					oneButton();
					Log.d("vortex","I get here?");
					break;
				case UNLOCK:
					uiBlockerWindow.cancel();
					break;
				case ALERT:
					if (twoButton)
						oneButton();
					row1 = (String)msg.obj;
					row2="";
					uiBlockerWindow.setMessage(row1+"\n"+row2);



					break;
				case UPDATE:
					row1 = (String)msg.obj;
					uiBlockerWindow.setMessage(row1+"\n"+row2);
					break;
				case UPDATE_SUB:
					row2 = (String)msg.obj;
					uiBlockerWindow.setMessage(row1+"\n"+row2);
					break;

				case CONFIRM:
					if (!twoButton)
						twoButton((ConfirmCallBack)msg.obj);

					break;

				}


			}


		};
		private Context mContext;

		public UIProvider(Context context) {

			mContext = context;
		}





		public void lock() {
			Log.d("vortex","Lock called");
			mHandler.obtainMessage(LOCK).sendToTarget();

		}

		/**
		 * 
		 * @param msg
		 * Shows message and switched to one button dialog.
		 */
		public void alert(String msg) {
			mHandler.obtainMessage(ALERT,msg).sendToTarget();

		}

		public void syncTry(String msg) {
			mHandler.obtainMessage(ALERT,msg).sendToTarget();

		}

		public void open() {
			mHandler.obtainMessage(UNLOCK).sendToTarget();
		}

		public void setInfo(String msg) {
			mHandler.obtainMessage(UPDATE_SUB,msg).sendToTarget();

		}


		public void onClose() {

		}

		/**
		 * 
		 * @param msg
		 * Shows message and switched to two button dialog. Callback if positive ok is pressed. Otherwise onClose.
		 */
		public void confirm(String msg, final ConfirmCallBack cb) {
			mHandler.obtainMessage(CONFIRM,cb).sendToTarget();
			mHandler.obtainMessage(UPDATE,msg).sendToTarget();
		}

		/**
		 * 
		 * @param msg
		 * Does not change dialog type.
		 */
		public void update(String msg) {
			mHandler.obtainMessage(UPDATE,msg).sendToTarget();			


		}











	}





}
