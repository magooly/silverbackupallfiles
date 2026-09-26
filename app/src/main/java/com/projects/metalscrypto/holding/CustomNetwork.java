package com.projects.metalscrypto.holding;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;

public class CustomNetwork
{
    public static String TAG="TAG";
    @SuppressLint("MissingPermission")
    public static  boolean  isNetworkAvailable(Context context)
    {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
