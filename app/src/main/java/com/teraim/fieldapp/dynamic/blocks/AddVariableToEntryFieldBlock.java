package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Selection;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;


public class AddVariableToEntryFieldBlock extends Block {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7978000865030730562L;
	boolean displayOut,isVisible,showHistorical;
	String target,namn,format,initialValue;
	GlobalState gs;
	
	public AddVariableToEntryFieldBlock(String id,String target,String namn,boolean displayOut,String format,boolean isVisible,boolean showHistorical,String initialValue)  {
		this.blockId=id;
		this.target=target;
		this.namn=namn;
		this.displayOut=displayOut;
		this.format = format;
		this.isVisible=isVisible;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;

	}
	
	public Variable create(WF_Context myContext) {
		gs = GlobalState.getInstance();
		o = gs.getLogger();

		WF_ClickableField_Selection myField = (WF_ClickableField_Selection)myContext.getDrawable(target);
		if (myField == null) {
			o.addRow("");
			o.addRedText("Couldn't find Entry Field with name "+target+" in AddVariableToEntryBlock" );
			
		} else {
			Variable var =  gs.getVariableCache().getVariable(namn,initialValue,-1);
			if (var!=null) {
				myField.addVariable(var, displayOut, format,isVisible,showHistorical);
				return var;
			}
		}
		return null;
	}
}
