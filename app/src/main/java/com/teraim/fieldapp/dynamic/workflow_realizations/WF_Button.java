package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.expr.SyntaxException;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.Exporter;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Created by Terje on 2016-08-23.
 */
public class WF_Button extends WF_Widget {

    protected Button myButton =null;
    protected Context ctx;

    public WF_Button(String id,Button button, boolean isVisible, WF_Context myContext) {
        super(id, button, isVisible, myContext);
        myButton=button;
        ctx = myContext.getContext();
    }

    public void setText(String text) {
        if (myButton!=null )
            myButton.setText(text);
        else
            Log.e("vortex","BUTTON NULL IN SETTEXT: "+text);
    }



    public void setOnClickListener(View.OnClickListener l) {
        myButton.setOnClickListener(l);
    }


    public static Button createInstance(String text, Context ctx) {
       // final LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
       //         (Context.LAYOUT_INFLATER_SERVICE);

        Button button = new Button(ctx);
        button.setTextSize(22);
        button.setText(text);
        return button;
    }


}
