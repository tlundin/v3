package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.Map;

public class WF_Cell_Widget extends WF_ClickableField implements WF_Cell {


	private final Map<String, String> myHash;

	public WF_Cell_Widget(Map<String, String> columnKeyHash, String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.cell_field_normal,null),
				isVisible,new DisplayFieldBlock("black",null,null,null));

		myHash = columnKeyHash;
	}



	@Override
	public LinearLayout getFieldLayout() {
		//LayoutInflater.from(context.getContext()).inflate(R.layout.clickable_field_normal,null)
		//return 	(LinearLayout)LayoutInflater.from(ctx).inflate(R.layout.output_field,null);
		//o.setText(varId.getLabel()+": "+value);	
		//u.setText(" ("+varId.getPrintedUnit()+")");

		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_output_field_selection_element,null);
	}

	@Override
	protected boolean shouldHideOutputView() {
		return false;
	}

	int i=0;
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue) {	
		Variable var = GlobalState.getInstance().getVariableCache().getCheckedVariable(myHash, varId, prefetchValue, prefetchValue!=null);
		super.addVariable(var, displayOut, format, isVisible,showHistorical);
	}

	@Override
	public Map<String, String> getKeyHash() {

		return myHash;
	}


}
