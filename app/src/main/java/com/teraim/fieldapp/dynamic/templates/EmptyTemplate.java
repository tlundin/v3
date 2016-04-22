package com.teraim.fieldapp.dynamic.templates;

import java.util.List;

import android.R;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

public class EmptyTemplate extends Executor {


	@Override
	protected List<WF_Container> getContainers() {
		return null;
	}

	@Override
	public void execute(String function, String target) {
	}

	@Override
	public void onStart() {
		Log.d("vortex","in onstart for empty fragment");
		super.onStart();
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if (wf!=null)
			run();


		
		
	}
	


}
