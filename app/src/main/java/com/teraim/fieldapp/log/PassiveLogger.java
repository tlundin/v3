package com.teraim.fieldapp.log;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.ui.MenuActivity;

public class PassiveLogger implements LoggerI {

    //CharSequence myTxt = new SpannableString("");
    SpannableStringBuilder myTxt = new SpannableStringBuilder();
    SpannableString s;
    TextView log = null;
    Context myContext;
    String loggerId;
    int ticky=0;
    boolean hasRed=false;

    public PassiveLogger(Context c,String loggerId) {
        myContext = c;
        this.loggerId = loggerId;
    }

    public void setOutputView(TextView txt) {
        //has no outputview.
    }

    public void addRow(String text) {
        s = new SpannableString("\n"+text);
        myTxt.append(s);

    }
    public void addRedText(String text) {
        if (!hasRed) {
            hasRed = true;
            LocalBroadcastManager.getInstance(myContext).sendBroadcast(new Intent(MenuActivity.REDRAW));
        }
        addText(text);
        //Log.d("vortex","hasRed true for "+this.toString());
        //Log.d("vortex",""+this.toString());
    }
    public void addGreenText(String text) {
       addText(text);
    }
    public void addYellowText(String text) {
        addText(text);
    }
    public void addText(String text) {
        s = new SpannableString(text);
        myTxt.append(text);
        if (log!=null) log.setText(myTxt);
    }

    @Override
    public void addCriticalText(String text) {
        addRow(text);
    }


    public CharSequence getLogText() {
        return myTxt;
    }

    public void draw() {
        //has no widget.
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

    private void removeTicky() {
        if (tickyIs!=null && myTxt.length()>=tickyIs.length()) {
            myTxt=myTxt.delete(myTxt.length()-tickyIs.length(), myTxt.length());
            tickyIs=null;
        }
    }

    @Override
    public void removeLine() {
        if (s!=null && myTxt.length()>=s.length())
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
