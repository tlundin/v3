package com.teraim.fieldapp.loadermodule;

import java.io.IOException;

import org.json.JSONException;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.teraim.fieldapp.utils.PersistenceHelper;

public abstract class JSONConfigurationModule extends ConfigurationModule {

	public JSONConfigurationModule(PersistenceHelper gPh,PersistenceHelper ph,
			Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh,ph, Type.json, source, urlOrPath, fileName, moduleName);
	}

	protected abstract LoadResult prepare(JsonReader reader) throws IOException, JSONException;
	public abstract LoadResult parse(JsonReader reader) throws IOException, JSONException;
	
	protected String getAttribute(JsonReader reader) throws IOException {
		String ret;
		if (reader.peek() != JsonToken.NULL) {
			ret =  reader.nextString();
			if (ret.isEmpty())
				ret=null;
		}
		else { 
			ret = null;
			reader.nextNull();
		}
		//Log.d("vortex","Value: "+ret);
		return ret;

	}
}
