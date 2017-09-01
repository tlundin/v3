package com.teraim.fieldapp.dynamic.blocks;

import java.util.Set;

import android.os.Handler;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;

public class AddVariableToEveryListEntryBlock extends Block {

	String target,variableSuffix,format;
	boolean displayOut,isVisible,showHistorical;
	private String initialValue=null;
	private static final long serialVersionUID = 3621078864866872867L;



	public AddVariableToEveryListEntryBlock(String id,String target,
			String variableSuffix, boolean displayOut, String format,boolean isVisible,boolean showHistorical,String initialValue) {
		super();

		this.target = target;
		this.variableSuffix = variableSuffix;
		this.displayOut = displayOut;
		this.format = format;
		this.isVisible=isVisible;
		this.blockId=id;
		this.initialValue = initialValue;
		this.showHistorical=showHistorical;
	}



	//addVariableToEveryListEntry(String varSuffix,boolean displayOut)
	//addVariable(String varLabel,Unit unit,String varId,boolean displayOut)

	public boolean create(WF_Context myContext) {

		final WF_Static_List l = myContext.getList(target);
		if (l==null) {
			o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addRedText("Couldn't find list with ID "+target+" in AddVariableToEveryListEntryBlock");
			return true;
		} else {
			Log.d("nils","Calling AddVariableToEveryListEntry for "+variableSuffix);
			return l.addVariableToEveryListEntry(variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);
		}

	}
}