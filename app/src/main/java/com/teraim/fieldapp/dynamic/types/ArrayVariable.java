package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
		timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
	}

	@Override
	public String getValue() {
		Log.d("baba","I get called");
		//long now = System.currentTimeMillis();
		String r = myDb.getValue(name,mySelection,myValueColumn);
		//Log.d("baba","T: "+Long.toString((System.currentTimeMillis()-now)));
		return r;
	}
}
