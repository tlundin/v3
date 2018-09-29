package com.teraim.fieldapp.dynamic.types;

public class ColumnDescriptor {

	public final String colName;
	private final boolean isEditable;
	public final boolean isHeader;
	private final boolean isOutput;
	
	public ColumnDescriptor(String name, boolean canEdit, boolean isHeader, boolean isOutput) {
		this.colName=name;
		this.isEditable=canEdit;
		this.isHeader=isHeader;
		this.isOutput=isOutput;		
	}
}
