package com.teraim.fieldapp.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.non_generics.Constants;

public class BackupManager {

	private BackupManager singleton;
	private GlobalState gs;
	private Context ctx;

	public BackupManager(GlobalState gs) {
		this.gs = gs;
		this.ctx=gs.getContext();
	}


	public BackupManager getInstance(GlobalState gs) {		
		if (singleton == null)
			singleton = new BackupManager(gs);
		return singleton;
	}


	/*
	public File getBackupStorageDir() {
		gs = GlobalState.getInstance();
		// Get the directory for the user's public pictures directory.
		PersistenceHelper globalPh = gs.getGlobalPreferences();
		String path = null;
		if (globalPh !=null)
			path = globalPh.get(PersistenceHelper.BACKUP_LOCATION);

		if (path==null||path.length()==0) {
			Log.d("vortex","Path was null for backupdir. will try default path");
			path = Constants.DEFAULT_EXT_BACKUP_DIR;
		}

		File backupFolder = new File(path);
		if (!backupFolder.exists()) {
			System.out.println("creating directory: " + Constants.DEFAULT_EXT_BACKUP_DIR);
			boolean result = false;

			try{
				backupFolder.mkdir();
				result = true;
			} catch(SecurityException se){
				//handle it
			}        
			if(result) {    
				System.out.println("DIR created");  
				return backupFolder;
			}
		}


		return null;
	}
	 */
	public boolean backupDatabase() {
		return backupDatabase(Constants.BACKUP_FILE_NAME+"_"+Constants.getSweDate());
	}

	/**
	 * 
	 * @param backupFileName --optional and should normally not be used.
	 * @return
	 */
	public boolean backupDatabase(String backupFileName) {
		String appName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
		Log.d("vortex","starting backup for "+ctx.getDatabasePath(appName));
		File dbFile = ctx.getDatabasePath(appName);

		try {
			
			File dir = createOrFindBackupStorageDir();
			
			if (dir == null) {
				Log.e("vortex","failed to create export folder!!");
				return false;
				
			}
			FileInputStream fis = new FileInputStream(dbFile);			
			String outFileName = dir+"/"+backupFileName;

			// Open the empty db as the output stream
			OutputStream output;

			output = new FileOutputStream(outFileName);

			Log.d("vortex","Output folder: "+outFileName);
			// Transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer))>0){
				output.write(buffer, 0, length);
			}

			// Close the streams
			output.flush();
			output.close();
			fis.close();
		} catch (FileNotFoundException e) {
			Log.e("vortex","backup failed");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e("vortex","backup failed");
			e.printStackTrace();
			return false;
		}
		Log.d("vortex","backup done");
		gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_BACKUP,System.currentTimeMillis());
		return true;
	}
	
	
	public File createOrFindBackupStorageDir() {

		//String state = Environment.getExternalStorageState();
		//Log.d("vortex","ext state: "+state);
		//Log.d("backup stordir: ", Environment.getExternalStorageDirectory().toString());


		//File sdCard = Environment.getExternalStorageDirectory();
		//File dir = new File (sdCard.getAbsolutePath() + "/vortex");
		String backupFolder = gs.getGlobalPreferences().get(PersistenceHelper.BACKUP_LOCATION);
		if (backupFolder==null || backupFolder.isEmpty()) {

			backupFolder = Constants.VORTEX_ROOT_DIR+gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/backup";
			Log.e("vortex","no backup folder configured...reverting to default:\n"+backupFolder);
			//backupFolder = Constants.DEFAULT_EXT_BACKUP_DIR;
		}
		File dir = new File (backupFolder);

		dir.mkdirs();
		return dir;
	}

	public boolean backupExportData(String exportFileName, String data) {

		File dir = createOrFindBackupStorageDir();
		
		if (exportFileName==null) {
			Log.e("vortex","no filename!!");
			return false;
		}
		if (data==null) {
			Log.e("vortex","no data sent to backup!");
			return false;
		}
		
		if (dir == null) {
			Log.e("vortex","failed to create export folder!!");
			return false;
		}
		
		File file = new File(dir, exportFileName);
		BufferedWriter outWriter=null;
		try { 
			FileOutputStream f = new FileOutputStream(file);
			outWriter = new BufferedWriter(new OutputStreamWriter(f),1024);
			// write the whole string
			outWriter.write(data);
			outWriter.close();
		}catch(Exception e){
			System.out.println("Could not write backup file! Filename: "+exportFileName);
			e.printStackTrace();
			try {
				if (outWriter!=null)
					outWriter.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}
		Log.d("vortex","file succesfully written to backup: "+exportFileName);

		return true;


	}




	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}


	public void startBackupIfTimeAndNeed() {
		if (gs.getGlobalPreferences().getB(PersistenceHelper.BACKUP_AUTOMATICALLY)) {
		long timeOfLast = gs.getPreferences().getL(PersistenceHelper.TIME_OF_LAST_BACKUP);
		Log.d("vortex","time of last backup: "+timeOfLast);
		if (System.currentTimeMillis()-timeOfLast>Constants.BACKUP_FREQUENCY) {
			Log.d("vortex","Time to backup!");
			if (!this.backupDatabase()) {
				gs.getLogger().addRow("");
				gs.getLogger().addRedText("Backup of data failed! Please make sure you have configured the backup folder correctly under the config menu.");
			} else {
				gs.getLogger().addRow("");
				gs.getLogger().addGreenText("Your database has been backed up");
			}
			
		} else {
			Log.d("vortex","No backup required");
			gs.getLogger().addRow("");
			gs.getLogger().addGreenText("Database backup not required");
		}
		} else {
			Log.d("vortex","Autobackup is off");
			gs.getLogger().addRow("");
			gs.getLogger().addGreenText("Database auto-backup turned off");
		}
	}

	public boolean restoreDatabase() {
		return restoreDatabase(Constants.BACKUP_FILE_NAME);
	}
	public boolean restoreDatabase(String backupFileName) {
		String appName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
		File dbFile = ctx.getDatabasePath(appName);
		Log.d("vortex","starting restore for "+dbFile);
		
		try {
			
			File dir = createOrFindBackupStorageDir();
			
			if (dir == null) {
				Log.e("vortex","failed to find backup folder named ["+dir+"]");
				return false;
				
			}
			//Close the database. 
			GlobalState.getInstance().getDb().closeDatabaseBeforeExit();
			
			String backupFile = dir+"/"+backupFileName;
			FileInputStream fis = new FileInputStream(backupFile);			
			

			// Output to the db file for the app.
			OutputStream output;

			output = new FileOutputStream(dbFile);

			Log.d("vortex","Restore file: "+backupFile);
			// Transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer))>0){
				output.write(buffer, 0, length);
			}

			// Close the streams
			output.flush();
			output.close();
			fis.close();
		} catch (FileNotFoundException e) {
			Log.e("vortex","restore failed");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e("vortex","restore failed");
			e.printStackTrace();
			return false;
		}
		Log.d("vortex","restore done");
		return true;
	}
}
