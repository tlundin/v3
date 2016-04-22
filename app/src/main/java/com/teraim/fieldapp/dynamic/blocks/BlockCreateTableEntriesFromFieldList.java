package com.teraim.fieldapp.dynamic.blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;


public class BlockCreateTableEntriesFromFieldList extends Block {

	String namn=null, type=null,containerId=null,selectionField=null,selectionPattern=null;
	String labelField=null,descriptionField=null,typeField=null,uriField=null,variatorColumn=null;

	private static Map <String,List<List<String>>> cacheMap=new HashMap <String,List<List<String>>>();


	public BlockCreateTableEntriesFromFieldList(String id,String namn,String type, String containerId, 
			String selectionPattern, String selectionField,
			String variatorColumn,String descriptionField,String uriField,
			String labelField) {
		super();
		this.namn = namn;
		this.type = type;
		this.containerId = containerId;
		this.selectionField = selectionField;
		this.selectionPattern = selectionPattern;
		this.blockId = id;
		this.labelField = labelField;
		this.descriptionField = descriptionField;
		this.uriField = uriField;
		this.variatorColumn = variatorColumn;
	}

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		WF_Table myTable=null;

		Container myContainer = myContext.getContainer(containerId);
		if (myContainer !=null) {
			Log.d("vortex","in create for createlistentries "+blockId);

			VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			List<List<String>>rows = cacheMap==null?null:cacheMap.get(selectionField+selectionPattern);
			if (rows==null)
				rows  = al.getTable().getRowsContaining(selectionField, selectionPattern);
			if (rows==null||rows.size()==0) {
				Log.e("vortex","Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
				o.addRow("");
				o.addRedText("Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
			} else {		
				cacheMap.put(selectionField+selectionPattern, rows);
				Log.d("nils","Number of rows in CreateEntrieFromList "+rows.size());
				//prefetch values from db.
				LayoutInflater inflater = (LayoutInflater)myContext.getContext().getSystemService
						(Context.LAYOUT_INFLATER_SERVICE);
				View tableView = inflater.inflate(R.layout.table_view, null);
				
				if (type.equals("selective")) {
					Log.d("vortex","creating table.");
					myTable = new WF_Table(namn, true, myContext,selectionPattern,variatorColumn,tableView);
					myTable.addRows(rows);
				}
			}
			myContainer.add(myTable);
			myContext.addTable(myTable);		
		}
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = -1074961225196569424L;




}
