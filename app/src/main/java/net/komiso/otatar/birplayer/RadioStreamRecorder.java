package net.komiso.otatar.birplayer;

import android.os.Environment;
import android.util.Log;

import net.komiso.otatar.biplayer.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by o.tatar on 10-Apr-17.
 * RadioStreamRecorder Class. Encapsulate functionality of stream recording. Singleton
 * TO-DO: Add constructor to accept RadioStation object. Pay attention on media type
 */
public class RadioStreamRecorder {

    public static RadioStreamRecorder radioStreamRecorder;

    private static final String LOG_TAG = "RadioStreamRecorder";
    public static final String RECORD_DIR = "BIR_Player_Recordings";
    private boolean stopRecording;
    private String radioURL;
    private File outputFile;
    private String radioStationName = "";

    private RadioStreamRecorder(String url) {

        this.radioURL = url;
        this.stopRecording = false;

    }

    private RadioStreamRecorder(RadioStation radioStation) {

        this.radioURL = radioStation.getListenUrl();
        this.stopRecording = false;
        this.radioStationName = radioStation.getRadioStationName();
    }


    private void setRadioStation(RadioStation radioStation) {

        this.radioURL = radioStation.getListenUrl();
        this.stopRecording = false;
        this.radioStationName = radioStation.getRadioStationName();
    }


    public static RadioStreamRecorder getRadioStreamRecorder(String url) {

        if (radioStreamRecorder == null) {
            radioStreamRecorder = new RadioStreamRecorder(url);
        }

        return radioStreamRecorder;
    }


    public static RadioStreamRecorder getRadioStreamRecorder(RadioStation radioStation) {

        if (radioStreamRecorder == null) {
            radioStreamRecorder = new RadioStreamRecorder(radioStation);
            return radioStreamRecorder;
        }

        radioStreamRecorder.setRadioStation(radioStation);
        return radioStreamRecorder;
    }


    /**
     * Rips internet radio stream byte-by-byte and stores it on external storage
     * @throws IOException
     */
    public void startRecording() throws IOException {

        if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Start recording");

        this.stopRecording = false;

        //Input
        if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Starting to record: " + radioStationName);
        URL url = new URL(radioURL);
        //URL url = new URL(RadioPlayerFragment.RADIO_LOCAL_URL);
        // We need to set connection timeout
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        InputStream inputStream = connection.getInputStream();

        //Output
        //Check for external storage
        if (!canWriteOnExternalStorage()) {
            throw new IOException("Can't write to external storage");

        } else {

            //Check if there is a our directory
            File extStorage = Environment.getExternalStorageDirectory();
            File recDir = new File(extStorage.getAbsolutePath() + "/" + RECORD_DIR);
            if (recDir.mkdirs()) {
                if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Directory created: " + recDir.getAbsolutePath());
            } else {
                if (recDir.isDirectory()) {
                    if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Directory already exists: " + recDir.getAbsolutePath());
                } else {
                    if (BuildConfig.DEBUG) Log.e(LOG_TAG, "Can't create directory: " + recDir.getAbsolutePath());
                    throw new IOException("Can't create directory on external storage");
                }
            }

            //Create output file
            outputFile = new File(recDir, "rec_" + this.radioStationName + "_" + String.valueOf(System.currentTimeMillis()).substring(7) + ".mp3");
            if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Output file: " + outputFile.getAbsolutePath());

        }

        //Ripping, ripping
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

        int c, bytesRead = 0;

        try {
            while (((c = inputStream.read()) != -1)) {

                if (stopRecording) {
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Recording stopped");
                    inputStream.close();
                    fileOutputStream.close();
                    break;
                }
                fileOutputStream.write(c);
            }
        } finally {
            inputStream.close();
            fileOutputStream.close();
        }
    }


    /**
     * Stops recording
     */
    public void stopRecording() {

        if(BuildConfig.DEBUG) Log.d(LOG_TAG, "Stop recording URL: " + radioURL);
        stopRecording = true;

    }

    /**
     * Checker function for availability of external storage
     * @return true - if external storage is available, false is external storage is unavailable.
     */
    private boolean canWriteOnExternalStorage() {

        // get the state of your external storage
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // if storage is mounted return true
            if(BuildConfig.DEBUG) Log.d(LOG_TAG, "We can write to external storage");
            return true;
        }
        if(BuildConfig.DEBUG) Log.e(LOG_TAG, "We can't write to external storage");
        return false;
    }
}

