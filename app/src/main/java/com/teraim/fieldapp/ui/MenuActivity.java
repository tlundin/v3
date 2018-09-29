package com.teraim.fieldapp.ui;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.synchronization.framework.SyncAdapter;
import com.teraim.fieldapp.synchronization.framework.SyncService;
import com.teraim.fieldapp.utils.BackupManager;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.teraim.fieldapp.dynamic.Executor.REDRAW_PAGE;
/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends AppCompatActivity implements TrackerListener   {


    private BroadcastReceiver brr;
    private GlobalState gs;
    protected PersistenceHelper globalPh;
    protected LoggerI debugLogger;
    private boolean initdone=false,initfailed=false;
    private MenuActivity me;
    private Account mAccount;
    private Runnable syncP;
    private Handler handler;

    public final static String REDRAW = "com.teraim.fieldapp.menu_redraw";
    public static final String INITDONE = "com.teraim.fieldapp.init_done";
    public static final String INITSTARTS = "com.teraim.fieldapp.init_starts";
    public static final String INITFAILED = "com.teraim.fieldapp.init_done_but_failed";
    public static final String SYNC_REQUIRED = "com.teraim.fieldapp.sync_required";

    protected PopupWindow mPopupWindow;
    private Switch sync_switch;
    private Button sync_button;


    static class MThread extends Thread {
        void stopMe() {}
    }

    private MThread t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;
        //Check that all permissions are granted.
        /*
        if(!hasPermissions(this, PERMISSIONS)){
            Log.d("grox","got here");
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else
        */
        continueCreate();

    }
    private void continueCreate() {
        globalPh = new PersistenceHelper(getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS));

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

                    Message msg = Message.obtain(null, SyncService.MSG_REGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    try {
                        if (mService!=null)
                            mService.send(msg);
                        //Now sync can start.
                        toggleSyncOnOff(ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY));
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
                            .setPositiveButton(R.string.ok, (dialog, which) -> uiLock=null)
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

        //create popupwindow.
        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        // Inflate the inner layout/view
        View syncpop = inflater.inflate(R.layout.sync_popup_inner,null);

        // Initialize a new instance of popup window
        mPopupWindow = new PopupWindow(
                syncpop,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        if(Build.VERSION.SDK_INT>=21){
            mPopupWindow.setElevation(5.0f);
        }
        Button closeButton = syncpop.findViewById(R.id.close_button);
        closeButton.setOnClickListener(view -> {
            // Dismiss the popup window
            mPopupWindow.dismiss();
        });
        sync_button = syncpop.findViewById(R.id.sync_button);
        sync_button.setOnClickListener(view -> {
            //Try to force immediate.
            SyncAdapter.forceSyncToHappen();
        });
        Button refresh_button = syncpop.findViewById(R.id.refresh_button);
        refresh_button.setOnClickListener(view -> {
            rowBuffer.clear();
            gs.getServerSyncStatus();
        });
        sync_switch = syncpop.findViewById(R.id.sync_switch);


        sync_switch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    String user = globalPh.get(PersistenceHelper.USER_ID_KEY, null);
                    String team = globalPh.get(PersistenceHelper.LAG_ID_KEY, null);
                    if (team == null || team.isEmpty() || user == null || user.isEmpty()) {
                        Toast.makeText(MenuActivity.this, "Username and/or team name missing", Toast.LENGTH_LONG).show();
                        sync_switch.setOnCheckedChangeListener(null);
                        sync_switch.setChecked(false);
                        sync_switch.setOnCheckedChangeListener(this);
                    } else
                        toggleSyncOnOff(true);
                } else
                    toggleSyncOnOff(false);

            }
        });

    }



    private boolean syncOn() {

        return ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY);
    }

    //Tracker callback.
    private GPS_State latestSignal=null;
    private final static long GPS_TIME_THRESHOLD = 15000;

    @Override
    public void gpsStateChanged(GPS_State signal) {
        if (signal.state == GPS_State.State.newValueReceived) {
            Log.d("glapp","Got gps signal!");
            latestSignal = signal;
        }
        refreshStatusRow();
    }

    private boolean noGPS() {
        return (latestSignal==null);

    }

// --Commented out by Inspection START (9/29/2018 6:36 PM):
//    private boolean gpsSignalIsOld() {
//        long diff = System.currentTimeMillis()-latestSignal.time;
//        return (diff>GPS_TIME_THRESHOLD);
//
//    }
// --Commented out by Inspection STOP (9/29/2018 6:36 PM)

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
        if(syncP!=null && handler !=null)
            handler.removeCallbacks(syncP);
    }
    @Override
    protected void onStart() {
        super.onStart();
       
    }

    @Override
    protected void onResume() {
        super.onResume();
        if("Internet".equals(getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS).getString(PersistenceHelper.SYNC_METHOD,"")))
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
    }


    /** Flag indicating whether we have called bind on the service. */
    private boolean mBound;

    private final ServiceConnection mConnection = new ServiceConnection() {
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

    private boolean isSynkServiceRunning() {
        return mBound;
    }


    private Messenger mService = null;
    private boolean syncActive=false;
    private boolean syncError = false;
    private boolean syncDbInsert=false;

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private AlertDialog uiLock=null;
    private Message reply=null;
    private boolean inSync;
    private volatile int z_totalSynced = 0;
    private volatile int z_totalToSync = 0;


    public static class IncomingHandler extends Handler {

        final MenuActivity menuActivity;

        public class Control {
            public volatile boolean flag = false;
            public boolean error=false;
        }
        final Control control = new Control();


        IncomingHandler(MenuActivity menuActivity) {
            this.menuActivity=menuActivity;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case SyncService.MSG_SYNC_STARTED:
                    Log.d("vortex","MSG -->SYNC STARTED");
                    GlobalState.getInstance().getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC_INET+menuActivity.globalPh.get(PersistenceHelper.LAG_ID_KEY),System.currentTimeMillis());
                    menuActivity.syncActive = true;
                    menuActivity.syncError=false;
                    break;

                case SyncService.MSG_SERVER_READ_MY_DATA:
                    long timestampFromMe = ((Bundle)msg.obj).getLong("maxstamp");
                    GlobalState.getInstance().getPreferences().put(PersistenceHelper.TIMESTAMP_LAST_SYNC_FROM_ME+menuActivity.globalPh.get(PersistenceHelper.LAG_ID_KEY),timestampFromMe);
                    GlobalState.getInstance().getDb().saveTimeStamp(GlobalState.getInstance().getMyTeam(),timestampFromMe);
                    Log.d("burlesk", "ME-->TEAM UPDATED TO? "+timestampFromMe+" --: "+GlobalState.getInstance().getPreferences().getL(PersistenceHelper.TIMESTAMP_LAST_SYNC_FROM_ME+menuActivity.globalPh.get(PersistenceHelper.LAG_ID_KEY)));
                    //Log.d("biff","Timestamp for last sync in Query is  "+timestampFromMe);
                    break;

                case SyncService.MSG_SYNC_DATA_ARRIVING:
                    menuActivity.syncActive=false;
                    menuActivity.syncDataArriving=true;
                    Bundle b = (Bundle)msg.obj;
                    menuActivity.z_totalSynced=b.getInt("number_of_rows_so_far");
                    menuActivity.z_totalToSync = b.getInt("number_of_rows_total");
                    Log.d("vortex","MSG_SYNC_DATA_ARRIVING...total rows to sync is "+menuActivity.z_totalToSync);
                    break;

                case SyncService.MSG_SYNC_ERROR_STATE:
                    String toastMsg = "";
                    Log.d("vortex","MSG -->SYNC ERROR STATE");
                    switch(msg.arg1) {
                        case SyncService.ERR_NOT_INTERNET_SYNC:
                            Log.d("vortex","ERR NOT INTERNET SYNC...");
                            //Send a config changed msg...reload!
                            Log.d("vortex","Turning sync off.");
                            menuActivity.stopSync();

                            break;
                        case SyncService.ERR_SETTINGS:
                            Log.d("vortex","ERR WRONG SETTINGS...");
                            if (menuActivity.uiLock == null) {
                                menuActivity.uiLock = new AlertDialog.Builder(menuActivity)
                                        .setTitle("Synchronize")
                                        .setMessage("Please enter a team and a user name under Settings.")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setCancelable(false)
                                        .setNegativeButton(R.string.close, (dialog, which) -> {
                                            if (menuActivity.syncOn()) {
                                                menuActivity.stopSync();
                                            }
                                            menuActivity.uiLock=null;
                                        })
                                        .show();
                            }

                            break;

                        case SyncService.ERR_SERVER_NOT_REACHABLE:
                            Log.d("vortex","Synkserver is currently not reachable.");
                            toastMsg = "Me --> Sync Server. No route";
                            menuActivity.syncError=true;
                            break;
                        case SyncService.ERR_SERVER_CONN_TIMEOUT:
                            toastMsg = "Me-->Sync Server. Connection timeout";
                            Log.d("vortex","Synkserver Timeout.");
                            //not an error really..just turn off sync.
                            menuActivity.syncError=true;
                            break;
                        default:
                            Log.d("vortex","Any other error!");
                            toastMsg = "Me-->Sync Server. Connection failure.";
                            menuActivity.syncError=true;
                            break;
                    }
                    if (toastMsg.length()>0) {
                        Toast.makeText(menuActivity.getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
                    }
                    menuActivity.syncDataArriving=false;
                    menuActivity.syncActive=false;
                    break;



                case SyncService.MSG_SYNC_REQUEST_DB_LOCK:
                    Log.d("vortex","MSG -->SYNC REQUEST DB LOCK");
                    menuActivity.inSync = false;
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
                    menuActivity.reply = Message.obtain(null, SyncService.MSG_DATABASE_LOCK_GRANTED);
                    try {
                        menuActivity.mService.send(menuActivity.reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        menuActivity.syncActive=false;
                        menuActivity.syncDataArriving=false;
                        menuActivity.syncError=true;
                    }
                    break;

                case SyncService.MSG_DEVICE_IN_SYNC:
                    Log.d("vortex","***DEVICE IN SYNC***");
                    menuActivity.inSync = true;
                    menuActivity.syncActive=false;
                    menuActivity.syncDataArriving=false;
                    menuActivity.syncError = false;
                    //String team = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
                    //update from the server.
                    GlobalState.getInstance().getServerSyncStatus();

                    break;

                case SyncService.MSG_SYNC_RELEASE_DB:
                    if (GlobalState.getInstance()!=null) {
                        Log.d("vortex", "MSG -->SYNC RELEASE DB LOCK");

                        //Create ui provider.

                        //Check if something needs to be saved. If so, do it on its own thread.
                        if (!menuActivity.syncDbInsert) {
                            menuActivity.syncDbInsert = true;
                            control.flag = false;
                            menuActivity.z_totalToSync = GlobalState.getInstance().getDb().getSyncRowsLeft();
                            menuActivity.setSyncState();
                            menuActivity.z_totalSynced = 0;
                            final UIProvider ui = new UIProvider(menuActivity.getApplicationContext());

                            Log.d("vortex", "total rows to sync is: " + menuActivity.z_totalToSync);
                            if (menuActivity.z_totalToSync==0) {
                                Log.e("vortex","sync table is empty! Aborting sync");
                                menuActivity.syncDbInsert=false;

                                menuActivity.reply = Message.obtain(null, SyncService.MSG_DATA_SAFELY_STORED);
                                try {


                                    menuActivity.mService.send(menuActivity.reply);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                if (menuActivity.t == null) {
                                    menuActivity.t = new MThread() {
                                        final int increment = menuActivity.z_totalToSync;

                                        @Override
                                        public void stopMe() {
                                            control.flag = true;
                                            this.interrupt();
                                        }

                                        @Override
                                        public void run() {
                                            boolean threadDone;
                                            while (!control.flag) {

                                                threadDone = GlobalState.getInstance().getDb().scanSyncEntries(control, increment, ui);
                                                Log.d("vortex", "done scanning syncs...threaddone is " + threadDone + " this is thread " + this.getId());
                                                if (control.error) {
                                                    Log.d("vortex", "Uppsan...exiting");
                                                    menuActivity.syncError = true;
                                                    menuActivity.syncActive = false;
                                                    menuActivity.syncDbInsert = false;
                                                    break;
                                                }
                                                if (!control.flag && !threadDone) {
                                                    menuActivity.z_totalSynced += increment;
                                                } else {
                                                    control.flag = threadDone;


                                                    Log.d("vortex", "End reached for sync. Sending msg safely_stored");
                                                    if (menuActivity.uiLock != null)
                                                        menuActivity.uiLock.cancel();
                                                    control.flag = true;
                                                    menuActivity.uiLock = null;

                                                    menuActivity.reply = Message.obtain(null, SyncService.MSG_DATA_SAFELY_STORED);
                                                    menuActivity.syncError = false;
                                                    menuActivity.syncActive = false;
                                                    menuActivity.syncDataArriving = false;

                                                    try {
                                                        menuActivity.mService.send(menuActivity.reply);
                                                        GlobalState.getInstance().sendEvent(REDRAW_PAGE);

                                                    } catch (RemoteException e) {
                                                        e.printStackTrace();
                                                        menuActivity.syncError = true;
                                                        menuActivity.syncActive = false;
                                                        menuActivity.syncDbInsert = false;
                                                        menuActivity.syncDataArriving = false;
                                                        //Object fuck = null;
                                                        //fuck.equals(fuck);
                                                    }

                                                }
                                                if (!control.error) {
                                                    menuActivity.runOnUiThread(menuActivity::setSyncState);
                                                }


                                            }
                                            Log.d("vortex", "I escaped infite");

                                            menuActivity.syncDbInsert = false;
                                            menuActivity.t = null;
                                            ui.closeProgress();
                                            if (!control.error) {
                                                menuActivity.runOnUiThread(menuActivity::refreshStatusRow);
                                            }
                                        }
                                    };

                                    menuActivity.t.setPriority(Thread.MIN_PRIORITY);
                                    menuActivity.t.start();

                                } else
                                    Log.e("vortex", "EXTRA CALL ON THREADSTART");
                            }

                        } else {
                            Log.e("vortex", "Extra call made to SYNC RELEASE DB LOCK");
                        }
                        menuActivity.syncActive = false;
                        menuActivity.syncDataArriving=false;
                        menuActivity.syncError = false;
                    } else {
                        menuActivity.syncActive=false;
                        menuActivity.syncDataArriving=false;
                        menuActivity.syncError=true;
                    }
                    break;
            }
            //Log.d("Vortex","Refreshing status row. status is se: "+syncError+" sA: "+syncActive);
            menuActivity.refreshStatusRow();
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

    private final MenuItem[] mnu = new MenuItem[NO_OF_MENU_ITEMS];
    private final static int MENU_ITEM_GPS_QUALITY 	= 0;
    private final static int MENU_ITEM_SYNC_TYPE 	= 1;
    private final static int MENU_ITEM_CONTEXT		= 2;
    private final static int MENU_ITEM_LOG_WARNING 	= 3;
    private final static int MENU_ITEM_SETTINGS 	= 4;
    private final static int MENU_ITEM_ABOUT 		= 5;



    private ImageView animView = null;

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

    private void refreshStatusRow() {
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
                setSyncState();

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



    private boolean animationRunning = false;
    private boolean syncDataArriving=false;

    private void setSyncState() {
        //Log.d("vortex","Entering setsyncstate");
        String title=null;
        boolean internetSync = globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet");
        Integer syncState = R.drawable.syncoff;
        int numOfUnsynchedEntries = -1;
        if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth")) {
            syncState = R.drawable.bt;
            title = gs.getDb().getNumberOfUnsyncedEntries()+"";
        }
        else if (internetSync) {
            //Log.d("mama","refresh synk dialog if visible");
            numOfUnsynchedEntries = gs.getDb().getNumberOfUnsyncedEntries();

            Log.d("mammamia","sync on? "+ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY)+"");
            if (syncError) {
                syncState = R.drawable.syncerr;
                title = "";
            }

            else if (!ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY)) {
                syncState = R.drawable.syncoff;
                Log.d("vortex","sync is off in redrawmenu");
            }
            else if (inSync) {
                Log.d("zazz","Unsynched: "+numOfUnsynchedEntries);
                if (numOfUnsynchedEntries>15) {
                    inSync=false;
                    syncState = R.drawable.syncon;
                    //request sync here.
                    reply = Message.obtain(null, SyncService.MSG_START_SYNC);
                    try {
                        mService.send(reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    title = "";
                    //check with server if no more data.
                    GlobalState.SyncGroup sg = gs.getSyncGroup();
                    List team = sg==null?null:sg.getTeam();
                    syncState = (team!=null && team.isEmpty())? R.drawable.insync:R.drawable.iminsync;
                    //if (syncState ==R.drawable.insync) {
                        //TODO: REMOVE - IF User is in sync with server, repair the database.
                        //GlobalState.getInstance().getDb().fixYearNull();
                        /*
                        if (GlobalState.getInstance().getDb().fixdoublets())

                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.repair)
                                    .setMessage(R.string.restartMessage)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok,new Dialog.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Tools.restart(MenuActivity.this);
                                        }

                                    })
                                    .setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .show();


                            ;

                            */
                    //}

                }
            }
            else if (syncActive) {
                if (!animationRunning) {
                    Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
                    rotation.setRepeatCount(Animation.INFINITE);
                    animView.startAnimation(rotation);
                    mnu[MENU_ITEM_SYNC_TYPE].setActionView(animView);
                    animView.setOnClickListener(v -> {
                        animView.clearAnimation();
                        mnu[MENU_ITEM_SYNC_TYPE].setActionView(null);
                        mnu[MENU_ITEM_SYNC_TYPE].setOnMenuItemClickListener(null);
                        animView.setOnClickListener(null);
                        animationRunning = false;
                        syncError = false;
                    });
                    animationRunning=true;
                }
                syncState =R.drawable.syncactive;
                refreshSynkDialog(syncState,numOfUnsynchedEntries);
                return;
            }
            else if (syncDataArriving) {
                title = (z_totalSynced+"/"+z_totalToSync);
                syncState = R.drawable.syncactive;
            }
            else if (syncDbInsert) {
                title = (z_totalSynced+"/"+z_totalToSync);
                syncState = R.drawable.dbase;
            }
            else {
                syncState = R.drawable.syncon;
                Log.d("vortex","icon set to syncon!");
            }
        }
        if ( title == null & numOfUnsynchedEntries > 0)
            title = numOfUnsynchedEntries+"";
        animationRunning = false;
        animView.clearAnimation();
        mnu[MENU_ITEM_SYNC_TYPE].setActionView(null);
        mnu[MENU_ITEM_SYNC_TYPE].setOnMenuItemClickListener(null);
        animView.setOnClickListener(null);
        Log.d("vortex","drawing icon: "+ syncState +"");
        mnu[MENU_ITEM_SYNC_TYPE].setIcon(syncState);
        mnu[MENU_ITEM_SYNC_TYPE].setTitle(title);
        mnu[MENU_ITEM_SYNC_TYPE].setVisible(true);

        refreshSynkDialog(syncState,numOfUnsynchedEntries);
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
                            .setNeutralButton("Ok", (dialog, which) -> {

                            })
                            .show();
                } else
                    refreshStatusRow();
                break;
            case MENU_ITEM_SYNC_TYPE:
                displaySyncDialog();
                //toggleSyncOnOff();
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
                            .setNeutralButton("Ok", (dialog, which) -> {

                            })
                            .show();
                }
                break;

            case MENU_ITEM_LOG_WARNING:
                mnu[MENU_ITEM_LOG_WARNING].setIcon(null);
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.log_dialog_popup);
                dialog.setTitle("Session Log");
                final TextView tv= dialog.findViewById(R.id.logger);
                final ScrollView sv= dialog.findViewById(R.id.logScroll);
                Typeface type=Typeface.createFromAsset(getAssets(),
                        "clacon.ttf");
                tv.setTypeface(type);
                final LoggerI log = Start.singleton.getLogger();
                log.setOutputView(tv);
                //trigger redraw.
                log.draw();
                //Button close=(Button)dialog.findViewById(R.id.log_close);
                dialog.show();
				/*close.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
						log.setOutputView(null);
					}
				});
				*/
                Button clear = dialog.findViewById(R.id.log_clear);
                clear.setOnClickListener(v -> {
                    log.clear();
                    //if(gs!=null && gs.getVariableCache()!=null)
                    //   gs.getVariableCache().printCache();
                    //List<PeriodicSync> l = ContentResolver.getPeriodicSyncs(mAccount, Start.AUTHORITY);
                    //for (PeriodicSync p:l) {
                    //    Log.d("marko","Period: "+p.period+" isSyncable: "+ContentResolver.getIsSyncable(mAccount, Start.AUTHORITY)+" isPending? "+ContentResolver.isSyncPending(mAccount,Start.AUTHORITY));
                    //}
                });
                Button scrollD = dialog.findViewById(R.id.scrollDown);
                scrollD.setOnClickListener(v -> sv.post(() -> sv.fullScroll(ScrollView.FOCUS_DOWN)));
                Button print = dialog.findViewById(R.id.printdb);
                Button printLog = dialog.findViewById(R.id.printlog);

                print.setOnClickListener(v -> {
                    if (gs!=null)
                        BackupManager.getInstance(gs).backupDatabase("dump");
                });

                printLog.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        printLog(log.getLogText());
                    }

                    private void printLog(CharSequence logText) {
                        String crashme = null;
                        crashme.charAt(0);
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

    private void displaySyncDialog() {

        if (mPopupWindow.isShowing()) {
            Log.d("pop", "already showing...return");
            return;
        }

        mPopupWindow.showAtLocation(findViewById(R.id.content_frame), Gravity.CENTER,0,0);
        setSyncState();
    }

    @SuppressLint("SetTextI18n")
    private void refreshSynkDialog(Integer syncState, int numberOfUnsynchedItems) {
        if (!mPopupWindow.isShowing()) {
            //Log.d("pop", "pop not showing...exit");
            return;
        }
        boolean sync_on=syncOn();
        sync_switch.setChecked(sync_on);
        sync_button.setEnabled(sync_on);
        View customView = mPopupWindow.getContentView();

        //The current synk status
        TextView synk_status_display = customView.findViewById(R.id.synk_status_display);
        String synkStat = getString(R.string.synk_off);
        if (syncState != null) {
            switch (syncState) {
                case R.drawable.syncon:
                    synkStat = getString(R.string.synk_idle);
                    break;
                case R.drawable.syncactive:
                    synkStat = getString(R.string.synk_send_receive) + " " + z_totalSynced + "/" + z_totalToSync;
                    break;
                case R.drawable.syncerr:
                    synkStat = getString(R.string.synk_error);
                    break;
                case R.drawable.syncoff:
                    synkStat = getString(R.string.synk_off);
                    break;
                case R.drawable.insync:
                    synkStat = getString(R.string.in_sync);
                    break;
                case R.drawable.iminsync:
                    synkStat = getString(R.string.imin_sync);
                    break;
                case R.drawable.dbase:
                    synkStat = getString(R.string.synk_inserting) + " " + z_totalSynced + "/" + z_totalToSync;
                    break;

            }
        }
        synk_status_display.setText(synkStat);


        //The current time since last sync was completed.
        final String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        Long timestamp = GlobalState.getInstance().getPreferences().getL(PersistenceHelper.TIME_OF_LAST_SYNC_INET + team);
        TextView time_last_sync = customView.findViewById(R.id.sync_time_since_last);
        String time = "-";
        if (timestamp!=-1) {
            Date date = new Date(timestamp);
            time = print_time_diff(date);
        }
        time_last_sync.setText(time);
        //Remaining objects to sync
        TextView unsync = customView.findViewById(R.id.sync_objects_remaining);
        unsync.setText(Integer.toString(numberOfUnsynchedItems ));

        //List of people

        GlobalState.SyncGroup syncGroup = gs.getSyncGroup();

        LinearLayout ll = customView.findViewById(R.id.list_container);
        TextView sync_refresh_date = customView.findViewById(R.id.sync_refresh_date);

        ll.removeAllViews();

        if (syncGroup != null && syncGroup.getTeam() != null) {
            sync_refresh_date.setText(syncGroup.getLastUpdate().toString());

            List<GlobalState.TeamMember> teamMemberList = syncGroup.getTeam();

            if (teamMemberList.isEmpty()) {
                ll.addView(rowBuffer.defaultRow(getString(R.string.in_sync)));


            } else {
                for (GlobalState.TeamMember tm:teamMemberList) {
                    ll.addView(rowBuffer.getRow(tm.user,tm.unsynched+"",print_time_diff(tm.getDate())));
                }
            }


        }
    }

    private String print_time_diff(Date date) {
        String time;

        Date now = new Date(System.currentTimeMillis());

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int d_month = cal.get(Calendar.MONTH);
        int d_day = cal.get(Calendar.DAY_OF_MONTH);
        int d_hour = cal.get(Calendar.HOUR_OF_DAY);
        int d_min = cal.get(Calendar.MINUTE);
        cal.setTime(now);
        int n_month = cal.get(Calendar.MONTH);
        int n_day = cal.get(Calendar.DAY_OF_MONTH);
        int n_hour = cal.get(Calendar.HOUR_OF_DAY);
        int n_min = cal.get(Calendar.MINUTE);
        try{
            if (n_month != d_month) {
                time = (n_month - d_month) + " " + getString(R.string.sync_time_months);
            }
            else if (n_day != d_day) {
                time = (n_day - d_day) + " " + getString(R.string.sync_time_days);
            }
            else if (n_hour != d_hour)
                time = (n_hour - d_hour) + " " + getString(R.string.sync_time_hours);
            else if (n_min != d_min)
                time = (n_min - d_min) + " " + getString(R.string.sync_time_minutes);
            else
                time = getString(R.string.sync_time_just_now);
        } catch (NumberFormatException e) {
            time = "-";
        }
        return time;
    }

    private final RowBuffer rowBuffer = new RowBuffer();
    private  class RowBuffer {
        final Map<String,View> buffer = new HashMap<>();

        View getRow(String name, String obj, String time) {
            View entry = getRow(name);
            setText(entry,name,obj,time);
            return entry;
        }

        void clear() {
            for (View entry:buffer.values()) {
                if(entry.getParent()!=null)
                    ((ViewGroup)entry.getParent()).removeView(entry);
            }
            buffer.clear();
        }

        private View getRow(String name) {
            View entry = buffer.get(name);
            if (entry == null) {
                entry = getLayoutInflater().inflate(R.layout.sync_popup_list_row,null);
                buffer.put(name,entry);
            } else {
                if(entry.getParent()!=null)
                    ((ViewGroup)entry.getParent()).removeView(entry);
            }
            return entry;
        }

        View defaultRow(String text) {
            View row = getRow("default");
            setText(row,text,"","");
            return row;
        }
        private void setText(View row, String name,String obj, String time) {
            ((TextView)row.findViewById(R.id.sync_row_user)).setText(name);
            ((TextView)row.findViewById(R.id.sync_row_unsynced)).setText(obj);
            ((TextView)row.findViewById(R.id.sync_row_unsync_date)).setText(time);

        }
    }

    private void toggleSyncOnOff(boolean on) {
        String syncMethod = globalPh.get(PersistenceHelper.SYNC_METHOD);
        if (syncMethod.equals("Bluetooth")) {
            DataSyncSessionManager.start(MenuActivity.this, new UIProvider(this) {
                @Override
                public void onClose() {
                    me.onCloseSync();

                }
            });
        } else {
            if (syncMethod.equals("Internet")) {
                Log.d("vortex","in togglesync internet");
                if (on) {
                    Log.d("vortex", "Starting sync process");
                    //Check there is name and team.
                    String user = globalPh.get(PersistenceHelper.USER_ID_KEY);
                    String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
                    if (user==null || user.length()==0 || team==null || team.length()==0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Sync cannot start")
                                .setMessage("Missing team ["+team+"] or user name ["+user+"]. Please add under the Settings menu")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                    syncError=false;
                                    syncActive=false;
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

                    if (isSynkServiceRunning()) {
                        ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);
                        Log.d("vortex","sync service is running...sending msg");
                        handler = new Handler();

                        syncP = new Runnable() {
                            @Override
                            public void run() {
                                Log.d("zazz","called");
                                if (MenuActivity.this.syncOn()) {
                                    if (gs!=null)
                                        gs.getServerSyncStatus();
                                    setSyncState();
                                    Log.d("zazz","reposting");
                                    handler.postDelayed(this, 60000);

                                } else {
                                    Log.d("zazz","blocked: gs: "+(gs!=null)+" on: "+MenuActivity.this.syncOn());

                                }
                            }
                        };
                        try {
                            Log.d("zazz","Sending start sync.");
                            handler.post(syncP);
                            reply = Message.obtain(null, SyncService.MSG_START_SYNC);
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
        if(syncP!=null && handler !=null)
            handler.removeCallbacks(syncP);
        ContentResolver.cancelSync(mAccount, Start.AUTHORITY);
        ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);

        if (syncError) {
            Log.d("vortex","Resetting sync error");
            syncError=false;
        }
        if (syncDbInsert){
            t.stopMe();
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
        //start sync if not started.

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



    private void onCloseSync() {
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

    public static class UIProvider {

        static final int LOCK =1, UNLOCK=2, ALERT = 3, UPDATE_SUB = 4, CONFIRM = 5, UPDATE = 6, PROGRESS = 7,SHOW_PROGRESS=8,CLOSE_PROGRESS=9;
        private String row1="",row2="";
        private AlertDialog uiBlockerWindow=null;
        private ProgressDialog progress;
        private final Context mContext;

        UIProvider(Context context) {
            mContext = context;
        }

        final Handler mHandler= new Handler(Looper.getMainLooper()) {
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
                        .setNegativeButton(R.string.close, (dialog, which) -> onClose())
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
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            //onClose must be overridden.
                            onClose();
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> cb.confirm())
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
        void closeProgress() {
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


        void onClose() {

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
