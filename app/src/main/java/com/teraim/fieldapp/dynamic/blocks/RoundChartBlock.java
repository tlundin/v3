package com.teraim.fieldapp.dynamic.blocks;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.SimpleChartDataSource;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

public class RoundChartBlock extends Block implements EventListener {


	private static final long serialVersionUID = 4030652478782165890L;
	String label, container,name, margins,startAngle;

	boolean displayValues=true,isVisible=true;
	int height,width;
	float textSize;
	private WF_Widget myWidget;
	GraphicalView pie;
	private CategorySeries distributionSeries;
	private Context ctx;
	private final static int DefaultHeight=300,DefaultWidth=300;
	private int insertIndex = -1;
	private WF_Context myContext;

	private SimpleChartDataSource myDataSource;


	public RoundChartBlock(String blockId, String name, String label, String container,
						   String textSize, String margins,String startAngle, int height,int width, boolean displayValues,
						   boolean isVisible) {
		super();
		float textSizeF = 10;
		try {
			textSizeF = Float.parseFloat(textSize);
		} catch (NumberFormatException e) {
			Log.d("vortex","error in format...default to 10");
		}
		this.blockId = blockId;
		this.name = name;
		this.label = label;
		this.container = container;
		this.textSize = textSizeF;
		this.margins = margins;
		this.startAngle = startAngle;
		this.displayValues = displayValues;
		this.isVisible = isVisible;
		this.height=height;
		this.width=width;
		Log.d("vortex","height"+height);

	}

	public void create(WF_Context myContext) {
		// Color of each Pie Chart Sections

		this.myContext = myContext;
		//delay creat until after other blocks executed, to make sure datasource has been added to context.
		//(In case this block is executed after the datasource block)
		myContext.registerEventListener(this, EventType.onFlowExecuted);
		myContext.registerEventListener(this, EventType.onSave);

		o = GlobalState.getInstance().getLogger();



		WF_Container myContainer = (WF_Container)myContext.getContainer(container);

		if (myContainer !=null) {
			insertIndex = myContainer.getWidgets().size();

		}

	}





	private float dpMeasure(float textSize) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize, ctx.getResources().getDisplayMetrics());
	}


	@Override
	public void onEvent(Event e) {


		if (e.getType()==Event.EventType.onFlowExecuted ) {
			WF_Container myContainer = (WF_Container) myContext.getContainer(container);
			//Create pie chart if not already done.
			if (pie==null) {
				Log.d("vortex", "got onAttach event in pieblock. h w " + height + "," + width);

				if (myContainer != null) {
					if (startAngle == null || startAngle.isEmpty())
						startAngle = "0";
					ctx = myContext.getContext();
					myDataSource = (SimpleChartDataSource) myContext.getChartDataSource(name);
					if (myDataSource != null) {
						// Instantiating CategorySeries to plot Pie Chart
						DefaultRenderer defaultRenderer = new DefaultRenderer();
						for (int i = 0; i < myDataSource.getSize(); i++) {
							SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
							seriesRenderer.setColor(myDataSource.getColors()[i]);
							//seriesRenderer.setDisplayChartValues(true);
							// Adding a renderer for a slice
							defaultRenderer.addSeriesRenderer(seriesRenderer);
						}
						defaultRenderer.setChartTitle(label);
						defaultRenderer.setChartTitleTextSize(dpMeasure(textSize + 10));
						defaultRenderer.setLabelsTextSize(dpMeasure(textSize));
						//defaultRenderer.setZoomButtonsVisible(true);
						defaultRenderer.setDisplayValues(false);
						defaultRenderer.setShowLegend(false);
						defaultRenderer.setShowLabels(true);
						defaultRenderer.setApplyBackgroundColor(true);
						defaultRenderer.setBackgroundColor(Color.TRANSPARENT);
						defaultRenderer.setLabelsColor(Color.parseColor("#696969"));
						defaultRenderer.setPanEnabled(false);

						defaultRenderer.setStartAngle(Float.parseFloat(startAngle));

						pie = ChartFactory.getPieChartView(ctx, generateSeries(), defaultRenderer);
						myWidget = new WF_Widget(blockId, pie, isVisible, myContext);
						myContainer.add(myWidget);
						myContainer.getViewGroup().addView(myWidget.getWidget(), insertIndex);
						myWidget.getWidget().post(new Runnable() {
							@Override
							public void run() {
								WF_Container myContainer = (WF_Container) myContext.getContainer(container);
								LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pie.getLayoutParams();
								if (height < 0) {
									height = myContainer.getViewGroup().getHeight();
								} else {
									Log.d("vortex", "Applying DIP measurements for height." + height );
									height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, ctx.getResources().getDisplayMetrics());
								}
								if (width < 0) {
									if (width == -1)
										width = myContainer.getViewGroup().getWidth();
									else {
										Log.d("bretox","w set to fill!");
										int w = myContainer.getViewGroup().getWidth();
										width = w;
										height = w;
									}
								} else {
									Log.d("vortex", "Applying DIP measurements for width." + height );
								  width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, ctx.getResources().getDisplayMetrics());
								}

								layoutParams.height = height;
								layoutParams.width = width;
								myWidget.getWidget().requestLayout();
								Log.d("vortex","W: "+layoutParams.width+" H:"+layoutParams.height);
								Log.d("vortex","pie is attached: "+myWidget.getWidget().isShown()+" w"+myWidget.getWidget().getWidth()+" h:"+myWidget.getWidget().getHeight());
								Log.d("vortex","pie is attached: "+pie.isShown()+" "+myWidget.isVisible());
							}
						});

					} else {
						o.addRow("");
						o.addRedText("Failed to add round chart block with id " + blockId + " - missing datasource!");
						myContext.removeEventListener(this);
						return;
					}

				} else {
					o.addRow("");
					o.addRedText("Failed to add round chart block with id " + blockId + " - missing container " + container);
					myContext.removeEventListener(this);
					return;
				}



			} else {
				generateSeries();
				myWidget = new WF_Widget(blockId, pie, isVisible, myContext);
				myContainer.add(myWidget);
				((ViewGroup)pie.getParent()).removeView(pie);
				myContainer.getViewGroup().addView(myWidget.getWidget(), insertIndex);
			}
			pie.setVisibility(isVisible? View.VISIBLE:View.GONE);
		} else if (e.getType()==Event.EventType.onSave) {
			if (myDataSource != null && myDataSource.hasChanged()) {
				generateSeries();
				pie.repaint();
			}
		}

	}

	private CategorySeries generateSeries() {
		distributionSeries = myDataSource.getSeries();
		distributionSeries.clear();
		boolean hasCategories = myDataSource.getCategories()!=null;
		int[] currentValues = myDataSource.getCurrentValues();
		//If all elements are 0, we want to make a default pie with a message.
		boolean sumzero = true;
		for(int i=0;i<myDataSource.getSize();i++) {
			Log.d("vortex","current value "+i+": "+currentValues[i]);
			if (hasCategories)
				distributionSeries.add(myDataSource.getCategories()[i],currentValues[i]);
			else
				distributionSeries.add(currentValues[i]);
			if (currentValues[i]>0)
				sumzero=false;
		}
		if (sumzero) {
			distributionSeries.set(myDataSource.getSize()-1,"EMPTY",1);
		}
		return distributionSeries;
	}

	@Override
	public String getName() {
		return name;
	}


}
