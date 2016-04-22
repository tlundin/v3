package com.teraim.fieldapp.dynamic.workflow_abstracts;

public abstract class Event {

	public enum EventType {
		onSave,
		onClick,
		onRedraw,
		onBluetoothMessageReceived,
		onActivityResult, onContextChange, onFlowExecuted
	}
	
	private  String generatorId;
	private  EventType myType;
	public final static String EXTERNAL_SOURCE = "ext";

	public Event (String fromId, EventType et) {
		this.generatorId = fromId;
		myType = et;
	}
	public EventType getType() {
		return myType;
	}
	
	public String getProvider() {
		return generatorId;
	}
	
	
}
