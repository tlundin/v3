package com.teraim.fieldapp.log;

import android.content.Context;
import android.widget.TextView;

public class PlainLogger implements LoggerI {
    private final Context myContext;
    TextView txt;
    String loggerId;

    public PlainLogger(Context c, String loggerId) {
        myContext = c;
        this.loggerId = loggerId;
    }
    @Override
    public void setOutputView(TextView txt) {
        this.txt = txt;
    }

    @Override
    public void addRow(String text) {
        txt.setText(text);
    }

    @Override
    public void addRedText(String text) {
        txt.setText(text);
    }

    @Override
    public void addGreenText(String text) {
        txt.setText(text);
    }

    @Override
    public void addYellowText(String text) {
        txt.setText(text);
    }

    @Override
    public void addText(String text) {
        txt.setText(text);
    }

    @Override
    public void addCriticalText(String text) {
        txt.setText(text);
    }

    @Override
    public CharSequence getLogText() {
        return txt.getText();
    }

    @Override
    public void draw() {

    }

    @Override
    public void clear() {
        txt.setText("");
    }

    @Override
    public void addPurpleText(String text) {
        txt.setText(text);
    }

    @Override
    public void removeLine() {

    }

    @Override
    public boolean hasRed() {
        return false;
    }
}
