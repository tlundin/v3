package com.teraim.fieldapp.loadermodule.configurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Table.ErrCode;
import com.teraim.fieldapp.loadermodule.CSVConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public class VariablesConfiguration extends CSVConfigurationModule {

	
	private final SpinnerDefinition sd=new SpinnerDefinition();
	private LoggerI o;
	private int pGroupIndex = 1;
	private int pNameIndex = 2;
	private Table myTable=null;
	private List<String> cheaderL;
	private boolean scanHeader;
	private String[] groupsFileHeaderS;
	private Map<String, List<List<String>>> groups;
	private int nameIndex;
	private int groupIndex;


	public VariablesConfiguration(PersistenceHelper globalPh,PersistenceHelper ph, String server, String bundle, LoggerI debugConsole) {
		super(globalPh,ph, Source.internet, server+bundle.toLowerCase()+"/",VariablesConfiguration.NAME,"Variables module      ");	 
		this.o = debugConsole;
		o.addRow("Parsing Variables.csv file");

	}

	public static String NAME = "Variables";
	

	@Override
	public float getFrozenVersion() {
		//Force reload of this file if Groups has been loaded.
		if (GroupsConfiguration.getSingleton()!=null) {
			return -1;
		}
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE));
	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE,version);

	}

	@Override
	public boolean isRequired() {
		return true;
	}

	GroupsConfiguration gc=null;
	
	@Override
	protected LoadResult prepare() throws IOException, Dependant_Configuration_Missing {		

		cheaderL = new ArrayList<String>();

		scanHeader = true;

		//check if there is a groups configuration.
		if ((gc=GroupsConfiguration.getSingleton()) != null) {
			groupsFileHeaderS = gc.getGroupFileColumns();
			groups = gc.getGroups();
			nameIndex = gc.getNameIndex();
			groupIndex = gc.getGroupIndex();
		} else {
			throw new Dependant_Configuration_Missing("Groups");
		}
		return null;
	}



	@Override
	public LoadResult parse(String row, Integer currentRow) throws IOException {


		if (scanHeader && row!=null) {
			Log.d("vortex","header is: "+row);			
			String[] varPatternHeaderS = row.split(",");
			if (varPatternHeaderS==null||varPatternHeaderS.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("Header corrupt in Variables.csv: "+varPatternHeaderS);
				return new LoadResult(this,ErrorCode.ParseError,"Corrupt header");
			}
			//Remove duplicte group column and varname if group file present. 
			
			if (gc!=null) {
				boolean foundFunctionalGroupHeader=false,foundVarNameHeader=false;
				Collections.addAll(cheaderL,groupsFileHeaderS);
				Log.d("vortex","header now "+cheaderL.toString());
				
				Iterator<String> it = cheaderL.iterator();
				while(it.hasNext()) {
					String header = it.next();
					if (header.equals(VariableConfiguration.Col_Functional_Group)) {
						Log.d("vortex","found column Functional Group "); 
						foundFunctionalGroupHeader=true;
						it.remove();
						}
					else
						if (header.equals(VariableConfiguration.Col_Variable_Name)) {
							Log.d("vortex","found column VariableName");
							foundVarNameHeader=true;
							it.remove();
						}
				}
				if (!foundFunctionalGroupHeader||!foundVarNameHeader) {
					o.addRow("");
					o.addRedText("Could not find required columns "+VariableConfiguration.Col_Functional_Group+" or "+VariableConfiguration.Col_Variable_Name);
					return new LoadResult(this,ErrorCode.ParseError,"Corrupt header");
				}

			}
			List<String> vheaderL = new ArrayList<String>(trimmed(varPatternHeaderS));
			vheaderL.addAll(cheaderL);
			myTable = new Table(vheaderL,0,pNameIndex);
			scanHeader=false;

		} else {
			List<List<String>> elems;


			String[] r = Tools.split(row);

			if (r==null || r.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("found null or too short row at "+currentRow+" in config file..skipping");
				return new LoadResult(this,ErrorCode.ParseError,"Parse error, row: "+currentRow);
			} else {	
				for(int i=0;i<r.length;i++) {
					if (r[i]!=null)
						r[i] = r[i].replace("\"", "");

				}
				String pGroup = r[pGroupIndex];
				List<String> trr=trimmed(r);
				if (pGroup==null || pGroup.trim().length()==0) {
					//Log.d("nils","found variable "+r[pNameIndex]+" in varpattern");							
					myTable.addRow(trr);
					//o.addRow("Generated variable(1): ["+r[pNameIndex]+"]");
					//Log.d("vortex","Generated variable ["+r[pNameIndex]+"] ROW:\n"+row);
				} else {
					//Log.d("nils","found group name: "+pGroup);
					elems = groups.get(pGroup);
					String varPatternName = r[pNameIndex];
					if (elems==null) {
						//If the variable has a group,add it 
						//Log.d("nils","Group "+pGroup+" in line#"+rowC+" does not exist in config file. Will use name: "+varPatternName);								
						String name = pGroup.trim()+Constants.VariableSeparator+varPatternName.trim();
						//o.addRow("Generated variable(2): ["+name+"]");
						trr.set(pNameIndex, name);
						myTable.addRow(trr);
					} else {
						for (List<String>elem:elems) {
							//Go through all rows in group. Generate variables.
							String cFileNamePart = elem.get(nameIndex);

							if (varPatternName==null) {
								o.addRow("");
								o.addRedText("varPatternNamepart evaluates to null at line#"+currentRow+" in varpattern file");
							} else {
								String fullVarName = pGroup.trim()+Constants.VariableSeparator+(cFileNamePart!=null?cFileNamePart.trim()+Constants.VariableSeparator:"")+varPatternName.trim();
								//Remove duplicate elements from Config File row.
								//Make a copy.
								List<String>elemCopy = new ArrayList<String>(elem);
								elemCopy.remove(nameIndex);
								elemCopy.remove(groupIndex);
								List<String>varPatternL = new ArrayList<String>(trimmed(r));
								varPatternL.addAll(elemCopy);
								//Replace name column with full name.
								varPatternL.set(pNameIndex, fullVarName);
								//o.addRow("Generated variable(3): ["+fullVarName+"]");
								ErrCode err = myTable.addRow(varPatternL);
								if (err!=ErrCode.ok) {
									switch (err) {
									case keyError:
										o.addRow("");
										o.addRedText("KEY ERROR!");
										break;
									case tooFewColumns:
										o.addRow("");
										o.addRedText("TOO FEW COLUMNS!");
										return new LoadResult(this,ErrorCode.ParseError);
									case tooManyColumns:
										o.addRow("");
										o.addRedText("TOO MANY COLUMNS!");
										o.addRedText("row not inserted. Something wrong at line "+currentRow);
										break;
									}
									
								}
							}
						}
					}
				}
			}

		}


		return null;
	}

	

	private List<String> trimmed(String[] r) {
		ArrayList<String> ret = new ArrayList<String>();
		for(int i=0;i<Constants.VAR_PATTERN_ROW_LENGTH;i++)
			ret.add(r[i]);
		return ret;
	}

	@Override
	public void setEssence() {
		essence = myTable;
	}

}
