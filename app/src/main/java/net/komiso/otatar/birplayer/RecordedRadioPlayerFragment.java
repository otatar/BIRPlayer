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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import android.widget.SeekBar;
import android.widget.TextView;

import net.komiso.otatar.biplayer.R;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 */
public class RecordedRadioPlayerFragment extends Fragment {

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
    public static final String ACTION_SEND_PROGRESS = "action_send_progress";
    public static final int SEND_PROGRESS = 10;
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

    public static final String LOG_TAG = "RecRadioPlayerFragment";
    public static final String RECORDED_RADIO_STATION_OBJECT = "recorded_radio_station_object";
    private static final String RECORDED_RADIO_STATION_OBJECT_LIST = "recorded_radio_station_object_list";


    /* Radio Station List and position of radio station*/
    private static ArrayList<File> recordedRadioStationList = new ArrayList<>();
    private static int recordedRadioStationPosition;

    /* If we are bound to service */
    private Boolean bound = false;

    //If we are bound to recording service
    private Boolean boundRecording = false;

    //Messenger
    private Messenger fromServiceMessenger;

    //Reference to service messenger
    private Messenger toServiceMessenger;

    /* Reference to radio station name TextView */
    private TextView recordingName;

    /* Reference to radio station location */
    private TextView recordingDate;

    /* Reference to radio station genre */
    private TextView recordingInfo;

    /* Reference to current Time */
    private TextView currentTime;

    /* Reference to end time */
    private TextView endTime;

    /* Reference to seektime */
    private SeekBar seekBar;

    private String recordedRadioStation;

    /* Reference to play/pause button */
    private ImageView btnPlay;

    /* Reference to forward button */
    private ImageButton btnForward;

    /* Reference to backward button */
    private ImageButton btnBackward;

    /* Reference to share button */
    private ImageButton btnShare;

    /* Is paying time running */
    private boolean playingTimeRunning;

    /* Num of sec since playing */
    private int playingSecs;

    /* Container Activity */
    private Main2Activity containerActivity;

    private static int seekBarProgress;

    /* Radio Station selected listener */
    private OnRadioStationChangedListener radioStationChangedListener;

    /**
     * Interface implemented by container activity
     */
    public interface OnRadioStationChangedListener {
        void onRadioStationChanged(int position);
    }


    public RecordedRadioPlayerFragment() {
        // Required empty public constructor
    }


    /**
     * Method to instantiate and initialize new RecordedRadioPlayerFragment object
     *
     * @param position         Position of the radio station to be played in the list
     * @param recordedRadioStationList List of radio stations
     * @return new RecordedRadioPlayerFragment object
     */
    public static RecordedRadioPlayerFragment newInstance(int position, ArrayList<File> recordedRadioStationList) {

        Bundle bundle = new Bundle();
        bundle.putInt(RECORDED_RADIO_STATION_OBJECT, position);
        bundle.putSerializable(RECORDED_RADIO_STATION_OBJECT_LIST, recordedRadioStationList);

        RecordedRadioPlayerFragment recordedRadioPlayerFragment = new RecordedRadioPlayerFragment();
        recordedRadioPlayerFragment.setArguments(bundle);

        return recordedRadioPlayerFragment;
    }


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG_TAG, "RecordedRadioPlayerFragment:handleMessage()");

            if (msg.what == UPDATE_STATUS) {
                Bundle bundle = msg.getData();

                if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PLAYING)) {
                    playingTimeRunning = true;
                    if (!btnPlay.isActivated()) {
                        btnPlay.setActivated(true);
                    }

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_PAUSED)) {
                    playingTimeRunning = false;

                } else if (bundle.getString(STATUS).equals(RadioPlayerService.MP_STOPPED)) {

                    playingTimeRunning = false;
                    playingSecs = 0;

                } else {

                    playingTimeRunning = false;
                    playingSecs = 0;
                }

            } else if (msg.what == SEND_COMPLETION) {

                //End playing recording
                playingTimeRunning = false;
                playingSecs = 0;

                //Toggle state
                btnPlay.setActivated(!btnPlay.isActivated());

            } else if (msg.what == SEND_ERROR || msg.what == SEND_ALERT) {
                //Inform user about that
                Bundle bundle = msg.getData();
                showSnackBar(bundle.getString(EXTRA_PARAM));

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
                playingTimeRunning = false;
                Bundle bundle = msg.getData();
                Log.d(LOG_TAG, "Error while recording: " + bundle.getString(InternetRadioRecorderService.BUNDLE_MSG));
                showSnackBar(containerActivity.getString(R.string.error_recording));

            }
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            toServiceMessenger = new Messenger(service);
            fromServiceMessenger = new Messenger(new IncomingHandler());
            bound = true;
            Log.d(LOG_TAG, "Connected to service");

            //Register to service for messages
            try {
                Message msg = Message.obtain(null,
                        RadioPlayerFragment.REGISTER);
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
        recordedRadioStationPosition = getArguments().getInt(RECORDED_RADIO_STATION_OBJECT);
        recordedRadioStationList.clear();
        recordedRadioStationList.addAll((ArrayList<File>) getArguments().getSerializable(RECORDED_RADIO_STATION_OBJECT_LIST));

        recordedRadioStation = recordedRadioStationList.get(recordedRadioStationPosition).getAbsolutePath();

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
        View v = inflater.inflate(R.layout.fragment_recorded_radio_player, container, false);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        //Digital font
        Typeface myTypeface = Typeface.createFromAsset(getActivity().getAssets(),
                "digital-7.ttf");

        /* Lets connect the parts */
        recordingName = (TextView) v.findViewById(R.id.recording_name);
        recordingDate = (TextView) v.findViewById(R.id.recording_date);
        recordingInfo = (TextView) v.findViewById(R.id.recording_info);
        currentTime = (TextView) v.findViewById(R.id.current_time);
        currentTime.setTypeface(myTypeface);
        endTime = (TextView) v.findViewById(R.id.end_time);
        endTime.setTypeface(myTypeface);
        seekBar = (SeekBar) v.findViewById(R.id.seek_bar);
        btnPlay = (ImageView) v.findViewById(R.id.button_play);
        btnForward = (ImageButton) v.findViewById(R.id.button_forward);
        btnBackward = (ImageButton) v.findViewById(R.id.button_backward);
        btnShare = (ImageButton) v.findViewById(R.id.button_share);

        runSeekBarTimer();

        //Listener for play/pause button
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(LOG_TAG, "isActivated: " + String.valueOf(btnPlay.isActivated()));

                if (!btnPlay.isActivated()) {
                    sendAction(ACTION_REC_START);
                } else {
                    sendAction(ACTION_PAUSE_REC);
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
                changeRecordedRadioStation(0);

                //If play/pause toggle button is active, deactivate it
                if (btnPlay.isActivated()) {
                    btnPlay.setActivated(false);
                }

            }
        });

        //Listener for backward button
        btnBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Send stop RadioPlayerService
                sendAction(ACTION_STOP);

                //Change load next radio station
                changeRecordedRadioStation(1);

                //If play/pause toggle button is activated, deactivate it
                if (btnPlay.isActivated()) {
                    btnPlay.setActivated(false);
                }

            }
        });

        //Listener for seekbar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser) {

                    RecordedRadioPlayerFragment.seekBarProgress = progress;
                    sendAction(RecordedRadioPlayerFragment.ACTION_SEND_PROGRESS);

                    if (playingTimeRunning) {
                        playingSecs = progress;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Listener for share button
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Share!!!
                Uri recordeFile = Uri.parse(recordedRadioStation);
                Intent iIntent = new Intent(Intent.ACTION_SEND);
                iIntent.setType("audio/mpeg");
                iIntent.putExtra(Intent.EXTRA_STREAM, recordeFile);
                startActivity(Intent.createChooser(iIntent, getString(R.string.share_birp)));
            }

        });


        //Display radio station info
        updateRecordedRadioStationDisplay();

        return v;
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");

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

        if (action.equals(RadioPlayerFragment.ACTION_REC_START)) {
            try {
                Message msg = Message.obtain(null, RadioPlayerFragment.SEND_ACTION);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.ACTION, action);
                bundle.putSerializable(Main2Activity.REC_FILEPATH, recordedRadioStation);
                msg.setData(bundle);
                toServiceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send action: " + action);
            }

        }else if (action.equals(RecordedRadioPlayerFragment.ACTION_SEND_PROGRESS)) {
            try {
                Message msg = Message.obtain(null, RecordedRadioPlayerFragment.SEND_PROGRESS);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.ACTION, action);
                bundle.putInt(RecordedRadioPlayerFragment.ACTION_SEND_PROGRESS, seekBarProgress);
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
     * Function to change radio station in given direction
     *
     * @param direction 0 forward, 1 backward
     */
    private void changeRecordedRadioStation(int direction) {

        Log.d(LOG_TAG, "changeRecordedRadioStation");


        if (direction == 0) {
            //Forward
            if (recordedRadioStationPosition == recordedRadioStationList.size() - 1) {
                recordedRadioStationPosition = 0;
                recordedRadioStation = recordedRadioStationList.get(recordedRadioStationPosition).getAbsolutePath();
            } else {
                recordedRadioStationPosition++;
                recordedRadioStation = recordedRadioStationList.get(recordedRadioStationPosition).getAbsolutePath();
            }

        } else if (direction == 1) {
            //Backward
            if (recordedRadioStationPosition == 0) {
                recordedRadioStationPosition = recordedRadioStationList.size() - 1;
                recordedRadioStation = recordedRadioStationList.get(recordedRadioStationPosition).getAbsolutePath();
            } else {
                recordedRadioStationPosition--;
                recordedRadioStation = recordedRadioStationList.get(recordedRadioStationPosition).getAbsolutePath();
            }
        }


        //Update radio station display
        updateRecordedRadioStationDisplay();

    }

    /**
     * Updates radio station display views
     */
    private void updateRecordedRadioStationDisplay() {

        Log.d(LOG_TAG, "updateRecordedRadioStationDisplay");

        // Set radio station name
        recordingName.setText(recordedRadioStation.substring(recordedRadioStation.lastIndexOf("/") + 1));
        Shader shader = new LinearGradient(0, 0, 0, recordingName.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        recordingName.getPaint().setShader(shader);

        // Set radio station date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        File file = new File(recordedRadioStation);

        recordingDate.setText(sdf.format(file.lastModified()));
        shader = new LinearGradient(0, 0, 0, recordingDate.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        recordingDate.getPaint().setShader(shader);

        // Set radio station info
        //Lets compute the duration of mp3
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(recordedRadioStation);
        String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String bitrate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        long dur = Long.parseLong(duration) / 1000;
        long hour = dur / 3600;
        long min = (dur % 3600) / 60;
        long res_sec = dur % 60;
        String time = String.format("%02d:%02d:%02d", hour, min, res_sec);

        recordingInfo.setText(bitrate + " bps, " + (recordedRadioStation.substring(recordedRadioStation.length() - 3)));
        shader = new LinearGradient(0, 0, 0, recordingInfo.getTextSize(), R.color.yellow, Color.BLUE,
                Shader.TileMode.CLAMP);
        recordingInfo.getPaint().setShader(shader);

        //Set current time
        currentTime.setText("00:00:00");

        //Set end time
        endTime.setText(time);
        Log.d(LOG_TAG, "time: " + endTime.getText());

        //Set seekbar
        seekBar.setMax((int)dur);
        seekBar.setProgress(0);

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


    /**
     * Timer for seekbar updating
     */
    private void runSeekBarTimer() {

        // Handler
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                int hour = playingSecs / 3600;
                int min = (playingSecs % 3600) / 60;
                int res_sec = playingSecs % 60;

                String time = String.format("%02d:%02d:%02d", hour, min, res_sec);
                currentTime.setText(time);

                //Update seekbar
                seekBar.setProgress(0);
                seekBar.setProgress(playingSecs);

                if (playingTimeRunning) {
                    playingSecs++;
                }

                handler.postDelayed(this, 1000);
            }
        });

    }


}
