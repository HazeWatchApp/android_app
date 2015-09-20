package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;

public class Datum {

    @Expose
    private String index;
    @Expose
    private String time;

    public String getIndex() {
        return index;
    }

    public String getTime() {
        return time;
    }
}
