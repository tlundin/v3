package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sortable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

public abstract class WF_List extends WF_Widget implements Sortable,Filterable {

	protected final List<Listable> list = new  ArrayList<Listable>(); //Instantiated in constructor
	protected final List<Filter> myFilters=new ArrayList<Filter>();
	protected final List<Sorter> mySorters=new ArrayList<Sorter>();
	protected List<? extends Listable> filteredList;

	protected WF_Context myContext;
	protected GlobalState gs;
	protected VariableCache varCache;
	
	private ViewGroup myW = null; 
	
	//Table needs to send only part of its layout to list
	public WF_List(String id,boolean isVisible,WF_Context ctx, View v) {
		super(id,v,isVisible,ctx);	
		myContext = ctx;
		gs = GlobalState.getInstance();		
		myW = (TableLayout)v.findViewById(R.id.table);
		

	}
	//TODO: MERGE THESE
	//How about using the Container's panel?? TODO
	public WF_List(String id,boolean isVisible,WF_Context ctx) {
		super(id,new LinearLayout(ctx.getContext()),isVisible,ctx);	
		myW = (LinearLayout)this.getWidget();
		LinearLayout ll = (LinearLayout)myW;
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		myContext = ctx;
		gs = GlobalState.getInstance();
		varCache = gs.getVariableCache();
	}
	
	@Override
	public void addSorter(Sorter s) {
		mySorters.add(s);
	}
	@Override
	public void removeSorter(Sorter s) {
		mySorters.remove(s);
	}
	@Override
	public void addFilter(Filter f) {
		myFilters.add(f);
	}

	@Override
	public void removeFilter(Filter f) {
		myFilters.remove(f);
	}
	
	public List<Listable> getList() {
		return list;
	}
	

	int intC=0;
	boolean drawActive = false;
	public void draw() {
		Log.d("draw","DRAW CALLED "+ (++intC)+" times in list"+this.getId());
		//Log.d("nils","DrawActive "+drawActive);
		if (!drawActive) {
			//Log.d("nils","Settingdrawactive to true from list"+this.getId());
			drawActive = true;
			final ProgressDialog progress = new ProgressDialog(myContext.getContext());

			new Handler().postDelayed(new Runnable() {
				public void run() {
					
					filteredList = list;
					if (myFilters != null) {			
						List<Listable> listx = new ArrayList<Listable>(list);
						for (Filter f:myFilters) {
							f.filter(listx);
						}
						filteredList = listx;
					}
					//Log.d("nils","before sorter: "+System.currentTimeMillis());
					if (mySorters != null) {
						for (Sorter s:mySorters) {
							filteredList = s.sort(filteredList);
						}
					}
					//Log.d("nils","After sorter: "+System.currentTimeMillis());

					//Log.d("nils","in redraw...");
					prepareDraw();
					
					for (Listable l:filteredList) {
						//l.refreshInputFields();
						l.refresh();						
						//Everything is WF_Widgets, so this is safe!					
						myW.addView(((WF_Widget)l).getWidget());
						Log.d("vortex","Drawing: "+l.getLabel());
					} 
					//Log.d("nils","Settingdrawactive to false");
					drawActive = false;

				}
			}, 0);
			
			
		     
		} else
			Log.d("nils","DISCARDED DRAW CALL");


		
	}
	
	protected void prepareDraw() {
		myW.removeAllViews();
	}

}
