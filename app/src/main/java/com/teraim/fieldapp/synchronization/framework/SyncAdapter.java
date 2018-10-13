package com.teraim.fieldapp.synchronization.framework;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.EndOfStream;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncFailed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {


    // Global variables
    // Define a variable to contain a content resolver instance
    private final ContentResolver mContentResolver;
    private Messenger mClient;

    private final Uri CONTENT_URI = Uri.parse("content://" + SyncContentProvider.AUTHORITY + "/synk");

    private boolean busy = true;
    private String mApp=null;
    private String mUser=null;
    private String mTeam=null;

    //Error codes.
    private boolean ERR_CONFIG,INIT_COMPLETE,ERR_TEMP_TIMESTAMP_MISSING,USER_STOPPED_SYNC;

    private long timestamp_from_team_to_me=-1;

    //temporary store timestamp in this
    private long potential_timestamp_from_team_to_me = -1;

    /**
     * Set up the sync adapter
     *
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mContentResolver = context.getContentResolver();
        lock();
    }



    public void init(Messenger client, long timestamp_from_team_to_me, String team, String user, String app) {
        //get the team and user.
        ERR_CONFIG = true;
        mTeam = team;
        mUser = user;
        mApp = app;
        Log.d("vortex","IN INIT SYNCADAPTER");
        //User stopped_sync should be false now.
        USER_STOPPED_SYNC = false;
        Log.d("vortex","USER_STOPPED now false");

        if (client == null || user == null || user.length() == 0 || team == null || team.length() == 0 || app == null || app.length() == 0) {
            Log.e("vortex", "Init error in Sync Adapter." + user + "," + team + "," + app + "," + INIT_COMPLETE);

        } else {
            //update both potential and known timestamp at start
            this.timestamp_from_team_to_me = timestamp_from_team_to_me;
            potential_timestamp_from_team_to_me = timestamp_from_team_to_me;
            mClient = client;
            ERR_CONFIG = false;
            INIT_COMPLETE = true;
        }
        releaseLock();
    }

    public void resetOnResume() {
        Log.d("vortex","IN RESET_ON_RESUME");
        INIT_COMPLETE=false;
    }

    private List<ContentValues> dataToInsert=null;


    /***
     * System interface for synchronisation.
     * @param accounts
     * @param extras
     * @param authority
     * @param provider
     * @param syncResult
     */
    @Override
    public void onPerformSync(Account accounts, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.d("vortex","system call on performsync");
        if (!isLocked())
            onPerformSync();
        else
            Log.e("vortex", "LOCKED - discarding call");
    }


    private void onPerformSync() {
        Message msg;
        int err = -1;

        Log.d("vortex", "************onPerformSync [" + mUser + "]");

        if (mClient == null ) {
            Log.e("vortex", "Not ready so discarding call");
            return;
        } else if (ERR_CONFIG || ERR_TEMP_TIMESTAMP_MISSING) {
            Log.e("vortex", "Error in config - discarding call");
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SETTINGS;
            sendMessage(msg);
            return;
        } else if (USER_STOPPED_SYNC) {
            Log.e("vortex", "User stopped the sync");
            return;
        }


        lock();
        //Tell client that the sync is now busy.
        sendMessage(Message.obtain(null, SyncService.MSG_SYNC_STARTED));

        //Check if there are unprocessed rows in sync table.
        if (syncDataAlreadyExists()) {
            Log.d("vortex","sync data exists...");
            sendMessage(Message.obtain(null, SyncService.MSG_SYNC_DATA_READY_FOR_INSERT));
            return;
        }
        Log.e("vortex", "FETCHING DATA TO SYNC");

        //Ask client for data entries
        Cursor c = mContentResolver.query(CONTENT_URI, null, null, null, null);
        assert(c!=null);

        StringBuilder targets=new StringBuilder();
        long maxStamp = -1;
        int maxToSync;
        Log.d("vortex","Found "+c.getCount()+" rows to sync from ["+mUser+"] to ["+mTeam+"]");
        String action,changes,variable;
        long entryStamp;
        int rowCount=0;
        //Either sync the number of lines returned, or MAX. Never more.
        int maxSyncableRows = 500;
        maxToSync = Math.min(c.getCount(), maxSyncableRows);

        SyncEntry[] syncEntries = new SyncEntry[maxToSync];

        while (c.moveToNext()&& rowCount < maxToSync) {
            action 		=	c.getString(c.getColumnIndex("action"));
            changes 	=	c.getString(c.getColumnIndex("changes"));
            entryStamp	=	c.getLong(c.getColumnIndex("timestamp"));
            variable 		= 	c.getString(c.getColumnIndex("target"));

            //Keep track of the highest timestamp in the set!
            if (entryStamp>maxStamp)
                maxStamp=entryStamp;

            syncEntries[rowCount++] = new SyncEntry(SyncEntry.action(action),changes,entryStamp,variable,mUser);
            targets.append(variable);
            targets.append(",");
        }
        c.close();
        Log.d("vortex","#ENTRIES ME->TEAM--> ["+maxToSync+"]");
        Log.d("vortex","VARIABLES --> ["+targets+"]");
        Log.d("vortex","TIMESTAMP ME-->TEAM--> ["+maxStamp+"]");

        long potential_timestamp_from_me_to_team = maxStamp;

        //Send and Receive.
        msg = sendAndReceive(
                syncEntries,
                potential_timestamp_from_me_to_team
        );
        if (msg!=null) {
            sendMessage(msg);
        }

        Log.d("vortex","exiting onsyncupdate");
    }

    public boolean isLocked() {
        return busy;
    }

    private boolean syncDataAlreadyExists() {
        Cursor c = mContentResolver.query(CONTENT_URI, null, "syncquery", null, null);
        assert (c!=null);
        if (c.moveToFirst()) {
            int iCount = c.getInt(0);
            if (iCount > 0) {
                Log.d("vortex", "sync table has entries: " + iCount);
                c.close();
                return true;
            }
            c.close();
            return false;
        }
        Log.e("vortex","cursor empty in syncDataAlreadyexist()");
        return false;
    }

    private void dataPump() {
        URL url ;
        URLConnection conn=null;
        int orderNr = 0;


        Log.d("vortex","In datapump");

        //Send a Start Sync message to the other side.

        //Send the SynkEntry[] array to server.
        try {
            url = new URL(Constants.SynkServerURI);
            conn = url.openConnection();
            conn.setConnectTimeout(10 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.addRequestProperty("type", "DATA_PUMP");
            conn.connect();

        }  catch (StreamCorruptedException e) {
            Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());

        }
        catch (SocketTimeoutException e) {
            e.printStackTrace();
            Message msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_CONN_TIMEOUT;
            Log.e("vortex","Socket timeout. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());

            return;
        }

        catch (IOException fe) {
            fe.printStackTrace();
            Message msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_NOT_REACHABLE;
            Log.e("vortex","IO Exception. Reason: "+fe.getMessage()+" Timestamp: "+System.currentTimeMillis());
            return;
        }

        catch (Exception ex) {
            ex.printStackTrace();

        }

    }

    private void sendMessage(Message msg) {
        if (mClient != null) {
            try {
                mClient.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                if (e instanceof DeadObjectException) {
                    Log.d("vortex","Dead object!!");
                    this.onSyncCanceled();
                }
            }
        } else
            Log.e("vortex","SendMessage failed. No client");

    }

    public void insertIntoDatabase() {
        Message msg;
        long d = System.currentTimeMillis();
        Log.d("vortex", "inserting into db called");
        if (dataToInsert !=null && dataToInsert.size()>0) {
            //Insert into database

            int i=0;

            for (ContentValues aDataToInsert : dataToInsert) {
                if (i % 10 == 0) {
                    Log.d("bascar", "inserting entry: " + i + "-" + (i + 10));

                    msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
                    msg.obj = bundle(i, dataToInsert.size());

                    sendMessage(
                            msg
                    );

                }
                i++;
                mContentResolver.insert(CONTENT_URI, aDataToInsert);
            }
        } else
            Log.e("vortex","No rows to insert in insertintoDB SyncAdapter");

        //send the from team timestamp to save in client if insert succeeds.
        msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_READY_FOR_INSERT);
        msg.obj = bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,potential_timestamp_from_team_to_me);
        sendMessage(msg);

        long t = System.currentTimeMillis()-d;
        Log.d("vortex","time used was "+t);
    }


    private void sendAwayBatch(ArrayList<ContentProviderOperation> list) {
        try {
            mContentResolver.applyBatch(SyncContentProvider.AUTHORITY, list);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

    }


    public static void forceSyncToHappen() {
        Log.d("vortex","trying to force sync");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(null, SyncContentProvider.AUTHORITY, bundle);
    }


    private Message sendAndReceive(SyncEntry[] dataToSend, long potential_timestamp_from_me_to_team) {
        Message msg;
        URL url ;
        URLConnection conn;
        assert(dataToSend!=null);


        Log.d("bascar","In Send And Receive.");

        ObjectOutputStream objOut=null;
        ObjectInputStream objIn=null;
        boolean sessionShouldEnd = true;

        try
        {
            url = new URL(Constants.SynkServerURI);
            conn = url.openConnection();

            conn.setConnectTimeout(10*1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
//			conn.addRequestProperty("type","TEAM_SYNK");

            // send object
            Log.d("vortex","creating outstream...");
            objOut = new ObjectOutputStream(conn.getOutputStream());

            //First syncgroup
            objOut.writeObject(mTeam);
            //Then user
            objOut.writeObject(mUser);
            //Then app name
            objOut.writeObject(mApp);
            //The last known timestamp.
            objOut.writeObject(timestamp_from_team_to_me);

            //check if more data to fetch or to send.
            boolean IHaveData=dataToSend.length>0;
            boolean ServerHasData=true;

            if (IHaveData) {
                Log.d("vortex","Writing "+dataToSend.length+" entries");
                objOut.writeObject(dataToSend);
            }

            //Done sending.
            objOut.writeObject(new EndOfStream());
            objOut.flush();


            //Receive.
            Object reply;
            objIn = new ObjectInputStream(conn.getInputStream());

            reply = objIn.readObject();
            Log.d("vortex","Server reply is "+(reply!=null?"not":"")+" null");

            if (reply instanceof String ) {
                //server read my data successfully.
                //Update read pointer (timestamp) to avoid sending same data again.
                if (potential_timestamp_from_me_to_team!=-1) {
                    Log.d("vortex","ME --> TEAM: "+potential_timestamp_from_me_to_team);
                } else {
                    if(IHaveData){
                        Log.e("vortex","I have data but timestamp is -1!!!!");
                    }
                    Log.d("vortex", "No new data from me. Timestamp not changed.");
                }

                msg = Message.obtain(null, SyncService.MSG_SERVER_ACKNOWLEDGED_READ);
                Log.d("vortex","Sending MSG_SERVER_ACKNOWLEDGED_READ");
                //We know that the SyncEntries from this user are safely stored.
                msg.obj = bundle(Constants.TIMESTAMP_LABEL_FROM_ME_TO_TEAM, potential_timestamp_from_me_to_team);
                sendMessage(msg);

                int numberOfEntriesFromServer = Integer.parseInt((String) reply);
                Log.d("vortex","Server sends: "+numberOfEntriesFromServer+" entries");
                int insertedRows=0;

                dataToInsert = new ArrayList<>();

                while (true) {

                    reply = objIn.readObject();

                    //Timestamp (Long) is the last message from the Server.
                    if (reply instanceof Long) {

                        objIn.close();
                        //Set a potential timestamp. Dont freeze until data read by client.
                        Log.d("vortex","Got a timestamp from the server: "+reply);
                        potential_timestamp_from_team_to_me = (Long)reply;

                        Log.d("vortex","Inserted rows: "+insertedRows);

                        if (insertedRows == 0 ) {
                            Log.d("vortex","In sync with server. Nothing to update");
                            //update timestamp - immediately.
                            timestamp_from_team_to_me = potential_timestamp_from_team_to_me;
                            //check if data to send from me to team.
                            if (IHaveData) {
                                msg = Message.obtain(null, SyncService.MSG_NO_NEW_DATA_FROM_TEAM_TO_ME);
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,timestamp_from_team_to_me);
                                //Retrigger sync after a 250 ms.
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        this.cancel();
                                        onPerformSync();
                                    }
                                }, 150);
                            } else {
                                msg = Message.obtain(null, SyncService.MSG_ALL_SYNCED);
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,timestamp_from_team_to_me);
                                Log.d("vortex","All is synced.");

                            }
                            return msg;
                        } else {
                            Log.d("vortex","Insert into DB begins");
                            //keep lock
                            sessionShouldEnd = false;
                            return Message.obtain(null, SyncService.MSG_SYNC_REQUEST_DB_LOCK);
                        }
                    } else if (reply instanceof SyncFailed) {
                        Log.e("vortex","SYNC FAILED. REASON: "+((SyncFailed)reply).getReason());
                        break;

                    } else if (reply instanceof byte[]) {

                        ContentValues cv = new ContentValues();
                        cv.put("DATA", (byte[])reply);
                        cv.put("count",insertedRows++);
                        dataToInsert.add(cv);

                        if (insertedRows%10==0) {
                            msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
                            msg.obj=bundle(insertedRows,numberOfEntriesFromServer);
                            sendMessage(msg);
                        }
                    }
                    else {
                        Log.e("vortex","Got back alien object!! "+reply.getClass().getSimpleName());
                        break;
                    }
                }
            }

            else {
                Log.e("vortex","OK not returned. instead "+reply!=null?reply.getClass().getCanonicalName():"NULL");
                if (reply instanceof SyncFailed) {
                    Log.e("vortex",((SyncFailed)reply).getReason());
                }

            }

        } catch (StreamCorruptedException e) {
            Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());

        }
        catch (SocketTimeoutException e) {
            e.printStackTrace();
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_CONN_TIMEOUT;
            Log.e("vortex","ERR_SERVER_CONN_TIMEOUT. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
            return msg;
        }

        catch (IOException fe) {
            fe.printStackTrace();
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_NOT_REACHABLE;
            return msg;
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if (sessionShouldEnd)
                releaseLock();
            try {
                if (objIn != null)
                    objIn.close();
                if (objOut != null)
                    objOut.close();
            } catch (IOException ignored) {

            }
        }

        msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
        msg.arg1=SyncService.ERR_UNKNOWN;
        return msg;

    }

    private Object bundle(int numberOfRowsSoFar, int numberOfRowsTotal) {
        Bundle b = new Bundle();
        b.putInt("number_of_rows_so_far",numberOfRowsSoFar);
        b.putInt("number_of_rows_total",numberOfRowsTotal);
        return b;
    }


    private Object bundle(String label, Long timeStamp) {
        Bundle b = new Bundle();
        b.putLong(label,timeStamp);
        return b;
    }

    public void releaseLock() {
        busy=false;
        Log.d("vortex","**SyncAdapter UN-Locked**");
    }

    public void lock() {
        Log.d("vortex","**SyncAdapter Locked**");
        busy=true;
    }


    public void safely_stored() {
        Log.d("vortex","Safely stored, release lock");
        if (potential_timestamp_from_team_to_me==-1)
            ERR_TEMP_TIMESTAMP_MISSING=true;
        else {
            timestamp_from_team_to_me = potential_timestamp_from_team_to_me;
            Log.d("vortex","TIMESTAMP TEAM->ME NOW "+timestamp_from_team_to_me);
        }

        releaseLock();
    }


    public boolean isInitDone() {
        return INIT_COMPLETE;
    }

    public void userAbortedSync() {
        Log.d("vortex","USER_STOPPED now true");
        USER_STOPPED_SYNC = true;
    }
}