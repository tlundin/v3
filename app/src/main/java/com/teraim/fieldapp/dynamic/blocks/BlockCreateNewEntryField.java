package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Selection_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Slider;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Tools.Unit;

public class BlockCreateNewEntryField extends Block {

	/**
	 *
	 */
	private static final long serialVersionUID = 2013870148670474248L;
	String name,type,label,containerId,initialValue,group;

	GlobalState gs;
	boolean isVisible = false,showHistorical,autoOpenSpinner=true;


	WF_ClickableField myField;
	String variableName=null;

	public BlockCreateNewEntryField(String id, String name,
									String containerId, boolean isVisible, boolean showHistorical, String initialValue, String label,String variableName,String group) {
		super();
		this.name = name;
		this.group = group;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;
		this.label=label;
		this.variableName = variableName;
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
			Variable v = gs.getVariableCache().getVariable(variableName,initialValue,-1);
			if (v == null) {
				o.addRow("");
				o.addRedText("Failed to create entryfield for block " + blockId);
				Log.d("nils", "Variable " + variableName + " referenced in block_create_entry_field not found.");
				o.addRow("");
				o.addRedText("Variable ["+variableName+"] referenced in block_create_entry_field not found.");
				o.addRow("");
				o.addRedText("Current context: ["+gs.getVariableCache().getContext()+"]");
			} else {
				Log.d("vortex", "current hash: " + gs.getVariableCache().getContext());
				myField = new WF_ClickableField_Slider(label, "This is a description for the entryfield"
						, myContext, name, isVisible);
				Log.d("nils", "In CreateNewEntryField.");
				myField.addVariable(v, true,"slider",true,showHistorical);
				myContext.addDrawable(v.getId(), myField);

				Log.d("vortex", "Adding Entryfield " + getName() + " to container " + containerId);
				o.addRow("Adding Entryfield " + getName() + " to container " + containerId);
				myContainer.add(myField);
				//				myField.refreshInputFields();	
				//myField.refresh();
				return v;
			}

		} else {
			Log.e("vortex","Container null! Cannot add entryfield!");
			o.addRow("");
			o.addRedText("Adding Entryfield for "+name+" failed. Container not configured");

		}
		return null;
	}

	public void attachRule(Rule r) {
		if (myField == null) {
			Log.e("vortex","no entryfield created. Rule block before entryfield block?");
		} else {
			myField.attachRule(r);
		}
	}




}


