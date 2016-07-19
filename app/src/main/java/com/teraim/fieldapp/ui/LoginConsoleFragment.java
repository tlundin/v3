package com.teraim.fieldapp.ui;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.loadermodule.Configuration;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.ModuleLoader;
import com.teraim.fieldapp.loadermodule.ModuleLoader.ModuleLoaderListener;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.log.Logger;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;


public class LoginConsoleFragment extends Fragment implements ModuleLoaderListener {

	TextView log;
	private final String License = "This sw uses 3rd party components that are under Apache 2.0 license.";
	private LoggerI loginConsole,debugConsole;
	private PersistenceHelper globalPh,ph;
	private ModuleLoader myLoader=null,myDBLoader=null;
	private String bundleName;
	private Configuration myModules;
	private DbHelper myDb;
	private TextView appTxt;
	private float oldV = -1;
	private Activity mActivity;
	private final static String InitialBundleName = "Rlotst";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login_console,
				container, false);
		TextView versionTxt,licenseTxt;
		Log.e("vortex","oncreatevieww!");
		log = (TextView)view.findViewById(R.id.logger);
		versionTxt = (TextView)view.findViewById(R.id.versionTxt);
		licenseTxt = (TextView)view.findViewById(R.id.licenseTxt);
		final ImageView logo = (ImageView)view.findViewById(R.id.logo);
		final ImageView bg = (ImageView)view.findViewById(R.id.bgImg);
		appTxt = (TextView)view.findViewById(R.id.appTxt);

		//Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
		//		"clacon.ttf");
		//log.setTypeface(type);
		log.setMovementMethod(new ScrollingMovementMethod());
		versionTxt.setText("Vortex engine ver. "+Constants.VORTEX_VERSION);
		licenseTxt.setText(License);
		//Create global state


		globalPh = new PersistenceHelper(mActivity.getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_MULTI_PROCESS));
		
		bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
		if (bundleName == null || bundleName.length()==0)
			bundleName = InitialBundleName;

		ph	 = new PersistenceHelper(mActivity.getSharedPreferences(globalPh.get(PersistenceHelper.BUNDLE_NAME), Context.MODE_MULTI_PROCESS));
		oldV= ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);

		appTxt.setText(bundleName+" "+(oldV==-1?"":oldV));
		String appBaseUrl = server()+bundleName.toLowerCase()+"/";

		//Log.d("vortex",  logoUrl);
		//new DownloadImageTask(logo)
		//.execute(logoUrl.toLowerCase());
		final String appRootFolderPath = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/";
		loginConsole = new Logger(mActivity,"INITIAL");
		loginConsole.setOutputView(log);
		Tools.onLoadCacheImage(appBaseUrl,"bg_image.jpg", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/bg_image.jpg", new BitmapFactory.Options());
					if (bm!=null)
						bg.setImageBitmap(bm);
				}
			}

			@Override
			public void progress(int bytesRead) {
			}
		});
		Tools.onLoadCacheImage(appBaseUrl,"logo.png", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/logo.png", new BitmapFactory.Options());
					if (bm!=null)
						logo.setImageBitmap(bm);
				}
			}
			@Override
			public void progress(int bytesRead) {
			}
		});

		//Tools.preCacheImage(bgUrl,"logo.png",appRootFolderPath+"cache/",loginConsole);

		//new DownloadImageTask((ImageView) view.findViewById(R.id.bgImg))
		//		.execute(bgUrl.toLowerCase());


		debugConsole = Start.singleton.getLogger();

		//Send a signal that init starts

		//First time vortex runs? Then create global folders.
		if (this.initIfFirstTime()) {
			if (!Connectivity.isConnected(getActivity())) {
				showErrorMsg("You need a network connection first time you start the program. Vortex requires configuration files to run.");
				return view;
			} else {
				this.initialize();
			}
		}
		//First time this application runs? Then create config folder.
		if (!new File(Constants.VORTEX_ROOT_DIR+bundleName).isDirectory()) {
			Log.d("vortex","First time execution!");
			debugConsole.addRow("");
			debugConsole.addPurpleText("First time execution of App "+bundleName);
		} //else 
		//	Log.d("vortex","This application has been executed before.");

		//TODO: Move this code into above in next release.
		File folder = new File(Constants.VORTEX_ROOT_DIR+bundleName);
//		if(!folder.mkdirs())
//			Log.d("NILS","Failed to create App root folder");
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/config");
//		if(!folder.mkdirs())
//			Log.("NILS","Failed to create config folder");
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/cache");
//		if(!folder.mkdirs())
//			Log.e("NILS","Failed to create cache folder");



		//write down version..quickly! :)
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, Constants.VORTEX_VERSION);

		//create module descriptors for all known configuration files.
		//Log.d("vortex","Creating Configuration and ModuleLoader");
		myModules = new Configuration(Constants.getCurrentlyKnownModules(globalPh,ph,server(),bundleName,debugConsole));
		String loaderId = "moduleLoader";
		boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+loaderId);
		myLoader = new ModuleLoader(loaderId,myModules,loginConsole,globalPh,allFrozen,debugConsole,this,mActivity);

		if (Constants.FreeVersion && expired())
			showErrorMsg("The license has expired. The App still works, but you will not be able to export any data.");;

			return view;
	}





	private boolean expired() {
		long takenIntoUseTime = globalPh.getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		return (diff > Constants.MS_MONTH);

	}





	@Override
	public void onResume() {
		super.onResume();
		Log.e("vortex","onresume!");
		if (GlobalState.getInstance() == null ) {
			String storedBundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
			if (!storedBundleName.equals(bundleName)) {
				if (!storedBundleName.isEmpty()) {
					Log.e("horcrux", "bundlename: " + bundleName + " inPHBN: " + globalPh.get(PersistenceHelper.BUNDLE_NAME));
					Fragment frg = null;
					frg = getFragmentManager().findFragmentById(R.id.content_frame);
					final FragmentTransaction ft = getFragmentManager().beginTransaction();
					ft.detach(frg);
					ft.attach(frg);
					ft.commit();
				}
			} else {

				Intent intent = new Intent();
				intent.setAction("INITSTARTS");
				mActivity.sendBroadcast(intent);
				Log.d("vortex","Loading In Memory Modules");
				myLoader.loadModules(true);
				loginConsole.draw();
				Log.d("vortex","loginConsole object "+loginConsole);
			}
		}
	}



	@Override
	public void onStop() {
		Log.e("vortex","onstop!");
		if (myLoader!=null)
			myLoader.stop();
		if (myDBLoader!=null)
			myDBLoader.stop();

		super.onStop();
	}


	/******************************
	 * First time? If so, create subfolders.
	 */
	private boolean initIfFirstTime() {
		//If testFile doesnt exist it will be created and found next time.
		Log.d("vortex","Checking if this is first time use of Vortex...");
		boolean first = (globalPh.get(PersistenceHelper.FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));


		if (first) {
			Log.d("vortex","Yes..executing  first time init");
			return true;
		}
		else {
			Log.d("vortex","..Not first time");
			return false;
		}

	}

	private void initialize() {

		//create data folder. This will also create the ROOT folder for the Strand app.
		File folder = new File(Constants.PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");
		folder = new File(Constants.OLD_PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create old pic root folder");
		folder = new File(Constants.EXPORT_FILES_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create export folder");

		//Set defaults if none.
		if (globalPh.get(PersistenceHelper.SERVER_URL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.SERVER_URL, "www.teraim.com");
		if (globalPh.get(PersistenceHelper.BUNDLE_NAME).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.BUNDLE_NAME, InitialBundleName);
		if (globalPh.get(PersistenceHelper.DEVELOPER_SWITCH).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.DEVELOPER_SWITCH, false);
		if (globalPh.get(PersistenceHelper.VERSION_CONTROL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.VERSION_CONTROL, "Major");
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.SYNC_METHOD, "Bluetooth");
		if (globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.USER_ID_KEY, getRandomName());		
		//if (globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED))
		//	globalPh.put(PersistenceHelper.LAG_ID_KEY, "Anonymous");
		if (globalPh.get(PersistenceHelper.LOG_LEVEL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.LOG_LEVEL, "critical");	
	

		folder = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/"+Constants.CACHE_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create gis image folder");		

		globalPh.put(PersistenceHelper.FIRST_TIME_KEY,"Initialized");
		long millis = System.currentTimeMillis();
		//date = Constants.getTimeStamp();
		globalPh.put(PersistenceHelper.TIME_OF_FIRST_USE,millis);
	}


	private String getRandomName() {

		List<String> start= Arrays.asList(new String[]{"Anna","Eva","Fiona","Berta"});
		List<String> end= Arrays.asList(new String[]{"stina","getrud","lena","eulalia"});        
        Collections.shuffle(start);
        Collections.shuffle(end);
        return start.get(0)+end.get(0)+"_"+(new Random()).nextInt(500);
	}





	private void showErrorMsg(String error) {
		new AlertDialog.Builder(mActivity)
		.setTitle("Error message")
		.setMessage(error) 
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setNeutralButton("Ok",new Dialog.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		} )
		.show();
	}

	public String server() {
		String serverUrl = globalPh.get(PersistenceHelper.SERVER_URL);
		if (!serverUrl.endsWith("/"))
			serverUrl+="/";
		if (!serverUrl.startsWith("http://")) 
			serverUrl = "http://"+serverUrl;
		return serverUrl;
	}


	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }


	@Override
	public void loadSuccess(String loaderId, final boolean majorVersionChange) {
		Log.d("vortex","Arrives to loadsucc with ID: "+loaderId);
		ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,true);
		if (mActivity==null) {
			Log.e("vortex","No activity App cannot continue load. Must Restart...");
			
		}
		//If load successful, create database and import data into it. 
		if (loaderId.equals("moduleLoader")) {
			loginConsole.addRow("");
			//			loginConsole.addGreenText("All modules loaded...preparing database");
			loginConsole.draw();

			//Create or update database from Table object.
			ConfigurationModule m = myModules.getModule(VariablesConfiguration.NAME);

			if (m!=null) {
				Table t = (Table)m.getEssence();
				myDb = new DbHelper(mActivity.getApplicationContext(),t, globalPh,ph,bundleName);
				//Load configuration files asynchronously.
				Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t, new AsyncLoadDoneCb() {
					public void onLoadSuccesful(List<ConfigurationModule> modules) {
						Configuration dbModules = new Configuration(modules);
						if (modules!=null) {
							String loaderId = "dbLoader";
							boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+loaderId);
							myDBLoader = new ModuleLoader(loaderId,dbModules,loginConsole,globalPh,allFrozen,debugConsole,LoginConsoleFragment.this,mActivity);
							loginConsole.addRow("Defaults & GIS modules");			
							myDBLoader.loadModules(majorVersionChange);
						} else 
							Log.e("vortex","null returned from getDBImportModules");
					}
				});
				//Configuration dbModules = new Configuration(Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t));
				//Import historical data to database. 


			} 
		} else {
			//Program is ready to run.
			//Create the global state from all module objects. 
			//Context applicationContext, PersistenceHelper globalPh,
			//PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
			//Map<String, Workflow> workflows,Table t,SpinnerDefinition sd

			List<Workflow> workflows = (List<Workflow>)(myModules.getModule(bundleName).getEssence());
			Table t = (Table)(myModules.getModule(VariablesConfiguration.NAME).getEssence());
			SpinnerDefinition sd = (SpinnerDefinition)(myModules.getModule(SpinnerConfiguration.NAME).getEssence());
			if (mActivity!=null) {
				GlobalState gs = 
						GlobalState.createInstance(mActivity.getApplicationContext(),globalPh,ph,debugConsole,myDb, workflows, t,sd);
				Start.alive=true;
				//Update app version if new
				//if (majorVersionChange) {
				float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
				Log.d("vortex","updating App version to "+loadedAppVersion);
				ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP,loadedAppVersion);
				//				}
				//drawermenu
				gs.setDrawerMenu(Start.singleton.getDrawerMenu());
				
				
				//Change to main.
				//execute main workflow if it exists.
				Workflow wf = gs.getWorkflow("Main");
				if (wf == null) {
					String[] x = gs.getWorkflowNames();
					debugConsole.addRow("");
					debugConsole.addRedText("workflow main not found. These are available:");
					for (String n:x) 
						debugConsole.addRow(n);
				}
				if (wf!=null) {
					Start.singleton.getDrawerMenu().closeDrawer();
					Start.singleton.getDrawerMenu().clear();
					gs.sendEvent(MenuActivity.INITDONE);
					float newV = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
					if (newV==-1)
						appTxt.setText(bundleName+" [no version]");
					else {
						if (newV>oldV)
							appTxt.setText(bundleName+" --New Version! ["+newV+"]");
						else
							appTxt.setText(bundleName+" "+newV);
					}
					Start.singleton.changePage(wf,null);
					Log.d("vortex","executing workflow main!");
					gs.setModules(myModules);

					GlobalState.getInstance().onStart();
				} else {
					loginConsole.addRow("");
					loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
				}
			} else {
				Log.e("vortex","No activity.");
			}
		}





	}



	private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {

			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			Log.d("vortex","setting image!!");
			if (result!=null)
				bmImage.setImageBitmap(result);
		}
	}



	@Override
	public void loadFail(String loaderId) {
		Log.d("vortex","loadFail!");
		getActivity().sendBroadcast(new Intent(MenuActivity.INITFAILED));
	}




}
