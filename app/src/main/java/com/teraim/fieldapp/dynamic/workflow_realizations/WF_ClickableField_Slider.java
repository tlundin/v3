package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;

import java.util.ArrayList;

public class WF_ClickableField_Slider extends WF_ClickableField implements EventListener {

	private final SeekBar sb;
	private final LinearLayout ll;
	private Variable var = null;
	private EditText etview;

	public WF_ClickableField_Slider(String headerT, String descriptionT,
									WF_Context context, String id, boolean isVisible) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.selection_field_normal,null),
				isVisible);

		context.registerEventListener(this, Event.EventType.onSave);
		ll = (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_slider_element,null);
		sb = (SeekBar)ll.findViewById(R.id.spinnerOut);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.d("vortex","progress: "+progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (var!=null) {
					String value = Integer.toString(seekBar.getProgress());
					etview.setText(value);
					save();
					refresh();

				}
			}
		});
	}

	@Override
	public LinearLayout getFieldLayout() {
		Log.d("brexit","Getting field layout for slide!!");


		return ll;
	}

	@Override
	public void onEvent(Event e) {
		if (!e.getProvider().equals(getId())) {
			Log.d("nils","In onEvent for WF_ClickableField_Selection_OnSave. Provider: "+e.getProvider());
			refresh();

		} else
			Log.d("nils","Discarded...from me");
	}

	@Override
	public String getName() {
		return "CLICKABLE_SLIDER "+this.getId();
	}


	@Override
	public void addVariable(final Variable var, boolean displayOut,
							String format, boolean isVisible, boolean showHistorical) {
		if (this.var!=null) {
			Log.e("vortex","only one variable allowed for slider entryfield");
			if (var!=null) {
				o.addRow("");
				o.addRedText("Attempt to add more than one variable to slider entryfield. Variable not added: " + var.getId());
			}
			return;
		}

		super.addVariable(var,displayOut,null,isVisible,showHistorical);
		if (!myVars.isEmpty()) {
			this.var=var;
			//Has to set value for inner view as well.
			etview = (EditText) myVars.get(var).findViewById(R.id.edit);
			String txt = etview.getText().toString();

		} else {
			Log.e("vortex", "cannot initialize seekbar! empty? " + myVars.isEmpty());
			return;
		}
		Log.d("vortex","Calling initialize seekbar for "+var.getId());
		initializeSeekBar();
	}

	public void initializeSeekBar() {

		if (var!=null) {
			try {
				if (var.getValue() != null)
					sb.setProgress(Integer.parseInt(var.getValue()));;
			} catch (NumberFormatException e) {
				o.addRow("");
				o.addRedText("The variable used for slider " + this.getId() + " is not containing a numeric value");
			}

		}
	}


}
