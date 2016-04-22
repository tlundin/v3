package com.teraim.fieldapp.ui;


import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.DrawerMenuAdapter.RowType;

public class DrawerMenuSelectable implements DrawerMenuItem {

	    private final String  str1, bgColor,textColor;

	    public DrawerMenuSelectable(String text1,String bgColor,String textColor) {
	        this.str1 = text1;
	        this.bgColor = bgColor;
	        this.textColor= textColor;
	    }

	    @Override
	    public int getViewType() {
	        return RowType.LIST_ITEM.ordinal();
	    }

	    @Override
	    public View getView(LayoutInflater inflater, View convertView) {
	        View view;
	        if (convertView == null) {
	            view = (View) inflater.inflate(R.layout.drawer_menu_selectable, null);
				TextView tv = (TextView)view.findViewById(R.id.list_content1);
				//Log.d("vortex","Menuheader bg text colors: "+bgColor+" "+textColor);
				if (bgColor!=null)
					tv.setBackgroundColor(Color.parseColor(bgColor));
				if (textColor!=null)
					tv.setTextColor(Color.parseColor(textColor));

	            // Do some initialization
	        } else {
	            view = convertView;
	        }

	        TextView text1 = (TextView) view.findViewById(R.id.list_content1);
	        text1.setText(str1);

	        return view;
	    }

	}
