package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

public class AddGisLayerBlock extends Block {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4149408006972701777L;
	private final String name;
    private final String label;
    private final String target;
	private final boolean isVisible;
    private final boolean hasWidget;
    private final boolean showLabels;
	
	public AddGisLayerBlock(String id, String name, String label,
			String target, boolean isVisible, boolean hasWidget, boolean showLabels) {
		super();
		this.blockId = id;
		this.name = name;
		this.label = label;
		this.target = target;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
		this.showLabels = showLabels;
		
	}
	

	public void create(WF_Context myContext) {

		Drawable gisMap = myContext.getDrawable(target);
		
		if (gisMap instanceof WF_Gis_Map) {
            WF_Gis_Map myGis = ((WF_Gis_Map) gisMap);
			if (!myGis.isZoomLevel()) {
			final GisLayer gisLayer = new GisLayer(myGis,name,label,isVisible,hasWidget,showLabels);
			myGis.addLayer(gisLayer);
			}
		} else {
			if (gisMap==null) {
				o = GlobalState.getInstance().getLogger();
				o.addRow("");
				o.addRedText("The target map in Gislayerblock "+getBlockId()+" is not found so the layer is not added");
			}
		}
		
	}
	
	
}