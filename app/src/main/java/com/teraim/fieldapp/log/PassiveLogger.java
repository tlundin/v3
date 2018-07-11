package com.teraim.fieldapp.log;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import com.teraim.fieldapp.ui.MenuActivity;

public class PassiveLogger implements LoggerI {

    StringBuilder log = new StringBuilder();
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
        addText(text+"\n");

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
        log.append(text);
    }

    @Override
    public void addCriticalText(String text) {
        addRow(text);
    }


    public CharSequence getLogText() {
        return log.toString();
    }

    public void draw() {
        //has no widget.
    }


    public void clear() {
    }



    @Override
    public void addPurpleText(String text) {
        addText(text);
    }

    @Override
    public void removeLine() {

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
