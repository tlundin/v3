package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import android.graphics.Path;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.Variable;

public abstract class GisPathObject extends GisObject {

	private List<Path> myPaths = null;
	public GisPathObject(FullGisObjectConfiguration conf,
			Map<String, String> keyChain, List<Location> myCoordinates,
			String statusVarName, String statusValue) {
		super(conf, keyChain, myCoordinates, statusVarName,statusValue);
		
	}

	
	public GisPathObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons,
			Map<String, String> attributes) {
		super(keyChain,null,attributes);
	}


	public List<Path> getPaths() {
		return myPaths;
	}
	public void addPath(Path p) {
		if (myPaths==null)
			myPaths = new ArrayList<>();
		myPaths.add(p);
	}

	@Override
	public void clearCache() {
		myPaths = null;
	}



}
