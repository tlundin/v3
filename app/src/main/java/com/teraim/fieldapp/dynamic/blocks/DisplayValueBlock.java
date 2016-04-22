package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_DisplayValueField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.utils.Tools.Unit;

public class DisplayValueBlock extends Block implements EventGenerator {

	private static final long serialVersionUID = 9151756426062334462L;
	private String namn,label,formula, containerId,format;
	boolean isVisible = false;
	Unit unit;
	GlobalState gs;
	private String textColor;
	private String bgColor;
	
	public DisplayValueBlock(String id,String namn, String label,Unit unit,
			String formula, String containerId,boolean isVisible,String format, String textColor, String bgColor) {
		this.blockId=id;
		this.unit=unit;
		this.namn=namn;;
		this.label=label;
		this.formula=formula;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format=format;
		this.textColor = textColor;
		this.bgColor = bgColor;
	}

	public void create(final WF_Context myContext) {
		gs = GlobalState.getInstance();
		o=gs.getLogger();
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer != null) {
		
		WF_DisplayValueField vf = new WF_DisplayValueField(namn,formula,myContext,unit,label,isVisible,format, bgColor, textColor);
		myContainer.add(vf);
		vf.onEvent(new WF_Event_OnSave(null));
		}  else {
			o.addRow("");
			o.addRedText("Failed to add display value block with id "+blockId+" - missing container "+containerId);
		}
			
	}
}
