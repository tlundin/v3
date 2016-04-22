/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

/**
 * @author tlundin
 *
 */

public class MenuEntryBlock extends Block {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6297016520560017438L;
	String target,type,label,bgColor,textColor;
	public MenuEntryBlock(String id, String target, String type, String bgColor, String textColor) {
		this.blockId=id;
		this.target=target;
		this.type=type;
		this.bgColor=bgColor;
		this.textColor=textColor;
	}
	public void create(WF_Context myContext) {
		Log.d("vortex","In create menuentry");
		
		GlobalState gs = GlobalState.getInstance();
		Workflow wf = gs.getWorkflow(target);
		if (wf == null)
			gs.getLogger().addRedText("Workflow "+target+" not found!!");
		else {
			label = wf.getLabel();		
			gs.getDrawerMenu().addItem(label,wf,bgColor,textColor);
		}
	}

}
