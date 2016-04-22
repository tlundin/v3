package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.teraim.fieldapp.R;

public class WF_TextBlockWidget extends WF_Widget {

	public WF_TextBlockWidget(WF_Context ctx,String label,String id, boolean isVisible) {	
		super(id, LayoutInflater.from(ctx.getContext()).inflate(R.layout.text_block,null), isVisible,ctx);
		if (label!=null) {
			Log.d("vortex","Label is: "+label);
			((TextView)getWidget().findViewById(R.id.text_block)).setText(Html.fromHtml(label));
		}
	}
	
	
}
