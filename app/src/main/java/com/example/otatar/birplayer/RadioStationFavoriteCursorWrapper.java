package com.example.otatar.birplayer;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.example.otatar.birplayer.database.RadioStationDbSchema;

/**
 * Created by o.tatar on 17-Oct-16.
 */
public class RadioStationFavoriteCursorWrapper extends CursorWrapper {

    public RadioStationFavoriteCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public int getRadioStationFavorite() {

        int id = getInt(getColumnIndex("_id"));
        int sid = getInt(getColumnIndex(RadioStationDbSchema.FavoriteTable.Cols.ID_RADIO_STATION));

        return sid;

    }
}
