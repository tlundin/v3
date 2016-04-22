package com.teraim.fieldapp.expr;

import com.teraim.fieldapp.dynamic.types.Numerable;
import com.teraim.fieldapp.dynamic.types.Numerable.Type;

public class Numeric extends Aritmetic {

	public Numeric(String name, String label) {
		super(name,label);
	}

	@Override
	public Type getType() {
		return Type.NUMERIC;
	}
	
}
