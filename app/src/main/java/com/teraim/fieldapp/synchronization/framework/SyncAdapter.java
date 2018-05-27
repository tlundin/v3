package com.teraim.fieldapp.synchronization.framework;

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
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final Long LONG_TIME_AGO = 0L;
	// Global variables
	// Define a variable to contain a content resolver instance
	ContentResolver mContentResolver;
	SharedPreferences gh;
	private Messenger mClient;

	private final Uri CONTENT_URI = Uri.parse("content://"
			+ SyncContentProvider.AUTHORITY + "/synk");

	boolean busy = false,internetSync = false;
	String app=null, user=null, team=null;
	private SharedPreferences ph;
	/**
	 * Set up the sync adapter
	 *
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */

		mContentResolver = context.getContentResolver();
		
		
	}

	private boolean refreshUserAppAndTeam() {
		gh = getContext().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS,Context.MODE_MULTI_PROCESS);
		ph = getContext().getApplicationContext().getSharedPreferences(app, Context.MODE_MULTI_PROCESS);
		user = gh.getString(PersistenceHelper.USER_ID_KEY, null);
		team = gh.getString(PersistenceHelper.LAG_ID_KEY, null);
		internetSync = gh.getString(PersistenceHelper.SYNC_METHOD,"").equals("Internet");
		Log.d("vortex","REFRESHED: user: "+user+" team: "+team+" ! yay!");
		if (user == null || user.length()==0 ||
				team == null || team.length()==0 || 
				app==null || app.length() == 0) {
			Log.e("vortex","user id or group name or app name is null or zero length in Sync Adapter. Cannot sync: "+user+","+team+","+app);
			return false;
		}
		return true;
	}

	/**
	 * Set up the sync adapter. This form of the
	 * constructor maintains compatibility with Android 3.0
	 * and later platform versions
	 */


	public SyncAdapter(
			Context context,
			boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);

		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */

		mContentResolver = context.getContentResolver();

	}

	public void setClient(Messenger client) {
		gh = getContext().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS,Context.MODE_MULTI_PROCESS);
		app = gh.getString(PersistenceHelper.BUNDLE_NAME, null);
		Log.d("vortex","mClient SET..my app is "+app);
		mClient = client;
	}

	

	//Never sync more than 1000 at a time.
	final int MaxSyncableRows = 1000;
	private List<ContentValues> rowsToInsert=null;



	@Override
	public void onPerformSync(Account accountz, Bundle extrasz, String authorityz,
			ContentProviderClient providerz, SyncResult syncResultz) {

		int err = -1;

		Log.d("vortex", "************onPerformSync [" + user + "]");

		if (mClient == null) {
			Log.e("vortex", "Not ready so discarding call");
			return;
		}
		if (busy) {
			Log.e("vortex", "Busy so discarding call");
			err = SyncService.ERR_SYNC_BUSY;
			return;
		}
		//Check for any change of username or team name. 
		else if (!refreshUserAppAndTeam()) {
			if (!internetSync) {
				Log.e("vortex", "Not internet sync so discarding call");
				err = SyncService.ERR_NOT_INTERNET_SYNC;
			} else {
				Log.e("vortex", "Settings error prevents sync (missing user, team or appName");
				err = SyncService.ERR_SETTINGS;
			}
		}
		//If error, send it to UI thread.
		Message msg;
		if (err != -1) {
			msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
			msg.arg1 = err;

			try {
				mClient.send(msg);
			} catch (RemoteException e) {

				e.printStackTrace();
			}
			return;
		}
		//Check if there are unprocessed rows in sync table.
		if (syncDataAlreadyExists()) {

			//if so, set busy true and begin processing.
			busy=true;
			Log.d("vortex","sync data exists...");
			sendMessage(Message.obtain(null, SyncService.MSG_SYNC_RELEASE_DB));
			return;
		}

		//We are now running!!
		busy = true;
		Log.e("vortex", "BUSY NOW TRUE");


		//Get data entries to sync if any.

		Cursor c = mContentResolver.query(CONTENT_URI, null, null, null, MaxSyncableRows+"");
		SyncEntry[] sa =null;
		StringBuilder targets=new StringBuilder("");
		long maxStamp = -1;
		if (c!=null) {
			if (c.getCount()==0) {
				Log.d("vortex","Nothing to sync from ["+user+"] to ["+team+"]");

			} else {
				Log.d("vortex","Found "+c.getCount()+" rows to sync from ["+user+"] to ["+team+"]");

				String action,changes,variable;
				long entryStamp;
				int rowCount=0;

				//Either sync the number of lines returned, or MAX. Never more.

				int maxToSync = Math.min(c.getCount(),MaxSyncableRows);

				sa = new SyncEntry[maxToSync];

				while (c.moveToNext()&& rowCount < maxToSync) {
					action 		=	c.getString(c.getColumnIndex("action"));
					changes 	=	c.getString(c.getColumnIndex("changes"));
					entryStamp	=	c.getLong(c.getColumnIndex("timestamp"));
					variable 		= 	c.getString(c.getColumnIndex("target"));

					//long currentStamp = Long.parseLong(entryStamp);

					//Keep track of the highest timestamp in the set!
					if (entryStamp>maxStamp)
						maxStamp=entryStamp;

					sa[rowCount++] = new SyncEntry(SyncEntry.action(action),changes,entryStamp,variable,user);

					targets.append(variable);
					targets.append(",");
				}
			}
			c.close();
			//If succesful, update the counter.

			Log.d("bascar","SYNCING --> ["+targets+"]");
			Log.d("vortex","Maxstamp--> ["+maxStamp+"]");

			//Send and Receive.
			rowsToInsert = new ArrayList<ContentValues>();
			msg = sendAndReceive(rowsToInsert,team,user,app,sa,maxStamp);
			if (msg!=null)
				sendMessage(msg);
	
		} else {
			Log.e("vortex", "DATABASE CURSOR NULL IN SYNCADAPTER");
			busy=false;
		}


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
			}

	}

	public void insertIntoDatabase() {
		Message msg;
		Log.d("vortex", "inserting into db called");
		
		//Insert into database
		long d = System.currentTimeMillis();
		int i=0;
		
		ArrayList<ContentProviderOperation> list = new
	            ArrayList<ContentProviderOperation>();
		Iterator<ContentValues>it = rowsToInsert.iterator();
		while (it.hasNext()) {
			if (i%10==0) {
				Log.d("bascar", "inserting entry: " + i + "-"+(i+10));

				msg = Message.obtain(null, SyncService.MSG_SYNC_DATA_ARRIVING);
				msg.obj=bundle(i,rowsToInsert.size());

					sendMessage(
							msg
					);

			}
			i++;
			mContentResolver.insert(CONTENT_URI,it.next());
			//list.add(ContentPinsertroviderOperation.newInsert(CONTENT_URI).withValues(it.next()).build());
			//sendAwayBatch(list);
			//list.clear();

		}
		//if (!list.isEmpty())
		//	sendAwayBatch(list);

		sendMessage(Message.obtain(null, SyncService.MSG_SYNC_RELEASE_DB));
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

	public void updateCounters() {
		Log.d("vortex","In Sync UpdateCounters");
		//Here it is safe to update the timestamp for syncentries received from server.

		Long potentialStamp = ph.getLong(PersistenceHelper.PotentiallyTimeStampToUseIfInsertDoesNotFail+team,-1);

		if (potentialStamp!=-1L) {
			ph.edit().putLong(PersistenceHelper.TIME_OF_LAST_SYNC_FROM_TEAM +team,
					potentialStamp).apply();
			Log.d("vortex","LAST_SYNC TEAM --> ME: "+potentialStamp+". Difference to real time: "+(System.currentTimeMillis()-potentialStamp));
			Log.d("vortex","Entry: "+ph.getLong(PersistenceHelper.TIME_OF_LAST_SYNC_FROM_TEAM +team,22)+" Team: "+team);
		} else
			Log.e("vortex","potentialStamp was null in updatecounters!");
		busy = false;
		//update ui here.....


	}

	public static void forceSyncToHappen() {
		Log.d("vortex","trying to force sync");
		Bundle bundle = new Bundle();
		bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
		bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		ContentResolver.requestSync(null, SyncContentProvider.AUTHORITY, bundle);
	}


	private Message sendAndReceive(List<ContentValues> ret, String team,String user, String app, SyncEntry[] sa, long maxStamp) {
		URL url ;
		URLConnection conn=null;
		int orderNr = 0;
		assert(sa!=null);

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
			Log.d("vortex","creating outstream...");
			ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());

			//First syncgroup
			Log.d("vortex","writing group..."+team);
			objOut.writeObject(team);
			//Then user
			Log.d("vortex","writing user..."+user);
			objOut.writeObject(user);
			//Then app name
			Log.d("vortex","writing app..."+app);
			objOut.writeObject(app);
			//The last timestamp.
			Long trId=ph.getLong(PersistenceHelper.TIME_OF_LAST_SYNC_FROM_TEAM +team,LONG_TIME_AGO);
			Log.d("vortex","LAST_SYNC_FROM_TEAM_TO_ME WAS "+trId);
			objOut.writeObject(trId);

			if (sa!=null && sa.length>0) {
				Log.d("vortex","Writing SA[] Array.");
				objOut.writeObject(sa);				
			}
			else 
				Log.d("bascar","did not write any Sync Entries. SA[] Empty");
				

			//Done sending.
			objOut.writeObject(new EndOfStream());
			objOut.flush();

			Message msg = Message.obtain(null, SyncService.MSG_SYNC_STARTED);
			sendMessage(msg);
			//Receive.


			int rowCount=0;
			Object reply=null;
			Log.d("bascar","Waiting for Data ["+rowCount++ +"]");
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());

			reply = objIn.readObject();
			Log.d("bascar","After read object. reply is "+(reply!=null?"not":"")+" null");

			if (reply instanceof String ) {
				//server read my data succesfully. we can update the read pointer (timestamp) to avoid sending same data again.
				msg = Message.obtain(null, SyncService.MSG_SERVER_READ_MY_DATA);
				msg.obj=bundle(maxStamp);
				sendMessage(
						msg
				);

				int numberOfRows = Integer.parseInt((String) reply);
				Log.d("bascar","Number of Rows that will arrive: "+numberOfRows);
				//We now know that the SyncEntries from this user are safely stored. So advance the pointer! 
				if (maxStamp!=-1) {
					Log.d("vortex","LAST_SYNC ME --> TEAM: "+maxStamp);
					Log.d("vortex","Teamname: "+team+" App: "+app);
					//Each team + project has an associated TIME OF LAST SYNC pointer. 

				} else
					Log.d("vortex","Timestamp for Time Of Last Sync not changed for Internet sync.");

				int insertedRows=0;
				objOut.close();
				int i=0;
				while (true) {
					reply = objIn.readObject();
					if (reply instanceof Long) {
						Log.d("vortex","received timestamp for next cycle: "+reply);
						//This should be the Timestamp of the last entry arriving.
						ph.edit().putLong(PersistenceHelper.PotentiallyTimeStampToUseIfInsertDoesNotFail+team,(Long)reply).apply();
						Log.d("vortex","Inserted rows: "+insertedRows);
						objIn.close();
						objOut.close();
						if (insertedRows == 0 ) {
							Log.d("vortex","In sync with server!!!");
							//update counters - no insert required.
							updateCounters();
							//maxStamp is the latest message I have sent.
							return Message.obtain(null, SyncService.MSG_DEVICE_IN_SYNC);
						} else {
							Log.d("vortex","Insert into DB begins");
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
						ret.add(cv);
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
			Message msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
			msg.arg1=SyncService.ERR_SERVER_CONN_TIMEOUT;
			Log.e("vortex","ERR_SERVER_CONN_TIMEOUT. Reason: "+e.getMessage()+" Timestamp: "+System.currentTimeMillis());
			busy = false;
			return msg;
		}

		catch (IOException fe) {
			fe.printStackTrace();
			Message msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
			msg.arg1=SyncService.ERR_SERVER_NOT_REACHABLE;
			busy = false;
			return msg;
		}

		catch (Exception ex) {
			ex.printStackTrace();

		}
		busy = false;
		//Log.e("vortex","BUSY NOW FALSE E");
		Message msg = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
		msg.arg1=SyncService.ERR_UNKNOWN;
		return msg;

	}

	private Object bundle(int numberOfRowsSoFar, int numberOfRowsTotal) {
		Bundle b = new Bundle();
		b.putInt("number_of_rows_so_far",numberOfRowsSoFar);
		b.putInt("number_of_rows_total",numberOfRowsTotal);
		return b;
	}


	private Object bundle(long maxStamp) {
		Bundle b = new Bundle();
		b.putLong("maxstamp",maxStamp);
		return b;
	}

	public void releaseLock() {
		busy=false;
		//Log.e("vortex","BUSY NOW FALSE T");
	}



}