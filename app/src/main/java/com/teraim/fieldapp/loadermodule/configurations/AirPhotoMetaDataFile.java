package com.teraim.fieldapp.loadermodule.configurations;

import android.util.Log;

import com.teraim.fieldapp.loadermodule.CSVConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.IOException;

/**
 * Created by Terje on 2017-09-11.
 */

public class AirPhotoMetaDataFile extends CSVConfigurationModule {


    public AirPhotoMetaDataFile(PersistenceHelper gPh, PersistenceHelper ph,
                               Source source, String urlOrPath, String metaDataFileName,
                               String moduleName) {
        super(gPh, ph, source, urlOrPath, metaDataFileName, moduleName);

    }
    @Override
    protected LoadResult prepare() throws IOException, Dependant_Configuration_Missing {
        //null means nothing to report and no error
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow) throws IOException {
        Log.d("franzon","Row"+currentRow+": "+row);
        return null;
    }

    @Override
    public float getFrozenVersion() {
        return 0;
    }

    @Override
    protected void setFrozenVersion(float version) {

    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public void setEssence() {

    }
}
