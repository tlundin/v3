package com.teraim.fieldapp.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.fieldapp.dynamic.blocks.AddGisFilter;
import com.teraim.fieldapp.dynamic.blocks.AddGisLayerBlock;
import com.teraim.fieldapp.dynamic.blocks.AddGisPointObjects;
import com.teraim.fieldapp.dynamic.blocks.RuleBlock;
import com.teraim.fieldapp.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToListEntry;
import com.teraim.fieldapp.dynamic.blocks.Block;
import com.teraim.fieldapp.dynamic.blocks.BlockAddColumnsToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockAddVariableToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTableEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockDeleteMatchingVariables;
import com.teraim.fieldapp.dynamic.blocks.ButtonBlock;
import com.teraim.fieldapp.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.fieldapp.dynamic.blocks.ContainerDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateImageBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateListFilter;
import com.teraim.fieldapp.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.fieldapp.dynamic.blocks.DisplayValueBlock;
import com.teraim.fieldapp.dynamic.blocks.JumpBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuHeaderBlock;
import com.teraim.fieldapp.dynamic.blocks.NoOpBlock;
import com.teraim.fieldapp.dynamic.blocks.PageDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.RoundChartBlock;
import com.teraim.fieldapp.dynamic.blocks.SetValueBlock;
import com.teraim.fieldapp.dynamic.blocks.SetValueBlock.ExecutionBehavior;
import com.teraim.fieldapp.dynamic.blocks.TextFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.VarValueSourceBlock;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnFlowExecuted;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;
import com.teraim.fieldapp.gis.Tracker;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.Expressor.Atom;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Tools;

/**
 * Executor - executes workflow blocks.  
 * Copyright Teraim 2015.
 * Core class in the Vortex Engine.
 * Redistribution and changes only after agreement with Teraim.
 */


public abstract class Executor extends Fragment implements AsyncResumeExecutorI {


	public static final String STOP_ID = "STOP";

	public static final String REDRAW_PAGE = "executor_redraw_page";

	protected Workflow wf;

	//Extended context.
	protected WF_Context myContext;


	//Keep track of input in below arraylist.

	protected final Map<Rule,Boolean>executedRules = new LinkedHashMap<Rule,Boolean>();	

	protected List<Rule> rules = new ArrayList<Rule>();


	protected abstract List<WF_Container> getContainers();
	public abstract void execute(String function, String target);

	protected GlobalState gs;

	protected LoggerI o;
	private IntentFilter ifi;
	protected BroadcastReceiver brr;
	private Map<String,String> jump= new HashMap<String,String>();
	private Set<Variable> visiVars;

	protected VariableConfiguration al;

	protected VariableCache varCache;

	private int savedBlockPointer=-1;

	private List<Block> blocks;

	//Create pop dialog to display status.
	private ProgressDialog pDialog; 

	protected boolean survivedCreate = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		survivedCreate = false;
		//If app has been murdered brutally, restart it. 
		if(!Start.alive) {
			Intent intent = new Intent(this.getActivity(), Start.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} else {


			gs = GlobalState.getInstance();
			if (gs == null) {
				Log.e("vortex","globalstate null in executor, exit");
				return;
			}
			al = gs.getVariableConfiguration();
			varCache=gs.getVariableCache();
			o = gs.getLogger();
			

			ifi = new IntentFilter();
			ifi.addAction(REDRAW_PAGE);
			//ifi.addAction(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
			//This receiver will forward events to the current context.
			//Bluetoothmessages are saved in the global context by the message handler.
			brr = new BroadcastReceiver() {
				@Override
				public void onReceive(Context ctx, Intent intent) {
					Log.d("nils","GETS HERE::::::");
					if (intent.getAction().equals(REDRAW_PAGE)) {

						myContext.registerEvent(new WF_Event_OnSave(Constants.SYNC_ID));
						Log.d("nils","Redraw page received in Executor. Sending onSave event.");
						gs.sendEvent(MenuActivity.REDRAW);
					} 
					/*
				else if (intent.getAction().equals(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED)) {
					Log.d("nils","New bluetoot message received event!");
					myContext.registerEvent(new WF_Event_OnBluetoothMessageReceived());
				}
					 */

				}
			};
			
			myContext = new WF_Context((Context)this.getActivity(),this,R.id.content_frame);
			wf = getFlow();
			if (wf == null) {
				Log.e("Vortex","WF was null in Executor. Exiting...");
				return;
			} else {
				myContext.setWorkflow(wf);
				Log.d("nils","GETS TO ONCREATE EXECUTOR FOR WF "+wf.getLabel());
				survivedCreate = true;
			}

		}

	}



	@Override
	public void onResume() {
		Log.d("vortex","in Executor onResume");

		gs = GlobalState.getInstance();
		if (gs!=null) {
			gs.getContext().registerReceiver(brr, ifi);
			if(myContext!=null&&myContext.hasGPSTracker())
				gs.getTracker().startScan(gs.getContext());

		}
		super.onResume();
	}


	@Override
	public void onPause()
	{
		Log.d("Vortex", "onPause() for executor");
		//Stop listening for bluetooth events.
		if (brr!=null && gs!=null)
			gs.getContext().unregisterReceiver(brr);
		if (gs!=null&&myContext!=null&&myContext.hasGPSTracker())
			gs.getTracker().stopUsingGPS();
		
		
		
		super.onPause();

	}

	protected Workflow getFlow() {
		Workflow wf=null;

		//Find out the name of the workflow to execute.
		Bundle b = this.getArguments();
		if (b!=null) {
			String name = b.getString("workflow_name");
			String statusVariable = b.getString("status_variable");

			if (statusVariable !=null) {
				myContext.setStatusVariable(b.getString("status_variable"));
				//Add onSaveListener for the statusvariable. Change to "1" when first value saved.
				//Log.e("vortex","Added onsave listener for "+b.getString("status_variable"));
				myContext.addEventListener(new EventListener() {
					@Override
					public void onEvent(Event e) {
						Log.e("vortex","Received onSave in statusvariable change when first save event!");
						String statusVar = myContext.getStatusVariable();
						Variable statusVariable =null;
						if (statusVar!=null)
							statusVariable = varCache.getVariable(statusVar);
						if (statusVariable!=null && statusVariable.getValue()!=null && statusVariable.getValue().equals(Constants.STATUS_INITIAL)) {
							statusVariable.setValue(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
							Log.e("vortex","GOTZZZ");
						} else
							Log.e("vortex","TURKEY");
						myContext.removeEventListener(this);
					}
				}, EventType.onSave);
			}
			if (name!=null && name.length()>0) 
				wf = gs.getWorkflow(name);

			if (wf==null&&name!=null&&name.length()>0) {
				o.addRow("");
				o.addYellowText("Workflow "+name+" NOT found!");
				return null;
			} 

		} else
			Log.e("vortex","BUNDLE WAS NULL!!!!");
		return wf;
	}


	/**
	 * Execute the workflow.
	 */
	protected void run() {
		o.addRow("");
		o.addRow("");
		o.addRow("*******EXECUTING: "+wf.getLabel());	
		Log.d("vortex","in Executor run()");
		
		myContext.resetState();
		
		DB_Context wfHash = DB_Context.evaluate(wf.getContext());
		//TODO: Erase below if.
		if (!wfHash.equals(gs.getVariableCache().getContext())) {
			Log.e("vortex","MYSTICAL MISMATCH: "+wfHash.toString()+" and "+gs.getVariableCache().getContext().toString());
			gs.setDBContext(wfHash);	
		}

		myContext.setHash(wfHash);		
		//Need to write down all variables in wf context keyhash.
		List<String> contextVars=null;
		if (wf.getContext()!=null) {
			for (EvalExpr e:wf.getContext()) {
				if (e instanceof Atom) {
					if (((Atom)e).isVariable()) {
						Log.d("vortex","Found variable in context:"+e.toString());
						if (contextVars==null)
							contextVars = new ArrayList<String>();
						contextVars.add(e.toString());
					}
				}
			}
		}
		myContext.setContextVariables(contextVars);
		gs.setCurrentWorkflowContext(myContext);		
		gs.sendEvent(MenuActivity.REDRAW);
		visiVars = new HashSet<Variable>();
		//LinearLayout my_root = (LinearLayout) findViewById(R.id.myRoot);		
		blocks = wf.getCopyOfBlocks();
		Log.d("vortex","*******EXECUTING: "+wf.getLabel());
		Log.d("vortex","myHash: "+wfHash);
		execute(0);
	}
	private void execute(int blockP) {

		boolean notDone = true;
		Set<Variable>blockVars;
		boolean hasDrawer=false;
		myContext.clearExecutedBlocks();
		try {

			while(notDone) {

				if (blockP>=blocks.size()) {
					notDone=false;
					Log.d("vortex","EXECUTION STOPPED ON BLOCK "+(blockP-1));
					break;
				}
				Block b = blocks.get(blockP);
				Log.d("vortex","In execute with block "+b.getClass().getSimpleName()+" ID: "+b.getBlockId());

				//Add block to list of executed blocks.
				try { myContext.addExecutedBlock(Integer.parseInt(b.getBlockId())); } catch (NumberFormatException e) {Log.e("vortex","blockId was not Integer");}

				if (b instanceof PageDefineBlock) {
					PageDefineBlock bl = (PageDefineBlock)b;
					Log.d("vortex","Found pagedefine!");
					if (bl.hasGPS()) {
						myContext.enableGPS();
						o.addRow("GPS scanning started");
						Log.d("vortex","GPS scanning started");

						Tracker.ErrorCode code = gs.getTracker().startScan(gs.getContext());
						o.addRow("GPS SCANNER RETURNS: "+code.name());
						Log.d("vortex","got "+code.name());
					} //else
					//	Log.e("vortex","This has no GPS");


				}

				else if (b instanceof ContainerDefineBlock) {
					o.addRow("");
					o.addYellowText("ContainerDefineBlock found "+b.getBlockId());
					String id = (((ContainerDefineBlock) b).getContainerName());
					if (id!=null) {
						if (myContext.getContainer(id)!=null) {
							o.addRow("found template container for "+id);
						}
						else {
							o.addRow("");
							o.addRedText("Could not find container "+id+" in template!");
						}

					}
				}			
				else if (b instanceof ButtonBlock) {
					o.addRow("");
					o.addYellowText("ButtonBlock found "+b.getBlockId());
					ButtonBlock bl = (ButtonBlock) b;
					bl.create(myContext);
				}			
				else if (b instanceof TextFieldBlock) {
					o.addRow("");
					o.addYellowText("CreatTextBlock found "+b.getBlockId());
					TextFieldBlock bl = (TextFieldBlock) b;
					bl.create(myContext);
				}
				else if (b instanceof CreateSortWidgetBlock) {
					o.addRow("");
					o.addYellowText("CreateSortWidgetBlock found "+b.getBlockId());
					CreateSortWidgetBlock bl = (CreateSortWidgetBlock) b;
					bl.create(myContext);
				}/*
			else if (b instanceof ListFilterBlock) {
				o.addRow("");
				o.addYellowText("ListFilterBlock found");
				ListFilterBlock bl = (ListFilterBlock)b;
				bl.create(myContext);
			}*/

				else if (b instanceof CreateEntryFieldBlock) {
					o.addRow("");
					o.addYellowText("CreateEntryFieldBlock found "+b.getBlockId());
					CreateEntryFieldBlock bl = (CreateEntryFieldBlock)b;
					Log.d("NILS","CreateEntryFieldBlock found");
					Variable v=bl.create(myContext);
					if (v!=null)
						visiVars.add(v);
				}
				else if (b instanceof AddSumOrCountBlock) {
					o.addRow("");
					o.addYellowText("AddSumOrCountBlock found "+b.getBlockId());
					AddSumOrCountBlock bl = (AddSumOrCountBlock)b;
					bl.create(myContext);
				}
				else if (b instanceof DisplayValueBlock) {
					o.addRow("");
					o.addYellowText("DisplayValueBlock found "+b.getBlockId());
					DisplayValueBlock bl = (DisplayValueBlock)b;
					bl.create(myContext);
				}
				else if (b instanceof AddVariableToEveryListEntryBlock) {
					o.addRow("");
					o.addYellowText("AddVariableToEveryListEntryBlock found "+b.getBlockId());
					AddVariableToEveryListEntryBlock bl = (AddVariableToEveryListEntryBlock)b;
					blockVars = bl.create(myContext);
					if (blockVars!=null)
						visiVars.addAll(blockVars);

				}
				else if (b instanceof BlockCreateListEntriesFromFieldList) {
					o.addRow("");
					o.addYellowText("BlockCreateListEntriesFromFieldList found "+b.getBlockId());
					BlockCreateListEntriesFromFieldList bl = (BlockCreateListEntriesFromFieldList)b;
					bl.create(myContext);
				}
				else if (b instanceof BlockCreateTableEntriesFromFieldList) {
					o.addRow("");
					o.addYellowText("BlockCreateTableEntriesFromFieldList found "+b.getBlockId());
					BlockCreateTableEntriesFromFieldList bl = (BlockCreateTableEntriesFromFieldList)b;
					bl.create(myContext);
				}

				else if (b instanceof BlockAddColumnsToTable) {
					o.addRow("");
					o.addYellowText("BlockAddColumn(s)ToTable found "+b.getBlockId());
					BlockAddColumnsToTable bl = (BlockAddColumnsToTable)b;
					bl.create(myContext);
				}

				else if (b instanceof BlockAddVariableToTable) {
					o.addRow("");
					o.addYellowText("BlockAddVariableToTable(s)ToTable found "+b.getBlockId());
					BlockAddVariableToTable bl = (BlockAddVariableToTable)b;
					bl.create(myContext);
				}

				else if (b instanceof AddVariableToEntryFieldBlock) {
					o.addRow("");
					o.addYellowText("AddVariableToEntryFieldBlock found "+b.getBlockId());
					AddVariableToEntryFieldBlock bl = (AddVariableToEntryFieldBlock)b;
					Variable v = bl.create(myContext);
					if (v!=null)
						visiVars.add(v);

				}
				else if (b instanceof AddVariableToListEntry) {
					o.addRow("");
					o.addYellowText("AddVariableToListEntry found "+b.getBlockId());
					AddVariableToListEntry bl = (AddVariableToListEntry)b;
					Variable v = bl.create(myContext);
					//TODO: REMOVE THIS??
				}
				else if (b instanceof AddEntryToFieldListBlock) {
					o.addRow("");
					o.addYellowText("AddEntryToFieldListBlock found "+b.getBlockId());
					AddEntryToFieldListBlock bl = (AddEntryToFieldListBlock)b;
					bl.create(myContext);

				}
				else if (b instanceof NoOpBlock) {
					o.addRow("");
					o.addYellowText("Noopblock found and skipped! "+b.getBlockId());
				}
				else if (b instanceof JumpBlock) {
					o.addRow("");
					o.addYellowText("Jumpblock found "+b.getBlockId());
					JumpBlock bl = (JumpBlock)b;			
					jump.put(bl.getBlockId(), bl.getJumpTo());
				}
				else if (b instanceof CreateImageBlock) {
					o.addRow("");
					o.addYellowText("CreateImageBlock found "+b.getBlockId());
					CreateImageBlock bl = (CreateImageBlock)b;			
					bl.create(myContext);
				}
				else if (b instanceof SetValueBlock) {
					final SetValueBlock bl = (SetValueBlock)b;
					//final List<TokenizedItem> tokens = gs.getRuleExecutor().findTokens(bl.getFormula(),null);
					if (bl.getBehavior()!=ExecutionBehavior.constant) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								if (!e.getProvider().equals(bl.getBlockId())) {
									Variable v = varCache.getVariable(bl.getMyVariable());
									if (v!=null) {
										//String	eval = bl.evaluate(gs,bl.getFormula(),tokens,v.getType()== DataType.text);
										String val = v.getValue();
										String eval = bl.getEvaluation();

										o.addRow("Variable: "+v.getId()+" Current val: "+val+" New val: "+eval);
										Log.d("vortex","Variable: "+v.getId()+" Current val: "+val+" New val: "+eval);
										if (!(eval == null && val == null)) {
											if (eval == null && val != null || val == null && eval != null || !val.equals(eval)) {
												//Remove .0 

												v.setValue(eval);
												o.addRow("");
												o.addYellowText("Value has changed to or from null in setvalueblock OnSave for block "+bl.getBlockId());
												Log.d("Vortex","Value has changed to or from null in setvalueblock OnSave for block "+bl.getBlockId());

												o.addRow("");
												o.addYellowText("BEHAVIOR: "+bl.getBehavior());
												if (bl.getBehavior()==ExecutionBehavior.update_flow) {
													if (myContext.myEndIsNear()) {
														Log.d("vortex","Disregarding event since flow reexecute already in progress.");
														return;
													}
													Log.d("nils","Variable has sideEffects...re-executing flow");
													myContext.setMyEndIsNear();
													new Handler().postDelayed(new Runnable() {
														public void run() {
															//myContext.resetState();

															Set<Variable> previouslyVisibleVars = visiVars;
															Executor.this.run();
															for (Variable v:previouslyVisibleVars) {
																Log.d("nils","Previously visible: "+v.getId());
																boolean found = false;
																for(Variable x:visiVars) {									
																	found = x.getId().equals(v.getId());
																	if (found)
																		break;
																}

																if (!found) {
																	Log.d("nils","Variable "+v.getId()+" not found.Removing");
																	v.deleteValue();
																}

															}	
														}
													}, 0);
												}
											}
										}
									} else {
										Log.e("vortex","variable null in SetValueBlock");
										o.addRow("Setvalueblock variable "+bl.getMyVariable()+" not found or missing columns");
									}

								} else
									o.addRow("Discarded onSave Event from myself in SetValueBlock "+bl.getBlockId());
							}
						};
						Log.d("nils","Adding eventlistener for the setvalue block");
						myContext.addEventListener(tiva, EventType.onSave);	
					}
					//Evaluate
					Variable v = varCache.getVariable(bl.getMyVariable());
					if (v!=null) {
						String eval = bl.getEvaluation();
						o.addRow("");
						o.addYellowText("SetValueBlock "+b.getBlockId()+" ["+bl.getMyVariable()+"]");
						o.addRow("Evaluation: "+eval);

						if (eval==null) {
							o.addRow("");
							o.addRow("Execution stopped on SetValueBlock "+bl.getBlockId()+". Expression "+bl.getExpression()+"evaluates to null");
							//jump.put(bl.getBlockId(), Executor.STOP_ID);
							notDone = false;
						} 				

						String val = v.getValue();
						if ((val == null && eval == null)||
								(val != null && eval != null && val.equals(eval))) {
							o.addRow("No change. Value: "+val+" Eval: "+eval);
						}
						else {	
							v.setValue(eval);
							o.addRow(bl.getExpression()+" Eval: ["+eval+"]");
							//Take care of any side effects before triggering redraw.
							myContext.registerEvent(new WF_Event_OnSave(bl.getBlockId()));
							o.addRow("Continues after onSave Event in block "+bl.getBlockId());
						}
					} else {
						o.addRow("");
						o.addRedText("Variable ["+bl.getMyVariable()+"] is missing in SetValueBlock "+bl.getBlockId());
					}






				}
				else if (b instanceof ConditionalContinuationBlock) {
					o.addRow("");
					o.addYellowText("ConditionalContinuationBlock "+b.getBlockId());
					final ConditionalContinuationBlock bl = (ConditionalContinuationBlock)b;
					final String formula = bl.getFormula();
					//final List<TokenizedItem> vars = gs.getRuleExecutor().findTokens(formula,null);
					if (bl.isExpressionOk()) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								//discard if redraw is coming..
								if (myContext.myEndIsNear()) {
									Log.d("vortex","Disregarding onsave due to ongoing reexecute.");
									return;
								}
								//If evaluation different than earlier, re-render workflow.
								if(bl.evaluate()) {
									//redraw! We block all other conditional blocks from triggering.
									myContext.setMyEndIsNear();
									//myContext.onResume();
									new Handler().postDelayed(new Runnable() {
										public void run() {
											//myContext.resetState();
											Set<Variable> previouslyVisibleVars = visiVars;
											Executor.this.run();
											for (Variable v:previouslyVisibleVars) {
												Log.d("nils","Previously visible: "+v.getId());
												boolean found = false;
												for(Variable x:visiVars) {									
													found = x.getId().equals(v.getId());
													if (found) 
														break;
												}

												if (!found) {
													Log.d("nils","Variable "+v.getId()+" not found.Removing");
													v.deleteValue();
												}

											}	
										}
									}, 0);

								}

							}
						};		
						Log.d("nils","Adding eventlistener for the conditional block");
						myContext.addEventListener(tiva, EventType.onSave);	
						//trigger event.
						bl.evaluate();

						switch (bl.getCurrentEval()) {
						case ConditionalContinuationBlock.STOP:
							jump.put(bl.getBlockId(), Executor.STOP_ID);
							break;
						case ConditionalContinuationBlock.JUMP:
							jump.put(bl.getBlockId(), bl.getElseId());						
							break;
						case ConditionalContinuationBlock.NEXT:
							jump.remove(bl.getBlockId());
						}

					}
					else {
						o.addRow("");
						o.addRedText("Cannot read formula: ["+formula+"] because of previous fail during the pre-Parse step. Please reimport the workflow configuration and check your log for the root cause.");
						Log.d("nils","Parsing of formula failed - no variables: ["+formula+"]");
					}
				}
				else if (b instanceof RuleBlock) {

					((RuleBlock) b).create(myContext,blocks);

				}
				else if (b instanceof MenuHeaderBlock) {
					((MenuHeaderBlock) b).create(myContext);
					hasDrawer=true;
				}

				else if (b instanceof MenuEntryBlock) {
					((MenuEntryBlock) b).create(myContext);
					hasDrawer=true;
				}
				else if (b instanceof RoundChartBlock) {
					((RoundChartBlock) b).create(myContext);

				}
				else if (b instanceof VarValueSourceBlock) {
					((VarValueSourceBlock) b).create(myContext);

				}
				else if (b instanceof CreateGisBlock) {
					pDialog = ProgressDialog.show(myContext.getContext(), "", 
							"Loading. Please wait...", true);
					savedBlockPointer = blockP+1;
					//Will callback to this object after image is loaded.
					CreateGisBlock bl = ((CreateGisBlock) b);
					if (bl.hasCarNavigation())
						myContext.enableSatNav();
					else
						Log.e("vortex","This has no SATNAV");
					if(!bl.create(myContext,this))
						//Pause execution and wait for callback..
						return;
				}

				else if (b instanceof AddGisLayerBlock) {
					((AddGisLayerBlock) b).create(myContext);

				}
				else if (b instanceof AddGisPointObjects) {
					((AddGisPointObjects) b).create(myContext);
				}

				else if (b instanceof AddGisFilter) {
					((AddGisFilter) b).create(myContext);
				}
				else if (b instanceof BlockDeleteMatchingVariables) {
					((BlockDeleteMatchingVariables) b).create(myContext);
				}


				else if (b instanceof CreateListFilter) {
					((CreateListFilter) b).create(myContext);
				}

				String cId = b.getBlockId();
				String jNext = jump.get(cId);
				if (jNext!=null) {	
					if (jNext.equals(Executor.STOP_ID))
						notDone = false;
					else
						blockP = indexOf(jNext,blocks);
				} else
					blockP++;

				if (!notDone)
					Log.d("vortex","EXECUTION STOPPED ON BLOCK "+b.getBlockId());
			}

			//Remove loading popup if displayed.
			removeLoadDialog();
			Container root = myContext.getContainer("root");
			if (root==null && myContext.hasContainers()) {
				o.addRow("");
				o.addRedText("TEMPLATE ERROR: Cannot find the root container. \nEach template must have a root! Execution aborted.");				
			} else {
				//Now all blocks are executed.
				//Draw the UI.
				o.addRow("");
				o.addYellowText("Now Drawing components recursively");
				Log.d("vortex","Now Drawing components recursively");
				//Draw all lists first.
				for (WF_Static_List l:myContext.getLists()) 			
					l.draw();
				for (WF_Table t:myContext.getTables())
					t.draw();
				/*
				WF_Gis_Map gis = myContext.getCurrentGis();
				if (gis!=null)
					gis.initialize();
				 */
				//Trgger redraw event on lists.
				//myContext.registerEvent(new WF_Event_OnSave("fackabuudle"));
				if (root!=null) 
					myContext.drawRecursively(root);
				//open menu if any

				if (hasDrawer) {
					Log.d("vortex","Drawing menu");
					gs.getDrawerMenu().openDrawer();
				}
				/*
					Object mDrawerLayout;
					if (hasDrawerMenu) {
						mDrawerLayout = myContext.getDrawerMenu();
						if (mDrawerLayout.isDrawerOpen(mDrawerList))
							mDrawerLayout.closeDrawers();
						mDrawerLayout.openDrawer(Gravity.LEFT);
						} else {
							mDrawerLayout.setEnabled(false);
							Log.d("vortex","NO drawer menu!");
						}

					}
				 */
				//Send event that flow has executed.
				Log.d("vortex","Registering WF EXECUTION");
				myContext.registerEvent(new WF_Event_OnFlowExecuted("executor"));

			}

		} catch (Exception e) {
			removeLoadDialog();
			Tools.printErrorToLog(o,e);
		}
	}



	private void removeLoadDialog() {
		if (pDialog!=null) {
			pDialog.dismiss();
			pDialog=null;
		}
	}
	private int indexOf(String jNext, List<Block> blocks) {

		for(int i=0;i<blocks.size();i++) {
			String id = blocks.get(i).getBlockId();
			//			Log.d("nils","checking id: "+id);
			if(id.equals(jNext)) {
				Log.d("vortex","Jumping to block "+jNext);
				o.addRow("Jumping to block "+jNext);
				return i;
			}
		}

		Log.e("nils","Jump pointer to non-existing block. Faulty ID: "+jNext);
		o.addRow("");
		o.addRedText("Jump pointer to non-existing block. Faulty ID: "+jNext);
		return blocks.size();
	}

	public Set<Variable> getVariables() {
		return visiVars;
	}


	@Override
	public void continueExecution() {
		if (savedBlockPointer!=-1)
			this.execute(savedBlockPointer);
		else
			Log.e("vortex","No saved block pointer...aborting execution");
	}

	@Override
	public void abortExecution(String reason) {
		Log.e("vortex","Execution aborted.");
		removeLoadDialog();
		new AlertDialog.Builder(myContext.getContext())
		.setTitle("Execution aborted")
		.setMessage(reason) 
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setNeutralButton("Ok",new Dialog.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}
		} )
		.show();
	}

	public void restart() {
		if (myContext.myEndIsNear()) {
			Log.d("vortex","skipping restart in restart!");
			return;
		}
		myContext.setMyEndIsNear();
		new Handler().postDelayed(new Runnable() {
			public void run() {
				//myContext.resetState();
				Executor.this.run();
				Log.d("vortex","workflow restarted");
			}
		}, 0);
	}

	//Refresh all the gislayers.
	public void refreshGisObjects() {
		for (Block b: wf.getBlocks()) {
			AddGisPointObjects bl;
			if (b instanceof AddGisPointObjects) {
				bl = ((AddGisPointObjects) b);
				bl.create(myContext, true);
			}
		}

	}
}