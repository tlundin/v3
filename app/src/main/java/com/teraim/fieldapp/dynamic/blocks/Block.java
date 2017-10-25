package com.teraim.fieldapp.dynamic.blocks;

import java.io.Serializable;

import com.teraim.fieldapp.log.LoggerI;

/**
 * Abstract base class Block
 * Marker class.
 * @author Terje
 *
 */
public abstract  class Block implements Serializable {
	private static final long serialVersionUID = -8275181338935274930L;
	protected LoggerI o;
	protected String blockId;
	
	
	public String getBlockId() {
		return blockId;
	}



}