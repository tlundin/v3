package com.teraim.fieldapp.dynamic.blocks;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.teraim.fieldapp.FileLoadedCb;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.AsyncResumeExecutorI;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.MapGisLayer;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.ConfigurationModule.Source;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.WebLoader;
import com.teraim.fieldapp.loadermodule.configurations.AirPhotoMetaData;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Tools.Unit;
import com.teraim.fieldapp.utils.Tools.WebLoaderCb;

public class CreateGisBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2013870148670474254L;
	private static final int MAX_NUMBER_OF_PICS = 100;
	private final String name,source,containerId,N,E,S,W;
	Unit unit;
	GlobalState gs;
	boolean isVisible = false,showHistorical;
	String format;

	private Cutout cutOut=null;

	private WF_Context myContext;
	private LoggerI o;
	private boolean hasSatNav;
	private WF_Gis_Map gis=null;
	private List<EvalExpr> sourceE;
	private int loadCount=0;
	private boolean aborted=false;
	private List<MapGisLayer> mapLayers;
	public boolean hasCarNavigation() {
		return hasSatNav;
	}

	public CreateGisBlock(String id,String name, 
			String containerId,boolean isVisible,String source,String N,String E, String S,String W, boolean hasSatNav) {
		super();

		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.sourceE=Expressor.preCompileExpression(source);
		this.source=source;
		this.N=N;
		this.E=E;
		this.S=S;
		this.W=W;
		this.hasSatNav=hasSatNav;

	}




	//Callback after image has loaded.
	AsyncResumeExecutorI cb;
	private int imageHeight;
	private int imageWidth;



	/**
	 * 
	 * @param myContext
	 * @param cb
	 * @return true if loaded. False if executor should pause.
	 */

	public boolean create(WF_Context myContext,final AsyncResumeExecutorI cb) {
		mapLayers  = new ArrayList<MapGisLayer>();
		aborted = false;
		Log.d("vortex","in create for createGisBlock!");
		Context ctx = myContext.getContext();
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.cb=cb;
		this.myContext = myContext;
		PersistenceHelper ph = gs.getPreferences();
		PersistenceHelper globalPh = gs.getGlobalPreferences();

		final String serverFileRootDir = server(globalPh.get(PersistenceHelper.SERVER_URL))+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase()+"/extras/";
		final String cacheFolder = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/";


		if (sourceE==null ) {
			Log.e("vortex","Image url evaluates to null! GisImageView will not load");
			o.addRow("");
			o.addRedText("GisImageView failed to load. No picture defined or failure to parse: "+source);
			//continue execution immediately.
			return true;
		}

		final String pics = Expressor.analyze(sourceE);
		Log.d("vortex","ParseString result: "+pics);
		//Load asynchronously. Put up a loadbar.
		//Need one async load per image.
		String[] picNames=null;
		if (pics!=null && pics.contains(",")) {
			picNames = pics.split(",");
			Log.d("vortex","found "+picNames.length+" pics");

		} else {
			picNames = new String[1];
			picNames[0]=pics;
		}
//		boolean allLoaded=false;
		final String masterPicName = picNames[0];

		for (int i=0;i<picNames.length;i++) {
			final boolean isLast = (i == picNames.length-1);
			final int I = i;
			final String picName = picNames[i];
			Tools.onLoadCacheImage(serverFileRootDir, picName, cacheFolder, new WebLoaderCb() {
				@Override
				public void progress(int bytesRead) {

				}
				@Override
				public void loaded(Boolean result) {
					if (!aborted) {
						if (result) {
							Log.d("vortex", "picture " + picName + " now in cache.");
							MapGisLayer mapLayer;
							if (picName.equals(masterPicName)) {
								mapLayer = new MapGisLayer(gis, GisConstants.DefaultTag, picName);

							} else
								mapLayer = new MapGisLayer(gis, "bg" + (I), picName);
							Log.d("vortex", "Added layer: " + mapLayer.getLabel());
							mapLayers.add(mapLayer);
						} else {
							Log.e("vortex", "Picture not found. "+serverFileRootDir+picName);
							o.addRow("");
							o.addRedText("GisImageView failed to load. File not found: " + serverFileRootDir + picName);
							if (picName.equals(masterPicName)) {
								aborted = true;
								cb.abortExecution("GisImageView failed to load. File not found: " + serverFileRootDir + picName);
							}

						}

						if (isLast)
							loadImageMetaData(masterPicName, serverFileRootDir, cacheFolder);

					}

				}
			});
		}
		return false;
	}

	Rect r = null;
	PhotoMeta photoMetaData;
	String cachedImgFilePath="";
	
	public void createAfterLoad(PhotoMeta photoMeta, final String cacheFolder, final String fileName) {
		this.photoMetaData=photoMeta;
		cachedImgFilePath = cacheFolder+fileName;
		final Container myContainer = myContext.getContainer(containerId);

		if (myContainer!=null && photoMetaData!=null) {
			LayoutInflater li = LayoutInflater.from(myContext.getContext());
			final FrameLayout mapView = (FrameLayout)li.inflate(R.layout.image_gis_layout, null);
			final View avstRL = mapView.findViewById(R.id.avstRL);
			final View createMenuL = mapView.findViewById(R.id.createMenuL);

			r=null;

			if (cutOut==null) {
				boolean found = false;
				GisLayer masterLayer = null;
				for (MapGisLayer layer:mapLayers) {
					if (layer.isVisible()) {
						cachedImgFilePath = cacheFolder + layer.getImageName();
						found = true;
						Log.d("gurk","layer with name "+layer.getLabel()+" now visible" );
						break;
					}
					if (layer.getLabel().equals(GisConstants.DefaultTag)) {
						Log.d("gurk","masterlayer found.");
						masterLayer = layer;
					}
				}
				if (!found) {
					masterLayer.setVisible(true);
					Log.d("gurk","masterlayer is now visible.");
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(cachedImgFilePath, options);
				imageHeight = options.outHeight;
				imageWidth = options.outWidth;
				Log.d("vortex","image rect h w is "+imageHeight+","+imageWidth);

				r = new Rect(0,0,imageWidth,imageHeight);

			} else {
				//This is a cutout. pop the correct slice specification from the stack. 
				r = cutOut.r;
				Location topC = cutOut.geoR.get(0);
				Location botC = cutOut.geoR.get(1);
				photoMetaData = new PhotoMeta(topC.getY(),botC.getX(),botC.getY(),topC.getX());
				cutOut=null;
				mapLayers.clear();
			}
			
			new Handler().postDelayed(new Runnable() {
				public void run() {

					Bitmap bmp = Tools.getScaledImageRegion(myContext.getContext(),cachedImgFilePath,r);
					if (bmp!=null) {

						gis = new WF_Gis_Map(CreateGisBlock.this,r,blockId, mapView, isVisible, bmp,myContext,photoMetaData,avstRL,createMenuL,myLayers,imageWidth,imageHeight);

						//need to throw away the reference to myLayers.
						myLayers=null;
						myContainer.add(gis);
						myContext.addGis(gis.getId(),gis);
						myContext.addEventListener(gis, EventType.onSave);
						myContext.addEventListener(gis, EventType.onFlowExecuted);
						myContext.addDrawable(name,gis);
						final View menuL = mapView.findViewById(R.id.mmenuL);

						menuL.setVisibility(View.INVISIBLE);
						avstRL.setVisibility(View.INVISIBLE);
						final ImageButton menuB = (ImageButton)mapView.findViewById(R.id.menuB);

							for (GisLayer gl:mapLayers) {
								gis.addLayer(gl);
							}


						menuB.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {

								int menuState = menuL.getVisibility();
								if (menuState == View.VISIBLE) {
									menuL.setVisibility(View.INVISIBLE);
									Log.d("vortex","setclickabletotrue");
									gis.getGis().setClickable(true);
								}
								else {
									gis.initializeLayersMenu(gis.getLayers());
									menuL.setVisibility(View.VISIBLE);
									Log.d("vortex","setclickabletofalse");
									gis.getGis().setClickable(false);
								}
							}
						});
						cb.continueExecution();
					} else {
						Log.e("vortex","Failed to create map image. Will exit");
						cb.abortExecution("Failed to create GisImageView. The map file ["+cachedImgFilePath+"] could not be parsed" );
					} 
				}
			}, 0);
			
		} else {
			o.addRow("");
			if (photoMetaData ==null) {
				Log.e("vortex","Photemetadata null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Photometadata missing (the boundaries of the image on the map)");
				cb.abortExecution("Adding GisImageView to "+containerId+" failed. Photometadata missing (the boundaries of the image on the map)");
			}
			else {
				Log.e("vortex","Container null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Container cannot be found in template");
				cb.abortExecution("Missing container for GisImageView: "+containerId);
			}
		}


	}


	private void loadImageMetaData(String picName,String serverFileRootDir,String cacheFolder) {		

		if (N==null||E==null||S==null||W==null||N.length()==0) {
			//Parse and cache the metafile.
			loadImageMetaFromFile(picName, serverFileRootDir,cacheFolder);
		}
		else {
			Log.e("vortex","Found tags for photo meta");
			createAfterLoad(new PhotoMeta(N,E,S,W),cacheFolder,picName);
		}
	}

	//User parser to parse and cache xml.

	private void loadImageMetaFromFile(final String fileName,String serverFileRootDir,final String cacheFolder) {

		//cut the .jpg type ending.
		String[]tmp = fileName.split("\\.");

		if (tmp!=null && tmp.length!=0) {
			final String metaFileName = tmp[0];			
			Log.d("vortex","metafilename: "+metaFileName);
			final ConfigurationModule meta = new AirPhotoMetaData(GlobalState.getInstance().getGlobalPreferences(),
					GlobalState.getInstance().getPreferences(),Source.internet,
					serverFileRootDir,metaFileName,""); 
			if (meta.thaw().errCode!=ErrorCode.thawed) {
				Log.d("vortex","no frozen metadata. will try to download.");
				new WebLoader(null, null, new FileLoadedCb(){
					@Override
					public void onFileLoaded(LoadResult res) {

						if (res.errCode==ErrorCode.frozen) {
							PhotoMeta pm = (PhotoMeta)meta.getEssence();
							Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
							createAfterLoad(pm,cacheFolder,fileName);
						}
						else {
							o.addRow("");
							o.addRedText("Could not find GIS image "+metaFileName);
							Log.e("vortex","Failed to parse image meta. Errorcode "+res.errCode.name());
							cb.abortExecution("Could not load GIS image meta file ["+metaFileName+"]. Likely reason: File missing under 'extras' folder or no connection");
						}
					}
					@Override
					public void onFileLoaded(ErrorCode errCode, String version) {
						Log.e("vortex","Error loading foto metadata! ErrorCode: "+errCode);
					}
					@Override
					public void onUpdate(Integer... args) {
					}},
					"No control").execute(meta);
			} else {
				Log.d("vortex","Found frozen metadata. Will use it");
				PhotoMeta pm = (PhotoMeta)meta.getEssence();
				Log.d("vortex","img N, W, S, E "+pm.N+","+pm.W+","+pm.S+","+pm.E);
				createAfterLoad(pm,cacheFolder,fileName);
			}
		}
	}


	private class Cutout {
		Rect r;
		List<Location> geoR;
	}


	//Reloads current flow with a new viewport.
	//Cache for layers.
	List<GisLayer> myLayers=null;

	public void setCutOut(Rect r, List<Location> geoR, List<GisLayer> myLayers) {
		cutOut = new Cutout();
		cutOut.r = r;
		cutOut.geoR = geoR;
		this.myLayers = myLayers;
	}

	public String server(String serverUrl) {
		if (!serverUrl.endsWith("/"))
			serverUrl+="/";
		if (!serverUrl.startsWith("http://")) 
			serverUrl = "http://"+serverUrl;
		return serverUrl;
	}

	
	


}


