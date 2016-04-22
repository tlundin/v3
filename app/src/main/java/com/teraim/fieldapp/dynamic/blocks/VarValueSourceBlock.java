package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

public class VarValueSourceBlock extends Block {
	
	String id=null,filter=null;
	
	public VarValueSourceBlock(String id, String filter) {
		super();
		this.blockId = id;
		this.filter = filter;
	}

	public void create(WF_Context myContext) {
		// TODO Auto-generated method stub
		
	}

	
}
