package com.teraim.fieldapp.synchronization.framework;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
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
import java.util.Iterator;
import java.util.List;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Long LONG_TIME_AGO = 0L;

    // Global variables
    // Define a variable to contain a content resolver instance
    private final ContentResolver mContentResolver;
    private SharedPreferences gh;
    private Messenger mClient;

    private final Uri CONTENT_URI = Uri.parse("content://"
            + SyncContentProvider.AUTHORITY + "/synk");

    private boolean busy = true;
    private String mApp=null;
    private String mUser=null;
    private String mTeam=null;

    //Error codes.
    private boolean ERR_CONFIG,INIT_COMPLETE;

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

    }

    public void init(Messenger client, long timestamp_from_team_to_me, String team, String user, String app) {
        //get the team and user.
        ERR_CONFIG = true;
        mTeam = team;
        mUser = user;
        mApp = app;
        if (INIT_COMPLETE || client == null || user == null || user.length() == 0 || team == null || team.length() == 0 || app == null || app.length() == 0) {
            Log.e("vortex", "Init error in Sync Adapter." + user + "," + team + "," + app + "," + INIT_COMPLETE);

        } else {
            //update both potential and known timestamp at start
            this.timestamp_from_team_to_me = timestamp_from_team_to_me;
            potential_timestamp_from_team_to_me = timestamp_from_team_to_me;
            mClient = client;
            ERR_CONFIG = false;
            INIT_COMPLETE = true;
        }
    }

    public void resetOnResume() {
        INIT_COMPLETE=false;
    }

    private List<ContentValues> dataToInsert=null;



    @Override
    public void onPerformSync(Account accountz, Bundle extrasz, String authorityz,
                              ContentProviderClient providerz, SyncResult syncResultz) {

        Message msg;
        int err = -1;

        Log.d("vortex", "************onPerformSync [" + user + "]");


        if (mClient == null ) {
            Log.e("vortex", "Not ready so discarding call");
            return;
        }
        else if (busy) {
            Log.e("vortex", "Busy so discarding call");
            //err = SyncService.ERR_SYNC_BUSY;
            return;
        } else if (ERR_CONFIG) {
            Log.e("vortex", "Error in config - discarding call");
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SETTINGS;
            sendMessage(msg);
            return;
        }

        assert(timestamp_from_team_to_me != -1);


        //We are now running!!
        busy = true;
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
        int maxToSync=0;
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

        Log.d("brakko","exiting onsyncupdate");
    }

    private boolean syncDataAlreadyExists() {
        Cursor c = mContentResolver.query(CONTENT_URI, null, "syncquery", null, null);
        if (c.moveToFirst()) {
            int icount = c.getInt(0);
            if (icount > 0) {
                Log.d("vortex", "sync table has entries: " + icount);
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
        busy = false;
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
            Log.e("antrax","no client...NO MESSAGE SENT");

    }

    public void insertIntoDatabase() {
        Message msg;
        long d = System.currentTimeMillis();
        Log.d("vortex", "inserting into db called");
        if (dataToInsert !=null && dataToInsert.size()>0) {
            //Insert into database

            int i=0;

            Iterator<ContentValues>it = dataToInsert.iterator();
            while (it.hasNext()) {
                if (i % 10 == 0) {
                    Log.d("bascar", "inserting entry: " + i + "-" + (i + 10));

                    msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
                    msg.obj = bundle(i, dataToInsert.size());

                    sendMessage(
                            msg
                    );

                }
                i++;
                mContentResolver.insert(CONTENT_URI, it.next());
            }
        } else
            Log.e("vortex","No rows to insert in insertintoDB SyncAdapter");


        sendMessage(Message.obtain(null, SyncService.MSG_SYNC_DATA_READY_FOR_INSERT));
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
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(null, SyncContentProvider.AUTHORITY, bundle);
    }


    private Message sendAndReceive(SyncEntry[] dataToSend, long potential_timestamp_from_me_to_team) {
        Message msg;
        URL url ;
        URLConnection conn;
        int orderNr = 0;
        assert(dataToSend!=null);

        Log.d("bascar","In Send And Receive.");

        //Send a Start Sync message to the other side.

        //Send the SynkEntry[] array to server.
        try {
            url = new URL(Constants.SynkServerURI);
            conn = url.openConnection();

            conn.setConnectTimeout(10*1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
//			conn.addRequestProperty("type","TEAM_SYNK");

            // send object
            Log.d("brakko","creating outstream...");
            ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());

            //First syncgroup
            Log.d("brakko","writing group..."+mTeam);
            objOut.writeObject(mTeam);
            //Then user
            Log.d("brakko","writing user..."+mUser);
            objOut.writeObject(mUser);
            //Then app name
            Log.d("brakko","writing app..."+mApp);
            objOut.writeObject(mApp);
            //The last known timestamp.
            Log.d("brakko","LAST_SYNC_FROM_TEAM_TO_ME WAS "+timestamp_from_team_to_me);
            objOut.writeObject(new Long(timestamp_from_team_to_me));
            boolean IHadData=true;
            if (dataToSend.length>0) {
                Log.d("brakko","Writing SA[] Array.");
                objOut.writeObject(dataToSend);
            }
            else {
                Log.d("brakko", "did not write any Sync Entries. SA[] Empty");
                IHadData=false;
            }

            //Done sending.
            objOut.writeObject(new EndOfStream());
            objOut.flush();


            //Receive.
            Object reply=null;
            ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());

            reply = objIn.readObject();
            Log.d("brakko","After read object. reply is "+(reply!=null?"not":"")+" null");

            if (reply instanceof String ) {
                //server read my data succesfully. we can update the read pointer (timestamp) to avoid sending same data again.
                if (potential_timestamp_from_me_to_team > -1) {
                    Log.d("antrax","Sending timestamp of ME->TEAM: "+potential_timestamp_from_me_to_team);
                    msg = Message.obtain(null, SyncService.MSG_SERVER_READ_MY_DATA);
                    msg.obj = bundle(Constants.TIMESTAMP_LABEL_FROM_ME_TO_TEAM,new Long(potential_timestamp_from_me_to_team));
                    sendMessage(
                            msg
                    );

                }

                int numberOfRows = Integer.parseInt((String) reply);
                Log.d("brakko","Number of Rows that will arrive: "+numberOfRows);
                //We now know that the SyncEntries from this user are safely stored. So advance the pointer!
                if (potential_timestamp_from_me_to_team!=-1) {
                    Log.d("burlesk","ME --> TEAM: "+potential_timestamp_from_me_to_team);
                    Log.d("brakko","Teamname: "+mTeam+" App: "+mApp);
                    //Each team + project has an associated TIME OF LAST SYNC pointer.

                } else
                    Log.d("burlesk","Timestamp for Time Of Last Sync not changed for Internet sync.");

                int insertedRows=0;
                objOut.close();
                int i=0;
                dataToInsert = new ArrayList<>();

                while (true) {
                    reply = objIn.readObject();

                    if (reply instanceof Long) {

                        objIn.close();
                        objOut.close();
                        //Set a potential timestamp. Dont freeze until data read by client.
                        Log.d("vortex","TIMESTAMP FROM TEAM-->ME: "+reply);
                        potential_timestamp_from_team_to_me = (Long)reply;

                        Log.d("vortex","Inserted rows: "+insertedRows);

                        if (insertedRows == 0 ) {
                            Log.d("vortex","In sync with server. Nothing to update");
                            //update timestamp - immediately.
                            timestamp_from_team_to_me = potential_timestamp_from_team_to_me;
                            //check if data to send from me to team.
                            if (IHadData) {
                                Log.d("vortex","I had data...");
                                msg = Message.obtain(null, SyncService.MSG_NO_NEW_DATA_FROM_TEAM_TO_ME);
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,reply);
                                forceSyncToHappen();
                            } else {
                                msg = Message.obtain(null, SyncService.MSG_ALL_SYNCED);
                                msg.obj=bundle(Constants.TIMESTAMP_LABEL_FROM_TEAM_TO_ME,reply);
                                Log.d("vortex","All is synced.");
                            }
                            return msg;
                        } else {
                            Log.d("brakko","Insert into DB begins");
                            //keep the timestamp
                            return Message.obtain(null, SyncService.MSG_SYNC_REQUEST_DB_LOCK);
                        }
                    }

                    else if (reply instanceof SyncFailed) {
                        Log.e("vortex","SYNC FAILED. REASON: "+((SyncFailed)reply).getReason());
                        break;


                    }
                    else if (reply instanceof byte[]) {
                        ContentValues cv = new ContentValues();
                        cv.put("DATA", (byte[])reply);
                        cv.put("count",orderNr++);
                        Log.d("vortex","Count: "+cv.get("count"));
                        dataToInsert.add(cv);
                        insertedRows++;
                        if (insertedRows%10==0) {
                            msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
                            msg.obj=bundle(insertedRows,numberOfRows);

                            sendMessage(
                                    msg
                            );
                        }
                    }
                    else {
                        Log.e("vortex","Got back alien object!! "+reply.getClass().getSimpleName());
                        break;
                    }
                }
            }

            else {
                Log.e("vortex","OK not returned. instead "+reply.getClass().getCanonicalName());
                if (reply instanceof SyncFailed) {
                    Log.e("vortex",((SyncFailed)reply).getReason());
                }

            }

            objIn.close();
            objOut.close();

        } catch (StreamCorruptedException e) {
            Log.e("vortex","Stream corrupted. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());

        }
        catch (SocketTimeoutException e) {
            e.printStackTrace();
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_CONN_TIMEOUT;
            Log.e("vortex","ERR_SERVER_CONN_TIMEOUT. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
            busy = false;
            return msg;
        }

        catch (IOException fe) {
            fe.printStackTrace();
            msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            msg.arg1=SyncService.ERR_SERVER_NOT_REACHABLE;
            busy = false;
            return msg;
        }

        catch (Exception ex) {
            ex.printStackTrace();

        }
        releaseLock();
        //Log.e("vortex","BUSY NOW FALSE E");
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
        Log.e("vortex","BUSY NOW FALSE T");
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


}