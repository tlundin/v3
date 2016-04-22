package com.teraim.fieldapp.dynamic.workflow_realizations;

/**
 * 
 * Copyright Teraim 2015.
 * Core class in the Vortex Engine.
 * Redistribution and changes only after agreement with Teraim.
 */



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.EventBroker;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

public class WF_Context {

	private Context ctx;
	private final List<WF_Static_List> lists=new ArrayList<WF_Static_List>();
	private final List<WF_Table> tables=new ArrayList<WF_Table>();
	private Map<String,Drawable> drawables;
	private List<WF_Container> containers;
	private final Executor myTemplate;
	private EventBroker eventBroker;
	private Set<Rule> rules=new HashSet<Rule>();
	private Set<Integer> executedBlocks = new HashSet<Integer>();
	private final int rootContainerId; 
	private String statusVariable=null;
	private List<Filterable> filterables;
	private WF_Gis_Map currentGis;
	private List<WF_Gis_Map> gisses;
	private boolean hasGPSTracker = false;
	private DB_Context myHash;
	private Workflow myWorkflow;
	private boolean hasSatNav;
	public int mapLayer=0;
	private List<String> contextVariables = null;
	private boolean myEndIsNear=false;


	public WF_Context(Context ctx,Executor e,int rootContainerId) {
		this.ctx=ctx;
		myTemplate = e;
		eventBroker = new EventBroker(ctx);
		this.rootContainerId=rootContainerId;
		this.drawables=new HashMap<String,Drawable>();
		this.filterables=new ArrayList<Filterable>();
		this.gisses=new ArrayList<WF_Gis_Map>();
	}
	public Context getContext() {
		return ctx;
	}
	
	public Workflow getWorkflow() {
		return myWorkflow;
	}
	
	public void setWorkflow(Workflow flow) {
		myWorkflow = flow;
	}

	public void addExecutedBlock(int blockId) {
		executedBlocks.add(blockId);
	}
	
	public void clearExecutedBlocks() {
		executedBlocks.clear();
	}
	
	public Activity getActivity() {
		return (Activity)ctx;
	}

	public List<WF_Static_List> getLists() {
		return lists;
	}
	
	public List<WF_Table> getTables() {
		return tables;
	}

	public  WF_Static_List getList(String id) {
		for (WF_Static_List wfl:lists) {
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl;
		}
		return null;
	}	
	
	public  WF_Table getTable(String id) {
		for (WF_Table wfl:tables) {
			Log.d("nils","In gettable: "+wfl.getId());
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl;
		}
		return null;
	}	

/*
	public List<Listable> getListable(String id) {
		for (WF_Static_List wfl:lists) {
			Log.d("nils","filterable list: "+wfl.getId());
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl.getList();
		}
		return null;
	}	
*/
	
	public Filterable getFilterable(String id) {
		Log.d("nils","Getfilterable called with id "+id);
		if (id==null)
			return null;
		for (Filterable wfl:filterables) {
			Log.d("nils","filterable: "+wfl.getId());
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl;
		}
		return null;
	}
	
	public void addContainers(List<WF_Container> containers) {
		this.containers = containers; 
	}
	//TODO: If required introduce nonfilterable lists and tables. For now it is assumed that all implements filterable. 
	public void addList(WF_Static_List l) {
		lists.add(l);
		filterables.add(l);
	}
	
	public void addTable(WF_Table t) {
		tables.add(t);
		filterables.add(t);
	}
	/*
	public void addFilterable(Filterable f) {
		filterables.add(f);
	}
	 */
	public void addDrawable(String key,Drawable d) {	
		drawables.put(key,d);
	}

	public Drawable getDrawable(String name) {
		return drawables.get(name);
	}


	public Container getContainer(String id) {
		if (containers == null)
			return null;
		//Log.d("nils","GetContainer. looking for container "+id);
		if (id==null || id.length()==0) {
			Log.e("nils","Container null!!");		
			return null;
		}
		for (WF_Container c:containers) {
			String myId =c.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return c;
		}
		Log.e("nils","Failed to find container "+id);
		return null;
	}

	public boolean hasContainers() {
		return containers!=null && !containers.isEmpty();
	}

	public void resetState() {
		emptyContainers();
		tables.clear();
		lists.clear();
		filterables.clear();
		drawables.clear();
		eventBroker.removeAllListeners();
		rules.clear();
		currentGis = null;
		gisses.clear();
		hasGPSTracker=false;
		contextVariables=null;
		myEndIsNear=false;
	}

	public void emptyContainers() {
		if (containers!=null)
			for (Container c:containers) 
				c.removeAll();
	}


	public void emptyContainer(Container c) {
		c.removeAll();
	}


	//draws all containers traversing the tree.
	public void drawRecursively(Container c) {
		if (c==null) {
			Log.e("nils","This container is null");
			return;
		}

		c.draw();
		List<Container> cs = getChildren(c);
		for(Container child:cs)
			drawRecursively(child);
		
	}

	private List<Container> getChildren(Container key) {
		List<Container>ret = new ArrayList<Container>();
		if (key!=null) {
			Container parent;
			for(Container c:containers) {
				parent = c.getParent();
				if (parent == null) {
					//Log.e("nils","Parent is null in getChildren");
					continue;
				}
				if (parent.equals(key))
					ret.add(c);
			}
		}
		return ret;
	}

	public Executor getTemplate() {
		return myTemplate;
	}

	public void addEventListener(EventListener el,
			EventType et) {

		eventBroker.registerEventListener(et, el);
	}
	
	public void removeEventListener(EventListener eventListener) {
		eventBroker.removeEventListener(eventListener);
	}

	public void onEvent(Event ev) {
		eventBroker.onEvent(ev);
	}
	public void registerEvent(Event event) {
		eventBroker.onEvent(event);
	}
	public int getRootContainer() {
		return rootContainerId;
	}
	
	/*
	public void onCreateView() {

		if (containers!=null) {
			resetState();
			containers.clear();
		}
	}
	*/

	public void addRule(Rule r) {
		rules.add(r);
	}

	/*public Set<Rule> getRules() {
		return rules;
	}
	*/
	public Set<Rule> getRulesThatApply() {
		Set<Rule> ret=new HashSet<Rule>();
		boolean add;
		for (Rule r:rules) {
			add=(r.getTarget()==-1)||(executedBlocks.contains(r.getTarget()));
			if (add)
				ret.add(r);
		}
		return ret;
	}

	public void setStatusVariable(String statusVar) {
		statusVariable = statusVar;
	}

	public String getStatusVariable() {
		return statusVariable;
	}
	public void addGis(String id, WF_Gis_Map wf_Gis_Map) {
		currentGis = wf_Gis_Map;
		gisses.add(wf_Gis_Map);
		mapLayer++;
	}
	
	public WF_Gis_Map getCurrentGis() {
		return currentGis;
	}
	
	
	public boolean hasGPSTracker() {
		return hasGPSTracker;
	}
	public void enableGPS() {
		hasGPSTracker = true;
	}
	public void setHash(DB_Context myHash) {
		this.myHash=myHash;
	}
	public DB_Context getHash() {
		return myHash;
	}
	public Map<String, String> getKeyHash() {
		if (myHash!=null)
			return myHash.getContext();
		else {
			Log.e("vortex", "myhash was null i getKeyHash, context!!");
			return null;
		}
	}
	
	public boolean hasSatNav() {
		return hasSatNav;
	}
	public void enableSatNav() {
		hasSatNav=true;
	}
	public void reload() {
		myTemplate.restart();
	}
	
	public void refreshGisObjects() {
		myTemplate.refreshGisObjects();
	}
	
	public void setContextVariables(List<String> contextVars) {
		contextVariables = contextVars;
	}
	
	public boolean isContextVariable(String cVar) {
		if (contextVariables == null)
			return false;
		return (contextVariables.contains(cVar));
	}
	public void setMyEndIsNear() {
		Log.e("Vortex","MY END IS NER IS NOW TRUEEE!!!!");
		myEndIsNear=true;
	}
	public boolean myEndIsNear() {
		return myEndIsNear;
	}
	
	


}
