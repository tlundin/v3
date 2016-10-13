package com.teraim.fieldapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.non_generics.Constants;

/**
 * Created by Terje on 2016-10-12.
 */
public class BarcodeReader implements EventListener {

    private final WF_Context myContext;
    BarcodeDetector detector;
    Variable barCodeTarget;

    public BarcodeReader(WF_Context myContext, String targetVariableName) {
        detector =
                new BarcodeDetector.Builder(GlobalState.getInstance().getContext())
                        .build();
        this.myContext = myContext;
        barCodeTarget = GlobalState.getInstance().getVariableCache().getVariable(targetVariableName);
        barCodeTarget.setValue("");
        myContext.registerEvent(new WF_Event_OnSave("barcode"));


    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == Event.EventType.onActivityResult) {
            if(!detector.isOperational()){
                barCodeTarget.setValue("Could not set up the detector!");
                return;
            }
            Log.d("vortex", "barcode image is likely taken");
            try {

                BitmapFactory.Options option = new BitmapFactory.Options();
                //option.inJustDecodeBounds = false;
                option.inSampleSize = 4;
                Bitmap myBitmap = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR + Constants.TEMP_BARCODE_IMG_NAME,option);
                Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
                SparseArray<Barcode> barcodes = detector.detect(frame);
                if (barcodes!=null) {
                    Log.d("vortex","barcodes size: "+barcodes.size());
                    if (barcodes.size()>0) {
                        Log.d("vortex",barcodes.valueAt(0).displayValue);
                        if (barCodeTarget != null)
                            barCodeTarget.setValue(barcodes.valueAt(0).rawValue);
                    } else
                        barCodeTarget.setValue("No barcode found!");
                    myContext.registerEvent(new WF_Event_OnSave("barcode"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /*
            //turn image on disk to bitmap.
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = false;



            Barcode thisCode = barcodes.valueAt(0);
            //Set the associated variable to the value given.
            if (barCodeTarget!=null)
                barCodeTarget.setValue(thisCode.rawValue);

        }
    }
    */

    @Override
    public String getName() {
        return "barcodereader";
    }
}
