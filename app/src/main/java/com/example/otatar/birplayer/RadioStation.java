package com.example.otatar.birplayer;

import java.io.Serializable;

/**
 * Models internet radio station object.
 * Created by o.tatar on 23-Sep-16.
 */
public class RadioStation implements Serializable {

    private int id;
    private int sid;
    private String radioStationName;
    private String radioStationGenre;
    private String radioStationUrl;
    private String radioStationLocation;
    private String listenUrl;
    private String listenType;

    public RadioStation(int id, int sid, String name, String genre, String url,
                        String location, String  listenUrl, String type) {

        this.id = id;
        this.sid = sid;
        this.radioStationName = name;
        this.radioStationGenre = genre;
        this.radioStationUrl = url;
        this.radioStationLocation= location;
        this.listenUrl = listenUrl;
        this.listenType = type;

    }

    public int getId() {
        return id;
    }

    public String getRadioStationName() {
        return radioStationName;
    }

    public String getRadioStationGenre() {
        return radioStationGenre;
    }

    public String getRadioStationUrl() {
        return radioStationUrl;
    }

    public String getRadioStationLocation() {
        return radioStationLocation;
    }

    public String getListenUrl() {
        return listenUrl;
    }

    public String getListenType() {
        return listenType;
    }

    public int getSid() {
        return sid;
    }
}
