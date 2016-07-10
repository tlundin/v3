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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import android.os.Handler;

/**
 * Created by Terje on 2016-07-08.
 */




public class CoupledVariableGroupBlock extends Block implements EventListener {
    String groupName, function, arguments;
    long delay=0;

    WF_Context myC;
    private boolean active=false;

    public CoupledVariableGroupBlock(String id, String groupName, String function, String arguments, String delay) {
        blockId=id;
        if (groupName==null||groupName.isEmpty())
            groupName="NoName";
        this.groupName=groupName;
        this.function=function;
        this.arguments=arguments;
        if (delay!=null && !delay.isEmpty())
            this.delay=Long.parseLong(delay);
    }


    public void create(final WF_Context myContext) {
        myC = myContext;
        active=false;
        myVariables=new HashSet<Variable>();
        myContext.registerEventListener(this, Event.EventType.onSave);
        myContext.addSliderGroup(this);

    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onEvent(Event e) {
        if (e.getType() == Event.EventType.onSave) {
            WF_Event_OnSave onS = (WF_Event_OnSave) e;
            if (onS.getListable()!=null) {
                Set<Variable> variables = onS.getListable().getAssociatedVariables();
                for (Variable v:variables) {
                    if (isOneofMyVariables(v)) {
                        if (!active) {
                            active = true;
                            calibrateMe(myC.getSliderGroupMembers(groupName));
                        } else {
                            Log.d("vortex","I AM ACTIVE !!"+groupName);
                        }
                    }
                }
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

    public String getName() {
        return groupName;
    }

    int currentSum = 0;



    //spawn a thread that calibrates group towards approved value.
    private void calibrateMe(final List<WF_ClickableField_Slider> sliders) {

        //IF SUM ....

        final int sumToReach = Integer.parseInt(this.getArguments());
        int  totalMin = 0;
        int totalMax = 0;
        for (WF_ClickableField_Slider slider:sliders) {
            totalMin += slider.getMin();
            totalMax += slider.getMax();
        }
        if (sumToReach>(totalMax) || sumToReach < totalMin) {
            Log.d("vortex","over Max or below min: "+sumToReach+", "+totalMin+" , "+totalMax);
            o = GlobalState.getInstance().getLogger();
            o.addRow("");
            o.addRedText("Argument to SUM in SliderGroup "+getName()+" is either too high or too low: "+getArguments()+". Max allowed is: "+totalMax+" Min allowed is "+totalMin);
            return;
        }
        currentSum = 0;
        for (WF_ClickableField_Slider slider:sliders) {
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
                    for (WF_ClickableField_Slider slider:sliders) {

                        if (currentSum < sumToReach) {
                            increaseSlider(slider);

                        } else if (currentSum > sumToReach) {
                            decreaseSlider(slider);
                        }

                    }
                    if (oldSum==currentSum) {
                        Log.e("vortex","ups...SUM cannot be reached in group "+groupName);
                        Log.e("vortex","currentSum: "+currentSum+" sumtoreach: "+sumToReach);
                        updateSliderVariables(sliders);
                        active = false;
                    } else
                        handler.postDelayed(this,50);
                    Log.d("vortex","Current sum is: "+currentSum);
                } else {
                    Log.d("vortex","Sliders are calibrated.update variables.");
                    updateSliderVariables(sliders);
                    active = false;
                }
            }
        };

        handler.postDelayed(runnable,delay);


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

    public String getArguments() {
        return arguments;
    }
}
