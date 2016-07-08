package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

/**
 * Created by Terje on 2016-07-08.
 */




public class SliderGroupBlock extends Block {
    String groupName, function, arguments,delay;



    public SliderGroupBlock(String id, String groupName, String function, String arguments, String delay) {
        blockId=id;
        this.groupName=groupName;
        this.function=function;
        this.arguments=arguments;
        this.delay=delay;


    }


    public void create(WF_Context myContext) {

        myContext.addSliderGroup(this);


    }

    public String getName() {
        return groupName;
    }
}
