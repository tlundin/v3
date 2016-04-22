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
  
    
    
    Messenger mClient; 
    
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_SYNC_ERROR_STATE = 2;
	public static final int MSG_SYNC_REQUEST_DB_LOCK = 3;
	public static final int MSG_SYNC_RELEASE_DB = 4;
	public static final int MSG_DATA_SAFELY_STORED = 5;
	public static final int MSG_DATABASE_LOCK_GRANTED = 6;
	public static final int MSG_SYNC_STARTED = 7;
	public static final int MSG_DEVICES_IN_SYNC = 8;
	
	
	
	public static final int ERR_NOT_READY = 1;
	public static final int ERR_SYNC_BUSY = 2;
	public static final int ERR_NOT_INTERNET_SYNC = 3;
	public static final int ERR_SETTINGS = 4;
	public static final int ERR_UNKNOWN = 0;
	public static final int ERR_SERVER_NOT_REACHABLE = 6;
	public static final int ERR_SERVER_CONN_TIMEOUT = 7;
	public static final int REFRESH = 8;
	

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                	Log.d("vortex","received MSG_REGISTER_CLIENT in SyncService");
                    mClient=msg.replyTo;
                   	sSyncAdapter.setClient(mClient);
                    break;
                case MSG_DATABASE_LOCK_GRANTED:
                   	sSyncAdapter.insertIntoDatabase();
                	break;
                //When data is safely inserted in database, update current time for last update.
                case MSG_DATA_SAFELY_STORED:
                	Log.d("vortex","received MSG_SAFELY_STORED in SyncService");
                	sSyncAdapter.updateCounters();
                	break;
                case MSG_SYNC_ERROR_STATE:
                	Log.d("vortex","received MSG_ERROR in SyncService");
                	sSyncAdapter.releaseLock();
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
    	sSyncAdapter.releaseLock();
    	if (intent.getAction().equals(Start.MESSAGE_ACTION)) {
    		Log.d("vortex","MASSAGE!");
    		return mMessenger.getBinder();
    		}
    	else {
    		Log.d("vortex","CRISP!");
    		if (mClient!=null) {
    			Log.d("vortex","CRAP!");
    			sSyncAdapter.setClient(mClient);
    		}
    		return sSyncAdapter.getSyncAdapterBinder();
    	}
    }
}
