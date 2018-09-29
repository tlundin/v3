package com.teraim.fieldapp.ui;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.DrawerMenuAdapter.RowType;


public class DrawerMenuHeader implements DrawerMenuItem {

	private final String name,bgColor,textColor;

	public DrawerMenuHeader(String label,String bgColor,String textColor) {
		this.name = label;
		this.bgColor=bgColor;
		this.textColor=textColor;
	}

	@Override
	public int getViewType() {
		return RowType.HEADER_ITEM.ordinal();
	}

	@Override
	public View getView(LayoutInflater inflater, View convertView) {
		View view;
		TextView tv;
		if (convertView == null) {
			view = inflater.inflate(R.layout.drawer_menu_header, null);
			tv = view.findViewById(R.id.separator);
			//Log.d("vortex","Menuheader bg text colors: "+bgColor+" "+textColor);
			if (bgColor!=null)
				tv.setBackgroundColor(Color.parseColor(bgColor));
			if (textColor!=null)
				tv.setTextColor(Color.parseColor(textColor));
			// Do some initialization
		} else {
			view = convertView;
			tv = view.findViewById(R.id.separator);
		}


		tv.setText(name);

		return view;
	}

}