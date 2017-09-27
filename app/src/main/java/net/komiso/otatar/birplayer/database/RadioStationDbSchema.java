package net.komiso.otatar.birplayer.database;

/**
 * Created by o.tatar on 22-Sep-16.
 * Class to hold database schema, nothing special
 */
public class RadioStationDbSchema {

    public static final class RadioStationTable {

        public static final String DB_TABLE_NAME = "radio_stations";

        public static final class Cols {

            public static final String SID = "s_id";
            public static final String STATION_NAME = "station_name";
            public static final String STATION_GENRE = "station_genre";
            public static final String STATION_URL = "station_url";
            public static final String STATION_LOCATION = "station_location";
            public static final String LISTEN_URL = "listen_url";
            public static final String LISTEN_TYPE = "listen_type";
            public static final String FAVORITE = "favorite";

        }
    }

    public static final class FavoriteTable {

        public static final String DB_TABLE_NAME = "favorite_radio_stations";

        public static final class Cols {

            public static final String ID_RADIO_STATION = "id_radio_station";
            public static final String FAVORITE = "favorite";

        }

    }

}
