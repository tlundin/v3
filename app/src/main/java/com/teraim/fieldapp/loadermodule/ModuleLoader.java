package com.teraim.fieldapp.loadermodule;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public class ModuleLoader implements FileLoadedCb{
	private Configuration myModules;
	private LoggerI o,debug;
	private PersistenceHelper globalPh;
	private ModuleLoaderListener caller;
	private String loaderId;
	private Context ctx;
	private boolean allFrozen;

	
	public interface ModuleLoaderListener {
		
		void loadSuccess(String loaderId, boolean majorVersionChange);
		void loadFail(String loaderId);
	}
	
	
	
	public ModuleLoader(String loaderId, Configuration currentlyKnownModules, LoggerI o, PersistenceHelper glPh, boolean allFrozen, LoggerI debugConsole,ModuleLoaderListener caller,Context ctx) {
		myModules = currentlyKnownModules;
		this.o=o;
		this.debug=debugConsole;
		globalPh = glPh;
		this.caller = caller;
		this.loaderId=loaderId;
		this.ctx=ctx;
		//boolean that tells if all files already exist frozen.
		this.allFrozen = allFrozen;
	}

	
	ConfigurationModule module;
	
	private boolean majorVersionChange = true; 
		
	public void loadModules(boolean majorVersionChange) {
		
		this.majorVersionChange=majorVersionChange;
		//If all frozen, simply load from memory and check version later on. 
		
		//check if any module needs to load.
			module = myModules.next();
		if (module!=null) {
			o.addRow(module.getLabel()+" :");
			Log.d("vortex",module.getLabel()+" :");
			if (majorVersionChange )
				module.load(this);
			else {
				//Load files in parallel from disk 
				new Handler().postDelayed(new Runnable() {
					public void run() {
				onFileLoaded(new LoadResult(module,ErrorCode.sameold));
					}
				},0);
			}
		} else 
			caller.loadSuccess(loaderId,majorVersionChange);
			
	}




	@Override
	public void onUpdate(Integer ...args) {
		if (args.length==1)
			o.writeTicky(" "+args[0].toString());
		else
			o.writeTicky(" "+args[0].toString()+"/"+args[1].toString());

	}

	@Override
	public void onFileLoaded(LoadResult res) {
		
		o.removeTicky();
		ConfigurationModule module = res.module;
		if (module != null) { 
			debug.addRow("Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));
			Log.d("vortex","Module "+res.module.fileName+" loaded. Returns code "+res.errCode.name()+(res.errorMessage!=null?" and errorMessage: "+res.errorMessage:""));
			

			switch (res.errCode) {
			//If version of App not updated, and default Version handling, and all config files exist frozen, then turn on justLoadCurrent
			case majorVersionNotUpdated:
				if (allFrozen) {
						Log.d("vortex","Major control!!");
						majorVersionChange = false;
						o.removeLine();
						o.addRow("Use existing configuration");
				} 
				
			break;
			case existingVersionIsMoreCurrent:
			case sameold:
				if (thawModule(module)) {
					module.setLoaded(true);
					if (res.errCode==ErrorCode.existingVersionIsMoreCurrent) {
						o.addRow("");
						o.addText(" Remote version older: [");
						o.addRedText(res.errorMessage);
						o.addText("]");
					}
					o.addRow("");
					float frozenModuleVersion = module.getFrozenVersion();
					String frozenModuleVersionS = frozenModuleVersion+"";
					if (frozenModuleVersion==-1)
						frozenModuleVersionS="missing";
					o.addYellowText("Version used: "+frozenModuleVersionS);
				} else { 
					Log.d("vortex","Retrying.");
					o.addYellowText(" fail..retrying..");
					module.setFrozenVersion(-1);
					module.load(this);
					o.draw();
					return;
					//failing thaw will lead to retry.
				}
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
			case Unsupported:
			case IOError:
			case BadURL:
			case ParseError:
			case Aborted:
			case notFound:
			case noData:
				
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
					if (thawModule(module)) {
						o.addRow("");o.addYellowText("Current version will be used: "+module.getFrozenVersion());
						module.setLoaded(true);
					} 
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
					o.addRow("");
					o.addGreenText("Reload required for dependant "+res.errorMessage);
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
			loadModules(majorVersionChange);
		}

	}





	private void printError(LoadResult res) {
		ErrorCode errCode = res.errCode;
		if (errCode==ErrorCode.IOError) {
			if (ctx!=null && !Tools.isNetworkAvailable(ctx)) 
				o.addRow("No network");
			else {
				if (res.errorMessage !=null)
					o.addRow(res.errorMessage);
				else
					o.addRow("No data (Network error)");
			}
		} else if (errCode==ErrorCode.Unsupported) {
			o.addRow("Remote file requires Vortex version ["+res.errorMessage+"]. Please upgrade.");
		} else if (errCode==ErrorCode.ParseError) {
			o.addRow("");
			o.addRedText("The file contains an error. Please check log for details");
		} else
			o.addYellowText(" "+res.errCode.name());

		
	}

	private boolean thawModule(ConfigurationModule module) {
		LoadResult res = module.thaw();
		if (res.errCode!=ErrorCode.thawed) {
			debug.addRow("");
			debug.addYellowText("Failed to thaw module "+module.fileName+" due to "+res.errCode.name());
			Log.e("vortex","Failed to thaw module "+module.fileName+" due to "+res.errCode.name());
			
			//Remove the corrupt file and remove version.
			if (module.deleteFrozen())
				debug.addYellowText("Corrupt file has been deleted");
			//retry loading the file - don't set it loaded.
				return false;
		}
		debug.addRow("Module "+module.getFileName()+" was thawed.");
		return true;
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
