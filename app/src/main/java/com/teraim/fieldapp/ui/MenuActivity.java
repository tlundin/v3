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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.JsonToken;
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
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.DataSyncSessionManager;
import com.teraim.fieldapp.synchronization.framework.SyncAdapter;
import com.teraim.fieldapp.synchronization.framework.SyncConsumerThread;
import com.teraim.fieldapp.synchronization.framework.SyncService;
import com.teraim.fieldapp.utils.BackupManager;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.teraim.fieldapp.synchronization.framework.SyncService.MSG_SYNC_DATA_READY_FOR_INSERT;
import static com.teraim.fieldapp.synchronization.framework.SyncService.MSG_SYNC_ERROR_STATE;
import static com.teraim.fieldapp.synchronization.framework.SyncService.MSG_SYNC_RUN_ENDED;
import static com.teraim.fieldapp.synchronization.framework.SyncService.MSG_SYNC_RUN_STARTED;

/**
 * Parent class for Activities having a menu row.
 * @author Terje
 *
 */
public class MenuActivity extends AppCompatActivity implements TrackerListener {

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
    private boolean initDone = false, initFailed = false;
    private MenuActivity me;
    private Account mAccount;
    private Integer syncState = R.drawable.syncoff;
    protected PopupWindow mPopupWindow;
    private Switch sync_switch;


    //Tracker callback.
    private GPS_State latestSignal = null;
    /**
     * Flag indicating whether we have called bind on the sync service.
     */
    private boolean mBound;
    private Messenger mService = null;
    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));
    private final static int NO_OF_MENU_ITEMS = 6;
    private final MenuItem[] mnu = new MenuItem[NO_OF_MENU_ITEMS];
    private final static int MENU_ITEM_GPS_QUALITY = 0;
    private final static int MENU_ITEM_SYNC_TYPE = 1;
    private final static int MENU_ITEM_CONTEXT = 2;
    private final static int MENU_ITEM_LOG_WARNING = 3;
    private final static int MENU_ITEM_SETTINGS = 4;
    private final static int MENU_ITEM_ABOUT = 5;
    private Button refresh_button;
    private CompoundButton.OnCheckedChangeListener mSyncSwitchListener;

    private enum GPSQuality {
        red,
        yellow,
        green
    }

    private long lastRedraw = 0;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        me = this;

        globalPh = new PersistenceHelper(this.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

        brr = new BroadcastReceiver() {
            private static final long MIN_REDRAW_DELAY = 5000;

            @Override
            public void onReceive(Context ctx, Intent intent) {
                Log.d("nils", "Broadcast: " + intent.getAction());

                switch (intent.getAction()) {
                    case INITDONE:
                        initDone = true;

                        //listen to Tracker
                        if (GlobalState.getInstance() != null) {

                            GlobalState.getInstance().getTracker().registerListener(MenuActivity.this);
                            gs = GlobalState.getInstance();
                            //check current state of synk server.
                            //This determines the sync status.
                            toggleSyncOnOff(syncOn());

                            if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet"))
                                getTeamSyncStatusFromServer();


                        }

                        break;
                    case INITSTARTS:
                        initDone = false;
                        initFailed = false;
                        me.refreshStatusRow();
                        break;
                    case INITFAILED:
                        Log.d("initf", "got initFailed");
                        initFailed = true;
                        me.refreshStatusRow();
                        break;
                    case SYNC_REQUIRED:
                        new AlertDialog.Builder(MenuActivity.this)
                                .setTitle("Synchronize")
                                .setMessage("The action you just performed mandates a synchronisation. Please synchronise with your partner before continuing.")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                })
                                .show();
                        break;
                    case REDRAW:
                        //check that there is some time between calls. 5s?
                        long currentTime = System.currentTimeMillis();
                        long diff = currentTime - lastRedraw;
                        if (diff < MIN_REDRAW_DELAY) {
                            //delay or discard call.
                            Log.d("vortex", "Calling redraw");
                            if (handler == null) {
                                handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("vortex", "Calling redraw..delayed diff: " + diff);
                                        handler = null;
                                        me.refreshStatusRow();
                                    }
                                }, MIN_REDRAW_DELAY - diff);
                            }
                        } else
                            me.refreshStatusRow();

                        lastRedraw = System.currentTimeMillis();
                        if (!Connectivity.isConnected(MenuActivity.this))
                            toggleSyncOnOff(false);
                        Log.d("kakka", "connected: " + Connectivity.isConnected(MenuActivity.this));
                        syncState = syncOn() ? R.drawable.syncon : R.drawable.syncoff;
                        break;
                }
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
        View syncpop = Objects.requireNonNull(inflater).inflate(R.layout.sync_popup_inner, null);

        // Initialize a new instance of popup window
        mPopupWindow = new PopupWindow(
                syncpop,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        //mPopupWindow.setElevation(5.0f);

        Button closeButton = syncpop.findViewById(R.id.close_button);

        closeButton.setOnClickListener(view -> {
            // Dismiss the popup window
            mPopupWindow.dismiss();
        });


        refresh_button = syncpop.findViewById(R.id.refresh_button);

        refresh_button.setOnClickListener(view -> {
            rowBuffer.clear();
            SyncAdapter.forceSyncToHappen();
        });

        sync_switch = syncpop.findViewById(R.id.sync_switch);

        mSyncSwitchListener = (compoundButton, b) -> toggleSyncOnOff(b);


    }

    @Override
    public void onDestroy() {
        Log.d("NILS", "In the onDestroy() event");
        latestSignal = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(brr);

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

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
        if ("Internet".equals(this.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE).getString(PersistenceHelper.SYNC_METHOD, ""))) {
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


    public static class IncomingHandler extends Handler {

        final MenuActivity menuActivity;
        private SyncConsumerThread syncConsumerThread;

        IncomingHandler(MenuActivity menuActivity) {

            this.menuActivity = menuActivity;
            syncConsumerThread = null;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_SYNC_RUN_STARTED:
                    Log.d("vortex", "MSG -->SYNC STARTED");
                    menuActivity.syncState = R.drawable.syncactive;
                    break;
                case MSG_SYNC_RUN_ENDED:
                    Log.d("vortex", "MSG -->SYNC ENDED");
                    GlobalState gs = GlobalState.getInstance();
                    if (gs!=null) {
                        gs.getDb().saveTimeStampOfLatestSuccesfulSync(gs.getMyTeam());
                        menuActivity.syncState = R.drawable.syncon;
                        menuActivity.getTeamSyncStatusFromServer();
                    }
                    break;
                case MSG_SYNC_ERROR_STATE:
                    menuActivity.syncState = R.drawable.syncerr;
                    String toastMsg;
                    Log.d("sync", "MSG -->SYNC ERROR STATE");
                    switch (msg.arg1) {
                        case SyncService.ERR_RECEIVE_FAILED:
                            toastMsg = "Sync Server --> Me. No route";
                            break;
                        case SyncService.ERR_SEND_FAILED:
                            toastMsg = "Me --> Sync Server. No route";
                            break;
                        case SyncService.ERR_TRANSMISSION_FAILURE:
                            toastMsg = "Transmission failure";
                        default:
                            toastMsg = "Sync Error";
                            break;
                    }

                    Toast.makeText(menuActivity, toastMsg, Toast.LENGTH_SHORT).show();

                    break;

                case MSG_SYNC_DATA_READY_FOR_INSERT:
                    Log.d("sync", "MSG -->SYNC_DATA_READY_FOR_INSERT");
                    if (syncConsumerThread == null || !syncConsumerThread.isAlive()) {
                        syncConsumerThread = new SyncConsumerThread(this);
                        syncConsumerThread.setPriority(Thread.MIN_PRIORITY);
                        syncConsumerThread.start();
                    } else {

                        Log.d("sync","Busy inserting...delaying");

                    }
                    break;
                //case MSG_SYNC_DATA_CONSUMED:
                //   Log.d("sync", "MSG -->SYNC_DATA_CONSUMED");
                //   syncConsumerThread = null;
                //   break;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        return menuChoice(item);
    }


    private void createMenu(Menu menu) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //animView = (ImageView) inflater.inflate(R.layout.refresh_load_icon, null);

        for (int c = 0; c < mnu.length; c++)
            mnu[c] = menu.add(0, c, c, "");

        mnu[MENU_ITEM_GPS_QUALITY].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        mnu[MENU_ITEM_SYNC_TYPE].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
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
            if (mnu[MENU_ITEM_LOG_WARNING] != null) {
                mnu[MENU_ITEM_LOG_WARNING].setVisible(true);
                mnu[MENU_ITEM_SETTINGS].setVisible(true);
                mnu[MENU_ITEM_SETTINGS].setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
            //Init done succesfully? Show all items.
        } else if (GlobalState.getInstance() != null && initDone) {

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
                    mnu[MENU_ITEM_GPS_QUALITY].setTitle(Math.round(latestSignal.accuracy) + "");
                }
            }

            if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("NONE") || GlobalState.getInstance().isSolo())
                mnu[MENU_ITEM_SYNC_TYPE].setVisible(false);
            else
                refreshSyncDisplay();

            mnu[MENU_ITEM_CONTEXT].setVisible(true);
            mnu[MENU_ITEM_LOG_WARNING].setVisible(!globalPh.get(PersistenceHelper.LOG_LEVEL).equals("off"));
            if (debugLogger != null && debugLogger.hasRed()) {
                mnu[MENU_ITEM_LOG_WARNING].setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                mnu[MENU_ITEM_LOG_WARNING].setIcon(R.drawable.warning);
            }
            mnu[MENU_ITEM_SETTINGS].setVisible(true);
            mnu[MENU_ITEM_ABOUT].setVisible(true);


        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void refreshSyncDisplay() {

        int numOfUnsynchedEntries = gs.getDb().getNumberOfUnsyncedEntries();
        long numOfInsertSyncEntries = gs.getDb().getSyncRowsLeft();
        //List of people in team with data on server
        SyncGroup syncGroup = getSyncGroup();

        boolean fullySyncedWithTeam = (syncGroup != null && syncGroup.getTeam() != null && syncGroup.getTeam().isEmpty());

        boolean fullySynced = (numOfUnsynchedEntries ==0)&&(numOfInsertSyncEntries == 0) && fullySyncedWithTeam;

        boolean sync_on = syncOn();

        String synkStatusTitle;

        if (fullySynced) {
            syncState = R.drawable.insync;
            synkStatusTitle="";
        } else
            synkStatusTitle = numOfUnsynchedEntries+((numOfInsertSyncEntries>0)?"/"+numOfInsertSyncEntries:"");
        if (!sync_on)
            syncState = R.drawable.syncoff;

        mnu[MENU_ITEM_SYNC_TYPE].setActionView(null);
        mnu[MENU_ITEM_SYNC_TYPE].setOnMenuItemClickListener(null);
        mnu[MENU_ITEM_SYNC_TYPE].setIcon(syncState);
        mnu[MENU_ITEM_SYNC_TYPE].setTitle(synkStatusTitle);
        mnu[MENU_ITEM_SYNC_TYPE].setVisible(true);

        //End here if the sync dialog is closed.
        if (!mPopupWindow.isShowing())
            return;


        //only show the current state, don't trigger event.
        sync_switch.setOnCheckedChangeListener (null);
        sync_switch.setChecked (sync_on);
        sync_switch.jumpDrawablesToCurrentState();
        sync_switch.setOnCheckedChangeListener (mSyncSwitchListener);
        refresh_button.setVisibility(sync_on?View.VISIBLE:View.INVISIBLE);

        View customView = mPopupWindow.getContentView();

        //Last time I succesfully synced.
        final String team = GlobalState.getInstance().getMyTeam();
        long timestamp = GlobalState.getInstance().getDb().getTimestampOfLatestSuccesfulSync(team);
        TextView time_last_succesful_sync = customView.findViewById(R.id.sync_time_since_last_sync);
        TextView time_last_entry = customView.findViewById(R.id.sync_time_since_last_entry);

        String time = "---";
        if (timestamp != 0) {
            Date date = new Date(timestamp);
            time = print_time_diff(date);
        }
        //Last time i entered data
        time_last_succesful_sync.setText(time);
        timestamp = GlobalState.getInstance().getDb().getSendTimestamp(team);
        time = "---";
        if (timestamp != 0) {
            Date date = new Date(timestamp);
            time = date.toString();
        }
        time_last_entry.setText(time);

        //Remaining objects to sync
        TextView unsync = customView.findViewById(R.id.sync_objects_remaining);
        unsync.setText(Integer.toString(numOfUnsynchedEntries));

        LinearLayout ll = customView.findViewById(R.id.list_container);
        TextView sync_refresh_date = customView.findViewById(R.id.sync_refresh_date);

        ll.removeAllViews();

        if (syncGroup != null && syncGroup.getTeam() != null && syncGroup.getLastUpdate() != null) {
            sync_refresh_date.setText(syncGroup.getLastUpdate().toString());

            List<TeamMember> teamMemberList = syncGroup.getTeam();

            if (fullySynced) {
                ll.addView(rowBuffer.defaultRow(getString(R.string.in_sync)));


            } else {
                for (TeamMember tm : teamMemberList) {
                    ll.addView(rowBuffer.getRow(tm.user, tm.unsynched + "", print_time_diff(tm.getDate())));
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
                synkStatusText = getString(R.string.synk_send_receive);
                break;
            case R.drawable.syncerr:
                synkStatusText = getString(R.string.synk_error);
                break;
            case R.drawable.syncoff:
                synkStatusText = getString(R.string.synk_off);
                break;
            case R.drawable.insync:
                //Refresh
                synkStatusText = getString(R.string.in_sync);
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
                if (latestSignal != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("GPS Details")
                            .setMessage("GPS_X: " + latestSignal.x + "\n" +
                                    "GPS_Y: " + latestSignal.y + "\n" +
                                    "Accuracy: " + latestSignal.accuracy + "\n" +
                                    "Time since last value: " + Math.round((System.currentTimeMillis() - latestSignal.time) / 1000) + "s")
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
                //REFRESH
                getTeamSyncStatusFromServer();

                break;
            case MENU_ITEM_CONTEXT:
                Log.d("vortex", "gs is " + GlobalState.getInstance() + " gs " + gs);
                //Log.d("vortex","in click for context: gs "+(gs==null)+" varc "+(gs.getVariableCache()==null));
                if (gs != null && gs.getVariableCache() != null) {
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
                final TextView tv = dialog.findViewById(R.id.logger);
                final ScrollView sv = dialog.findViewById(R.id.logScroll);
                Typeface type = Typeface.createFromAsset(getAssets(),
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
                Button crashLog = dialog.findViewById(R.id.crashlog);

                print.setOnClickListener(v -> {
                    if (gs != null)
                        BackupManager.getInstance(gs).backupDatabase("dump");
                });

                crashLog.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String crashme = null;
                        crashme.charAt(0);
                    }
                });


                printLog.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick (View v){
                        printLog(log.getLogText());
                    }
                    private void printLog(CharSequence logText) {
                        if (logText == null || logText.length() == 0)
                            return;
                        try {
                            String fileName = "log.txt";
                            File outputFile = new File(Constants.VORTEX_ROOT_DIR + globalPh.get(PersistenceHelper.BUNDLE_NAME) + "/backup/", fileName);
                            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                            writer.write(logText.toString());
                            Toast.makeText(MenuActivity.this,
                                    "LOG successfully written to backup folder. Name: " + fileName,
                                    Toast.LENGTH_LONG).show();
                            writer.close();
                        } catch (IOException e) {
                            Log.e("Exception", "File write failed: " + e.toString());
                        }

                    }
                });



                break;

            case MENU_ITEM_SETTINGS:
                //close drawer menu if open
                if (Start.singleton.getDrawerMenu() != null)
                    Start.singleton.getDrawerMenu().closeDrawer();
                if (isSynkServiceRunning())
                    stopSync();
                Intent intent = new Intent(getBaseContext(), ConfigMenu.class);
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
        mPopupWindow.showAtLocation(findViewById(R.id.content_frame), Gravity.CENTER, 0, 0);
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
        try {
            if (n_month != d_month) {
                time = (n_month - d_month) + " " + getString(R.string.sync_time_months);
            } else if (n_day != d_day) {
                time = (n_day - d_day) + " " + getString(R.string.sync_time_days);
            } else if (n_hour != d_hour)
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

    private class RowBuffer {
        final Map<String, View> buffer = new HashMap<>();

        View getRow(String name, String obj, String time) {
            View entry = getRow(name);
            setText(entry, name, obj, time);
            return entry;
        }

        void clear() {
            for (View entry : buffer.values()) {
                if (entry.getParent() != null)
                    ((ViewGroup) entry.getParent()).removeView(entry);
            }
            buffer.clear();
        }

        private View getRow(String name) {
            View entry = buffer.get(name);
            if (entry == null) {
                entry = getLayoutInflater().inflate(R.layout.sync_popup_list_row, null);
                buffer.put(name, entry);
            } else {
                if (entry.getParent() != null)
                    ((ViewGroup) entry.getParent()).removeView(entry);
            }
            return entry;
        }

        View defaultRow(String text) {
            View row = getRow("default");
            setText(row, text, "", "");
            return row;
        }

        private void setText(View row, String name, String obj, String time) {
            ((TextView) row.findViewById(R.id.sync_row_user)).setText(name);
            ((TextView) row.findViewById(R.id.sync_row_unsynced)).setText(obj);
            ((TextView) row.findViewById(R.id.sync_row_unsync_date)).setText(time);

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
                Log.d("vortex", "in togglesync internet");

                if (on) {

                    //Check there is name and team.
                    String user = globalPh.get(PersistenceHelper.USER_ID_KEY);
                    String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
                    String app = globalPh.get(PersistenceHelper.BUNDLE_NAME);
                    if (user == null || user.length() == 0 || team == null || team.length() == 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Sync cannot start")
                                .setMessage("Missing team [" + team + "] or user name [" + user + "]. Please add under the Settings menu")
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
                            long tsSync = gs.getDb().getReceiveTimestamp(team);
                            b.putLong(Constants.TIMESTAMP_SYNC_RECEIVE, tsSync);
                            b.putString("user", user);
                            b.putString("app", app);
                            b.putString("team", team);
                            b.putString("uuid", gs.getUserUUID());

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
                            Log.d("vortex", "Synk server is not running");
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
        ContentResolver.cancelSync(mAccount, Start.AUTHORITY);
        ContentResolver.setSyncAutomatically(mAccount, Start.AUTHORITY, false);

        syncState = R.drawable.syncoff;
        Message reply = Message.obtain(null, SyncService.MSG_USER_STOPPED_SYNC);
        try {
            mService.send(reply);
        } catch (RemoteException e) {
            e.printStackTrace();
            syncState = R.drawable.syncerr;
        }
    }

    private void initializeSynchronisation() {
        mAccount = GlobalState.getmAccount(this);
        ContentResolver.addPeriodicSync(
                mAccount,
                Start.AUTHORITY,
                Bundle.EMPTY,
                Start.SYNC_INTERVAL);
        Log.d("vortex", "added periodic sync");

    }


    private void onCloseSync() {
        Log.d("vortex", "IN on close SYNC!!");
        refreshStatusRow();
        DataSyncSessionManager.stop();
    }


    /**
     * @author Terje
     * <p>
     * Helper class that allows other threads to interact with the UI main thread.
     * Caller must override onClose for specific actions to happen when aborting sync.
     */

    public static class UIProvider {

        static final int LOCK = 1, UNLOCK = 2, ALERT = 3, UPDATE_SUB = 4, CONFIRM = 5, UPDATE = 6, PROGRESS = 7, SHOW_PROGRESS = 8, CLOSE_PROGRESS = 9;
        private String row1 = "", row2 = "";
        private AlertDialog uiBlockerWindow = null;
        private ProgressDialog progress;
        private final Context mContext;

        UIProvider(Context context) {
            mContext = context;
        }

        final Handler mHandler = new Handler(Looper.getMainLooper()) {
            boolean twoButton = false;

            private void showProgress(int max) {
                progress = new ProgressDialog(mContext);
                progress.setIndeterminate(false);
                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progress.setMax(max);
                progress.setTitle("Inserting");
                progress.show();

            }

            private void progress(int curr) {
                if (progress != null) {
                    Log.d("vortex", "progress: " + curr);
                    progress.setProgress(curr);
                }
            }


            private void oneButton() {
                dismiss();
                uiBlockerWindow = new AlertDialog.Builder(mContext)
                        .setTitle("Synchronizing")
                        .setMessage("Receiving data..standby")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setNegativeButton(R.string.close, (dialog, which) -> onClose())
                        .show();
                twoButton = false;
            }

            private void twoButton(final ConfirmCallBack cb) {
                dismiss();
                uiBlockerWindow = new AlertDialog.Builder(mContext)
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
                twoButton = true;
            }


            private void dismiss() {
                if (uiBlockerWindow != null)
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
                        if (progress != null) {
                            progress.dismiss();
                            progress = null;
                        }
                        break;
                    case LOCK:
                        oneButton();
                        Log.d("vortex", "One button Lock interface");
                        break;

                    case UNLOCK:
                        uiBlockerWindow.cancel();
                        break;
                    case ALERT:
                        if (twoButton)
                            oneButton();
                        row1 = (String) msg.obj;
                        row2 = "";
                        uiBlockerWindow.setMessage(row1 + "\n" + row2);

                        break;
                    case UPDATE:
                        row1 = (String) msg.obj;
                        uiBlockerWindow.setMessage(row1 + "\n" + row2);
                        break;
                    case UPDATE_SUB:
                        row2 = (String) msg.obj;
                        uiBlockerWindow.setMessage(row1 + "\n" + row2);
                        break;

                    case CONFIRM:
                        if (!twoButton)
                            twoButton((ConfirmCallBack) msg.obj);

                        break;

                }


            }


        };


        public void startProgress(int totalRows) {
            if (progress == null) {
                Message msg = mHandler.obtainMessage(SHOW_PROGRESS);
                msg.arg1 = totalRows;
                msg.sendToTarget();
            }
        }

        public void setProgress(int count) {
            Message msg = mHandler.obtainMessage(PROGRESS);
            msg.arg1 = count;
            msg.sendToTarget();

        }

        void closeProgress() {
            mHandler.obtainMessage(CLOSE_PROGRESS).sendToTarget();
        }

        public void lock() {
            Log.d("vortex", "Lock called");
            mHandler.obtainMessage(LOCK).sendToTarget();

        }

        public void unlock() {
            Log.d("vortex", "UnLock called");
            mHandler.obtainMessage(UNLOCK).sendToTarget();

        }

        /**
         * @param msg Shows message and switched to one button dialog.
         */
        public void alert(String msg) {
            mHandler.obtainMessage(ALERT, msg).sendToTarget();

        }

        public void syncTry(String msg) {
            mHandler.obtainMessage(ALERT, msg).sendToTarget();

        }

        public void open() {
            mHandler.obtainMessage(UNLOCK).sendToTarget();
        }

        public void setInfo(String msg) {
            mHandler.obtainMessage(UPDATE_SUB, msg).sendToTarget();

        }


        void onClose() {

        }

        /**
         * @param msg Shows message and switched to two button dialog. Callback if positive ok is pressed. Otherwise onClose.
         */
        public void confirm(String msg, final ConfirmCallBack cb) {
            mHandler.obtainMessage(CONFIRM, cb).sendToTarget();
            mHandler.obtainMessage(UPDATE, msg).sendToTarget();
        }

        /**
         * @param msg Does not change dialog type.
         */
        public void update(String msg) {
            mHandler.obtainMessage(UPDATE, msg).sendToTarget();


        }


    }


    private boolean syncOn() {
        boolean syncOn = ContentResolver.getSyncAutomatically(mAccount, Start.AUTHORITY);
        Log.d("sync","in syncOn. Syncon is "+syncOn);
        return syncOn;
    }

    @Override
    public void gpsStateChanged(GPS_State signal) {
        if (signal.state == GPS_State.State.newValueReceived) {
            Log.d("glapp", "Got gps signal!");
            latestSignal = signal;
        }
        refreshStatusRow();
    }

    private boolean noGPS() {
        return (latestSignal == null);

    }

    private GPSQuality calculateGPSKQI() {
        if (latestSignal.accuracy <= 6)
            return GPSQuality.green;
        else if (latestSignal.accuracy <= 10)
            return GPSQuality.yellow;
        else
            return GPSQuality.red;
    }


    private boolean callInProgress = false;

    private void getTeamSyncStatusFromServer() {
        Log.d("vortex", "update team sync state called");
        //block multiple calls.
        if (!callInProgress) {
            callInProgress = true;
            String team = gs.getMyTeam();
            String project = globalPh.get(PersistenceHelper.BUNDLE_NAME);
            String useruuid = globalPh.get(PersistenceHelper.USERUUID_KEY);
            long timestamp = gs.getDb().getReceiveTimestamp(team);

            Log.d("vortex", "TIMESTAMP_LAST_SYNC_FROM_TEAM_TO_ME: " + timestamp);
            if (Connectivity.isConnected(this)) {
                //connected...lets call the sync server.
                final String SyncServerStatusCall = Constants.SynkStatusURI +
                        team + "&project=" + project + "&timestamp=" + timestamp + "&useruuid=" + useruuid;
                //final TextView mTextView = (TextView) findViewById(R.id.text);
                RequestQueue queue = Volley.newRequestQueue(this);

                StringRequest stringRequest = new StringRequest(Request.Method.GET, SyncServerStatusCall,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Display the first 500 characters of the response string.
                                Log.d("vortex", "Response is: " + response);
                                syncGroup = new SyncGroup(response);
                                //request a new sync if the team has data.
                                if (syncGroup.getTeam() != null && !syncGroup.getTeam().isEmpty()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                                    ContentResolver.requestSync(mAccount, Start.AUTHORITY, bundle);

                                }
                                callInProgress = false;
                                refreshSyncDisplay();
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("vortex", "Got an error when attempting to contact the sync server: " + error.getMessage());
                        callInProgress = false;
                    }
                });


                queue.add(stringRequest);
            } else {
                Log.d("vortex", "no connection");
                syncState=R.drawable.syncerr;
                callInProgress = false;
            }
        } else
            Log.d("vortex", "blocked call to getteamstatus");
    }




    private SyncGroup syncGroup = null;

    private SyncGroup getSyncGroup() {
        return syncGroup;
    }

    public class SyncGroup {

        private List<TeamMember> team = null;
        private Date lastUpdate;

        SyncGroup(String json) {
            //insert current data on the sync status for the team.
            //{"USER0":["T1",2,"2018-05-14 23:06:32.737"],"USER1":["T2",1,"2018-05-14 23:02:54.213"
            if (json != null) {
                //clear current state.
                team = new ArrayList<>();
                try {
                    JsonReader jr = new JsonReader(new StringReader(json));

                    jr.beginObject();
                    while (!jr.peek().equals(JsonToken.END_OBJECT)) {
                        String name = jr.nextName();
                        Log.d("vortex", name);
                        jr.beginArray();
                        String user = jr.nextString();
                        int unsynced = jr.nextInt();
                        Long date = jr.nextLong();
                        Log.d("vortex", "user:" + user + " unsynced: " + unsynced + " time: " + date);
                        jr.endArray();
                        //name, number of unsynced entries, datetime last seen on sync server.
                        addEntry(new TeamMember(user, unsynced, date));
                    }
                    lastUpdate=new Date(System.currentTimeMillis());

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }

        private void addEntry(TeamMember t) {
            if (team == null)
                team = new ArrayList<>();
            team.add(t);

        }

        List<TeamMember> getTeam() {
            return team;
        }

        Date getLastUpdate() {
            return lastUpdate;
        }
    }

    class TeamMember {
        final int unsynched;
        final String user;
        private final Long date;

        TeamMember(String user, int uns, Long date) {
            unsynched = uns;
            this.user = user;
            this.date = date;

        }

        Date getDate() {
            return new java.util.Date(date);
        }

        public Long getRawDate() {
            return date;
        }
    }

}
