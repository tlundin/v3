package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.teraim.fieldapp.dynamic.types.DB_Context;


//Subclass with interfaces that restricts access to all.

public interface FullGisObjectConfiguration extends GisObjectBaseAttributes {

	public enum GisObjectType {
		Point,
		Multipoint,
		Polygon, Linestring
	}
	
	public enum PolyType {
		circle,
		rect,
		triangle
	}
	
	public float getRadius();
	public String getColor();
	public GisObjectType getGisPolyType();
	public Bitmap getIcon();
	public Paint.Style getStyle();
	public PolyType getShape();
	public String getClickFlow();
	public DB_Context getObjectKeyHash();
	public String getStatusVariable();
	public boolean isUser();
	public String getName();
	public String getRawLabel();
	
	
}
