package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

//Specialized filter. Will filter a list on Prefix.
public class WF_Column_Name_Filter extends WF_Filter {

	String myPrefix = "";
	String filterColumn;
	private String columnToMatch;
	private FilterType filterType;
	private boolean totMatch=false;

	public enum FilterType{
		exact,
		prefix
	}

	public WF_Column_Name_Filter(String id,String filterCh,String columnToMatch,FilterType type) {
		super(id);
		myPrefix = filterCh;
		this.columnToMatch=columnToMatch;
		filterType = type;
	}
	
	@Override
	public List<? extends Listable> filter(List<? extends Listable> list) {
		String key;
		Iterator<? extends Listable> it = list.iterator();		
		while(it.hasNext()) {
			Listable l = it.next();
			key = l.getSortableField(columnToMatch);
			if (key==null) {
				Log.e("nils","Key was null in filter");
				continue;
			}				
			boolean match = false;
			
			if (filterType == FilterType.prefix) {
				if (key.isEmpty()) {
					match=false;					
				}
					
				for (int i=0;i<myPrefix.length();i++) {
					if (Character.toLowerCase(key.charAt(0))==Character.toLowerCase(myPrefix.charAt(i))) {
						match = true;
						break;					
					}
				}
			} else {
				if (filterType == FilterType.exact) {
					match = true;
					if (myPrefix.length()!=key.length()) {
						match = false;
					}
					else {
						for (int i=0;i<myPrefix.length();i++) {
							if (Character.toLowerCase(key.charAt(i))!=Character.toLowerCase(myPrefix.charAt(i))) {
								match = false;
								break;
							}
						}
					}
				}
			}

			if (!match) {
				it.remove();
				//Log.d("nils","filter removes element "+key+" because "+key.charAt(0)+" doesn't match "+myPrefix);
			}
			else 
				totMatch=true;

		}
		if (!totMatch) {
			o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addYellowText("No matches found in Column filter. Column used: ["+columnToMatch+"]");
		}
			
		return list;
	}


	


}