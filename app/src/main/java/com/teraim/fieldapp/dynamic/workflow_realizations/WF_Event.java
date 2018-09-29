package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

import java.util.List;

class WF_Event extends Event {

	private final EventType t;
	private final List<String> parameters;
	

	
	public WF_Event(EventType t, List<String> parameters,String sender) {
		super(sender,t);
		this.t=t;
		this.parameters=parameters;
	}
}
