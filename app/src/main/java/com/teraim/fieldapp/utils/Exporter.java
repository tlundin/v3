package com.teraim.fieldapp.utils;

import android.content.Context;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.ExportDialog;
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
		Report(String res, int vars) {
			this.result=res;
			this.noOfVars=vars;
			er = ExportReport.OK;
		}
		public Report(ExportReport problem) {
			er = problem;
		}
		public String result;
		public int noOfVars = 0;
		final private ExportReport er;

		public ExportReport getReport() {
			return er;
		}

	}
	
	final GlobalState gs;
	final VariableConfiguration al;
	final PersistenceHelper ph;
	final PersistenceHelper globalPh;
	protected static Exporter instance;
	final ExportDialog eDialog;
	final Context ctx;

	public static Exporter getInstance(Context ctx, String type) {

		//Check clock
		if (Constants.FreeVersion) {
		long takenIntoUseTime = GlobalState.getInstance().getGlobalPreferences().getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		if (diff > Constants.MS_MONTH)
			return null;
		}
		//return new CSVExporter(ctx);

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
	Exporter(Context ctx) {
		this.gs=GlobalState.getInstance();
		al = gs.getVariableConfiguration();
		ph = gs.getPreferences();
		globalPh = gs.getGlobalPreferences();
		eDialog = new ExportDialog();
		this.ctx = ctx;
		
	}

	public abstract Report writeVariables(DBColumnPicker cp);
	public abstract String getType();
	public ExportDialog getDialog() { return eDialog;}
	public Context getContext() {return ctx;}
}
