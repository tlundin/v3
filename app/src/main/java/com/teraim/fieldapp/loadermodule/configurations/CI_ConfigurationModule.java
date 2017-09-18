package com.teraim.fieldapp.loadermodule.configurations;

import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.IOException;

/**
 * Created by Terje on 2017-09-12.
 */

public abstract class CI_ConfigurationModule extends ConfigurationModule {

    public CI_ConfigurationModule(PersistenceHelper gPh, PersistenceHelper ph,ConfigurationModule.Type type,
                                  ConfigurationModule.Source source, String urlOrPath, String fileName, String moduleName) {
        super(gPh,ph, type, source, urlOrPath, fileName, moduleName);
    }

    public abstract LoadResult prepare() throws IOException, Dependant_Configuration_Missing;
    public abstract LoadResult parse(String row, Integer currentRow) throws IOException;
    public abstract LoadResult finalizeMe() throws IOException;


}
