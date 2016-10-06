package com.example.otatar.birplayer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivityLog";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //*********************** Check network connection *****************************************
        switch (NetworkUtil.checkNetworkConnection(this)) {

            case NetworkUtil.TYPE_WIFI:
                //Start NowPlayingActivity
                Intent intent = new Intent(this, NowPlayingActivity.class);
                startActivity(intent);
                break;

            case NetworkUtil.TYPE_MOBILE:
                //Alert user about connection with a dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.mobile_connection_hint)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //Start NowPlayingActivity
                                        Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
                                        startActivity(intent);

                                    }
                                }
                        );

                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            case NetworkUtil.TYPE_NOT_CONNECTED:
                //Alert user about connection with a dialog
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage(R.string.no_connection)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //Start NowPlayingActivity
                                        Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
                                        startActivity(intent);
                                    }
                                }
                        );

                AlertDialog dialog1 = builder1.create();
                dialog1.show();
                break;

            //**************************************************************************************
        }

    }




}


