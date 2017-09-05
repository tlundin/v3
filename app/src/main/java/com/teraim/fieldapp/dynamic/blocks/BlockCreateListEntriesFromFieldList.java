package com.teraim.fieldapp.dynamic.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Alphanumeric_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_IndexOrder_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Instance_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_List_UpdateOnSaveEvent;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TimeOrder_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_OnlyWithValue_Filter;

public class BlockCreateListEntriesFromFieldList extends DisplayFieldBlock {

    private static Map <String,List<List<String>>> cacheMap=new HashMap <String,List<List<String>>>();
    private String id,type,containerId,selectionPattern,selectionField,variatorColumn;
    boolean isVisible = true;
    public BlockCreateListEntriesFromFieldList(String id,String namn, String type,
                                               String containerId, String selectionPattern, String selectionField,String variatorColumn,
                                               String textColor, String bgColor,String verticalFormat,String verticalMargin) {
        super(textColor,bgColor,verticalFormat,verticalMargin);

        this.blockId=id;
        this.id = namn;
        this.type = type;
        this.containerId = containerId;
        this.selectionPattern = selectionPattern;
        this.selectionField = selectionField;
        this.variatorColumn=variatorColumn;
    }

    private static final long serialVersionUID = -5618217142115636962L;

    WF_Static_List myList = null;

    public void create(WF_Context myContext) {
        //prefetch values from db.
        o = GlobalState.getInstance().getLogger();
        associatedFiltersList=null;
        associatedVariablesList=null;

        Container myContainer = myContext.getContainer(containerId);
        if (myContainer != null) {

            if (type.equals("selected_values_list")) {
                o.addRow("This is a selected values type list. Adding Time Order sorter.");
                myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                myList.addSorter(new WF_TimeOrder_Sorter());
                o.addRow("Adding Filter Type: only instantiated");
                myList.addFilter(new WF_OnlyWithValue_Filter(id));
            } else {
                if (type.equals("selection_list")) {
                    o.addRow("This is a selection list. Adding Alphanumeric sorter.");
                    myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                    myList.addSorter(new WF_Alphanumeric_Sorter());
                } else if (type.equals("instance_list")) {
                    o.addRow("instance selection list. Time sorter.");
                    myList = new WF_Instance_List(id, myContext, variatorColumn, isVisible, this);
                    myList.addSorter(new WF_IndexOrder_Sorter());
                } else {
                    //TODO: Find other solution
                    myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                    myList.addSorter(new WF_Alphanumeric_Sorter());
                }
            }

            if (myList != null) {
                myContainer.add(myList);
                myContext.addList(myList);
                //Return true if instance list. Otherwise false (true = list is ready. False=async creation ongoing)

            }
        } else {
            o.addRow("");
            o.addRedText("Failed to add list entries block with id " + blockId + " - missing container " + containerId);
        }

    }
    public void generate(WF_Context myContext) {


        VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
        List<List<String>> rows = cacheMap == null ? null : cacheMap.get(selectionField + selectionPattern);
        if (rows == null) {

            rows = al.getTable().getRowsContaining(selectionField, selectionPattern);
            Log.d("baza", "rows: " + rows.size());
            if (associatedFiltersList!=null) {
                for (AddFilter f : associatedFiltersList) {
                    rows = getRowsContaining(al, rows, f.getSelectionField(), f.getSelectionPattern());
                    Log.d("baza", "filtered rows size: " + rows.size());
                }
            }
            cacheMap.put(selectionField + selectionPattern, rows);

        }
        if (rows == null || rows.size() == 0) {
            Log.e("vortex", "Selectionfield: " + selectionField + " selectionPattern: " + selectionPattern + " returns zero rows! List cannot be created");
            o.addRow("");
            o.addRedText("Selectionfield: " + selectionField + " selectionPattern: " + selectionPattern + " returns zero rows! List cannot be created");
            al.getTable().printTable();
        } else {
            myList.setRows(rows);
            Log.d("baza","try to reuse");

        }

    }




    private List<List<String>> getRowsContaining(VariableConfiguration al,List<List<String>> rows, String columnName, String pattern) {
        String colValue;
        List<List<String>> ret = new ArrayList<List<String>>();
        for (List<String> row: rows) {
            colValue=al.getColumn(columnName, row);
            if (colValue!=null && colValue.equals(pattern)||colValue.matches(pattern)) {
                ret.add(row);
            }
        }
        return ret;
    }



    List<AddVariableToEveryListEntryBlock> associatedVariablesList;
    List<AddFilter>associatedFiltersList;

    public void associateVariableBlock(AddVariableToEveryListEntryBlock bl) {
        if (associatedVariablesList==null)
            associatedVariablesList = new ArrayList<>();
        associatedVariablesList.add(bl);
    }

    public void associateFilterBlock(AddFilter bl) {
        if (associatedFiltersList==null)
            associatedFiltersList=new ArrayList<>();
        associatedFiltersList.add(bl);

    }

    public String getListId() {
        return id;
    }

    public void createVariables(WF_Context myContext) {
        for (AddVariableToEveryListEntryBlock bl:associatedVariablesList){
            bl.create(myContext);
        }
    }
}

