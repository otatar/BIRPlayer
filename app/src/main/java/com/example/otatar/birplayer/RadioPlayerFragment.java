package com.example.otatar.birplayer;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pheelicks.visualizer.VisualizerView;
import com.pheelicks.visualizer.renderer.BarGraphRenderer;

import org.w3c.dom.Text;

import es.claucookie.miniequalizerlibrary.EqualizerView;


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

    /* Reference to progress_bar*/
    private ProgressBar progressBar;

    /* Reference to visualizer view */
    private com.pheelicks.visualizer.VisualizerView visualizerView;



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

        ProgressBar progressBar;

        IncomingHandler(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == UPDATE_STATUS)  {
                Bundle bundle = msg.getData();
                setStatusField(bundle.getString(STATUS));

            } else if (msg.what == SEND_ERROR || msg.what == SEND_ALERT) {
                //Inform user about that
                Bundle bundle = msg.getData();
                showSnackBar(bundle.getString(EXTRA_PARAM));

            } else if (msg.what == SEND_BUFFERING) {
                if(msg.getData().getString(EXTRA_PARAM).equals("start")) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }

            } else if (msg.what == SEND_TITLE) {
                setStatusField(msg.getData().getString(EXTRA_PARAM));
            }
        }
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            toServiceMessenger = new Messenger(service);
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

        fromServiceMessenger = new Messenger(new IncomingHandler(progressBar));

        /* Get RadioStaion object from fragments arguments */
        radioStation = (RadioStation) getArguments().getSerializable(RADIO_STATION_OBJECT);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_radio_player, container, false);

        TextView radioStationName = (TextView) v.findViewById(R.id.radio_name);
        TextView radioStationLocation = (TextView) v.findViewById(R.id.radio_location);
        TextView radioStationUrl = (TextView) v.findViewById(R.id.radio_url);
        statusTextView = (TextView) v.findViewById(R.id.text_status);
        favoriteCheckBox = (CheckBox) v.findViewById(R.id.station_favorite);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        visualizerView = (VisualizerView) v.findViewById(R.id.visualizer_view);

        Button buttonPlay = (Button)v.findViewById(R.id.button_play);
        Button buttonPause = (Button)v.findViewById(R.id.button_pause);
        Button buttonStop = (Button)v.findViewById(R.id.button_stop);


        // Incoming messenger
        fromServiceMessenger = new Messenger(new IncomingHandler(progressBar));

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
    public void onDestroyView() {
        super.onDestroyView();
        if (bound) {
            getActivity().unbindService(serviceConnection);
            bound = false;
        }
    }

    //Changing status
    protected void setStatusField(String statusText) {

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
                    visualizerView.link(RadioPlayerService.getMediaPlayer());

                    Paint paint = new Paint();
                    paint.setStrokeWidth(50f);
                    paint.setAntiAlias(true);
                    paint.setColor(Color.argb(200, 56, 138, 252));
                    BarGraphRenderer barGraphRendererBottom = new BarGraphRenderer(16, paint, false);
                    visualizerView.addRenderer(barGraphRendererBottom);

                    break;
                case RadioPlayerService.MP_STOPPED:
                    status = "Stopped";
                    visualizerView.release();
                    progressBar.setVisibility(View.GONE);
                    break;
                case RadioPlayerService.MP_PAUSED:
                    status = "Paused";
                    break;
                default:
                    status = statusText;
            }
            statusTextView.setText(status);
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

    public static String sec2min(int sec) {

        int hour = sec / 3600;
        int min = (sec % 3600) / 60;
        int res_sec = (sec % 3600) % 60;

        return String.format("%02d", hour) + ":" + String.format("%02d", min) + ":" + String.format("%02d", res_sec);

    }

}
