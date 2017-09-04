package com.teraim.fieldapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.utils.PersistenceHelper;


public class AboutActivity extends Activity {


    boolean gone = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.fragment_about);

        super.onCreate(savedInstanceState);
       if (GlobalState.getInstance()!=null) {
            final TextView logView = (TextView)findViewById(R.id.LoadDetails);
            logView.setVisibility(View.GONE);
            CharSequence log = GlobalState.getInstance().getLogTxt();
            if (log!=null)
                logView.setText(log);
            else
                Log.e("bortex","LogTxt Null..!");

            String bName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
            float version = GlobalState.getInstance().getPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
            TextView ver = (TextView)findViewById(R.id.BundleNameAndVersion);
            ver.setText(bName+" "+((version==-1)?"":version));
           Button feedbackButton = (Button)findViewById(R.id.feedbackButton);
           final Button buttonDetails = (Button)findViewById(R.id.buttonDetails);

           feedbackButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   endMail();
               }
           });
           buttonDetails.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   if (gone) {
                       gone=false;
                       logView.setVisibility(View.VISIBLE);
                       buttonDetails.setText("Hide details");
                   } else {
                       gone=true;
                       logView.setVisibility(View.GONE);
                       buttonDetails.setText("Details");
                   }


               }
           });

       }
    }


    private void endMail() {
        /* Create the Intent */
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

/* Fill it with Data */
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"info@teraim.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
/* Send it off to the Activity-Chooser */
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }









}
