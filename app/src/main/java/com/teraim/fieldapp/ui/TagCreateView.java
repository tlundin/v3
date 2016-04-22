package com.teraim.fieldapp.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.teraim.fieldapp.dynamic.types.Delyta;
import com.teraim.fieldapp.dynamic.types.Point;
import com.teraim.fieldapp.dynamic.types.Segment;
import com.teraim.fieldapp.non_generics.DelyteManager.Coord;

/**
 * @author Terje
 * 
 * This class is used to draw a Provyta with all its parts (delytor)
 */
public class TagCreateView extends View {





	protected static final float DRAW_THRESHOLD = 55;

	protected static final float ON_R_DIFF = 2;

	protected float oScaleF;

	private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

	private Paint px = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p50 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint p100 = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint dottedLinePaint = new Paint();

	private Paint pSma = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
	private Paint pySelected = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);	

	private List<Delyta> delytor;

	private Paint       wCursorPaint,bCursorPaint,rCursorPaint,currCursorPaint;

	private boolean abo=false;
	private Handler handler;
	private int cursorX=200,cursorY=245;
	private float r,realRadiusinMeter=100;

	private boolean onCircle=true;
	private boolean drawActive=false;
	private boolean moveCursor=false;

	private TagListenerI tagListener;


	public TagCreateView(Context context, AttributeSet attrs,TagListenerI tagListener) {
		super(context,attrs);


		resetTag();

		this.tagListener = tagListener;

		pf.setColor(Color.BLACK);
		pf.setStyle(Style.STROKE);
		pf.setStrokeWidth(1);

		pySelected.setColor(Color.RED);
		pySelected.setStyle(Style.STROKE);
		pySelected.setStrokeWidth(3);

		dottedLinePaint.setARGB(255, 0, 0,0);
		dottedLinePaint.setStyle(Style.STROKE);
		dottedLinePaint.setStrokeWidth(4);
		dottedLinePaint.setPathEffect(new DashPathEffect(new float[] {10,10}, 5));
		dottedLinePaint.setAlpha(120);

		p.setColor(Color.BLACK);
		p.setStyle(Style.STROKE);
		p.setStrokeWidth(2);

		px.setColor(Color.DKGRAY);
		px.setTypeface(Typeface.SANS_SERIF);


		pl.setColor(Color.BLACK);
		pl.setStyle(Style.STROKE);
		pl.setTypeface(Typeface.DEFAULT_BOLD); 
		pl.setTextSize(25);

		p50.setColor(Color.BLUE);
		p50.setStrokeWidth(2);
		p50.setStyle(Style.STROKE);

		p50.setTypeface(Typeface.SANS_SERIF); 


		p100.setColor(Color.RED);
		p100.setStrokeWidth(3);
		p100.setStyle(Style.STROKE);
		p100.setTypeface(Typeface.SANS_SERIF); 

		pSma.setColor(Color.GREEN);
		pSma.setStyle(Style.FILL);



		wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Paint.Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Paint.Style.FILL);
		rCursorPaint = new Paint();
		rCursorPaint.setColor(Color.RED);
		rCursorPaint.setStyle(Paint.Style.FILL);

		currCursorPaint=wCursorPaint;
		final int interval = 1000; // 1 Second
		handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				currCursorPaint = currCursorPaint.equals(wCursorPaint)?bCursorPaint:wCursorPaint;
				invalidate();
				handler.postDelayed(this, interval);

			}
		};

		this.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				final float x = event.getX();
				final float y = event.getY();
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (event.getAction() == MotionEvent.ACTION_DOWN){
						Log.d("vortex","Touch coordinates : " +
								String.valueOf(event.getX()) + "x" + String.valueOf(event.getY()));
						float dx = x-cursorX;
						float dy = y-cursorY;
						float distance = (float)Math.sqrt((dx)*(dx) + (dy)*(dy));

						if (distance<=40)
							moveCursor = true;
					}
					return true;
				case MotionEvent.ACTION_UP:
					if (moveCursor) {
						moveCursor = false;
						if (drawActive) {

							float d = distanceFromCenter(cursorX,cursorY);
							if (Math.abs(d-r)<ON_R_DIFF) {
								float[] currentP = closestPointOnRadius(cursorX,cursorY,d);
								cursorX=(int)currentP[0];
								cursorY=(int)currentP[1];
								addTagPoint(new float[]{cursorX,cursorY},r);
								onCircle = true;
								Log.d("vortex","on circle now true");
							} else
								addTagPoint(new float[]{cursorX,cursorY},d);

						}

					}
					v.performClick();
					break;
				case MotionEvent.ACTION_MOVE:
					if (moveCursor) { 
						currCursorPaint = wCursorPaint;
						float[] xy = insideCircleXY(x,y,onCircle);
						if (onCircle) {
							if (distanceFromCenter(x,y)<=(r-DRAW_THRESHOLD)) {
								//float[] currentP = insideCircleXY(x,y,true);						
								//if first, set a point.
								if (tag.size()==0) 
									addTagPoint(new float[]{cursorX,cursorY},r);
								Log.d("onCircle","onCircle is now false");	
								onCircle = false;
							} else {
								//Check that the cursor is not moving over an existing point.
								//End if at beginning.
								float nx = (xy[0]-cx)*(oScaleF);
								float ny= (xy[1]-cy)*(oScaleF);
								Coord c = new Coord(nx,ny,(r*oScaleF));
								if (tagPunkter!=null && tag.size()>0) {
									//float distance = distance(c,tagPunkter.getStart());
									//if(distance != Undefined && distance <5) {
									Coord startC = tagPunkter.getStart();
									if (startC!=null) {
										if (Math.abs(startC.rikt-c.rikt)<3) {
											endTag();
											resetTag();
										}  
										//Log.d("vortex","distance "+distance);
									}
									if(!toRightOf(c,tagPunkter.getLastCoord())) {
										currCursorPaint = rCursorPaint;	
										xy[0]=cursorX;
										xy[1]=cursorY;	
									} 
										
								}

							}
						} 

						cursorX=(int)xy[0];
						cursorY=(int)xy[1];	

						invalidate();
					}
					break;
				default:
					break;
				}
				return true;
			}


			private boolean toRightOf(Coord c, Coord pX) {
				//check range -5 ... 0;
				final int range = 100;

				if (tagPunkter!=null) {

					if (pX!=null && c!=null) {
						boolean b=false;
						if (pX.rikt<range) {
							if (c.rikt>(360-pX.rikt) && c.rikt<= pX.rikt) {
								Log.d("vortex","fail1");
								b=true;
							}
						} else 
							if (c.rikt>(pX.rikt-range) && c.rikt<= pX.rikt) {
								b=true;
								Log.d("vortex","fail2");
							}
						if (b) {
							Log.d("vortex","Stop on: "+c.rikt+" In forbidden area "+pX.rikt);
							return false;
						}
						//else
						//	Log.d("vortex","Cont on: "+c.rikt+" ok "+pX.rikt);
					}
				}
				return true;
			}

			private float[] stopAtCrossing(float[] xy) {
				float x = Math.round(xy[0]);
				float y = Math.round(xy[1]);
				boolean match = false;
				if (tagPunkter!=null) {
					float[] raw = tagPunkter.getRaw();
					for (int i=0;i<tagPunkter.size();i+=2) {
						if (x==raw[i] && y==raw[i+1]) {
							Log.d("vortex","MATCH!!!!");
							match=true;
						}
					}

					if (!match){
						Log.d("vortex","!for ["+x+","+y+"]");
						for (int i=0;i<tagPunkter.size();i+=2)
							Log.d("vortex",i+":"+raw[i]+","+raw[i+1]);

					}
				}
				return xy;
			}


		});

		handler.postDelayed(runnable, interval);
		drawActive=false;
	}


	private float[] insideCircleXY(float x, float y, boolean onCircle) {
		float distance = distanceFromCenter(x,y);
		if (distance<=r &&!onCircle)
			return new float[]{x,y};
		//not on circle. Return closest point on diameter.
		else {
			return closestPointOnRadius(x,y,distance);

		}
	}


	private float[] closestPointOnRadius(float x, float y,float distance) {
		float w = getWidth();
		float h = getHeight();
		float cx = w/2;
		float cy = h/2+20;
		float dx = x-cx;
		float dy = y-cy;
		float aX = cx + dx / distance * r;
		float aY = cy + dy / distance * r;
		return new float[]{aX,aY};
	}


	private float distanceFromCenter(float x,float y) {
		float w = getWidth();
		float h = getHeight();
		float cx = w/2;
		//float r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
		float cy = h/2+20;
		float dx = x-cx;
		float dy = y-cy;
		float distance = (float)Math.sqrt((dx)*(dx) + (dy)*(dy));
		return distance;
	}

	private static float Undefined = -1;

	public static float distance(Coord c1, Coord c2) {
		if (c1!=null && c2!=null) {
			float dx = c1.x-c2.x;
			float dy = c1.y-c2.y;
			return (float)Math.sqrt((dx)*(dx) + (dy)*(dy));
		}
		return Undefined;

	}

	float cy;
	float cx;
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);			
		float w = getWidth();
		float h = getHeight();



		boolean largeSize = (h>500);
		float margY = largeSize?20:0;
		if (largeSize) {
			r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
			px.setTextSize(25);		
		}
		else {
			r=h/2;
			px.setTextSize(13);
		}

		oScaleF = r/realRadiusinMeter;

		cx = w/2;
		cy = (h/2+margY);//h/2;

		//A dot in the middle!
		canvas.drawPoint(cx, cy, p);

		float sr = 2.8f*oScaleF;
		Coord c;
		//6 meter ut på 0,120,240
		int sDists[] = new int[]{30,50,70};
		for (int i = 0; i<(abo?3:1);i++) {
			int sDist = sDists[i];
			c = calc_xy((int)(sDist*oScaleF),0);		
			canvas.drawCircle(cx,cy+c.y,sr,pSma);
			canvas.drawCircle(cx,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist*oScaleF),120);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
			c = calc_xy((int)(sDist*oScaleF),240);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pSma);
			canvas.drawCircle(cx+c.x,cy+c.y,sr,pf);
		}
		drawDelytor(canvas,cx,cy,(float)oScaleF);
		canvas.drawText("100",(int)(cx+r)-15, cy, pf);
		if (largeSize)
			canvas.drawText("N",cx,(float)(h*.1), pl);

		drawTag(canvas,cx,cy,(float)oScaleF);

		if (drawActive) {
			//Log.d("vortex","oScaleF: "+oScaleF);
			//Draw line or arc to cursor.
			if (moveCursor) {
				if (onCircle) {
					float x = (cursorX-cx)*oScaleF;
					float y= (cursorY-cy)*oScaleF;
					Coord stC = tag.get(tag.size()-1).end;
					Coord enC = new Coord(x,y,(int)(r*oScaleF));
					canvas.drawArc(oval, stC.rikt, Delyta.rDist(stC.rikt,enC.rikt), false, dottedLinePaint);
				}
				else {
					float[] xy=tagPunkter.getLastXY();
					canvas.drawLine(xy[0],xy[1], cursorX, cursorY, dottedLinePaint);
				}
			}

		} else 
			initCursor(cx,1000);

		canvas.drawCircle(cursorX,cursorY, 20, currCursorPaint);	

	}


	//private List<Coord> scords=null;

	boolean cursorInitialized = false;

	private RectF oval;
	public void initCursor(float x,float y) {
		if (!cursorInitialized) {
			float[] xy = insideCircleXY(x,y,true);
			cursorX=(int)xy[0];
			cursorY=(int)xy[1];
			cursorInitialized=true;
			oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);
		}
	}
	//before delytor is calculated.
	private void drawTag(Canvas c, float cx, float cy, float oScaleF) {
		for (List<Segment>tag:tagtag) {
			for (Segment s:tag) {
				drawSegment(s,c, cx, cy, oScaleF);			
			}
		}
	}

	private void drawDelytor(Canvas c, float cx, float cy, float oScaleF) {
		if (delytor !=null&&!delytor.isEmpty()) {
			for (Delyta d:delytor) {
				Log.d("vortex","Found "+delytor.size()+" delytor!");
				for (Segment s:d.getSegments()) {
					Log.d("vortex","Has segment!");
					drawSegment(s,c, cx, cy, oScaleF);

				}
				Point numPos = d.getNumberPos();
				if (numPos!=null) {
					float nx = cx+(numPos.x*oScaleF);
					float ny = cy+(numPos.y*oScaleF);
					Log.d("nils","Drawing number at: "+nx+","+ny);
					p.setColor(Color.BLACK);
					c.drawText(d.getId()+"", nx, ny, px);
				}
			}
		} else {
			c.drawCircle(cx, cy, r, p);
		}

	}

	private void drawSegment(Segment s, Canvas c, float cx, float cy, float oScaleF) {
		float startX,startY,endX,endY;
		if (s.isArc) {
			//					if (!d.isSelected())
			//						continue;	
			//					else {

			//Log.d("nils","start:end"+s.start.rikt+":"+s.end.rikt);
			float start = s.start.rikt;
			float end = Delyta.rDist((int)start,s.end.rikt);

			//start = start-90;

			//Log.d("nils","Drawing arc from "+start+" with sweep "+end);
			//p.setColor(Color.BLUE);

			//double phi = 0.0174532925*(s.start.rikt);
			//float nx = (float)(s.start.avst * Math.cos(phi));
			//float ny = (float)(s.start.avst * Math.sin(phi));	
			//c.drawText("X",cx+ nx*oScaleF,cy+ ny*oScaleF, p100);


			c.drawArc(oval, start, end, false, p50);


			//c.drawArc(oval, start, end2, false, p100);
			//						c.drawArc(oval, start, end, false, pySelected);

			//					}
		} else {
			//float mirror = margY+r*2;							
			startX = cx+(s.start.x*oScaleF);
			startY = cy+(s.start.y*oScaleF);
			endX = cx+(s.end.x*oScaleF);
			endY = cy+(s.end.y*oScaleF);				
			//Log.d("nils","Drawing Start: "+startX+","+startY+" End: "+endX+","+endY);
			//p.setColor(Color.BLUE);
			//					c.drawLine(startX,startY,endX,endY, d.isSelected()?pySelected:p);	
			c.drawLine(startX,startY,endX,endY, p50);
		}

	}

	public void showDelytor(List<Delyta> delytor) {

		this.delytor = delytor;
		this.invalidate();
	}




	private Coord calc_xy(int avst, int rikt) {
		return new Coord(avst,rikt);
	}

	private Path mPath = null;


	List<Segment> tag ;

	private TagPunkter tagPunkter;



	private class TagPunkter {
		float[] raw = new float[200];
		List<Coord> rawC = new ArrayList<Coord>();
		int c=0;
		float scaleF;

		public TagPunkter() {
			scaleF = 1/oScaleF;
		}
		public Coord getStart() {
			if (rawC.size()>0)
				return rawC.get(0);
			return null;
		}
		public Coord getLastCoord() {
			if (rawC.size()>0)
				return rawC.get(rawC.size()-1);
			return null;
		}
		public float[] getRaw() {
			return raw;
		}
		public Segment getArc(float[] xy,float distance) {	
			return createSegment(xy,distance,true);
		}
		public Segment getLine(float[]xy,float distance) {
			return createSegment(xy,distance,false);
		}

		private Segment createSegment(float[] xy,float distance,boolean isArc) {
			store(xy,distance);
			Log.d("vortex","in create segment xy"+xy[0]+","+xy[1]);
			int l = rawC.size();
			if (!equal(rawC.get(l-2),rawC.get(l-1)))
				return new Segment (rawC.get(l-2),rawC.get(l-1),isArc);
			else {
				Log.d("vortex","Equal.Undo changes");
				rawC.remove(l-1);
				c-=2;
				Log.d("vortex","c is now "+c);
				return null;
			}
		}


		private boolean equal(Coord coord, Coord coord2) {
			return (Math.abs(coord.avst-coord2.avst)<3 && Math.abs(coord.rikt-coord2.rikt)<3);
		}
		public void store(float xy[],float distance) {
			raw[c++]=xy[0];
			raw[c++]=xy[1];
			float x = (xy[0]-cx)*scaleF;
			float y= (xy[1]-cy)*scaleF;
			Log.d("vortex","in store segment xy "+x+","+y+","+distance);
			Coord cord = new Coord(x,y,(distance*scaleF));
			rawC.add(cord);	
		}

		public int size() {
			return c;
		}


		public float[] getLastXY() {
			float[] retz = new float[2];
			if (c>=2) {
				retz[0]=raw[c-2];
				retz[1]=raw[c-1];
			}
			return retz;
		}

		public void clear() {
			c=0;
			rawC.clear();
		}


	}

	public interface TagListenerI {
		public void newTag(List<Segment> s);
		public void pointAdded();
	}

	private void endTag() {
		Log.d("vortex","Jag hamna häär");
		tag.add(new Segment(tagPunkter.getLastCoord(),tagPunkter.getStart(),true));
	}

	List<List<Segment>> tagtag = new ArrayList<List<Segment>>();
	public void addTagPoint(float[]xy,float distance) {
		if (!drawActive) {
			tagPunkter = new TagPunkter();
			tagPunkter.store(xy,distance);
			drawActive=true;
			tagListener.newTag(tag);
		}  else {

			Segment newSegment = onCircle?tagPunkter.getArc(xy,distance):
				tagPunkter.getLine(xy,distance);
			if (newSegment!=null) {
				tag.add(newSegment);
				tagListener.pointAdded();
			}
		}

		/*
		else {
			if (onCircle) {
				float w = getWidth();
				float h = getHeight();
				boolean largeSize = (h>500);
				float margY = largeSize?20:0;
				float cy;
				float cx;
				if (largeSize) {
					r=(float) ((w>=h)?((h/2)-h*.1):((w/2)-w*.1));
					px.setTextSize(25);		
				}
				else {
					r=h/2;
					px.setTextSize(13);
				}
				cx = w/2;
				cy = (h/2+margY);//h/2;
				RectF oval = new RectF(-r+cx, -r+cy, r+cx, r+cy);
				Coord s1 = new Coord(cursorX,cursorY);
				float start = prev.rikt;
				float end = Delyta.rDist((int)start,s1.rikt);
				start = start-90;
				if (start<0)
					start +=360;
				mPath.addArc(oval, start,end);
				prev = s1;
			} else
				mPath.lineTo(cursorX, cursorY);
			onCircle=!onCircle;	
			invalidate();
		}
		 */
	}

	public void resetTag() {
		onCircle=true;
		cursorInitialized=false;		
		moveCursor = false;
		tag= new ArrayList<Segment>();
		tagtag.add(tag);
		drawActive = false; 
		if (tagPunkter!=null)
			tagPunkter.clear();	

	}


	public void reset() {
		tagtag.clear();
		resetTag();
		tag.clear();
	}





}


