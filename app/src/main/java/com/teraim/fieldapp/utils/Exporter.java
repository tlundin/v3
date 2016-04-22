package com.teraim.fieldapp.utils;

import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;


public abstract class Exporter {
	
	
	public enum ExportReport {
		OK,
		NO_DATA,
		FILE_WRITE_ERROR,
		COLUMN_DOES_NOT_EXIST,
		EXPORTFORMAT_UNKNOWN
	}
	public static class Report {
		public Report(String res,int vars) {
			this.result=res;
			this.noOfVars=vars;
		}
		public Report(ExportReport problem) {
			er = problem;
		}
		public String result;
		public int noOfVars = 0;
		public ExportReport er = ExportReport.OK;

	}
	
	protected GlobalState gs;
	protected VariableConfiguration al;
	protected PersistenceHelper ph;
	protected PersistenceHelper globalPh;
	protected static Exporter instance; 
	
	public static Exporter getInstance(Context ctx, String type) {

		//Check clock
		if (Constants.FreeVersion) {
		long takenIntoUseTime = GlobalState.getInstance().getGlobalPreferences().getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		if (diff > Constants.MS_MONTH)
			return null;
		}
		if (type==null||type.equalsIgnoreCase("csv"))
			return new CSVExporter(ctx);
		else
			if (type.equalsIgnoreCase("json"))
				return new JSONExporter(ctx);
			else
				if (type.equalsIgnoreCase("geojson"))
					return new GeoJSONExporter(ctx);
		return null;
	}
	protected Exporter(Context ctx) {
		this.gs=GlobalState.getInstance();
		al = gs.getVariableConfiguration();
		ph = gs.getPreferences();
		globalPh = gs.getGlobalPreferences();
		
	}
	public abstract Report writeVariables(DBColumnPicker cp);
	public abstract String getType();
	

}
