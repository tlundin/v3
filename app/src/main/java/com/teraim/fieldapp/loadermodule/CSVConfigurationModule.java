package com.teraim.fieldapp.loadermodule;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.utils.PersistenceHelper;

public abstract class CSVConfigurationModule extends ConfigurationModule {

	public CSVConfigurationModule(PersistenceHelper gPh,PersistenceHelper ph,
			Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh,ph, Type.csv, source, urlOrPath, fileName, moduleName);
	}

	protected abstract LoadResult prepare() throws IOException, Dependant_Configuration_Missing;
	public abstract LoadResult parse(String row, Integer currentRow) throws IOException;
	
}
