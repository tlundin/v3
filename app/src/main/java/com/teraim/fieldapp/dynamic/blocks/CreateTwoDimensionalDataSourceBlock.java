package com.teraim.fieldapp.dynamic.blocks;

import android.graphics.Color;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.SimpleChartDataSource;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.log.LoggerI;

import org.achartengine.model.CategorySeries;

import java.util.ArrayList;
import java.util.List;

public class CreateTwoDimensionalDataSourceBlock extends Block {

	String id=null,myChart=null;

	CategorySeries series;
	String[] myCategories =null,myVariableNames=null;

	List<Variable> myVariables;

	final int[] colors = { Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
			Color.YELLOW,Color.BLUE, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.RED,
			Color.YELLOW };

	public CreateTwoDimensionalDataSourceBlock(String id, String title, String chart, String[] categories, String[] variableNames) {
		super();
		this.blockId = id;
		series = new CategorySeries(title);
		myVariableNames = variableNames;
		myChart=chart;

		if (categories != null && variableNames!=null && (categories.length == variableNames.length))
			myCategories = categories;
	}

	public void create(WF_Context myContext) {
		Variable v;
		VariableCache cache = GlobalState.getInstance().getVariableCache();
		LoggerI o = GlobalState.getInstance().getLogger();
		if (myVariables==null) {
			myVariables = new ArrayList<Variable>();
			for (String variable : myVariableNames) {
				v = cache.getVariable(variable);
				if (v == null) {
					o.addRow("");
					o.addRedText("Variable " + variable + " not found when creating datasource in block " + blockId);
					return;
				}

				myVariables.add(v);
			}
		}

		myContext.addChartDataSource(myChart, new SimpleChartDataSource() {
			@Override
			public boolean hasChanged() {
				return true;
			}

			@Override
			public CategorySeries getSeries() {
				return series;
			}

			@Override
			public int[] getCurrentValues() {

				int i = 0;
				int[] ret = new int[myVariables.size()];
				for(Variable v:myVariables) {
					String valS = v.getValue();
					ret[i]=-1;
					if (valS!=null)
					try {
						ret[i] = Integer.parseInt(valS);
					} catch (NumberFormatException e) {}
					i++;
				}
				Log.d("vortex","CurrValues: "+ret.toString());
				return ret;
			}

			@Override
			public int getSize() {
				return myVariables.size();
			}

			@Override
			public int[] getColors() {
				return colors;
			}

			@Override
			public String[] getCategories() {
				return  myCategories;
			}
		});
	}

	
}
