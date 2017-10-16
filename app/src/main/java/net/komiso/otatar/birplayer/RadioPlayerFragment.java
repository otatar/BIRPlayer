package net.komiso.otatar.birplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.BoringLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.komiso.otatar.biplayer.R;

import java.util.ArrayList;


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
    public static final String ACTION_INIT_AND_START = "initAndStart";
    public static final String ACTION_START = "start";
    public static final String ACTION_REC_START = "rec_start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_PAUSE_REC = "pause_rec";
    public static final int SEND_COMPLETION = 9;
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
    public static final String RADIO_STATION_OBJECT_LIST = "radio_station_object_list";

    public static final String RADIO_LOCAL_URL = "http://10.0.2.2:8080";

    /* Radio Station object */
    private RadioStation radioStation;

    /* Radio Station List and position of radio station*/
    private static ArrayList<RadioStation> radioStationList = new ArrayList<>();
    private static int radioStationPosition;

    /* If we are bound to service */
    private Boolean bound = false;

    //If we are bound to recording service
    private Boolean boundRecording = false;

    //Messenger
    private Messenger fromServiceMessenger;

    //Reference to service messenger
    private Messenger toServiceMessenger;

    //Reference to recording service messenger
    private Messenger recordingServiceMessenger;

    //Reference to local recording service messenger
    private Messenger localRecordingServiceMessenger;

    /* Reference to radio station name TextView */
    private TextView radioStationName;

    /* Reference to radio station location */
    private TextView radioStationLocation;

    /* Reference to radio station genre */
    private TextView radioStationGenre;

    /* Reference to radio station url */
    private TextView radioStationUrl;

    /* Reference to play/pause button */
    private ImageView btnPlay;

    /* Reference to forward button */
    private ImageButton btnForward;

    /* Reference to backward button */
    private ImageButton btnBackward;

    /* Reference to record button */
    private ImageView btnRecord;

    /* Reference to image button */
    private ImageButton btnShare;

    /* Reference to status TextView */
    private TextView statusTextView;

    /* Reference to current playing time */
    private TextView playTime;

    /* Reference to current bit rate */
    private TextView bitRate;

    /* Reference to favorite check box */
    private CheckBox favoriteCheckBox;

    /* Is paying time running */
    private boolean playingTimeRunning;

    /* Num of sec since playing */
    private int playingSecs;

    private int playingBitrate = 0;

    /* Container Activity */
    private Main2Activity containerActivity;

    /* Radio Station selected listener */
    private OnRadioStationChangedListener radioStationChangedListener;

    /**
     * Interface implemented by container activity
     */
    public interface OnRadioStationChangedListener {
        void onRadioStationChanged(int position);
    }


    public RadioPlayerFragment() {
        // Required empty public constructor
    }


    /**
     * Method to instantiate and initialize new RadioPlayerFragment object
     *
     * @param position         Position of the radio station to be played in the list
     * @param radioStationList List of radio stations
     * @return new RadioPlayerFragment object
     */
    public static RadioPlayerFragment newInstance(int position, ArrayList<RadioStation> radioStationList) {

        Bundle bundle = new Bundle();
        bundle.putInt(RADIO_STATION_OBJECT, position);
        bundle.putSerializable(RADIO_STATION_OBJECT_LIST, radioStationList);

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

            if (msg.what == UPDATE_STATUS) {
                Bundle bundle = msg.getData();
                setStatus(bundle.getString(STATUS));

                if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PLAYING)) {
                    playingTimeRunning = true;
                    if (!btnPlay.isActivated()) {
                        btnPlay.setActivated(true);
                    }

                    //Activate the recording button
                    int MyVersion = Build.VERSION.SDK_INT;
                    if (Main2Activity.storagePerm || (MyVersion <= Build.VERSION_CODES.LOLLIPOP_MR1)) {
                        btnRecord.setEnabled(true);
                    }

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PAUSED)) {
                    playingTimeRunning = false;

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_CONNECTING)) {
                    if (!btnPlay.isActivated()) {
                        btnPlay.setActivated(true);
                    }

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_STOPPED)) {

                    playingTimeRunning = false;
                    playingSecs = 0;
                    playingBitrate = 0;

                    //Toggle button state
                    if (btnRecord.isActivated()) {
                        btnRecord.setActivated(false);
                    }
                    btnRecord.setEnabled(false);
                    stopRecording();

                } else {

                    playingTimeRunning = false;
                    playingSecs = 0;
                    playingBitrate = 0;

                }

            } else if (msg.what == SEND_ERROR || msg.what == SEND_ALERT) {
                //Inform user about that
                Bundle bundle = msg.getData();
                showSnackBar(bundle.getString(EXTRA_PARAM));

                //Toggle button state
                btnRecord.setActivated(!btnRecord.isActivated());
                btnRecord.setEnabled(false);
                stopRecording();

            } else if (msg.what == SEND_BUFFERING) {
                if (msg.getData().getString(EXTRA_PARAM).equals("start")) {
                    setStatus("Buffering");
                } else {
                    setStatus(RadioPlayerService.MP_PLAYING);
                }

            } else if (msg.what == SEND_TITLE) {
                if (RadioPlayerService.mediaPlayer.isPlaying()) {
                    setStatus(msg.getData().getString(EXTRA_PARAM));
                }

            } else if (msg.what == SEND_BITRATE) {
                playingBitrate = Integer.valueOf(msg.getData().getString(EXTRA_PARAM));

            } else if (msg.what == SEND_TIME) {
                Log.d(LOG_TAG, "Received time: " + msg.getData().getString(EXTRA_PARAM));
                playingSecs = Integer.parseInt(msg.getData().getString(EXTRA_PARAM));

            } else if (msg.what == InternetRadioRecorderService.SEND_STATE) {
                Log.d(LOG_TAG, "Receive SEND_STATE from recording service");
                Bundle bundle = msg.getData();
                if (bundle.getBoolean(InternetRadioRecorderService.BUNDLE_STATE)) {
                    Log.d(LOG_TAG, "Recording service is recording given radio station");
                } else {
                    Log.d(LOG_TAG, "Recording service stopped recording radio station");
                }

            } else if (msg.what == InternetRadioRecorderService.SEND_ERR) {
                Log.d(LOG_TAG, "Receive SEND_ERR msg");
                Bundle bundle = msg.getData();
                Log.d(LOG_TAG, "Error while recording: " + bundle.getString(InternetRadioRecorderService.BUNDLE_MSG));
                showSnackBar(containerActivity.getString(R.string.error_recording));

                //Toggle button state
                btnRecord.setActivated(!btnRecord.isActivated());

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


    private ServiceConnection recordingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            recordingServiceMessenger = new Messenger(service);
            localRecordingServiceMessenger = new Messenger(new IncomingHandler());
            boundRecording = true;
            Log.d(LOG_TAG, "Connected to recording service");

            //Register to service for messages
            try {
                Message msg = Message.obtain(null, InternetRadioRecorderService.REGISTER);
                msg.replyTo = localRecordingServiceMessenger;
                recordingServiceMessenger.send(msg);

            } catch (RemoteException e) {
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(LOG_TAG, "Disconnected from recording service");
            boundRecording = false;

        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get RadioStation object from fragments arguments */
        radioStationPosition = getArguments().getInt(RADIO_STATION_OBJECT);
        radioStationList.clear();
        radioStationList.addAll((ArrayList<RadioStation>) getArguments().getSerializable(RADIO_STATION_OBJECT_LIST));

        radioStation = radioStationList.get(radioStationPosition);

        //Select the station
        deselectAllRadioStations(radioStationList);
        Log.d(LOG_TAG, "Radio Station list size: " + radioStationList.size());
        radioStation.setSelected(true);

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

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        //Digital font
        Typeface myTypeface = Typeface.createFromAsset(getActivity().getAssets(),
                "digital-7.ttf");

        /* Lets connect the parts */
        radioStationName = (TextView) v.findViewById(R.id.radio_name);
        radioStationLocation = (TextView) v.findViewById(R.id.radio_location);
        radioStationGenre = (TextView) v.findViewById(R.id.radio_genre);
        radioStationUrl = (TextView) v.findViewById(R.id.radio_url);
        statusTextView = (TextView) v.findViewById(R.id.text_status);
        favoriteCheckBox = (CheckBox) v.findViewById(R.id.station_favorite);
        playTime = (TextView) v.findViewById(R.id.playing_time);
        playTime.setTypeface(myTypeface);
        bitRate = (TextView) v.findViewById(R.id.bitrate);
        bitRate.setTypeface(myTypeface);

        btnPlay = (ImageView) v.findViewById(R.id.button_play);
        btnForward = (ImageButton) v.findViewById(R.id.button_forward);
        btnBackward = (ImageButton) v.findViewById(R.id.button_backward);
        btnRecord = (ImageView) v.findViewById(R.id.button_record);
        btnShare = (ImageButton) v.findViewById(R.id.button_share);

        //Disable the record button if we didn't get the storage permission and we are not playing
        if (btnRecord != null) {
            btnRecord.setEnabled(false);
        }

        //Run the playing timer handler
        runTimer();

        //Listener for play/pause button
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(LOG_TAG, "isActivated: " + String.valueOf(btnPlay.isActivated()));

                if (!btnPlay.isActivated()) {
                    sendAction(ACTION_INIT);
                    sendAction(ACTION_START);
                } else {
                    sendAction(ACTION_STOP);
                }

                //Toggle state
                btnPlay.setActivated(!btnPlay.isActivated());
            }
        });

        //Listener for forward button
        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Send stop RadioPlayerService
                sendAction(ACTION_STOP);

                //Change load next radio station
                changeRadioStation(0);

                //If play/pause toggle button is active, deactivate it
                if (btnPlay.isActivated()) {
                    btnPlay.setActivated(false);
                }

                //Stop recording
                if (btnRecord.isActivated()) {
                    btnRecord.setActivated(false);
                }
                btnRecord.setEnabled(false);
                stopRecording();
            }
        });

        //Listener for backward button
        btnBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Send stop RadioPlayerService
                sendAction(ACTION_STOP);

                //Change load next radio station
                changeRadioStation(1);

                //If play/pause toggle button is activated, deactivate it
                if (btnPlay.isActivated()) {
                    btnPlay.setActivated(false);
                }

                //Stop recording
                if (btnRecord.isActivated()) {
                    btnRecord.setActivated(false);
                }
                btnRecord.setEnabled(false);
                stopRecording();
            }
        });


        //Listener for record button
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(LOG_TAG, "Clicked on record button");

                if (!btnRecord.isActivated()) {
                    startRecording();
                } else {
                    stopRecording();
                }

                //Toggle button state
                btnRecord.setActivated(!btnRecord.isActivated());

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

        //Listener for share button
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Share!!!
                Intent iIntent = new Intent(Intent.ACTION_SEND);
                iIntent.setType("text/plain");
                iIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_listen, radioStation.getRadioStationName()));
                startActivity(Intent.createChooser(iIntent, getString(R.string.share_birp)));
            }

        });

        //Display radio station info
        updateRadioStationDisplay();

        return v;
    }

    private void startRecording() {
        Log.d(LOG_TAG, "Start recording!!!");

        //Start recording service
        Intent intent = new Intent(getActivity(), InternetRadioRecorderService.class);
        intent.setAction(InternetRadioRecorderService.START_RECORDING);
        Bundle bundle = new Bundle();
        bundle.putSerializable(InternetRadioRecorderService.BUNDLE_RADIO_STATION, radioStation);
        intent.putExtra(InternetRadioRecorderService.BUNDLE_RADIO_STATION, bundle);
        getActivity().startService(intent);

        //Bind to service
        getActivity().bindService(intent, recordingServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void stopRecording() {
        Log.d(LOG_TAG, "Stop recording!!!");

        //Stop Recording Service
        Intent intent = new Intent(getActivity(), InternetRadioRecorderService.class);
        intent.setAction(InternetRadioRecorderService.STOP_RECORDING);
        Bundle bundle = new Bundle();
        bundle.putSerializable(InternetRadioRecorderService.BUNDLE_RADIO_STATION, radioStation);
        intent.putExtra(InternetRadioRecorderService.BUNDLE_RADIO_STATION, bundle);
        getActivity().startService(intent);

        if (boundRecording) {
            getActivity().unbindService(recordingServiceConnection);
            boundRecording = false;
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        Log.d(LOG_TAG, "setUserVisibleHint");

        if (isVisibleToUser) {
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


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a = null;

        if (context instanceof Activity) {
            a = (Activity) context;
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            radioStationChangedListener = (OnRadioStationChangedListener) a;
            containerActivity = (Main2Activity) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }


    //Changing status
    protected void setStatus(String statusText) {

        String status;

        switch (statusText) {
            case RadioPlayerService.MP_CONNECTING:
                status = containerActivity.getString(R.string.connecting_string);
                break;
            case RadioPlayerService.MP_ERROR:
                status = containerActivity.getString(R.string.error_string);
                btnPlay.setActivated(false);
                break;
            case RadioPlayerService.MP_INIT:
                status = "";
                break;
            case RadioPlayerService.MP_READY:
                status = "";
                break;
            case RadioPlayerService.MP_NOT_READY:
                status = "";
                break;
            case RadioPlayerService.MP_PLAYING:
                status = containerActivity.getString(R.string.playing_string);
                break;
            case RadioPlayerService.MP_STOPPED:
                status = containerActivity.getString(R.string.stopped_string);
                break;
            case RadioPlayerService.MP_PAUSED:
                status = containerActivity.getString(R.string.paused_string);
                break;
            case "Buffering":
                status = containerActivity.getString(R.string.buffering_string);
                break;
            default:
                status = statusText;
        }

        statusTextView.setText(RadioPlayerFragment.capitalizeString(status));
    }

    /**
     * Show message via snack bar to the user
     *
     * @param msg Message to show
     */
    private void showSnackBar(String msg) {

        Log.d(LOG_TAG, "showSnackBar: " + msg);

        Snackbar snackbar = Snackbar
                .make(getView(), msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }


    /**
     * Send action to service thread via messenger
     *
     * @param action Action string
     */
    private void sendAction(String action) {

        Log.d(LOG_TAG, "sendAction");

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
                    playTime.setText(time);
                    bitRate.setText(playingBitrate / 1000 + " kb/s");
                } else {
                    playTime.setText(time);
                    bitRate.setText("0 kb/s");
                }


                if (playingTimeRunning) {
                    playingSecs++;
                }

                handler.postDelayed(this, 1000);
            }
        });

    }


    /**
     * Function to change radio station in given direction
     *
     * @param direction 0 forward, 1 backward
     */
    private void changeRadioStation(int direction) {

        Log.d(LOG_TAG, "changeRadioStation");

        //Deselect previous station
        radioStation.setSelected(false);

        if (direction == 0) {
            //Forward
            if (radioStationPosition == radioStationList.size() - 1) {
                radioStationPosition = 0;
                radioStation = radioStationList.get(radioStationPosition);
            } else {
                radioStationPosition++;
                radioStation = radioStationList.get(radioStationPosition);
            }

        } else if (direction == 1) {
            //Backward
            if (radioStationPosition == 0) {
                radioStationPosition = radioStationList.size() - 1;
                radioStation = radioStationList.get(radioStationPosition);
            } else {
                radioStationPosition--;
                radioStation = radioStationList.get(radioStationPosition);
            }
        }

        //Select new station
        radioStation.setSelected(true);

        //Notice about that
        containerActivity.onRadioStationChanged(radioStationPosition);

        //Update radio station display
        updateRadioStationDisplay();

    }

    /**
     * Updates radio station display views
     */
    private void updateRadioStationDisplay() {

        Log.d(LOG_TAG, "updateRadioStationDisplay");

        // Set radio station name
        radioStationName.setText(radioStation.getRadioStationName());
        Shader shader = new LinearGradient(0, 0, 0, radioStationName.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        radioStationName.getPaint().setShader(shader);

        // Set radio station location
        radioStationLocation.setText(radioStation.getRadioStationLocation());
        shader = new LinearGradient(0, 0, 0, radioStationLocation.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        radioStationLocation.getPaint().setShader(shader);

        // Set radio station url
        shader = new LinearGradient(0, 0, 0, radioStationUrl.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        radioStationUrl.setText(radioStation.getRadioStationUrl());
        radioStationUrl.getPaint().setShader(shader);

        //Set radio station genre
        shader = new LinearGradient(0, 0, 0, radioStationUrl.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        radioStationGenre.setText(radioStation.getRadioStationGenre());
        radioStationGenre.getPaint().setShader(shader);

        //Set favorite check box
        favoriteCheckBox.setChecked(radioStation.getFavorite());
    }

    /**
     * Deselects all radio stations
     *
     * @param list List of radio stations
     */
    public static void deselectAllRadioStations(ArrayList<RadioStation> list) {

        Log.d(LOG_TAG, "deselectAllRadioStations");

        for (RadioStation station : list) {
            if (station.getSelected()) {
                station.setSelected(false);
                // station.setPlaying(false);
            }
        }
    }

    /**
     * Stops all radio stations
     */
    public static void stopAllRadioStations(ArrayList<RadioStation> list) {

        Log.d(LOG_TAG, "stopAllRadioStations");

        for (RadioStation station : list) {
            if (station.getPlaying()) {
                station.setPlaying(false);
            }

            if (station.getConnecting()) {
                station.setConnecting(false);
            }
        }
    }


    /**
     * Capitalize first char of String
     *
     * @param string String to capitalize
     * @return Capitalized string
     */
    @Nullable
    public static String capitalizeString(String string) {

        Log.d(LOG_TAG, "capitalizeString");

        String retString = "";

        if (string == null) {
            return null;
        } else {

            String[] words = string.split(" ");
            if (words.length < 2) {
                return string;
            } else {
                for (String word : words) {
                    char[] chars = word.toLowerCase().toCharArray();
                    if (Character.isLetter(chars[0])) {
                        chars[0] = Character.toUpperCase(chars[0]);
                    }

                    retString += String.valueOf(chars);
                    retString += " ";
                }
            }

        }

        return retString;
    }


}
