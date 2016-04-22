package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

public  class RuleBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2045031005203874391L;
	private Rule r;
	private enum Scope {
		block,
		flow,
		both
	}
	private Scope myScope = Scope.flow;

	public RuleBlock(String id,String ruleName,String target, String condition, String action, String errorMsg, String myScope) {
		this.r = new Rule(id,ruleName,target,condition,action,errorMsg);
		this.blockId=id;
		if (myScope!=null && myScope.length()>0)
			try {
				this.myScope = Scope.valueOf(myScope);
			} catch (IllegalArgumentException e) { Log.e("vortex","Argument "+myScope+" not recognized. Defaults to scope flow");}
		
	}

	public Rule getRule() {
		return r;
	}

	public void create(WF_Context myContext, List<Block> blocks) {
		Log.d("nils","Create called in addRuleBlock. Target name: "+r.targetName);
		//Add rules that will be executed att flow exit.
		if (myScope == Scope.flow || myScope == Scope.both)
			myContext.addRule(r);
		//If target mentions specific block, find it and attach rule to EntryField.
		if (myScope!=Scope.flow && r.getTarget()!=-1) {
			 int index = findBlockIndex(r.targetName,blocks);
			 if (index==-1) {
				 o.addRow("");
				 o.addRedText("target block for rule "+blockId+" not found ("+r.getTarget()+")");
				 return;
			 } 
			 Block b = blocks.get(index);
			 if (b instanceof CreateEntryFieldBlock) {
				 Log.d("vortex","target ok");
				 ((CreateEntryFieldBlock)b).attachRule(r);
			 } else {
				 Log.e("vortex","target for rule with scope 'both' or 'block' can only be a createentryfield block");
				 o = GlobalState.getInstance().getLogger();
				 o.addRow("");
				 o.addRedText("target for rule "+blockId+" with scope 'both' or 'block' can only be a createentryfield block");
			 }
		}
		
	}
	
	private int findBlockIndex(String tid, List<Block> blocks) {
		if (tid==null)
			return -1;
		for(int i=0;i<blocks.size();i++) {
			String id = blocks.get(i).getBlockId();
			//			Log.d("nils","checking id: "+id);
			if(id.equals(tid)) {
				
				return i;
			}
		}

	
		return -1;
	}

}