package com.teraim.fieldapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;

public class Connectivity {


    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isAvailable = (activeNetworkInfo != null && activeNetworkInfo.isConnected());
        if (isAvailable) {
            return !isAirplaneModeOn(context);
        }
        Log.d("vortex","network deed...");
        return false;
    }

    private static boolean isAirplaneModeOn(Context context) {

        return Settings.Global.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;

    }

}
