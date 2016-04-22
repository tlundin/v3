package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.utils.Geomatte;

public class GisMultiPointObject extends GisPathObject {

	private FullGisObjectConfiguration poc;
	
	public GisMultiPointObject(FullGisObjectConfiguration conf,Map<String, String> keyChain,List<Location> myCoordinates) {
		//TODO: Add statusvariable.
		super(conf,keyChain,myCoordinates,null);
		poc = conf;
	}
	
	public boolean isLineString() {
		return poc.getGisPolyType()==GisObjectType.Linestring;
	}

	
	//return the geometrical centroid of the first polygon. 
	@Override
	public Location getLocation() {
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.e("Vortex","Cannot calculate centroid for multipoint. No points found or obj contains no points!!");
			return null;
		}
		double centroidX=0,centroidY=0;
		for (Location l:myCoordinates) {
			centroidX+=l.getX();
			centroidY+=l.getY();
			
		}
		//TODO: Add LATLONG!!
		return new SweLocation(centroidX/myCoordinates.size(),centroidY/myCoordinates.size());
	}
	
	final static Location origo = new SweLocation(0,0);
	
	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		//Check if this object has something! A workflow. Otherwise no touch.
		if (this.getWorkflow()==null)
			return false;
		//Only linestrings can be touched.
		Log.d("vortex", "in istouch for linestr "+this.getLabel());
		if (!isLineString()) {
			Log.e("vortex", "this is no linestring...exiting.");
			return false;
		}
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.e("vortex", "found no coordinates...exiting");
			return false;
		}
		distanceToClick = ClickThresholdInMeters;
		for (int i=0;i<myCoordinates.size()-1;i++) {

			Location A = Geomatte.subtract(myCoordinates.get(i),mapLocationForClick);
			Location B = Geomatte.subtract(myCoordinates.get(i+1),mapLocationForClick);
			//double dist = Geomatte.pointToLineDistance(A, B, origo);
			double dist2 = Geomatte.pointToLineDistance3(A, B, origo,pxr,pyr);
			Log.d("vortex"," Distance N: "+dist2);
			if (dist2<distanceToClick) 
				distanceToClick=dist2;
			
		}
		if (distanceToClick<ClickThresholdInMeters) 
			return true;
		
			
		return false;
	}
	
	
}
