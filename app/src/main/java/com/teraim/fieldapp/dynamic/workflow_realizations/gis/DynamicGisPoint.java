package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.AddGisPointObjects;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;

public class DynamicGisPoint extends GisPointObject {
	
	boolean multivar = false;
	Variable myXVar,myYVar,myXYVar;
	
	public DynamicGisPoint(FullGisObjectConfiguration conf, Map<String, String> keyChain,Variable x, Variable y, Variable statusVar) {
		super(conf,keyChain,null,statusVar);
		Log.d("vortex","Creating dyna gis with variable x y "+x.getId()+","+y.getId());
		multivar=true;
		myXVar=x;
		myYVar=y;
	}
	public DynamicGisPoint(FullGisObjectConfiguration conf, Map<String, String> keyChain,Variable v1,Variable statusVar) {
		super(conf,keyChain,null,statusVar);
		Log.d("vortex","Creating dyna gis with variable "+v1.getLabel());
		multivar=false;
		myXYVar=v1;
	}
	@Override
	public Location getLocation() {
		if (multivar) {
			if (myXVar==null || myYVar == null) {
				Log.e("vortex","(At least) one variable missing in GisPObject: "+myXVar+" "+myYVar);
				return null;
			}
			String x = myXVar.getValue();
			String y = myYVar.getValue();
			if (x== null || y == null) {
				Log.d("vortex","(At least) one variable have no value in GisPObject: "+myXVar+" "+myYVar);
				return null;
			} else
				return new SweLocation(x,y);
		} else {
			if (myXYVar == null) {
				Log.e("vortex","Variable missing in GisPObject getLocation(!multivar). Other vars are: X,Y: "+myXVar+","+myYVar);
				return null;
			}
			String xy = myXYVar.getValue();
			if (xy==null) {
				Log.d("vortex","Variable has no value in GisPObject: "+myXYVar);
				return null;
			}
			String[] xys = xy.split(",");
			if (xys.length<2) {
				Log.e("vortex","Strange value for GPS coord variable: "+xy);
				return null;
			} else {
				Log.d("vortex","returning new XY for user!!!");
				return new SweLocation(xys[0],xys[1]);
			}
		}
	}
	
	@Override
	public String toString() {
		String res="";
		res+=" \nDynamic: yes";
		res+="\nMultivar: "+(multivar?"yes":"no");
		res+="\nLabel: "+poc.getRawLabel();
		res+="\nVariable values: xy, x, y";
			if (myXYVar==null)
				res+="null, ";
				else 
					res+=myXYVar.getValue()+", ";
			if (myXVar==null)
				res+="null, ";
			else
				res+=myXVar.getValue()+", ";
			if (myYVar ==null)
				res+="null, ";
			else
				res+=myYVar.getValue();
		return res;
	}
	@Override
	public boolean isDynamic() {
		return true;
	}
	
	public boolean isUser() {
		return poc.isUser();
	}
	
	//Dynamic points are always recalculated from geo location. So always return null for translated location!
	//@Override
	//public int[] getTranslatedLocation() {
	//	return xy;
	//}
	
}
