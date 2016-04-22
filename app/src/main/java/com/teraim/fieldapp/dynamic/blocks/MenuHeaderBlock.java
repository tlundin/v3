/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

/**
 * @author tlundin
 *
 */


public class MenuHeaderBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2542614941496953004L;
	String label,textColor, bgColor;

	public MenuHeaderBlock(String id, String label, String textColor,
			String bgColor) {
		this.blockId=id;
		this.label=label;
		this.textColor=textColor;
		this.bgColor=bgColor;
	}

	public void create(WF_Context myContext) {
		//Log.d("vortex","In create menuheader");
		
		GlobalState gs = GlobalState.getInstance();
		
		gs.getDrawerMenu().addHeader(label,bgColor,textColor);
		
		
	}

}
