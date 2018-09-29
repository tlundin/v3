package com.teraim.fieldapp.synchronization.framework;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.ArrayList;

public class SyncContentProvider extends ContentProvider {

	private static final String TAG = "SynkDataProvider";
	public static final String AUTHORITY = "com.teraim.fieldapp.provider";
	private SharedPreferences sp;
	private DatabaseHelper dbHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context, String databaseName) {

			super(context, databaseName, null, DbHelper.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.e("vortex","Not creating anything with this helper!!");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.e("vortex","Not upgrading anything with this helper!!");
		}


		Long getTimeStamp(String team) {
			Cursor c  = this.getReadableDatabase().rawQuery("select value from variabler where lag = ? AND var = ? ORDER BY id DESC LIMIT 1", new String[]{team,"timestamp_from_me_to_team"});
			if (c.getCount() != 0) {
				c.moveToFirst();
				return c.getLong(0);
			}
			return 0L;
		}

	}
	@Override
	public boolean onCreate() {
		Log.d("vortex","ON CREATE CALLED FOR SYNC CONTENT PROVIDER!!!");
		currentCount=0;
		return true;
	}
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Context ctx = getContext();

		SharedPreferences globalPh = ctx.getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS);
		final String bundleName = globalPh.getString(PersistenceHelper.BUNDLE_NAME,null);
		final String teamName =  globalPh.getString(PersistenceHelper.LAG_ID_KEY,"");
		if (bundleName == null || bundleName.length()==0) {
			Log.e("vortex","Bundlename was null in content provider!");
			return null;
		} else {
			sp = ctx.getApplicationContext().getSharedPreferences(bundleName, Context.MODE_MULTI_PROCESS);
			if (sp == null) {
				Log.e("vortex","persistencehelper null in onCreate");
				return null;
			} else
				if (dbHelper == null) 
					dbHelper = new DatabaseHelper(getContext(),bundleName);
			SQLiteDatabase db = dbHelper.getReadableDatabase();

			Cursor c = null;
			if(selection!=null && selection.equals("syncquery")) {
				c = db.rawQuery("SELECT count(*) FROM "+DbHelper.TABLE_SYNC, null);
			} else {
				//Timestamp key includes team name, since change of team name should lead to resync from zero.
				Long timestamp = sp.getLong(PersistenceHelper.TIMESTAMP_LAST_SYNC_FROM_ME + teamName,0);
				Long timestamp2 = dbHelper.getTimeStamp(teamName);
				if (timestamp != timestamp2)
					Log.e("antrax","PS: "+timestamp+", DB: "+timestamp2);
				timestamp = Math.max(timestamp,timestamp2);
				Log.d("biff", PersistenceHelper.TIMESTAMP_LAST_SYNC_FROM_ME+teamName);
				Log.d("burlesk", "SYNCPROVIDER - Timestamp for last sync in Query is " + timestamp);


				c = db.query(DbHelper.TABLE_AUDIT, null,
						"timestamp > ?", new String[]{timestamp.toString()}, null, null, "timestamp asc", null);

			}
			return c;
		}
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}


	@Override
	public ContentProviderResult[] applyBatch(
	        ArrayList<ContentProviderOperation> operations)
	            throws OperationApplicationException {
	        //System.out.println("starting transaction");
			 if (db== null)
				 db= dbHelper.getReadableDatabase();

	        db.beginTransaction();
	        ContentProviderResult[] result;
	        try {
	            result = super.applyBatch(operations);
	        } catch (OperationApplicationException e) {
	            System.out.println("aborting transaction");
	            db.endTransaction();
	            throw e;
	        }
	        db.setTransactionSuccessful();
	        db.endTransaction();
	        //System.out.println("ending transaction");
	        return result;
	    }

	private SQLiteDatabase db;
	private int currentCount=0;
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int count = values.getAsInteger("count");
		//long ct = System.currentTimeMillis()/1000;
		if (count!=currentCount) {
			Log.e("vortex", "current count differ from count. count: " + count + " curr "+currentCount);
			currentCount=count;
		}
		currentCount++;
		values.remove("count");
		//Log.d("vortex","In insert with values data = "+values.getAsByteArray("DATA"));
		if (db== null && dbHelper!=null)
			db= dbHelper.getReadableDatabase();

		if (db!=null)
			db.insert(DbHelper.TABLE_SYNC, null, values);
		else
			Log.e("vortex","DB null in adapter...insert fail");
		//Log.d("bascar","insert row done.");
		return uri;
	}







	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}







	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}




}
