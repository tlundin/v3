package com.teraim.fieldapp.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.FilterFactory.Range;

public class CombinedRangeAndListFilter implements TextFilter {

	List<TextFilter> myFilters;
	String myPrint = "";
	boolean hasDefault = false;
	private Variable mVar;
	private Vibrator myVibrator;

	public CombinedRangeAndListFilter(Vibrator vib, Variable myVar, Set<String> allowedValues, List<Range> allowedRanges, boolean hasDefault) {
		this.hasDefault=hasDefault;
		myVibrator = vib;
		myFilters = new ArrayList<TextFilter>();
		if (allowedRanges!=null) {
			for (Range r:allowedRanges) 
				myFilters.add(new InputFilterMinMax(r.min,r.max));

		}
		if (allowedValues!=null)
			myFilters.add(new InputFilterListValues(allowedValues));
		if (myFilters.size()>0) {		
			Iterator<TextFilter>it=myFilters.iterator();
			//Skip default filter.
			if (hasDefault)
				it.next();
			while (it.hasNext())
				myPrint +=it.next().prettyPrint()+",";

			if (myPrint.endsWith(","))
				myPrint = myPrint.substring(0, myPrint.length()-1);
		}
		mVar = myVar;
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {
		//Return whenever one filter accepts.
		int c=0;
		//Set if default filter triggers.
		mVar.setOutOfRange(false);
		for (TextFilter filter:myFilters) {
			//Log.d("nils","checking filter "+c+" hasdefault: "+hasDefault);
			if (filter.filter(source, start, end,dest, dstart, dend)==null){
				if (hasDefault && c==0) {
//					Log.d("nils","Default triggered!");
					mVar.setOutOfRange(true);
				} else {
//					Log.d("nils","Other filter triggered.");
				}
				return null;
			} //else
//				Log.d("nils","Filter did not trigger");
			c++;
		}
		//If no filter ok - disallow.
		/*
		if (source.length()>0) {
			Log.e("vortex","burr: "+source+"dest: "+((dest==null)?"null":dest.toString()));
			burroblink(Constants.BURR_LENGTH);
		}
		*/
		return "";
	}

	public String prettyPrint() {
		return myPrint;	
	}

	public void testRun() {
		final SpannableString DUMMY = new SpannableString("");		
			filter(mVar.getValue(), 0, 0, DUMMY, 0, 0);
	}
	
	private void burroblink(long time) {
		myVibrator.vibrate(time);
		//ColorDrawable[] colorBlink = {new ColorDrawable(Color.parseColor("#47A3FF")), new Color.parseColor("#60BF60")};
		//TransitionDrawable trans = new TransitionDrawable(colorBlink);
		//bg.setBackgroundDrawable(trans);
		//trans.startTransition((int)time);
	}
	
}
