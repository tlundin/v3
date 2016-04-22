package com.teraim.fieldapp.dynamic.blocks;

import static com.teraim.fieldapp.utils.Expressor.preCompileExpression;

import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public class BlockDeleteMatchingVariables extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1134485697631003990L;
	String label,context,pattern;
	private List<EvalExpr> contextE;
	
	public BlockDeleteMatchingVariables(String id, String label, String target, String pattern) {
		this.blockId=id;
		this.label=label;
		this.context=target;
		this.pattern=pattern;
		if (pattern!=null && pattern.isEmpty())
			this.pattern = null;
		if (context !=null)
			contextE = preCompileExpression(context);
		System.err.println("Bananas: "+((contextE == null)?"null":contextE.toString()));
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance().getLogger();
		o.addRow("Now deleting all variables under Context: ["+context+"] and pattern "+pattern);
		DB_Context evaluatedContext = DB_Context.evaluate(contextE);
		if (evaluatedContext.isOk()) {
			Map<String, String> hash = evaluatedContext.getContext();
			//Delete database entries.
			//int entriesDeleted = GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(hash);
			StringBuilder keyBuilder = new StringBuilder("");
			boolean last = false;
			int i = 0;
			for (String key:hash.keySet()) {
				last = (i == hash.keySet().size()-1);
				keyBuilder.append(key+"="+hash.get(key));
				if (!last)
					keyBuilder.append(",");

				i++;
			}
			GlobalState.getInstance().getDb().erase(keyBuilder.toString(),pattern);
			o.addRow("Deleted all entries under context "+hash);
			//Create sync entry.
			Log.d("vortex","Creating Erase sync entry for "+keyBuilder.toString());			
			GlobalState.getInstance().getDb().insertEraseAuditEntry(keyBuilder.toString(),pattern);
			
		} else {
			o.addRow("");
			o.addRedText("The target context in Delete block contains an error. Error: "+evaluatedContext);
		}
		
	}	
	
			
		
		
}


