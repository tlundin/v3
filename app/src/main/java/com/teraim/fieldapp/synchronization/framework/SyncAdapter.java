package com.teraim.fieldapp.synchronization.framework;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.EndOfStream;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncFailed;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.SyncFailedException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final ContentResolver mContentResolver;
    private final int MaxSyncableRows = 1000;
    private final int MaxSyncableRowsServer = 10;
    
    private Messenger mClient;
    public  static final Uri BASE_CONTENT_URI = Uri.parse("content://" + SyncContentProvider.AUTHORITY + "/synk");
    public  static final Uri SYNC_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(DbHelper.TABLE_SYNC)
            .build();
    public  static final Uri TIMESTAMPS_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(DbHelper.TABLE_TIMESTAMPS)
            .build();
    public  static final Uri SYNC_DATA_URI = BASE_CONTENT_URI.buildUpon()
            .appendPath(DbHelper.TABLE_SYNC)
            .build();



    private String mApp=null;
    private String mUser=null;
    private String mTeam=null;
    private String mUUID=null;
    private long mTimestamp_receive;


    private boolean INITIALIZED = false;
    private boolean USER_STOPPED_SYNC = false;
    private boolean LOCKED = true;

    private class StampedData {
        Object data;
        long timestamp;
        boolean hasMore = false;

        public StampedData(Object data, long timestamp,boolean hasMore) {
            this.data = data;
            this.timestamp=timestamp;
            this.hasMore=hasMore;
        }
    }


    /**
     * Set up the sync adapter
     *
     */

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
        Log.d("vortex", "I was called at "+ Tools.getCurrentTime());
    }



    public void init(Messenger client,
                     String team,
                     String user,
                     String app,
                     String uuid,
                     long last_known_receiveTimestamp) {

        Log.d("vortex","IN INIT SYNCADAPTER");
        Log.d("vortex", "I was called at "+ Tools.getCurrentTime());
        mTeam = team;
        mUser = user;
        mApp = app;
        mUUID = uuid;
        mClient = client;
        mTimestamp_receive = last_known_receiveTimestamp;
        INITIALIZED = true;
    }

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
        Log.d("vortex","System call onPerformSync");
        if (!isLocked())
            onPerformSync(syncResult);
        else
            Log.e("vortex", "LOCKED - discarding call");
    }


    private void onPerformSync(SyncResult syncResult) {
        Message msg;
        int err = -1;

        Log.d("vortex", "************onPerformSync [" + mUser + "]");

        if (LOCKED) {
            Log.e("vortex", "Locked...exit");
        }

        else if (!INITIALIZED) {
            Log.e("vortex", "Not initialized...exit");
            syncResult.delayUntil = (System.currentTimeMillis() / 1000) + 5;
            return;
        }
        else if (USER_STOPPED_SYNC) {
            Log.e("vortex", "User has stopped the sync");
            return;
        }

        LOCKED = true;
        //Tell client that the sync is now busy.
        sendMessage(Message.obtain(null, SyncService.MSG_SYNC_STARTED));

        //Write any data to the Server.
        try {
            //get my Sync Entries
            StampedData dataOut = getMyUnsynchedEntries();
            //long potential_timestamp_from_me_to_team = maxStamp;
            if (dataOut != null ) {
                sendSyncData((SyncEntry[]) dataOut.data);
                updateCounter(dataOut.timestamp,Constants.TIMESTAMP_SEND_POSITION);
            }
        //Check if there are unprocessed rows in sync table.
        if (syncDataAlreadyExists()) {
            Log.d("vortex","sync data exists to insert");
            sendMessage(Message.obtain(null, SyncService.MSG_SYNC_DATA_READY_FOR_INSERT));
            return;
        }

        StampedData dataIn = receiveSyncData(mTimestamp_receive);

        if (dataIn != null) {
            int rowsInserted = mContentResolver.bulkInsert(SYNC_DATA_URI,(ContentValues[])dataIn.data);
            updateCounter(dataIn.timestamp,Constants.TIMESTAMP_RECEIVE_POSITION);
        }

        } catch (SyncFailedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    //Send counter - where next chunk of data to send should begin
    private void updateCounter(long timestamp, String label) throws SyncFailedException {
        ContentValues cv = new ContentValues();
        cv.put(DbHelper.Table_Timestamps.LABEL.name(), label);
        cv.put(DbHelper.Table_Timestamps.VALUE.name(), timestamp);
        cv.put(DbHelper.Table_Timestamps.SYNCGROUP.name(), mTeam);
        mContentResolver.insert(TIMESTAMPS_URI,cv);
    }
    private StampedData getMyUnsynchedEntries() throws SyncFailedException {

        Cursor c = mContentResolver.query(BASE_CONTENT_URI, null, null, null, null);
        if (c==null)
            throw new SyncFailedException("ContentResolver null in getMyUnsynchedEntries");

        //StringBuilder targets=new StringBuilder();
        long maxStamp = -1,entryStamp;
        int rows,maxToSync,rowCount=0;
        String action,changes,variable;


        rows = c.getCount();

        Log.d("vortex",rows+" rows to sync from ["+mUser+"] to ["+mTeam+"]");
        if (rows == 0)
            return null;

        maxToSync = Math.min(c.getCount(), MaxSyncableRows);
        boolean hasMore = maxToSync==MaxSyncableRows;

        SyncEntry[] syncEntries = new SyncEntry[maxToSync];

        while (c.moveToNext() && rowCount < maxToSync) {
            action 		=	c.getString(c.getColumnIndex("action"));
            changes 	=	c.getString(c.getColumnIndex("changes"));
            entryStamp	=	c.getLong(c.getColumnIndex("timestamp"));
            variable    = 	c.getString(c.getColumnIndex("target"));
            //Keep track of the highest timestamp in the set!
            if (entryStamp>maxStamp)
                maxStamp=entryStamp;

            syncEntries[rowCount++] = new SyncEntry(SyncEntry.action(action),changes,entryStamp,variable,mUser);
            //targets.append(variable);
            //targets.append(",");
        }
        c.close();

        return new StampedData(syncEntries, maxStamp,hasMore);
    }
    private void sendSyncData(SyncEntry[] dataToSend) throws IOException  {

        URLConnection conn = getSyncConnection(Constants.SendDataURI);
        ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
        sendHeader(out);

        Log.d("vortex","Writing "+dataToSend.length+" entries");
        out.writeObject(dataToSend);
        out.writeObject(new EndOfStream());
        out.flush();
        out.close();
    }

    //returns data and a new read position
    private StampedData receiveSyncData(long readPosition) throws IOException, ClassNotFoundException {

        URLConnection conn = getSyncConnection(Constants.ReadDataURI);
        ObjectOutputStream out = new ObjectOutputStream(conn.getOutputStream());
        sendHeader(out);
        out.writeObject(readPosition);

        //Receive.
        Object reply;
        ObjectInputStream in = new ObjectInputStream(conn.getInputStream());
        int numberOfEntriesFromServer = Integer.parseInt((String) in.readObject());
        boolean hasMore = numberOfEntriesFromServer == MaxSyncableRowsServer;
        int rowC=0;
        ContentValues[] dataToInsert = new ContentValues[numberOfEntriesFromServer];
        while (true) {
            reply = in.readObject();
            if (reply instanceof byte[]) {
                ContentValues cv = new ContentValues();
                cv.put("data", (byte[]) reply);
                dataToInsert[rowC++]=cv;

                if (rowC%10==0) {
                    Message msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
                    msg.obj=bundle(rowC,numberOfEntriesFromServer);
                    sendMessage(msg);
                }
            }

            //Timestamp (Long) is the last message from the Server.
            if (reply instanceof Long) {
                in.close();
                Log.d("vortex", "Timestamp from the server: " + reply);
                if (rowC==0)
                    //no change
                    return null;
                else {
                    return new StampedData(dataToInsert, (Long) reply,hasMore);
                }
            } else if (reply instanceof SyncFailed) {
                String reason = ((SyncFailed) reply).getReason();
                Log.e("vortex", "SYNC FAILED. REASON: " + reason);
                in.close();
                throw new SyncFailedException(reason);
            }
        }
    }


    private URLConnection getSyncConnection(String uri) throws IOException {
        URL url ;
        URLConnection conn=null;
        url = new URL(uri);
        conn = url.openConnection();
        conn.setConnectTimeout(10*1000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        return conn;
    }

    private void sendHeader(ObjectOutputStream out) throws IOException {
        //syncgroup
        out.writeObject(mTeam);
        //user
        out.writeObject(mUser);
        //app name
        out.writeObject(mApp);
    }

    public boolean isLocked() {
        return busy;
    }

    private boolean syncDataAlreadyExists() throws SyncFailedException {
        Cursor c = mContentResolver.query(BASE_CONTENT_URI, null, "syncquery", null, null);
        if (c==null) {
            Log.d("syncDataAlreadyExists","cursor null");
            throw new SyncFailedException("Cursor null in syncDataAlreadyExists");
        }
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
            //The userUUID
            objOut.writeObject(mUUID);
            //Then app name
            objOut.writeObject(mApp);
            //The last known timestamp.
            objOut.writeObject(timestamp_from_team_to_me);
            //The current seq_no
            objOut.writeObject(seq_no);

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
                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_ME_TO_TEAM, potential_timestamp_from_me_to_team);
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
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,timestamp_from_team_to_me,
                                        Constants.TIMESTAMP_CURRENT_SEQUENCE_NUMBER,seq_no);
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
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,timestamp_from_team_to_me,
                                        Constants.TIMESTAMP_CURRENT_SEQUENCE_NUMBER,seq_no);
                                Log.d("vortex","All is synced.");

                            }
                            return msg;
                        } else {
                            Log.d("vortex","Insert into DB begins");
                            //keep lock
                            sessionShouldEnd = false;
                            insertIntoDatabase((List<ContentValues>) dataIn.data);
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

    private Object bundle(String timestampLabelFromTeamToMe, long timestamp_from_team_to_me,
                          String timestampCurrentSequenceNumber, int i) {
        Bundle b = new Bundle();
        b.putLong(timestampLabelFromTeamToMe,timestamp_from_team_to_me);
        b.putInt(timestampCurrentSequenceNumber,i);
        return b;
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

    private void releaseLock() {
        busy=false;
        Log.d("vortex","**SyncAdapter UN-Locked**");
    }

    private void lock() {
        Log.d("vortex","**SyncAdapter Locked**");
        busy=true;
    }


    public void safely_stored() {
        Log.d("vortex","Safely stored, release lock");
        if (potential_timestamp_from_team_to_me==-1)
            ERR_TEMP_TIMESTAMP_MISSING=true;
        else {
            seq_no++;
            timestamp_from_team_to_me = potential_timestamp_from_team_to_me;
            Log.d("vortex","TIMESTAMP TEAM->ME NOW "+timestamp_from_team_to_me+" seq_no: "+seq_no);


        }

        releaseLock();
    }

    public void userAbortedSync() {
        Log.d("vortex","USER_STOPPED now true");
        USER_STOPPED_SYNC = true;
    }
}