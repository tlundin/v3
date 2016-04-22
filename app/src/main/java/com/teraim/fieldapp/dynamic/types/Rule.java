package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.util.Log;

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
            Log.d("vortex", "Bananas rule " + condition.toString());
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


    public enum Type {
        ERROR,
        WARNING;
    }



    //Execute Rule. Target will be colored accordingly.
    public Boolean execute() throws SyntaxException {
       if (condition!=null) {
    	   System.err.println("BANANA: CALING BOOL ANALYSIS WITH "+condition.toString());
       
        //Log.d("NILS", "Result of rule eval was: " + Expressor.analyzeBooleanExpression(condition));
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
