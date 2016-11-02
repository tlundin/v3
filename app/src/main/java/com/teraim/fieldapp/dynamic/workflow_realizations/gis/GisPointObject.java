package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.utils.Tools;


public abstract class GisPointObject extends GisObject {

	protected FullGisObjectConfiguration poc; 

	
	private int[] xy=new int[2];
	
	public GisPointObject(FullGisObjectConfiguration poc,Map<String, String> keyChain,List<Location> myCoordinates, String statusVar, String statusVal) {
		super(poc,keyChain,myCoordinates,statusVar,statusVal);
		this.poc=poc;
		
	}

	public abstract boolean isDynamic();
	public abstract boolean isUser();
	
	public Bitmap getIcon() {
		return poc.getIcon();
	}
	public float getRadius() {
		return poc.getRadius();
	}




	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		Location myLocation = this.getLocation();
		//Log.d("vortex",this.getLabel()+" touched by click? ");
		if (myLocation==null) {
		//	Log.d("vortex","No location found for object "+this.getLabel());
			return false;
		}
		if (this.getWorkflow()==null)
			return false;

		double xD = (mapLocationForClick.getX()-myLocation.getX())*pxr;
		double yD = (mapLocationForClick.getY()-myLocation.getY())*pyr;

		//double touchThresh = ClickThresholdInMeters;
		
		distanceToClick = Math.sqrt(xD*xD+yD*yD);
		/*
		if (isCircle())
			touchThresh = this.getRadius()/pxr;
		else {
			touchThresh = this.getRadius()/pyr;
		}
		 */
		//Log.d("vortex","pxr pyr"+pxr+","+pyr);
		//Log.d("vortex","I: D: "+this.getLabel()+","+distanceToClick);
		if (distanceToClick<ClickThresholdInMeters) {
			//Log.d("vortex","Touched by click...");
			return true;
		}
		//Log.d("vortex", "NO!: Dist x  y  tresh: "+xD+","+yD+","+distanceToClick+" thresh: "+ClickThresholdInMeters);
		return false;
	}
	public Style getStyle() {
		return poc.getStyle();
	}
	
	
	private Map<GisFilter,Boolean> filterCache = new HashMap<GisFilter,Boolean>();
	
	public boolean hasCachedFilterResult(GisFilter filter) {
		return filterCache.get(filter)!=null;
	}
	public void setCachedFilterResult(GisFilter filter, Boolean b) {
		filterCache.put(filter, b);
		
	}
	public boolean getCachedFilterResult(GisFilter filter) {
		Boolean b=filterCache.get(filter);
		
		return b;
	}

	public int[] getTranslatedLocation() {
		//Log.d("vortex","fetched gop object xy, x is "+xy[0]);
		return xy;
	}
	public void setTranslatedLocation(int[] xy) {
		//Log.d("vortex","set gop object xy");
		this.xy[0]=xy[0];
		this.xy[1]=xy[1];
	}

	@Override
	public void clearCache() {
		//Log.d("vortex","cleared gop object xy");
		xy=new int[2];};

}









