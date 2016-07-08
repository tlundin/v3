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
		String ret=null;
		if (reader.peek() != JsonToken.NULL) {
			if (reader.peek() == JsonToken.STRING) {
				ret = reader.nextString();
				if (ret.isEmpty())
					ret = null;
			} else if (reader.peek() == JsonToken.NUMBER) {
				ret = Long.toString(reader.nextLong());
			}
			else if (reader.peek() == JsonToken.BEGIN_OBJECT) {
				reader.beginObject();
				while (reader.peek() != JsonToken.END_OBJECT) {
					String name = reader.nextName();
					String attr = getAttribute(reader);
					ret = name+":"+attr;
				}
				reader.endObject();
			}
		}
		else { 
			ret = null;
			reader.nextNull();
		}
		//Log.d("vortex","Value: "+ret);
		return ret;

	}
}
