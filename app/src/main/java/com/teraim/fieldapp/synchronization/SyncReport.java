package com.teraim.fieldapp.synchronization;

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
	public boolean hasChanges() {
		return (deletes+inserts+updates) > 0 ;
	}


}
