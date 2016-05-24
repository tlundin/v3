package com.teraim.fieldapp.gis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.MapGisLayer;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPathObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.Tools;

public class GisImageView extends GestureImageView implements TrackerListener {

	private final static String Deg = "\u00b0";

	//Various paints.
	private Paint txtPaint;
	private Paint polyPaint;
	private Paint rCursorPaint;
	private Paint blCursorPaint;
	private Paint btnTxt,vtnTxt;
	private Paint grCursorPaint;
	private Paint selectedPaint;
	private Paint borderPaint;
	private Paint fgPaintSel;
	private Paint  wCursorPaint,bCursorPaint,markerPaint;
	private Paint paintBlur;
	private Paint paintSimple;

	private Handler handler;
	private Context ctx;
	private Calendar calendar = Calendar.getInstance();

	//Photometadata for the current view.
	private PhotoMeta photoMetaData;
	private Variable myX,myY;
	private static final Map<String,String>YearKeyHash = new HashMap<String,String>();

	private static final int LabelOffset = 5;

	//Long click or short click?
	private boolean clickWasShort=false;

	//To avoid creating a new rect in onDraw.
	private Rect rectReuse = new Rect();

	//The user Gis Point Object
	private GisPointObject userGop;

	//The bag and layer that contains the object currently clicked.
	private Set<GisObject> touchedBag;
	private GisLayer touchedLayer;
	private GisObject touchedGop = null;



	private Set<GisObject> candidates = new TreeSet<GisObject>(new Comparator<GisObject>() {
		@Override
		public int compare(GisObject lhs, GisObject rhs) {
			return (int)(lhs.getDistanceToClick()-rhs.getDistanceToClick());
		}
	});

	private LocationManager locationManager;



	public GisImageView(Context context) {
		super(context);
		init(context);
	}

	public GisImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public GisImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context ctx) {

		this.setClickable(true);

		locationManager = (LocationManager) ctx
				.getSystemService(Context.LOCATION_SERVICE);
		YearKeyHash.clear();
		YearKeyHash.put("år", Constants.getYear());
		this.ctx=ctx;
		//used for cursor blink.
		calendar.setTime(new Date());
		grCursorPaint = new Paint();
		grCursorPaint.setColor(Color.GRAY);
		grCursorPaint.setStyle(Paint.Style.FILL);
		blCursorPaint = new Paint();
		blCursorPaint.setColor(Color.BLUE);
		blCursorPaint.setStyle(Paint.Style.FILL);
		rCursorPaint = new Paint();
		rCursorPaint.setColor(Color.RED);
		rCursorPaint.setStyle(Paint.Style.FILL);
		wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Paint.Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Paint.Style.FILL);
		markerPaint = new Paint();
		markerPaint .setColor(Color.YELLOW);
		markerPaint .setStyle(Paint.Style.FILL);
		txtPaint = new Paint();
		txtPaint.setTextSize(8);
		txtPaint.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.STROKE);
		txtPaint.setTextAlign(Paint.Align.CENTER);
		selectedPaint = new Paint();
		selectedPaint.setTextSize(8);
		selectedPaint.setColor(Color.BLACK);
		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setTextAlign(Paint.Align.CENTER);
		btnTxt = new Paint();
		btnTxt.setTextSize(8);
		btnTxt.setColor(Color.WHITE);
		btnTxt.setStyle(Paint.Style.STROKE);
		btnTxt.setTextAlign(Paint.Align.CENTER);
		vtnTxt = new Paint();
		vtnTxt.setTextSize(8);
		vtnTxt.setColor(Color.WHITE);
		vtnTxt.setStyle(Paint.Style.STROKE);
		vtnTxt.setTextAlign(Paint.Align.CENTER);
		borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(3);
		polyPaint = new Paint();
		polyPaint.setColor(Color.WHITE);
		polyPaint.setStyle(Paint.Style.STROKE);
		polyPaint.setStrokeWidth(0);

		fgPaintSel = new Paint();
		fgPaintSel.setColor(Color.YELLOW);
		fgPaintSel.setStyle(Paint.Style.STROKE);
		fgPaintSel.setStrokeWidth(2);

		paintSimple = new Paint();
		paintSimple.setAntiAlias(true);
		paintSimple.setDither(true);
		paintSimple.setColor(Color.argb(248, 255, 255, 255));
		paintSimple.setStrokeWidth(2f);
		paintSimple.setStyle(Paint.Style.STROKE);
		paintSimple.setStrokeJoin(Paint.Join.ROUND);
		paintSimple.setStrokeCap(Paint.Cap.ROUND);

		paintBlur = new Paint();
		paintBlur.set(paintSimple);
		paintBlur.setColor(Color.argb(235, 74, 138, 255));
		paintBlur.setStrokeWidth(5f);
		paintBlur.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));


		if (GlobalState.getInstance()!=null&&GlobalState.getInstance().getTracker()!=null)
			GlobalState.getInstance().getTracker().registerListener(this);


	}
	double pXR,pYR;
	private WF_Gis_Map myMap;
	private boolean allowZoom;



	/**
	 *
	 * @param wf_Gis_Map 	The map object
	 * @param pm			The geo coordinates for the image corners
	 * @param allowZoom			If extreme zoom is enabled
	 * @return
	 */
	public void initialize(WF_Gis_Map wf_Gis_Map, PhotoMeta pm,boolean allowZoom) {

		if (pm== null) {
			Log.e("vortex","Photometadata was null for "+wf_Gis_Map.getName());
		}
		mapLocationForClick=null;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getHeight();

		//Filter away all objects not visible and create cached values for all gisobjects on this map and zoom level.
		myMap = wf_Gis_Map;



		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
		myX = GlobalState.getInstance().getVariableCache().getVariable(YearKeyHash, NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableCache().getVariable(YearKeyHash, NamedVariables.MY_GPS_LONG);
		this.allowZoom = allowZoom;


		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("vortex","Gets short! Clickable is "+GisImageView.this.isClickable());
				if (clickXY!=null || !GisImageView.this.isClickable())
					return;

				calculateMapLocationForClick(polyVertexX,polyVertexY);


				if (gisTypeToCreate!=null) {
					//GisObject newP = StaticGisPoint(gisTypeToCreate, Map<String, String> keyChain,Location myLocation, Variable statusVar)

					if (newGisObj ==null) {
						Set<GisObject> bag;
						for (GisLayer l:myMap.getLayers()) {

							bag = l.getBagOfType(gisTypeToCreate.getName());
							if (bag!= null) {
								Log.d("vortex","found correct bag!");
								newGisObj = createNewGisObject(gisTypeToCreate,bag);
								if (gisTypeToCreate.getGisPolyType()==GisObjectType.Point)
									myMap.setVisibleCreate(true,newGisObj.getLabel());
							}
						}
					}
					if (newGisObj !=null) {

						//Add new point.
						if (newGisObj instanceof GisPathObject)
							newGisObj.getCoordinates().add(mapLocationForClick);

					} else
						Log.e("vortex","New GisObj is null!");
				} else
					Log.e("vortex","gistype null!");
				clickWasShort = true;
				invalidate();

			}
		});



		setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Log.d("vortex","Gets long! clickable is "+GisImageView.this.isClickable());
				if (clickXY!=null || !GisImageView.this.isClickable())
					return false;
				calculateMapLocationForClick(polyVertexX,polyVertexY);
				clickWasShort = false;
				invalidate();
				return true;
			}


		});

		//Remove any gis objects outside current viewport.
		initializeAndSiftGisObjects();

	}


	/**
	 *
	 * @return copy only the gis objects in the layers that are visible in this view.
	 */
	public void initializeAndSiftGisObjects() {
		if (myMap==null) {
			Log.e("vortex","myMap null in initializeAndSiftGisObjects");
			return;
		}
		for (GisLayer layer :myMap.getLayers()) {

			layer.filterLayer(this);

		}

	}




	private float fixedX=-1;
	private float fixedY;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (fixedX==-1) {
			fixedY=y;
			fixedX=x;
			Log.d("vortex","fixed xy"+fixedX+","+fixedY);
		}
	}


	PhotoMeta gisImage;
	//difference in % between ruta and image size.

	private Location mapLocationForClick=null;
	private float[] clickXY;
	private double imgHReal;
	private double imgWReal;

	private GisObject newGisObj;

	private Set<GisObject> currentCreateBag;

	private float[] translateToReal(float mx,float my) {
		float fixScale = scale * scaleAdjust;
		Log.d("vortex","fixscale: "+fixScale);
		mx = (mx-x)/fixScale;
		my = (my-y)/fixScale;
		return new float[]{mx,my};
	}


	private Location translateRealCoordinatestoMap(float[] xy) {

		float rX = this.getImageWidth()/2+((float)xy[0]);
		float rY = this.getImageHeight()/2+((float)xy[1]);
		pXR = photoMetaData.getWidth()/this.getImageWidth();
		pYR = photoMetaData.getHeight()/this.getImageHeight();
		double mapDistX = rX*pXR;
		double mapDistY = rY*pYR;
		return new SweLocation(mapDistX + photoMetaData.W,photoMetaData.N-mapDistY);

	}

	/**
	 *
	 * @param l	- the location to examine
	 * @param xy - the translated x and y into screen coordinates.
	 * @return true if the coordinate is inside the current map.
	 */
	public boolean translateMapToRealCoordinates(Location l, int[] xy) {
		//Unknown location, surely outside.
		if (l == null) {
			xy=null;
			return false;
		}
		//Assume it is inside
		boolean isInside = true;
		double mapDistX = l.getX()-photoMetaData.W;
		if (mapDistX <=imgWReal && mapDistX>=0);
			//Log.d("vortex","Distance X in meter: "+mapDistX+" [inside]");
		else {

			//Log.e("vortex","Distance X in meter: "+mapDistX+" [outside!]");
			//Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			//Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			//Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			//Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());

			//No, it is outside.
			isInside = false;
		}
		double mapDistY = l.getY()-photoMetaData.S;
		if (mapDistY <=imgHReal && mapDistY>=0)
			;//Log.d("vortex","Distance Y in meter: "+mapDistY+" [inside]");
		else {
			//Log.e("vortex","Distance Y in meter: "+mapDistY+" [outside!]");
			//Log.d("vortex","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
			//Log.d("vortex","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
			//Log.d("vortex","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
			//Log.d("vortex","object X,Y: "+l.getX()+","+l.getY());
			isInside = false;
		}
		pXR = this.getImageWidth()/photoMetaData.getWidth();
		pYR = this.getImageHeight()/photoMetaData.getHeight();

		//		Log.d("vortex","px, py"+pXR+","+pYR);
		double pixDX = mapDistX*pXR;
		double pixDY = mapDistY*pYR;
		//Log.d("vortex","distance on map (in pixel no scale): x,y "+pixDX+","+pixDY);
		float rX = ((float)pixDX)-this.getImageWidth()/2;
		float rY = this.getImageHeight()/2-((float)pixDY);

		//		Log.d("vortex","X: Y:"+x+","+y);
		//		Log.d("vortex","fixScale: "+fixScale);
		//		Log.d("vortex","after calc(x), calc(y) "+rX+","+rY);
		//		Log.d("vortex","fX: fY:"+fixedX+","+fixedY);
		//		Log.d("vortex","after fcalc(x), fcalc(y) "+this.fCalcX(rX)+","+fCalcY(rY));
		xy[0]=(int)rX;
		xy[1]=(int)rY;

		return isInside;

	}

	public Location calculateMapLocationForClick(float x, float y) {
		//Figure out geo coords from pic coords.
		clickXY=translateToReal(x,y);
		mapLocationForClick = translateRealCoordinatestoMap(clickXY);
		Log.d("vortex","click at "+x+","+y);
		Log.d("vortex","click at "+mapLocationForClick.getX()+","+mapLocationForClick.getY());
		return mapLocationForClick;
	}

	private GisObject createNewGisObject(
			FullGisObjectConfiguration gisTypeToCreate, Set<GisObject> bag) {
		//create object or part of object.
		Map<String, String> keyHash = Tools.copyKeyHash(gisTypeToCreate.getObjectKeyHash().getContext());
		GisObject ret=null;

		//break if no keyhash.
		if (keyHash!=null) {
			String uid = UUID.randomUUID().toString();
			Log.d("vortex","HACK: Adding uid: "+uid);
			keyHash.put("uid", uid);
			Log.d("vortex","keyhash for new obj is: "+keyHash.toString());
			List<Location> myDots;

			switch (gisTypeToCreate.getGisPolyType()) {

				case Point:
					ret = new StaticGisPoint(gisTypeToCreate,keyHash,mapLocationForClick, null);
					int[] xy = new int[2];
					translateMapToRealCoordinates(mapLocationForClick,xy);
					((StaticGisPoint)ret).setTranslatedLocation(xy);
					break;

				case Linestring:
					myDots = new ArrayList<Location>();
					ret = new GisMultiPointObject(gisTypeToCreate, keyHash,myDots);
					break;
				case Polygon:
					ret = new GisPolygonObject(gisTypeToCreate, keyHash,
							"",GisConstants.SWEREF,null);
					break;

			}
			//Log.d("vortex","Adding "+ret.toString()+" to bag "+bag.toString());
			ret.markAsUseful();
			bag.add(ret);

			//save layer and object for undo.
			currentCreateBag = bag;

		} else
			Log.e("vortex","Cannot create, object keyhash is null!!!");
		return ret;
	}

	//returns true if there is no more backing up possible.
	public void goBack() {
		if (newGisObj!=null) {
			if (newGisObj instanceof StaticGisPoint) {
				cancelGisObjectCreation();


			} else if (newGisObj instanceof GisPathObject) {
				List<Location> myDots = newGisObj.getCoordinates();
				if (myDots!=null && !myDots.isEmpty())
					myDots.remove(myDots.size()-1);

				if (myDots==null ||myDots.isEmpty()) {
					cancelGisObjectCreation();
					Log.d("vortex","Go BACK: NewGisObj removed");
				} else
					//invalidate done inside cancelGisObjectCreation for other case.
					invalidate();
			}

		}
	}

	public void createOk() {
		//if this is a path, close it.
		if (newGisObj instanceof GisPolygonObject)
			((GisPathObject)newGisObj).getPath().close();
		if (currentCreateBag!=null) {
			Log.d("vortex","inserting new GIS object.");
			GlobalState.getInstance().getDb().insertGisObject(newGisObj);

			gisTypeToCreate=null;
			touchedBag = currentCreateBag;
			currentCreateBag=null;
			touchedGop = newGisObj;
			newGisObj=null;
			//Log.d("vortex","Here touched is TG TB"+touchedGop.toString()+","+touchedBag.toString());
			myMap.setVisibleAvstRikt(true,touchedGop);

			this.redraw();
		} else
			Log.e("vortex","cannot create...createbag null");
	}

	Drawable d1;

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();
		//scale and adjust.
		try {
			float adjustedScale = scale * scaleAdjust;
			if (allowZoom && adjustedScale>2.0f && this.getDrawable()!=null)
				myMap.setZoomButtonVisible(true);
			else
				myMap.setZoomButtonVisible(false);
			canvas.translate(x, y);
			if(adjustedScale != 1.0f) {
				canvas.scale(adjustedScale, adjustedScale);
			}

			candidates.clear();

			for (GisLayer layerO:myMap.getLayers()) {
				//String layerId = layerO.getId();
				//Log.d("vortex","drawing layer "+layerId);
				//get all objects that should be drawn on this layer.
				//Treat maplayers
				if (!layerO.isVisible()) {
					//Log.d("vortex","layer not visible...skipping "+layerId+" Obj: "+layerO.toString());
					continue;
				}

				//if (layerO.hasDynamic()) {
				//Log.d("vortex","dynamic obj found in "+layer);
				//	this.startDynamicRedraw();
				//}
				//only allow clicks if something is not already touched.
				pXR = this.getImageWidth()/photoMetaData.getWidth();
				pYR = this.getImageHeight()/photoMetaData.getHeight();
				Map<String, Set<GisObject>> bags = layerO.getGisBags();
				Map<String, Set<GisFilter>> filterMap = layerO.getFilters();

				if (bags!=null && !bags.isEmpty()) {

					for (String key:bags.keySet()) {
						Set<GisFilter> filters = filterMap!=null?filterMap.get(key):null;
						Set<GisObject> bagOfObjects = bags.get(key);
						Iterator<GisObject> iterator = bagOfObjects.iterator();
						//Log.d("vortex","CAPROX Bag "+ " has "+bagOfObjects.size()+" members");
						while (iterator.hasNext()) {
							GisObject go = iterator.next();
							//If not inside map, or if touched, skip.
							if (!go.isUseful() || (touchedGop!=null&&go.equals(touchedGop)))
								continue;
							if (go instanceof GisPointObject) {
								GisPointObject gop = (GisPointObject)go;

								if (gop.isDynamic()) {
									//Log.d("vortex","found dynamic object");
									int[] xy = new int[2];
									boolean inside = translateMapToRealCoordinates(go.getLocation(),xy);
									if (!inside) {
										//Log.d("vortex","outside!");
										if (gop.equals(userGop)) {
											myMap.showCenterButton(false);
											userGop=null;
										}
										//This object should not be drawn.
										continue;
									} else {
										//Log.d("vortex","inside!");
										if (gop.isUser()) {
											//Log.d("vortex","user!");
											userGop = gop;
											myMap.showCenterButton(true);
										}
										gop.setTranslatedLocation(xy);
									}


								}
								Bitmap bitmap = gop.getIcon();
								float radius = gop.getRadius();
								String color = gop.getColor();
								Style style = gop.getStyle();
								PolyType polyType=gop.getShape();

								String statusColor = colorShiftOnStatus(gop.getStatusVariable());
								if (statusColor!=null)
									color = statusColor;

								if (filters!=null&&!filters.isEmpty()) {

									//Log.d("vortex","has filter!");

									for (GisFilter filter:filters) {
										if (filter.isActive()) {
											//Log.d("vortex","Filter active!");

											if (!gop.hasCachedFilterResult(filter)) {
												Boolean result = Expressor.analyzeBooleanExpression(filter.getExpression(),gop.getKeyHash());
												gop.setCachedFilterResult(filter,result);
											}
											if ( gop.getCachedFilterResult(filter)) {
												Log.d("vortex","FILTER MATCH FOR FILTER: "+filter.getLabel());
												bitmap = filter.getBitmap();
												radius = filter.getRadius();
												color = filter.getColor();
												style = filter.getStyle();
												polyType = filter.getShape();
											}
										} else
											Log.d("vortex","Filter turned off!");
									}
								}
								int[] xy = gop.getTranslatedLocation();
								if (xy!=null) {
									//Log.d("vortex","Calling drawpoint in dispatchdraw for "+go.getLabel());
									drawPoint(canvas,bitmap,radius,color,style,polyType,xy,adjustedScale);
									if (layerO!=null && layerO.showLabels()) {
										drawGopLabel(canvas,xy,gop.getLabel(),LabelOffset,bCursorPaint,txtPaint);
									}
								}
								/*


								 */

							} else if (go instanceof GisPathObject) {
								if (go instanceof GisMultiPointObject) {
									if (!((GisMultiPointObject)go).isLineString()) {
										Log.d("vortex","This is a multipoint. Path is useless.");
									}
								}

								drawGop(canvas,layerO,go,false);
							}

							//Check if an object has been clicked in this layer.
							if (touchedGop==null && gisTypeToCreate == null && mapLocationForClick!=null && go.isTouchedByClick(mapLocationForClick,pXR,pYR) && !go.equals(userGop))
								candidates.add(go);
						}
					}
				}
			}

			//Special rendering of touched gop.
			if (!candidates.isEmpty()) {
				//Candidates are sorted in a set. Return first.
				String candidatesS="";
				for (GisObject go:candidates) {
					candidatesS+=go.getLabel()+" Distance: "+go.getDistanceToClick()+"\n";
				}
				Log.d("vortex","DEBUG: Members of candidateS: \n"+candidatesS);
				touchedGop = candidates.iterator().next();
				if (touchedGop!=null)
					Log.d("vortex","Gop selected now has Keychain: "+touchedGop.getKeyHash());
				//Find the layer and bag touched.
				for (GisLayer layer:myMap.getLayers()) {
					touchedBag = layer.getBagContainingGo(touchedGop);
					if (touchedBag!=null) {
						Log.d("vortex","setting touchedlayer");
						touchedLayer = layer;
						break;
					}
				}
				if (touchedBag!=null) {
					//if longclick, open the actionbar menu.
					if (!clickWasShort)
						myMap.startActionModeCb();
					else {
						myMap.setVisibleAvstRikt(true,touchedGop);
						displayDistanceAndDirection();
					}
				} else {
					Log.e("vortex","The touched object does not belong to a bag.");
					touchedGop=null;
				}
				candidates.clear();

			}

			if (touchedGop!=null) {

				if (clickWasShort && (riktLinjeStart == null || riktLinjeEnd == null) && mostRecentGPSValueTimeStamp!=-1 && myX!=null&&myY!=null&&myX.getValue()!=null && myY.getValue()!=null) {
					//Create a line from user to object.
					double mX = Double.parseDouble(myX.getValue());
					double mY = Double.parseDouble(myY.getValue());
					riktLinjeStart = new int[2];riktLinjeEnd = new int[2];
					translateMapToRealCoordinates(new SweLocation(mX,mY),riktLinjeStart);
					translateMapToRealCoordinates(touchedGop.getLocation(),riktLinjeEnd);
				}
				//if (touchedLayer == null)
				//	Log.d("vortex","TOUCHEDLAYER WAS NULL. TouchedBag was: "+touchedBag);

				drawGop(canvas,touchedLayer,touchedGop,true);
			}
		} catch(Exception e) {
			if (GlobalState.getInstance()!=null) {
				LoggerI o = GlobalState.getInstance().getLogger();
				if (o!=null) {
					o.addRow("");

					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					o.addRedText(sw.toString());
					e.printStackTrace();
				}
			}
		}

		if (newGisObj!=null) {
			double lengthOfPath = 0;
			myMap.setVisibleCreate(true,newGisObj.getLabel());
			//Show ok button only after at least 2 points defined.
			boolean showOk = true;
			if (newGisObj instanceof GisPathObject) {
				List<Location> myDots = newGisObj.getCoordinates();
				showOk = (myDots!=null && myDots.size()>1);
				lengthOfPath = Geomatte.lengthOfPath(myDots);

			}
			myMap.setVisibleCreateOk(showOk);
			myMap.showLength(lengthOfPath);

		} else
			myMap.setVisibleCreate(false,"");


		canvas.restore();
		//Reset any click done.
		mapLocationForClick=null;
		clickXY=null;

	}

	/*
	private void startShowLabelTimer() {
		final int interval = 1000; // 1 Second
		Handler handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				showLabelForAWhile=false;
				postInvalidate();
			}
		};

		handler.postDelayed(runnable, interval);
	}
	 */
	private String colorShiftOnStatus(Variable statusVar) {
		String color=null;
		if (statusVar!=null ) {
			String value = statusVar.getValue();
			if (value.equals("1")) {
				color = "yellow";
			}
			else if (value.equals("2")) {
				color = "red";
			}
			else if	(value.equals("3")) {
				color = "green";
			}
		}
		return color;
	}

	private void drawGop(Canvas canvas, GisLayer layerO, GisObject go, boolean selected) {

		boolean beingDrawn = false;
		if (newGisObj !=null)
			beingDrawn = go.equals(newGisObj);
		//will only be called from here if selected.
		GisPointObject gop=null;
		//Only gets her for Gispoint, if it is selected.
		if (go instanceof GisPointObject) {
			gop = (GisPointObject)go;
			if (gop.getTranslatedLocation()!=null) {
				//Log.d("vortex","Calling drawpoint in dispatchdraw for "+go.getLabel());
				drawPoint(canvas, null,gop.getRadius(), "red", Style.FILL, gop.getShape(), gop.getTranslatedLocation(),1);
			} else
				Log.e("vortex","NOT calling drawpoint since translatedlocation was null");
		}

		else if (go instanceof GisPathObject) {

			GisPathObject gpo = (GisPathObject)go;
			//Add glow effect if it is currently being drawn.

			boolean onlyOne =false;

			Path p = gpo.getPath();

			//objects being drawn cannot be cached.
			if (p==null || beingDrawn) {
				p = new Path();
				List<Location> ll = go.getCoordinates();
				onlyOne = ll.size()==1;
				Log.d("vortex","Only one is "+onlyOne);
				boolean first = true;
				if (ll!=null) {
					int[] xy = new int[2];
					for (Location l:ll) {
						translateMapToRealCoordinates(l,xy);
						if (xy==null)
							continue;
						if (onlyOne) {
							drawPoint(canvas, null,2, "white", Style.STROKE, PolyType.circle, xy,1);
							break;
						} else {
							if (first) {
								p.moveTo(xy[0],xy[1]);
								first =false;
							} else
								p.lineTo(xy[0],xy[1]);
						}
					}
				}

			}
			if (!onlyOne) {
				gpo.setPath(p);


				if (selected) {
					canvas.drawPath(p, paintBlur);
					canvas.drawPath(p, paintSimple);

				}
				else if (beingDrawn) {
					canvas.drawPath(p, polyPaint);
					//myMap.showLenthOfPath(new PathMeasure(p,false));

				} else {
					String color = colorShiftOnStatus(go.getStatusVariable());
					if (color==null)
						color = go.getColor();
					canvas.drawPath(p, createPaint(color,Paint.Style.STROKE,0));
				}
			}

		}
		//Check if label should be drawn.
		int[]xy;
		if (!selected&&layerO!=null && layerO.showLabels() ) {
			xy = new int[2];
			translateMapToRealCoordinates(go.getLocation(),xy);
			Log.d("vortex","Calling drawlabel in drawgop for "+go.getLabel()+". I am a path? "+(go instanceof GisPathObject)+" Iam point? "+(go instanceof GisPointObject));
			drawGopLabel(canvas,xy,go.getLabel(),LabelOffset,bCursorPaint,vtnTxt);
		}
		//Check if directional line should be drawn.
		if (riktLinjeStart!=null)
			canvas.drawLine(riktLinjeStart[0], riktLinjeStart[1], riktLinjeEnd[0],riktLinjeEnd[1],fgPaintSel);//fgPaintSel

	}




	/**
	 * Draws a Label above the object location at the distance given by offSet
	 * @param canvas
	 * @param xy
	 * @param mLabel
	 * @param offSet
	 * @param bgPaint
	 * @param txtPaint
	 */
	private void drawGopLabel(Canvas canvas, int[] xy, String mLabel, float offSet, Paint bgPaint, Paint txtPaint) {
		Rect bounds = new Rect();
		txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
		int textH = bounds.height()/2;
		bounds.offset((int)xy[0]-bounds.width()/2,(int)xy[1]-(bounds.height()/2+(int)offSet));
		bounds.set(bounds.left-2,bounds.top-2,bounds.right+2,bounds.bottom+2);
		//txtPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawRect(bounds, bgPaint);
		canvas.drawText(mLabel, bounds.centerX(), bounds.centerY()+textH,txtPaint);

	}

	private void drawPoint(Canvas canvas, Bitmap bitmap, float radius, String color, Style style, PolyType type, int[] xy, float adjustedScale) {
		Rect r = new Rect();
		//pXR = this.getImageWidth()/photoMetaData.getWidth();
		//radius = Math.max(5, (int)(radius*pXR));
		//Log.d("vortex","PXR: Radius:"+pXR+","+radius);
		if (bitmap!=null) {
			//Log.d("vortex","bitmap! "+gop.getLabel());
			r.set(xy[0]-32, xy[1]-32, xy[0], xy[1]);
			canvas.drawBitmap(bitmap, null, r, null);
		} //circular?

		else if (type == PolyType.circle) {
			//Log.d("vortex","x,y,r"+xy[0]+","+xy[1]+","+radius);
			canvas.drawCircle(xy[0], xy[1],radius, createPaint(color,style));
		}
		//no...square.
		else if (type == PolyType.rect) {
			//Log.d("vortex","rect!");
			int diam = (int)(radius/2);
			r.set(xy[0]-diam, xy[1]-diam, xy[0]+diam, xy[1]+diam);
			canvas.drawRect(r, createPaint(color,style));
		}
		else if (type == PolyType.triangle) {
			drawTriangle(canvas,color,style,radius,xy[0], xy[1]);
		}
	}

	//0 = distance, 1=riktning.


	public void drawTriangle(Canvas canvas, String color, Style style,
							 float radius, int x, int y) {
		Paint paint = this.createPaint(color, style);
		Path path = new Path();
		path.setFillType(FillType.EVEN_ODD);

		path.moveTo(x,y-radius);
		path.lineTo(x+radius, y+radius);
		path.lineTo(x-radius, y+radius);
		path.close();

		canvas.drawPath(path, paint);
	}

	public void unSelectGop() {
		touchedLayer=null;
		touchedBag=null;
		touchedGop=null;
		myMap.setVisibleAvstRikt(false,null);
		//Remove directional line.
		riktLinjeStart=null;
		riktLinjeEnd=null;
		invalidate();
	}



	//save the position where the user pressed start.
	private int[] riktLinjeStart,riktLinjeEnd;

	private Integer currentDistance=null;
	final static int TimeOut = 3;

	private void displayDistanceAndDirection() {
		final int interval = TimeOut*1000;

		if (handler==null) {
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					displayDistanceAndDirectionL();
					//Log.d("vortex","displaydistcalled from disp timer");
					if (handler!=null)
						handler.postDelayed(this, interval);
				}
			};

			handler.postDelayed(runnable, 3000);

		}

	}

	private void displayDistanceAndDirectionL() {


		//Check preconditions for GPS to work

		if (myX==null||myY==null||GlobalState.getInstance()==null) {
			myMap.setAvstTxt("Config");
			myMap.setRiktTxt("fault!");
			handler=null;
			return;
		}
		if (touchedGop==null) {
			handler=null;
			Log.e("vortex","Touched GOP null in dispDistAndDir. Will exit");
			return;
		}

		if (!GlobalState.getInstance().getTracker().isGPSEnabled) {
			myMap.setAvstTxt("GPS OFF");
			myMap.setRiktTxt("");
			return;
			//myMap.setRiktTxt(spinAnim());
		}
		//Start a redrawtimer if not already started that redraws this window independent of the redraw cycle of the gops.

		//Check  timediff. Returns null in case no value exists.

		long timeDiff;
		long ct;
		if (mostRecentGPSValueTimeStamp!=-1) {
			ct = System.currentTimeMillis();
			timeDiff = (ct-mostRecentGPSValueTimeStamp)/1000;

		} else {

			myMap.setAvstTxt("GPS");
			myMap.setRiktTxt("searching");
			return;
		}

		boolean old = timeDiff>TimeOut;
		if (old) {
			//Log.d("vortex","Time of insert: "+mostRecentGPSValueTimeStamp);
			//Log.d("vortex","Current time: "+ct);
			//Log.d("vortex","TimeDiff: "+timeDiff);
			myMap.setAvstTxt("Lost signal");
			myMap.setRiktTxt(timeDiff+" s");

		}
		String mXs = myX.getValue();
		String mYs = myY.getValue();
		if (mXs!=null && mYs!=null) {
			double mX = Double.parseDouble(mXs);
			double mY = Double.parseDouble(mYs);
			double gX = touchedGop.getLocation().getX();
			double gY = touchedGop.getLocation().getY();
			currentDistance = (int)Geomatte.sweDist(mY,mX,gY,gX);
			int rikt = (int)(Geomatte.getRikt2(mY, mX, gY, gX)*57.2957795);
			myMap.setAvstTxt(currentDistance>9999?(currentDistance/1000+"km"):(currentDistance+"m"));
			myMap.setRiktTxt(rikt+Deg);


		}

	}




	private Map<String,Paint> paintCache = new HashMap<String,Paint>();

	public Paint createPaint(String color, Paint.Style style) {
		return createPaint(color,style,2);
	}

	public Paint createPaint(String color, Paint.Style style, int strokeWidth) {
		String key = style==null?color:color+style.name();
		Paint p = paintCache.get(key);
		if (p!=null) {
			//Log.d("vortex","returns cached paint for "+key);
			return p;
		}
		//If no cached object, create.
		p = new Paint();
		p.setColor(color!=null?Color.parseColor(color):Color.YELLOW);
		p.setStyle(style!=null?style:Paint.Style.FILL);
		p.setStrokeWidth(strokeWidth);
		paintCache.put(key, p);
		return p;
	}








	private boolean isStarted=false;


	private FullGisObjectConfiguration gisTypeToCreate;



	public void runSelectedWf() {
		//update image to close polygon.
		invalidate();
		if (touchedGop!=null)
			runSelectedWf(touchedGop);
		unSelectGop();
	}
	public void runSelectedWf(GisObject gop) {
		GlobalState.getInstance().setDBContext(new DB_Context(null,gop.getKeyHash()));

		Log.d("vortex","Setting current keyhash to "+gop.getKeyHash());
		String target = gop.getWorkflow();
		Workflow wf = GlobalState.getInstance().getWorkflow(target);
		if (wf ==null) {
			Log.e("vortex","missing click target workflow");
			new AlertDialog.Builder(ctx)
					.setTitle("Missing workflow")
					.setMessage("No workflow associated with the GIS object or workflow not found: ["+target+"]. Check your XML.")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setNeutralButton("Ok",new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

						}
					} )
					.show();
		} else {

			Variable statusVariable = gop.getStatusVariable();
			String statusVarS=null;
			if (statusVariable!=null) {
				statusVarS = statusVariable.getId();
				String valS = statusVariable.getValue();
				if (valS==null || valS.equals("0")) {
					Log.d("vortex","Setting status variable to 1");
					statusVariable.setValue("1");
				} else
					Log.d("vortex","NOT Setting status variable to 1...current val: "+statusVariable.getValue());
				myMap.registerEvent(new WF_Event_OnSave("Gis"));

			}

			Start.singleton.changePage(wf,statusVarS);

		}
	}





	final static float ScaleTo = 4.0f;
	public void centerOnUser() {
		if (userGop!=null) {
			int[] xy = new int[2];
			boolean inside = translateMapToRealCoordinates(userGop.getLocation(),xy);
			if (inside) {
				//float[] rxy = translateToReal((float)xy[0],(float)xy[1]);
				float scaleDiff = ScaleTo-scaleAdjust;
				//moveBy(rxy[0],rxy[1]);
				setPosition(fixedX-xy[0]*ScaleTo,fixedY-xy[1]*ScaleTo); //300 450
				redraw();
				Log.d("vortex","X Y USER "+xy[0]+","+xy[1]);
				//Log.d("vortex","RX RY USER "+rxy[0]+","+rxy[1]);
				Log.d("vortex","X Y SCALE: SCALEADJ:"+x+","+y+","+scale+","+scaleAdjust);
				//this.invalidate();
				//float newScale = 4.0f-scaleAdjust;
				startZoom(x, y,scaleDiff);
			}
		}/* else {
			new AlertDialog.Builder(ctx)
			.setTitle("Context problem")
			.setMessage("You are either outside map or have no valid GPS location.")
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(true)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
		}*/
	}

	enum CreateState {
		initial,
		inBetween,
		readyToGo
	}

	public void cancelGisObjectCreation() {
		if (gisTypeToCreate!=null) {
			gisTypeToCreate=null;
			if (newGisObj!=null) {
				currentCreateBag.remove(newGisObj);
				currentCreateBag =null;
				newGisObj=null;
			}
		}
		invalidate();
	}


	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		//unselect if selected
		this.unSelectGop();
		gisTypeToCreate=fop;
	}

	public void deleteSelectedGop() {
		if (touchedGop!=null) {
			GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(touchedGop.getKeyHash());
			touchedBag.remove(touchedGop);
			//Dont need to keep track of the bag anymore.
			invalidate();
		} else
			Log.e("vortex","Touchedgop null in deleteSelectedGop");
	}

	public GisObject getSelectedGop() {
		return touchedGop;
	}

	public void describeSelectedGop() {
		String hash = "*null*";
		if (touchedGop.getKeyHash()!=null)
			hash = touchedGop.getKeyHash().toString();
		new AlertDialog.Builder(ctx)
				.setTitle("GIS OBJECT DESCRIPTION")
				.setMessage("Type: "+touchedGop.getId()+"\nLabel: "+touchedGop.getLabel()+
						"\nSweref: "+touchedGop.getLocation().getX()+","+touchedGop.getLocation().getY()+
						"\nAttached workflow: "+touchedGop.getWorkflow()+
						"\nKeyHash: "+hash+
						"\nPolygon type: "+touchedGop.getGisPolyType().name())
				.setIcon(android.R.drawable.ic_menu_info_details)
				.setCancelable(true)
				.setNeutralButton("Ok",new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				} )
				.show();
	}

	long mostRecentGPSValueTimeStamp=-1;
	@Override
	public void gpsStateChanged(GPS_State newState) {
		//Log.d("vortex","Got GPS STATECHANGE");
		if (newState==GPS_State.newValueReceived||newState==GPS_State.ping) {
			mostRecentGPSValueTimeStamp = System.currentTimeMillis();
			if (currentDistance!=null && currentDistance<20)
				displayDistanceAndDirectionL();
		}
		this.postInvalidate();


	}



	public List<Location> getRectGeoCoordinates() {

		//Left top right bottom!
		List<Location> ret = new ArrayList<Location> ();

		Location topCorner = calculateMapLocationForClick(0,0);
		Location bottomCorner = calculateMapLocationForClick(this.displayWidth,this.displayHeight);

		ret.add(topCorner);
		ret.add(bottomCorner);

		return ret;

	}

	public Rect getCurrentViewSize(float fileImageWidth,float fileImageHeight) {
		//float Scales = scale * scaleAdjust;


		//int top = (int)(rX-this.getImageWidth()/2);

		final float Xs = (this.getScaledWidth()/2)-x;
		final float Ys = (this.getScaledHeight()/2)-y;
		final float Xe = Xs+this.displayWidth;
		final float Ye = Ys+this.displayHeight;


		final float scaleFx = fileImageWidth/this.getScaledWidth();
		final float scaleFy = fileImageHeight/this.getScaledHeight();


		final int left  = (int)(Xs * scaleFx);
		final int top  = (int)(Ys * scaleFy);

		final int right = (int)(Xe * scaleFx);
		final int bottom =(int)(Ye * scaleFy);

		Rect r = new Rect(left,top,right,bottom);

		Log.d("vortex","top bottom left right "+top+","+bottom+","+left+","+right);
		return r;
	}







}
