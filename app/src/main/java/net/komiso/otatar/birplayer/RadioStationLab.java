package net.komiso.otatar.birplayer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.komiso.otatar.birplayer.database.RadioStationDatabaseHelper;


import net.komiso.otatar.birplayer.database.RadioStationDbSchema;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by o.tatar on 22-Sep-16.
 * Class that handles database manipulation with RadioStation objects. Implemented as singleton.
 * Radio stations are stored on local SQLite database.
 */
public class RadioStationLab {

    private Context context;
    private static SQLiteDatabase database;
    private Activity activity;

    private static String LOG_TAG = "net.komiso.otatar.birplayer.radiostationlab.log";

    // Variable to hold json string
    private String jsonString;

    // Static variable to hold reference to self
    public static RadioStationLab radioStationLab;

    public static final String[] radioGenrePop = {"pop", "zabavna"};
    public static final String[] radioGenreFolk = {"folk", "narodna"};

    /* Array list that contains all radio station objects */
    public static ArrayList<RadioStation> radioStationsAll = new ArrayList<>();


    // Last time we refreshed radio station from server
    private static long lastRefreshTimestamp;


    /**
     * Returns single instance of RadioStationLab class
     *
     * @param activity Activity
     * @return radioStationLab
     */
    public static RadioStationLab get(Activity activity) {

        if (radioStationLab == null) {
            radioStationLab = new RadioStationLab(activity);
        }

        return radioStationLab;
    }

    /**
     * Interface for informing activities about changing radio station data
     */
    static interface RadioStationRefreshable {
        void onRadioStationRefreshed(String s);
    }

    /**
     * Reference to radioStationRefreshable to inform
     */
    private RadioStationRefreshable radioStationRefreshable;

    /**
     * Setting reference to radioStationRefreshable
     *
     * @param activity Activity
     */
    public void setRadioStationRefreshable(RadioStationRefreshable activity) {
        radioStationRefreshable = activity;
    }


    /**
     * Private constructor
     *
     * @param activity Activity
     */
    private RadioStationLab(Activity activity) {

        this.context = activity.getApplicationContext();
        database = new RadioStationDatabaseHelper(this.context).getWritableDatabase();
        this.activity = activity;

    }

    /**
     * Inserts radio stations data into database, from json string
     *
     * @param jsonString JSON String
     */
    private void insertRadioStations(String jsonString) throws IOException, JSONException {

        Log.d(LOG_TAG, "insertRadioStations()");

        //Parsing json
        JSONObject jsonBody = new JSONObject(jsonString);

        JSONObject stationsJsonObject = jsonBody.getJSONObject("radio_stations");
        JSONArray stationsJsonArray = stationsJsonObject.getJSONArray("stations");

        //Empty preloaded database
        database.execSQL("DELETE FROM " + RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME);

        for (int i = 0; i < stationsJsonArray.length(); i++) {

            JSONObject stationJsonObject = stationsJsonArray.getJSONObject(i);

            //Construct ContentValues
            ContentValues values = new ContentValues();
            values.put(RadioStationDbSchema.RadioStationTable.Cols.SID, stationJsonObject.getInt("id"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.STATION_NAME, stationJsonObject.getString("station_name"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.STATION_GENRE, stationJsonObject.getString("station_genre"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.STATION_URL, stationJsonObject.getString("station_url"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.STATION_LOCATION, stationJsonObject.getString("station_location"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.LISTEN_URL, stationJsonObject.getString("listen_url"));
            values.put(RadioStationDbSchema.RadioStationTable.Cols.LISTEN_TYPE, stationJsonObject.getString("listen_type"));

            // Insert into database
            database.insert(RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME, null, values);

            Log.d(LOG_TAG, "Inserted into database radio station:" + stationJsonObject.getString("station_name"));

        }

        Log.d(LOG_TAG, "Refresh favorite stations");
        // Refresh favorite state
        RadioStationFavoriteCursorWrapper cursor = queryFavoriteRadioStation();

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                // Update favorite field in radio_station table
                ContentValues contentValues = new ContentValues();
                contentValues.put(RadioStationDbSchema.RadioStationTable.Cols.FAVORITE, true);
                database.update(RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME, contentValues,
                        RadioStationDbSchema.RadioStationTable.Cols.SID + " = ?", new String[]{Integer.toString(cursor.getRadioStationFavorite())});
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

    }

    /**
     * AsyncTask class used for getting data out of database
     */
    class GetRadioStationsDB extends AsyncTask<Void, String, ArrayList<RadioStation>> {


        // Get radio stations from database in background
        @Override
        protected ArrayList<RadioStation> doInBackground(Void... params) {

            return getRadioStationsDB();

        }

        @Override
        protected void onPostExecute(ArrayList<RadioStation> radioStations) {
            super.onPostExecute(radioStations);

            /* Add stations to list*/
            RadioStationLab.radioStationsAll.clear();
            RadioStationLab.radioStationsAll.addAll(radioStations);
            //Inform about new list
            //radioStationRefreshable.onRadioStationRefreshed();

        }

    }

    /**
     * Fetch all radio station from database in background
     */
    public void getRadioStations() {
        Log.d(LOG_TAG, "Getting stations from database");

        new GetRadioStationsDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Fetches all radio stations from database.
     *
     * @return list array of all radio stations
     */
    public ArrayList<RadioStation> getRadioStationsDB() {

        ArrayList<RadioStation> radioStations = new ArrayList<>();

        RadioStationCursorWrapper cursor = queryRadioStation();

        try {

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                radioStations.add(cursor.getRadioStation());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        return radioStations;

    }

    // Helper function to return CursorWrapper
    private RadioStationCursorWrapper queryRadioStation() {

        Cursor cursor = database.query(RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME, null, null, null, null, null, null);

        return new RadioStationCursorWrapper(cursor);

    }

    // Helper function to RadioStationFavoriteCursorWrapper
    private RadioStationFavoriteCursorWrapper queryFavoriteRadioStation() {

        Cursor cursor = database.query(RadioStationDbSchema.FavoriteTable.DB_TABLE_NAME, null, null, null, null, null, null);

        return new RadioStationFavoriteCursorWrapper(cursor);
    }


    /**
     * Fetch radio station data from web and store it in database
     *
     * @param url Server URL
     */
    public void getRadioStationsWeb(String url) {

        Log.d(LOG_TAG, RadioStationLab.class + ":getRadioStationWeb");
        new FetchRadioStations().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);

    }


    /**
     * A simple class that loads radio stations from HTTP server.
     * Station data are JSON encoded
     */
    class FetchRadioStations extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            //Refresh radio station if it was over half an hour since last refresh
            if ((System.currentTimeMillis() / 1000 - lastRefreshTimestamp) > 1800) {
                try {
                    //Fetch radio station data from server
                    Log.d(LOG_TAG, "Server: " + params[0]);
                    jsonString = fetchHttp(params[0]);

                    //Parse and insert into database
                    insertRadioStations(jsonString);

                    //We have refreshed
                    lastRefreshTimestamp = System.currentTimeMillis() / 1000;


                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot load data from server: " + e.toString());
                    jsonString = e.toString();
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.toString());
                    jsonString = e.toString();
                }
            }

            //Load from database in ArrayList
            RadioStationLab.radioStationsAll.clear();
            RadioStationLab.radioStationsAll.addAll(getRadioStationsDB());

            return jsonString;
        }

        @Override
        protected void onPostExecute(String s) {
            /* Inform radioStationRefreshable about refreshed data */
            radioStationRefreshable.onRadioStationRefreshed(s);
        }


        /**
         * Fetches html content from given url
         *
         * @param url URL String
         * @return html
         */
        public String fetchHttp(String url) throws IOException {

            URL httpurl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) httpurl.openConnection();
            connection.setConnectTimeout(3000);

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = connection.getInputStream();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException(connection.getResponseMessage() + ": with " + url);
                }

                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                return new String(out.toByteArray());

            } finally {
                connection.disconnect();
            }

        }
    }

    /**
     * Updates favorite radio station in database
     *
     * @param radioStation RadioStation object
     * @param favorite     If radio station if favorite
     */
    public static void updateFavoriteRadioStation(final RadioStation radioStation, final boolean favorite) {

        Log.d(LOG_TAG, RadioStationLab.class + ":updateFavoriteRadioStation");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues contentValues = new ContentValues();

                // First update radio_station_favorite table
                if (favorite) {

                    contentValues.put(RadioStationDbSchema.FavoriteTable.Cols.ID_RADIO_STATION, radioStation.getSid());
                    contentValues.put(RadioStationDbSchema.FavoriteTable.Cols.FAVORITE, favorite);
                    Log.d(LOG_TAG, "Inserting into " + RadioStationDbSchema.FavoriteTable.DB_TABLE_NAME + " table: " + radioStation.getSid() + favorite);
                    database.insert(RadioStationDbSchema.FavoriteTable.DB_TABLE_NAME, null, contentValues);

                } else {
                    Log.d(LOG_TAG, "Deleting from " + RadioStationDbSchema.FavoriteTable.DB_TABLE_NAME + " table: " + radioStation.getSid());
                    database.delete(RadioStationDbSchema.FavoriteTable.DB_TABLE_NAME, RadioStationDbSchema.FavoriteTable.Cols.ID_RADIO_STATION
                            + " = ?", new String[]{Integer.toString(radioStation.getSid())});
                }


                // After update radio_station table
                contentValues = new ContentValues();
                contentValues.put(RadioStationDbSchema.RadioStationTable.Cols.FAVORITE, favorite);

                Log.d(LOG_TAG, "Updating " + RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME + " table");
                database.update(RadioStationDbSchema.RadioStationTable.DB_TABLE_NAME, contentValues,
                        "_id = ?", new String[]{Integer.toString(radioStation.getId())});

            }
        }).start();

    }


    /**
     * Filters list of all radio stations for radio station with specific genre
     *
     * @param genres String array of genres
     */
    public static ArrayList<RadioStation> filterRadioStationsByGenre(String[] genres) {

        Log.d(LOG_TAG, RadioStationLab.class + ":filterRadioStationByGenre");
        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : RadioStationLab.radioStationsAll) {

            //Split genres
            for (String genre : radioStation.getRadioStationGenre().split(",")) {
                //If there is a match, add radio station to list
                if (Arrays.asList(genres).contains(genre.toLowerCase())) {
                    list.add(radioStation);
                }

            }
        }

        return list;
    }


    /**
     * Filters list of all radio stations for radio station with specific location
     *
     * @param location Location
     */
    public static ArrayList<RadioStation> filterRadioStationsByLocation(String location) {

        Log.d(LOG_TAG, RadioStationLab.class + ":filterRadioStationByLocation");
        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : RadioStationLab.radioStationsAll) {

            if (radioStation.getRadioStationLocation().equals(location)) {
                list.add(radioStation);
            }
        }

        return list;
    }

    /**
     * Filters list of all radio stations for favorite radio stations
     */
    public static ArrayList<RadioStation> filterRadioStationByFavorite() {

        Log.d(LOG_TAG, RadioStationLab.class + ":filterRadioStationByFavorite");
        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : RadioStationLab.radioStationsAll) {

            if (radioStation.getFavorite().equals(true)) {
                list.add(radioStation);
            }
        }

        return list;
    }

    /**
     * Filters list of all radio stations by name
     *
     * @param name Name of radio station
     */
    public static ArrayList<RadioStation> filterRadioStationByName(String name) {

        Log.d(LOG_TAG, RadioStationLab.class + ":filterRadioStationByName");
        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : RadioStationLab.radioStationsAll) {

            if (radioStation.getRadioStationName().toLowerCase().contains(name.toLowerCase())) {
                list.add(radioStation);
            }

        }

        return list;
    }


    public static ArrayList<RadioStation> getRadioStationsAll() {
        return RadioStationLab.radioStationsAll;
    }
}


