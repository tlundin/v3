package com.teraim.fieldapp.dynamic.types;

import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

/**
 * Created by Terje on 2016-05-11.
 */
public class MapGisLayer extends GisLayer {
    private final String myImg;
    public MapGisLayer(WF_Gis_Map myGis, String label, String myImgName) {
        super(myGis, label+myImgName, label, false, true, false);
        myImg = myImgName;
    }
@Override
public String getId() {
        return getLabel()+getImageName();
    }
public Object getImageName() {
        return myImg;
    }

}
