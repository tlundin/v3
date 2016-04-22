package com.teraim.fieldapp.dynamic.blocks;

import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

public class RoundChartBlock extends Block implements EventListener {


	private static final long serialVersionUID = 4030652478782165890L;
	String label, container,
	type, axisTitle, margins,startAngle, dataSource;

	boolean displayValues=true,isVisible=true,percentage=false;
	int height,width;
	float textSize;
	private WF_Widget myWidget;
	GraphicalView pie;
	private CategorySeries distributionSeries;

	public RoundChartBlock(String id, String label, String container,
			String type, String axisTitle, String textSize, String margins,
			String startAngle, int height,int width, boolean displayValues, boolean percentage,
			boolean isVisible,String dataSource) {
		super();
		float textSizeF = 10;
		try {
			textSizeF = Float.parseFloat(textSize);
		} catch (NumberFormatException e) {
			Log.d("vortex","error in format...default to 10");
		}
		this.blockId = id;
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
		this.dataSource = dataSource;
		Log.d("vortex","height"+height);

	}

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		WF_Container myContainer = (WF_Container)myContext.getContainer("root");
		//		WF_Container myPieContainer = (WF_Container)myContext.getContainer("pie");
		if (myContainer !=null) {
			if (startAngle ==null||startAngle.isEmpty())
				startAngle = "0";
			pie = createPie(myContext.getContext());

			//View mX = LayoutInflater.from(myContext.getContext()).inflate(R.layout.chart_wrapper,myContainer.getViewGroup());
			//LinearLayout mChart = (LinearLayout)mX.findViewById(R.id.myChart);

			myWidget = new WF_Widget(blockId,pie,isVisible,myContext);	
			myContainer.add(myWidget);
			Button b = new Button(myContext.getContext());
			b.setText("Generate some data");
			b.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					RoundChartBlock.this.onEvent(new WF_Event_OnSave("me"));
				}
			});
			myContainer.add(new WF_Widget("tst",b,isVisible,myContext));
			Log.d("nils","Added pie chart "+myWidget.getId()+" to container "+myContainer.getId());
			myContext.addEventListener(this, EventType.onFlowExecuted);

		}  else {
			o.addRow("");
			o.addRedText("Failed to add round chart block with id "+blockId+" - missing container "+myContainer);
		}

	}


	private GraphicalView createPie(Context ctx){

		//View mChart = LayoutInflater.from(ctx).inflate(R.layout.chart_wrapper,null);
		// Pie Chart Section Names
		String[] code = new String[] {
				"Eclair & Older", "Froyo", "Gingerbread", "Honeycomb",
				"IceCream Sandwich", "Jelly Bean"
		};

		// Pie Chart Section Value
		double[] distribution = { 3.9, 12.9, 55.8, 1.9, 23.7, 1.8 } ;


		// Color of each Pie Chart Sections
		int[] colors = { Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
				Color.YELLOW };

		// Instantiating CategorySeries to plot Pie Chart
		distributionSeries = new CategorySeries("PIe");
		for(int i=0 ;i < distribution.length;i++){
			// Adding a slice with its values and name to the Pie Chart
			distributionSeries.add(code[i], distribution[i]);
		}

		// Instantiating a renderer for the Pie Chart
		DefaultRenderer defaultRenderer  = new DefaultRenderer();
		for(int i = 0 ;i<distribution.length;i++){
			SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
			seriesRenderer.setColor(colors[i]);
			seriesRenderer.setDisplayChartValues(true);
			// Adding a renderer for a slice
			defaultRenderer.addSeriesRenderer(seriesRenderer);
		}

		defaultRenderer.setChartTitle(label);
		defaultRenderer.setChartTitleTextSize(textSize+10);
		//defaultRenderer.setZoomButtonsVisible(true);
		defaultRenderer.setBackgroundColor(Color.GRAY);
		defaultRenderer.setDisplayValues(true);
		defaultRenderer.setInScroll(true);
		defaultRenderer.setStartAngle(Float.parseFloat(startAngle));


		//Need to create the datasource and ask for data.


		// Getting a reference to LinearLayout of the MainActivity Layout
		//LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart_container);


		// Creating a Line Chart
		// ((LinearLayout)mChart.findViewById(R.id.chart)).addView(ChartFactory.getPieChartView(ctx, distributionSeries , defaultRenderer),new LayoutParams
		// 		(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


		//Log.w("In Chart", mChart.toString() +"");
		return ChartFactory.getPieChartView(ctx, distributionSeries , defaultRenderer);

	}

	private final String[] cat = new String[] {"ab","ra","ka","da","bra","la"};
	@Override
	public void onEvent(Event e) {
		if (e.getType()==Event.EventType.onFlowExecuted) {
			Log.d("vortex","got onAttach event in pieblock. h w "+height+","+width);
			LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pie.getLayoutParams();
			layoutParams.height=(height!=-1?height:300);
			layoutParams.width=(width!=-1?width:300);
		} else if (e.getType()==Event.EventType.onSave) {
			Random random = new Random();
			int[] d2 = new int[6];
			distributionSeries.clear();
			for (int i=0;i<d2.length;i++) {
				int R = random.nextInt(100);			
				distributionSeries.add(cat[i],(double)R);
			}
			pie.repaint();
		}
	}



}
