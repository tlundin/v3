package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.View;

public interface WF_Cell {

	
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue);
	
	
	public boolean hasValue();
	
	public void refresh();
	
	public View getWidget();
}
