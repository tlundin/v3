package com.teraim.fieldapp.synchronization;

import java.io.Serializable;
import java.util.List;

public class VariableRowEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 453476342824385369L;
	private final String var;
    private final String value;
    private final String lag;
    private final String timeStamp;
    private final String author;
	private final List<String> valueColumns;
	
	public VariableRowEntry(String var, String value, String lag, String timeStamp, String author, List<String> values) {
		this.value=value;
		this.var=var;
		this.lag=lag;
		this.timeStamp=timeStamp;
		this.author=author;
		this.valueColumns=values;
	}
	
	

}