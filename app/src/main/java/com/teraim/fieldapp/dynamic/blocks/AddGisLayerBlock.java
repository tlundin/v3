package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

public class AddGisLayerBlock extends Block {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4149408006972701777L;
	private String name, label,target;
	private boolean isVisible, hasWidget;
	private WF_Gis_Map myGis;
	private boolean showLabels;
	
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
		
		if (gisMap!=null && gisMap instanceof WF_Gis_Map) {
			myGis = ((WF_Gis_Map)gisMap);
			if (!myGis.isZoomLevel()) {
			final GisLayer gisLayer = new GisLayer(myGis,name,label,isVisible,hasWidget,showLabels);		
			myGis.addLayer(gisLayer);
			}
		}
		
	}
	
	
}