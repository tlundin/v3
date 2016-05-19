package com.teraim.fieldapp.non_generics;

/**
 * @author Terje 
 * 
 * This is the Common Vars class giving access to global state stored in the Persisted memory.
 * For now, persistence implemented via SharedPreferences only.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.ConfigurationModule.Source;
import com.teraim.fieldapp.loadermodule.configurations.GisObjectConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GroupsConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.ImportDataConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.ui.AsyncLoadDoneCb;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

public class Constants {

	public final static float VORTEX_VERSION = 3.92f;
	//String constants
	//The root folder for the SD  card is in the global Environment.
	private final static String path = Environment.getExternalStorageDirectory().getPath();
	//Remember to always add system root path before any app specific path!

	//Root for NILS

	public final static String HISTORICAL_TOKEN_IN_XML = "*HISTORICAL*";
	
	public static final String HISTORICAL_TOKEN_IN_DATABASE = "H";
	public final static String VORTEX_ROOT_DIR = path+"/vortex/";
	public static final String EXPORT_FILES_DIR = VORTEX_ROOT_DIR + "export/";
	public static final String PIC_ROOT_DIR = VORTEX_ROOT_DIR + "pics/";
	public static final String OLD_PIC_ROOT_DIR = VORTEX_ROOT_DIR + "old_pics/";
	public static final String CACHE_ROOT_DIR = "cache/";
	public static final String GIS_FILE_DIR = "/gisdata/";
	//Folder for backup on SD card.
	public static final String DEFAULT_EXT_BACKUP_DIR = "/mnt/external_sdcard/vortex/backup";
	//public static String NILS_BASE_DIR = "/nils";
	public static String UNDEFINED = "undefined";

	public static String Color_Pressed="#4682B4";


	public static String[] defaultGroupHeader=new String[] {"Label,Description,Group Name,Variable Name,Internet link,P_Familj,P_Class"};


	public static final String SYNC_ID = "SYNX";

	//NILS uid
	public static final UUID RED_UID = UUID.fromString("58500d27-6fd9-47c9-bf6b-d0969ce78bb3");
	public static final UUID BLUE_UID = UUID.fromString("ce8ec829-30e3-469b-886e-6cf8f1168e98");


	//Static methods
	public static String compassToPicName(int compass) {
		return (compass==0?"vast":(compass==1?"norr":(compass==2?"syd":(compass==3?"ost":null))));
	}


	public static final String TRUE = "true";
	public static final String FALSE= "false";

	//Static constants
	public static final long MS_MONTH = 2629746000L;

	public static final int KEY_LENGTH = 10;
	public static final String SLU_URL = "https://arbetsplats.slu.se/sites/srh/Landskapsanalys/Faltportal/default.aspx";
	public static final String STATUS_INITIAL = "0";
	public static final String STATUS_STARTAD_MEN_INTE_KLAR = "1";
	public static final String STATUS_STARTAD_MED_FEL = "2";
	public static final String STATUS_AVSLUTAD_OK = "3";
	public static final String STATUS_DISABLED = "4";
	public static final int MAX_NILS_LINJER = 12;
	public static final String NORR = "NORR";
	public static final String SYD = "SYD";
	public static final String OST = "OST";
	public static final String VAST = "VAST";
	public static final String AVST = "AVST";
	public static final String SMA = "SMA";
	public static final String NULL_VALUE = "NULL";
	public static final String NOT_NULL = "*NN*";
	
	//Name of the special variable group used for status variables 
	public static final String STATUS_VARIABLES_GROUP_NAME = "STATUS";

	public static final String NO_DEFAULT_VALUE = "*NULL*";




	public static UUID getmyUUID() {
		/*
		String myC = getDeviceColor();
		if (myC.equals(nocolor()))
			return null;
		else if (myC.equals(red()))
			return RED_UID;
		else
		 */
		return BLUE_UID;
	}


	//TODO: REMOVE
	public static final int MAX_NILS = 16;

	public static final int MIN_ABO = 50;

	public static final int MAX_ABO = 99;

	//ruta size in meters.
	public static final float RUTA_SIZE = 3000;

	public static final String CONFIG_FROZEN_FILE_ID = null;

	public static final String TypesFileName = null;

	public static boolean isAbo(int pyID) {
		return pyID>=Constants.MIN_ABO && pyID<=Constants.MAX_ABO;
	}

	//Static Time providers.


	public static String getTimeStamp() {
		return getYear()+getMonth()+getDayOfMonth()+"_"+getHour()+"_"+getMinute();
	}

	public static String getYear() {
		return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
	}

	public static String getWeekNumber() {
		return Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
	}

	public static String getMonth() {
		return Integer.toString(Calendar.getInstance().get(Calendar.MONTH)+1);
	}

	public static String getDayOfMonth() {
		return Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
	}

	public static String getHour() {
		return Integer.toString(Calendar.getInstance().get(Calendar.HOUR));
	}

	public static String getMinute() {
		return Integer.toString(Calendar.getInstance().get(Calendar.MINUTE));
	}
	public static String getSecond() {
		return Integer.toString(Calendar.getInstance().get(Calendar.SECOND));
	}
	
	@SuppressLint("SimpleDateFormat") 
	public static String getSweDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(new Date()); 
	}

	public static final String GroupFileName = "Groups.csv";
	public static final String VariablesFileName = "Variables.csv";
	public static final String SpinnersFileName = "Spinners.csv";
	public static final String PY_HISTORICAL_FILE_NAME = "Importdata.json";

	public static final String WF_FROZEN_SPINNER_ID = null;

	public static final String WF_FROZEN_FILE_ID = null;

	public static final String GLOBAL_PREFS = "GlobalPrefs";

	public final static int VAR_PATTERN_ROW_LENGTH = 11;


	public static final String VariableSeparator = ":";

	//Vibrate when error x milliseconds...
	public static final long BURR_LENGTH = 250;


	public static final String WILD_CARD_MARKER = "%";


	public final static int TAKE_PICTURE = 133;


	private static final String GPS_LIST_FILE_NAME = "content.txt";


	private static final String GPS_CONFIG_WEB_FOLDER = "gis_objects";


	public static final String BLUETOOTH_NAME = "vortex";


	public static final String FIELD_PAD_VERSION = "1.0";

	public static final boolean FreeVersion = false;

	public static final String BACKUP_FILE_NAME = "backup";

	//Backup if data older than 24h
	public static final long BACKUP_FREQUENCY = 86400000;

	//public static final String SynkServerURI = "http://109.157.177.172:8080/com.teraim.synkserv/SynkServ";

	//public static final String SynkServerURI = "http://130.211.51.77:8080/synkserv/SynkServ";
	public static final String SynkServerURI = "https://rlotest.slu.se:8080/com.teraim.synkserv/SynkServ";
	public static final Object TRANS_ACK = "TRANSACK";

	




	public static List<ConfigurationModule> getCurrentlyKnownModules(PersistenceHelper globalPh,PersistenceHelper ph,String server, String bundle, LoggerI debugConsole) {
		List<ConfigurationModule> ret = new ArrayList<ConfigurationModule>();
		//Workflow xml. Named same as bundle.

		ret.add(new WorkFlowBundleConfiguration(globalPh,ph,server,bundle,debugConsole));
		ret.add(new SpinnerConfiguration(globalPh,ph,server,bundle,debugConsole));
		ret.add(new GroupsConfiguration(globalPh,ph,server,bundle,debugConsole));		
		//VariableConfiguration depends on the Groups Configuration.
		ret.add(new VariablesConfiguration(globalPh,ph,server,bundle,debugConsole));

		return ret;
	}

	public static void getDBImportModules(
			final PersistenceHelper globalPh, final PersistenceHelper ph, final String server,
			final String bundle, final LoggerI debugConsole,final DbHelper db, final Table t, final AsyncLoadDoneCb asyncLoadDoneCb) {
		final List<ConfigurationModule> ret = new ArrayList<ConfigurationModule>();
		//Workflow xml. Named same as bundle.		
		//ret.add(new GisPolygonConfiguration(globalPh,ph,VORTEX_ROOT_DIR+bundle+AIR_PHOTO_FILE_DIR,debugConsole,db));
		ret.add(new ImportDataConfiguration(globalPh,ph,server,bundle,debugConsole,db,t));

		final String fileFolder = VORTEX_ROOT_DIR+bundle+GIS_FILE_DIR;
		final String serverFolder = server+bundle.toLowerCase()+"/"+Constants.GPS_CONFIG_WEB_FOLDER+"/";

		new DownloadFileTask(new WebLoaderCb() {

			@Override
			public void loaded(List<String> fileNames) {
				if (fileNames!=null)
					Log.d("vortex","loadresult is "+fileNames.toString());
				getAllConfigurationFileNamesFromWebOrFile(fileNames, serverFolder,fileFolder,asyncLoadDoneCb,globalPh,ph,debugConsole,db,ret,t);
			}
		})
		.execute(serverFolder+Constants.GPS_LIST_FILE_NAME);	
		//Try server.

	}

	private static void getAllConfigurationFileNamesFromWebOrFile(List<String> fileNames,
			String serverFolder, String fileFolder, AsyncLoadDoneCb asyncLoadDoneCb, PersistenceHelper globalPh,PersistenceHelper ph, LoggerI debugConsole,DbHelper db, List<ConfigurationModule> modules, Table t) {

		boolean loadFromWeb=false;
		//look for contents.txt file on net.
		if (fileNames!=null) {
			Log.d("vortex","found GIS files list.");
			loadFromWeb = true;
		} else 
			fileNames = getAllConfigurationFileNames(fileFolder);
		if (fileNames!=null && !fileNames.isEmpty()) {
			for (String file:fileNames) {
				if (!loadFromWeb)
					modules.add(new GisObjectConfiguration(globalPh,ph,Source.file,fileFolder,file,debugConsole,db,t));
				else
					modules.add(new GisObjectConfiguration(globalPh,ph,Source.internet,serverFolder,file,debugConsole,db,t));
			}
		} else 
			Log.d("vortex","found no GIS configuration files.");


		asyncLoadDoneCb.onLoadSuccesful(modules);
	}




	private static List<String> getAllConfigurationFileNames(String folderName) {
		List<String> ret = new ArrayList<String>();
		File folder = new File(folderName);
		folder.mkdir();
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles!=null) {
			for (File f:listOfFiles) {
				Log.d("vortex","scanning "+f.getName());
				if (f.isFile() && f.getName().endsWith(".json"))
					ret.add(f.getName().substring(0, f.getName().length()-".json".length()));
			}
		}
		return ret;
	}

	//Historical picture year is currently five years minus current year.
	public static int getHistoricalPictureYear() {
		return Calendar.getInstance().get(Calendar.YEAR)-5;
	}


	private interface WebLoaderCb {

		public void loaded(List<String> result);
	}

	private static class DownloadFileTask extends AsyncTask<String, Void, List<String>> {
		WebLoaderCb cb;

		public DownloadFileTask(WebLoaderCb cb) {
			this.cb=cb;
		}

		protected List<String> doInBackground(String... url) {
		    String inputLine;
		    URL website=null;
		    BufferedReader in;
		    List<String> fileNames = null;
		    try {
		    	website = new URL(url[0]);
		    } catch (MalformedURLException e) {
		        e.printStackTrace();
		    }
		    try {
		        in = new BufferedReader(new InputStreamReader(website.openStream()));
		        
		        while ((inputLine = in.readLine()) != null) {
		        	if (fileNames==null)
		        		fileNames = new ArrayList<String>();
		        	fileNames.add(inputLine);
		        }
		        in.close();

		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    return fileNames;
		}

		protected void onPostExecute(List<String> fileNames) {
			
			cb.loaded(fileNames);
		}
	}






}
