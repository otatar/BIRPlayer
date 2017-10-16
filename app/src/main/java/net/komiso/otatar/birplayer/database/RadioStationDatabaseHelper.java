package net.komiso.otatar.birplayer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.komiso.otatar.birplayer.RadioStation;
import net.komiso.otatar.birplayer.database.RadioStationDbSchema.RadioStationTable;
import net.komiso.otatar.birplayer.database.RadioStationDbSchema.FavoriteTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by o.tatar on 22-Sep-16.
 */
public class RadioStationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "bir_player";
    private static final int VERSION = 2;
    private static final String LOCAL_JSON_FILE = "radio_stations_json.json";

    private Context context;


    public RadioStationDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);

        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + RadioStationTable.DB_TABLE_NAME + "(" +
                   "_id INTEGER primary key autoincrement, " +
                   RadioStationTable.Cols.SID + " INTEGER, " +
                   RadioStationTable.Cols.STATION_NAME + " STRING, " +
                   RadioStationTable.Cols.STATION_GENRE + " STRING, " +
                   RadioStationTable.Cols.STATION_URL + " STRING, " +
                   RadioStationTable.Cols.STATION_LOCATION + " STRING, " +
                   RadioStationTable.Cols.LISTEN_URL + " STRING, " +
                   RadioStationTable.Cols.LISTEN_TYPE + " STRING, " +
                   RadioStationTable.Cols.FAVORITE + " INTEGER DEFAULT 0" + ")"
        );

        // We need favorite table
        db.execSQL("CREATE TABLE " + FavoriteTable.DB_TABLE_NAME + "(" +
                "_id INTEGER primary key autoincrement, " +
                FavoriteTable.Cols.ID_RADIO_STATION + " INTEGER, " +
                FavoriteTable.Cols.FAVORITE + " INTEGER" + ")"
        );

        insertRadioStationIntoTable(db);

    }


    /**
     * Inserts radio station object from JSON file into database
     * @param db
     */
    private void insertRadioStationIntoTable(SQLiteDatabase db) {
        String jsonString = getLocalJsonFile(LOCAL_JSON_FILE);

        try {

            //Parsing json
            JSONObject jsonBody = new JSONObject(jsonString);

            JSONObject stationsJsonObject = jsonBody.getJSONObject("radio_stations");
            JSONArray stationsJsonArray = stationsJsonObject.getJSONArray("stations");

            for (int i = 0; i < stationsJsonArray.length(); i++) {

                JSONObject stationJsonObject = stationsJsonArray.getJSONObject(i);

                //Construct ContentValues
                ContentValues values = new ContentValues();
                values.put(RadioStationTable.Cols.SID, stationJsonObject.getInt("id"));
                values.put(RadioStationTable.Cols.STATION_NAME, stationJsonObject.getString("station_name"));
                values.put(RadioStationTable.Cols.STATION_GENRE, stationJsonObject.getString("station_genre"));
                values.put(RadioStationTable.Cols.STATION_URL, stationJsonObject.getString("station_url"));
                values.put(RadioStationTable.Cols.STATION_LOCATION, stationJsonObject.getString("station_location"));
                values.put(RadioStationTable.Cols.LISTEN_URL, stationJsonObject.getString("listen_url"));
                values.put(RadioStationTable.Cols.LISTEN_TYPE, stationJsonObject.getString("listen_type"));

                // Insert into database
                db.insert(RadioStationTable.DB_TABLE_NAME, null, values);

            }
        } catch (JSONException e) {}

    }


    @Override
    /**
     * Runs on upgrade, when we need to update table
     */
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //First delete data from table
        db.execSQL("DELETE FROM " + RadioStationTable.DB_TABLE_NAME);

        //Insert new data
        insertRadioStationIntoTable(db);
    }


    /**
     * Gets local file with json data from assets
     * @param filename Filename String
     * @return JSON string
     */
    private String getLocalJsonFile(String filename) {

        String json;
        try {
            InputStream inputStream = context.getAssets().open(filename);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            json = new String(buffer, "UTF-8");

        } catch (IOException e) {
            return null;
        }

        return json;

    }
}
