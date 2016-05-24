package com.teraim.fieldapp.log;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.MenuActivity;

public class Logger implements LoggerI {

	//CharSequence myTxt = new SpannableString("");
	SpannableStringBuilder myTxt = new SpannableStringBuilder();
	SpannableString s;
	TextView log = null;
	Context myContext;
	String loggerId;
	int ticky=0;
	boolean hasRed=false;

	public Logger(Context c,String loggerId) {
		myContext = c;
		this.loggerId = loggerId;
	}

	public void setOutputView(TextView txt) {
		log = txt;
	}

	public void addRow(String text) {
		s = new SpannableString("\n"+text);
		myTxt.append(s);
	}
	public void addRedText(String text) {
		if (!hasRed) {
			Log.e("vortex", "GETS TO SEND");
			hasRed = true;
			myContext.sendBroadcast(new Intent(MenuActivity.REDRAW));
		}
		s = new SpannableString(text);
		s.setSpan(new TextAppearanceSpan(myContext, R.style.RedStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		myTxt.append(text);
		if (log!=null) log.setText(myTxt);
		//Log.d("vortex","hasRed true for "+this.toString());
		//Log.d("vortex",""+this.toString());
	}
	public void addGreenText(String text) {
		s = new SpannableString(text);
		s.setSpan(new TextAppearanceSpan(myContext, R.style.GreenStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		myTxt.append(s);
		if (log!=null) log.setText(myTxt);
	}
	public void addYellowText(String text) {
		s = new SpannableString(text);
		s.setSpan(new TextAppearanceSpan(myContext, R.style.YellowStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		myTxt.append(s);
		if (log!=null) log.setText(myTxt);
	}
	public void addText(String text) {
		s = new SpannableString(text);
		myTxt.append(text);
		if (log!=null) log.setText(myTxt);
	}



	public CharSequence getLogText() {
		return myTxt;
	}

	public void draw() {
		if (log!=null) {
			log.setText(myTxt);
			Layout layout = log.getLayout();
			if (layout!=null) {
				final int scrollAmount = layout.getLineTop(log.getLineCount()) - log.getHeight();
				// if there is no need to scroll, scrollAmount will be <=0
				if (scrollAmount > 0) {
					//Log.d("vortex","scrollamount is "+scrollAmount);
					log.scrollTo(0, scrollAmount);
				}
				else
					log.scrollTo(0, 0);
			}
		}
		else
			Log.e("nils","LOG WAS NULL IN DRAW!!");
	}


	public void clear() {
		myTxt.clear();
		if (log!=null) log.setText(myTxt);
	}



	@Override
	public void addPurpleText(String text) {
		s = new SpannableString(text);
		s.setSpan(new TextAppearanceSpan(myContext, R.style.PurpleStyle),0,s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		myTxt.append(s);
	}


	String tickyIs = null;

	@Override
	public void writeTicky(String tickyText) {
		if (tickyIs==null) {
			myTxt.append(tickyText);
		}
		else {
			removeTicky();
			myTxt.append(tickyText);

		}
		tickyIs=tickyText;
		draw();
	}

	@Override
	public void removeTicky() {
		if (tickyIs!=null) {
			myTxt=myTxt.delete(myTxt.length()-tickyIs.length(), myTxt.length());
			tickyIs=null;
		}
	}

	@Override
	public void removeLine() {
		if (s!=null)
			myTxt = myTxt.delete(myTxt.length()-s.length(),myTxt.length());
	}

	@Override
	public boolean hasRed() {
		if (hasRed) {
			hasRed=false;
			return true;
		}
		return false;
	}


}
