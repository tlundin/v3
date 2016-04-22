package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.utils.DbHelper.StoredVariableData;


public abstract class WF_ListEntry extends WF_Widget implements Listable,Comparable<Listable> {

	//String keyVariable=null;
	List<String> keyRow =null;
	String label = "";
	protected Variable myVar = null;
	
	public abstract void refresh();
//	public abstract void refreshInputFields();

	public WF_ListEntry(String id,View v,WF_Context ctx,boolean isVisible) {
		super(id,v,isVisible,ctx);
	}

	public void setKey(Variable var) {
			myVar = var;
			if (myVar!=null) {
				keyRow = myVar.getBackingDataSet();		
				//Log.d("nils","Calling setKeyRow for "+keyRow.toString());
				label = al.getEntryLabel(keyRow);
				Log.d("vortex","Keyvar label: "+label);
			}
	}

	@Override
	public String getSortableField(String columnId) {
		if (keyRow!=null && columnId!=null)
			return al.getTable().getElement(columnId, keyRow);
		else 
			return null;
	}

	@Override
	public String getKey() { 
		if (myVar == null)
			return null;
		else return myVar.getId();
	}

	
	@Override
	public boolean hasValue() {
		if (myVar == null)
			return false;
		else
			return myVar.getValue()!=null;
	}
	
	@Override
	public long getTimeStamp() {
		if (myVar == null)
			return -1;
		else {
			Long t = myVar.getTimeOfInsert();
			if (t == null)
				return -1;
			else 
				return t;		
		}
		
			
	}
		
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public int compareTo(Listable other) {
		return this.getLabel().compareTo(other.getLabel());
	}

	public Map<String,String> getKeyChain() {
		return myVar.getKeyChain();
	}
	
	 public static class Comparators {

	        public static Comparator<Listable> Alphabetic = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1,Listable o2) {
	                return o1.getLabel().compareTo(o2.getLabel());
	            }
	        };
	        public static Comparator<Listable> Time = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1, Listable o2) {
	                return (int)(o2.getTimeStamp() - o1.getTimeStamp());
	            }
	        };
	        
	        public static Comparator<Listable> Index = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1, Listable o2) {
	            	String i1 = o1.getKeyChain().get("index");
	            	String i2 = o2.getKeyChain().get("index");
	            	if (i1 == null || i2==null)
	            		return -1;
	                return (int)( Float.parseFloat(i2)-Float.parseFloat(i1));
	            	
	            }
	        };
	     
	   }
	

}

