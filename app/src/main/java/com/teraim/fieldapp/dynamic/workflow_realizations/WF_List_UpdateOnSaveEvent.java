package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.non_generics.Constants;

public class WF_List_UpdateOnSaveEvent extends WF_Static_List implements EventListener,EventGenerator{


	//Precached variables.
	private Map<String,String> varValueMap;

	private class EntryField {
		WF_ClickableField_Selection cfs;
		Set<String> varIDs;

		public EntryField() {
			varIDs = new HashSet<String>();
		}
	}


	private Map<String,EntryField> entryFields = new HashMap<String,EntryField>();
	int index = 0;

	public WF_List_UpdateOnSaveEvent(String id, WF_Context ctx,List<List<String>> rows,boolean isVisible) {
		super(id, ctx,rows,isVisible);
		String namePrefix = al.getFunctionalGroup(rows.get(0));
		Log.d("nils","SkarmGrupp: "+namePrefix);
		varValueMap = gs.getDb().preFetchValuesForAllMatchingKey(gs.getVariableCache().getContext().getContext(), namePrefix);
		ctx.addEventListener(this, EventType.onSave);
		o = GlobalState.getInstance().getLogger();

		for (List<String>r:rows)  {
			addEntryField(r);
		}
	}

	private void addEntryField(List<String> r) {
		EntryField ef;
		String entryLabel = al.getEntryLabel(r);
		if (entryLabel==null||entryLabel.length()==0) {
			Log.d("nils","Skipping empty entrylabel");
			return;
		}
		//Log.d("nils","ADD EntryField with label "+entryLabel);
		ef = entryFields.get(entryLabel);
		if (ef==null) 	{	
			WF_ClickableField_Selection entryF = new WF_ClickableField_Selection(entryLabel,al.getDescription(r),myContext,"C_F_"+index++,true);
			list.add(entryF);	
			ef = new EntryField();
			entryFields.put(entryLabel, ef);
			ef.cfs = entryF;
		}		
		ef.varIDs.add(al.getVarName(r));
		Log.d("vortex","added "+al.getVarName(r)+" to varIDs");
	}



	@Override 
	public Set<Variable> addVariableToEveryListEntry(String varSuffix,boolean displayOut,String format,boolean isVisible, boolean showHistorical,String initialValue) {
		//		List<String>cRow;
		Set<Variable> retVar=null;

		Log.e("nils","in AddVariableToEveryListEntry for "+varSuffix);
		EntryField ef;
		Map<String,EntryField> mapmap = new HashMap<String,EntryField>();
		for (String key:entryFields.keySet()) {
			ef = entryFields.get(key);

			boolean success=false;

			Log.d("vortex","varIDs contain: "+ef.varIDs);
			for (String varID:ef.varIDs) {
				//Log.d("vortex",varID);
				if (varID.endsWith(varSuffix)) {
					mapmap.put(varID,ef);
					success=true;
					Log.e("nils","Found Match for suffix: "+varSuffix+" Match: "+varID);
					break;
				}

			} 

			if (!success) {
				//This variable is either wrong or global.
				Log.d("nils","DID NOT FIND MATCH for suffix: "+varSuffix);
				Variable v= varCache.getVariable(varSuffix,initialValue,-1);
				if (v!=null)
					ef.cfs.addVariable(v, displayOut,format,isVisible,showHistorical);	
				else {
					o.addRow("");
					o.addRedText("Variable with suffix "+varSuffix+" was not found when creating list "+this.getId());
					o.addRow("context: ["+gs.getVariableCache().getContext().toString()+"]");
					String namePrefix = al.getFunctionalGroup(myRows.get(0));
					o.addRow("Group: "+namePrefix);
				}
			}
		}
		if (!mapmap.isEmpty())
			createAsync(mapmap,displayOut,format,isVisible,showHistorical,initialValue);

		return retVar;
	}




	private void createAsync(final Map<String, EntryField> mapmap,final  boolean displayOut,final  String format,final  boolean isVisible,final boolean showHistorical,final String initialValue) {



		/*new Handler().postDelayed(new Runnable() {
			public void run() {
			}

			}, 0);
		}
		 */
		Variable v;

		for (String vs: mapmap.keySet()) {

			//If variabel has value in db, use it.
			if (varValueMap!=null && varValueMap.containsKey(vs)) 
				v = varCache.getCheckedVariable(vs, varValueMap.get(vs),true);	
			else
				//Otherwise use default. 
				v = varCache.getCheckedVariable(vs, initialValue,false);


			//Defaultvalue is either the historical value, null or the current value. 
			//Historical value will be set if the variable does not exist already. If it exists, the current value is used, even if it is null.

			if (v!=null) {
				//Log.d("nils","CreateAsync. Adding variable "+v.getId()+" to "+mapmap.get(vs).cfs.label);
				mapmap.get(vs).cfs.addVariable(v, displayOut,format,isVisible,showHistorical);		
			} else {
				o.addRow("");
				o.addRedText("Variable with suffix "+vs+" was not found when creating list with id "+getId());
				Log.e("nils","Variable with suffix "+vs+" was not found when creating list with id "+getId());
			}
		}
	}




	@Override
	public void addFieldListEntry(String listEntryID,String label,String description) {		
		WF_ClickableField_Selection entryF = new WF_ClickableField_Selection(label,description,myContext,this.getId()+listEntryID,true);
		list.add(entryF);	
		EntryField ef = new EntryField();
		entryFields.put(this.getId()+listEntryID, ef);
		Log.d("vortex","I am now adding listentry "+this.getId()+listEntryID);
		ef.cfs = entryF;
	}

	public Variable addVariableToListEntry(String varNameSuffix,boolean displayOut,String targetField,
			String format, boolean isVisible, boolean showHistorical, String initialValue) {
		String tfName = this.getId()+targetField;
		EntryField ef = entryFields.get(tfName);
		if (ef==null) {
			Log.e("nils","Didnt find entry field "+tfName);
			o.addRow("");
			o.addRedText("Did NOT find entryfield referred to as "+tfName);
			return null;
		}

		String vName = targetField+Constants.VariableSeparator+varNameSuffix;
		Variable v = varCache.getVariable(vName,initialValue,-1);

		if (v==null) {
			//try with simple name.
			o.addRow("Will retry with variable name: "+varNameSuffix);
			v = varCache.getVariable(varNameSuffix,initialValue,-1);
			if (v==null) {
				Log.e("nils","Didnt find variable "+vName+" in AddVariableToList");
				o.addRow("");
				o.addRedText("Did NOT find variable referred to as "+vName+" in AddVariableToList");
				return null;
			}
		}
		ef.cfs.addVariable(v, displayOut,format,isVisible,showHistorical);
		return v;

	}

	@Override
	public void onEvent(Event e) {
		if (e.getProvider().equals(this))
			Log.d("nils","Throwing event that originated from me");
		else {
			Log.d("nils","GOT EVENT!!");
			draw();
		}
		myContext.registerEvent(new WF_Event_OnRedraw(this.getId()));
	}










}
