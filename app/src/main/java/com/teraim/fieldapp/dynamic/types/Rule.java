package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.expr.SyntaxException;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.Expressor;

public class Rule implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1965204853256767316L;
    public String targetName,  action, errorMsg,label,id;
    private Expressor.EvalExpr condition;
    private Context ctx;
    private Type myType;
    private boolean initDone = false;
    private LoggerI o;
    private int myTarget=-1;
    private String conditionS=null;
    //Old rule engine for back compa.
    private boolean oldStyle = false;

    public Rule(String id, String ruleLabel, String target, String condition,
                String action, String errorMsg) {

        this.label=ruleLabel;
        this.id=id;
        this.targetName=target;
        //
        List<Expressor.EvalExpr> tmp = Expressor.preCompileExpression(condition);
        if (tmp!=null) {
            this.condition = tmp.get(0);
            Log.d("vortex", "Bananas rule " + condition);
        } else
            Log.d("vortex", "Condition precompiles to null: "+condition);
        this.errorMsg=errorMsg;
        myType = Type.WARNING;
        if (action!=null && action.equalsIgnoreCase("Error_severity"))
            myType = Type.ERROR;
        try {
            myTarget = Integer.parseInt(target);
        } catch (NumberFormatException e) {};
    }

    WF_Context ruleContext=null;
    BlockCreateListEntriesFromFieldList myBlock=null;

    public void setTarget(WF_Context myContext, BlockCreateListEntriesFromFieldList bl) {
        ruleContext=myContext;
        myBlock=bl;

    }


    public enum Type {
        ERROR,
        WARNING;
    }



    //Execute Rule. Target will be colored accordingly.
    public Boolean execute() throws SyntaxException {
       if (condition!=null) {
    	   System.err.println("BANANA: CALING BOOL ANALYSIS WITH "+condition.toString());
       
        //Log.d("NILS", "Result of rule eval was: " + Expressor.analyzeBooleanExpression(condition));
           if (ruleContext!=null && myBlock!=null) {
               WF_Static_List list = ruleContext.getList(myBlock.getListId());

               //target list for rule.
               List<List<String>> l = list.getRows();
               if (l!=null)
                   Log.d("vortex","jaaaaa");
               return Expressor.analyzeBooleanExpression(condition,l);

           }
           return Expressor.analyzeBooleanExpression(condition);
       } 
       return false;
    }

    public String getRuleText() {
        return errorMsg;
    }

    public String getRuleHeader() {
        return label;
    }

    public Type getType() {
        return myType;
    }

    public int getTarget() {
        return myTarget;
    }
    public String getCondition() {
        return conditionS;
    }

}
