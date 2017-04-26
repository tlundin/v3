package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

/**
 * Created by Terje on 2017-04-23.
 */


public class BlockGoSub extends Block {

    private static final long serialVersionUID = -8381560302516157092L;
    private String target;
    public BlockGoSub(String id, String target) {
        blockId=id;
        this.target=target;
    }

    public String getTarget() {
        return target;
    }
}
