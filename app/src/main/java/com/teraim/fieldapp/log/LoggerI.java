package com.teraim.fieldapp.log;

import android.text.SpannableString;
import android.widget.TextView;

public interface LoggerI {
	
	 void setOutputView(TextView txt);
	 void addRow(String text);
	 void addRedText(String text);
	 void addGreenText(String text);
	 void addYellowText(String text);
	 void addText(String text);

	void addCriticalText(String text);

	CharSequence getLogText();
	 void draw();
	 void clear();
	 void addPurpleText(String string);
	 void writeTicky(String tickyText);
	 void removeLine();
	 boolean hasRed();

}
