package com.teraim.fieldapp.synchronization.framework;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.ArrayList;

public class SyncContentProvider extends ContentProvider {

    private static final String TAG = "SynkDataProvider";
    public static final String AUTHORITY = "com.teraim.fieldapp.provider";


    @Override
    public boolean onCreate() {
        Log.d("mask","ON CREATE CALLED FOR SYNC CONTENT PROVIDER!!!");
        Log.d("mask","GLOBAL: "+GlobalState.getInstance());
        currentCount=0;
        return true;
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        GlobalState gs = GlobalState.getInstance();

        if (gs==null) {
            Log.e("vortex","Gs null...exit");
            return null;
        }
        PersistenceHelper globalPh = gs.getGlobalPreferences();
        final String bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
        final String teamName =  globalPh.get(PersistenceHelper.LAG_ID_KEY,"");
        if (bundleName == null || bundleName.length()==0) {
            Log.e("vortex","Bundlename was null in content provider!");
            return null;
        } else {
            PersistenceHelper localPh = gs.getPreferences();

            db = gs.getDb().db();
            Cursor c = null;
            if (db!=null && db.isOpen()) {

                if (selection != null && selection.equals("syncquery")) {

                    c = db.rawQuery("SELECT count(*) FROM " + DbHelper.TABLE_SYNC, null);

                } else {

                    //Timestamp key includes team name, since change of team name should lead to resync from zero.
                    Long timestamp = gs.getDb().getTimeStampFromMeToTeam(teamName);
                    Log.d("burlesk", "SYNCPROVIDER - Timestamp for last sync in Query is " + timestamp);
                    c = db.query(DbHelper.TABLE_AUDIT, null,
                            "timestamp > ?", new String[]{timestamp.toString()}, null, null, "timestamp asc", null);

                }
            } else {
                Log.e("vortex","Db null or closed in sync_contentprovider");
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
        GlobalState gs = GlobalState.getInstance();
        if (gs!=null) {
            db = gs.getDb().db();
            if (db!=null && db.isOpen()) {
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
            } else {
                Log.e("vortex","Db null or closed in syncccontentprovider");
            }
        } else {
            Log.e("vortex","globalstate was null in synccontentprovider");
        }
        return null;
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
        GlobalState gs = GlobalState.getInstance();
        if (gs!=null) {
            if (db != null && db.isOpen()) {
                db.insert(DbHelper.TABLE_SYNC, null, values);
                return uri;
            } else
                Log.e("vortex", "DB null or closed in adapter...insert fail");


        } else
            Log.e("vortex", "Globalstate null in adapter");
        return null;
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
