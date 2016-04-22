package com.teraim.fieldapp.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.DbHelper.StoredVariableData;

public class CSVExporter extends Exporter {

	StringWriter sw;
	int varC=0;


	public CSVExporter(Context ctx) {
		super(ctx);
	}



	@Override
	public Report writeVariables(DBColumnPicker cp) {
		sw = new StringWriter();

		try {
			if (cp.moveToFirst()) {
				Map<String,String> currentKeys = cp.getKeyColumnValues();
				Log.d("nils","Current keys: "+currentKeys.toString());

				Log.d("vortex","Exporting csv");


				String header="";
				for (String key: currentKeys.keySet()) {
					header+=key+",";
				}
				header+="name,";
				header+="value,";
				header+="type,";
				header+="team,";
				header+="author,";
				header+="timestamp";


				Log.d("vortex",header);
				sw.write(header);
				sw.append(System.getProperty("line.separator"));
				String row,value,var;
				
				do {
					row = "";					
					currentKeys = cp.getKeyColumnValues();
					var = writeVariable(cp.getVariable());
					if (var.length()>0) {
						for (String key:currentKeys.keySet()) {
							value = currentKeys.get(key);
							if (value==null)
								value = "";
							row+=value+",";
						}
						row += var;

						Log.d("vortex",row);
						sw.write(row);
						sw.append(System.getProperty("line.separator"));
						varC++;
					}
				} while (cp.next());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Report(sw.toString(),varC);//new Report(result,noOfVars);
	}


	private String writeVariable(StoredVariableData variable) throws IOException {
		//Type found from extended data
		String ret = "";
		String type;
		List<String> row = al.getCompleteVariableDefinition(variable.name);
		boolean isExported = true;
		if (row==null)
			type ="";
		else {
			type = al.getnumType(row).name();
			isExported = !al.isLocal(row);
		}
		if (isExported) {
			ret = variable.name+","+variable.value+","+type+","+variable.lagId+","+variable.creator+","+variable.timeStamp;
		} else 
			Log.d("nils","Didn't export "+variable.name);

		return ret;
	}
	





	@Override
	public String getType() {
		return "csv";
	}
}
