package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

public class WF_Table_Row extends WF_Widget implements Listable,Comparable<Listable> {
	List<String> myRow;
	List<WF_Cell> myColumns;
	private WF_Context myContext;
	private LinearLayout myBody;
	private TextView headerT;
	private String id;
	private TableLayout myTable;

	public WF_Table_Row(TableLayout myTable,String id,View v,WF_Context ctx,boolean isVisible) {
		super(id,v,isVisible,ctx);
		myColumns=null;
		myContext = ctx;
		headerT = (TextView)v.findViewById(R.id.headerT);
		this.id=id;
		this.myTable = myTable;
		//Log.d("vortex","Added header "+this.getLabel() );

	}
	//Add the Entry (row header)
	//Note that the row here could be any row belonging to the entry. 
	public void addEntryField(List<String> row) {
		myRow=row;
		String label = getLabel();
		if (label==null) {
			Log.e("vortex","label null for row with id "+id);
			label="*null*";
		}
		//Headertext is not there for header row..
		if (headerT!=null)
			headerT.setText(label);
		else
			headerT.setVisibility(View.GONE);
	}


	@Override
	public String getSortableField(String columnId) {
		return al.getTable().getElement(columnId, myRow);
	}


	//Not supported.
	@Override
	public long getTimeStamp() {
		return 0;
	}



	@Override
	public boolean hasValue() {
		if (myColumns==null)
			return false;
		for (WF_Cell w:myColumns)
			if (w.hasValue())
				return true;

		return false;

	}



	@Override
	public String getLabel() {
		return al.getEntryLabel(myRow);
	}

	@Override
	public void refresh() {
		if (myColumns==null)
			return ;
		for (WF_Cell w:myColumns)
			w.refresh();
	}

	//Return any cells assoicated variables since each cell contain same.
	@Override
	public Set<Variable> getAssociatedVariables() {
		if (myColumns!=null && !myColumns.isEmpty()) {
			return myColumns.get(0).getAssociatedVariables();
		}
		return null;
	}


	@Override
	public int compareTo(Listable other) {
		return this.getLabel().compareTo(other.getLabel());
	}

	public void addNoClickHeaderCell(String label, String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header,null);
		TextView tv=(TextView)emptyCell.findViewById(R.id.headerT);
		tv.setText(label);
		((TableRow)this.getWidget()).addView(emptyCell);
		if (backgroundColor!=null)
			emptyCell.setBackgroundColor(Color.parseColor(backgroundColor));
		if (textColor!=null)
			tv.setTextColor(Color.parseColor(textColor));
	}

	int headerIndex=1;
	int selectedColumnIndex =-1;
	//Add a cell of purely graphical nature.
	public void addHeaderCell(String label, String backgroundColor, String textColor) {
		View headerC = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header,null);
		TextView headerT = (TextView)headerC.findViewById(R.id.headerT);
		headerT.setText(label);
		((TableRow)this.getWidget()).addView(headerC);

		headerC.setOnClickListener(new OnClickListener() {
			final int myHeaderIndex=headerIndex;

			@Override
			public void onClick(View v) {
				Log.d("vortex","column "+myHeaderIndex+" clicked!");
				toggleToggleState();
				selectedColumnIndex = getToggleState()?myHeaderIndex:-1;
					for (int i = 1; i < headerIndex; i++) {
						if (i != myHeaderIndex) {
							myTable.setColumnCollapsed(i, getToggleState());
						}
					}
			}

		});
		if (backgroundColor!=null)
			headerC.setBackgroundColor(Color.parseColor(backgroundColor));
		if (textColor!=null)
			headerT.setTextColor(Color.parseColor(textColor));
		//If no column selected, selectedIndex is -1

		headerIndex++;
	}
	private boolean iAmCollapsed =false;

	private boolean getToggleState() {
		return iAmCollapsed;

	}
	public int getSelectedColumn() {
		return selectedColumnIndex;
	}
	private void toggleToggleState() {
		iAmCollapsed =!iAmCollapsed;
	}




	//Add a Vortex Cell.
	public void addCell(String colHeader, String colKey, Map<String,String> columnKeyHash, String type, String width) {

		if (myColumns==null)
			myColumns = new ArrayList<WF_Cell>();
			WF_Cell widget;
			if ("simple".equals(type))
				widget = new WF_Simple_Cell_Widget(columnKeyHash,getLabel(), al.getDescription(myRow),
						myContext, this.getId()+colKey,true);
				
			else {
				widget = new WF_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow),
						myContext, this.getId() + colKey, true);
				int widthI = 50;
				if (width!=null) try {
					widthI = Integer.parseInt(width);
				} catch (NumberFormatException e) {};
				widget.getWidget().setMinimumWidth(widthI);
			}
			
			
			//TODO:ADD FORMAT! NULL BELOW
			//SHOW HISTORICAL IS TRUE!
			//Variable v= al.getVariableUsingKey(columnKeyHash, this.getKey());
			//if (v!=null)
			//	widget.addVariable(v, true,null,true,true);
			myColumns.add(widget);
			((TableRow)getWidget()).addView(widget.getWidget());
			//Log.d("feodor","Row now has "+((TableRow) getWidget()).getChildCount()+" children");
	}

	public TextView addAggregateTextCell(String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_text_aggregate,null);
		View bg = (View)emptyCell.findViewById(R.id.outputContainer);
		TextView tv=(TextView)emptyCell.findViewById(R.id.contentT);

		((TableRow)this.getWidget()).addView(emptyCell);
		bg.setBackgroundColor(Color.parseColor(backgroundColor));
		tv.setTextColor(Color.parseColor(textColor));
//		Log.d("vortex","var for row "+this.getLabel());
//		Log.d("vortex","v: "+al.getVarName(myRow)+" key: "+al.getKeyChain(myRow));
		return tv;
	}

	public CheckBox addAggregateLogicalCell(String backgroundColor, String textColor) {
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_logical_aggregate,null);

		//View bg = (View)emptyCell.findViewById(R.id.outputContainer);
		CheckBox cb=(CheckBox) emptyCell.findViewById(R.id.contentT);

		((TableRow)this.getWidget()).addView(emptyCell);
		//((TableRow)this.getWidget()).setBackgroundColor(Color.parseColor(backgroundColor));
		emptyCell.setBackgroundColor(Color.parseColor(backgroundColor));
		cb.setTextColor(Color.parseColor(textColor));

//		Log.d("vortex","var for row "+this.getLabel());
//		Log.d("vortex","v: "+al.getVarName(myRow)+" key: "+al.getKeyChain(myRow));
		return cb;
	}


	public List<WF_Cell> getCells() {
		return myColumns;
	}
	
	@Override
	public String getKey() {
		return getLabel();
	}
	@Override
	public Map<String, String> getKeyChain() {
		// TODO Auto-generated method stub
		return null;
	}


}
