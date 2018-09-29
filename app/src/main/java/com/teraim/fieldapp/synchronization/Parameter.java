package com.teraim.fieldapp.synchronization;

import java.io.Serializable;

public class Parameter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String mkey;
    private final String mvalue;
	
	Parameter(String key, String value) {
		mkey  = key;
		mvalue = value;
	}
}

