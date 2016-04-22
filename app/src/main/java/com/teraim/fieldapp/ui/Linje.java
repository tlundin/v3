package com.teraim.fieldapp.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.teraim.fieldapp.utils.Tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.View;

public class Linje extends View {

	private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p1 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p2 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pTag = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint iTag = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint tagText = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private static float h=700,w=300,r;
	private final LineMarkerFactory lmFactory;
	private MovingMarker user = new MovingMarker(w,h);
	private static float TAG_W = 20, TAG_H = 10,Max_Dev_X = 50;
	private static float LineLengthInMeters=200,areaWidthInMeters=2*Max_Dev_X;
	private float lineX=0,lineStart=0,lineEnd=0;
	private String mPole;
	
	private Map <String,Map<String,LineMarker>> markers = new HashMap<String,Map<String,LineMarker>>();
	public Linje(Context context, String pole) {
		super(context);
		mPole = pole;
		lmFactory = new LineMarkerFactory();
		
		p.setColor(Color.BLACK);
		p.setStyle(Style.STROKE);
		p.setStrokeWidth(2);

		p1.setColor(Color.BLACK);
		p1.setStyle(Style.STROKE);
		p1.setStrokeWidth(3);

		p2.setColor(Color.BLACK);
		p2.setStyle(Style.STROKE);
		p2.setTextSize(15);
		p2.setTextAlign(Align.CENTER);
		
		pp.setColor(Color.BLUE);
		pp.setStyle(Style.STROKE);
		pp.setTextSize(15);
		pp.setStrokeWidth(1);
		pp.setTextAlign(Align.CENTER);

		
		pTag.setStyle(Style.FILL);
		pTag.setStrokeWidth(1);
		
		iTag.setStrokeWidth(1);
		iTag.setStyle(Style.STROKE);
//	    iTag.setPathEffect(new DashPathEffect(new float[]{5, 10, 15, 20}, 0));
		iTag.setColor(Color.BLACK);
		
		tagText.setColor(Color.BLACK);
		tagText.setStyle(Style.STROKE);
		tagText.setTextSkewX(-0.25f);
		tagText.setTextSize(12);
		tagText.setTextAlign(Align.LEFT);
		
		r=w/9;
	}

	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);	
		h = this.getHeight();
		float lineLength = (h-2*(r+w/4));
		lineEnd = r+w/4;
		lineStart = h-lineEnd;
		float pixelsPerMeter_Y = lineLength/LineLengthInMeters;
		float pixelsPerMeter_X = w/areaWidthInMeters;
		lineX = w/2;
		canvas.drawText(mPole, lineX, w/4-r-10, pp);
		canvas.drawCircle(lineX, w/4,r, p);
		canvas.drawText("SLUT",lineX, w/4, p2);
		canvas.drawCircle(lineX, h-w/4,r, p);
		canvas.drawText("START",lineX, h-w/4, p2);
		canvas.drawLine(lineX, lineEnd, lineX, lineStart, p1);
		
		if (!markers.isEmpty()) {
			Set<Entry<String, Map<String, LineMarker>>> markerE = markers.entrySet();
			for (Entry<String, Map<String, LineMarker>>e:markerE) {
				Map<String, LineMarker> f = e.getValue();
				Set<Entry<String, LineMarker>> g = f.entrySet();
				int shiftX = 0;
				for (Entry<String, LineMarker>h:g) {
					String tag = h.getKey();
					LineMarker lm = h.getValue();
					if (lm.isInterval()) {

						float startY = lineStart-lm.getStart()*pixelsPerMeter_Y;
						float stopY = lineStart-lm.getEnd()*pixelsPerMeter_Y;
						canvas.drawLine(lineX, startY, lineX+TAG_W, startY, iTag);
						canvas.drawLine(lineX+TAG_W/2, startY, lineX+TAG_W/2, stopY, iTag);
						canvas.drawLine(lineX, stopY, lineX+TAG_W, stopY, iTag);
						canvas.drawText(lm.tag, lineX+TAG_W+2, startY-(startY-stopY)/2, tagText);
					} else {
						pTag.setColor(lm.myColor);			
						float left = shiftX+lineX-TAG_W/2;
						float right = shiftX+lineX+TAG_W/2;
						float bottom = lineStart - lm.getStart()*pixelsPerMeter_Y+TAG_H/2;
						float top = bottom-TAG_H;
						canvas.drawRect(left, top,right,bottom,pTag);
						canvas.drawText(tag, right, bottom, tagText);
					}
				}
				
			}
		}
		
		if (user.isInsideScreen()) {
			float x = user.getPosX();
			float y = user.getPosY();
			pTag.setColor(Color.BLACK);
			
			if ( x<Max_Dev_X && 
				 x>-Max_Dev_X &&
				 y>0 && 
				 y<=LineLengthInMeters )
				
				pTag.setColor(Color.GREEN);
			else
				pTag.setColor(Color.RED);
			canvas.drawCircle(lineX+x*pixelsPerMeter_X, lineStart-y*pixelsPerMeter_Y, 10, pTag);
		}
	}
	
	
	
	
	public void addMarker(String start,String tag) {
		addMarker(start,null,tag);
	}
	
	public void removeMarker(String tag, String meters) {
		Log.e("vortex","Removing marker "+tag+" at "+meters+" meters");
		Map<String,LineMarker> lm = markers.get(meters);
		LineMarker a;
		if (lm!=null)
			if ((a=lm.get(tag))!=null)
				lm.remove(a);
	}
	
	public void addMarker(String start, String end, String tag) {
		Log.d("nils","Adding marker");
		if (!Tools.isNumeric(start)||(end!=null && !Tools.isNumeric(end)))
			return;
		Map<String,LineMarker> lm = markers.get(start);
		if (lm==null) {
			lm = new HashMap<String,LineMarker>();
			markers.put(start, lm);
		}
		lm.put(tag, lmFactory.create(start,end,tag));	
		this.invalidate();
	}
	
	
	private class LineMarker {
		private String start=null,end=null;
		public String tag;
		public int myColor;
		
		public LineMarker(String start,String end,String tag,int color) {
			this.start=start;
			this.end=end;
			this.tag=tag;
			this.myColor=color;
		}
		public boolean isInterval() {
			return end!=null;
		}
		
		public float getStart() {
			return Float.parseFloat(start);
		}
		public float getEnd() {
			return Float.parseFloat(end);
		}
	}
	private class LineMarkerFactory {
		
		
		
		Integer[] colors = {Color.parseColor("#CC3232"),Color.BLUE,Color.GREEN,Color.RED,Color.YELLOW,Color.DKGRAY,Color.BLACK};
		int currentColor = 0;
		Map <String, Integer> assigned = new HashMap<String,Integer>();
		
		public LineMarker create(String start, String end,String tag) {
			Integer c = assigned.get(tag);
			if (c==null) {
				c = colors[currentColor];
				assigned.put(tag, c);
				currentColor = (currentColor + 1)%colors.length;			
			} 
			
			return new LineMarker(start,end,tag,c);
		}
	}
	
	
	
	public class MovingMarker {
		
		float x=-1,y=-1;
		float w,h;
		
		public MovingMarker (float w, float h) {
			this.w=w;
			this.h=h;
		}
		
		//x and y are relative to the Line that is in the center of the view. 
		public void setPos(float x,float y) {
			this.x=x;
			this.y=y;
		}
		
		private boolean isInsideScreen() {
			return x>=-(w/2)&&x<=(w/2)&&y>=0&&y<=h;
			
		}
		
		private float getPosX() {
			return x;
		}
		
		private float getPosY() {
			return y;
		}
		
	}

	public void setUserPos(float distX, float distY) {
		user.setPos(distX, distY);
	}


	public void removeAllMarkers() {
		markers.clear();
	}


	public void init(double eastStart, double northStart, boolean isHorizontal, boolean isUpOrLeft) {
		
	}
	
}
