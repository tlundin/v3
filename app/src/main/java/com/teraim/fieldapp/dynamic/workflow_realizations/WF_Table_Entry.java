package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.HashSet;
import java.util.Set;

import android.view.View;

import com.teraim.fieldapp.dynamic.types.Variable;

public class WF_Table_Entry extends WF_ListEntry {
	protected WF_Context myContext;	
	protected Variable myVar;

	public WF_Table_Entry(String id, View v, WF_Context ctx, boolean isVisible) {
		super(id, v, ctx, isVisible);
		this.myContext = ctx;
	}

	@Override
	public Set<Variable> getAssociatedVariables() {
		final Set<Variable> s = new HashSet<Variable>();
		s.add(myVar);
		return s;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

}
