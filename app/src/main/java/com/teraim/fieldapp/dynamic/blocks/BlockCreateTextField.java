package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_SorterWidget;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TextBlockWidget;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class BlockCreateTextField extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1134485697631003990L;
	private final String background;
	String label,containerId;
	boolean isVisible = true;
	private List<EvalExpr> labelE;
	int horizontalMargin,verticalMargin,textSize;
	
	public BlockCreateTextField(String id, String label, String background, String containerId, boolean isVisible,String textSize,String horiz,String vert) {
		this.blockId=id;
		this.label=label;
		this.labelE=Expressor.preCompileExpression(label);
		this.containerId=containerId;
		this.isVisible = isVisible;
		this.background = background;
		try {
			horizontalMargin=Integer.parseInt(horiz);

		} catch (NumberFormatException e) {
			horizontalMargin=5;

		}
		try {
			verticalMargin=Integer.parseInt(vert);

		} catch (NumberFormatException e) {
			verticalMargin=5;

		}
		try {
			this.textSize=Integer.parseInt(textSize);
		} catch (NumberFormatException e) {
			this.textSize=-1;
		}
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance().getLogger();
		//Identify targetList. If no list, no game.
		Container myContainer = ctx.getContainer(containerId);
		if (myContainer != null)  {
			myContainer.add(new WF_TextBlockWidget(ctx,Expressor.analyze(labelE),background,blockId,isVisible,textSize,horizontalMargin, verticalMargin));
			o.addRow("Added new TextField with ID"+blockId);
		} else {
			o.addRow("");
			o.addRedText("Failed to add text field block with id "+blockId+" - missing container "+containerId);
		}
		
	}	
	
			
		
		
}


