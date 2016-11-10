package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Slider;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.os.Handler;

/**
 * Created by Terje on 2016-07-08.
 */




public class CoupledVariableGroupBlock extends Block implements EventListener {
    private final List<Expressor.EvalExpr> argumentE;
    String groupName, function, argument;
    private Integer currentEvaluationOfArg = null;
    long delay=0;

    WF_Context myC;
    private boolean active=false;

    public CoupledVariableGroupBlock(String id, String groupName, String function, String argument, String delay) {
        blockId=id;
        if (groupName==null||groupName.isEmpty())
            groupName="NoName";
        this.groupName=groupName;
        this.function=function;
        argumentE= Expressor.preCompileExpression(argument);
        if (delay!=null && !delay.isEmpty())
            this.delay=Long.parseLong(delay);
    }

    private static final String SUM = "SUM";
    private static final String MIN_SUM = "MINSUM";
    private static final String MAX_SUM = "MAXSUM";
    private static final String SUM_STICKY_LIMITS = "SUM_STICKY_LIMITS";
    private static final String SUM_STICKY_MIN = "SUM_STICKY_MIN";
    private static final String SUM_STICKY_MAX = "SUM_STICKY_MAX";
    private static final String SUM_STICKY = "SUM_STICKY";

    public void create(final WF_Context myContext) {
        myC = myContext;
        active=false;
        myVariables=new HashSet<Variable>();
        currentEvaluationOfArg = null;
        myContext.registerEventListener(this, Event.EventType.onSave);
        myContext.addSliderGroup(this);
        if (function==null || function.isEmpty()) {
            function = "SUM";
            Log.d("vortex","?");
        }
        Log.d("vortex","function is "+function);

        onEvent(new WF_Event_OnSave(null));
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onEvent(Event e) {
        Log.d("vortex","in onEvent");
        if (e.getType() == Event.EventType.onSave) {
            argument = Expressor.analyze(argumentE);
            Log.d("vortex","in onSave with "+argument);
            if (!Tools.isNumeric(argument)){
                Log.d("vortex","cannot calibrate...argument evaluates to non numeric: "+argument);
                o.addRow("");
                o.addRedText("Argument to SUM in SliderGroup "+getName()+" is not numeric: "+argument+" Expr: "+argumentE);
                return;
            }
            final int sumToReach = Integer.parseInt(argument);
            Log.d("vortex","sum to reach: "+sumToReach);
            boolean change = false;
            Log.d("vortex","curreval: "+currentEvaluationOfArg);
            if (currentEvaluationOfArg == null || (sumToReach != currentEvaluationOfArg)) {
                currentEvaluationOfArg = sumToReach;
                change = true;
                Log.d("vortex","arg change in group "+groupName);
            } //Check if a variable value has changed
            else {
                WF_Event_OnSave onS = (WF_Event_OnSave)e;
                if (onS.getListable() != null) {
                    Set<Variable> variables = onS.getListable().getAssociatedVariables();
                    for (Variable v : variables) {
                        if (isOneofMyVariables(v)) {
                            change = true;
                            Log.d("vortex","var change in group "+groupName);
                            break;
                        }
                    }
                }
            }
            //If no calibration is active, and value of function has changed or one of the variables in slidergroup has changed.
            if (!active && change) {
                active=true;
                calibrateMe(myC.getSliderGroupMembers(groupName), currentEvaluationOfArg);
            } else {
                Log.d("vortex","not required");
            }

        }
    }

    Set<Variable> myVariables;
    private boolean isOneofMyVariables(Variable v) {
        if (myVariables.isEmpty()) {
            List<WF_ClickableField_Slider> sliders = myC.getSliderGroupMembers(getName());
            if (sliders!=null) {
                for(WF_ClickableField entryField:sliders) {
                    Set<Variable> variables = entryField.getAssociatedVariables();
                    if (variables!=null) {
                        myVariables.addAll(variables);
                    }


                }
            }
        }
        if (myVariables!=null) {
            return myVariables.contains(v);
        }

        return false;
    }

    @Override
    public String getName() {
        return groupName;
    }

    int currentSum = 0;



    //spawn a thread that calibrates group towards approved value.
    private void calibrateMe(final List<WF_ClickableField_Slider> sliders, final int sumToReach) {


        int  totalMin = 0;
        int totalMax = 0;
        final List<WF_ClickableField_Slider> slidersToCalibrate;//=new ArrayList<>();

        if (sliders==null || sliders.isEmpty())
            return;
        if (functionIsStickyLimits() && sliders.size()<3) {
            Log.d("vortex","sticky return");
            return;
        }
        if (functionIsSticky() && sliders.size()<2) {
            Log.d("vortex","sticky min max return");
            return;
        }

        WF_ClickableField_Slider min=null,max=null;
        for (WF_ClickableField_Slider slider:sliders) {
            totalMin += slider.getMin();
            totalMax += slider.getMax();
            if (min == null || slider.getPosition()<min.getPosition())
                min = slider;
            if (max == null || slider.getPosition()>max.getPosition())
                max = slider;
        }

        //Remove min max sliders.
        if (functionIsSticky()) {
            slidersToCalibrate = new ArrayList<>(sliders);
            if (functionIsStickyLimits()||functionIsStickyLimitsMin())
                slidersToCalibrate.remove(min);
            if (functionIsStickyLimits()||functionIsStickyLimitsMax())
                slidersToCalibrate.remove(max);
        } else
            slidersToCalibrate=sliders;



        if (sumToReach>totalMax || sumToReach < totalMin) {
            Log.d("vortex","over Max or below min: "+sumToReach+", "+totalMin+" , "+totalMax);
            o = GlobalState.getInstance().getLogger();
            o.addRow("");
            o.addRedText("Argument to SUM in SliderGroup "+getName()+" is either too high or too low: "+argument+". Max allowed is: "+totalMax+" Min allowed is "+totalMin);
            return;
        }
        Log.d("vortex","SUM TO REACH: "+sumToReach);
        currentSum = 0;

        for (WF_ClickableField_Slider slider:slidersToCalibrate) {
            Integer value = slider.getSliderValue();
            if (value!=null) {
                currentSum += value;
                Log.d("currentsum","slider "+slider.getId()+" value: "+value);
            }

        }
        Log.d("currentsum","Currentsum initial is: "+currentSum);
        final Handler handler = new Handler();
        Runnable runnable = new Runnable(){
            public void run() {
                if (currentSum!=sumToReach) {
                    //Check if any change.
                    int anyChange=0;
                    int oldSum = currentSum;
                    Log.d("currentsum","Currentsum is: "+currentSum+" min func? "+functionIsMinSum()+" sum to reach: "+sumToReach);

                    for (WF_ClickableField_Slider slider:slidersToCalibrate) {

                        if ((functionIsSum() || functionIsMinSum() || functionIsSticky()) && currentSum < sumToReach) {
                            increaseSlider(slider);

                        } else if ((functionIsSum() || functionIsMaxSum() || functionIsSticky()) && currentSum > sumToReach) {
//                            Log.d("vortex","gets here: x n m"+functionIsMaxSum()+","+functionIsSum()+","+functionIsMinSum());
                            decreaseSlider(slider);
                        }

                    }
                    if (oldSum==currentSum) {
                        Log.d("vortex","SUM not changed in group "+groupName);
                        Log.d("vortex","currentSum: "+currentSum+" sumtoreach: "+sumToReach);
                        updateSliderVariables(slidersToCalibrate);
                        active = false;
                    } else
                        handler.postDelayed(this,50);
                    Log.d("vortex","Current sum is: "+currentSum);
                } else {
                    Log.d("vortex","Sliders are calibrated.update variables.");
                    updateSliderVariables(slidersToCalibrate);
                    active = false;
                }
            }




            }

            ;

        handler.postDelayed(runnable,delay);


    }

    private boolean functionIsSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM);
    }

    private boolean functionIsMaxSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.MAX_SUM);
    }


    private boolean functionIsMinSum() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.MIN_SUM);
    }

    public boolean functionIsSticky() {
        return function.toUpperCase().startsWith(CoupledVariableGroupBlock.SUM_STICKY);
    }
    public boolean functionIsStickyLimits() {
        return function.toUpperCase().startsWith(CoupledVariableGroupBlock.SUM_STICKY_LIMITS);
    }
    public boolean functionIsStickyLimitsMin() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM_STICKY_MIN);
    }
    public boolean functionIsStickyLimitsMax() {
        return function.equalsIgnoreCase(CoupledVariableGroupBlock.SUM_STICKY_MAX);
    }
    private void updateSliderVariables(List<WF_ClickableField_Slider> sliders) {
        for (WF_ClickableField_Slider slider:sliders) {
           slider.setValueFromSlider();
        }
    }

    final static Random r = new Random();

    private void increaseSlider(WF_ClickableField_Slider slider) {
        int curr = slider.getPosition();
        if (curr<slider.getMax()) {
            currentSum++;
            slider.setPosition(curr + 1);
        } else
            Log.d("vortex",slider.getId()+" is at max: "+slider.getPosition());
    }

    private void decreaseSlider(WF_ClickableField_Slider slider) {
        int curr = slider.getPosition();
        if (curr>slider.getMin()) {
            currentSum--;
            slider.setPosition(curr - 1);
        } else
            Log.d("vortex","this slider is at min: "+slider.getPosition());
    }

}
