package com.example.otatar.birplayer;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


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
    private boolean isForeground = false;

    /**
     * Are we bound?
     */
    private boolean isBound = false;

    /**
     * Radio Station that is being played
     */
    private RadioStation radioStation;

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
    public MediaPlayer mediaPlayer;


    public final IBinder binder = new RadioPlayerBinder();

    /**
     * LOG_TAG string
     */
    public static final String LOG_TAG = "RadioPlayerServiceLog";

    /**
     * ID used from notification service
     */
    public static final int NOTIFICATION_ID = 333;

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
     * Variable to track MediaPlayer state
     */
    private String mpState = this.MP_NOT_READY;


    //Constructor
    public RadioPlayerService() {

        this.mediaPlayer = new MediaPlayer();

    }


    class RadioPlayerBinder extends Binder {
        RadioPlayerService getRadioPlayer() {
            return RadioPlayerService.this;
        }
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
                        pauseMediaPlayer();
                        break;
                    case RadioPlayerFragment.ACTION_STOP:
                        stopMediaPlayer();
                        break;
                }

            }
        }
    }


    /**
     * Returns new intent that starts service
     * @param context
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

                pauseMediaPlayer();

            } else if (intent.getAction().equals(RadioPlayerFragment.ACTION_STOP)) {

                stopMediaPlayer();
            }
        }


        return START_STICKY;
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

        //Acquire wake lock (we want to ensure that CPU is running when we play media)
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        //Create WiFi lock
        wiFiLock =  ((WifiManager) getSystemService(Context.WIFI_SERVICE))
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

                if(what == mediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    Log.d(LOG_TAG, radioStation.getListenUrl());
                    setMPState(RadioPlayerService.MP_ERROR);
                    sendStatus();
                } else if(what == mediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    setMPState(RadioPlayerService.MP_LOST_CONNECTION);
                    sendStatus();
                }
                return true;
            }
        });

        //Callback when preparation is done
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {

                // We are ready
                setMPState(RadioPlayerService.MP_READY);

                //Request audio focus
                int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);

                if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.w(LOG_TAG, "Could not get audio focus");

                    //Inform user about that
                    Toast.makeText(getApplicationContext(), R.string.no_audio_focus, Toast.LENGTH_SHORT).show();

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

                /* If we are connected over WiFi, acquire a WiFi lock */
                if (NetworkUtil.checkNetworkConnection(getApplicationContext()) == NetworkUtil.TYPE_WIFI) {
                    wiFiLock.acquire();
                }
                setMPState(RadioPlayerService.MP_PLAYING);
                sendStatus();

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
     * @param mediaURL
     */
    protected void initMediaPlayer(String mediaURL) {

        Log.i(LOG_TAG, "initMediaPlayer");

        // Skip initialization if media player is paused or is playing
        if (mpState == RadioPlayerService.MP_PAUSED || mpState == RadioPlayerService.MP_PLAYING) {
            return;
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d(LOG_TAG, "Setting data source: " + mediaURL);
        try {
            mediaPlayer.setDataSource(mediaURL);
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
        if (mpState == RadioPlayerService.MP_PAUSED || mpState == RadioPlayerService.MP_PLAYING) {
            // Play!!!
            mediaPlayer.start();
            setMPState(RadioPlayerService.MP_PLAYING);
            sendStatus();

        } else if (mpState == RadioPlayerService.MP_ERROR) {

            //Media player should be reseted
            return;

        } else {
            //We should first be prepared
            mediaPlayer.prepareAsync();
            setMPState(RadioPlayerService.MP_CONNECTING);
            sendStatus();
        }

    }

    /**
     * Pause media player
     */
    protected void pauseMediaPlayer() {

        Log.i(LOG_TAG, "pauseMediaPlayer");

        if (mpState == RadioPlayerService.MP_PLAYING) {
            mediaPlayer.pause();
            setMPState(RadioPlayerService.MP_PAUSED);
            sendStatus();
        }

    }

    /**
     * Stop media player
     */
    protected void stopMediaPlayer() {

        Log.i(LOG_TAG, "stopMediaPlayer");

        if ((mpState == RadioPlayerService.MP_PAUSED) || (mpState == RadioPlayerService.MP_PLAYING)) {
            mediaPlayer.stop();
            mediaPlayer.reset();
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
        if(wiFiLock.isHeld()) {
            wiFiLock.release();
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
     * Moves Radio Player Service in foreground and sets notifications
     */
    private void serviceStartForeground() {

        //Pending intent
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), NowPlayingActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Intents for action keys
        Intent playIntent = new Intent(this, RadioPlayerService.class);
        playIntent.setAction(RadioPlayerFragment.ACTION_START);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent pauseIntent = new Intent(this, RadioPlayerService.class);
        pauseIntent.setAction(RadioPlayerFragment.ACTION_PAUSE);
        PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                pauseIntent, 0);

        Intent stopIntent = new Intent(this, RadioPlayerService.class);
        stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);


        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle("BIR Player")
                .setContentText("Playing " + radioStation.getRadioStationName())
                .setContentIntent(pi)
                .addAction(android.R.drawable.ic_media_play, "Play", pplayIntent)
                .addAction(android.R.drawable.ic_media_pause, "Pause", ppauseIntent)
                .addAction(R.drawable.ic_media_stop, "Stop", pstopIntent)
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
}
