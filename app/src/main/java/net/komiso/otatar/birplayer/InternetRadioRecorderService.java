package net.komiso.otatar.birplayer;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.BuildConfig;
import android.util.Log;

import java.io.IOException;

public class InternetRadioRecorderService extends Service {

    public static final String LOG_TAG = "RadioRecordingService";
    public static final String START_RECORDING = "start";
    public static final String STOP_RECORDING = "stop";
    public static final String BUNDLE_URL = "bundle_url";
    public static final String BUNDLE_RADIO_STATION = "bundle_radio_station";

    /* Recording state: true - recording, false - not recording */
    private boolean recordingState = false;

    /* Is any client bound */
    private boolean isBound = false;

    /* Client messenger */
    private Messenger clientMessenger;

    /* Local messenger */
    private Messenger messenger = new Messenger(new IncomingHandler());

    /* Messenger service messages */
    public static final int REGISTER = 0;
    public static final int SEND_STATE = 11;
    public static final int SEND_ERR = 12;

    public static final String BUNDLE_STATE = "state";
    public static final String BUNDLE_MSG = "msg";


    // Constructor
    public InternetRadioRecorderService() {
    }


    /**
     * Handler for receiving messages from client
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG_TAG, "Received message: " + msg.what);

            if (msg.what == REGISTER) {
                clientMessenger = msg.replyTo;
            }

        }
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
        Log.d(LOG_TAG, "Unbind");
        isBound = false;
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "OnCreate");

    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "OnDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {


        if (intent.getAction().equals(START_RECORDING)) {

            //We will do recording in a separated thread (no networking on main thread!!!)
            new Thread(new Runnable() {
                @Override
                public void run() {

                    //URL
                    Bundle bundle = intent.getBundleExtra(BUNDLE_RADIO_STATION);
                    RadioStreamRecorder streamRecorder = RadioStreamRecorder.getRadioStreamRecorder((RadioStation) bundle.getSerializable(BUNDLE_RADIO_STATION));

                    try {
                        recordingState = true;
                        streamRecorder.startRecording();
                    } catch (IOException e) {
                        Log.d(LOG_TAG, "Something went wrong: " + e.toString());
                        sendErr(e.toString());
                    }
                }

            }).start();

        } else if (intent.getAction().equals(STOP_RECORDING)) {

            Bundle bundle = intent.getBundleExtra(BUNDLE_RADIO_STATION);
            RadioStreamRecorder streamRecorder = RadioStreamRecorder.getRadioStreamRecorder((RadioStation) bundle.getSerializable(BUNDLE_RADIO_STATION));
            streamRecorder.stopRecording();
            recordingState = false;
            sendState();

            stopSelf();

        }

        return START_NOT_STICKY;

    }

    private void sendState() {

        if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Send recording state");

        // Only send status if we have UI bound to service
        if (isBound) {
            try {
                Message msg = Message.obtain(null, SEND_STATE);
                Bundle bundle = new Bundle();
                bundle.putBoolean(BUNDLE_STATE, recordingState);
                msg.setData(bundle);
                clientMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send state: " + recordingState);
            }
        }

    }


    private void sendErr(String msgErr) {

        if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Send error to client ");

        if (isBound) {
            try {
                Message msg = Message.obtain(null, SEND_ERR);
                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_MSG, msgErr);
                msg.setData(bundle);
                clientMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Cannot send error " + msgErr);
            }
        }

    }

}
