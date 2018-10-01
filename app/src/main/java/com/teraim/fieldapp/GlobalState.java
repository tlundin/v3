package com.teraim.fieldapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.expr.Aritmetic;
import com.teraim.fieldapp.expr.Parser;
import com.teraim.fieldapp.gis.Tracker;
import com.teraim.fieldapp.loadermodule.Configuration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.synchronization.ConnectionManager;
import com.teraim.fieldapp.synchronization.SyncMessage;
import com.teraim.fieldapp.ui.DrawerMenu;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.BackupManager;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author Terje
 *
 * Classes defining datatypes for ruta, provyta, delyta and tåg.
 * There are two Scan() functions reading data from two input files (found under the /raw project folder).
 */
public class GlobalState {

    //access only through getSingleton(Context).
    //This is because of the Activity lifecycle. This object might need to be re-instantiated any time.
    private static GlobalState singleton;


    private static Context myC = null;
    private String imgMetaFormat = Constants.DEFAULT_IMG_FORMAT;
    private final LoggerI logger;
    private PersistenceHelper ph = null;
    private DbHelper db = null;
    private Parser parser = null;
    private VariableConfiguration artLista = null;
    //Map workflows into a hash with name as key.
    private final Map<String, Workflow> myWfs;
    //Spinner definitions
    private final SpinnerDefinition mySpinnerDef;
    private DrawerMenu myDrawerMenu;

    public String TEXT_LARGE;
    private WF_Context currentContext;
    private String myPartner = "?";

    private PersistenceHelper globalPh = null;
    private Tracker myTracker = null;
    private final ConnectionManager myConnectionManager;
    private final BackupManager myBackupManager;


    private final VariableCache myVariableCache;
    private static Account mAccount;
    private GisObject selectedGop;
    private final CharSequence logTxt;

    public static GlobalState getInstance() {

        return singleton;
    }

    public static GlobalState createInstance(Context applicationContext, PersistenceHelper globalPh,
                                             PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
                                             List<Workflow> workflows, Table t, SpinnerDefinition sd, CharSequence logTxt, String imgMetaFormat) {
        singleton = null;
        return new GlobalState(applicationContext, globalPh,
                ph, debugConsole, myDb,
                workflows, t, sd, logTxt, imgMetaFormat);

    }

    //private GlobalState(Context ctx)  {
    private GlobalState(Context applicationContext, PersistenceHelper globalPh,
                        PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
                        List<Workflow> workflows, Table t, SpinnerDefinition sd, CharSequence logTxt, String imgMetaFormat) {

        myC = applicationContext;
        this.globalPh = globalPh;
        this.ph = ph;

        this.db = myDb;
        //TODO REMOVE
        db.fixYearNull();
        this.logger = debugConsole;
        //Parser for rules
        parser = new Parser(this);

        artLista = new VariableConfiguration(this, t);
        myWfs = mapWorkflowsToNames(workflows);
        //Event Handler on the Bluetooth interface.
        //myHandler = getHandler();
        //Handles status for
        myStatusHandler = new StatusHandler(this);

        mySpinnerDef = sd;

        singleton = this;


        myVariableCache = new VariableCache(this);

        //GPS listener service


        //myExecutor = new RuleExecutor(this);

        myConnectionManager = new ConnectionManager(this);

        myBackupManager = new BackupManager(this);

        this.logTxt = logTxt;

        Log.d("fennox", "my ID is " + getMyId());
        Log.d("jgw", "my imgmeta is " + imgMetaFormat);
        if (imgMetaFormat != null)
            this.imgMetaFormat = imgMetaFormat;
        //check current state of synk server.
        if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals("Internet"))
            getServerSyncStatus();

        Log.d("antrax","GS VALUE AT START: "+ph.getL(PersistenceHelper.TIMESTAMP_LAST_SYNC_FROM_ME +getMyTeam()));
        Log.d("antrax","GS VALUE AT START: "+ph.getL(PersistenceHelper.TIME_OF_LAST_BACKUP));
        Log.d("antrax","GS VALUE AT START: "+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP));
        Log.d("antrax","GS VALUE AT START: "+ph.getL(PersistenceHelper.TIME_OF_LAST_SYNC_FROM_TEAM +getMyTeam()));

    }


    public static Account getmAccount(Context ctx) {
        if (mAccount == null)
            mAccount = CreateSyncAccount(ctx);
        return mAccount;
    }

    public String getMyTeam() {
        return globalPh.get(PersistenceHelper.LAG_ID_KEY);
    }
    /*Validation
     *
     */


    /*Singletons available for all classes
     *
     */
    public SpinnerDefinition getSpinnerDefinitions() {
        return mySpinnerDef;
    }

    //Persistance for app specific variables.
    public PersistenceHelper getPreferences() {
        return ph;
    }

    //Persistence for global, non app specific variables
    public PersistenceHelper getGlobalPreferences() {
        return globalPh;
    }


    public DbHelper getDb() {
        return db;
    }

    public Parser getParser() {
        return parser;
    }

    public Context getContext() {
        return myC;
    }

    //public RuleExecutor getRuleExecutor() {
    //	return myExecutor;
    //}

    public VariableConfiguration getVariableConfiguration() {
        return artLista;
    }

    public BackupManager getBackupManager() {
        return myBackupManager;
    }

    public String getImgMetaFormat() {
        return imgMetaFormat;
    }

    /**************************************************
     *
     * Mapping workflow to workflow name.
     */

    private Map<String, Workflow> mapWorkflowsToNames(List<Workflow> l) {
        Map<String, Workflow> ret = null;
        if (l == null)
            Log.e("NILS", "Parse Error: Workflowlist is null in SetWorkFlows");
        else {

            for (Workflow wf : l)
                if (wf != null) {
                    if (wf.getName() != null) {
                        Log.d("NILS", "Adding wf with name " + wf.getName() + " and length " + wf.getName().length());
                        if (ret == null)
                            ret = new TreeMap<String, Workflow>(String.CASE_INSENSITIVE_ORDER);

                        ret.put(wf.getName(), wf);
                    } else
                        Log.d("NILS", "Workflow name was null in setWorkflows");
                } else
                    Log.d("NILS", "Workflow was null in setWorkflows");
        }
        return ret;
    }

	/*
	public Table thawTable() { 	
		return ((Table)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.CONFIG_FROZEN_FILE_ID));		
	}
	 */

    public Workflow getWorkflow(String id) {
        if (id == null || id.isEmpty())
            return null;
        return myWfs.get(id);
    }

    public Workflow getWorkflowFromLabel(String label) {
        if (label == null)
            return null;
        for (Workflow wf : myWfs.values())
            if (wf.getLabel() != null && wf.getLabel().equals(label))
                return wf;
        Log.e("nils", "flow not found: " + label);
        return null;
    }


    public String[] getWorkflowNames() {
        if (myWfs == null)
            return null;
        String[] array = new String[myWfs.keySet().size()];
        myWfs.keySet().toArray(array);
        return array;

    }

    public String[] getWorkflowLabels() {
        if (myWfs == null)
            return null;
        String[] array = new String[myWfs.keySet().size()];
        int i = 0;
        String label;
        for (Workflow wf : myWfs.values()) {
            label = wf.getLabel();
            if (label != null)
                array[i++] = label;
        }
        return array;

    }


    public synchronized Aritmetic makeAritmetic(String name, String label) {
		/*Variable result = myVars.get(name);
		if (result == null) {
		    myVars.put(name, result = new Aritmetic(name,label));
		    return (Aritmetic)result;
		}
		else {
			return (Aritmetic)result;
		}
		 */
        return new Aritmetic(name, label);
    }

    public VariableCache getVariableCache() {
        return myVariableCache;
    }

    public LoggerI getLogger() {
        return logger;
    }
/*
	public void setCurrentWorkflowContext(WF_Context myContext) {
		currentContext = myContext;
	}

	public WF_Context getCurrentWorkflowContext() {
		return currentContext;
	}
*/

    public void setDBContext(DB_Context context) {
        myVariableCache.setCurrentContext(context);
    }

    public boolean isMaster() {
        String m;
        if ((m = globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY_NEW)).equals(PersistenceHelper.UNDEFINED)) {
            globalPh.put(PersistenceHelper.DEVICE_COLOR_KEY_NEW, "Master");
            return true;
        } else
            return m.equals("Master");

    }

    public boolean isSolo() {
        return globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY_NEW).equals("Solo");
    }

    public boolean isSlave() {
        return globalPh.get(PersistenceHelper.DEVICE_COLOR_KEY_NEW).equals("Client");
    }

    public GisObject getSelectedGop() {
        return selectedGop;
    }

    public void setSelectedGop(GisObject go) {
        selectedGop = go;
    }


    //Map<String,WF_Static_List> listCache = new HashMap<>();

    /*public WF_Static_List getListFromCache(String blockId) {
        return listCache.get(blockId);
    }

    public void addListToCache(String blockId, WF_Static_List list) {
        listCache.put(blockId,list);
    }
    */
	/*
	public MessageHandler getHandler() {
		if (myHandler==null)
			myHandler = getNewMessageHandler(isMaster());
		return myHandler;
	}

	public void resetHandler() {
		myHandler = getNewMessageHandler(isMaster());
		getHandler();
	}

	private MessageHandler getNewMessageHandler(boolean master) {
		if (master)
			return new MasterMessageHandler();
		else
			return new SlaveMessageHandler();
	}
	 */
    public enum ErrorCode {
        ok,
        missing_required_column,
        file_not_found, workflows_not_found,
        tagdata_not_found, parse_error,
        config_not_found, spinners_not_found,
        missing_lag_id,
        missing_user_id,

    }


    public ErrorCode checkSyncPreconditions() {
        if (this.isMaster() && globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED))
            return ErrorCode.missing_lag_id;
        else if (globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED))
            return ErrorCode.missing_user_id;

        else
            return ErrorCode.ok;
    }


    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            Intent intent = null;

            if (msg.obj instanceof String) {
                //Log.d("vortex","IN HANDLE MESSAGE WITH MSG: "+msg.toString());
                String s = (String) msg.obj;
                intent = new Intent();
                intent.setAction(s);
            } else if (msg.obj instanceof Intent)
                intent = (Intent) msg.obj;
            if (intent != null)
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            else
                Log.e("vortex", "Intent was null in handleMessage");

        }


    };


    public void sendSyncEvent(Intent intent) {
        Log.d("vortex", "IN SEND SYNC EVENT WITH ACTION " + intent.getAction());
        if (mHandler != null) {
            Message m = Message.obtain(mHandler);
            m.obj = intent;
            m.sendToTarget();
        } else
            Log.e("vortex", "NO MESSAGE NO HANDLER!!");
    }

    public void sendEvent(String action) {
        Log.d("vortex", "IN SEND EVENT WITH ACTION " + action);
        if (mHandler != null) {
            Message m = Message.obtain(mHandler);
            m.obj = action;
            m.sendToTarget();
        } else
            Log.e("vortex", "NO MESSAGE NO HANDLER!!");
    }

    private SyncMessage message;


    private final StatusHandler myStatusHandler;


    public void setSyncMessage(SyncMessage message) {
        this.message = message;
    }

    public SyncMessage getOriginalMessage() {
        return message;
    }


    public void setMyPartner(String partner) {
        myPartner = partner;
    }

    public String getMyPartner() {
        return myPartner;
    }

    public StatusHandler getStatusHandler() {
        return myStatusHandler;
    }


    /*
        public void synchronise(SyncEntry[] ses, boolean isMaster) {
            Log.e("nils,","SYNCHRONIZE. MESSAGES: ");
            setSyncStatus(SyncStatus.writing_data);
            for(SyncEntry se:ses) {
                Log.e("nils","Action:"+se.getAction());
                Log.e("nils","Target: "+se.getTarget());
                Log.e("nils","Keys: "+se.getKeys());
                Log.e("nils","Values:"+se.getValues());
                Log.e("nils","Change: "+se.getChange());

            }
            db.synchronise(ses, myVarCache,this);

        }
    */
    public DrawerMenu getDrawerMenu() {
        // TODO Auto-generated method stub
        return myDrawerMenu;
    }

    public void setDrawerMenu(DrawerMenu mDrawerMenu) {
        myDrawerMenu = mDrawerMenu;
    }


    public Map<String, Workflow> getWfs() {
        return myWfs;
    }


    //Change current context (side effect) to the context given in the workflow startblock.
    //If no context can be built (missing variable values), return error. Otherwise, return null.


    public void setModules(Configuration myModules) {
        Configuration myModules1 = myModules;
    }


    public static void destroy() {
        if (singleton.getTracker() != null)
            singleton.getTracker().stopSelf();

        singleton = null;


    }

    public Tracker getTracker() {
        if (myTracker == null)
            myTracker = new Tracker();
        return myTracker;
    }

    public File getCachedFileFromUrl(String fileName) {
        return Tools.getCachedFile(fileName, Constants.VORTEX_ROOT_DIR + globalPh.get(PersistenceHelper.BUNDLE_NAME) + "/cache/");
    }


    public ConnectionManager getConnectionManager() {
        return myConnectionManager;
    }

    //Get a string resource and print it. convenience function.
    public CharSequence getString(int identifier) {
        return getContext().getResources().getString(identifier);
    }


    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                Start.ACCOUNT, Start.ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        Start.ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            Log.d("vortex", "Created account: " + newAccount.name);
            
        } else {
        	/*
        	Account[] aa = accountManager.getAccounts();
        	Log.d("vortex","Accounts found: ");
        	for (Account a:aa) {
        		Log.d("vortex",a.name);
        		if (a.equals(newAccount)) {
        			Log.d("vortex","failed...exists..");
        			break;
        		}
        	}
        	*/
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            Log.d("vortex", "add  sync account failed for some reason");
        }
        return newAccount;
    }

    public void onStart() {
        //check synk
        //db.processSyncEntriesIfAny();

    }


    private String getMyId() {
        return Settings.Secure.getString(getContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }


    public CharSequence getLogTxt() {
        return logTxt;
    }


    public String getServerSyncStatus() {
        Context ctx = getContext();
        if (ctx != null) {
            Log.d("vortex", "update team sync state called");

            String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
            String project = globalPh.get(PersistenceHelper.BUNDLE_NAME);
            String user = globalPh.get(PersistenceHelper.USER_ID_KEY);

            Long timestamp = ph.getL(PersistenceHelper.TIME_OF_LAST_SYNC_FROM_TEAM +team);
            Log.d("vortex","TIMESTAMP_LAST_SYNC_FROM_TEAN_TO_ME: "+timestamp);
           timestamp=(timestamp==-1)?0:timestamp;
            Log.d("mama","TS: "+timestamp);
            if (Connectivity.isConnected(ctx)) {
                //connected...lets call the sync server.
                final String SyncServerStatusCall = Constants.SynkServerURI + "?action=get_team_status&team=" +
                        team + "&project=" + project + "&timestamp=" + timestamp+"&user="+user;
                //final TextView mTextView = (TextView) findViewById(R.id.text);
                RequestQueue queue = Volley.newRequestQueue(ctx);

                StringRequest stringRequest = new StringRequest(Request.Method.GET, SyncServerStatusCall,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Display the first 500 characters of the response string.
                                Log.d("mama", "Response is: " + response);
                                syncGroup = new SyncGroup(response);
                                //request a new sync if the team has data.
                                if (syncGroup.getTeam()!=null && !syncGroup.getTeam().isEmpty()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                                    ContentResolver.requestSync(mAccount, Start.AUTHORITY, bundle);

                                }
                                sendEvent(MenuActivity.REDRAW);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("mama", "That didn't work! " + error.getMessage());
                    }
                });

// Add the request to the RequestQueue.
                queue.add(stringRequest);
            } else
                Log.d("mama", "no internet...");
        }

        Log.d("mama", "I return here");
        return null;
    }

    private SyncGroup syncGroup = null;

    public SyncGroup getSyncGroup() {
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

        public List<TeamMember> getTeam() {
            return team;
        }

        public Date getLastUpdate() {
            return lastUpdate;
        }
    }

    public class TeamMember {
        public final int unsynched;
        public final String user;
        private final Long date;

        TeamMember(String user, int uns, Long date) {
            unsynched = uns;
            this.user = user;
            this.date = date;

        }

        public Date getDate() {
                return new java.util.Date(date);
        }

        public Long getRawDate() {
            return date;
        }
    }


}




