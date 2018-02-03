package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;

public class Coordinates {

    @Expose
    private double latitude;
    @Expose
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
