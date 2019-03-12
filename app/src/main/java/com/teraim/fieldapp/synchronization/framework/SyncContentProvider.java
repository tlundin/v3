package com.teraim.fieldapp.synchronization.framework;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
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

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static final int C_TIMESTAMPS = 1;
    public static final int C_SYNC_DATA = 2;


    public static UriMatcher buildUriMatcher() {

        /*
         * All paths added to the UriMatcher have a corresponding code to return when a match is
         * found. The code passed into the constructor of UriMatcher here represents the code to
         * return for the root URI. It's common to use NO_MATCH as the code for this case.
         */
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(AUTHORITY, DbHelper.TABLE_SYNC,C_TIMESTAMPS);
        matcher.addURI(AUTHORITY, DbHelper.TABLE_TIMESTAMPS,C_SYNC_DATA);

        return matcher;

    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        GlobalState gs = GlobalState.getInstance();
        long _id=-1;
        if (gs!=null) {
            final SQLiteDatabase db = gs.getDb().db();
            switch (sUriMatcher.match(uri)) {
                case C_TIMESTAMPS:
                    _id = db.insert(DbHelper.TABLE_TIMESTAMPS, null, values);
                    break;
                case C_SYNC_DATA:
                    _id = db.insert(DbHelper.TABLE_SYNC, null, values);
            }
        }
        else
            Log.e("vortex", "Globalstate null in adapter");

        return SyncAdapter.BASE_CONTENT_URI.buildUpon().appendPath(Long.toString(_id)).build();
    }



    @Override
    public boolean onCreate() {
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
