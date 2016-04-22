package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.Map;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;

public class WF_Simple_Cell_Widget extends WF_Widget implements WF_Cell {


	private Map<String, String> myHash;
	private CheckBox myCheckBox;
	private Variable myVariable = null;
	
	public WF_Simple_Cell_Widget(Map<String, String> columnKeyHash, String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible) {
		super(id,new CheckBox(context.getContext()),isVisible,context);
		myCheckBox = (CheckBox)this.getWidget();
		myHash = columnKeyHash;
		
		myCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (myVariable!=null) {
					if (isChecked)
						myVariable.setValue("1");
					else
						myVariable.deleteValue();
				} 
			}
		});
	}

	@Override
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue) {	
		myVariable = GlobalState.getInstance().getVariableCache().getCheckedVariable(myHash, varId, prefetchValue, prefetchValue!=null);
		if (myVariable!=null) {
			String val = myVariable.getValue();
			myCheckBox.setChecked(val!=null && Integer.parseInt(val)==1);
		}
		
	}


	@Override
	public boolean hasValue() {
		return myVariable!=null && myVariable.getValue()!=null;
	}


	@Override
	public void refresh() {
		//This does nothing.
	}
	
	
	

}
