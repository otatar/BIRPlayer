package com.example.otatar.birplayer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.otatar.birplayer.database.RadioStationDbSchema.RadioStationTable;

/**
 * Created by o.tatar on 22-Sep-16.
 */
public class RadioStationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "bir_player";
    private static final int VERSION = 1;

    public RadioStationDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
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
                   RadioStationTable.Cols.LISTEN_TYPE + " STRING" + ")"
        );

        //Put some values into database
        ContentValues values = new ContentValues();
        values.put(RadioStationTable.Cols.SID, 1);
        values.put(RadioStationTable.Cols.STATION_NAME, "Radio Ba");
        values.put(RadioStationTable.Cols.STATION_GENRE,"Various");
        values.put(RadioStationTable.Cols.STATION_URL,"wwww.radioba.ba");
        values.put(RadioStationTable.Cols.STATION_LOCATION,"Sarajevo");
        values.put(RadioStationTable.Cols.LISTEN_URL,"http://streaming.radioba.ba:10002/radio_Ba");
        values.put(RadioStationTable.Cols.LISTEN_TYPE,1);

        // Insert into database
        db.insert(RadioStationTable.DB_TABLE_NAME, null, values);

        //Put some values into database
        values = new ContentValues();
        values.put(RadioStationTable.Cols.SID, 2);
        values.put(RadioStationTable.Cols.STATION_NAME, "RSG");
        values.put(RadioStationTable.Cols.STATION_GENRE,"pop, zabavna");
        values.put(RadioStationTable.Cols.STATION_URL,"wwww.rsg.ba");
        values.put(RadioStationTable.Cols.STATION_LOCATION,"Sarajevo");
        values.put(RadioStationTable.Cols.LISTEN_URL,"http://195.222.57.33:8090/");
        values.put(RadioStationTable.Cols.LISTEN_TYPE,0);

        // Insert into database
        db.insert(RadioStationTable.DB_TABLE_NAME, null, values);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
