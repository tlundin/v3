package com.teraim.fieldapp.log;

import android.widget.TextView;

public interface LoggerI {
	
	 public void setOutputView(TextView txt);	 
	 public void addRow(String text);
	 public void addRedText(String text);
	 public void addGreenText(String text);
	 public void addYellowText(String text);
	 public void addText(String text);
	 public CharSequence getLogText();
	 public void draw();
	 public void clear();
	 public void addPurpleText(String string);
	 public void writeTicky(String tickyText);
	 public void removeTicky();
	 public void removeLine();
	 public boolean hasRed();

}
