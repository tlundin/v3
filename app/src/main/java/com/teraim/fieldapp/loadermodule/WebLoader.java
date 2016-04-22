package com.teraim.fieldapp.loadermodule;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
		try {

			url = new URL(module.fullPath);

			URLConnection ucon = url.openConnection();
			if (Build.VERSION.SDK != null && Build.VERSION.SDK_INT > 13) { ucon.setRequestProperty("Connection", "close"); }
			in = ucon.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			//reader.readLine();
			String headerRow1 = reader.readLine();
			
			//If the file is not readable or reachable, header is null.
			if (headerRow1==null) {
				Log.e("vortex","cannot read data..exiting");
				return new LoadResult(module,ErrorCode.IOError);
			}
		
			//setresult runs a parser before returning. Parser is depending on module type.
			LoadResult loadResult = read(module,getVersion(headerRow1,null),reader,sb);;
			if (loadResult!=null && loadResult.errCode==ErrorCode.loaded) {
				loadResult = parse(module);
				if (loadResult.errCode==ErrorCode.parsed)
					return freeze(module);
				else
					return loadResult;
			}
			else
				return loadResult;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.BadURL);

		} catch (IOException e) {
			if (e instanceof MalformedJsonException)
				return new LoadResult(module,ErrorCode.ParseError);
			else if (e instanceof FileNotFoundException) {
				return new LoadResult(module,ErrorCode.notFound);
			}
			else {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);		
				e.printStackTrace();
				return new LoadResult(module,ErrorCode.IOError,sw.toString());
			}
		} catch (XmlPullParserException e) {
			return new LoadResult(module,ErrorCode.ParseError);
		} catch (JSONException e) {
			e.printStackTrace();
			return new LoadResult(module,ErrorCode.ParseError);
		} catch (Dependant_Configuration_Missing e) {
			return new LoadResult(module, ErrorCode.reloadDependant,e.getDependendant());
		}
		finally {
			try {if (in!=null)in.close();}catch (Exception e){};
		}
	}





}
