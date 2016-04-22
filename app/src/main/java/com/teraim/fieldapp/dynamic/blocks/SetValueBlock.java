package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class SetValueBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9113802658084282751L;

	public enum ExecutionBehavior {
		constant,dynamic,update_flow
	}


	String target;
	ExecutionBehavior executionBehaviour=ExecutionBehavior.update_flow;
	private List<EvalExpr> expression;
	private String formula;
	
	public SetValueBlock(String id,String target,String expression,String eb) {
		this.blockId = id;
		this.target=target;
		this.expression=Expressor.preCompileExpression(expression);
		this.formula = expression;
		if (eb !=null) {
			for (ExecutionBehavior ex:ExecutionBehavior.values()) {
				if (eb.equals(ex.name()))
					executionBehaviour = ex;
			}
		}
		//Log.e("nils","EXECUTIONBEHAVIOR"+executionBehaviour.name());
	}


	public String getEvaluation() {
		return Expressor.analyze(expression);
	}
	
	public String getExpression() {
		return formula;
	}

	public String getMyVariable() {
		return target;
	}

	public ExecutionBehavior getBehavior() {
		return executionBehaviour;
	}

	/*
	RuleExecutor re;
	public String evaluate(GlobalState gs,String formula,
			List<TokenizedItem> tokens, boolean stringT) {
		//assume fail
		re = GlobalState.getInstance().getRuleExecutor();
		//substitute any variables or functions.
		SubstiResult sr = re.substituteForValue(tokens,formula,stringT);
		String subst = sr.result;
		if (subst!=null && Tools.isNumeric(subst))
			return subst;
		if (subst!=null && !sr.iAmAString()) { 
			subst = re.parseExpression(formula,subst);
			Log.d("vortex","YYY Evaluate "+subst);
			return subst;
		}
		if (sr.iAmAString() && subst  == null) {
			Log.e("nils","Formula null after substitution: ["+formula+"]");
			return null;
		}
		else {
			Log.d("nils","New eval returns "+subst);
			return subst;
		}

	}
	*/

}
