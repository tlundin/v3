package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Selection;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Selection_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Tools.Unit;

public class CreateEntryFieldBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	String name,type,label,containerId,postLabel,initialValue;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical,autoOpenSpinner=true;
	String format;
	
	WF_ClickableField_Selection myField;


	public CreateEntryFieldBlock(String id,String name, 
			String containerId,boolean isVisible,String format,boolean showHistorical,String initialValue, String label, boolean autoOpenSpinner) {
		super();
		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format = format;
		this.blockId=id;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;
		this.label=label;
		this.autoOpenSpinner=autoOpenSpinner;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}



	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}






	public Variable create(WF_Context myContext) {
		gs = GlobalState.getInstance();
		Container myContainer = myContext.getContainer(containerId);
		o = gs.getLogger();
		if(myContainer !=null) {
			VariableConfiguration al = gs.getVariableConfiguration();
			Log.d("vortex","current hash: "+gs.getVariableCache().getContext());
			Variable v = gs.getVariableCache().getVariable(name,initialValue,-1);
			if (v == null) {
				o.addRow("");
				o.addRedText("Failed to create entryfield for block "+blockId);
				Log.d("nils","Variable "+name+" referenced in block_create_entry_field not found.");

				o.addRedText("Current DB Context: ["+gs.getVariableCache().getContext()+"]");
			} else	{	
				myField = new WF_ClickableField_Selection_OnSave(label==null||label.equals("")?v.getLabel():label,
						al.getDescription(v.getBackingDataSet()),myContext,name,isVisible,autoOpenSpinner);
				Log.d("nils", "In CreateEntryField. Description: "+al.getDescription(v.getBackingDataSet()));
				Log.d("nils","Backing data: "+v.getBackingDataSet().toString());
				myField.addVariable(v, true,format,true,showHistorical);
				myContext.addDrawable(v.getId(), myField);

				Log.d("vortex","Adding Entryfield "+v.getId()+" to container "+containerId);
				o.addRow("Adding Entryfield "+v.getId()+" to container "+containerId);
				myContainer.add(myField);
				//				myField.refreshInputFields();	
				//myField.refresh();

			}

			return v;
		} else {
			Log.e("vortex","Container null! Cannot add entryfield!");
			o.addRow("");
			o.addRedText("Adding Entryfield for "+name+" failed. Container not configured");
			return null;
		}
	}

	public void attachRule(Rule r) {
		if (myField == null) {
			Log.e("vortex","no entryfield created. Rule block before entryfield block?");
		} else {
			myField.attachRule(r);
		}
	}




}


