package com.teraim.fieldapp.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.MalformedJsonException;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.loadermodule.JSONConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public class GisObjectConfiguration extends JSONConfigurationModule {

	private LoggerI o;
	private DbHelper myDb;
	private List<GisObject> myGisObjects = new ArrayList<GisObject>();
	private String myType;
	private boolean generatedUID = false;
	private Table varTable;

	public GisObjectConfiguration(PersistenceHelper globalPh, PersistenceHelper ph, Source source, String fileLocation, String fileName, LoggerI debugConsole, DbHelper myDb, Table t) {
		super(globalPh,ph, source,fileLocation, fileName, fixedLength(fileName));
		this.o = debugConsole;
		this.myDb = myDb;
		//isDatabaseModule=true;
		this.hasSimpleVersion=true;
		this.isDatabaseModule=true;
		this.myType = fileName;
		if (myType!=null&&myType.length()>0) {
			myType = myType.toLowerCase();
			myType =( myType.substring(0,1).toUpperCase() + myType.substring(1));
			Log.e("vortex","MYTYPE: "+myType);
		}
		varTable = t;
	}

	private static String fixedLength(String fileName) {
		if ((20-fileName.length())<=0)
			return fileName;
		
		String space20 = new String(new char[20-fileName.length()]).replace('\0', ' ');

		return (fileName+space20);
	}

	@Override
	public float getFrozenVersion() {
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName));

	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName,version);

	}

	@Override
	public boolean isRequired() {
		return false;
	}


	@Override
	protected LoadResult prepare(JsonReader reader) throws IOException, JSONException {
		//first should be an array.
		if (!myDb.deleteHistoryEntries(GisConstants.TYPE_COLUMN,myType))
			return new LoadResult(this,ErrorCode.Aborted,"Database is missing column 'ÅR', cannot continue");
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();

			if (name.equals("features")) {
				reader.beginArray();
				return null;
			} else
				reader.skipValue();
		}
		o.addRow("");
		o.addRedText("Could not find beginning of data (features) in input file");
		return new LoadResult(this,ErrorCode.IOError);
	}

	//Parses one row of data, then updates status.
	@Override
	public LoadResult parse(JsonReader reader) throws JSONException,IOException {
		Location myLocation;
		try {

		JsonToken tag = reader.peek();
		if (tag.equals(JsonToken.END_ARRAY)) {
			//end array means we are done.
			this.setEssence();
			reader.close();
			o.addRow("");
			o.addText("Found "+myGisObjects.size()+" objects");
			freezeSteps = myGisObjects.size();
			Log.d("vortex","Found "+myGisObjects.size()+" objekt");
			//freezeSteps=myBlocks.size();
			return new LoadResult(this,ErrorCode.parsed);
		}
		else if (tag.equals(JsonToken.BEGIN_OBJECT)) {
			reader.beginObject();
			//type
			reader.nextName();
			//Feature
			this.getAttribute(reader);	
			//Geometry
			reader.nextName();
			reader.beginObject();
			reader.nextName();
			String type = this.getAttribute(reader);
			//Log.d("vortex","This is a "+type);
			if (type==null) {
				o.addRow("");
				o.addRedText("Type field expected (point, polygon..., but got null");
				Log.e("vortex","type null!");
				return new LoadResult(this,ErrorCode.ParseError);
			}
			reader.nextName();
			reader.beginArray();
			double x,y,z;
			Map<String,String>keyChain = new HashMap<String,String>();
			Map<String,String> attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			String mType = type.trim();
			if (mType.equals(GisConstants.POINT)) {
				//Log.d("vortex","parsing point object.");
				//coordinates
				//Log.d("vortex","reading coords");
				x = reader.nextDouble();
				y = reader.nextDouble();
				myLocation = new SweLocation(x, y);
				myGisObjects.add(new GisObject(keyChain,Arrays.asList(new Location[] {myLocation}),attributes));
			} else if (mType.equals(GisConstants.MULTI_POINT)||(mType.equals(GisConstants.LINE_STRING))){
				List<Location> myCoordinates = new ArrayList<Location>();
				while (!reader.peek().equals(JsonToken.END_ARRAY)) {					
					reader.beginArray();
					x = reader.nextDouble();
					y = reader.nextDouble();
					myCoordinates.add(new SweLocation(x, y));
					reader.endArray();
				}
				myGisObjects.add(new GisObject(keyChain,myCoordinates,attributes));
			}  else if (mType.equals(GisConstants.POLYGON)){
				Map<String,List<Location>> holedPolygons = null;
				List<Location> myCoordinates=null; 
				int proxyId = 0;
				holedPolygons = new HashMap<String,List<Location>>();
				while (!reader.peek().equals(JsonToken.END_ARRAY)) {	
					reader.beginArray();
					myCoordinates = new ArrayList<Location>();
					while (!reader.peek().equals(JsonToken.END_ARRAY)) {
						reader.beginArray();
						x = reader.nextDouble();
						y = reader.nextDouble();
						if (!reader.peek().equals(JsonToken.END_ARRAY)) 
							z = reader.nextDouble();
						myCoordinates.add(new SweLocation(x, y));
						reader.endArray();
					}					
					holedPolygons.put((proxyId)+"" , myCoordinates);
					proxyId++;
					reader.endArray();
				}

				if (holedPolygons!=null&&!holedPolygons.isEmpty())
					myGisObjects.add(new GisPolygonObject(keyChain,holedPolygons,attributes));

			} else if (mType.equals(GisConstants.MULTI_POLYGON)){
				Log.d("vortex","MULTIPOLYGON!!");
				Set<GisPolygonObject> multiPoly 			= new HashSet<GisPolygonObject>();
				Map<String,List<Location>> 	 holedPolygons 	= new HashMap<String,List<Location>>();
				List<Location> myCoordinates=null; 
				int proxyId = 0;
				while (!reader.peek().equals(JsonToken.END_ARRAY)) {
					while (!reader.peek().equals(JsonToken.END_ARRAY)) {	
						reader.beginArray();
						myCoordinates = new ArrayList<Location>();
						while (!reader.peek().equals(JsonToken.END_ARRAY)) {
							reader.beginArray();
							x = reader.nextDouble();
							y = reader.nextDouble();
							if (!reader.peek().equals(JsonToken.END_ARRAY)) 
								z = reader.nextDouble();
							myCoordinates.add(new SweLocation(x, y));
							reader.endArray();
						}					
						holedPolygons.put((proxyId)+"" , myCoordinates);
						proxyId++;
						reader.endArray();
					} 
					if (holedPolygons!=null&&!holedPolygons.isEmpty())
						multiPoly.add(new GisPolygonObject(keyChain,holedPolygons,attributes));

				}
				if (holedPolygons!=null&&!holedPolygons.isEmpty())
					myGisObjects.add(new GisPolygonObject(keyChain,holedPolygons,attributes));
			} else {
				o.addRow("");
				o.addRedText("Unsupported Geo Type in parser: "+type);
				Log.e("vortex","type not supported! "+type);
				return new LoadResult(this,ErrorCode.ParseError);
			}
			int skipped=0;
			while(reader.hasNext()) {
				this.getAttribute(reader);
				skipped++;
			}
			if (skipped>0) {
				o.addRow("");
				o.addYellowText("Skipped "+skipped+" attributes in file "+getFileName());
			}

			reader.endArray();
			reader.endObject();
			//Properties
			reader.nextName();
			reader.beginObject();
			while(reader.hasNext()) {
				attributes.put(reader.nextName().toLowerCase(),this.getAttribute(reader));
			}
			//Log.d("vortex",attributes.toString());
			//end attributes
			reader.endObject();
			//end row
			reader.endObject();
			String uuid = attributes.remove(GisConstants.FixedGid);
			//Log.d("vortex","FixedGid: "+uuid);
			String rutaId = attributes.remove(GisConstants.RutaID);


			if (uuid!=null) {
				uuid = uuid.replace("{","").replace("}","");
				//Log.d("vortex","FixedGid: "+uuid);
				keyChain.put("uid", uuid);
			}
			else {
				generatedUID=true;
				keyChain.put("uid", UUID.randomUUID().toString());
			}
			//keyChain.put("år", Constants.HISTORICAL_TOKEN_IN_DATABASE);

			//Tarfala hack. TODO: Remove.
			if (rutaId==null)
				Log.e("vortex","ingen ruta ID!!!!");
			else
				keyChain.put("ruta", rutaId);

			keyChain.put(GisConstants.TYPE_COLUMN, myType);

			//Add geotype to attributes so that the correct object can be used at export.
			attributes.put(GisConstants.Geo_Type, mType);



			//Log.d("vortex","added new gis object");
			return null;
		} else {
			o.addRow("");
			o.addRedText("Parse error when parsing file "+fileName+". Expected Object type at line ");
			Log.e("vortex","Parse error when parsing file "+fileName+". Expected Object type at line ");
			return new LoadResult(this,ErrorCode.ParseError);
		}
		} catch (MalformedJsonException je) {
			Tools.printErrorToLog(o, je);
			throw(je);
		}
	}




	@Override
	public void setEssence() {
		essence = null;
	}



	boolean firstCall = true;
	Set<String> missingVariables=null;
	@Override
	public boolean freeze(int counter) throws IOException {

		boolean debug =false;
		if (firstCall) {
			missingVariables=new HashSet<String>();
			debug = globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH);
			if (generatedUID) {
				o.addRow("");
				o.addYellowText("At least one row in file "+fileName+" did not contain FixedGID (UUID). Generated value will be used");
			}
			myDb.beginTransaction();
			Log.d("vortex","Transaction begins");
			firstCall = false;
			Log.d("vortex","keyhash for first: "+myGisObjects.get(0).getKeyHash().toString());
		}

		//Insert GIS variables into database
		GisObject go = myGisObjects.get(counter);
		if (!myDb.fastHistoricalInsert(go.getKeyHash(),
				GisConstants.GPS_Coord_Var_Name,go.coordsToString())) {
			o.addRow("");
			o.addRedText("Row: "+counter+". Insert failed for "+GisConstants.GPS_Coord_Var_Name+". Hash: "+go.getKeyHash().toString());
		}
		Map<String, String> attr = go.getAttributes();

		for (String key:attr.keySet()) {
			String val = attr.get(key);
			if (!myDb.fastHistoricalInsert(go.getKeyHash(),key,val)) {
				o.addRow("");
				o.addRedText("Row: "+counter+". Insert failed for "+key+". Hash: "+go.getKeyHash().toString());;
			}
			if (debug) {
				if (varTable.getRowFromKey(key)==null) {
					missingVariables.add(key);
					Log.d("vortex","key missing: "+key);
				}
			}
		}

		if (this.freezeSteps==(counter+1)) {
			Log.d("vortex","Transaction ends");
			myDb.endTransactionSuccess();
			debug = globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH);
			if (debug && !missingVariables.isEmpty()) {
				o.addRow("");
				o.addRedText("VARIABLES MISSING IN VARIABLES CONFIGURATION FOR " + this.fileName + ":");

				for (String m : missingVariables) {
					o.addRow("");
					o.addRedText(m);
				}
			}
		}

		return true;

	}




}
