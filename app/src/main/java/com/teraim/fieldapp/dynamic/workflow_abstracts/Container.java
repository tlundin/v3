package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.List;

import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

public interface Container {	
	public Container getParent();		
	public void add(WF_Widget d);		
	public void remove(WF_Widget d);
	public Container getRoot();
	public List<WF_Widget> getWidgets();
	public void draw();
	public void removeAll();

}