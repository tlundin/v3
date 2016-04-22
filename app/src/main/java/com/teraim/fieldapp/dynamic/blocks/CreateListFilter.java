package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_RegExp_Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter.FilterType;
import com.teraim.fieldapp.log.LoggerI;

public class CreateListFilter extends Block {

	private String target;
	private String type;
	private String selectionField;
	private String selectionPattern;

	public CreateListFilter(String id, String target, String type,
			String selectionField, String selectionPattern, LoggerI o) {
		this.blockId=id;
		this.target=target;
		this.type=type;
		this.selectionField = selectionField;
		this.selectionPattern=selectionPattern;
	}
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8707251750682677396L;

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		WF_Static_List myList = myContext.getList(target);
		if (myList==null) {
			o.addRow("");
			o.addRedText("Target list ["+target+"] for block_create_list_filter (blockId: "+blockId+") could not be found");
			return;
		}
		
		if (type ==null) {
			o.addRow("FilterType was not set in block_create_filter (blockId: "+blockId+"). Cannot execute block.");
			return;
		}
		
		if (type.equals("regular_expression"))
			myList.addFilter(new WF_Column_RegExp_Filter(blockId,selectionField,selectionPattern));
		else if (type.equals("exact"))
			myList.addFilter(new WF_Column_Name_Filter(blockId,selectionPattern,selectionField,FilterType.exact));
		else if (type.equals("prefix"))
			myList.addFilter(new WF_Column_Name_Filter(blockId,selectionPattern,selectionField,FilterType.prefix));
	}

}
