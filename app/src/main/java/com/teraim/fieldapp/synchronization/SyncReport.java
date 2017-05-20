package com.teraim.fieldapp.synchronization;

import com.teraim.fieldapp.utils.DbHelper;

import java.io.Serializable;


public class SyncReport implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6763023289026007976L;
	public int deletes = 0;
	public int inserts = 0;
	public int faults = 0;
	public int refused = 0;
	public int updates=0;
	public int conflicts=0;
	public int totalRows=0;
	public int currentRow = 1;
	public int failedDeletes=0;
	public int replace=0;
	private TimeStampedMap tsMap = new TimeStampedMap();
	public int faultInValues=0;
	public int faultInKeys=0;
	public int insertsArray=0;

	public boolean hasChanges() {
		return (deletes+inserts+updates) > 0 ;
	}


	public TimeStampedMap getTimeStampedMap() {
		return tsMap;
	}
}
