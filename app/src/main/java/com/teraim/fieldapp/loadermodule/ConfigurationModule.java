package com.teraim.fieldapp.loadermodule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import android.provider.MediaStore.Files;
import android.util.Log;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;


//Class that describes the specific load behaviour for a certain type of input data.
public abstract class ConfigurationModule {



	public enum Type {
		json,
		xml,
		csv,
		ini,
		jgw
	}

	public enum Source {
		file,
		internet
	}
	public Source source;
	public Type type;
	public String fileName;
	private String rawData,printedLabel,frozenPath;
	protected float newVersion;
	protected PersistenceHelper globalPh,ph;
	private boolean IamLoaded=false;
	protected String versionControl;

	private Integer linesOfRawData;
	protected Object essence;
	protected String baseBundlePath;
	private boolean notFound=false;
	private String fullPath;
	//freezeSteps contains the number of steps required to freeze the object. Should be -1 if not set specifically by specialized classes.
	protected int freezeSteps=-1;
	//tells if this module is stored on disk or db.
	protected boolean isDatabaseModule = false,hasSimpleVersion=true;

	public ConfigurationModule(PersistenceHelper gPh,PersistenceHelper ph, Type type, Source source, String urlOrPath,String fileName,String moduleName) {
		this.source=source;
		this.type=type;
		
		this.fileName=fileName;
		this.globalPh=gPh;
		this.ph=ph;
		this.printedLabel=moduleName;
		this.baseBundlePath=urlOrPath;
		fullPath = urlOrPath+fileName+"."+type.name();
		frozenPath = Constants.VORTEX_ROOT_DIR+gPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/"+fileName;
		Log.d("balla","full path "+fullPath);
		Log.d("balla","base bundle path "+baseBundlePath);
		this.versionControl = globalPh.get(PersistenceHelper.VERSION_CONTROL);
	}


	public boolean frozenFileExists() {
		return new File(frozenPath).isFile() && (getFrozenVersion()!=-1);
	}

	public abstract float getFrozenVersion();

	public String getFullPath() {
		return fullPath;
	}

	public String getURL() {
		return fullPath;
	}
	//Stores version number. Can be different from frozen version during load.
	public void setNewVersion(float version) {
		this.newVersion=version;
	}

	//Freeze version number when load succesful
	public void setLoaded(boolean loadStatus) {
		IamLoaded=loadStatus;
		setThawActive(false);
	}


	boolean isThawing = false;

	private void setThawActive(boolean t) {
		isThawing=t;
	}
	public boolean thawing() {
		return isThawing;
	}

	public void setNotFound() {
		isThawing=false;
		notFound=true;
	}

	protected abstract void setFrozenVersion(float version);

	public abstract boolean isRequired();

	public boolean isLoaded() {
		// :)
		return IamLoaded||notFound;
	}
	
	public boolean isMissing() {
		return notFound;
	}

	public void cancelLoader() {
		if (mLoader!=null) {
			Log.e("vortex","Cancelled mLoader!");
			mLoader.cancel(true);
			mLoader = null;
		}
	}
	private Loader mLoader=null;

	public void load(FileLoadedCb moduleLoader) {
		if (source == Source.internet) 
			mLoader = new WebLoader(null, null, moduleLoader,versionControl);
		else 
			mLoader = new FileLoader(null, null, moduleLoader,versionControl);
		mLoader.execute(this);
	}


	public String getRawData() {
		return rawData;
	}
	public void setRawData(String data, Integer tot) {
		rawData = data;
		this.linesOfRawData = tot;
	}

	protected Integer getNumberOfLinesInRawData() {
		// TODO Auto-generated method stub
		return linesOfRawData;
	}


	public void setLoadedFromFrozenCopy() {
		//load the data from frozen
		IamLoaded=true;
	}


	public String getFileName() {
		return fileName;
	}

	public String getLabel() {
		return printedLabel;
	}

	//Freeze this configuration. counter is used by some dependants.
	public boolean freeze(int counter) throws IOException {
		this.setEssence();
		if (essence!=null) {
			final String fPath = frozenPath;
			Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					
					try {
						Tools.witeObjectToFile(essence, fPath);
					} catch (IOException e) {

						GlobalState gs = GlobalState.getInstance();
						if (gs!=null) {
							gs.getLogger().addRow("");
							StringWriter sw = new StringWriter();
							PrintWriter pw = new PrintWriter(sw);
							e.printStackTrace(pw);		
							gs.getLogger().addRedText(sw.toString());
						}
						e.printStackTrace();
					}
				}
			};

			Thread t = new Thread(r);
			t.start();

			return true;
		}

		else
			return isDatabaseModule;
	}

	public LoadResult thawSynchronously() {

		//A database module is by default saved already.
		if (isDatabaseModule)
			return new LoadResult(this,ErrorCode.thawed);
		else {
			//Unthaw asynchronously
			Object result = Tools.readObjectFromFile(this.frozenPath);
			this.setThawActive(false);
			setEssence(result);
			return new LoadResult(this,result == null?ErrorCode.thawFailed:ErrorCode.thawed);
		}

	}

	public boolean thaw(ModuleLoader caller) {

		//A database module is by default saved already.
		if (isDatabaseModule)
			return true;
        else {
			//Unthaw asynchronously
			this.setThawActive(true);
			Tools.readObjectFromFileAsync(this.frozenPath, this, caller);
			return false;
		}

	}



	public Object getEssence() {
		return essence;
	}

	//Must set essence before freeze.
	public abstract void setEssence();

    //If thawed, set essence from file.
    public void setEssence(Object result) {
        this.essence=result;
    }


	public boolean deleteFrozen() {
		return new File(this.frozenPath).delete();
	}








}
