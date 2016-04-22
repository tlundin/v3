package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sortable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

public abstract class WF_Static_List extends WF_List {


	protected final List<List<String>> myRows;
	//protected String group;
	//How about using the Container's panel?? TODO
	public WF_Static_List(String id, WF_Context ctx,List<List<String>> rows,boolean isVisible) {
		super(id,isVisible,ctx);	
		myRows = rows;
		//group = al.getFunctionalGroup(myRows.get(0));
		
	}

	public abstract Set<Variable> addVariableToEveryListEntry(String varSuffix,boolean displayOut,String format,boolean isVisible,boolean showHistorical,String initialValue);
	public abstract void addFieldListEntry(String listEntryID, 
			String label, String description);
	public abstract Variable addVariableToListEntry(String varNameSuffix,boolean displayOut,String targetField,
			String format, boolean isVisible,boolean showHistorical,String initialValue);



}
