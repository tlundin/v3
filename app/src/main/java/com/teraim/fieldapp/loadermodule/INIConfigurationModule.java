package com.teraim.fieldapp.loadermodule;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.configurations.CI_ConfigurationModule;
import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
import com.teraim.fieldapp.utils.PersistenceHelper;

public abstract class INIConfigurationModule extends CI_ConfigurationModule {

    public INIConfigurationModule(PersistenceHelper gPh, PersistenceHelper ph,
                                  ConfigurationModule.Source source, String urlOrPath, String fileName, String moduleName) {
        super(gPh,ph, ConfigurationModule.Type.ini, source, urlOrPath, fileName, moduleName);
    }


}
