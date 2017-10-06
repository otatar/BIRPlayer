package net.komiso.otatar.birplayer;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import net.komiso.otatar.biplayer.R;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 *
 */
public class RadioPlayerService extends Service {

    /**
     * State of the service
     */
    public static boolean isRunning = false;

    /**
     * Service in foreground?
     */
    public static boolean isForeground = false;

    /**
     * Are we bound?
     */
    private boolean isBound = false;

    /**
     * Radio Station that is being played
     */
    private RadioStation radioStation;

    /**
     * File path that is being played
     */
    private String recFilePath;

    /**
     * Reference to WiFi lock
     */
    private WifiManager.WifiLock wiFiLock;

    /**
     * Reference to audio manager
     */
    private AudioManager audioManager;

    /*
    * MediaPlayer Object
    */
    public static MediaPlayer mediaPlayer;

    /**
     * LOG_TAG string
     */
    public static final String LOG_TAG = "RadioPlayerServiceLog";

    /**
     * ID used from notification service
     */
    public static final int NOTIFICATION_ID = 333;

    /**
     * Timer used for retrieving metadata from shoutcast/icecast server
     */
    public Timer retrieveTimer;

    private Messenger fragmentMessenger = null;
    private Messenger messenger = new Messenger(new IncomingHandler());

    /**
     * Constants for service class
     */
    private static final String START_SERVICE = "com.example.otatar.birplayer.radioplayerservice.start_service";

    /**
     * Constants to track media player state
     */
    public static final String MP_PLAYING = "com.example.otatar.birplayer.radioplayerservice.playing";
    public static final String MP_CONNECTING = "com.example.otatar.birplayer.radioplayerservice.connecting";
    public static final String MP_STOPPED = "com.example.otatar.birplayer.radioplayerservice.stopped";
    public static final String MP_PAUSED = "com.example.otatar.birplayer.radioplayerservice.paused";
    public static final String MP_READY = "com.example.otatar.birplayer.radioplayerservice.ready";
    public static final String MP_ERROR = "com.example.otatar.birplayer.radioplayerservice.error";
    public static final String MP_NOT_READY = "com.example.otatar.birplayer.radioplayerservice.notready";
    public static final String MP_LOST_CONNECTION = "com.example.otatar.birplayer.radioplayerservice.lostconnection";
    public static final String MP_INIT = "com.example.otatar.birplayer.radioplayerservice.init";

    /**
     * Constant for intents extra
     */
    public static final String NOTIFY_RADIO_STATION = "com.example.otatar.birplayer.NOTIFY_RADIO_STATION";

    /**
     * Variable to track MediaPlayer state
     */
    private String mpState = RadioPlayerService.MP_NOT_READY;

    // Static variable to hold reference to self (singleton)
    public static RadioPlayerService radioPlayerService;

    /* Is paying time running */
    private boolean playingTimeRunning;

    /* Num of sec since playing */
    private int playingSecs;

    /*BitRate*/
    private String bitRate;


    //Constructor (private - singleton)
    public RadioPlayerService() {

        this.mediaPlayer = new MediaPlayer();

    }

    /**
     * Returns signle instance of RadioPlayerService class (singleton)
     *
     * @return RadioPlayer service object (singleton)
     */
    public static RadioPlayerService getRadioPlayerService() {

        if (radioPlayerService == null) {
            radioPlayerService = new RadioPlayerService();
        }

        return radioPlayerService;

    }

    /**
     * Handler for receiving messages from RadioPlayer fragment
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG_TAG, "Received message: " + msg.what);

            if (msg.what == RadioPlayerFragment.REGISTER) {
                fragmentMessenger = msg.replyTo;

                if (isRunning) {
                    //Send media player state to UI
                    sendStatus();
                    //Send playing time
                    sendAlert(RadioPlayerFragment.SEND_TIME, String.valueOf(playingSecs));
                    //Send bitrate
                    if (bitRate != null) {
                        sendAlert(RadioPlayerFragment.SEND_BITRATE, bitRate);
                    }

                }

            } else if (msg.what == RadioPlayerFragment.SEND_ACTION) {
                Bundle bundle = msg.getData();
                switch (bundle.getString(RadioPlayerFragment.ACTION)) {
                    case RadioPlayerFragment.ACTION_INIT:
                        /* Receive RadioStation object */
                        radioStation = (RadioStation) bundle.getSerializable(RadioPlayerFragment.RADIO_STATION_OBJECT);
                        initMediaPlayer(radioStation.getListenUrl());
                        break;
                    case RadioPlayerFragment.ACTION_START:
                        startMediaPlayer();
                        break;
                    case RadioPlayerFragment.ACTION_PAUSE:
                        pauseMediaPlayer(false);
                        break;
                    case RadioPlayerFragment.ACTION_STOP:
                        stopMediaPlayer();
                        break;
                    case RadioPlayerFragment.ACTION_REC_START:
                        recFilePath = bundle.getString(Main2Activity.REC_FILEPATH);
                        Log.d(LOG_TAG, "Received recording: " + recFilePath);
                        startRecMediaPlayer(recFilePath);
                        break;
                    case RadioPlayerFragment.ACTION_PAUSE_REC:
                        pauseMediaPlayer(true);
                        break;

                }

            } else if (msg.what == RecordedRadioPlayerFragment.SEND_PROGRESS) {

                Bundle bundle = msg.getData();
                Log.d(LOG_TAG, "Seek bar progress: " + bundle.getInt(RecordedRadioPlayerFragment.ACTION_SEND_PROGRESS) + "s");
                mediaPlayer.seekTo(bundle.getInt(RecordedRadioPlayerFragment.ACTION_SEND_PROGRESS) * 1000);
            }
        }
    }


    /**
     * Returns new intent that starts service
     *
     * @param context Context object
     * @return intent that starts service
     */
    public static Intent newStartIntent(Context context) {

        Intent intent = new Intent(context, RadioPlayerService.class);
        intent.setAction(START_SERVICE);

        return intent;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {

            Log.d(LOG_TAG, "Received intent: " + intent.getAction());

            if (intent.getAction().equals(RadioPlayerService.START_SERVICE)) {

                Log.d(LOG_TAG, "Starting service...");
                isRunning = true;


            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_START)) {

                startMediaPlayer();

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_PAUSE)) {

                pauseMediaPlayer(false);

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_PAUSE_REC)) {

                pauseMediaPlayer(true);

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_STOP)) {

                stopMediaPlayer();

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_INIT_AND_START)) {

                Bundle bundle = intent.getExtras();
                radioStation = (RadioStation) bundle.getSerializable(Main2Activity.SELECTED_RADIO_STATION);
                initMediaPlayer(radioStation.getListenUrl());
                startMediaPlayer();

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_REC_START)) {

                Bundle bundle = intent.getExtras();
                recFilePath = bundle.getString(Main2Activity.REC_FILEPATH);
                Log.d(LOG_TAG, "Received recording: " + recFilePath);
                stopMediaPlayer();
                startRecMediaPlayer(recFilePath);

            }
        }


        return START_NOT_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return messenger.getBinder();
    }


    @Override
    public void onRebind(Intent intent) {
        isBound = true;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "Service created");

        //Run Timer
        runTimer();

        //Acquire wake lock (we want to ensure that CPU is running when we play media)
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        //Create WiFi lock
        wiFiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        // Get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Callback on audio focus change
        final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {

                    @Override
                    public void onAudioFocusChange(int focusChange) {

                        switch (focusChange) {

                            case AudioManager.AUDIOFOCUS_GAIN:
                                // resume volume
                                Log.i(LOG_TAG, "Audio focus gain");
                                mediaPlayer.setVolume(1.0f, 1.0f);
                                break;

                            case AudioManager.AUDIOFOCUS_LOSS:
                                // Lost focus for an unbounded amount of time: stop playback and release media player
                                Log.i(LOG_TAG, "Lost focus for unbounded amount of time");
                                mediaPlayer.stop();
                                mediaPlayer.reset();
                                setMPState(RadioPlayerService.MP_STOPPED);
                                sendStatus();
                                break;

                            default:
                                // Lost focus for a short time, duck volume
                                Log.i(LOG_TAG, "Lost focus a short time");
                                mediaPlayer.setVolume(0.0f, 0.0f);
                        }

                    }
                };

        // We want to stream music from internet
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // Callback on error
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    Log.d(LOG_TAG, "Unknown error");
                    setMPState(RadioPlayerService.MP_ERROR);
                    sendStatus();
                    //Send alert
                    sendAlert(RadioPlayerFragment.SEND_ERROR, getResources().getString(R.string.streaming_server_unreachable));

                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    Log.d(LOG_TAG, "Lost connection to streaming server");
                    setMPState(RadioPlayerService.MP_LOST_CONNECTION);
                    sendStatus();
                    //Send alert
                    sendAlert(RadioPlayerFragment.SEND_ERROR, getResources().getString(R.string.streaming_server_unreachable));
                }
                return true;
            }
        });

        //Callback when preparation is done
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {

                Log.d(LOG_TAG, "onPrepared");

                //If we are playing file, than we don't need onPrepared callback
                if (mediaPlayer.getDuration() > 1) {
                    return;
                }

                // We are ready
                setMPState(RadioPlayerService.MP_READY);

                //Request audio focus
                int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);

                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.w(LOG_TAG, "Could not get audio focus");

                    //Inform user about that
                    sendAlert(RadioPlayerFragment.SEND_ALERT, getResources().getString(R.string.no_audio_focus));

                    //Reset media player and return
                    mediaPlayer.reset();
                    setMPState(RadioPlayerService.MP_NOT_READY);
                    sendStatus();
                }

                //Go in foreground
                if (!isForeground) {
                    serviceStartForeground();
                }

                // Play!!!
                mediaPlayer.start();
                radioStation.setPlaying(true);
                playingTimeRunning = true;

                /* If we are connected over WiFi, acquire a WiFi lock */
                if (Main2Activity.internetConnection == NetworkUtil.TYPE_WIFI) {
                    wiFiLock.acquire();
                }
                setMPState(RadioPlayerService.MP_PLAYING);
                sendStatus();

                //Retrieve metadata from server
                if (radioStation.getListenType().equals("0") || radioStation.getListenType().equals("1")) {
                    refreshMetadata(radioStation.getListenUrl());
                }

                retrieveBitRate();

            }
        });

        //Callback for on info update
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(LOG_TAG, "onInfo()");

                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    // buffering
                    Log.d(LOG_TAG, "Buffering started");
                    sendAlert(RadioPlayerFragment.SEND_BUFFERING, "start");

                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    // stop buffering
                    Log.d(LOG_TAG, "Buffering stopped");
                    sendAlert(RadioPlayerFragment.SEND_BUFFERING, "stop");

                } else if (what == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
                    Log.d(LOG_TAG, "Metadata update");
                }
                return false;
            }
        });


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                Log.d(LOG_TAG, "onCompletion");
                setMPState(MP_NOT_READY);
                playingTimeRunning = false;
                playingSecs = 0;
                sendAlert(RadioPlayerFragment.SEND_COMPLETION, "");
            }
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        //Clean up the media player
        mediaPlayer.release();
        mediaPlayer = null;
    }

    /**
     * Initialize media player.
     *
     * @param mediaURL Location of the stream
     */
    protected void initMediaPlayer(String mediaURL) {

        Log.i(LOG_TAG, "initMediaPlayer");

        // Skip initialization if media player is paused or is playing
        if (mpState.equals(RadioPlayerService.MP_PAUSED) || mpState.equals(RadioPlayerService.MP_PLAYING)) {
            return;
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d(LOG_TAG, "Setting data source: " + mediaURL);
        try {
            //mediaPlayer.setDataSource(this, Uri.parse(RadioPlayerFragment.RADIO_LOCAL_URL));
            mediaPlayer.setDataSource(this, Uri.parse(mediaURL));
        } catch (Exception e) {
            Log.d(LOG_TAG, radioStation.getListenUrl() + e.toString());
            setMPState(RadioPlayerService.MP_ERROR);
            sendStatus();
        }

        setMPState(RadioPlayerService.MP_INIT);
        sendStatus();
    }

    /**
     * Prepare asynchronously media player
     */
    protected void startMediaPlayer() {

        Log.i(LOG_TAG, "startMediaPlayer");

        // If media player is pause or playing, just play
        if (mpState.equals(RadioPlayerService.MP_PAUSED)) {
            // Play!!!
            mediaPlayer.start();
            radioStation.setPlaying(true);
            playingTimeRunning = true;
            setMPState(RadioPlayerService.MP_PLAYING);
            sendStatus();

            //Retrieve meta data from server
            if (radioStation.getListenType().equals("0") || radioStation.getListenType().equals("1")) {
                refreshMetadata(radioStation.getListenUrl());
            }

        } else if (mpState.equals(RadioPlayerService.MP_PLAYING)) {
            //We are already playing, well do nothing

        } else if (mpState.equals(RadioPlayerService.MP_ERROR)) {

            //Media player should be reset

        } else {
            //Because prepareAsync() can take too long if radio station server isn't reachable, check for it
            setMPState(RadioPlayerService.MP_CONNECTING);
            mediaPlayer.prepareAsync();
            sendStatus();
        }

    }


    /**
     * Start playing recorded file
     */
    protected void startRecMediaPlayer(String recFilePath) {

        Log.i(LOG_TAG, "startRecMediaPlayer");

        // If media player is pause or playing, just play
        if (mpState.equals(RadioPlayerService.MP_PAUSED)) {
            // Play!!!
            mediaPlayer.start();
            setMPState(RadioPlayerService.MP_PLAYING);
            sendStatus();

        } else if (mpState.equals(RadioPlayerService.MP_PLAYING)) {

            //Well, do nothing

        } else {

            mediaPlayer.reset();
            //Prepare
            mediaPlayer.setAudioStreamType(AudioManager.MODE_NORMAL);
            Log.d(LOG_TAG, "Setting data source: " + recFilePath);
            try {
                mediaPlayer.setDataSource(recFilePath);
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.d(LOG_TAG, e.toString());
                setMPState(RadioPlayerService.MP_ERROR);
                sendStatus();
            }

            mediaPlayer.start();
            playingTimeRunning = true;
            setMPState(RadioPlayerService.MP_PLAYING);
            sendStatus();
        }

    }


    /**
     * Pause media player
     */
    protected void pauseMediaPlayer(Boolean isRecPlaying) {

        Log.i(LOG_TAG, "pauseMediaPlayer");

        if (mpState.equals(RadioPlayerService.MP_PLAYING)) {
            mediaPlayer.pause();
            setMPState(RadioPlayerService.MP_PAUSED);
            playingTimeRunning = false;
            sendStatus();
            if (!isRecPlaying) {
                radioStation.setPlaying(false);

                //Stop retrieving meta data from server
                if (radioStation.getListenType().equals("0") || radioStation.getListenType().equals("1")) {
                    retrieveTimer.cancel();
                }
            }
        }

    }

    /**
     * Stop media player
     */
    protected void stopMediaPlayer() {

        Log.i(LOG_TAG, "stopMediaPlayer");

        if (radioStation != null) {
            radioStation.setPlaying(false);
        }

        if ((mpState.equals(RadioPlayerService.MP_PAUSED)) || (mpState.equals(RadioPlayerService.MP_PLAYING))) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            playingTimeRunning = false;
            playingSecs = 0;
            setMPState(RadioPlayerService.MP_STOPPED);
            sendStatus();

            // Return service from foreground (maybe device needs resources)
            serviceStopForeground();

        } else {
            //If media player is in other state, reset it
            mediaPlayer.reset();
            setMPState(RadioPlayerService.MP_NOT_READY);
            sendStatus();
        }

        //Release WiFi lock
        if (wiFiLock.isHeld()) {
            wiFiLock.release();
        }

        //Stop retrieving metadata from server
        if (retrieveTimer != null) {
            Log.d(LOG_TAG, "Cancel metadata retriever");
            retrieveTimer.cancel();
        }

    }

    /**
     * Setter method for media player state
     */
    private void setMPState(String state) {
        this.mpState = state;
    }

    /**
     * Send status to UI thread via messenger
     */
    private void sendStatus() {

        // Only send status if we have UI bound to service
        if (isBound) {
            try {
                Message msg = Message.obtain(null, RadioPlayerFragment.UPDATE_STATUS);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.STATUS, mpState);
                msg.setData(bundle);
                fragmentMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send status: " + mpState);
            }
        }

    }

    /**
     * Send error to UI thread via messenger
     *
     * @param alert Alert message
     */
    private void sendAlert(int type, String alert) {

        // Only send error message if we have UI bound to service
        if (isBound) {
            try {
                Message msg = Message.obtain(null, type);
                Bundle bundle = new Bundle();
                bundle.putString(RadioPlayerFragment.EXTRA_PARAM, alert);
                msg.setData(bundle);
                fragmentMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send alert: " + alert);
            }
        }
    }

    /**
     * Moves Radio Player Service in foreground and sets notifications
     */
    private void serviceStartForeground() {

        //Main intent
        Intent intent = new Intent(getApplicationContext(), Main2Activity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(Intent.ACTION_RUN);
        intent.putExtra(RadioPlayerService.NOTIFY_RADIO_STATION, radioStation);

        //Pending intent
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intents for action keys
        Intent playIntent = new Intent(this, RadioPlayerService.class);
        playIntent.setAction(RadioPlayerFragment.ACTION_START);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent pauseIntent = new Intent(this, RadioPlayerService.class);
        pauseIntent.setAction(RadioPlayerFragment.ACTION_PAUSE);
        PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                pauseIntent, 0);

        Bitmap large_icon = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.large_icon_bw);

        Resources res = this.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        large_icon = Bitmap.createScaledBitmap(large_icon, width, height, false);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setLargeIcon(large_icon)
                .setContentTitle("BIR Player")
                .setContentText("Playing " + radioStation.getRadioStationName())
                .setContentIntent(pi)
                .addAction(android.R.drawable.ic_media_play, "Play", pplayIntent)
                .addAction(android.R.drawable.ic_media_pause, "Pause", ppauseIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        isForeground = true;
    }


    /**
     * Bring service to background
     */
    private void serviceStopForeground() {

        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }
    }


    /**
     * Try to retrieve metadata from shoutcast/icecast server
     *
     * @param url Url with metadata
     */
    private void refreshMetadata(String url) {
        Log.d(LOG_TAG, "refreshMetadata()");

        final IcyStreamMeta streamMeta = new IcyStreamMeta();

        //Set the url
        try {

            streamMeta.setStreamUrl(new URL(url));
        } catch (MalformedURLException e) {
            Log.w(LOG_TAG, "Malformed URL: " + url);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                retrieveTimer = new Timer();
                retrieveTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        String songTitle = "";
                        try {
                            //Refresh metadata
                            streamMeta.refreshMeta();
                            songTitle = new String(streamMeta.getStreamTitle().getBytes("ISO-8859-1"), "UTF-8");
                        } catch (IOException e) {
                            Log.d(LOG_TAG, "No song title info!");
                            return;
                        }

                        //Send song title to RadioPlayer Fragment
                        if (songTitle.length() > 4) {
                            Log.d(LOG_TAG, "Current song: " + songTitle);
                            sendAlert(RadioPlayerFragment.SEND_TITLE, songTitle);
                        }
                    }
                }, 0, 1000);

            }
        }).start();

    }


    /**
     * Retrieve bitrate of the listening stream using MediaMetadataRetriever
     */
    private void retrieveBitRate() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                HashMap<String, String> metadata = new HashMap<>();

                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(radioStation.getListenUrl(), metadata);
                //mediaMetadataRetriever.setDataSource(RadioPlayerFragment.RADIO_LOCAL_URL, metadata);

                bitRate = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                Log.d(LOG_TAG, "Bitrate: " + bitRate);

                sendAlert(RadioPlayerFragment.SEND_BITRATE, bitRate);
            }
        });

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

                if (playingTimeRunning) {
                    playingSecs++;
                }

                handler.postDelayed(this, 1000);
            }
        });

    }

}



