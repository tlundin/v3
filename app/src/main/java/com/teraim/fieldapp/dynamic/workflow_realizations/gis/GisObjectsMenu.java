package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.gis.GisImageView;

/** Class for GPS menu objects
 * 
 * @author Terje
 *
 */

public class GisObjectsMenu extends View {
	private static final int Padding = 15,InnerPadding = 20;
	private static final int NoOfButtonsPerRow = 5;
	private static final int MAX_ROWS = 5;
	private static final int SpaceBetweenHeaderAndButton = 5;
	Map<GisObjectType,Set<FullGisObjectConfiguration>> menuGroupsM;
	private Paint headerTextP;
	private Paint gopButtonBackgroundP;
	private Paint gopButtonEdgeP;
	private GisImageView myGis;
	private Paint thinBlackEdgeP;
	private Paint blackTextP;
	
	private MenuButton[][] menuButtonArray= new MenuButton[NoOfButtonsPerRow][MAX_ROWS];
	private String[] menuHeaderArray = new String[MAX_ROWS];
	private MenuButton oldB = null;
	private int buttonWidth; 
	private int RowH,ColW;
	private int w=0;
	private Paint gopButtonBackgroundSP;
	private Paint thinWhiteEdgeP;
	private Paint whiteTextP;
	private WF_Gis_Map myMap;

	public GisObjectsMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public GisObjectsMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public GisObjectsMenu(Context context) {
		super(context);
		init();
	}

	private void init() {
		
		menuGroupsM = new LinkedHashMap<GisObjectType,Set<FullGisObjectConfiguration>>(); 

		
		headerTextP = new Paint();
		headerTextP.setColor(Color.WHITE);
		headerTextP.setTextSize(20);
		headerTextP.setStyle(Paint.Style.STROKE);
		headerTextP.setTextAlign(Paint.Align.CENTER);

		blackTextP = new Paint();
		blackTextP.setColor(Color.BLACK);
		blackTextP.setTextSize(20);
		blackTextP.setStyle(Paint.Style.STROKE);
		blackTextP.setTextAlign(Paint.Align.CENTER);
		
		
		whiteTextP = new Paint();
		whiteTextP.setColor(Color.WHITE);
		whiteTextP.setTextSize(20);
		whiteTextP.setStyle(Paint.Style.STROKE);
		whiteTextP.setTextAlign(Paint.Align.CENTER);
		
		gopButtonBackgroundP = new Paint();
		gopButtonBackgroundP.setColor(Color.WHITE);
		gopButtonBackgroundP.setStyle(Paint.Style.FILL);

		gopButtonBackgroundSP = new Paint();
		gopButtonBackgroundSP.setColor(Color.BLACK);
		gopButtonBackgroundSP.setStyle(Paint.Style.FILL);
		
		gopButtonEdgeP = new Paint();
		gopButtonEdgeP.setColor(Color.BLACK);
		gopButtonEdgeP.setStyle(Paint.Style.STROKE);
		gopButtonEdgeP.setStrokeWidth(4);

		thinBlackEdgeP = new Paint();
		thinBlackEdgeP.setColor(Color.BLACK);
		thinBlackEdgeP.setStyle(Paint.Style.STROKE);
		thinBlackEdgeP.setStrokeWidth(1);
		
		thinWhiteEdgeP = new Paint();
		thinWhiteEdgeP.setColor(Color.WHITE);
		thinWhiteEdgeP.setStyle(Paint.Style.STROKE);
		thinWhiteEdgeP.setStrokeWidth(1);
		
		
		
		this.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					Log.e("vortex","Padding: "+Padding+" evX: "+event.getX()+" NOBu: "+NoOfButtonsPerRow+" ColW: "+ColW);
					int clickedColumn = Math.round((event.getX()-Padding)/ColW);
					if (clickedColumn<0||clickedColumn>=NoOfButtonsPerRow) {
						Log.d("vortex","click in column "+clickedColumn+". Outside allowed range");
						
					} else {
					Log.d("vortex","Clicked column: "+clickedColumn);
					//check for hit on all buttons in given column.
					for (int row = 0;row<MAX_ROWS;row++) {
						MenuButton currB = menuButtonArray[clickedColumn][row];
						if (currB!=null) {
							RectF r = currB.myRect;
							if (r.contains(event.getX(),event.getY())) {
								Log.e("vortex","CLICK HIT!!");
								currB.toggleSelected();								
								oldB=currB;
							}
							
						}
					}
					GisObjectsMenu.this.invalidate();
					}
				break;
				case MotionEvent.ACTION_UP:
					if (oldB!=null) {
						oldB.toggleSelected();						
						GisObjectsMenu.this.invalidate();
						if (myMap!=null)
							myMap.startGisObjectCreation(oldB.myMenuItem);
						oldB=null;
					}
					
					v.performClick();
					break;
				default:
					break;
				}
				return true;
			}
		});

	}

	@Override
	public boolean performClick() {
		// Calls the super implementation, which generates an AccessibilityEvent
		// and calls the onClick() listener on the view, if any
		super.performClick();

		Log.e("vortex","Gets here!");

		return true;
	}

	private class MenuButton {

		public RectF myRect;
		public FullGisObjectConfiguration myMenuItem;
		public boolean isSelected=false; 

		public MenuButton(FullGisObjectConfiguration menuItem, RectF rf) {
			myRect = rf;
			myMenuItem=menuItem;
		}
		
		public void toggleSelected() {
			isSelected=!isSelected;
		}


	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d("vortex","inSizeChange!");
		this.w=w;
		//First time called?
		if (oldw == 0)
			generateMenu();
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void setMenuItems(List<FullGisObjectConfiguration> myMenuItems, GisImageView gis, WF_Gis_Map map) {
		//Create Menu items.
		myGis=gis;
		myMap=map;
		for (FullGisObjectConfiguration item:myMenuItems) {
			Set<FullGisObjectConfiguration> itemSet = menuGroupsM.get(item.getGisPolyType());
			if (itemSet==null) {
				itemSet = new HashSet<FullGisObjectConfiguration>();
				menuGroupsM.put(item.getGisPolyType(), itemSet);
			}
			itemSet.add(item);
		}
	}
	
	private void generateMenu() {

		int col = 0;
		int row = 0;		
		Log.d("vortex","Width is "+w);
		buttonWidth = (w-Padding*2-(InnerPadding*(NoOfButtonsPerRow-1)))/NoOfButtonsPerRow; 
		RowH = InnerPadding+buttonWidth;
		ColW = RowH;

		//Padding needs to be greater than height of main header.
		int totalHeaderHeight = 0;
		int i=0;
		for (GisObjectType type:menuGroupsM.keySet()) {
			Set<FullGisObjectConfiguration> itemSet = menuGroupsM.get(type);
			Iterator<FullGisObjectConfiguration> it = itemSet.iterator();
			menuHeaderArray[i++]=type.name();
			while (it.hasNext()) {
				//Left padding + numer of buttons + number of spaces in between.
				FullGisObjectConfiguration fop = it.next();
				int left = col*ColW+Padding;
				int top = row*RowH+Padding+totalHeaderHeight+SpaceBetweenHeaderAndButton;
				RectF r = new RectF(left,top, left+buttonWidth,top+buttonWidth);
				menuButtonArray[col][row] = new MenuButton(fop,r);
				col++;
				if (col==NoOfButtonsPerRow) {
					col=0;
					row++;
					i++;
					totalHeaderHeight+=SpaceBetweenHeaderAndButton;
				}

			}
			totalHeaderHeight += headerTextP.getTextSize()+SpaceBetweenHeaderAndButton;
			col=0;
			row++;
		}


	}

	@Override
	protected void onDraw(Canvas canvas) {
		int totalHeaderHeight = 0;
		int w = canvas.getWidth();
		for (int row = 0 ; row < MAX_ROWS; row++) {
			if (menuHeaderArray[row]!=null)
				canvas.drawText(menuHeaderArray[row]+" types", w/2, row*RowH+Padding+totalHeaderHeight, headerTextP);
			MenuButton currB=null;
			for (int col = 0; col < NoOfButtonsPerRow;col++) {
				currB = menuButtonArray[col][row];
				if (currB == null) {
					//if first element is null in a row, we are done.
					if (col==0)
						return;
					else
						break;
				}
				RectF r = currB.myRect;
				FullGisObjectConfiguration fop = currB.myMenuItem;
				canvas.drawRoundRect(r,5f,5f,currB.isSelected?gopButtonBackgroundSP:gopButtonBackgroundP);
				canvas.drawRoundRect(r,5f,5f,gopButtonEdgeP);
				//Draw symbol or icon inside Rect.
				int iconPadding=10; int radius = 15;
				RectF rect = new RectF(r.left+iconPadding, r.top+iconPadding, r.right-iconPadding, r.bottom-iconPadding);

				if (fop.getIcon()==null) {
					if (fop.getShape()==PolyType.circle) {
						Log.d("vortex","circle!!");
						//draw circle at rect mid.
						canvas.drawCircle(r.left+r.width()/2, r.top+r.height()/2, r.width()/2-iconPadding*2,(fop.getStyle()==Style.FILL? (currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP):myGis.createPaint(fop.getColor(),fop.getStyle())) );
						//since background is white, add a black edge.
						//if (fop.getStyle()==Style.FILL)
						//	canvas.drawCircle(r.left+r.width()/2, r.top+r.height()/2, r.width()/2-iconPadding*2, currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP);
					} else if (fop.getShape()==PolyType.rect){
						Log.d("vortex","rect!!");
						canvas.drawRect(rect, myGis.createPaint(fop.getColor(),fop.getStyle()));
						//since background is white, add a black edge.
						if (fop.getStyle()==Style.FILL)
							canvas.drawRect(rect, currB.isSelected?thinWhiteEdgeP:thinBlackEdgeP);
					} else if (fop.getShape()==PolyType.triangle) {
						Log.d("vortex","triangle!!");
						myGis.drawTriangle(canvas, fop.getColor(),fop.getStyle(),
								r.width()/2-iconPadding*2,(int) (r.left+r.width()/2), (int)(r.top+r.height()/2));
					}
				} else {
					canvas.drawBitmap(fop.getIcon(),null , rect, null);
				}
				canvas.drawText(fop.getName(), r.left+r.width()/2, r.bottom+blackTextP.getTextSize(),currB.isSelected?blackTextP:whiteTextP);

			}
			totalHeaderHeight += headerTextP.getTextSize()+SpaceBetweenHeaderAndButton;
		}

		super.onDraw(canvas);
	}

}
