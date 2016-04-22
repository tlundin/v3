package com.teraim.fieldapp.loadermodule.configurations;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.XMLConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.utils.PersistenceHelper;

public class AirPhotoMetaData extends XMLConfigurationModule {



	
	public AirPhotoMetaData(PersistenceHelper gPh, PersistenceHelper ph,
			 Source source, String urlOrPath, String metaDataFileName,
			String moduleName) {
		super(gPh, ph, source, urlOrPath, metaDataFileName, moduleName);
		hasSimpleVersion=false;
	}

	@Override
	protected LoadResult prepare(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		return null;
	}

	@Override
	protected LoadResult parse(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d("vortex","Parsing metadata");
		   int eventType = parser.getEventType();
	      while (eventType != XmlPullParser.END_DOCUMENT) {

	            if (eventType == XmlPullParser.START_TAG) {
	                if (parser.getName().equalsIgnoreCase("nativeExtBox")) {

	                   essence = readCorners(parser);
	                   return new LoadResult(this,ErrorCode.parsed);
	                   }
	            }

	            eventType = parser.next();
	        }
          Log.d("found","Did not find the meta data for image GPS coordinates!");
	      return new LoadResult(this,ErrorCode.ParseError);
	}

	private PhotoMeta readCorners(XmlPullParser parser) throws XmlPullParserException, IOException {
		double N=-1,E=-1,S=-1,W=-1;
		Log.d("vortex","calling readCordners");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			Log.d("vortex","reading corners!");
			String name= parser.getName();
			if (name.equals("westBL")) {
				W = getCorner(parser);
			} else if (name.equals("eastBL")) {
				E = getCorner(parser);
			} else if (name.equals("northBL")) {
				N = getCorner(parser);
			} else if (name.equals("southBL")) {
				S = getCorner(parser);
				
			} else
				skip(name,parser);
		}
		PhotoMeta pm = new PhotoMeta(N,E,S,W);
		return pm;
	}
	
	private double getCorner(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.next();
		double f =Double.parseDouble(parser.getText());
		parser.nextTag();
		return f;

	}

	@Override
	public float getFrozenVersion() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	protected void setFrozenVersion(float version) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRequired() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEssence() {
		
	}

}
