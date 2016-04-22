package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class ConditionalContinuationBlock extends Block {

	String elseID,expr;
	List<String>variables;
	List<EvalExpr>exprE;
	public ConditionalContinuationBlock(String id, List<String> varL,
			String expr, String elseBlockId) {
		this.blockId=id;
		this.variables=varL;
		this.expr=expr;
		this.exprE = Expressor.preCompileExpression(expr);
		this.elseID=elseBlockId;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5923203475793337276L;
	public String getFormula() {
		return expr;
	}
	public String getElseId() {
		return elseID;
	}
    public final static int STOP = 1,JUMP=2,NEXT = 3;
    
    Integer lastEval = null;
    
    public boolean isExpressionOk() {
    	return exprE!=null;
    }
    
	public boolean evaluate() {
			//assume fail
			int eval=STOP;
			if (exprE!=null && exprE.size()==1) {
				Boolean result = Expressor.analyzeBooleanExpression(exprE.get(0));
			if (result!=null) { 
				if (result==true) {
						Log.d("nils","Evaluates to true");
						eval=NEXT;
				} else {
						eval=JUMP;
						Log.d("nils","Evaluates to false");
					}
				}
			} else {
				Log.e("vortex","Stopped on block "+this.getBlockId()+" due to null eval");
				eval=STOP;
			}
			
		
		boolean ret = lastEval==null?true:eval!=lastEval;
		lastEval = eval;
		return ret;
	}
	public Integer getCurrentEval() {
		return lastEval;
	}

	
}
