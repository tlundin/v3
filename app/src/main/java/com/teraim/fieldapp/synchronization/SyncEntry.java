package com.teraim.fieldapp.synchronization;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class SyncEntry extends SyncMessage {

    public static Type action(String type) {
        Type mType;
        if (type.equals("I"))
            mType = Type.insert;
        else if (type.equals("D"))
            mType = Type.delete;
        else if (type.equals("M"))
            mType = Type.deleteMany;
        else if (type.equals("A"))
            mType = Type.insertArray;
        else {
            System.err.println("Unknown type of Sync action!: "+type);
            mType = Type.unknown;
        }
        return mType;
    }

    enum Type {
        insert,
        delete,
        deleteMany,
        unknown, insertArray
    }
    private static final long serialVersionUID = 862826293136691825L;
    private Type mType;
    private String changes;
    private String timeStamp;
    private String target;
    private Map<String,String> keys, values;
    private boolean invalid = false;
    public SyncEntry() {}

    public SyncEntry(Type type,String changes,String timeStamp,String target) {
        this.changes=changes;
        mType=type;
        this.timeStamp=timeStamp;
        this.target=target;
    }


    public Type getAction() {
        return mType;
    }

    public Map getKeys() {
        //if no keys, create.
        if (keys == null)
            generate();
        return keys;
    }
    public Map getValues() {
        //if no values, create.
        if (values == null)
            generate();
        return values;
    }

    //D: uid=63C31FDC-03A6-4221-B1E4-3BEB10BBE927|gistyp=Angsobete|trakt=991801|år=2018|var=TackningOvrigt_$_lag=teraim§author=terje1§value=100§timestamp=1526213565
    private void generate() {
        if (changes == null)
            return;
        String[] tmp = changes.split("_\\$_");
        if (tmp==null||tmp.length!=2)
            System.err.println("something wrong with syncentry with changes: ["+changes+"]");
        else {
            keys = collectPairs(tmp[0].split("\\|"));
            values = collectPairs(tmp[1].split("§"));
        };
    }



    private static Map collectPairs(String[] pairs) {
        Map result = new HashMap<String,String>();
        for (String pair : pairs) {
            String tmp[] = pair.split("=");
            result.put(tmp[0],tmp.length>1?tmp[1]:"");
        }
        return result;
    }

    public String getChange() {
        return changes;
    }

    public boolean isInsert() {
        return (mType==Type.insert);
    }

    public boolean isInsertArray() {
        return (mType==Type.insertArray);
    }

    public boolean isDelete() {
        return (mType==Type.delete);
    }

    public boolean isDeleteMany() {
        return (mType==Type.deleteMany);
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getTarget() {
        return target;
    }

}
