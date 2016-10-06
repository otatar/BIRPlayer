package com.example.otatar.birplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by o.tatar on 05-Oct-16.
 * Class for checking internet connectivity
 */

public class NetworkUtil {

    private static final String LOG_TAG = "NetworkUtilLog";

    public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;

    /**
     * Check whether the device is connected, and if so, whether the connection
     * is wifi or mobile (it could be something else).
     */
    public static int checkNetworkConnection(Context context) {

        boolean wifiConnected;
        boolean mobileConnected;

        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;

            if (wifiConnected) {
                Log.i(LOG_TAG, context.getString(R.string.vifi_connection));
                return TYPE_WIFI;

            } else if (mobileConnected) {
                Log.i(LOG_TAG, context.getString(R.string.mobile_connection));
                return TYPE_MOBILE;

            }
        }

        Log.i(LOG_TAG, context.getString(R.string.no_connection));
        return TYPE_NOT_CONNECTED;

    }


    /**
     * Class for network change listening (BroadcastReceiver)
     */
    class NetworkChangeReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(LOG_TAG, "onReceive");

            //Network change, inform user
           switch (checkNetworkConnection(context)) {

               case NetworkUtil.TYPE_MOBILE:
                   Toast.makeText(context, R.string.mobile_connection_hint, Toast.LENGTH_SHORT);
                   break;

               case NetworkUtil.TYPE_NOT_CONNECTED:
                   Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT);
                   break;
           }
        }
    }

}
