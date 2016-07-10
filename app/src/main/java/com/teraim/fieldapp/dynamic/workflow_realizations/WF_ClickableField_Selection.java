package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Rule;

public class WF_ClickableField_Selection extends WF_ClickableField {


	public WF_ClickableField_Selection(String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible,String textColor,String backgroundColor) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.selection_field_normal,null),
				isVisible,textColor,backgroundColor);


	}

	@Override
	public LinearLayout getFieldLayout() {
		//LayoutInflater.from(context.getContext()).inflate(R.layout.clickable_field_normal,null)
		//return 	(LinearLayout)LayoutInflater.from(ctx).inflate(R.layout.output_field,null);
		//o.setText(varId.getLabel()+": "+value);	
		//u.setText(" ("+varId.getPrintedUnit()+")");

		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}








}
