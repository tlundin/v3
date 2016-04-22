package com.teraim.fieldapp.loadermodule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.util.MalformedJsonException;
import android.util.Xml;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.loadermodule.ConfigurationModule.Type;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Tools;

//Loader might have progressbar.

public abstract class Loader extends AsyncTask<ConfigurationModule ,Integer,LoadResult> {


	ProgressBar pb;
	TextView tv;
	FileLoadedCb cb;
	LoggerI myLog;
	String vNo ="";
	LoadResult loadR=null;
	//Values defined in @arrays file, 
	boolean versionControl;

	public Loader(ProgressBar pb, TextView tv,FileLoadedCb cb, String versionControlS) {
		this.pb=pb;
		this.tv=tv;
		this.cb=cb;
		this.versionControl=(versionControlS==null || !versionControlS.equals("No control"));
	}


	public static float getVersion(String h1,String h2) {


		String[] header = h1.split(",");

		if (header!=null&&header.length>=2) {
			String potVer = header[1].trim(); 
			if (Tools.isVersionNumber(potVer))
				return Float.parseFloat(potVer);
		}
		if (h2!=null) {
			int p = h2.indexOf("version");
			if (p>0) {
				String vNo = h2.substring(p+9, h2.indexOf('\"', p+9));
				Log.d("vortex","Version line: "+vNo);
				if (Tools.isVersionNumber(vNo))
					return Float.parseFloat(vNo);
			}
		}
		Log.d("vortex","No version found for simple lookup.");
		Log.d("vortex","Header row1: "+h1);
		Log.d("vortex","Header row2: "+h2);
		return -1;

	}

	/* Reads input files with historical data and inserts into database. */
	@Override
	protected void onPostExecute(LoadResult res) {
		cb.onFileLoaded(res);
	}


	@Override
	protected void onProgressUpdate(Integer... values) {
		//tv.setText("Loaded: "+values[0]+"/"+values[1]);
		//pb.setProgress(values[0]);
		cb.onUpdate(values);
	}

	int rowC = 1;
	protected LoadResult read(ConfigurationModule m, float newVersion, BufferedReader reader,StringBuilder sb) throws IOException {
		String line = null;
		if (newVersion != -1) {
			m.setNewVersion(newVersion);
			if (versionControl) {
				Log.d("vortex","newversion equals: "+newVersion);
				float frozenVersion = m.getFrozenVersion();
				if (frozenVersion!=-1) {
					if (frozenVersion==newVersion) {
						//We can set the version number safely.
						Log.d("vortex","frozenversion equals new: "+frozenVersion);
						return new LoadResult(m,ErrorCode.sameold);
					} else
					if (frozenVersion > newVersion)
						return new LoadResult(m,ErrorCode.existingVersionIsMoreCurrent,newVersion+"");

				}
			}
		}
		while ((line = reader.readLine()) != null)
		{
			sb.append(line + "\n");
			if(isCancelled()) {
				reader.close();
				return new LoadResult(m,ErrorCode.Aborted);
			}
			if ((rowC++%20)==0) 
				this.publishProgress(0,rowC);
		}
		m.setRawData(sb.toString(), rowC);
		return new LoadResult(m,ErrorCode.loaded);
	}

	protected LoadResult parse(ConfigurationModule m) throws IOException, XmlPullParserException, JSONException, Dependant_Configuration_Missing {
		if (m.type==Type.csv)
			return parseCSV((CSVConfigurationModule)m);
		else if (m.type==Type.json)
			return parseJSON((JSONConfigurationModule)m);
		else
			return parseXML((XMLConfigurationModule)m);
	}

	protected LoadResult parseCSV(CSVConfigurationModule m) throws IOException, Dependant_Configuration_Missing {
		String[] myRows=null;
		int noOfRows=rowC;
		LoadResult res = null;
		rowC=1;
		LoadResult lr = m.prepare();
		//exit if prepare returns.
		if (lr!=null)
			return lr;

		if (m.rawData!=null) 
			myRows = m.rawData.split("\\n");
		else
			return new LoadResult(m,ErrorCode.noData);
		for (String row:myRows) {
			loadR = m.parse(row,rowC);
			if (loadR!=null) {
				res = loadR;
				break;
			}
			if ((rowC++%20)==0) 
				this.publishProgress(rowC,noOfRows);

		}
		if (res==null)
			res = new LoadResult(m,ErrorCode.parsed);
		this.publishProgress(rowC,noOfRows);
		return res;
	}


	protected LoadResult parseXML(XMLConfigurationModule m) throws XmlPullParserException, IOException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(new StringReader(m.rawData));
		LoadResult lr = m.prepare(parser);
		//exit if prepare returns.
		if (lr!=null)
			return lr;
		rowC=0;
		while((lr=m.parse(parser))==null) {
			if ((rowC++%20)==0) 
				this.publishProgress(rowC);
		}
		return lr;
	}


	protected LoadResult parseJSON(JSONConfigurationModule m) throws IOException, JSONException {
		JsonReader parser = new JsonReader(new StringReader(m.rawData));
		LoadResult lr = m.prepare(parser);
		if (lr!=null)
			return lr;
		rowC=0;

		while((lr=m.parse(parser))==null) {
			if ((rowC++%20)==0) 
				this.publishProgress(rowC);				

		}

		return lr;

	}



	protected LoadResult freeze(ConfigurationModule m) throws IOException {

		//Multiple steps or only one to freeze?
		if (m.freezeSteps>0) {
			rowC=0;
			while (rowC<m.freezeSteps) {
				m.freeze(rowC);
				if ((rowC++%100)==0)
					this.publishProgress(rowC,m.freezeSteps);

			}
		} else 
			m.freeze(-1);


		if (m.newVersion!=-1) {
			m.setFrozenVersion(m.newVersion);
			Log.d("vortex","Frozen version set to "+m.newVersion);
		} else
			Log.d("vortex","NewVersion number was -1 in setfrozenversion");

		if (m.getEssence()!=null||m.isDatabaseModule)
			return new LoadResult(m,ErrorCode.frozen);
		else {
			//Log.d("vortex","in freez: Essence is "+m.getEssence());
			return new LoadResult(m,ErrorCode.noData);
		}

	}

}
