package com.teraim.fieldapp.synchronization;

import java.io.Serializable;
import java.util.List;

public class VariableRowEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 453476342824385369L;

    public VariableRowEntry(String var, String value, String lag, String timeStamp, String author, List<String> values) {
        String value1 = value;
        String var1 = var;
        String lag1 = lag;
        String timeStamp1 = timeStamp;
        String author1 = author;
        List<String> valueColumns = values;
	}
	
	

}