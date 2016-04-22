package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;
import com.teraim.fieldapp.utils.Tools;

public class WF_Table extends WF_List {

	//protected final List<Listable> tableRows = new  ArrayList<Listable>(); //Instantiated in constructor
	protected final List<Filter> myFilters=new ArrayList<Filter>();
	protected final List<Sorter> mySorters=new ArrayList<Sorter>();
	protected List<? extends Listable> filteredList;

	protected WF_Context myContext;
	protected GlobalState gs;
	protected VariableConfiguration al;
	private String myVariator;
	private LinearLayout headerV;
	private LayoutInflater inflater ;
	private TableLayout tableView;

	private 		int rowNumber=0;
	private final String ColHeadId = "TableHeader";
	private WF_Table_Row headerRow;
	private Map<String, Map<String, String>> allInstances;
	
	private String varNamePrefix;
	
	//How about using the Container's panel?? TODO
	public WF_Table(String id,boolean isVisible,WF_Context ctx,String namePrefix,String variatorColumn, View tableV) {
		super(id,isVisible,ctx,tableV);	
		this.tableView = (TableLayout)tableV.findViewById(R.id.table);;
		myContext = ctx;
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		al = gs.getVariableConfiguration();
		myVariator = variatorColumn;
		//myTable = new GridView(ctx.getContext());
		
		//Create rows.
		inflater = (LayoutInflater)ctx.getContext().getSystemService
				(Context.LAYOUT_INFLATER_SERVICE); 
		
		//Add the header.
		headerRow = new WF_Table_Row(tableView,ColHeadId,inflater.inflate(R.layout.header_table_row, null),myContext,true);
		//Add a first empty cell 
		headerRow.addEmptyCell(id);
		tableView.addView(headerRow.getWidget());
		
		varNamePrefix = namePrefix;
		
		allInstances = gs.getDb().preFetchValues(myContext.getKeyHash(), namePrefix, myVariator);		
		Log.d("nils","in update entry fields. AllInstances contain "+allInstances.size()+ ": "+allInstances.toString());
		tableView.setStretchAllColumns (true);
		addSorter(new WF_Alphanumeric_Sorter());

	}
	
	Map<String,Set<String>>varIdMap=new HashMap<String,Set<String>>();
	//Creates new rows and adds dataset to each.
	public void addRows(List<List<String>> rows) {
		//Rows are not containing unique entries. only need one of each.
		Map<String,List<String>>uRows = new HashMap<String,List<String>>();
		for (List<String> row:rows) {
			
			String key = al.getEntryLabel(row);
			if (uRows.get(key)==null)
				uRows.put(key,row);							//Smprov:bjork:selectedSpy1
															//smaprov:ek:selectedSpy1
															//smaprov:bjork:selectedSpy2
			//collect all variables existing under one label. GrName_VarGrId_VarId
			Set<String> s = varIdMap.get(key);
			if (s==null){
				s=new HashSet<String>();
				varIdMap.put(key, s);
			}
			s.add(al.getVarName(row));
		}	
		//Now add only the unique entrylabel ones
		for (String rowKey:uRows.keySet()) {
			addRow(uRows.get(rowKey));
		}
	}
	//Create a new row + dataset.
	public void addRow(List<String> row) {		
		WF_Table_Row rowWidget = new WF_Table_Row(tableView,(rowNumber++)+"",inflater.inflate(R.layout.table_row, null),myContext,true);
		rowWidget.addEntryField(row);
		list.add(rowWidget);
	}
	
	public void addColumns(List<String> labels,
			List<String> columnKeyL, String type, String width) {

		boolean useColumKeyAsHeader = labels==null;
		
		//Add as many columns as there are keys. Check if labels are used or if the columnkey should be used.
		if (useColumKeyAsHeader&&labels.size()<columnKeyL.size()) {
			Log.e("vortex","There are too few labels in addColumns! Labels: "+labels.toString());
			o.addRow("");
			o.addRedText("There are too few labels in addColumns! Labels: "+labels.toString());
			return;
		}
		if (columnKeyL==null) {
			Log.e("vortex","columnkeys missing for addColumn!!");
			o.addRow("");
			o.addRow("columnkeys missing for addColumn!!");
			return;
		}
		String k,l;
		for (int i=0;i<columnKeyL.size();i++) {
			
			k = columnKeyL.get(i);
			l = (useColumKeyAsHeader?null:labels.get(i));
			//Add the column
			addColumn(l,k,type,width);
			
		}
		for (String s:columnKeys)
			Log.d("vortex","my columns: "+s);
		
	}
	
	
	//Keep column keys in memory.
	private List<String> columnKeys = new ArrayList<String>();
	
	public void addColumn(String header, String colKey, String type, String width) {
		//Copy the key and add the variator.
		Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
		colHash.put(myVariator, colKey);
		
		//Add header to the header row? Duh!!
		headerRow.addHeaderCell(header);
		
		
		//Create all row entries.
		for (Listable l:list) {
			WF_Table_Row wft = (WF_Table_Row)l;
			wft.addCell(header, colKey, colHash, type, width);
		}
		columnKeys.add(colKey);
	}
	





	@Override
	protected void prepareDraw() {
		tableView.removeAllViews();
		tableView.addView(headerRow.getWidget());
		
	}

	public void addVariableToEveryCell(String variableSuffix,
			boolean displayOut, String format, boolean isVisible,
			boolean showHistorical, String initialValue) {
		

		//Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
		
		
		
		
		//get rows.
		for (Listable l:list) {
			WF_Table_Row wft = (WF_Table_Row)l;
			Set<String> varIds = varIdMap.get(wft.getLabel());
			String varGrId=null;
			if (varIds==null) {
				Log.e("vortex","No variableIds found for "+wft.getLabel());
				return;
			} else {
				Log.d("vortex","varIds contains "+varIds.size()+" variables");
				for (String varGr:varIds) {
					Log.d("vortex","varGr: "+varGr);
					if (varGr.endsWith(variableSuffix)) {
						varGrId = varGr;
						break;
					}
				}
			}
			if (varGrId==null) {
				Log.e("vortex","found no variable with suffix: "+variableSuffix);
				o.addRow("");
				o.addRedText("Could not add variables with suffix: "+variableSuffix+". No instances found. Check spelling and case");
				return;
			}
			//Construct variablename. 
			//String varId = varNamePrefix+Constants.VariableSeparator+varGrId+Constants.VariableSeparator+variableSuffix;
			Log.d("vortex","Adding variable "+varGrId);
			//Get prefetchvalue per variator value. 
			Map<String, String> valueMap = allInstances.get(varGrId);
			
				
			//add to each cell
			
			int columnIndex=0;
			for (WF_Cell cell:wft.getCells()) {
				//Get columnKey
				String colKey = columnKeys.get(columnIndex);

				//Get value for this column
				String prefetchValue = null;
				if (valueMap!=null) {
					prefetchValue = valueMap.get(colKey);
				}
				if (prefetchValue!=null) {
					Log.d("vortex","valueMap: "+valueMap.toString()+" colKey: "+colKey);
					Log.d("vortex","found prefetch value "+prefetchValue);
				}
				cell.addVariable(varGrId, displayOut, format, isVisible, showHistorical,prefetchValue);
				columnIndex++;
			}
		
		}
	}
	

	

	
	

	
	


}
