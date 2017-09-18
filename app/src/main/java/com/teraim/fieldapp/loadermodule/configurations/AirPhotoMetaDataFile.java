package com.teraim.fieldapp.loadermodule.configurations;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.INIConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.IOException;

/**
 * Created by Terje on 2017-09-11.
 */

public class AirPhotoMetaDataFile extends INIConfigurationModule {


    private static enum Corners {
        LOWERLEFTCORNERX,
        UPPERRIGHTCORNERX,
        LOWERLEFTCORNERY,
        UPPERRIGHTCORNERY
    }


    String s,w,e,n;


    public AirPhotoMetaDataFile(PersistenceHelper gPh, PersistenceHelper ph,
                                ConfigurationModule.Source source, String urlOrPath, String metaDataFileName,
                                String moduleName) {
        super(gPh, ph, source, urlOrPath, metaDataFileName, moduleName);


    }
    @Override
    public LoadResult prepare() throws IOException, Dependant_Configuration_Missing {
        //null means nothing to report and no error
        s=w=e=n=null;
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow) throws IOException {
        Log.d("franzon","Row"+currentRow+": "+row);
        String[] coord = row.split("=");
        if (coord==null || coord.length!=2)
            return new LoadResult(this, LoadResult.ErrorCode.ParseError);
        if(coord[0].equals(Corners.LOWERLEFTCORNERX.name())) {
            s=coord[1].trim();
        }
        else if(coord[0].equals(Corners.LOWERLEFTCORNERY.name())) {
            w=coord[1].trim();
        }
        else  if(coord[0].equals(Corners.UPPERRIGHTCORNERX.name())) {
            n=coord[1].trim();
        }
        else  if(coord[0].equals(Corners.UPPERRIGHTCORNERY.name())) {
            e=coord[1].trim();
        } else
            return new LoadResult(this, LoadResult.ErrorCode.ParseError);

        return null;
    }

    @Override
    public LoadResult finalizeMe() throws IOException {
        if (w!=null&&e!=null&&s!=null&&n!=null) {
            Log.d("franzon","photometa is parsed");
            setEssence(new PhotoMeta(n ,e, s, w));
        }
        else {
            Log.e("vortex","Photometa file is corrupt");
            return new LoadResult(this, LoadResult.ErrorCode.ParseError,"Photometa file is corrupt");
        }
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
