package com.teraim.fieldapp.synchronization.framework;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.teraim.fieldapp.Start;

import static com.teraim.fieldapp.synchronization.framework.SyncAdapter.forceSyncToHappen;

/**
 * Define a Service that returns an IBinder for the
 * sync adapter class, allowing the sync adapter framework to call
 * onPerformSync().
 */
public class SyncService extends Service {

    // Storage for an instance of the sync adapter
    private static SyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();
  
    
    
    static Messenger mClient;
    
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_SYNC_ERROR_STATE = 2;
	public static final int MSG_SYNC_REQUEST_DB_LOCK = 3;
	public static final int MSG_SYNC_RELEASE_DB = 4;
	public static final int MSG_DATA_SAFELY_STORED = 5;
	public static final int MSG_DATABASE_LOCK_GRANTED = 6;
	public static final int MSG_SYNC_STARTED = 7;
	public static final int MSG_DEVICE_IN_SYNC = 8;
	public static final int MSG_USER_STOPPED_SYNC = 9;
	public static final int MSG_SYNC_DATA_ARRIVING = 10;
	public static final int MSG_START_SYNC = 11;
	public static final int MSG_SERVER_READ_MY_DATA = 12;

	public static final int ERR_UNKNOWN = 0;
	public static final int ERR_NOT_READY = 1;
	public static final int ERR_SYNC_BUSY = 2;
	public static final int ERR_NOT_INTERNET_SYNC = 3;
	public static final int ERR_SETTINGS = 4;
	public static final int ERR_SERVER_NOT_REACHABLE = 5;
	public static final int ERR_SERVER_CONN_TIMEOUT = 6;

	public static final int REFRESH = 9;


    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                	Log.d("vortex","received MSG_REGISTER_CLIENT in SyncService");
                    mClient=msg.replyTo;
                   	sSyncAdapter.setClient(mClient);
                    break;
                case MSG_DATABASE_LOCK_GRANTED:
					Log.d("vortex","Datalock granted. Inserting");
                   	sSyncAdapter.insertIntoDatabase();
                	break;
                //When data is safely inserted in database, update current time for last update.
                case MSG_DATA_SAFELY_STORED:
                	Log.d("vortex","received MSG_SAFELY_STORED in SyncService");
                	sSyncAdapter.updateCounters();
                	//force a new sync event to check for more data.
					SyncAdapter.forceSyncToHappen();
                	break;
                case MSG_SYNC_ERROR_STATE:
                	Log.d("vortex","received MSG_ERROR in SyncService");
                	sSyncAdapter.releaseLock();
                	break;
				case MSG_USER_STOPPED_SYNC:
					Log.d("vortex","received MSG_USER_STOPPED_SYNC in SyncService");
					break;
				case MSG_START_SYNC:
					Log.d("baboo","request to start sync");
					sSyncAdapter.releaseLock();
					//sSyncAdapter.onPerformSync(null,null,null,null,null);
					break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
   
    
    @Override
    public void onCreate() {
    
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("tortex","sync service destroyed");
	}

	/**
     * Return an object that allows the system to invoke
     * the sync adapter.
     *
     */
    @Override
    public IBinder onBind(Intent intent) {
        /*
         * Get the object that allows external processes
         * to call onPerformSync(). The object is created
         * in the base class code when the SyncAdapter
         * constructors call super()
         */

    	//sSyncAdapter.releaseLock();
    	if (intent.getAction().equals(Start.MESSAGE_ACTION)) {
    		Log.d("vortex","OnBind returning mMessgenger_Binder");
    		return mMessenger.getBinder();
    		}
    	else {
    		Log.d("vortex","In OnBindm returning syncAdapter_Binder");
    		if (mClient!=null) {
    			Log.e("vortex","myClient is not Null. Setting sSyncAdapter: "+mClient);
    			sSyncAdapter.setClient(mClient);
    		}
    		return sSyncAdapter.getSyncAdapterBinder();
    	}
    }
}
