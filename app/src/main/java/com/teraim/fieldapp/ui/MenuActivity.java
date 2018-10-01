package com.teraim.fieldapp.ui;

import android.accounts.Account;
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

    public final static String REDRAW = "com.teraim.fieldapp.menu_redraw";
    public static final String INITDONE = "com.teraim.fieldapp.init_done";
    public static final String INITSTARTS = "com.teraim.fieldapp.init_starts";
    public static final String INITFAILED = "com.teraim.fieldapp.init_done_but_failed";
    public static final String SYNC_REQUIRED = "com.teraim.fieldapp.sync_required";

    public static final String MESSAGE_ACTION = "Massage_Massage";

    private BroadcastReceiver brr;
    private GlobalState gs;
    protected PersistenceHelper globalPh;
    protected LoggerI debugLogger;
    private boolean initDone =false, initFailed =false;
    private MenuActivity me;
    private Account mAccount;
    private Integer syncState = R.drawable.syncoff;
    protected PopupWindow mPopupWindow;
    private Switch sync_switch;
    private Button sync_button;

    //Tracker callback.
    private GPS_State latestSignal=null;

    static class MThread extends Thread {
        void stopMe() {}
    }
    private MThread t;

    /** Flag indicating whether we have called bind on the sync service. */
    private boolean mBound;

    private Messenger mService = null;

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private AlertDialog uiLock=null;
    private Message reply=null;
    private volatile int z_totalSynced = 0;
    private volatile int z_totalToSync = 0;

    private final static int NO_OF_MENU_ITEMS = 6;
    private final MenuItem[] mnu = new MenuItem[NO_OF_MENU_ITEMS];
    private final static int MENU_ITEM_GPS_QUALITY 	= 0;
    private final static int MENU_ITEM_SYNC_TYPE 	= 1;
    private final static int MENU_ITEM_CONTEXT		= 2;
    private final static int MENU_ITEM_LOG_WARNING 	= 3;
    private final static int MENU_ITEM_SETTINGS 	= 4;
    private final static int MENU_ITEM_ABOUT 		= 5;

    private ImageView animView = null;

    private boolean animationRunning = false;

    private enum GPSQuality {
        red,
        yellow,
        green
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        me = this;

        globalPh = new PersistenceHelper(this.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

        brr = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                Log.d("nils", "Broadcast: "+intent.getAction());

                if (intent.getAction().equals(INITDONE)) {
                    initDone =true;

                    //listen to Tracker
                    if (GlobalState.getInstance()!=null) {
                        Log.d("glapp","menuactivity now listnes to tracker");
                        GlobalState.getInstance().getTracker().registerListener(MenuActivity.this);
                    }
                    //Should sync be on or off?
                    boolean syncIsActive = ContentResolver.getSyncAutomatically(mAccount, Start.AUTHORITY);
                    toggleSyncOnOff(syncIsActive);

                }
                else if (intent.getAction().equals(INITSTARTS)) {
                    initDone =false;
                    initFailed =false;
                }
                else if (intent.getAction().equals(INITFAILED)) {
                    Log.d("initf","got initFailed");
                    initFailed =true;
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


        IntentFilter filter = new IntentFilter();
        filter.addAction(INITDONE);
        filter.addAction(INITSTARTS);
        filter.addAction(REDRAW);
        filter.addAction(INITFAILED);

        LocalBroadcastManager.getInstance(this).registerReceiver(brr, filter);

        //Register to sync framework
        initializeSynchronisation();

        //Gps latest signal
        latestSignal = null;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        // Inflate the sync popup
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
                    toggleSyncOnOff(b);
            }
        });

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

        syncState = R.drawable.syncoff;

        super.onDestroy();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if("Internet".equals(this.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.SYNC_METHOD,"")))
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
            syncState = R.drawable.syncerr;
            me.refreshStatusRow();
        }
    };

    private boolean isSynkServiceRunning() {
        return mBound;
    }


    private static class IncomingHandler extends Handler {

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
                    menuActivity.syncState=R.drawable.syncon;
                    Log.d("vortex","MSG -->SYNC STARTED");
                    break;

                case SyncService.MSG_SERVER_READ_MY_DATA:
                    long timestampFromMe = ((Bundle)msg.obj).getLong("maxstamp");
                    GlobalState.getInstance().getDb().saveTimeStampFromMeToTeam(GlobalState.getInstance().getMyTeam(),timestampFromMe);
                    Log.d("vortex", "ME-->TEAM UPDATED TO "+timestampFromMe);
                    break;

                case SyncService.MSG_SYNC_DATA_ARRIVING:
                    menuActivity.syncState=R.drawable.syncactive;
                    Bundle b = (Bundle)msg.obj;
                    menuActivity.z_totalSynced=b.getInt("number_of_rows_so_far");
                    menuActivity.z_totalToSync=b.getInt("number_of_rows_total");
                    Log.d("vortex","MSG_SYNC_DATA_ARRIVING...total rows to sync is "+menuActivity.z_totalToSync);
                    break;

                case SyncService.MSG_SYNC_ERROR_STATE:

                    menuActivity.syncState=R.drawable.syncerr;
                    String toastMsg = "";
                    Log.d("vortex","MSG -->SYNC ERROR STATE");
                    switch(msg.arg1) {
                        case SyncService.ERR_SETTINGS:
                            Log.d("vortex","ERR WRONG SETTINGS...");
                            if (menuActivity.uiLock == null) {
                                menuActivity.uiLock = new AlertDialog.Builder(menuActivity)
                                        .setTitle("Synchronize")
                                        .setMessage("Sync cannot start. Please check that you have entered team and username under settings.")
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
                            break;
                        case SyncService.ERR_SERVER_CONN_TIMEOUT:
                            toastMsg = "Me-->Sync Server. Connection timeout";
                            Log.d("vortex","Synkserver Timeout.");
                            break;
                        default:
                            Log.d("vortex","Any other error!");
                            toastMsg = "Me-->Sync Server. Connection failure.";
                            break;
                    }
                    if (toastMsg.length()>0) {
                        Toast.makeText(menuActivity, toastMsg, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case SyncService.MSG_SYNC_REQUEST_DB_LOCK:
                    Log.d("vortex","MSG -->SYNC REQUEST DB LOCK");
                    menuActivity.reply = Message.obtain(null, SyncService.MSG_DATABASE_LOCK_GRANTED);
                    try {
                        menuActivity.mService.send(menuActivity.reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        menuActivity.syncState = R.drawable.syncerr;
                    }
                    break;

                case SyncService.MSG_NO_NEW_DATA_FROM_TEAM_TO_ME:
                case SyncService.MSG_ALL_SYNCED:
                    Log.d("vortex","***DEVICE IN SYNC WITH SERVER***");
                    Bundle envelope = ((Bundle)msg.obj);
                    long timestamp_from_team_to_me = envelope.getLong(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME);
                    GlobalState.getInstance().getDb().saveTimeStampFromTeamToMe(GlobalState.getInstance().getMyTeam(),timestamp_from_team_to_me);
                    if (msg.what == SyncService.MSG_ALL_SYNCED) {
                        menuActivity.syncState=R.drawable.insync;
                    } else {
                        menuActivity.syncState=R.drawable.iminsync;
                    }
                    break;




                case SyncService.MSG_SYNC_DATA_READY_FOR_INSERT:
                    if (GlobalState.getInstance()!=null) {
                        Log.d("vortex", "MSG -->SYNC_DATA_READY_FOR_INSERT");
                        //Block
                        if (menuActivity.syncState!=R.drawable.dbase) {
                            menuActivity.syncState=R.drawable.dbase;
                            control.flag = false;
                            menuActivity.z_totalToSync = GlobalState.getInstance().getDb().getSyncRowsLeft();
                            menuActivity.z_totalSynced = 0;
                            final UIProvider ui = new UIProvider(menuActivity);
                            Log.d("vortex", "total rows to sync is: " + menuActivity.z_totalToSync);
                            if (menuActivity.z_totalToSync==0) {
                                Log.e("vortex","sync table is empty! Aborting sync");
                                menuActivity.syncState=R.drawable.syncon;
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
                                                    menuActivity.syncState = R.drawable.syncerr;
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
                                                    menuActivity.syncState = R.drawable.syncon;
                                                    try {
                                                        menuActivity.mService.send(menuActivity.reply);
                                                        //Trigger a redraw to update UI with new data
                                                        GlobalState.getInstance().sendEvent(REDRAW_PAGE);
                                                    } catch (RemoteException e) {
                                                        e.printStackTrace();
                                                        menuActivity.syncState = R.drawable.syncerr;
                                                    }

                                                }
                                                if (!control.error) {
                                                    menuActivity.runOnUiThread(menuActivity::refreshSyncDisplay);
                                                }
                                            }
                                            Log.d("vortex", "I escaped the infinite");
                                            menuActivity.t = null;
                                            menuActivity.syncState=R.drawable.syncon;
                                            ui.closeProgress();

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

                    } else {
                        menuActivity.syncState = R.drawable.syncerr;
                    }
                break;
            }
            menuActivity.refreshSyncDisplay();

        }

    }






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


    private void createMenu(Menu menu) {
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



    private void refreshStatusRow() {
        //If init failed, show only log and settings
        if (initFailed) {
            if (mnu[MENU_ITEM_LOG_WARNING]!=null) {
                mnu[MENU_ITEM_LOG_WARNING].setVisible(true);
                mnu[MENU_ITEM_SETTINGS].setVisible(true);
                mnu[MENU_ITEM_SETTINGS].setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
            //Init done succesfully? Show all items.
        } else 	if (GlobalState.getInstance()!=null && initDone) {

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

            if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE") || GlobalState.getInstance().isSolo())
                mnu[MENU_ITEM_SYNC_TYPE].setVisible(false);
            else
                refreshSyncDisplay();

            mnu[MENU_ITEM_CONTEXT].setVisible(true);
            mnu[MENU_ITEM_LOG_WARNING].setVisible(!globalPh.get(PersistenceHelper.LOG_LEVEL).equals("off"));
            if (debugLogger!=null && debugLogger.hasRed()) {
                mnu[MENU_ITEM_LOG_WARNING].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                mnu[MENU_ITEM_LOG_WARNING].setIcon(R.drawable.warning);
            }
            mnu[MENU_ITEM_SETTINGS].setVisible(true);
            mnu[MENU_ITEM_ABOUT].setVisible(true);


        }
    }

    private void refreshSyncDisplay() {

        String synkStatusTitle="";
        int numOfUnsynchedEntries = -1;
        boolean internetSync = globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet");

        if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Bluetooth")) {
            syncState = R.drawable.bt;
            synkStatusTitle = gs.getDb().getNumberOfUnsyncedEntries()+"";
        }
        else if (internetSync) {
            numOfUnsynchedEntries = gs.getDb().getNumberOfUnsyncedEntries();

            if ( synkStatusTitle == null & numOfUnsynchedEntries > 0)
                synkStatusTitle = numOfUnsynchedEntries+"";
            switch (syncState) {
                case R.drawable.syncactive:
                case R.drawable.dbase:
                    synkStatusTitle = z_totalSynced + "/" + z_totalToSync;
                    /*
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
                        });
                        animationRunning=true;
                    }
                    */
                    break;




            }

        }
        //animationRunning = false;
        //animView.clearAnimation();
        //animView.setOnClickListener(null);
        mnu[MENU_ITEM_SYNC_TYPE].setActionView(null);
        mnu[MENU_ITEM_SYNC_TYPE].setOnMenuItemClickListener(null);
        mnu[MENU_ITEM_SYNC_TYPE].setIcon(syncState);
        mnu[MENU_ITEM_SYNC_TYPE].setTitle(synkStatusTitle);
        mnu[MENU_ITEM_SYNC_TYPE].setVisible(true);

        //End here if the sync dialog is closed.
        if (!mPopupWindow.isShowing())
            return;

        boolean sync_on=syncOn();
        sync_switch.setChecked(sync_on);
        sync_button.setEnabled(sync_on);

        View customView = mPopupWindow.getContentView();


        //Last time I sent my data.
        final String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        Long timestamp = GlobalState.getInstance().getDb().getTimeStampFromMeToTeam(team);
        TextView time_last_sync = customView.findViewById(R.id.sync_time_since_last);
        String time = "-";
        if (timestamp!=-1) {
            Date date = new Date(timestamp);
            time = print_time_diff(date);
        }
        time_last_sync.setText(time);
        //Remaining objects to sync
        TextView unsync = customView.findViewById(R.id.sync_objects_remaining);
        unsync.setText(Integer.toString(numOfUnsynchedEntries));

        //List of people
        GlobalState.SyncGroup syncGroup = gs.getSyncGroup();

        LinearLayout ll = customView.findViewById(R.id.list_container);
        TextView sync_refresh_date = customView.findViewById(R.id.sync_refresh_date);

        ll.removeAllViews();

        if (syncGroup != null && syncGroup.getTeam() != null && syncGroup.getLastUpdate() !=null) {
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
        //The current synk status
        String synkStatusText = getString(R.string.synk_off);
        switch (syncState) {
            case R.drawable.syncon:
                synkStatusText = getString(R.string.synk_idle);
                break;
            case R.drawable.syncactive:
                synkStatusText = getString(R.string.synk_send_receive) + " " + z_totalSynced + "/" + z_totalToSync;
                break;
            case R.drawable.syncerr:
                synkStatusText = getString(R.string.synk_error);
                break;
            case R.drawable.syncoff:
                synkStatusText = getString(R.string.synk_off);
                break;
            case R.drawable.insync:
                //Refresh
                gs.getServerSyncStatus();
                synkStatusText = getString(R.string.in_sync);
                break;
            case R.drawable.iminsync:
                //Refresh
                gs.getServerSyncStatus();
                synkStatusText = getString(R.string.imin_sync);
                break;
            case R.drawable.dbase:
                synkStatusText = getString(R.string.synk_inserting) + " " + z_totalSynced + "/" + z_totalToSync;
                break;
        }
        TextView synk_status_display = customView.findViewById(R.id.synk_status_display);
        synk_status_display.setText(synkStatusText);


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
                            Toast.makeText(MenuActivity.this,
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
        refreshSyncDisplay();
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

                    //Check there is name and team.
                    String user = globalPh.get(PersistenceHelper.USER_ID_KEY);
                    String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
                    String app = globalPh.get(PersistenceHelper.BUNDLE_NAME);
                    if (user==null || user.length()==0 || team==null || team.length()==0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Sync cannot start")
                                .setMessage("Missing team ["+team+"] or user name ["+user+"]. Please add under the Settings menu")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                })
                                .show();
                    } else {
                        if (isSynkServiceRunning()) {
                            Message msg = Message.obtain(null, SyncService.MSG_REGISTER_CLIENT);
                            msg.replyTo = mMessenger;
                            //inform the sync engine when I got data last time.
                            Bundle b = new Bundle();
                            b.putLong(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME, gs.getDb().getTimeStampFromTeamToMe(team));
                            b.putString("user", user);
                            b.putString("app", app);
                            b.putString("team", team);

                            msg.obj = b;

                            try {
                                if (mService != null)
                                    mService.send(msg);

                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, true);
                            Log.d("vortex", "sync service is running...sending msg");


                        } else {
                            Log.d("vortex","Synk server is not running");
                            GlobalState.getInstance().getLogger().addCriticalText("Sync service is not up...");
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


        if (syncState==R.drawable.dbase){
            t.stopMe();
        }
        syncState=R.drawable.syncoff;
        reply = Message.obtain(null, SyncService.MSG_USER_STOPPED_SYNC);
        try {
            mService.send(reply);
        } catch (RemoteException e) {
            e.printStackTrace();
            syncActive=false;
            syncError=true;
        }
    }

    private void initializeSynchronisation() {
        mAccount = GlobalState.getmAccount(this);
        ContentResolver.addPeriodicSync(
                mAccount,
                Start.AUTHORITY,
                Bundle.EMPTY,
                Start.SYNC_INTERVAL);
        Log.d("vortex","added periodic sync");

    }



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


        private boolean syncOn() {
            return ContentResolver.getSyncAutomatically(mAccount,Start.AUTHORITY);
        }

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

        private GPSQuality calculateGPSKQI() {
            if (latestSignal.accuracy<=6)
                return GPSQuality.green;
            else if (latestSignal.accuracy<=10)
                return GPSQuality.yellow;
            else
                return GPSQuality.red;
        }

}
