package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.non_generics.NamedVariables;

import java.util.List;
import java.util.Map;

public class ArrayVariable extends Variable {

	public ArrayVariable(String name, String label, List<String> row,
			Map<String, String> keyChain, GlobalState gs, String valueColumn,
			String defaultOrExistingValue, Boolean valueIsPersisted, String historicalValue) {
		super(name, label, row, keyChain, gs, valueColumn, defaultOrExistingValue,
				valueIsPersisted,historicalValue);

	}

	private static final long serialVersionUID = 4404839378820201885L;

	@Override
	protected void insert(String value, boolean isSynchronized) {
		this.isSynchronizedNext= isSynchronized;
		myDb.insertVariableSnap(this,value, isSynchronized);
	}

	@Override
	public String getValue() {
		//long now = System.currentTimeMillis();
		GlobalState.MyGps gps = GlobalState.getMyGps();
		if (gps != null) {
			if (NamedVariables.MY_GPS_LAT.equals(this.name)) {
				Log.d("baba","X");
				return gps.x;
			} else if (NamedVariables.MY_GPS_LONG.equals(this.name)) {
				Log.d("baba","Y");
				return gps.y;
			} else if (NamedVariables.MY_GPS_ACCURACY.equals(this.name)) {
				Log.d("baba","A");
				return gps.a;
			}
		}
		return myDb.getValue(name, mySelection, myValueColumn);
		//Log.d("baba","T: "+Long.toString((System.currentTimeMillis()-now)));
	}
}
