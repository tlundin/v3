package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.Block;
import com.teraim.fieldapp.dynamic.blocks.PageDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.StartBlock;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

//Workflow
public class Workflow implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8806673639097744372L;
	//TODO: List of blocks needs to be a map.

	private List<Block> blocks;
	private String name,label,applicationName,applicationVersion;
	private DB_Context mContext=null;




	public Workflow() {

	}

	public Workflow(String bundleName) {
		this.applicationName=bundleName;
	}


	public List<Block> getBlocks() {
		return blocks;
	}

	public List<Block> getCopyOfBlocks() {
		if (blocks==null)
			return null;
		List<Block> ret = new ArrayList<Block>(blocks);
		return ret;
	}

	public void addBlocks(List<Block> _blocks) {
		blocks = _blocks;
	}

	public String getName() {
		if (name==null) {
			if (blocks!=null && blocks.size()>0)
				name = ((StartBlock)blocks.get(0)).getName();

		}
		return name;
	}

	public String getLabel() {
		if (label==null) {
			if (blocks!=null && blocks.size()>1 && blocks.get(1) instanceof PageDefineBlock)
				label = ((PageDefineBlock)blocks.get(1)).getPageLabel();
		}
		return label;
	}

	public boolean isBackAllowed() {
		if (blocks!=null && blocks.size()>1 && blocks.get(1) instanceof PageDefineBlock)
			return ((PageDefineBlock)blocks.get(1)).goBackAllowed();

		Log.e("vortex","failed to find pagedefineblock");
		return true;
	}

	public Fragment createFragment(String templateName) {
		Fragment f = null;
		try {
			Class<?> cs = Class.forName("com.teraim.fieldapp.dynamic.templates."+templateName);
			f = (Fragment)cs.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e3) {
			e3.printStackTrace();
		}
		return f;
	}

	public String getTemplate() {
		for (Block b:blocks) {
			if (b instanceof PageDefineBlock) {
				PageDefineBlock bl = (PageDefineBlock)b;
				return bl.getPageType();
			}
		}
		Log.d("vortex","Could not find a PageDefineBlock for workflow "+this.getName());

		return null;
	}

	public String getApplication() {
		return applicationName;
	}


	public List<EvalExpr> getContext() {
		if (blocks!=null && blocks.size()>0) {
			StartBlock bl = ((StartBlock)blocks.get(0));
			if (bl==null) {
				Log.e("vortex","Missing Startblock...context will remain same.");
				return null;
			}
			else
				return bl.getWorkFlowContext();
		} 

		Log.e("vortex","startblock missing");
		return null;
	}

	
}





/*	

									else {
										//Variable or function. need to evaluate first..
										Variable v=null;// = getVariableConfiguration().getVariableInstance(val);
										String varVal=null;
										//if (v==null) {
										//Parse value..either constant, function or variable.

										List<TokenizedItem> tokens = myExecutor.findTokens(val, null);
										if (tokens!=null && !tokens.isEmpty()) {										
											TokenizedItem firstToken = tokens.get(0);
											if (firstToken.getType()==TokenType.variable) {
												Log.d("vortex","Found variable!");
												v = firstToken.getVariable();
												varVal = v.getValue();
												if (varVal==null) {
													o.addRow("");
													o.addRedText("One of the variables used in current context("+v.getId()+") has no value in database");
													Log.e("nils","var was null or empty: "+v.getId());
													err = "One of the variables used in current context("+v.getId()+") has no value in database";
													contextError=true;
												}
											} else if (firstToken.getType().getParent()==TokenType.function) {
												Log.d("vortex","Found function!");
												SubstiResult subsRes = myExecutor.substituteForValue(tokens, val, false);
												if (subsRes!=null) {
													varVal = subsRes.result;
												} else {
													Log.e("vortex","subsresult was null for function"+val+" in evalContext");
													contextError=true;
													err = "subsresult was null for function"+val+" in evalContext";
												}
											} else if (firstToken.getType()==TokenType.literal) {
												Log.d("vortex","Found literal!");
												varVal = val;
											} else {
												Log.e("vortex","Could not find "+firstToken.getType().name());
												contextError=true;
												err = "Could not find "+firstToken.getType().name();
											}

										} else {
											o.addRow("");
											o.addRedText("Could not evaluate expression "+val+" in context");
											Log.e("vortex","Could not evaluate expression "+val+" in context");
											contextError=true;
											err="Could not evaluate expression "+val+" in context";
										}

										//} else 
										//	varVal = v.getValue();

										if(!contextError) {
											keyHash.put(arg, varVal);
											rawHash.put(arg,v);
											Log.d("nils","Added "+arg+","+varVal+" to current context");
											if (v!=null)
												v.setKeyChainVariable(arg);
											//update status menu

										}


									}
								}
							}
						}
					} else {
						Log.d("nils","Found empty or null pair");
						contextError=true;
						err="Found empty or null pair";
					}

				} 

			}
		}

		if (keyHash!=null && !contextError && !keyHash.isEmpty()) {
			o.addRow("");
			o.addYellowText("Context now: "+keyHash.toString());
			return new CHash(keyHash,rawHash);
		}
		else
			return new CHash(err);

	}
}
 */





