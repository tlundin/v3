package com.teraim.fieldapp.dynamic.workflow_realizations;

import java.util.List;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Tools.Unit;

public class WF_DisplayValueField extends WF_Widget implements EventListener {

	private String formula,label;
	protected GlobalState gs;
	protected Unit unit;
	private String format;
	private List<EvalExpr> formulaE;
	private WF_Context myContext;

	public WF_DisplayValueField(String id, String formula,WF_Context ctx, Unit unit, 
			String label, boolean isVisible,String format,String bgColor, String textColor) {
		super(id, LayoutInflater.from(ctx.getContext()).inflate(R.layout.display_value_textview,null), isVisible,ctx);
		TextView header = (TextView)getWidget().findViewById(R.id.header);
		LinearLayout bg = (LinearLayout)getWidget().findViewById(R.id.background);
		header.setText(label);
		this.label=label;
		if (bgColor!=null)
			bg.setBackgroundColor(Color.parseColor(bgColor));
		if (textColor!=null)
			header.setTextColor(Color.parseColor(textColor));
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.formula = formula;
		Log.d("nils","In WF_DisplayValueField Create");	
		ctx.addEventListener(this, EventType.onSave);	
		this.unit=unit;
		formulaE = Expressor.preCompileExpression(formula);
		if (formulaE==null)
		{
			
			o.addRow("");
			o.addRedText("Parsing of formula for DisplayValueBlock failed. Formula: "+formula);
			Log.e("vortex","Parsing of formula for DisplayValueBlock failed. Formula: "+formula);
		}
		this.format = format;
		this.myContext=ctx;
		
		
		//this.onEvent(new WF_Event_OnSave("display_value_field"));
	}

	//update variable.
	@Override
	public void onEvent(Event e) {
		Log.d("vortex","In onEvent for create_display_value_field. Caller: "+e.getProvider());
		if (myContext.myEndIsNear()) {
			Log.e("vortex","END IS NEAAARRR");
			return;
		}
		String result = Expressor.analyze(formulaE);
		//Do not evaluate if the expression is evaluated to be a literal or defined as literal.
		if (result==null) {
				o.addRow("");
				o.addText("Formula "+formula+" returned null");	
				((TextView)this.getWidget().findViewById(R.id.outputValueField)).setText("");
				((TextView)this.getWidget().findViewById(R.id.outputUnitField)).setText("");
				return;
		} 
		String strRes=result;
		if (format!=null && format.equalsIgnoreCase("B")) {
			if (result.equals("true"))
					strRes = GlobalState.getInstance().getContext().getString(R.string.yes);
			else if (result.equals("false"))
				strRes = GlobalState.getInstance().getContext().getString(R.string.no);
		} 
		else if (Tools.isNumeric(result))
			strRes = WF_Not_ClickableField.getFormattedText(result,format);

		
			
		o.addRow("");
		o.addText("Text in DisplayField "+label+" is [");o.addGreenText(strRes); o.addText("]");
		((TextView)this.getWidget().findViewById(R.id.outputValueField)).setText(strRes);
		((TextView)this.getWidget().findViewById(R.id.outputUnitField)).setText(Tools.getPrintedUnit(unit));
	}

}





