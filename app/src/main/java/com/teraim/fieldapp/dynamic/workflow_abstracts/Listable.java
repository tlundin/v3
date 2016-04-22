package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.Map;
import java.util.Set;

import com.teraim.fieldapp.dynamic.types.Variable;

//Listable represents a row of data with columns. 
//TODO: Weaknesses : Cannot sort on value, only columns in Configuration Time data.
public interface Listable {
	public String getSortableField(String columnId);
	//Return the keychain for this listables key.
	public Map<String,String> getKeyChain();
	public String getKey();
	//TODO: Must separate into Comparable class or similar?
	public long getTimeStamp();
	public boolean hasValue();
	public String getLabel();
	public void refresh();

	Set<Variable> getAssociatedVariables();


}