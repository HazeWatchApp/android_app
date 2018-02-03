package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;

public class Location {

    @Expose
    private String state;
    @Expose
    private Coordinates coordinates;
    @Expose
    private String area;

    public String getState() {
        return state;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public String getArea() {
        return area;
    }
}
