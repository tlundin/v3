package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

public class WF_OnlyWithValue_Filter extends WF_Filter implements Filter {

	public WF_OnlyWithValue_Filter(String id) {
		super(id);
		
	}

	@Override
	public List<? extends Listable> filter(List<? extends Listable> list) {
		Log.d("nils","In only_with_value filter with "+list.size()+" elements");
		Iterator<? extends Listable> it = list.iterator();
		while(it.hasNext()) {
			Listable l = it.next();
			if(!l.hasValue()) 
				it.remove();
				//Log.d("nils","filter removes element "+l.getKey()+" because its value is null");
			
		}
		Log.d("nils","Exit only_with_value filter with "+list.size()+" elements");
		return list;
	}

	

}

