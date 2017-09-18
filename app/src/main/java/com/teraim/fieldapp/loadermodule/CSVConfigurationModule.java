package com.teraim.fieldapp.loadermodule;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.teraim.fieldapp.loadermodule.configurations.CI_ConfigurationModule;
import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.utils.PersistenceHelper;

public abstract class CSVConfigurationModule extends CI_ConfigurationModule {

	public CSVConfigurationModule(PersistenceHelper gPh, PersistenceHelper ph,
								  ConfigurationModule.Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh,ph, ConfigurationModule.Type.csv, source, urlOrPath, fileName, moduleName);
	}
	@Override
	public LoadResult finalizeMe() throws IOException {
		return null;
	}

}
