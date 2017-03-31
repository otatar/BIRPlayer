package net.komiso.otatar.birplayer;

import android.database.Cursor;
import android.database.CursorWrapper;

import net.komiso.otatar.birplayer.database.RadioStationDbSchema;

/**
 * Created by o.tatar on 23-Sep-16.
 */
public class RadioStationCursorWrapper extends CursorWrapper {

    public RadioStationCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public RadioStation getRadioStation() {

        int id = getInt(getColumnIndex("_id"));
        int sid = getInt(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.SID));
        String name = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.STATION_NAME));
        String genre = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.STATION_GENRE));
        String location = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.STATION_LOCATION));
        String url = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.STATION_URL));
        String listen_url = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.LISTEN_URL));
        String type = getString(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.LISTEN_TYPE));
        int favorite = getInt(getColumnIndex(RadioStationDbSchema.RadioStationTable.Cols.FAVORITE));

        RadioStation radioStation = new RadioStation(id, sid, name, genre, url, location, listen_url, type, favorite);

        return radioStation;

    }


}
