package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.view.Gravity;
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
			final WF_Context context, String id,boolean isVisible) {
		super(id,new CheckBox(context.getContext()),isVisible,context);
		myCheckBox = (CheckBox)this.getWidget();
		myCheckBox.setGravity(Gravity.CENTER_VERTICAL);
		myHash = columnKeyHash;
		
		myCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (myVariable!=null) {
					if (isChecked)
						myVariable.setValue("true");
						else
						myVariable.deleteValue();

					context
							.registerEvent(new WF_Event_OnSave("table_cell", null,null));
				} 
			}
		});
	}

	@Override
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue) {	
		myVariable = GlobalState.getInstance().getVariableCache().getCheckedVariable(myHash, varId, prefetchValue, prefetchValue!=null);
		if (myVariable!=null) {
			String val = myVariable.getValue();
			myCheckBox.setChecked(val!=null && val.equals("true"));
		}
		
	}


	@Override
	public boolean hasValue() {
		return myVariable!=null && myVariable.getValue()!=null;
	}


	@Override
	public void refresh() {
		//this.getWidget().refreshDrawableState();
		//this.getWidget().requestLayout();
	}

	@Override
	public Map<String, String> getKeyHash() {
		return null;
	}

	@Override
	public Set<Variable> getAssociatedVariables() {
		if (myVariable!=null) {
			Set<Variable> ret = new HashSet<Variable>();
			ret.add(myVariable);
			return ret;
		}
		return null;

	}


}
