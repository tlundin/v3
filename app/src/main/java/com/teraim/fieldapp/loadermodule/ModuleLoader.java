package com.teraim.fieldapp.loadermodule;

import java.util.List;
import java.util.Objects;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.text.SpannableString;
import android.util.Log;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.log.PassiveLogger;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public class ModuleLoader implements FileLoadedCb{
	private final LoggerI frontPageLog;
	private Configuration myModules;
	private LoggerI o,debug;
	private PersistenceHelper globalPh;
	private ModuleLoaderListener caller;
	private String loaderId;
	private Context ctx;
	private boolean allFrozen;


	public interface ModuleLoaderListener {

		void loadSuccess(String loaderId, boolean majorVersionChange, CharSequence loadText);
		void loadFail(String loaderId);
	}



	public ModuleLoader(String loaderId, Configuration currentlyKnownModules, LoggerI log, PersistenceHelper glPh, boolean allFrozen, LoggerI debugConsole,ModuleLoaderListener caller,Context ctx) {
		myModules = currentlyKnownModules;
		frontPageLog = log;

		this.debug=debugConsole;
		//Passive logger will be the main log window...text will appear on About page.
		globalPh = glPh;
		this.caller = caller;
		this.loaderId=loaderId;
		this.ctx=ctx;
		//boolean that tells if all files already exist frozen.
		this.allFrozen = allFrozen;
		o = new PassiveLogger(ctx,"PassiveLog");
	}


	ConfigurationModule module;

	//private boolean majorVersionChange = true;

	public void loadModules(boolean majorVersionChange) {

		//this.majorVersionChange=majorVersionChange;
		//If all frozen, simply load from memory and check version later on. 

		//check if any module needs to load.
		module = myModules.next();
		if (module!=null) {
			if (!module.isThawing) {
				o.addRow(module.getLabel() + " :");
				frontPageLog.addRow(module.getLabel()+ " :");
				Log.d("amazon", module.getLabel() + " :");
				boolean forced = module.versionControl.equals("Forced");
				//Check if connection is slow. If so, use existing version IF versioncontrol is not set to "Always load".
				if ((module.source == ConfigurationModule.Source.internet) &&
						!Connectivity.isConnectedFast(ctx) &&
						!forced) {

						onFileLoaded(new LoadResult(module, ErrorCode.slowConnection));

				} else {
					if (majorVersionChange || forced) {
						Log.d("amazon","load");
						module.load(this);
					}
					else {
						Log.d("amazon","sameold");
						onFileLoaded(new LoadResult(module, ErrorCode.sameold));
					}
				}
			} else
				Log.d("vortex","wait for thawing of "+module.getLabel());
		} else {

			caller.loadSuccess(loaderId, majorVersionChange, o.getLogText());
		}

	}




	@Override
	public void onUpdate(Integer ...args) {
		if (args.length==1)
			frontPageLog.writeTicky(" "+args[0].toString());
		else
			frontPageLog.writeTicky(" "+args[0].toString()+"/"+args[1].toString());

	}




	@Override
	public void onFileLoaded(LoadResult res) {


		frontPageLog.removeTicky();
		frontPageLog.removeLine();
		ConfigurationModule module = res.module;
		if (module != null) {
			debug.addRow("Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));
			Log.d("vortex","Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));


			switch (res.errCode) {
				//If version of App not updated, and default Version handling, and all config files exist frozen, then turn on justLoadCurrent
				case majorVersionNotUpdated:
					if (allFrozen) {
						//majorVersionChange = false;
						o.removeLine();
						o.addRow("Use existing configuration");

					}
					break;
				case existingVersionIsMoreCurrent:
				case thawed:
                    debug.addRow("Module "+module.getFileName()+" was thawed.");
                    module.setLoaded(true);
                    if (res.errCode==ErrorCode.existingVersionIsMoreCurrent) {
                        o.addRow("");
                        o.addText(" Remote version older: [");
                        o.addRedText(res.errorMessage);
                        o.addText("]");
                    }
                    float frozenModuleVersion = module.getFrozenVersion();
                    String frozenModuleVersionS = frozenModuleVersion+"";
                    if (frozenModuleVersion==-1)
                        frozenModuleVersionS="unknown";
                    o.addText(" ["+frozenModuleVersionS+"]");
					break;

				case sameold:
					//async call...will callback with onFileLoaded.
					module.thaw(this);

					break;

				case frozen:
				case nothingToFreeze:
					module.setLoaded(true);
					if (debug.hasRed()) {
						o.addRedText(" *Check Log!*");
					}
					else
						o.addGreenText(" New!");
					o.addText(" [");
					if (module.newVersion!=-1)
						o.addText(module.newVersion+"");
					else
						o.addText("?");
					o.addText("]");

					break;
                case thawFailed:
                    //if thaw failed, remove file and try again.
                    Log.d("vortex","Retrying.");
                    o.addYellowText(" corrupt..reload..");
                    module.deleteFrozen();
                    module.setFrozenVersion(-1);
                    module.load(this);
                    o.draw();
                    break;

				case Unsupported:
				case IOError:
				case BadURL:
				case ParseError:
				case Aborted:
				case notFound:
				case noData:
				case slowConnection:

					if (module.isRequired()&&!module.frozenFileExists()) {
						o.addRedText(" !");o.addText(res.errCode.name());o.addRedText("!");
						o.addRow("Upstart aborted..Unable to load mandatory file.");
						//printError(res);
						o.draw();
						//Need to enable the settings menu.
						caller.loadFail(loaderId);
						return;
					}
					printError(res);
					if (module.frozenFileExists()) {
						module.thaw(this);
                        o.addRow("");o.addYellowText("Using current: "+module.getFrozenVersion());
					} else
						module.setNotFound();

					break;
				case reloadDependant:
					Log.d("vortex","Dependant ["+res.errorMessage+"] needs to be reloaded.");
					ConfigurationModule reloadModule = myModules.getModule(res.errorMessage);
					if (reloadModule!=null) {
						if (reloadModule.isMissing() && !reloadModule.isRequired()) {
							Log.d("vortex","Dependant is not required and is not defined");

						} else {
							reloadModule.setLoaded(false);
							reloadModule.setFrozenVersion(-1);

							//module.setFrozenVersion(-1);
							//module.setLoaded(false);
							Log.d("vortex","Now retry load of modules");
							//o.addRow("");
							//o.addGreenText("Reload required for dependant "+res.errorMessage);
							//majorVersionChange=true;
							reloadModule.load(this);
							o.draw();

						}
						return;
					}
					break;
				default:
					o.addText("?: "+res.errCode.name());
					break;
			}
			o.draw();
			if (res.errCode!=ErrorCode.sameold)
				loadModules(!allFrozen);

		}

	}





	private void printError(LoadResult res) {
		ErrorCode errCode = res.errCode;
		if (errCode==ErrorCode.IOError) {
			if (ctx!=null && !Connectivity.isConnected(ctx))
				o.addRow("No network");
			else {
				if (res.errorMessage !=null)
					o.addRow(res.errorMessage);
				else
					o.addRow("File not found (Network error)");
			}
		} else if (errCode==ErrorCode.Unsupported) {
			o.addRow("");
			o.addRedText("The version of FieldPad you use cannot run this App. Min version required: "+res.errorMessage+". ");
		} else if (errCode==ErrorCode.ParseError) {
			o.addRow("");
			o.addRedText("Error. Please check log for details");
			Log.e("fall",res.errorMessage==null?"":res.errorMessage);
			debug.addRedText(module.getFileName());
			debug.addCriticalText(res.errorMessage);
		} else
			o.addYellowText(" "+res.errCode.name());


	}

	//TODO:REMOVE
	public void onFileLoaded(ErrorCode errCode, String version) {};

	public void stop() {
		Log.e("vortex","In stop for "+loaderId);
		List<ConfigurationModule> list = myModules.getAll();
		for (ConfigurationModule module:list) {
			Log.e("vortex","Cancelling loader process for "+module.getFileName());
			module.cancelLoader();
		}
	}


}
