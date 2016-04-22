package com.teraim.fieldapp.synchronization;

import java.io.Serializable;
import java.util.List;

public class VariableRowEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 453476342824385369L;
	public String var,value,lag,timeStamp,author;
	public List<String> valueColumns;
	
	public VariableRowEntry(String var, String value, String lag, String timeStamp, String author, List<String> values) {
		this.value=value;
		this.var=var;
		this.lag=lag;
		this.timeStamp=timeStamp;
		this.author=author;
		this.valueColumns=values;
	}
	
	

}