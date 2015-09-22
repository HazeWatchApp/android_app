package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;

public class Coordinates {

    @Expose
    private String latitude;
    @Expose
    private String longitude;

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
