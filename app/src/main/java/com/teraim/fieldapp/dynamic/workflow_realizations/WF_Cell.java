package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.View;

import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.Map;
import java.util.Set;

public interface WF_Cell {

	
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue);
	
	
	public boolean hasValue();
	
	public void refresh();

	public View getWidget();

	public Map<String,String> getKeyHash();

	public Set<Variable> getAssociatedVariables();
}
