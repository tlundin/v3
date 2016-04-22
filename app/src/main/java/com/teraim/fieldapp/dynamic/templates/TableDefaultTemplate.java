package com.teraim.fieldapp.dynamic.templates;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

/**
 * 
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class TableDefaultTemplate extends Executor {



	View view; 
	private LinearLayout my_root,sortPanel,tablePanel,filterPanel;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","In onCreate");

		
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("nils","I'm in the onPause method");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("nils","I'm in the onCreateView method");
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
			
		View v = inflater.inflate(R.layout.template_table_default, container, false);	
//		errorView = (TextView)v.findViewById(R.id.errortext);
		my_root = (LinearLayout)v.findViewById(R.id.myRoot);
		sortPanel = (LinearLayout)v.findViewById(R.id.sortPanel);
		tablePanel = (LinearLayout)v.findViewById(R.id.myTable);
		filterPanel = (LinearLayout)v.findViewById(R.id.filterPanel);
		myContext.addContainers(getContainers());

		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();
		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");
			
		
		return v;
	}
	
	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		WF_Container root = new WF_Container("root",my_root, null);
		ret.add(root);
		ret.add(new WF_Container("sort_panel",sortPanel,root));
		ret.add(new WF_Container("filter_panel",filterPanel,root));
		ret.add(new WF_Container("table_panel",tablePanel,root));
		return ret;
	}
	@Override
	public void execute(String function, String target) {
		
	}
	
	

	@Override
	public void onStart() {
		Log.d("nils","I'm in the onStart method");
		super.onStart();


	}





}
