package com.teraim.fieldapp.loadermodule;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class Configuration {
	
	private List<ConfigurationModule> mModules;

	
	public Configuration(List<ConfigurationModule> modules) {
		mModules=modules;
	}

		
	
	public ConfigurationModule next() {
		
		for (ConfigurationModule cm:mModules) {
			if (!cm.isLoaded())
				return cm;
		}
		return null;
	}
	
	public ConfigurationModule getModule(String moduleName) {
		for(ConfigurationModule m:mModules) 
			if (m.getFileName().equals(moduleName))
				return m;
		return null;
	}
	public List<ConfigurationModule> getAll() {
		return mModules;
	}
	
	
}
