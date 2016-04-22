package com.teraim.fieldapp.log;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.MenuActivity;

public class CriticalOnlyLogger implements LoggerI {
	boolean hasRed=false;
	SpannableStringBuilder myTxt = new SpannableStringBuilder();
	SpannableString s;
	TextView log = null;
	Context myContext;
	
	public CriticalOnlyLogger(Context myContext) {
		this.myContext = myContext;
	}
	
	@Override
	public void setOutputView(TextView txt) {
		log = txt;
	}

	@Override
	public void addRow(String text) {
		
	}

	@Override
	public void addRedText(String text) {
		if (!hasRed) {
			hasRed=true;
			myContext.sendBroadcast(new Intent(MenuActivity.REDRAW));
		}
		s = new SpannableString("\n"+text);
		s.setSpan(new TextAppearanceSpan(myContext, R.style.RedStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		myTxt.append(s);	 
		if (log!=null) log.setText(myTxt);
		Log.d("vortex","hasRed true for "+this.toString());
	}

	@Override
	public void addGreenText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addYellowText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public CharSequence getLogText() {
		return myTxt;
	}

	@Override
	public void draw() {
		if (log!=null) {
			log.setText(myTxt);
		}
		else
			Log.e("nils","LOG WAS NULL IN DRAW!!");
	}

	@Override
	public void clear() {
		myTxt.clear();
		if (log!=null) log.setText(myTxt);
	}

	@Override
	public void addPurpleText(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeTicky(String tickyText) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTicky() {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeLine() {
		// TODO Auto-generated method stub

	}

	
	@Override
	public boolean hasRed() {
		//Log.d("vortex","calling hasred on "+this.toString());
		if (hasRed) {
			hasRed=false;
			return true;
		}
		return false;
	}

}
