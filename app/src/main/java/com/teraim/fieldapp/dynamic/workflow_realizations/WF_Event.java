package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.List;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

public class WF_Event extends Event {

	EventType t;
	List<String> parameters;
	

	
	public WF_Event(EventType t, List<String> parameters,String sender) {
		super(sender,t);
		this.t=t;
		this.parameters=parameters;
	}
}
