package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;

public class RutListEntry implements Serializable {		
	private static final long serialVersionUID = 7796264436655011442L;
	public Integer id;
	public double n,e;
	public String currentDistance;
	
	public RutListEntry() {
		
	}
	public RutListEntry (RutListEntry rl) {
		this.id=rl.id;
		this.n = rl.n;
		this.e = rl.e;
		this.currentDistance="";
	}
}
