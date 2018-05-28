package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
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
import android.view.ViewGroup;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher.ViewFactory;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.MapGisLayer;
import com.teraim.fieldapp.dynamic.types.NudgeListener;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.gis.GisImageView;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

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

	private final FrameLayout mapView;
	private final Rect rect;
	private PersistenceHelper globalPh;
	private GisImageView gisImageView;
	private final WF_Context myContext;
	private View avstRL,createMenuL;
	private View candidatesL;
	private LinearLayout candidatesButtonL;

	private TextSwitcher avstTS;
	private TextSwitcher riktTS;
	private Button unlockB,startB;
	private ImageButton objectMenuB,carNavB,zoomB,centerB,plusB,minusB;
	private Animation popupShow,layersPopupShow;
	private Animation popupHide,layersPopupHide;
	private GisObjectsMenu gisObjectMenu;
	private View gisObjectsPopUp,layersPopup;
	private boolean gisObjMenuOpen=false;
	protected boolean animationRunning=false;
	private Map<String,List<FullGisObjectConfiguration>> myGisObjectTypes;
	private Button createBackB;
	private Button createOkB;
	private TextView selectedT,selectedT2,circumT,lengthT,areaT;
	private final static String squareM = "\u33A1";
	private List<GisLayer> myLayers = new ArrayList<GisLayer>();

	private boolean isZoomLevel;

	private final int  realW,realH;
	private ToggleButton filterB,layerB,mapB;
	private final static String[] statusValues = new String[] {"0","1","2","3"};
	private final Map<String,Boolean> standardFilterM = new HashMap<String, Boolean>();

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
				case R.id.menu_continue:
					gisImageView.editSelectedGop();
					mActionMode.finish();
					return true;
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
	private NudgeView nudgeMenu;


	public WF_Gis_Map(CreateGisBlock createGisBlock,final Rect rect, String id, final FrameLayout mapView, boolean isVisible, Bitmap bmp,
					  final WF_Context myContext, final PhotoMeta photoMeta, View avstRL, List<GisLayer> daddyLayers, final int realWW, final int realHH) {
		super(id, mapView, isVisible, myContext);

		GlobalState gs = GlobalState.getInstance();

		this.myContext=myContext;
		this.myDaddy=createGisBlock;
		//This is a zoom level if layers are imported.
		isZoomLevel = daddyLayers!=null;
		gisImageView = (GisImageView)mapView.findViewById(R.id.GisV);
		gisImageView.setImageBitmap(bmp);
		Log.d("vortex", "Image width and height is :"+ bmp.getWidth()+","+bmp.getHeight());
		Log.d("vortex", "realWW and realHH is :"+ realWW+","+realHH);
		this.realW = realWW; //bmp.getWidth();
		this.realH = realHH; //bmp.getHeight();
		this.photoMeta = photoMeta;
		globalPh = gs.getGlobalPreferences();
		ctx = myContext.getContext();
		this.avstRL = avstRL;
		createMenuL=getWidget().findViewById(R.id.createMenuL);
		avstTS = (TextSwitcher)avstRL.findViewById(R.id.avstTS);
		riktTS = (TextSwitcher)avstRL.findViewById(R.id.riktTS);
		LayoutInflater li = LayoutInflater.from(ctx);
		gisObjectsPopUp = li.inflate(R.layout.gis_object_menu_pop,null);

		layersPopup = li.inflate(R.layout.layers_menu_pop,null);

		gisObjectMenu = (GisObjectsMenu)gisObjectsPopUp.findViewById(R.id.gisObjectsMenu);
		nudgeMenu = (NudgeView)createMenuL.findViewById(R.id.gisNudgeButtonMenu);
		nudgeMenu.setListener(new NudgeListener() {
			@Override
			public void onNudge(Direction d, int changeDistance) {
				if (d.equals(Direction.NONE))
					return;
				GisObject gop = gisImageView.getGopBeingCreated();
				if (gop != null) {
					gop.clearCache();
					List<Location> gopCoordinates = gop.getCoordinates();
					if (gopCoordinates != null && !gopCoordinates.isEmpty()) {
						Location last = gopCoordinates.get(gopCoordinates.size()-1);
						if (last instanceof SweLocation) {
							SweLocation sweloc = (SweLocation)last;
							switch (d) {
								case UP:
									sweloc.north+=changeDistance;
									break;
								case DOWN:
									sweloc.north-=changeDistance;
									break;
								case LEFT:
									sweloc.east-=changeDistance;
									break;
								case RIGHT:
									sweloc.east+=changeDistance;
									break;

							}
							Log.d("nudge","sweloc changed with "+changeDistance+" dir: "+d);
							if (gop instanceof GisPointObject) {
								int[] xy = new int[2];
								gisImageView.translateMapToRealCoordinates(sweloc,xy);
								((GisPointObject) gop).setTranslatedLocation(xy);
							}
							//gopCoordinates.remove(last);
							//gopCoordinates.add(new SweLocation(east,north));

							gisImageView.redraw();
						} else
							Log.d("vortex","not sweloc!! "+last.getClass().getCanonicalName());
					} else
						Log.d("vortex","coordinates null or empty!");
				} else
					Log.d("vortex","Selected GOP null!!");
			}

			@Override
			public void centerOnNudgeDot() {
				gisImageView.centerOnCurrentDot();
			}
		});
		gisObjectsPopUp.setVisibility(View.GONE);
		layersPopup.setVisibility(View.GONE);
		mapView.addView(gisObjectsPopUp);
		mapView.addView(layersPopup);
		//LinearLayout filtersL = (LinearLayout)mapView.findViewById(R.id.FiltersL);
		//filtersL.setVisibility(View.GONE);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

		params.gravity=Gravity.CENTER_HORIZONTAL|Gravity.TOP;
		params.topMargin=100;

		gisObjectsPopUp.setLayoutParams(params);
		layersPopup.setLayoutParams(params);


		final ImageButton menuB = (ImageButton)mapView.findViewById(R.id.menuB);
		menuB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!layersPopup.isShown()) {
					layersPopup.startAnimation(layersPopupShow);
					getGis().setClickable(false);
					mapView.invalidate();
				} else {
					layersPopup.startAnimation(layersPopupHide);
					getGis().setClickable(true);
				}

			}
		});




		layerB = (ToggleButton) layersPopup.findViewById(R.id.btn_Layers);

		filterB = (ToggleButton)layersPopup.findViewById(R.id.btn_filters);

		mapB = (ToggleButton) layersPopup.findViewById(R.id.btn_bckgrounds);

		final TextView headerT = (TextView) layersPopup.findViewById(R.id.filterName);
		headerT.setText("Layers");
		final OnCheckedChangeListener myToggleListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				if (!buttonView.isPressed()) {
					Log.d("zaza","discarding system press");
					return;
				}
				switch (buttonView.getId()) {

					case R.id.btn_Layers:
						if (isChecked) {
							headerT.setText("Layers");
							Log.d("vortex", "Layer checked!!");
							initializeLayersMenu();
							filterB.setOnCheckedChangeListener(null);
							mapB.setOnCheckedChangeListener(null);
							filterB.setChecked(false);
							mapB.setChecked(false);
						}
						else {
							layerB.setOnCheckedChangeListener(null);
							layerB.setChecked(true);
						}
						break;
					case R.id.btn_filters:
						if (isChecked) {
							headerT.setText("Filters");
							Log.d("vortex", "Filter checked!!");
							initializeFiltersMenu();
							layerB.setOnCheckedChangeListener(null);
							mapB.setOnCheckedChangeListener(null);
							layerB.setChecked(false);
							mapB.setChecked(false);

						}
						else {
							filterB.setOnCheckedChangeListener(null);
							filterB.setChecked(true);
						}

						break;
					case R.id.btn_bckgrounds:
						if (isChecked) {
							headerT.setText("Map backgrounds");
							Log.d("vortex", "MAP checked!!");
							initializeMapBgMenu();
							filterB.setOnCheckedChangeListener(null);
							layerB.setOnCheckedChangeListener(null);
							filterB.setChecked(false);
							layerB.setChecked(false);

						}
						else {
							mapB.setOnCheckedChangeListener(null);
							mapB.setChecked(true);
						}

						break;


				}
				layerB.setOnCheckedChangeListener(this);
				filterB.setOnCheckedChangeListener(this);
				mapB.setOnCheckedChangeListener(this);


			}
		};
		layerB.setOnCheckedChangeListener(myToggleListener);
		filterB.setOnCheckedChangeListener(myToggleListener);
		mapB.setOnCheckedChangeListener(myToggleListener);

		//layerB.setChecked(true);

		objectMenuB = (ImageButton)mapView.findViewById(R.id.objectMenuB);
		objectMenuB.setVisibility(View.GONE);
		objectMenuB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!animationRunning) {
					if (!gisObjMenuOpen && mActionMode==null) {
						gisImageView.cancelGisObjectCreation();
						gisObjectsPopUp.startAnimation(popupShow);
						getGis().setClickable(false);
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
							Toast.makeText(ctx, "Saknar koordinater", Toast.LENGTH_SHORT).show();
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

				resetMenus();
				//get cutout
				Rect r = gisImageView.getCurrentViewSize(realW,realH);
				r.left=r.left+rect.left;
				r.right=r.right+rect.left;
				r.top = r.top+rect.top;
				r.bottom=r.bottom+rect.top;

				//get geocordinates
				List<Location> geoR = gisImageView.getRectGeoCoordinates();

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

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch(event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						if (gisImageView.handleScaleOut(Initial)&&isZoomLevel) {
							//trigger pop on fragment.
							gisImageView.unSelectGop();
							myContext.getActivity().getFragmentManager().popBackStackImmediate();

						}
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
				myText.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
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
				myText.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);

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

		candidatesL = mapView.findViewById(R.id.candidatesMenuL);
		candidatesButtonL = (LinearLayout) mapView.findViewById(R.id.candidatesButtonL);

		popupShow = AnimationUtils.loadAnimation(ctx, R.anim.popup_show);
		popupShow.setAnimationListener(this);
		popupHide = AnimationUtils.loadAnimation(ctx, R.anim.popup_hide);
		popupHide.setAnimationListener(this);
		layersPopupShow = AnimationUtils.loadAnimation(ctx, R.anim.popup_show);
		layersPopupShow.setAnimationListener(this);
		layersPopupHide = AnimationUtils.loadAnimation(ctx, R.anim.popup_hide);
		layersPopupHide.setAnimationListener(this);




		myGisObjectTypes = new HashMap<String,List<FullGisObjectConfiguration>>();




		if (isZoomLevel) {
			Log.e("vortex","Zoom level!");
			myLayers = daddyLayers;
			clearLayerCaches();
		} else {
			myLayers = new ArrayList<GisLayer>();
			if (createGisBlock.isTeamVisible()) {
				String team = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
				if(team ==null || team.isEmpty()) {
					//o = GlobalState.getInstance().getLogger();
					//Log.d("vortex", "no team but team is set to show. alarm!");
					//o.addRow("");
					//o.addRedText("Team name missing. Cannot show team members.");
				} else {
					Log.d("bortex", "team is visible! Adding layer for team "+team);
					final GisLayer teamLayer = new GisLayer(this, "Team", "Team", true, true, true);
					myLayers.add(teamLayer);
				}
				//Add all team members to here.
			}

		}

		//this.realW = realW;
		//this.realH = realH;
		this.mapView = mapView;
		this.rect=rect;

		for(String statusValue:statusValues)
			standardFilterM.put(statusValue,true);
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
			String status = touchedGop.getStatus();
			boolean isInitial = status==null || status.equals(Constants.STATUS_INITIAL);
			startB.setText(isInitial?R.string.start:R.string.continue_);
			Log.d("borste","getstat: "+touchedGop.getStatus());
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
			GlobalState.getInstance().setSelectedGop(touchedGop);
		}
		else {

			avstRL.setVisibility(View.GONE);
		}
	}

	public void setVisibleCreate(boolean isVisible, String label) {

		if (isVisible) {
			createMenuL.setVisibility(View.VISIBLE);

		}
		else {
			createMenuL.setVisibility(View.GONE);

		}

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

		Log.d("vortex", "In GIS_Map Event Handler");
		if (e.getProvider().equals(Constants.SYNC_ID)) {
			if (!gisImageView.isInitialized()) {
				Log.d("vortex","discarding event. Imageview is not initialized");
				return;
			}
			Log.d("Vortex", "new sync event. Refreshing map.");
			myContext.refreshGisObjects();
			Log.d("vortex", "Issuing redraw of gisimageview!!");
			//refresh team layer.
			GisLayer teamLayer = this.getLayerFromId("Team");
			if (teamLayer != null) {
				Log.d("bortex", "Refreshing team layer!");
				//if team layer, refresh team positions.
				teamLayer.clear();
				Set<GisObject> teamMembers = gisImageView.findMyTeam();
				if (teamMembers == null || teamMembers.isEmpty())
					Log.e("bortex", "No team members found on this map after Sync Event");
				else {
					Log.d("bortex", "found " + teamMembers.size() + " team members");
					teamLayer.addObjectBag("Team", teamMembers, false, gisImageView);
				}

			}


			gisImageView.redraw();

		}
		else if (e.getType() == EventType.onFlowExecuted) {
			Log.d("vortex","flow executed! Initializing gis imageview!");
			//Must be done here since all layers first needs to be added.
			//!isZoomLevel
			gisImageView.initialize(this,photoMeta,true);
		}
	}

	@Override
	public String getName() {
		return "WF_GIS_MAP";
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
		} else if (animation.equals(layersPopupShow))
			layersPopup.setVisibility(View.VISIBLE);
		animationRunning = true;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		animationRunning = false;
		if (animation.equals(popupShow))
			gisObjMenuOpen = true;
		else if (animation.equals(popupHide)) {
			gisObjectsPopUp.setVisibility(View.GONE);
			gisObjMenuOpen = false;
			getGis().setClickable(true);
		}
		else if (animation.equals(layersPopupShow)) {
			Log.d("vortex","Oooh...it ended!!");
			FrameLayout layersF = (FrameLayout)layersPopup.findViewById(R.id.LayersL);

			//filterB.setOnCheckedChangeListener(null);
			//mapB.setOnCheckedChangeListener(null);
			if (layersF.getChildCount()==0) {
				layerB.setChecked(true);
				initializeLayersMenu();

			}
			/*
			if (layersF.getChildCount()==0)
			 {
				 Log.d("vortox","setting checked");
				 layerB.setChecked(true);
			} else {
				View rootView = layersF.getChildAt(0);
				for(int index=0; index<((ViewGroup)rootView).getChildCount(); ++index) {
					View layersRow = ((ViewGroup)rootView).getChildAt(index);
					Log.d("vortex","row found");
					CheckBox lShow = (CheckBox) layersRow.findViewById(R.id.cbShow);
					if (lShow!=null) {
						Log.d("vortex", "checkbox found");
						if (lShow.isChecked()) {
							Log.d("vortex", "isChecked ");
						}
					}

				}
			}
			*/
		}
		else if (animation.equals(layersPopupHide)) {
			layersPopup.setVisibility(View.GONE);
			getGis().setClickable(true);
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}

	//Add a gisobject to the createMenu.
	public void addGisObjectType(FullGisObjectConfiguration gop,String paletteName) {
		List typesInPalette = myGisObjectTypes.get(paletteName);
		if (typesInPalette==null) {
			typesInPalette=new ArrayList <FullGisObjectConfiguration>();
			myGisObjectTypes.put(paletteName,typesInPalette);
		}
		typesInPalette.add(gop);
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
							Toast.makeText(ctx,"Click on map to put down first coordinate",Toast.LENGTH_LONG).show();
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
		else {
			String l = ctx.getString(R.string.length_) +"\n"+ new DecimalFormat("##.##").format(lengthOfPath);
			Log.d("vortex","L streng is "+l);
			lengthT.setText(l);
		}

	}


	public void addLayer(GisLayer layer) {
		if(layer!=null) {
			Log.d("vortex","Succesfully added layer "+layer.getLabel());
			myLayers.add(layer);


		}

	}
	/*

	 */
	int currentlyChecked=-1;
	int previouslyChecked = -1;

	private void initializeMapBgMenu() {
		int bgId=0;
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		View bg = li.inflate(R.layout.map_background_radiogroup,null);
		final RadioGroup radioGroup = (RadioGroup)bg.findViewById(R.id.radioL);
		FrameLayout  layersL = (FrameLayout )layersPopup.findViewById(R.id.LayersL);
		layersL.removeAllViews();
		layersL.addView(bg);
		//(RadioGroup)layersPopup.findViewById(R.id.radioL);

		radioGroup.setOnCheckedChangeListener(null);
		radioGroup.clearCheck();
		RadioButton rb = (RadioButton)li.inflate(R.layout.map_layers_row, null);
		rb.setText("None");
		rb.setChecked(true);
		rb.setId(bgId++);
		radioGroup.addView(rb);

		for (final GisLayer layer:myLayers) {
			if (layer instanceof MapGisLayer) {
				rb = (RadioButton) li.inflate(R.layout.map_layers_row, null);
				rb.setText(layer.getLabel());
				rb.setId(bgId);
				radioGroup.addView(rb);
				if (layer.isVisible()) {
					Log.d("vortex", "found visible layer " + layer.getLabel() + "...setting rb to on");
					radioGroup.clearCheck();
					radioGroup.check(bgId);
					currentlyChecked = bgId;
				}
				bgId++;
			}
		}
		Log.d("vortex","children: "+radioGroup.getChildCount());

		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {


			public void onCheckedChanged2(RadioGroup group, int checkedId) {
				Log.d("vortex","inoncheckchanged");
				for (int i=0;i<radioGroup.getChildCount();i++) {
					RadioButton child = (RadioButton)radioGroup.getChildAt(i);
					Log.d("vortex","setting checked: "+child.getId());
					child.setChecked(checkedId==child.getId());
				}
			}
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				RadioButton radioB;
				String text;
				gisImageView.hideImage();
				MapGisLayer layer=null;
				//Current layer if visible -> not visible.
				if (currentlyChecked!=-1) {
					radioB = (RadioButton) radioGroup.findViewById(currentlyChecked);
					text = radioB.getText().toString();
					layer = (MapGisLayer) getLayerFromLabel(text);
					if (layer != null) {
						layer.setVisible(false);
						Log.d("Vortex", "hiding layer " + layer.getLabel());
					} else
						Log.e("vortex", "could not find layer " + text);
				}
				radioB = (RadioButton) radioGroup.findViewById(checkedId);
				if (radioB==null) {
					Log.d("vortex","cannot find radiobutton "+checkedId+" currently checked: "+currentlyChecked);
					return;
				}
				text = radioB.getText().toString();
				if (text.equals("None")) {
					setZoomButtonVisible(false);
					Log.d("vortex","found none tag");
				} else {
					layer = (MapGisLayer) getLayerFromLabel(text);
					if (layer!=null) {
						layer.setVisible(true);
						final String cacheFolder = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/";

						String cachedImgFilePath = cacheFolder + layer.getImageName();
						Log.d("vortex", "found layer on checked change: "+cachedImgFilePath+" for text "+text);
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;
						BitmapFactory.decodeFile(cachedImgFilePath, options);
						int imageHeight = options.outHeight;
						int imageWidth = options.outWidth;
						Log.d("vortex","image rect h w is "+imageHeight+","+imageWidth);
						//Rect r = gisImageView.getCurrentViewSize(realW,realH);
						//Rect r = new Rect(0, 0, imageWidth, imageHeight);
						Bitmap bmp = Tools.getScaledImageRegion(myContext.getContext(),cachedImgFilePath,rect);
						if (bmp!=null) {
							gisImageView.setImageBitmap(bmp);
						}
						currentlyChecked = checkedId;
					}
					else
						Log.d("vortex","oh bugger");



				}
				Log.d("vortex","Checked radiobutton is "+radioGroup.getCheckedRadioButtonId()+" checkedID: "+checkedId+" TEXT: "+((RadioButton) radioGroup.findViewById(checkedId)).getText());
				//group.check(checkedId);
				gisImageView.redraw();
			}
		});
	}

	private void initializeFiltersMenu() {
		FrameLayout layersF = (FrameLayout)layersPopup.findViewById(R.id.LayersL);
		layersF.removeAllViews();
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		LinearLayout layersL = (LinearLayout)li.inflate(R.layout.layers_body,null);
		layersF.addView(layersL);
		View layersRow=null;

		//String[] filterNames = new String[] {"Not Started","Started (Yellow)","Faulty (Red)","Status Done (Green)"};
		String[] filterNames = ctx.getResources().getStringArray(R.array.filter_array);

		int i=0;
		for (String filterName:filterNames) {
			layersRow = li.inflate(R.layout.filters_row, null);
			((TextView)layersRow.findViewById(R.id.filterName)).setText(filterName);
			CheckBox cb = ((CheckBox)layersRow.findViewById(R.id.cbShow));
			final String statusValue = statusValues[i++];
			cb.setChecked(isNotExcluded(statusValue));
			cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					standardFilterM.put(statusValue,isChecked);
					gisImageView.invalidate();
				}
			});
			layersL.addView(layersRow);
		}


	}

	private void resetMenus() {
		FrameLayout layersF = (FrameLayout)layersPopup.findViewById(R.id.LayersL);
		layersF.removeAllViews();

		layerB.setChecked(false);
		mapB.setChecked(false);
		filterB.setChecked(false);

	}
	private void initializeLayersMenu() {
		FrameLayout layersF = (FrameLayout)layersPopup.findViewById(R.id.LayersL);
		layersF.removeAllViews();
		LayoutInflater li = LayoutInflater.from(myContext.getContext());
		LinearLayout layersL = (LinearLayout)li.inflate(R.layout.layers_body,null);
		layersF.addView(layersL);
		View layersRow=null;
		//layersRow = li.inflate(R.layout.layers_row, null);

		//find the longest text. if shorter than max, all strings will have this length.
		final int MaxLabelLength = 18;
		int maxLength=-1,length=0;

		for (final GisLayer layer:myLayers) {
			if (layer instanceof MapGisLayer)
				continue;
			length = layer.getLabel().length();
			if (length> maxLength)
				maxLength=length;
		}
		if (maxLength>MaxLabelLength)
			maxLength=MaxLabelLength;
		Log.d("vortex","maxlength now "+maxLength);
		for (final GisLayer layer:myLayers) {
			if (layer instanceof MapGisLayer)
				continue;
			if (layer.hasWidget()) {
				Log.d("zaza","layer row created for "+layer.getLabel()+" show labels: "+layer.showLabels()+" is visible: "+layer.isVisible()+" Obj: "+layer.toString());
				layersRow = li.inflate(R.layout.layers_row, null);
				final CheckBox lShow = (CheckBox) layersRow.findViewById(R.id.cbShow);
				final CheckBox lLabels = (CheckBox) layersRow.findViewById(R.id.cbLabels);
				//Log.d("vortex","Layer "+layer.getLabel()+" has a widget");
				TextView filterNameT = (TextView) layersRow.findViewById(R.id.filterName);
				String fixedL = Tools.fixedLengthString(layer.getLabel(),maxLength);
				filterNameT.setText(fixedL);
				//Log.d("vortex","length for"+fixedL+" is "+fixedL.length());
				lShow.setChecked(layer.isVisible());
				lLabels.setChecked(layer.showLabels());


				lShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (lShow.isPressed()) {
							layer.setVisible(isChecked);
							Log.d("vortex","layer "+layer.getLabel()+" setvisible "+isChecked);
							gisImageView.invalidate();
						} else {
							Log.d("vortex", "discarded...by system");
							lShow.setChecked(!isChecked);
						}

					}
				});

				lLabels.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						Log.d("baba","checking "+isChecked+" for button "+buttonView.getId());
						if (lLabels.isPressed()) {
							Log.d("vortex","by user...");
							layer.setShowLabels(isChecked);
							gisImageView.invalidate();
						} else {
							Log.d("vortex", "discarded...by system");
							lLabels.setChecked(!isChecked);
						}
					}
				});




			} else
				layersRow = null;
			if (layersRow!=null)
				layersL.addView(layersRow);
		}

	}







	public GisLayer getLayerFromLabel(String label) {
		if (myLayers==null||myLayers.isEmpty()||label==null)
			return null;
		for (GisLayer gl:myLayers) {
			if (gl.getLabel().equals(label)) {
				Log.d("vortex","MATCH Label!!");
				return gl;
			}

		}
		Log.e("vortex","NO MATCH Label!!");
		return null;

	}

	public GisLayer getLayerFromId(String identifier) {
		if (myLayers==null||myLayers.isEmpty()||identifier==null)
			return null;
		for (GisLayer gl:myLayers) {
			//Log.d("vortex","ID for layer: "+gl.getId());
			if (gl.getId().equals(identifier)) {
				//	Log.d("vortex","MATCH GL!!");
				return gl;
			}
		}
		Log.d("vortex", "Did not find layer " + identifier + " from GisMap.");

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


	public boolean wasShowingPopup() {
		Log.d("vortex","popupshowing?");
		boolean ret=false;
		final View menuL = mapView.findViewById(R.id.mmenuL);
		int menuState = menuL.getVisibility();
		if (menuState == View.VISIBLE) {
			layersPopup.startAnimation(layersPopupHide);
			ret=true;

		}
		Log.d("vortex","layers? "+ret);
		menuState = gisObjectsPopUp.getVisibility();
		if (menuState == View.VISIBLE) {
			gisObjectsPopUp.startAnimation(popupHide);
			ret=true;
		}
		Log.d("vortex","gisObjCreate? "+ret);
		menuState = avstRL.getVisibility();
		if (menuState == View.VISIBLE) {
			getGis().unSelectGop();
			ret=true;
		}
		Log.d("vortex","avstRikt? "+ret);
		menuState = candidatesL.getVisibility();
		if (menuState == View.VISIBLE) {
			showCandidates(null);
			ret=true;
		}
		Log.d("vortex","candidates? "+ret);
		getGis().setClickable(true);
		return ret;
	}


	public void showCandidates(List<GisObject> candidates) {
		if (candidates!=null) {
			wasShowingPopup();
			candidatesButtonL.removeAllViews();
			candidatesL.setVisibility(View.VISIBLE);
			LayoutInflater li = LayoutInflater.from(ctx);
			Button button;
			for (final GisObject go : candidates) {
				button = (Button) li.inflate(R.layout.gis_candidate_button, null);
				button.setText(go.getLabel() );
				candidatesButtonL.addView(button);
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showCandidates(null);
						gisImageView.selectGop(go);
						//Redraw will show avstMenu.
						gisImageView.invalidate();
					}
				});
			}
		} else {
			candidatesL.setVisibility(View.GONE);
			gisImageView.selectGop(null);
		}

	}


	public boolean isNotExcluded(String status) {
		return standardFilterM.get(status);
	}
}
