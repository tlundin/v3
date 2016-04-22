package com.teraim.fieldapp.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.MalformedJsonException;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
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

	public GisObjectConfiguration(PersistenceHelper globalPh,PersistenceHelper ph,Source source,String fileLocation, String fileName,LoggerI debugConsole,DbHelper myDb) {
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
		
	}

	private static String fixedLength(String fileName) {
		if ((20-fileName.length())<=0)
			return fileName;
		
		String space20 = new String(new char[20-fileName.length()]).replace('\0', ' ');

		return ("[" + fileName + "]"+space20);
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
			return new LoadResult(this,ErrorCode.Aborted,"Database is missing column 'år', cannot continue");
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			Log.d("vortex","found "+name);
			if (name.equals("features")) {
				Log.d("vortex","Found beginning of data");
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
			Map<String,String> attributes = new HashMap<String,String>();
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
			while(reader.hasNext()) {
				Log.d("vortex","skipping: "+this.getAttribute(reader));
			}
			reader.endArray();
			reader.endObject();
			//Properties
			reader.nextName();
			reader.beginObject();
			while(reader.hasNext()) {
				attributes.put(reader.nextName().toLowerCase(),this.getAttribute(reader));
			}
			Log.d("vortex",attributes.toString());
			//end attributes
			reader.endObject();
			//end row
			reader.endObject();
			String uuid = attributes.remove(GisConstants.GlobalID);
			String rutaId = attributes.remove(GisConstants.RutaID);


			if (uuid!=null)
				keyChain.put("uid",uuid);
			else
				keyChain.put("uid",UUID.randomUUID().toString());
			keyChain.put("år", Constants.HISTORICAL_TOKEN_IN_DATABASE);

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
	@Override
	public boolean freeze(int counter) throws IOException {
		if (firstCall) {
			myDb.beginTransaction();
			Log.d("vortex","Transaction begins");
			firstCall = false;
			Log.d("vortex","keyhash for first: "+myGisObjects.get(0).getKeyHash().toString());
		}

		//Insert GIS variables into database
		GisObject go = myGisObjects.get(counter);
		if (!myDb.fastHistoricalInsert(go.getKeyHash(),
				GisConstants.Location,go.coordsToString())) {
			o.addRow("");
			o.addRedText("Row: "+counter+". Insert failed for "+GisConstants.Location+". Hash: "+go.getKeyHash().toString());
		}
		Map<String, String> attr = go.getAttributes();

		for (String key:attr.keySet()) {
			String val = attr.get(key);
			if (!myDb.fastHistoricalInsert(go.getKeyHash(),key,val)) {
				o.addRow("");
				o.addRedText("Row: "+counter+". Insert failed for "+key+". Hash: "+go.getKeyHash().toString());;
			}
		}

		if (this.freezeSteps==(counter+1)) {
			Log.d("vortex","Transaction ends");
			myDb.endTransactionSuccess();
		}

		return true;

	}




}
