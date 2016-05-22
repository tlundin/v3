package com.teraim.fieldapp.dynamic.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

/**
 *
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class TableDefaultTemplate extends Executor implements Animation.AnimationListener {



	View view;
	private LinearLayout my_root,displayPanel,tablePanel,filterPanel,filterPop,filterC1,filterC2;
	private View filterC1o,filterC2o;
	private FrameLayout frame;
	Map<String,Boolean> popupVisible=null;
	private Animation popupShow;
	private Animation popupHide;
	private boolean animationRunning=false;
	private LayoutInflater inflater=null;

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
		popupVisible=new HashMap<String, Boolean>();
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		this.inflater = inflater;
		View v = inflater.inflate(R.layout.template_table_default, container, false);
//		errorView = (TextView)v.findViewById(R.id.errortext);
		frame = (FrameLayout)v.findViewById(R.id.frame);
		my_root = (LinearLayout)v.findViewById(R.id.myRoot);
		displayPanel = (LinearLayout)v.findViewById(R.id.displayPanel);
		tablePanel = (LinearLayout)v.findViewById(R.id.myTable);
		filterPanel = (LinearLayout)v.findViewById(R.id.filterPanel);

		filterC1o = (View)inflater.inflate(R.layout.filter_pop_inner,null);
		filterC2o = (View)inflater.inflate(R.layout.filter_pop_inner,null);
		filterC1 = (LinearLayout)filterC1o.findViewById(R.id.inner);
		filterC2 = (LinearLayout)filterC2o.findViewById(R.id.inner);
		myContext.addContainers(getContainers());

		/*Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		filterPop.setY(height/2);
		*/

		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();

		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");

		popupShow = AnimationUtils.loadAnimation(getActivity(), R.anim.popup_show_bottom);
		popupShow.setAnimationListener(this);
		popupHide = AnimationUtils.loadAnimation(getActivity(), R.anim.popup_hide_bottom);
		popupHide.setAnimationListener(this);

		return v;
	}

	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		//WF_Container frameC = new WF_Container("frame", frame, null);
		WF_Container root = new WF_Container("root",my_root, null);

		//ret.add(frameC);
		ret.add(root);
		ret.add(new WF_Container("display_panel",displayPanel,root));
		ret.add(new WF_Container("table_panel",tablePanel,root));
		ret.add(new WF_Container("filter_panel",filterPanel,root));
		ret.add(new WF_Container("filter_C1",filterC1,root));
		ret.add(new WF_Container("filter_C2",filterC2,root));
		return ret;
	}


	@Override
	public void execute(String function, String target) {

		Log.d("vortex","Called execute with target "+target);
		if (function.equals("template_pop_up_filters") ) {

			Boolean popupVis = popupVisible.get(target);
			if (popupVis==null || !popupVis) {
				if (filterPop==null) {
					Log.d("vortex","creating!");
					filterPop= (LinearLayout) inflater.inflate(R.layout.filter_menu_pop, null);

				}
				if (target.equals("filter_C1")) {
					filterPop.addView(filterC1o);
				}
				else {
					filterPop.addView(filterC2o);
				}
				if (filterPop.getParent()==null) {
					frame.addView(filterPop);
					filterPop.startAnimation(popupShow);
				}
				Log.d("vortex", "animation started!");
				popupVisible.put(target,true);
			} else {
				Log.d("vortex","deleting!");
				popupVisible.remove(target);

				if (popupVisible.isEmpty()) {
					filterPop.startAnimation(popupHide);
					Log.d("vortex","empty! closing!");
					closePopIfUp();
				}
				else {
					Log.d("vortex","removing!");
					filterPop.removeView(target.equals("filter_C1") ? filterC1o : filterC2o);
				}
			}
		}

	}



	@Override
	public void onStart() {
		Log.d("nils","I'm in the onStart method");
		super.onStart();


	}


	public void onAnimationStart(Animation animation) {
		Log.d("vortex","gets here!");
		if (animation.equals(popupShow)) {
			Log.d("vortex","popshoe!");
			filterPop.setVisibility(View.VISIBLE);
		}
		animationRunning = true;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		animationRunning = false;
		if (animation.equals(popupHide)) {
			filterPop.removeAllViews();
			frame.removeView(filterPop);
			filterPop=null;
			popupVisible.clear();
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}

	public void closePopIfUp() {
		if (!popupVisible.isEmpty()) {
			filterPop.startAnimation(popupHide);
		}
	}
}
