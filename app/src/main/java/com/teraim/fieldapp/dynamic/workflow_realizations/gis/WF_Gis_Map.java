package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.gis.GisImageView;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * 
 * @author Terje
 * Copyright Teraim 2015
 * Do not redistribute or change without prior agreement with copyright owners.
 * 
 * 
 * Implements A GIS Map widget. Based on  
 */
public class WF_Gis_Map extends WF_Widget implements Drawable, EventListener, AnimationListener {

	private boolean click=false;
	private PersistenceHelper globalPh,ph;
	private String gisDir;
	private GisImageView gisImageView;
	private LinearLayout filtersC,layersC;
	private final WF_Context myContext;
	private View avstRL,createMenuL;
	private TextView avstT,riktT;
	private TextSwitcher avstTS;
	private TextSwitcher riktTS;
	private Button unlockB,startB;
	private ImageButton objectMenuB,carNavB,zoomB,centerB,plusB,minusB;
	private Animation popupShow;
	private Animation popupHide;
	private GisObjectsMenu gisObjectMenu;
	private View gisObjectsPopUp;
	private boolean gisObjMenuOpen=false;
	protected boolean animationRunning=false;
	private List<FullGisObjectConfiguration> myGisObjectTypes;
	private Button createBackB;
	private Button createOkB;
	private TextView selectedT,selectedT2,circumT,lengthT,areaT;
	private final static String squareM = "\u33A1";
	private List<GisLayer> myLayers = new ArrayList<GisLayer>();

	private boolean isZoomLevel;


	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.gis_longpress_menu, menu);

			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			MenuItem x = menu.getItem(0);
			MenuItem y = menu.getItem(1);


			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {

			case R.id.menu_delete:
				gisImageView.deleteSelectedGop();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.menu_info:
				gisImageView.describeSelectedGop();
				return false;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			gisImageView.unSelectGop();
		}
	};

	ActionMode mActionMode;

	//final PhotoMeta photoMeta;
	final Context ctx;

	final CreateGisBlock myDaddy;
	private PhotoMeta photoMeta;



	public WF_Gis_Map(CreateGisBlock createGisBlock,Rect rect, String id, final FrameLayout mapView, boolean isVisible, Bitmap bmp,
			final WF_Context myContext, final PhotoMeta photoMeta, View avstRL, View createMenuL,  List<GisLayer> daddyLayers, final int realW, final int realH) {
		super(id, mapView, isVisible, myContext);
		
		GlobalState gs = GlobalState.getInstance();

		this.myContext=myContext;
		this.myDaddy=createGisBlock;
		//This is a zoom level if layers are imported.
		isZoomLevel = daddyLayers!=null;
		gisImageView = (GisImageView)mapView.findViewById(R.id.GisV);		
		gisImageView.setImageBitmap(bmp);
		
		
		this.photoMeta = photoMeta; 
		globalPh = gs.getGlobalPreferences();
		ctx = myContext.getContext();	
		ph = gs.getPreferences();
		//Bitmap bmp = Tools.getScaledImage(ctx,fullPicFileName);


		

		//Only allow zoom if this is *not* a zoom level. Only one level of zoom!


		this.avstRL = avstRL;

		this.createMenuL=createMenuL;

		avstTS = (TextSwitcher)avstRL.findViewById(R.id.avstTS);
		riktTS = (TextSwitcher)avstRL.findViewById(R.id.riktTS);

		LayoutInflater li = LayoutInflater.from(ctx);
		gisObjectsPopUp = li.inflate(R.layout.gis_object_menu_pop,null);

		gisObjectMenu = (GisObjectsMenu)gisObjectsPopUp.findViewById(R.id.gisObjectsMenu);
		gisObjectsPopUp.setVisibility(View.GONE);
		mapView.addView(gisObjectsPopUp);
		LinearLayout filtersL = (LinearLayout)mapView.findViewById(R.id.FiltersL);
		filtersL.setVisibility(View.GONE);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

		params.gravity=Gravity.RIGHT;
		params.topMargin=300;

		gisObjectsPopUp.setLayoutParams(params);
		Button cancelB = (Button)gisObjectsPopUp.findViewById(R.id.cancelB);

		cancelB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gisObjectsPopUp.startAnimation(popupHide);
			}
		});

		objectMenuB = (ImageButton)mapView.findViewById(R.id.objectMenuB);
		objectMenuB.setVisibility(View.GONE);
		objectMenuB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!animationRunning) {
					if (!gisObjMenuOpen && mActionMode==null) {
						gisImageView.cancelGisObjectCreation();
						gisObjectsPopUp.startAnimation(popupShow);
						mapView.invalidate();

						//gisImageView.centerOnUser();
					}

				}
			}
		});
		centerB = (ImageButton)mapView.findViewById(R.id.centerUserB);
		centerB.setVisibility(View.GONE);
		centerB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gisImageView.centerOnUser();

			}
		});

		carNavB = (ImageButton)mapView.findViewById(R.id.carNavB);

		if (!myContext.hasSatNav()) 
			carNavB.setVisibility(View.GONE);
		else {

			carNavB.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//Get Lat Long.
					GisObject gop = gisImageView.getSelectedGop();
					//sweref
					Location sweref = gop.getLocation();
					if (sweref!=null) {
						Location latlong = Geomatte.convertToLatLong(sweref.getX(),sweref.getY());
						if (latlong!=null) {
							Log.d("vortex","Nav to: "+sweref.getX()+","+sweref.getY()+" LAT: "+latlong.getX()+" LONG: "+latlong.getY());
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="+latlong.getX()+","+latlong.getY()));
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							ctx.startActivity(intent);
						} else {
							Toast.makeText(ctx, "Saknar koordinater", Toast.LENGTH_SHORT).show();;
						}
					}
				}
			});
		}

		selectedT = (TextView)avstRL.findViewById(R.id.selectedT);
		selectedT2 = (TextView)avstRL.findViewById(R.id.selectedT2);
		circumT = (TextView)avstRL.findViewById(R.id.circumT);
		lengthT = (TextView)createMenuL.findViewById(R.id.lengthT);
		areaT = (TextView)avstRL.findViewById(R.id.areaT);

		unlockB = (Button)avstRL.findViewById(R.id.unlockB);

		unlockB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gisImageView.unSelectGop();

			}
		});

		startB = (Button)avstRL.findViewById(R.id.startB);

		startB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gisImageView.runSelectedWf();

			}
		});

		zoomB = (ImageButton)mapView.findViewById(R.id.zoomB);
		setZoomButtonVisible(false);
		//zoomB.setVisibility(View.INVISIBLE);
		zoomB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {



				//get cutout
				Rect r = gisImageView.getCurrentViewSize(realW,realH);
				//get geocordinates
				List<Location> geoR = gisImageView.getRectGeoCoordinates(r);

				//Trigger reexecution of flow.
				Log.d("vortex","Cutout layers has "+myLayers.size()+" members");
				myDaddy.setCutOut(r,geoR,myLayers);
				//myContext.getTemplate().restart();
				Start.singleton.changePage(myContext.getWorkflow(), null);


			}

		});


		plusB = (ImageButton)mapView.findViewById(R.id.plusB);

		plusB.setOnTouchListener(new OnTouchListener() {
			final float Initial = 2f;
			float scaleIncrement = Initial;
			long interval=100;
			Handler handler;
			Runnable runnable;
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					//startScrollIn();
					gisImageView.handleScale(Initial);
					break;
				case MotionEvent.ACTION_UP:
					//stopScrollIn();
					break;
				}

				return v.performClick();
			}
		});

		minusB = (ImageButton)mapView.findViewById(R.id.minusB);

		minusB.setOnTouchListener(new OnTouchListener() {
			final float Initial = .5f;
			float scaleIncrement = Initial;
			long interval=100;
			Handler handler;
			Runnable runnable;
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (gisImageView.handleScaleOut(Initial)&&isZoomLevel) {
						//trigger pop on fragment.
						gisImageView.unSelectGop();
						myContext.getActivity().getFragmentManager().popBackStackImmediate();

					};
					//startScrollOut();
					break;
				case MotionEvent.ACTION_UP:
					//stopScrollOut();
					break;
				}

				return v.performClick();
			}
		});



		avstTS.setFactory(new ViewFactory() {

			public View makeView() {
				TextView myText = new TextView(ctx);
				myText.setGravity(Gravity.CENTER);

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
						Gravity.CENTER);
				myText.setLayoutParams(params);

				myText.setTextSize(36);
				myText.setTextColor(Color.WHITE);
				return myText;
			}});

		riktTS.setFactory(new ViewFactory() {

			public View makeView() {
				TextView myText = new TextView(ctx);
				myText.setGravity(Gravity.CENTER);

				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
						Gravity.CENTER);
				myText.setLayoutParams(params);

				myText.setTextSize(36);
				myText.setTextColor(Color.WHITE);
				return myText;
			}});

		createBackB = (Button)mapView.findViewById(R.id.createBackB);

		createBackB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				gisImageView.goBack();
			}
		});


		createOkB = (Button)mapView.findViewById(R.id.createOkB);

		createOkB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setVisibleCreate(false,"");
				//Will open the distance dialog, select the new object and close polygons.
				gisImageView.createOk();
			}
		});


		popupShow = AnimationUtils.loadAnimation(ctx, R.anim.popup_show);
		popupShow.setAnimationListener(this);
		popupHide = AnimationUtils.loadAnimation(ctx, R.anim.popup_hide);
		popupHide.setAnimationListener(this);

		myGisObjectTypes = new ArrayList<FullGisObjectConfiguration>();



		
		if (isZoomLevel) {
			Log.e("vortex","Zoom level!");
			myLayers = daddyLayers;
			clearLayerCaches();
		} else
			myLayers = new ArrayList<GisLayer>();
	}





	public void setZoomButtonVisible(boolean visible) {
		if (!visible)
			zoomB.setVisibility(View.GONE);
		else
			zoomB.setVisibility(View.VISIBLE);
	}


	public void startActionModeCb() {
		if (!gisObjMenuOpen && mActionMode==null) {
			// Start the CAB using the ActionMode.Callback defined above
			mActionMode = ((Activity)myContext.getContext()).startActionMode(mActionModeCallback);
		} else {
			Log.d("vortex","Actionmode already running or gisObjMenu open...");
		}
	}

	public void stopActionModeCb() {
		if (mActionMode == null) {
			Log.d("vortex","Actionmode not running");
			return;
		} else
			mActionMode.finish();
	}

	public GisImageView getGis() {
		return gisImageView;
	}

	int noA=10,noR=10;

	public void setAvstTxt(String text) {
		//String ct = ((TextView)avstTS.getCurrentView()).getText().toString();
		avstTS.setText(text);
		/*if (noA-->0)
			return;
		else {
			noA=10;
			avstTS.setText(text);
		}
		*/
	}
	public void setRiktTxt(String text) {
		riktTS.setText(text);
	}
		/*String ct = ((TextView)riktTS.getCurrentView()).getText().toString();
		if (ct.equals(text)&&noR-->0)
			return;
		else {
			noR=10;
			riktTS.setText(text);
		}
	}*/

	public void setSelectedObjectText(String txt) {
		selectedT.setText(txt);
	}
	public void setVisibleAvstRikt(boolean isVisible, GisObject touchedGop) {
		Log.d("vortex","Entering setVisibleAvstRikt..");
		if (isVisible) {
			avstRL.setVisibility(View.VISIBLE);
			areaT.setVisibility(View.GONE);
			circumT.setVisibility(View.GONE);
			setSelectedObjectText(touchedGop.getLabel());
			if (touchedGop.getGisPolyType()!=GisObjectType.Point) {
				circumT.setVisibility(View.VISIBLE);
				double omkrets = Geomatte.getCircumference(touchedGop.getCoordinates());
				circumT.setText(new DecimalFormat("##.##").format(omkrets)+"m");
				if (touchedGop.getGisPolyType()==GisObjectType.Polygon) {
					areaT.setVisibility(View.VISIBLE);
					double area = Geomatte.getArea(touchedGop.getCoordinates());
					areaT.setText(new DecimalFormat("##.##").format(area)+squareM);
				}
			}
		}
		else {

			avstRL.setVisibility(View.GONE);
		}
	}

	public void setVisibleCreate(boolean isVisible, String label) {

		if (isVisible)
			createMenuL.setVisibility(View.VISIBLE);
		else
			createMenuL.setVisibility(View.GONE);

		if (selectedT2==null) {
			selectedT2 = (TextView)createMenuL.findViewById(R.id.selectedT2);
		}
		if (selectedT2!=null)
			selectedT2.setText(label);
		else
			Log.e("vortex","Java sucks");

	}

	public void setVisibleCreateOk(boolean isVisible) {
		if (isVisible)
			createOkB.setVisibility(View.VISIBLE);
		else
			createOkB.setVisibility(View.GONE);

	}

	public void showCenterButton(boolean isVisible) {
		if (isVisible)
			centerB.setVisibility(View.VISIBLE);
		else
			centerB.setVisibility(View.GONE);
	}

	@Override
	public void onEvent(Event e) {

		Log.d("vortex","In GIS_Map Event Handler");
		if (e.getProvider().equals(Constants.SYNC_ID)) {
			Log.d("Vortex","new sync event. Refreshing map.");
			myContext.refreshGisObjects();
			Log.d("vortex","Issuing redraw of gisimageview!!");
			gisImageView.redraw();
			
		}
		else if (e.getType() == EventType.onFlowExecuted) {
			Log.d("vortex","flow executed! Initializing gis imageview!");
			//Must be done here since all layers first needs to be added.
			gisImageView.initialize(this,photoMeta,!isZoomLevel);
		}
	}

	//Relay event to myContext without exposing context to caller.
	public void registerEvent(Event event) {
		myContext.registerEvent(event);
	}

	@Override
	public void onAnimationStart(Animation animation) {
		if (animation.equals(popupShow)) {
			Log.d("vortex","gets here!");
			gisObjectsPopUp.setVisibility(View.VISIBLE);
			gisObjectMenu.setMenuItems(myGisObjectTypes,gisImageView,this);
		}
		animationRunning = true;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		animationRunning = false;
		if (animation.equals(popupShow))
			gisObjMenuOpen = true;
		else {
			gisObjectsPopUp.setVisibility(View.GONE);
			gisObjMenuOpen = false;
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}

	//Add a gisobject to the createMenu.
	public void addGisObjectType(FullGisObjectConfiguration gop) {
		myGisObjectTypes.add(gop);
		objectMenuB.setVisibility(View.VISIBLE);
	}

	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		gisObjectsPopUp.startAnimation(popupHide);
		boolean firstTime = (globalPh.get(PersistenceHelper.GIS_CREATE_FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));
		if (firstTime) {
			globalPh.put(PersistenceHelper.GIS_CREATE_FIRST_TIME_KEY, "notfirstanymore!");
			new AlertDialog.Builder(myContext.getContext())
			.setTitle("Creating GIS objects")
			.setMessage("You are creating your first GIS object!\nClick on the location on the map where you want to place it or its first point.")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setCancelable(false)
			.setNeutralButton(ctx.getString(R.string.ok),new Dialog.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
		}
		//Put Map and GisViewer into create mode.

		//swap in buttons for create mode. 
		gisImageView.startGisObjectCreation(fop);
	}





	public void showLength(double lengthOfPath) {
		if (lengthOfPath==0)
			lengthT.setText("");
		else
			lengthT.setText(ctx.getString(R.string.length_) + new DecimalFormat("##.##").format(lengthOfPath));
	}

	/*
	public void addLayers (List<GisLayer> gl) {
		Log.d("vortex","adding all layers at once.");
		myLayers = gl;
		for (GisLayer l:myLayers) {
			Map<String,Set<GisObject>> bag = l.getGisBags();
			for (String key:bag.keySet()) {
				Set<GisObject> s = bag.get(key);
				for (GisObject go:s)
					if (go instanceof GisPointObject) {
						((GisPointObject)go).setTranslatedLocation(null);
					}
			}
		}
		gisImageView.unSelectGop();
	}
	 */	
	public void addLayer(GisLayer layer) {
		if(layer!=null) {
			Log.d("vortex","Succesfully added layer "+layer.getLabel());
			myLayers.add(layer);


		}

	}
	/*

	 */
	public void initializeLayersMenu(List<GisLayer> layers) {
		LinearLayout layersL = (LinearLayout)getWidget().findViewById(R.id.LayersL);
		layersL.removeAllViews();
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		View layersHeader = li.inflate(R.layout.layers_header, null);
		layersL.addView(layersHeader);
		
		for (final GisLayer layer:layers) {

			if (layer.hasWidget()) {
				//Log.d("vortex","Layer "+layer.getLabel()+" has a widget");
				View layersRow = li.inflate(R.layout.layers_row, null);
				TextView filterNameT = (TextView)layersRow.findViewById(R.id.filterName);
				CheckBox lShow = (CheckBox)layersRow.findViewById(R.id.cbShow);
				CheckBox lLabels = (CheckBox)layersRow.findViewById(R.id.cbLabels);
				filterNameT.setText(layer.getLabel());
				lShow.setChecked(layer.isVisible());
				//Log.d("vortex","SETCCHECKED: "+layer.isVisible());
				lLabels.setChecked(layer.showLabels());
				

				lShow.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						layer.setVisible(((CheckBox)v).isChecked());
						gisImageView.invalidate();
						
					}});

				lLabels.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {						
						layer.setShowLabels(isChecked);
						gisImageView.invalidate();
					}
				});
				layersL.addView(layersRow);
				

			}
		}
		
	}


	public GisLayer getLayer(String identifier) {
		if (myLayers==null||myLayers.isEmpty()||identifier==null)
			return null;
		for (GisLayer gl:myLayers) {
			if (gl.getId().equals(identifier))
				return gl;
		}
		return null;
	}


	public List<GisLayer> getLayers() {
		return myLayers;
	}


	public void clearLayerCaches() {
		for (GisLayer gl:myLayers) 
			gl.clearCaches();
		Log.d("vortex","Is zoom? "+isZoomLevel);
	}



	public boolean isZoomLevel() {
		return isZoomLevel;
	}





	




	




















}
