package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.Arrays;
import java.util.Map;

import com.teraim.fieldapp.dynamic.blocks.AddGisPointObjects;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.Variable;

public class StaticGisPoint extends GisPointObject {
	
	public StaticGisPoint(FullGisObjectConfiguration conf, Map<String, String> keyChain,Location myLocation, Variable statusVar) {
		super(conf,keyChain,Arrays.asList(new Location[]{myLocation}),statusVar);
	}



	@Override
	public Location getLocation() {
		if (myCoordinates.isEmpty())
			return null;
		return myCoordinates.get(0);
	}
	
	@Override
	public String toString() {
		String res="";
		res+=" \nDynamic: no";
		res+="\nLabel: "+this.getLabel();
		return res;
	}



	@Override
	public boolean isDynamic() {
		return false;
	}



	@Override
	public boolean isUser() {
		return false;
	}

}
