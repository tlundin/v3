package com.teraim.fieldapp.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;


import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.Exporter.Report;

public class GeoJSONExporter extends Exporter {

	private StringWriter sw;
	private JsonWriter writer;
	private String author ="";
	private List<String> coordLess = new ArrayList<>();
	private Map<String,String> rutMap = new HashMap<>();
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
				HashMap<String, Map<String, Map<String, String>>> gisObjects=null;
				

				Map<String, String> gisObjM;
				do {
					String uid=null,spy=null;
					currentHash = cp.getKeyColumnValues();
					if (currentHash==null) {
						o.addRow("");
						o.addRedText("Missing keyHash!");
						Log.e("vortex","Missing keyHash!");
						continue;
					}
					uid = currentHash.get("uid");
					rutMap.put(uid,currentHash.get("ruta"));
					spy = currentHash.get("spy");
					if (spy!=null) {
						Log.d("vortex","found spy!!");

					}
					Log.d("botox","CURRENT_HASH: "+currentHash);
					if (uid==null) {
						Log.e("vortex","missing uid!!!");
						Log.e("vortex","keyhash: "+currentHash);
					}
					else {
						if (gisObjects==null)
							gisObjects = new HashMap<String,Map<String,Map<String,String>>>();
						//Find maps per spy.
						Map<String, Map<String, String>> gisObjH = gisObjects.get(uid);
						if (gisObjH==null) {
							//No spys at all? Create new sub.
							gisObjH = new HashMap<String, Map<String, String>>();
							gisObjects.put(uid,gisObjH);
						}
						//Find correct "spy" for this uid. if spy doesnt exist, create.
						gisObjM = gisObjH.get(spy);
						if (gisObjM==null) { 
							gisObjM = new LinkedHashMap<String,String>();
							gisObjH.put(spy, gisObjM);
							if (spy == null)
								gisObjM.put("GISTYP", currentHash.get(GisConstants.TYPE_COLUMN));
							//Log.d("vortex","keyhash: "+currentHash.toString());
						}

						//Hack for multiple SPY1 variables.
						List<String> row;
						if (cp.getVariable()!=null) {
							String name = cp.getVariable().name;
							//Try to find in variable config.
							row = gs.getVariableConfiguration().getCompleteVariableDefinition(name);
							if (row!=null) {
								name =  gs.getVariableConfiguration().getVarName(row);
							}

							author  = cp.getVariable().creator;

							if (name!=null) {
								//Check if this variable is supposed to be exported.
								boolean isLocal = gs.getVariableConfiguration().isLocal(gs.getVariableConfiguration().getCompleteVariableDefinition(name));
								if (isLocal) {
									Log.d("vortex","skipping "+name+" since it is marked local");
									o.addRow("skipping "+name+" since it is marked local");

								} else {
									gisObjM.put(name, cp.getVariable().value);
									gisObjM.put("ts_" + name, cp.getVariable().timeStamp);
									varC++;
								}
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
				//For each fixedGid (uid)...
				if (gisObjects!=null) {
					for (String keyUID:gisObjects.keySet()) {
						//Log.d("vortex", "Spy sets under " + keyUID);
						Map<String, Map<String, String>> gisObjH = gisObjects.get(keyUID);

							//First do the default.
							gisObjM = gisObjH.get(null);
							String geoType = gisObjM.remove(GisConstants.Geo_Type);
							if (geoType == null)
								geoType = "Point";
							String coordinates = getCoordinates(keyUID, gisObjM.remove(GisConstants.GPS_Coord_Var_Name));

							if (coordinates == null) {
								//Try to find the object from historical. If not possible, sound alert.
/*
								Log.e("vortex", "Object " + keyUID + " is missing GPS Coordinates!!!");
								o.addRow("");
								o.addRedText("No GPS coordinates found for " + keyUID);
								for (String mKey : gisObjM.keySet()) {
									o.addRow("");
									o.addRedText("variable: " + mKey + ", value: " + gisObjM.get(mKey));
								}
*/
								coordLess.add(keyUID);
								coordinates = "0,0";
							}

							//Beg of line.
							writer.beginObject();
							write("type", "Feature");
							writer.name("geometry");
							writer.beginObject();
							write("type", geoType);
							writer.name("coordinates");

							String[] polygons = null;
							boolean isPoly = false;
							if (!geoType.equals("Polygon")) {
								//Log.d("volde", "POINT!!!");

								polygons = new String[]{coordinates};
							} else {
								isPoly = true;
								//Log.d("volde", "POLYGON!!!");
								polygons = coordinates.split("\\|");
								writer.beginArray();
							}
							for (String polygon : polygons) {

								String[] coords = polygon.split(",");

								if (isPoly)
									writer.beginArray();
								for (int i = 0; i < coords.length; i += 2) {
									//Log.d("vortex", "coord [" + i + "] :" + coords[i]);
									writer.beginArray();
									printCoord(writer, coords[i]);
									printCoord(writer, coords[i + 1]);
									writer.endArray();
								}
								if (isPoly)
									writer.endArray();
							}
							if (isPoly)
								writer.endArray();
							//End geometry.
							writer.endObject();
							writer.name("properties");
							writer.beginObject();
							//Add the UUID
							write(GisConstants.FixedGid, keyUID);
							String ruta = rutMap.get(keyUID);
							if (ruta!=null)
								write("RUTA",ruta);
							write("author", author);
							//write("timestamp",cp.getVariable().timeStamp);
							//write("author",cp.getKeyColumnValues().get("author"));
							for (String mKey : gisObjM.keySet()) {
								write(mKey, gisObjM.get(mKey));
								//Log.d("volde", "var, value: " + mKey + "," + gisObjM.get(mKey));
							}
							//Check if there are other spy than default.
						if (gisObjH != null && !gisObjH.isEmpty()) {
							writer.name("sub");
							writer.beginArray();
							for (String key : gisObjH.keySet()) {
								if (key == null) {
									//Log.d("volde", "skipping null key");
									continue;
								}
								//Log.d("volde", "found some extra under " + key);
								gisObjM = gisObjH.get(key);
								if (gisObjM != null) {
									writer.beginObject();
									write("SPY", key);
									for (String mKey : gisObjM.keySet()) {
										write(mKey, gisObjM.get(mKey));
										//Log.d("volde", "var, value: " + mKey + "," + gisObjM.get(mKey));
									}
									writer.endObject();
								}

							}
							writer.endArray();
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
				if (!coordLess.isEmpty()&&globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH)) {
					o.addRow("");
					o.addRedText("No coordinates found for these objects:");
					for (String keyUID:coordLess) {
						o.addCriticalText(keyUID);
					}
				}
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

	private String getCoordinates(String uid, String thisYear) {
		//If there is a coord for this year, return it.
		if (thisYear!=null)
			return thisYear;
		//otherwise, search historical for uid.
		return
				GlobalState.getInstance().getDb().findCoordinatesInHistorical(uid);
	}

	private void printCoord(JsonWriter writer, String coord) {
		try {
			if (coord == null || "null".equalsIgnoreCase(coord)) {
				Log.e("vortex", "coordinate was null in db. ");

				writer.nullValue();

			} else {
				try {
					writer.value(Float.parseFloat(coord));
				} catch (NumberFormatException e) {
					writer.nullValue();
				}
				;
			}
		}
			catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public String getType() {
		return "json";
	}

	private void write(String name,String value) throws IOException {
		String val = (value==null||value.length()==0)?"NULL":value;
		writer.name(name).value(val);
	}



}
