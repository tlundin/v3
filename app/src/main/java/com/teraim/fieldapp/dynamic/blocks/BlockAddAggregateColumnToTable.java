package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.List;

public class BlockAddAggregateColumnToTable extends Block {


	/**
	 *
	 */
	private static final long serialVersionUID = -3041902713022605254L;
	private Expressor.EvalExpr expressionE=null;

	String target,  expression, aggregationFunction, aggreagationVariable, format,  width, label;
	String backgroundColor, textColor;
	boolean isDisplayed;

	public BlockAddAggregateColumnToTable(String id, String label, String target, String expression, String aggregationFunction,
										   String format, String width, String backgroundColor, String textColor,boolean isDisplayed) {
		super();
		this.blockId=id;
		this.target=target;
		this.expression=expression;
		this.aggregationFunction=aggregationFunction;
		this.aggreagationVariable=aggreagationVariable;
		this.format=format;
		this.width=width;
		this.isDisplayed=isDisplayed;
		this.label=label;
		List<Expressor.EvalExpr> tmp = Expressor.preCompileExpression(expression);
		if (tmp!=null) {
			this.expressionE = tmp.get(0);
			Log.d("vortex", "Bananas rule " + expression);
		}
		this.textColor=textColor;
		this.backgroundColor=backgroundColor;
	}


	public void create(WF_Context myContext) {
		WF_Table myTable = myContext.getTable(target);
		o = GlobalState.getInstance().getLogger();
		if (myTable==null) {
			Log.e("vortex","Did not find target table "+target+" in BlockAddAggregateColumnToTable, create");
			o.addRow("");
			o.addRedText("Did not find target table "+target+" when trying to add AggregateColumn with block ID: "+blockId+". Operation cancelled");
			return;
		}
		
		myTable.addAggregateColumn(label,expressionE,aggregationFunction,format,width,isDisplayed,backgroundColor, textColor);
	}

}
