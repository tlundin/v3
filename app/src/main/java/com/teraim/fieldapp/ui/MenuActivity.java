package com.teraim.fieldapp.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.synchronization.framework.SyncService;
import com.teraim.fieldapp.utils.PersistenceHelper;

import static com.teraim.fieldapp.dynamic.Executor.REDRAW_PAGE;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends Activity implements TrackerListener   {


	private BroadcastReceiver brr;
	private GlobalState gs;
	protected PersistenceHelper globalPh;
	protected LoggerI debugLogger;
	private boolean initdone=false,initfailed=false;
	private MenuActivity me;
	private Account mAccount;

	public final static String REDRAW = "com.teraim.fieldapp.menu_redraw";
	public static final String INITDONE = "com.teraim.fieldapp.init_done";
	public static final String INITSTARTS = "com.teraim.fieldapp.init_starts";
	public static final String INITFAILED = "com.teraim.fieldapp.init_done_but_failed";
	public static final String SYNC_REQUIRED = "com.teraim.fieldapp.sync_required";




	public class Control {
		public volatile boolean flag = false;
		public boolean error=false;
	}
	final Control control = new Control();
	public class MThread extends Thread {
		public void stopMe() {};
	}
	MThread t;
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
					//set this to listen to Tracker.
					if (GlobalState.getInstance()!=null) {
						Log.d("glapp","menuactivity now listnes to tracker");
						GlobalState.getInstance().getTracker().registerListener(MenuActivity.this);
					}
					//Now sync can start.
					Message msg = Message.obtain(null, SyncService.MSG_REGISTER_CLIENT);
					msg.replyTo = mMessenger;
					try {
						if (mService!=null)
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
					Log.d("initf","got initfailed");
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

		LocalBroadcastManager.getInstance(this).registerReceiver(brr,
				filter);
		//this.registerReceiver(brr, filter);

		//Listen for Service started/stopped event.


		configureSynk();

		latestSignal = null;
	}

	//Tracker callback.
	GPS_State latestSignal=null;
	private final static long GPS_TIME_THRESHOLD = 15000;

	@Override
	public void gpsStateChanged(GPS_State signal) {



		if (signal.state == GPS_State.State.newValueReceived) {
			Log.d("glapp","Got gps signal!");
			latestSignal = signal;
		} else {
			//Log.d("glapp", "Got " + signal.state.toString());
			//latestSignal = null;
		}
		refreshStatusRow();
	}

	private boolean noGPS() {
		return (latestSignal==null);

	}

	private boolean gpsSignalIsOld() {
		long diff = System.currentTimeMillis()-latestSignal.time;
		return (diff>GPS_TIME_THRESHOLD);

	}

	private GPSQuality calculateGPSKQI() {
		if (latestSignal.accuracy<=6)
			return GPSQuality.green;
		else if (latestSignal.accuracy<=10)
			return GPSQuality.yellow;
		else
			return GPSQuality.red;
	}


	@Override
	public void onDestroy()
	{
		Log.d("NILS", "In the onDestroy() event");
		latestSignal = null;
		//Stop listening for bluetooth events.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(brr);

		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}

		if (t!=null)
			t.stopMe();
		super.onDestroy();

	}

	@Override
	protected void onStop() {
		super.onStop();

	}
	@Override
	protected void onStart() {
		super.onStart();
		//Bind to synk service

	}

	@Override
	protected void onResume() {
		super.onResume();
		if("Internet".equals(getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS).getString(PersistenceHelper.SYNC_METHOD,"")))
		{

			Intent myIntent = new Intent(MenuActivity.this, SyncService.class);
			myIntent.setAction(MESSAGE_ACTION);
			bindService(myIntent, mConnection,
					Context.BIND_AUTO_CREATE);
		} else {
			if (mBound) {
				unbindService(mConnection);
				mBound = false;
			}
		}
	};

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
	boolean syncActive=false, syncError = false, syncDbInsert=false;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	AlertDialog uiLock=null;
	Message reply=null;
	private boolean inSync;
	Thread sThread = null;
	private volatile int z_totalSynced = 0;
	private volatile int z_totalToSync = 0;


	class IncomingHandler extends Handler {




		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case SyncService.MSG_SYNC_STARTED:
					Log.d("vortex","MSG -->SYNC STARTED");
					syncActive = true;
					syncError=false;
					break;
				case SyncService.MSG_SYNC_DATA_ARRIVING:
					syncActive=false;
					syncDataArriving=true;
					z_totalSynced = msg.arg1;
					z_totalToSync = msg.arg2;
					Log.d("vortex","MSG_SYNC_DATA_ARRIVING...total rows to sync is "+z_totalToSync);
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
							syncError=true;
							break;
						case SyncService.ERR_SERVER_CONN_TIMEOUT:
							toastMsg = "Me-->Sync Server. Connection timeout";
							//not an error really..just turn off sync.
							break;
						default:
							Log.d("vortex","Any other error!");
							toastMsg = "Me-->Sync Server. Connection failure.";
							syncError=true;
							break;
					}
					if (toastMsg !=null && toastMsg.length()>0) {
						Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
					}
					syncDataArriving=false;
					syncActive=false;
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
						syncDataArriving=false;
						syncError=true;
					}
					break;

				case SyncService.MSG_DEVICE_IN_SYNC:
					Log.d("vortex","***DEVICES IN SYNC***");
					inSync = true;
					syncActive=false;
					syncDataArriving=false;
					syncError = false;
					break;

				case SyncService.MSG_SYNC_RELEASE_DB:
					if (GlobalState.getInstance()!=null) {
						Log.d("vortex", "MSG -->SYNC RELEASE DB LOCK");

						//Create ui provider.


						//Check if something needs to be saved. If so, do it on its own thread.
						if (!syncDbInsert) {
							syncDbInsert = true;
							control.flag = false;
							z_totalToSync = GlobalState.getInstance().getDb().getSyncRowsLeft();
							setSyncState(mnu[1]);
							z_totalSynced = 0;
							final UIProvider ui = new UIProvider(MenuActivity.this);

							Log.d("vortex", "total rows to sync is: " + z_totalToSync);
							if (z_totalToSync==0) {
								Log.e("vortex","sync table is empty! Aborting sync");
								syncDbInsert=false;

								reply = Message.obtain(null, SyncService.MSG_DATA_SAFELY_STORED);
								try {


									mService.send(reply);
								} catch (RemoteException e) {
									e.printStackTrace();
								}

							} else {
								if (t == null) {
									t = new MThread() {
										final int increment = z_totalToSync;

										@Override
										public void stopMe() {
											control.flag = true;
											this.interrupt();
										}

										@Override
										public void run() {
											boolean threadDone = false;
											while (!control.flag) {

												threadDone = GlobalState.getInstance().getDb().scanSyncEntries(control, increment, ui);
												Log.d("vortex", "done scanning syncs...threaddone is " + threadDone + " this is thread " + this.getId());
												if (control.error) {
													Log.d("vortex", "Uppsan...exiting");
													syncError = true;
													syncActive = false;
													syncDbInsert = false;
													break;
												}
												if (!control.flag && !threadDone) {
													z_totalSynced += increment;
												} else {
													control.flag = threadDone;
													gs.sendEvent(REDRAW_PAGE);
													Log.d("vortex", "End reached for sync. Sending msg safely_stored");
													if (uiLock != null)
														uiLock.cancel();
													control.flag = true;
													uiLock = null;

													reply = Message.obtain(null, SyncService.MSG_DATA_SAFELY_STORED);
													syncError = false;
													syncActive = false;
													syncDataArriving = false;

													try {


														mService.send(reply);
													} catch (RemoteException e) {
														e.printStackTrace();
														syncError = true;
														syncActive = false;
														syncDbInsert = false;
														syncDataArriving = false;
														Object fuck = null;
														fuck.equals(fuck);
													}

												}
												if (!control.error) {
													runOnUiThread(new Runnable() {
														@Override
														public void run() {
															setSyncState(mnu[1]);
														}
													});
												}


											}
											Log.d("vortex", "I escaped infite");

											syncDbInsert = false;
											t = null;
											ui.closeProgress();
											if (!control.error) {
												runOnUiThread(new Runnable() {
													@Override
													public void run() {
														refreshStatusRow();
													}
												});
											}
										}
									};

									t.setPriority(Thread.MIN_PRIORITY);
									t.start();

								} else
									Log.e("vortex", "EXTRA CALL ON THREADSTART");
							}

						} else {
							Log.e("vortex", "Extra call made to SYNC RELEASE DB LOCK");
						}
						syncActive = false;
						syncDataArriving=false;
						syncError = false;
					} else {
						syncActive=false;
						syncDataArriving=false;
						syncError=true;
					}
					break;
			}
			//Log.d("Vortex","Refreshing status row. status is se: "+syncError+" sA: "+syncActive);
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

	private final static int NO_OF_MENU_ITEMS = 6;

	MenuItem mnu[] = new MenuItem[NO_OF_MENU_ITEMS];
	final static int MENU_ITEM_GPS_QUALITY 	= 0;
	final static int MENU_ITEM_SYNC_TYPE 	= 1;
	final static int MENU_ITEM_CONTEXT		= 2;
	final static int MENU_ITEM_LOG_WARNING 	= 3;
	final static int MENU_ITEM_SETTINGS 	= 4;
	final static int MENU_ITEM_ABOUT 		= 5;



	ImageView animView = null;

	private void createMenu(Menu menu)
	{
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		animView = (ImageView)inflater.inflate(R.layout.refresh_load_icon, null);

		for(int c=0;c<mnu.length;c++)
			mnu[c]=menu.add(0,c,c,"");



		mnu[MENU_ITEM_GPS_QUALITY].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mnu[MENU_ITEM_SYNC_TYPE].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mnu[MENU_ITEM_CONTEXT].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[MENU_ITEM_LOG_WARNING].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[MENU_ITEM_LOG_WARNING].setIcon(null);
		mnu[MENU_ITEM_SETTINGS].setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mnu[MENU_ITEM_CONTEXT].setTitle(R.string.context);
		mnu[MENU_ITEM_LOG_WARNING].setTitle(R.string.log);
		mnu[MENU_ITEM_SETTINGS].setTitle(R.string.settings);
		mnu[MENU_ITEM_SETTINGS].setIcon(android.R.drawable.ic_menu_preferences);
		mnu[MENU_ITEM_ABOUT].setTitle(R.string.about);

	}

	private enum GPSQuality {
		red,
		yellow,
		green
	}

	protected void refreshStatusRow() {
		//If init failed, show only log and settings
		if (initfailed ) {
			if (mnu[MENU_ITEM_LOG_WARNING]!=null) {
				mnu[MENU_ITEM_LOG_WARNING].setVisible(true);
				mnu[MENU_ITEM_SETTINGS].setVisible(true);
				mnu[MENU_ITEM_SETTINGS].setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			}
			//Init done succesfully? Show all items.
		} else 	if (GlobalState.getInstance()!=null && initdone) {

			if (noGPS())
				mnu[MENU_ITEM_GPS_QUALITY].setVisible(false);
			else {
				mnu[MENU_ITEM_GPS_QUALITY].setVisible(true);
				//if (gpsSignalIsOld())
				//	mnu[0].setIcon(R.drawable.btn_icon_none);
				//else
				{
					GPSQuality GPSq = calculateGPSKQI();
					if (GPSq == GPSQuality.green) {
						mnu[MENU_ITEM_GPS_QUALITY].setIcon(R.drawable.btn_icon_ready);
					} else if (GPSq == GPSQuality.yellow)
						mnu[MENU_ITEM_GPS_QUALITY].setIcon(R.drawable.btn_icon_started);
					else
						mnu[MENU_ITEM_GPS_QUALITY].setIcon(R.drawable.btn_icon_started_with_errors);
					mnu[MENU_ITEM_GPS_QUALITY].setTitle(Math.round(latestSignal.accuracy)+"");
				}
			}

			gs = GlobalState.getInstance();
			if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE") || gs.isSolo())
				mnu[MENU_ITEM_SYNC_TYPE].setVisible(false);
			else
				setSyncState(mnu[MENU_ITEM_SYNC_TYPE]);

			mnu[MENU_ITEM_CONTEXT].setVisible(true);
			mnu[3].setVisible(!globalPh.get(PersistenceHelper.LOG_LEVEL).equals("off"));
			if (debugLogger!=null && debugLogger.hasRed()) {
				mnu[MENU_ITEM_LOG_WARNING].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				mnu[MENU_ITEM_LOG_WARNING].setIcon(R.drawable.warning);
			}
			mnu[MENU_ITEM_SETTINGS].setVisible(true);
			mnu[MENU_ITEM_ABOUT].setVisible(true);

		}
	}




	boolean animationRunning = false,syncDataArriving=false;

	private void setSyncState(final MenuItem menuItem) {
		//Log.d("vortex","Entering setsyncstate");
		String title=null;
		boolean internetSync = globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet");
		Integer ret = R.drawable.syncoff;
		int numOfUnsynchedEntries = -1;
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth")) {
			ret = R.drawable.bt;
			title = gs.getDb().getNumberOfUnsyncedEntries()+"";
		}
		else if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE"))
			ret = null;

		else if (internetSync) {

			if (syncError) {
				//menuItem.setTitle("!ERROR!");
				ret = R.drawable.syncerr;
				title = "";
			}
			else if (!ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY))
				ret = R.drawable.syncoff;
			else if (inSync) {
				numOfUnsynchedEntries = gs.getDb().getNumberOfUnsyncedEntries();
				if (numOfUnsynchedEntries>0) {
					inSync=false;
					ret = R.drawable.syncon;

				} else {
					ret = R.drawable.insync;
					title = "";
				}
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
							syncError = false;
						}
					});
					animationRunning=true;
				}
				return;
			}
			else if (syncDataArriving) {
				title = (z_totalSynced+"/"+z_totalToSync);
				ret = R.drawable.syncactive;
			}
			else if (syncDbInsert) {
				title = (z_totalSynced+"/"+z_totalToSync);
				ret = R.drawable.dbase;
			}
			else {
				ret = R.drawable.syncon;
				numOfUnsynchedEntries = gs.getDb().getNumberOfUnsyncedEntries();
				Log.d("vortex","icon set to syncon!");
			}
		}
		if ( title == null & numOfUnsynchedEntries > 0)
			title = numOfUnsynchedEntries+"";
		animationRunning = false;
		animView.clearAnimation();
		menuItem.setActionView(null);
		menuItem.setOnMenuItemClickListener(null);
		animView.setOnClickListener(null);
		menuItem.setIcon(ret);
		menuItem.setTitle(title);
		menuItem.setVisible(true);
		//Log.d("vortex","Exiting setsyncstate");
	}


	private boolean menuChoice(MenuItem item) {

		int selection = item.getItemId();
		//case must be constant..

		switch (selection) {
			case MENU_ITEM_GPS_QUALITY:
				if (latestSignal!=null) {
					new AlertDialog.Builder(this)
							.setTitle("GPS Details")
							.setMessage("GPS_X: " + latestSignal.x + "\n" +
									"GPS_Y: " + latestSignal.y + "\n" +
									"Accuracy: " + latestSignal.accuracy + "\n" +
									"Time since last value: " + Math.round((System.currentTimeMillis() - latestSignal.time)/1000)+"s")
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(true)
							.setNeutralButton("Ok", new Dialog.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {

								}
							})
							.show();
				} else
					refreshStatusRow();
				break;
			case MENU_ITEM_SYNC_TYPE:
				toggleSyncOnOff();
				break;
			case MENU_ITEM_CONTEXT:
				Log.d("vortex","gs is "+GlobalState.getInstance()+" gs "+gs);
				//Log.d("vortex","in click for context: gs "+(gs==null)+" varc "+(gs.getVariableCache()==null));
				if (gs!=null && gs.getVariableCache()!=null) {
					//Object moo=null;
					//moo.equals("moo");
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

			case MENU_ITEM_LOG_WARNING:
				mnu[MENU_ITEM_LOG_WARNING].setIcon(null);
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
						gs.getVariableCache().printCache();
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
				Button printLog = (Button)dialog.findViewById(R.id.printlog);

				print.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						gs.getBackupManager().backupDatabase("dump");
					}
				});

				printLog.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						printLog(log.getLogText());
					}

					private void printLog(CharSequence logText) {
						if (logText==null || logText.length()==0)
							return;
						try {
							String fileName = "log.txt";
							File outputFile = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/backup/", fileName);
							BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
							writer.write(logText.toString());
							Toast.makeText(MenuActivity.this.getApplicationContext(),
									"LOG successfully written to backup folder. Name: " + fileName,
									Toast.LENGTH_LONG).show();
							writer.close();
						}
						catch (IOException e) {
							Log.e("Exception", "File write failed: " + e.toString());
						}

					}
				});
				break;

			case MENU_ITEM_SETTINGS:
				//close drawer menu if open
				if (Start.singleton.getDrawerMenu()!=null)
					Start.singleton.getDrawerMenu().closeDrawer();
				if(isSynkServiceRunning())
					stopSync();
				Intent intent = new Intent(getBaseContext(),ConfigMenu.class);
				startActivity(intent);
				return true;
			case MENU_ITEM_ABOUT:
                Intent myIntent = new Intent(this, AboutActivity.class);
                startActivity(myIntent);

				break;
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
					if (!isSynkServiceRunning()) {
						Log.d("vortex", "The service is not bound yet...try that first before starting it.");
						this.onResume();

					}

					ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);
					if (isSynkServiceRunning()) {
						Log.d("vortex","sync service is running...sending msg");
						reply = Message.obtain(null, SyncService.MSG_USER_STARTED_SYNC);
						try {
							mService.send(reply);
						} catch (RemoteException e) {
							e.printStackTrace();
							syncActive = false;
							syncError = true;
						}
					}
				} else {
					stopSync();



				}

				refreshStatusRow();

			}
		}
	}

	private void stopSync() {
		Log.d("vortex", "Trying to stop Internet sync");
		ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);
		if (syncError) {
			Log.d("vortex","Resetting sync error");
			syncError=false;
		}
		if (syncDbInsert){

			t.stopMe();
			Log.e("vortex","control flag is now true");
		}
		reply = Message.obtain(null, SyncService.MSG_USER_STOPPED_SYNC);
		try {
			mService.send(reply);
		} catch (RemoteException e) {
			e.printStackTrace();
			syncActive=false;
			syncError=true;
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

		public static final int LOCK =1, UNLOCK=2, ALERT = 3, UPDATE_SUB = 4, CONFIRM = 5, UPDATE = 6, PROGRESS = 7,SHOW_PROGRESS=8,CLOSE_PROGRESS=9;
		private String row1="",row2="";
		private AlertDialog uiBlockerWindow=null;
		public static final int Destroy_Sync = 1;
		private ProgressDialog progress;

		Handler mHandler= new Handler(Looper.getMainLooper()) {
			boolean twoButton=false;

			private void showProgress(int max) {
				progress = new ProgressDialog(mContext);
				progress.setIndeterminate(false);
				progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progress.setMax(max);
				progress.setTitle("Inserting");
				progress.show();

			}
			private void progress(int curr) {
				if (progress!=null) {
					Log.d("vortex","progress: "+curr);
					progress.setProgress(curr);
				}
			}


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
					case SHOW_PROGRESS:
						showProgress(msg.arg1);
						break;
					case PROGRESS:
						progress(msg.arg1);
						break;
					case CLOSE_PROGRESS:
						if (progress!=null) {
							progress.dismiss();
							progress=null;
						}
						break;
					case LOCK:
						oneButton();
						Log.d("vortex","One button Lock interface");
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


		public void startProgress(int totalRows) {
			if (progress==null) {
				Message msg = mHandler.obtainMessage(SHOW_PROGRESS);
				msg.arg1=totalRows;
				msg.sendToTarget();
			}
		}

		public void setProgress(int count) {
			Message msg = mHandler.obtainMessage(PROGRESS);
			msg.arg1=count;
			msg.sendToTarget();

		}
		public void closeProgress() {
			mHandler.obtainMessage(CLOSE_PROGRESS).sendToTarget();
		}

		public void lock() {
			Log.d("vortex","Lock called");
			mHandler.obtainMessage(LOCK).sendToTarget();

		}
		public void unlock() {
			Log.d("vortex","Lock called");
			mHandler.obtainMessage(UNLOCK).sendToTarget();

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
