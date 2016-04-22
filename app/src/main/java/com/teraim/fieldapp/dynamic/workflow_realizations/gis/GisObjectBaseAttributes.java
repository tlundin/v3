package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.List;

import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public interface GisObjectBaseAttributes {

	public boolean isVisible();
	public List<EvalExpr> getLabelExpression();

}
