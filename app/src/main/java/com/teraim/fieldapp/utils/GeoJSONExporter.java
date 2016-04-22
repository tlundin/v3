package com.teraim.fieldapp.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;


import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.Exporter.Report;

public class GeoJSONExporter extends Exporter {

	private StringWriter sw;
	private JsonWriter writer;
	private String author ="";

	protected GeoJSONExporter(Context ctx) {
		super(ctx);

	}

	@Override
	public Report writeVariables(DBColumnPicker cp) {
		int varC=0;
		LoggerI o = GlobalState.getInstance().getLogger();
		sw = new StringWriter();
		writer = new JsonWriter(sw);	

		try {
			if (cp!=null && cp.moveToFirst()) {
				writer.setIndent("  ");
				//Begin main obj
				writer.beginObject();
				Log.d("nils","Writing header");
				write("name","Export");
				write("type","FeatureCollection");
				writer.name("crs");
				writer.beginObject();
				write("type","name");
				writer.name("properties");
				writer.beginObject();
				write("name","EPSG:3006");
				writer.endObject();
				//end header
				writer.endObject();
				writer.name("features");
				writer.beginArray();
				
				Map<String,String> currentHash=null;
				
				//gisobjects: A map between UID and variable key-value pairs.
				Map<String,Map<String,String>> gisObjects=null;
				
				String uid;
				Map<String, String> gisObjM;
				do {
					currentHash = cp.getKeyColumnValues();
					if (currentHash==null) {
						o.addRow("");
						o.addRedText("Missing keyHash!");
						Log.e("vortex","Missing keyHash!");
						continue;
					}
					uid = currentHash.get("uid");
					/*
					if (varC>0) {
						if (!Tools.sameKeys(previousHash,currentHash)) {
							Log.e("vortex","Diff!!!");
						Map<String, String> diff = Tools.findKeyDifferences(currentHash, previousHash);
						if (diff!=null) {
						//Find difference.
							Log.d("vortex","UID: "+uid+" DIFF: "+diff.toString());
						}
						}
					}
					 */
					if (uid==null) {

						Log.e("vortex","missing uid!!!");
						//Log.e("vortex","keyhash: "+currentHash.toString());
					}
					else {
						if (gisObjects==null)
							gisObjects = new HashMap<String,Map<String,String>>();
						gisObjM = gisObjects.get(uid);
						if (gisObjM==null) { 
							gisObjM = new HashMap<String,String>();
							gisObjects.put(uid, gisObjM);
							gisObjM.put("Gistyp", currentHash.get(GisConstants.TYPE_COLUMN));
							Log.d("vortex","keyhash: "+currentHash.toString());
						}
						//Hack for multiple SPY1 variables.
						if (cp.getVariable()!=null) {
							String name = cp.getVariable().name;
							author  = cp.getVariable().creator;
							if (name!=null) {
								gisObjM.put(name, cp.getVariable().value);
								varC++;
							}
							else {
								o.addRow("");
								o.addRedText("Variable name was null!");
							}
						} else {
							o.addRow("");
							o.addRedText("Variable was null!");
						}
					}
				} while (cp.next());
				Log.d("vortex","now inserting into json.");
				//For each gis object...
				if (gisObjects!=null) {
					for (String key:gisObjects.keySet()) {
						Log.d("vortex","variables under "+key);
						gisObjM = gisObjects.get(key);

						String geoType = gisObjM.remove(GisConstants.Geo_Type);
						if (geoType==null)
							geoType = "point";
						String coordinates = gisObjM.remove(GisConstants.GPS_Coord_Var_Name);
						if (coordinates==null) {
							Log.e("vortex","Object "+key+" is missing GPS Coordinates!!!");
							coordinates = "0,0";
						}

						//Beg of line.
						writer.beginObject();
						write("type", "Feature");
						writer.name("geometry");
						writer.beginObject();
						write("type",geoType);
						writer.name("coordinates");

						String[] polygons=null;
						boolean p = false;
						if (!geoType.equals("Polygon")) {
							Log.d("vortex","POINT!!!");
							Log.d("geotype",geoType);
							polygons = new String[] {coordinates};
						}
						else {
							p=true;
							Log.d("vortex","POLYGON!!!");
							polygons = coordinates.split("\\|");
							writer.beginArray();
						}
						for (String polygon:polygons) {
							if (p)
								writer.beginArray();
							String[] coords = polygon.split(",");
							writer.beginArray();
							for (int i =0;i<coords.length;i++) {
								Log.d("vortex","cord length: "+coords.length);
								Log.d("vortex","coord ["+i+"] :"+coords[i]);
								if (coords[i]==null || "null".equalsIgnoreCase(coords[i])) {
									Log.e("vortex","coordinate was null in db. ");
									writer.nullValue();
								} else {
									try {
									writer.value(Float.parseFloat(coords[i]));
									} catch (NumberFormatException e) { writer.nullValue();};
								}
							}
							writer.endArray();
							if (p)
								writer.endArray();
						}
						if (p)
							writer.endArray();
						//End geometry.
						writer.endObject();
						writer.name("properties");
						writer.beginObject();	
						//Add the UUID
						write("GlobalID",key);
						write("author",author);
						//write("author",cp.getKeyColumnValues().get("author"));
						for (String mKey:gisObjM.keySet()) {
							write(mKey,gisObjM.get(mKey));
							Log.d("vortex","var, value: "+mKey+","+gisObjM.get(mKey));
						}
						writer.endObject();

						//eol
						writer.endObject();
						
					}
				} else {
					o.addRow("");
					o.addRedText("GisObjects was null!");
					return new Report(ExportReport.NO_DATA);
				}
				//End of array.
				writer.endArray();
				//End of all.
				writer.endObject();

				Log.d("nils","finished writing JSON");
				Log.d("nils", sw.toString());
				return new Report(sw.toString(),varC);
			}else
				Log.e("vortex","EMPTY!!!");
		} catch (Exception e) {

			Tools.printErrorToLog(GlobalState.getInstance().getLogger(), e);

			cp.close();
		} finally {
			cp.close();
		}

		return null;	}

	@Override
	public String getType() {
		return "json";
	}

	private void write(String name,String value) throws IOException {
		String val = (value==null||value.length()==0)?"NULL":value;
		writer.name(name).value(val);
	}



}
