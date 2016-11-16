package com.teraim.fieldapp.synchronization;

import android.content.ContentValues;
import android.util.Log;

import com.teraim.fieldapp.utils.DbHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Terje on 2016-11-14.
 */
public class TimeStampedMap {

    //uid, mapped to variable id, mapped to timestamp.
    private Map<String,Map<String,ContentValues>> myMap = new HashMap();

    private int size = 0;

    public void add(String uniqueKey, String varName, ContentValues cv) {
        //Log.d("bang","uid: "+uniqueKey+" vn: "+varName);

        Map<String, ContentValues> ves = myMap.get(uniqueKey);

        if (ves==null) {
            ves = new HashMap<String, ContentValues>();
            myMap.put(uniqueKey,ves);
        }

        ContentValues existingCV= ves.get(varName);

        if (existingCV == null) {
            ves.put(varName, cv);
            size++;
        }
        else {
            //Log.d("zoobaz","Comparing!");
            try {
                String existingStamp = existingCV.getAsString("timestamp");
                String newTimeStamp = cv.getAsString("timestamp");
                if (existingStamp!=null && newTimeStamp!=null) {
                    int existing = Integer.parseInt(existingStamp);
                    int newTs = Integer.parseInt(newTimeStamp);
                    if (existing < newTs)
                        ves.put(varName, cv);
                    //Log.d("zoobaz","SWAP!");
                }

            } catch (NumberFormatException e) {

            };

        }

    }


    public ContentValues get(String uniqueKey, String varName) {
        Map<String, ContentValues> ves = myMap.get(uniqueKey);

        if (ves!=null && ves.get(varName)!=null) {
            Log.d("zoobaz","found variable "+varName);
            return ves.get(varName);
        }

        return null;
    }

    public Map<String, ContentValues> get(String uniqueKey) {
        if (myMap!=null)
            return myMap.get(uniqueKey);
        else
            return null;
    }

    public int size() {
        return size;
    }

    public boolean delete(String uniqueKey, String variableName) {
        if (uniqueKey==null || variableName==null)
            return true;
        Map<String, ContentValues> ves = myMap.get(uniqueKey);
        if (ves.get(variableName)!=null) {
            Log.d("zoobaz","Removing "+variableName);
            ves.remove(variableName);
            if (ves.isEmpty())
                myMap.remove(ves);
            return true;
        }

        return false;

    }


    public Set<String> getKeySet() {
        if (myMap!=null)
            return myMap.keySet();
        return null;

    }
}
