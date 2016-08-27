package com.teraim.fieldapp.dynamic.blocks;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Button;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_StatusButton;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ToggleButton;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.expr.SyntaxException;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.Exporter;
import com.teraim.fieldapp.utils.Exporter.ExportReport;
import com.teraim.fieldapp.utils.Exporter.Report;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * buttonblock
 * 
 * Class for all created Buttons
 * 
 * @author Terje
 *
 */
public  class ButtonBlock extends Block  implements EventListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6454431627090793561L;
	String onClick,name,containerId;
	private Boolean validationResult = true;
	Type type;
	private android.graphics.drawable.Drawable originalBackground;
	private List<EvalExpr>textE,targetE,buttonContextE,statusContextE;

	WF_Context myContext;
	private boolean isVisible;
	private String statusVar=null;
	private OnclickExtra extraActionOnClick=null;
	private GlobalState gs;
	private PopupWindow mpopup=null;

	private String exportFormat;
	private String exportFileName = null;
	private boolean enabled;

	private DB_Context buttonContextOld=null,buttonContext=null;
	private boolean syncRequired;
	private VariableCache varCache;
	private WF_Button button = null;
	private Map<String,String> statusVariableHash=null;

	enum Type {
		action,
		toggle
	}


	//TODO: REMOVE THIS Constructor!!
	//Function used with buttons that need to attach customized actions after click
	public ButtonBlock(String id,String lbl,String action, String name,String container,String target, String type, String statusVariableS,boolean isVisible,
			OnclickExtra onclickExtra,DB_Context buttonContext, int dummy) {		
		this(id,lbl,action,name,container,target,type,statusVariableS,isVisible,null,true,null,null,false);
		extraActionOnClick = onclickExtra;
		this.buttonContextOld = buttonContext;
	}

	public ButtonBlock(String id,String lbl,String action, String name,String container,String target, String type, String statusVariableS,boolean isVisible,String exportFormat,boolean enabled, String buttonContextS, String statusContextS,boolean requestSync) {
		Log.d("NILS","In NEW for Button "+name+" with context: "+buttonContextS);
		this.blockId=id;
		this.textE = Expressor.preCompileExpression(lbl);
		this.onClick=action;
		this.name=name;
		this.containerId = container;
		this.targetE=Expressor.preCompileExpression(target);
		this.type=type.equals("toggle")?Type.toggle:Type.action;
		this.isVisible = isVisible;
		this.statusVar = statusVariableS;	
		if (statusVar!=null&&statusVar.length()==0)
			statusVar=null;
		this.enabled=enabled;


		this.exportFormat = exportFormat;
		this.buttonContextE=Expressor.preCompileExpression(buttonContextS);
		this.statusContextE=Expressor.preCompileExpression(statusContextS);
		if (statusVar!=null && statusContextE==null)
			statusContextE=buttonContextE;
		Log.d("blorg","button "+textE+" statusVar: "+statusVar+" status_context: "+statusContextS);
		this.syncRequired = requestSync;
		//Log.d("vortex","syncRequired is "+syncRequired);
	}


	public String getText() {
		return Expressor.analyze(textE);
	}



	public void onEvent(Event e) {
		Log.d("bulla","in event for "+this.getText()+" button is "+button.getClass().getCanonicalName());
		if (button!=null) {
			button.setText(getText());
			if (button instanceof WF_StatusButton) {
				Log.d("bulla","calling refresh for "+this.getText());
				((WF_StatusButton)button).refreshStatus();
			}
			Log.d("bulla","aftercall");
			if (buttonContextE!=null&&!buttonContextE.isEmpty())
				buttonContext = DB_Context.evaluate(buttonContextE);

		}
	}

	public String getName() {
		return name;
	}
	public String getTarget() {
		return Expressor.analyze(targetE);
	}


	public WF_Widget create(final WF_Context myContext) {
		button = null;
		final Context ctx = myContext.getContext();
		myContext.registerEventListener(this, Event.EventType.onSave);
		gs = GlobalState.getInstance();
		varCache = gs.getVariableCache();
		o=gs.getLogger();
		final LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
				(Context.LAYOUT_INFLATER_SERVICE);
		Log.d("nils","In CREATE for BUTTON "+getText());
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null) {
			//Is the context provided already?
			if (buttonContextOld!=null)
				buttonContext=buttonContextOld;
			else {
				Log.d("vortex","ButtonContextS: "+buttonContextE);
				Log.d("vortex","statusContextS: "+statusContextE);
				//If not, parse the buttoncontext if provided in the button.
				//Else, use context in flow
				if (buttonContextE!=null&&!buttonContextE.isEmpty())
					buttonContext = DB_Context.evaluate(buttonContextE);
				else {
					Log.e("vortex","No button context. Will use default");
					buttonContext = myContext.getHash();
				}
			}

			Log.d("nils","Buttoncontext set to: "+buttonContext+" for button: "+getText());

			if (type == Type.action) {
				button=null;
				if (statusVar!=null) {

					button = new WF_StatusButton(blockId, WF_StatusButton.createInstance(0, getText(), ctx), isVisible, myContext, statusVar,statusContextE);
					if(((WF_StatusButton)button).refreshStatus()) {
						Log.d("vortex","sucessfully created statusbutton "+(button instanceof WF_StatusButton));
					} else {
						button=null;
						o.addRow("");
						if (buttonContext==null)
							o.addRedText("Statusvariable ["+statusVar+"], has something wrong with its context. Check precompile log.");
						o.addRedText("Statusvariable [" + statusVar + "], buttonblock " + blockId + " does not exist. Will use normal button");
						Log.e("vortex", "Statusvariable [" + statusVar + "], buttonblock " + blockId + " does not exist. Will use normal button");

					}
				}
				//If status wrong, no lamp
				if (button==null)
					button = new WF_Button(blockId,WF_Button.createInstance(getText(),ctx),isVisible,myContext);

				button.setOnClickListener(new View.OnClickListener() {
					boolean clickOngoing = false;

					@Override
					public void onClick(View view) {
						if (clickOngoing)
							return;
						else
							clickOngoing=true;
						originalBackground = view.getBackground();
						view.setBackgroundColor(Color.parseColor(Constants.Color_Pressed));
						//ACtion = workflow to execute.
						//Commence!
						if (extraActionOnClick!=null) {
							extraActionOnClick.onClick();
						}

						if (onClick.startsWith("template"))
							myContext.getTemplate().execute(onClick,getTarget());
						else {


							if (onClick.equals("Go_Back")) {
								final Variable statusVariable;
								String statusVar = myContext.getStatusVariable();

								if (statusVar != null)
									statusVariable = varCache.getVariable(buttonContext.getContext(),statusVar);
								 else
									statusVariable = null;
								Set<Rule> myRules = myContext.getRulesThatApply();
								boolean showPop=false;

								View popUpView = null; // inflating popup layout

								if (myRules != null && myRules.size()>0) {
									Log.d("nils","I have "+myRules.size()+" rules!");
									validationResult  = null;
									//We have rules. Each rule adds a line in the popup.
									popUpView = inflater.inflate(R.layout.rules_popup, null);
									mpopup = new PopupWindow(popUpView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true); //Creation of popup
									mpopup.setAnimationStyle(android.R.style.Animation_Dialog);
									LinearLayout frame = (LinearLayout)popUpView.findViewById(R.id.pop);
									Button avsluta = (Button)popUpView.findViewById(R.id.avsluta);
									Button korrigera = (Button)popUpView.findViewById(R.id.korrigera);
									avsluta.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											if (statusVariable!=null) {
												statusVariable.setValue(validationResult?"3":"2");
												Log.e("vortex","SETTING STATUSVAR: "+statusVariable.getId()+" key: "+statusVariable.getKeyChain()+ "Value: "+statusVariable.getValue());
												//Save value of all variables to database in current flow.

											}


											else
												Log.d("nils","Found no status variable");

											Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
											Log.d("vortex", "Variables To save contains "+(variablesToSave==null?"null":variablesToSave.size()+" objects."));
											for (Variable var:variablesToSave) {
												Log.d("vortex","Saving "+var.getLabel());
												boolean resultOfSave = var.setValue(var.getValue());
												if (resultOfSave) {
													for (int i=0;i<100;i++)
													Log.e("vortex","KORS I TAKET!!!!!!!!!!!!!!!!!!!!!!!!");
												}
											}
											myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));
											mpopup.dismiss();
											goBack();

										}
									});

									korrigera.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											mpopup.dismiss();
										}
									});
									LinearLayout row;
									TextView header,body;
									ImageView indicator;
									//Assume correct.
									validationResult = true;
									boolean isDeveloper = gs.getGlobalPreferences().getB(PersistenceHelper.DEVELOPER_SWITCH);

									for (Rule r:myRules) {
										Boolean ok=false;
										try {
											ok = r.execute();
										} catch (SyntaxException e) {
											o.addRow("");
											o.addRedText("Rule "+r.id+" has syntax errors in condition: "+r.getCondition());
										}
										if (ok!=null) {
											Rule.Type type = r.getType();
											int indicatorId=0;
											boolean bok = false;
											if (ok) {
												indicatorId = R.drawable.btn_icon_ready;
												bok=true;
											}
											else
												if (type == Rule.Type.ERROR) {
													indicatorId = R.drawable.btn_icon_started_with_errors;
													bok = false;
												}
												else {
													indicatorId = R.drawable.btn_icon_started;
													bok = true;
												}
											if (!bok)
												validationResult = false;
											if (!ok || ok && isDeveloper) {
												showPop=true;
												row = (LinearLayout)inflater.inflate(R.layout.rule_row, null);
												header = (TextView)row.findViewById(R.id.header);
												body = (TextView)row.findViewById(R.id.body);
												indicator = (ImageView)row.findViewById(R.id.indicator);
												indicator.setImageResource(indicatorId);
												Log.d("nils"," Rule header "+r.getRuleHeader()+" rule body: "+r.getRuleText());
												header.setText(r.getRuleHeader());
												body.setText(r.getRuleText());
												frame.addView(row);
											}
										}
									}

								}

								if (showPop)
									mpopup.showAtLocation(popUpView, Gravity.TOP, 0, 0);    // Displaying popup
								else {
									//no rules? Then validation is always ok.
									Log.d("nils","No rules found - exiting");
									if (statusVariable!=null) {
										statusVariable.setValue(WF_StatusButton.Status.ready.ordinal()+"");
										//myContext.registerEvent(new WF_Event_OnSave(ButtonBlock.this.getBlockId()));

									}
									else
										Log.d("nils","Found no status variable");
									Set<Variable> variablesToSave = myContext.getTemplate().getVariables();
									Log.d("nils", "Variables To save contains "+variablesToSave==null?"null":variablesToSave.size()+" objects.");
									for (Variable var:variablesToSave) {
										Log.d("nils","Saving "+var.getLabel());
										var.setValue(var.getValue());
									}
									goBack();
								}
							}
							else if (onClick.equals("Start_Workflow")) {
								String target = getTarget();
								Workflow wf = gs.getWorkflow(target);
								if (buttonContext!=null) {
									Log.d("vortex","Will use buttoncontext: "+buttonContext);
									gs.setDBContext(buttonContext);
								}
								if (wf == null) {
									Log.e("NILS","Cannot find workflow ["+target+"] referenced by button "+getName());
									o.addRow("");
									o.addRow("Cannot find workflow ["+target+"] referenced by button "+getName());
								} else {
									o.addRow("");
									o.addRow("Action button pressed. Executing wf: "+target+" with statusvar "+statusVar);
									Log.d("Vortex","Action button pressed. Executing wf: "+target+" with statusvar "+statusVar);
									Start.singleton.changePage(wf,statusVar);

								}

							} else if (onClick.equals("export")) {
								Map<String, String> exportContext = null;
								String msg = null;
								if (buttonContext!=null) {
									exportContext = buttonContext.getContext();
								} else {
									Log.e("export","Export failed...no context");
								}
								//msg is  null if no errors.
								if (msg == null) {

									exportFileName = getTarget();
									if (exportFormat == null)
										exportFormat = "csv";
									exportFormat = exportFormat.toLowerCase();
									Exporter exporter = Exporter.getInstance(ctx, exportFormat);
									Report jRep = gs.getDb().export(exportContext, exporter, exportFileName);
									if (jRep.er == ExportReport.OK) {
										msg = jRep.noOfVars + " variables exported to file: " + exportFileName + "." + exporter.getType() + "\n";
										msg += "You can find this file under " + Constants.EXPORT_FILES_DIR + " on your device";
										if (button instanceof WF_StatusButton)
											((WF_StatusButton)button).changeStatus(WF_StatusButton.Status.ready);
									} else {
										if (jRep.er == ExportReport.NO_DATA)
											msg = "Nothing to export! Have you entered any values? Have you marked your export variables as 'global'? (Local variables are not exported)";
										else
											msg = "Export failed. Reason: " + jRep.er.name();


									}
									//Context was broken


								}

								new AlertDialog.Builder(ctx)
								.setTitle("Export done")
								.setMessage(msg)
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
									}
								})
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
							} else if (onClick.equals("Start_Camera")) {
								Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
								File file = new File(Constants.PIC_ROOT_DIR,getTarget());
								Uri outputFileUri = Uri.fromFile(file);
								intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
								//				intent.putExtra(Strand.KEY_PIC_NAME, name);
								((Activity) ctx).startActivityForResult(intent, Constants.TAKE_PICTURE);


							} else if (onClick.equals("backup")) {
								boolean success = GlobalState.getInstance().getBackupManager().backupDatabase();
								new AlertDialog.Builder(ctx)
								.setTitle("Backup "+(success?"succesful":"failed"))
								.setMessage(success?"A file named 'backup_"+Constants.getSweDate()+"' has been created in your backup folder.":"Failed. Please check if the backup folder you specified under the config menu exists.")
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
									}
								})
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
							}
							else if (onClick.equals("restore_from_backup")) {

								new AlertDialog.Builder(ctx)
								.setTitle("Warning!")
								.setMessage("If you go ahead, you current database will be replaced by a backup file.")
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										boolean success = GlobalState.getInstance().getBackupManager().restoreDatabase();
										new AlertDialog.Builder(ctx)
										.setTitle("Restore "+(success?"succesful":"failed"))
										.setMessage(success?"Your database has been restored from backup. Please restart the app now.":"Failed. Please check that the backup file is in the staging area")
										.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
											}
										})
										.setIcon(android.R.drawable.ic_dialog_alert)
										.show();
									}
								})
								.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
									}
								})
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();


							}else if (onClick.equals("synctest")) {
								Log.e("vortex","gets HEREE!!!!");


							        // Pass the settings flags by inserting them in a bundle
							        Bundle settingsBundle = new Bundle();
							        settingsBundle.putBoolean(
							                ContentResolver.SYNC_EXTRAS_MANUAL, true);
							        settingsBundle.putBoolean(
							                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
							        /*
							         * Request the sync for the default account, authority, and
							         * manual sync settings
							         */
							        Account mAccount = GlobalState.getmAccount(ctx);
							        final String AUTHORITY = "com.teraim.fieldapp.provider";
							        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

							        //Also try to say hello.




							}
							else {
								o.addRow("");
								o.addRedText("Action button had no associated action!");
							}
						}

						clickOngoing = false;
						view.setBackgroundDrawable(originalBackground);
					}

					//Check if a sync is required. Pop current fragment.
					private void goBack() {

						myContext.getActivity().getFragmentManager().popBackStackImmediate();
						//myContext.reload();
						if (syncRequired)
							gs.sendEvent(MenuActivity.SYNC_REQUIRED);

					}

				});
				myContainer.add(button);
			} else if (type == Type.toggle) {
				final String text =this.getText();
				o.addRow("Creating Toggle Button with text: "+text);
				final ToggleButton toggleB = (ToggleButton)LayoutInflater.from(ctx).inflate(R.layout.toggle_button,null);
				//ToggleButton toggleB = new ToggleButton(ctx);
				toggleB.setTextOn(text);
				toggleB.setTextOff(text);
				toggleB.setChecked(enabled);
				LayoutParams params = new LayoutParams();
				//params.width = LayoutParams.MATCH_PARENT;
				//params.height = LayoutParams.WRAP_CONTENT;
				//toggleB.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
				//toggleB.setLayoutParams(params);

				toggleB.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if(onClick==null||onClick.trim().length()==0) {
							o.addRow("");
							o.addRedText("Button "+text+" has no onClick action!");
							Log.e("nils","Button clicked ("+text+") but found no action");
						} else {

							o.addRow("Togglebutton "+text+" pressed. Executing function "+onClick);
							String target = getTarget();
							if (onClick.startsWith("template")) {
								boolean result = myContext.getTemplate().execute(onClick, target);
								if (!result) {
									toggleB.toggle();
								}
							}
							else if (onClick.equals("toggle_visible")) {
								Log.d("nils","Executing toggle");
								Drawable d = myContext.getDrawable(target);
								if (d!=null) {
									if(d.isVisible())
										d.hide();
									else
										d.show();
								} else {
									Log.e("nils","Couldn't find target "+target+" for button");
									for (Drawable dd:myContext.getDrawables()) {
										Log.d("vortex",((WF_Widget)dd).getId());
									}
									o.addRow("");
									o.addRedText("Target for button missing: "+target);
								}

							}
						}
					}
				});
				button = new WF_ToggleButton(text,toggleB,isVisible,myContext);
				myContainer.add(button);
			}
			return button;
		} else {
			o.addRow("");
			o.addRedText("Failed to add text field block with id "+blockId+" - missing container "+myContainer);
			return null;
		}
	}



	public String getStatusVariable() {
		return statusVar;
	}

	public List<EvalExpr> getPrecompiledButtonContext() {
		return buttonContextE;
	}
}