package com.teraim.fieldapp.loadermodule;


public class LoadResult {
	
	public enum ErrorCode {
		ok,
		newVarPatternVersionLoaded,
		loaded,
		bothFilesLoaded,
		notFound,
		ParseError,
		IOError,
		sameold, 
		whatever, 
		configurationError, 
		Aborted, 
		LoadInBackground, newConfigVersionLoaded, 
		BadURL,frozen, parsed, noData, thawed, 
		ClassNotFound, nothingToFreeze, Unsupported, 
		reloadDependant, existingVersionIsMoreCurrent, majorVersionNotUpdated
	}
	
	
	public ErrorCode errCode;
	public ConfigurationModule module;
	public String errorMessage;


	public LoadResult(ConfigurationModule module,ErrorCode errC,String errM) {
		errCode = errC;
		this.module=module;
		errorMessage=errM;
	}

	public LoadResult(ConfigurationModule module, ErrorCode errC) {
		errCode = errC;
		this.module = module;
	}

	public LoadResult(ErrorCode configurationerror, Object object) {
		// TODO Auto-generated constructor stub
	}
}
