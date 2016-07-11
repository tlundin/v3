package com.teraim.fieldapp.dynamic.blocks;

import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.DataSource;
import com.teraim.fieldapp.dynamic.types.SimpleChartDataSource;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

public class RoundChartBlock extends Block implements EventListener {


	private static final long serialVersionUID = 4030652478782165890L;
	String label, container,name,
	type, axisTitle, margins,startAngle, dataSource;

	boolean displayValues=true,isVisible=true,percentage=false;
	int height,width;
	float textSize;
	private WF_Widget myWidget;
	GraphicalView pie;
	private CategorySeries distributionSeries;
	private Context ctx;
	private final static int DefaultHeight=300,DefaultWidth=300;
	private WF_Context myContext;

	private SimpleChartDataSource myDataSource;


	public RoundChartBlock(String blockId, String name, String label, String container,
			String type, String axisTitle, String textSize, String margins,
			String startAngle, int height,int width, boolean displayValues, boolean percentage,
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
		this.type = type;
		this.axisTitle = axisTitle;
		this.textSize = textSizeF;
		this.margins = margins;
		this.startAngle = startAngle;
		this.displayValues = displayValues;
		this.isVisible = isVisible;
		this.percentage = percentage;
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

		o = GlobalState.getInstance().getLogger();


	}




	private float dpMeasure(float textSize) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize, ctx.getResources().getDisplayMetrics());
	}


	@Override
	public void onEvent(Event e) {
		if (e.getType()==Event.EventType.onFlowExecuted) {
			Log.d("vortex","got onAttach event in pieblock. h w "+height+","+width);


			WF_Container myContainer = (WF_Container)myContext.getContainer("root");
			if (myContainer !=null) {
				if (startAngle ==null||startAngle.isEmpty())
					startAngle = "0";
				ctx = myContext.getContext();
				myDataSource = (SimpleChartDataSource) myContext.getChartDataSource(name);
				if (myDataSource!=null) {
					// Instantiating CategorySeries to plot Pie Chart



					DefaultRenderer defaultRenderer = new DefaultRenderer();
					for (int i = 0; i < myDataSource.getSize(); i++) {
						SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
						seriesRenderer.setColor(myDataSource.getColors()[i]);
						seriesRenderer.setDisplayChartValues(true);
						// Adding a renderer for a slice
						defaultRenderer.addSeriesRenderer(seriesRenderer);
					}
					defaultRenderer.setChartTitle(label);
					defaultRenderer.setChartTitleTextSize(dpMeasure(textSize + 10));
					//defaultRenderer.setZoomButtonsVisible(true);
					defaultRenderer.setDisplayValues(true);
					defaultRenderer.setStartAngle(Float.parseFloat(startAngle));

					pie = ChartFactory.getPieChartView(ctx, generateSeries(), defaultRenderer);
					myWidget = new WF_Widget(blockId, pie, isVisible, myContext);
					myContainer.add(myWidget);
				} else {
					o.addRow("");
					o.addRedText("Failed to add round chart block with id "+blockId+" - missing datasource!");
					return;
				}

			}  else {
				o.addRow("");
				o.addRedText("Failed to add round chart block with id "+blockId+" - missing container "+"root");
				return;
			}

			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pie.getLayoutParams();
			if (height==-1&&width==-1) {
				width = ctx.getResources().getDisplayMetrics().widthPixels;
				height=width;
			} else {
				if (height==-1)
					height=DefaultHeight;
				if (width==-1)
					width=DefaultWidth;
				Log.d("vortex", "Applying DIP measurements for density." + height + "," + width);
				height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, ctx.getResources().getDisplayMetrics());
				width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, ctx.getResources().getDisplayMetrics());
			}
			layoutParams.height=height;
			layoutParams.width=width;

		} else if (e.getType()==Event.EventType.onSave) {
			if (myDataSource != null && myDataSource.hasChanged()) {

				distributionSeries.clear();
				generateSeries();


				pie.repaint();
			}
		}

	}

	private CategorySeries generateSeries() {
		distributionSeries = myDataSource.getSeries();
		boolean hasCategories = myDataSource.getCategories()!=null;
		int[] currentValues = myDataSource.getCurrentValues();
		for(int i=0;i<myDataSource.getSize();i++) {
			if (hasCategories)
				distributionSeries.add(myDataSource.getCategories()[i],currentValues[i]);
			else
				distributionSeries.add(currentValues[i]);
		}
		return distributionSeries;
	}

	@Override
	public String getName() {
		return name;
	}


}
