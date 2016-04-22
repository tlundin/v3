package com.teraim.fieldapp.dynamic.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.Path;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPathObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.gis.GisImageView;
import com.teraim.fieldapp.utils.PersistenceHelper;
/**
 * 
 * A Layer holds the GIS Objects drawn in GisImageView, created by block_add_gis_layer.
 * Each GIS Layer may hold reference to any GIS Object type. 
 * Each GIS Layer may be visible or hidden, controlled by user.
 * A GIS Layer may or may not have a Widget, controlled by XML Tag. 
 * Please see the XML Block definition for block_add_gis_layer
 */

public class GisLayer {

	private String name, label;
	private boolean hasWidget,hasDynamic=false;
	private Map<String,Set<GisObject>> myObjects;
	private boolean showLabels;
	private Map<String, Set<GisFilter>> myFilters;
	private boolean defaultVisible;
	


	public GisLayer(WF_Gis_Map myGis, String name, String label, boolean isVisible,
			boolean hasWidget, boolean showLabels) {
		super();
		this.name = name;
		this.label = label;
		this.hasWidget = hasWidget;
		this.showLabels=showLabels;
		//If no user set value exist, use default.
		//Else, set isvisible if stored value is 1.
		defaultVisible = isVisible;
		
	}

	//TODO: Potentially split incoming objects into two bags. one for static and one for changeable. 
	//This would speed up CRUD for map objects.

	public void addObjectBag(String key, Set<GisObject> myGisObjects, boolean dynamic, GisImageView gisView) {
		boolean merge =  false;
		if (myObjects==null) {
			myObjects = new HashMap<String,Set<GisObject>>();

		}
		
		Set<GisObject> existingBag = myObjects.get(key);
		//If no objects found we add an empty set provided none exist.
		if (myGisObjects == null && existingBag == null) {
			Log.d("vortex","Added empty set to layer: "+name+" of type "+key);
			myObjects.put(key, new HashSet<GisObject>());
		} else {
			//If bag already exists, we merge. If not, we create new.
			if (existingBag!=null) {
				merge = true;
				Log.d("Vortex","Merging bag of type "+key);
				//First mark if this is a merge.
				Iterator<GisObject> iterator = myGisObjects.iterator();
				int c=0;
				while (iterator.hasNext()) {
					GisObject go = iterator.next();
					//Mark this object if it is visible.
					markIfUseful(go,gisView);
					if (go.isDefect()) {
						Log.e("vortex","Removing DEFECT GIS OBJECT");
						iterator.remove();
					} 
					if (go.isUseful()) {
						c++;
					}
				}
				Log.d("vortex", "number of objects marked as useful: "+c);
				existingBag.addAll(myGisObjects);
			} else {
				Log.d("Vortex","Adding a new bag of type "+key);
				myObjects.put(key, myGisObjects);
			}
			Log.d("vortex","added "+myGisObjects.size()+" objects to layer: "+name+" of type "+key);
		}
		if (dynamic)
			this.hasDynamic = true;
		Set<GisObject> l = myObjects.get(key);
		if (merge)
			Log.d("vortex","CAPRIX Bag "+name+" now has "+l.size()+" members"+" bag obj: "+((Object)l.toString()));
	}

	public void addObjectFilter(String key, GisFilter f) {
		Set<GisFilter> setOfFilters = myFilters.get(key);
		if (setOfFilters==null) 
			setOfFilters = new HashSet<GisFilter>();

		setOfFilters.add(f);
		Log.d("vortex","added filter "+name+" of type "+key);
		myFilters.put(key, setOfFilters);

	}

	public Map<String,Set<GisObject>> getGisBags() {
		return myObjects;
	}
	public Set<GisObject> getBagOfType(String type) {
		if (myObjects !=  null )
			return myObjects.get(type);
		return null;
	}
	public Map<String,Set<GisFilter>> getFilters() {
		if (myFilters !=  null )
			return myFilters;
		return null;
	}

	public void setVisible(boolean isVisible) {

		Log.d("vortex","SetVisible called with "+isVisible+" on "+this.getId()+" Obj: "+this.toString());
		GlobalState.getInstance().getPreferences().put(PersistenceHelper.LAYER_VISIBILITY+name, isVisible?1:0);

	}

	/** Search for GisObject in all bags. 
	 * @param go   -- the object to look for.
	 * @return -- the first instance of the object if found. 
	 * */
	public Set<GisObject> getBagContainingGo(GisObject go) {
		if (myObjects == null)
			return null;
		for (String k:myObjects.keySet()) {
			Set<GisObject> gos = myObjects.get(k);
			for (GisObject g:gos) 
				if (go.equals(g))
					return gos;

		}
		return null;
	}

	public void setShowLabels(boolean show) {
		showLabels=show;
	}

	public boolean hasDynamic() {
		return hasDynamic;
	}

	public boolean isVisible() {
		int v = GlobalState.getInstance().getPreferences().getI(PersistenceHelper.LAYER_VISIBILITY+name);
		//Log.d("vortex","Layer "+name+" has visibility set to "+v+" default is "+defaultVisible);
		if (v==-1)
			return defaultVisible;
		else
			return v==1;

	}

	public boolean showLabels() {
		return showLabels;
	}

	public String getLabel() {
		return label;
	}

	public String getId() {
		return name;
	}

	public void clearCaches() {
		if (myObjects==null)
			return;
		for (String key:myObjects.keySet()) {
			Set<GisObject> bag = myObjects.get(key);
			for (GisObject go:bag) {
				go.clearCache();
				go.unmark();
			}
		}
	}


	public boolean hasWidget() {
		return hasWidget;
	}

	
	
	/**
	 * 
	 * @param layer
	 * 
	 * Will go through a layer and check if the gisobjects are inside the map. 
	 * If inside, the object is marked as useful.
	 * As a sideeffect, calcualte all the local coordinates.
	 */

	public void filterLayer(GisImageView gisImageView) {


		Map<String, Set<GisObject>> gops = getGisBags();
		if (gops == null) {
			Log.e("vortex","Layer "+ getLabel()+" has no bags. Exiting filterlayer");
			return;
		}
		Log.d("vortex","In filterAndCopy, layer has "+gops.size()+" bags");
		for (String key:gops.keySet()) {
			Set<GisObject> bag = gops.get(key);

			Iterator<GisObject> iterator = bag.iterator();
			

			while (iterator.hasNext()) {
				GisObject go = iterator.next();
				//Mark this object if it is visible.
				markIfUseful(go,gisImageView);
				if (go.isDefect())
					iterator.remove();
				

			}
			Log.d("vortex","Bag: "+key+" size: "+bag.size());
			int c=0;
			for (GisObject gob:bag) {
				if (gob.isUseful())
					c++;
			}
			Log.d("vortex","bag has "+c+" useful members");

		}
	}

	public void markIfUseful(GisObject go, GisImageView gisImageView) {
		int[] xy;
		//All dynamic objects are always in the map potentially.
		if (go instanceof DynamicGisPoint) {

			//Dynamic objects are always useful and do not have a cached value.
			go.markAsUseful();
		}
		else if (go instanceof GisPointObject) {
			GisPointObject gop = (GisPointObject)go;
			xy = new int[2];
			boolean inside = gisImageView.translateMapToRealCoordinates(gop.getLocation(),xy);
			if (inside) {
				go.markAsUseful();
				gop.setTranslatedLocation(xy);
			}
			//else 
			//	Log.d("vortex","Removed object outside map");
			return;
		} 
		else if (go instanceof GisPathObject) {
			GisPathObject gpo = (GisPathObject)go;
			boolean hasAtleastOneCornerInside = false;
			List<int[]> corners = new ArrayList<int[]>();
			if (go.getCoordinates()==null) {
				GlobalState.getInstance().getLogger().addRow("");
				GlobalState.getInstance().getLogger().addRedText("Gis object had *NULL* coordinates: "+go.getLabel());
				go.markForDestruction();
				return;
			}
			for (Location location:go.getCoordinates()) {
				xy = new int[2];
				if(gisImageView.translateMapToRealCoordinates(location,xy))
					hasAtleastOneCornerInside = true;
				corners.add(xy);
			}
			if (hasAtleastOneCornerInside) {
				go.markAsUseful();
				Path p = new Path();
				boolean first =true;
				for (int[] corner:corners) {
					if (first) {
						first=false;
						p.moveTo(corner[0], corner[1]);
					}
					else
						p.lineTo(corner[0], corner[1]);
				}
				if (go instanceof GisPolygonObject)
					p.close();
				gpo.setPath(p);
			}
		} 
		else
			Log.d("vortex","Gisobject "+go.getLabel()+" was not added");

	}








}
