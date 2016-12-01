package com.example.otatar.birplayer;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import me.itangqi.waveloadingview.WaveLoadingView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RadioPlayerFragment extends Fragment {

    /**
     * Constants to use with Messenger and Intents
     */
    public static final String EXTRA_PARAM = "param";
    public static final String ACTION = "action";
    public static final String ACTION_INIT = "init";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_PAUSE = "pause";
    public static final int SEND_TIME = 8;
    public static final int SEND_BITRATE = 7;
    public static final int SEND_TITLE = 6;
    public static final int SEND_BUFFERING = 5;
    public static final int SEND_ERROR = 4;
    public static final int SEND_ALERT = 3;
    public static final int SEND_ACTION = 2;
    public static final int UPDATE_STATUS = 1;
    public static final int REGISTER = 0;
    public static final String STATUS = "status";
    public static final String START_FOREGROUND = "startForeground";

    public static final String LOG_TAG = "RadioPlayerFragmentLog";
    public static final String RADIO_STATION_OBJECT = "radio_station_object";

    public static final String RADIO_URL = "http://streaming.radioba.ba:10002/radio_Ba";
    public static final String RADIO_LOCAL_URL = "http://10.0.2.2:8080";

    /* Radio Station object */
    private RadioStation radioStation;

    private Boolean bound = false;

    //Messenger
    private Messenger fromServiceMessenger;
    
    //Reference to service messenger
    private Messenger toServiceMessenger;

    /* Reference to status TextView */
    private TextView statusTextView;

    /* Reference to favorite check box */
    private CheckBox favoriteCheckBox;

    /* Reference to playing time text view */
    private TextView playingTime;

    /* Is paying time running */
    private boolean playingTimeRunning;

    /* Num of sec since playing */
    private int playingSecs;

    private int playingBitrate = 0;

    private WaveLoadingView mWaveLoadingView;





    public RadioPlayerFragment() {
        // Required empty public constructor
    }


    /**
     * Method to instantiate and initialize new RadioPlayerFragment object
     * @param radioStation
     * @return new RadioPlayerFragment object
     */
    public static RadioPlayerFragment newInstance(RadioStation radioStation) {

        Bundle bundle = new Bundle();
        bundle.putSerializable(RADIO_STATION_OBJECT, radioStation);

        RadioPlayerFragment radioPlayerFragment = new RadioPlayerFragment();
        radioPlayerFragment.setArguments(bundle);

        return radioPlayerFragment;
    }


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG_TAG, "RadioPlayerFragment:handleMessage()");

            if(msg.what == UPDATE_STATUS)  {
                Bundle bundle = msg.getData();
                setStatus(bundle.getString(STATUS));

                if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PLAYING)) {
                    playingTimeRunning = true;
                    mWaveLoadingView.setAmplitudeRatio(60);

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PAUSED)) {
                    playingTimeRunning = false;
                    mWaveLoadingView.setAmplitudeRatio(0);

                } else {
                    playingTimeRunning = false;
                    playingSecs = 0;
                    playingBitrate = 0;
                    mWaveLoadingView.setAmplitudeRatio(0);
                }

            } else if (msg.what == SEND_ERROR || msg.what == SEND_ALERT) {
                //Inform user about that
                Bundle bundle = msg.getData();
                showSnackBar(bundle.getString(EXTRA_PARAM));

            } else if (msg.what == SEND_BUFFERING) {
                if(msg.getData().getString(EXTRA_PARAM).equals("start")) {
                    setStatus("Buffering");
                } else {
                   setStatus(RadioPlayerService.MP_PLAYING);
                }

            } else if (msg.what == SEND_TITLE) {
                setStatus(msg.getData().getString(EXTRA_PARAM));

            } else if (msg.what == SEND_BITRATE) {
                playingBitrate = Integer.valueOf(msg.getData().getString(EXTRA_PARAM));

            } else if (msg.what == SEND_TIME) {
                Log.d(LOG_TAG, "Received time: " + msg.getData().getString(EXTRA_PARAM));
                playingSecs = Integer.parseInt(msg.getData().getString(EXTRA_PARAM));
            }
        }
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            toServiceMessenger = new Messenger(service);
            fromServiceMessenger = new Messenger(new IncomingHandler());
            bound = true;
            Log.d(LOG_TAG, "Connected to service: " + radioStation.getRadioStationName());

            //Register to service for messages
            try {
                Message msg = Message.obtain(null,
                        REGISTER);
                msg.replyTo = fromServiceMessenger;
                toServiceMessenger.send(msg);

            } catch (RemoteException e) {
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;

        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get RadioStation object from fragments arguments */
        radioStation = (RadioStation) getArguments().getSerializable(RADIO_STATION_OBJECT);

    }

    @Override
    public void onResume() {
        super.onResume();

        //Bind to service
        Intent intent = new Intent(getActivity(), RadioPlayerService.class);
        getActivity().bindService(intent, serviceConnection, 0);
        bound = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_radio_player, container, false);

        TextView radioStationName = (TextView) v.findViewById(R.id.radio_name);
        TextView radioStationLocation = (TextView) v.findViewById(R.id.radio_location);
        TextView radioStationUrl = (TextView) v.findViewById(R.id.radio_url);
       // statusTextView = (TextView) v.findViewById(R.id.text_status);
        favoriteCheckBox = (CheckBox) v.findViewById(R.id.station_favorite);
       // playingTime = (TextView) v.findViewById(R.id.playing_time);
        mWaveLoadingView = (WaveLoadingView) v.findViewById(R.id.waveLoadingView);

        Button buttonPlay = (Button)v.findViewById(R.id.button_play);
        Button buttonPause = (Button)v.findViewById(R.id.button_pause);
        Button buttonStop = (Button)v.findViewById(R.id.button_stop);

        //Run the playing timer handler
        runTimer();
        animateWaveLoading();

        //Listener for play button
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendAction(RadioPlayerFragment.ACTION_INIT);
                sendAction(RadioPlayerFragment.ACTION_START);


            }
        });

        //Listener for pause button
        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendAction(RadioPlayerFragment.ACTION_PAUSE);

            }
        });


        //Listener for stop button
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAction(ACTION_STOP);
            }
        });

        //Listener for check box
        favoriteCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (favoriteCheckBox.isChecked()) {
                    // Set favorite
                    radioStation.setFavorite(true);
                    // Update database
                    RadioStationLab.updateFavoriteRadioStation(radioStation, true);
                } else {
                    radioStation.setFavorite(false);
                    // Update database
                    RadioStationLab.updateFavoriteRadioStation(radioStation, false);
                }

            }
        });

        // Set radio station name
        radioStationName.setText(radioStation.getRadioStationName());

        // Set radio station location
        radioStationLocation.setText(radioStation.getRadioStationLocation());

        // Set radio station url
        radioStationUrl.setText(radioStation.getRadioStationUrl());

        //Set favorite check box
        favoriteCheckBox.setChecked(radioStation.getFavorite());



        return v;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        Log.d(LOG_TAG, "setUserVisibleHint");

        if (isVisibleToUser){
            //Bind to service
            Intent intent = new Intent(getActivity(), RadioPlayerService.class);
            getActivity().bindService(intent, serviceConnection, 0);
        } else {
            if (bound) {
                getActivity().unbindService(serviceConnection);
                bound = false;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");

        if (bound) {
            getActivity().unbindService(serviceConnection);
            bound = false;
        }
    }

    //Changing status
    protected void setStatus(String statusText) {

            String status;

            switch (statusText) {
                case RadioPlayerService.MP_CONNECTING:
                    status = "Connecting...";
                    break;
                case RadioPlayerService.MP_ERROR:
                    status = "Error";
                    break;
                case RadioPlayerService.MP_INIT:
                    status = "Initialized";
                    break;
                case RadioPlayerService.MP_READY:
                    status = "Ready";
                    break;
                case RadioPlayerService.MP_NOT_READY:
                    status = "Idle";
                    break;
                case RadioPlayerService.MP_PLAYING:
                    status = "Playing...";
                    break;
                case RadioPlayerService.MP_STOPPED:
                    status = "Stopped";
                    break;
                case RadioPlayerService.MP_PAUSED:
                    status = "Paused";
                    break;
                case "Buffering":
                    status = "Buffering...";
                    break;
                default:
                    status = "Playing...";
                    setSongTitle(statusText);

            }
            mWaveLoadingView.setTopTitle(status);
    }

    /**
     * Show message via snack bar to the user
     * @param msg
     */
    private void showSnackBar(String msg) {

        Log.d(LOG_TAG, "showSnackBar");

        Snackbar snackbar = Snackbar
                .make(getView(), msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }


    /**
     * Send action to service thread via messenger
     * @param action
     */
    private void sendAction(String action) {

        if (action.equals(RadioPlayerFragment.ACTION_INIT)) {
            try {
                Message msg = Message.obtain(null, RadioPlayerFragment.SEND_ACTION);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.ACTION, action);
                bundle.putSerializable(RadioPlayerFragment.RADIO_STATION_OBJECT, radioStation);
                msg.setData(bundle);
                toServiceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send action: " + action);
            }

        } else {

            try {
                Message msg = Message.obtain(null, RadioPlayerFragment.SEND_ACTION);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.ACTION, action);
                msg.setData(bundle);
                toServiceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send action: " + action);
            }

        }
    }

    /**
     * Timer for displaying playing time
     */
    private void runTimer() {

        // Handler
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                int hour = playingSecs / 3600;
                int min = (playingSecs % 3600) / 60;
                int res_sec = playingSecs % 60;

                String time = String.format("%02d:%02d:%02d", hour, min, res_sec);
                if (playingBitrate != 0) {
                    mWaveLoadingView.setBottomTitle(time + " \n" + playingBitrate/1000 + " kb/s");
                } else {
                    mWaveLoadingView.setBottomTitle(time);
                }
                //playingTime.setText(time);

                if (playingTimeRunning) {
                    playingSecs++;
                }

                handler.postDelayed(this, 1000);
            }
        });

    }

    private void animateWaveLoading() {

        // Handler
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {

                if (playingTimeRunning) {

                    Random generator = new Random();
                    int i = generator.nextInt(10) + 1;

                    mWaveLoadingView.setAmplitudeRatio(60 + i);
                    mWaveLoadingView.setProgressValue(60 + i);

                }

                handler.postDelayed(this, 1000);
            }
        });

    }


    private void startVisualizer(){

        new Thread(new Runnable() {
            @Override
            public void run() {

                BufferedInputStream in = null;

                try {
                    URL url = new URL(radioStation.getListenUrl());

                    InputStream inputStream = url.openStream();
                    in = new BufferedInputStream(inputStream);

                    byte[] data = new byte[100];

                    while ((in.read(data)) != -1) {
                        //horizon.updateView(data);

                    }


                }catch (IOException e) {
                    Log.d(LOG_TAG, "Exception");
                }

            }
        }).start();

    }


    private void setSongTitle(String songTitle) {

        mWaveLoadingView.setCenterTitle(songTitle);

    }

}
