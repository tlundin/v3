package com.teraim.fieldapp.loadermodule;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Build;
import android.util.Log;
import android.util.MalformedJsonException;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.Tools;


public class WebLoader extends Loader {

	private InputStream in;

	public WebLoader(ProgressBar pb, TextView tv, FileLoadedCb cb, String versionControlS) {
		super(pb, tv, cb,versionControlS);
	}

	@Override
	protected LoadResult doInBackground(ConfigurationModule... params) {
		ConfigurationModule module = params[0];
		URL url;
		float version = -1;
		try {
			url = new URL(module.getURL());
			Log.d("vortex","trying to open connection");
			URLConnection ucon = url.openConnection();
			Log.d("vortex","setting timeout");
			ucon.setConnectTimeout(5000);
			in = ucon.getInputStream();
			InputStreamReader inStream = new InputStreamReader(in, "UTF-8");
			BufferedReader reader = new BufferedReader(inStream);
			//check version for xml file.
			String headerRow1,headerRow2;
			if (module.isBundle) {
				reader.mark(500) ;
				headerRow1 = reader.readLine();
				headerRow2 = reader.readLine();
				if (headerRow1 == null) {
					Log.e("vortex", "cannot read data..exiting");
					return new LoadResult(module, ErrorCode.IOError);
				}
				Log.d("amazon","h1: "+headerRow1);
                Log.d("amazon","h2: "+headerRow2);
				reader.reset();
				version = getVersion(null,headerRow2);

			} else {
				if (module.hasSimpleVersion) {
					headerRow1 = reader.readLine();
					if (headerRow1 == null) {
						Log.e("vortex", "cannot read data..exiting");
						return new LoadResult(module, ErrorCode.IOError);
					}
					version = getVersion(headerRow1,null);
				}
			}

			StringBuilder sb = new StringBuilder();

				//If the file is not readable or reachable, header is null.

			//setresult runs a parser before returning. Parser is depending on module type.
			LoadResult loadResult = read(module,version,reader,sb);
            if (loadResult!=null && loadResult.errCode==ErrorCode.loaded) {
				loadResult = parse(module);
				if (loadResult.errCode==ErrorCode.parsed)
					return freeze(module);
				else {
					Log.d("abba",module.getLabel()+" returns "+loadResult.errorMessage);
					return loadResult;
				}
			}
			else
				return loadResult;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.BadURL);

		} catch (IOException e) {
			if (e instanceof MalformedJsonException)
				return new LoadResult(module,ErrorCode.ParseError,"Malformed JSON: "+e.getMessage()+"\n Did you forget to add a version number of the first row?");
			else if (e instanceof FileNotFoundException) {
				return new LoadResult(module,ErrorCode.notFound);
			} else if (e instanceof java.net.SocketTimeoutException) {
				return new LoadResult(module,ErrorCode.socket_timeout);
			}
			else {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);		
				e.printStackTrace();
				Log.d("vortex","gets to here...");
				return new LoadResult(module,ErrorCode.IOError,sw.toString());
			}
		} catch (XmlPullParserException e) {
			return new LoadResult(module,ErrorCode.ParseError,"Malformed XML");
		} catch (JSONException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.ParseError,"JSONException :"+e.getMessage());
		} catch (Dependant_Configuration_Missing e) {
			return new LoadResult(module, ErrorCode.reloadDependant,e.getDependendant());
		}
		finally {
			try {if (in!=null)in.close();}catch (Exception ignored){}
        }
	}





}
