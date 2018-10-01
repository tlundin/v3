package com.teraim.fieldapp.dynamic;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBroker {

	private final Map<EventType,List<EventListener>> eventListeners= new ConcurrentHashMap<EventType,List<EventListener>>();


    public EventBroker(Context ctx) {
        GlobalState gs = GlobalState.getInstance();
	}

	public void registerEventListener(EventType et,EventListener el) {

		List<EventListener> els = eventListeners.get(et);
		if (els==null) {
			els = new LinkedList<EventListener>();
			eventListeners.put(et, els);
		}
		if (els.contains(el))
			Log.d("vortex","registerEventListener discarded...listener already exist");
		else {
			els.add(el);
			//Log.d("nils","Added eventlistener for event "+et.name());
		}

	}

	public void onEvent(Event e) {
		List<EventListener> els = eventListeners.get(e.getType());
		if (els==null) {
			Log.d("nils","No eventlistener exists for event "+e.getType().name());
		} else {
			//Log.d("nils","sending event to "+els.size()+" listeners:");
		int i =0;
			for(EventListener el:els) {

				//Log.d("vortex","LISTENER NUMBER "+i+": "+el.getName());
				el.onEvent(e);
				i++;
			}
		}

		//TODO: ADD THIS LATER
		/*
		if (e instanceof WF_Event_OnSave && e.getProvider()!=Constants.SYNC_ID) {
			Log.d("nils","Save event...sending delayed sync request");
			new Handler().postDelayed(new Runnable() {
				public void run() {
					gs.triggerTransfer();
				}
			}, 2000);
		} 
		*/
	}

	public void removeAllListeners() {
		Log.d("nils","remove all listeneres called on EVENTBROKER");
		eventListeners.clear();
	}

	public void removeEventListener(EventListener eventListener) {
		eventListeners.remove(eventListener);
	}

}
