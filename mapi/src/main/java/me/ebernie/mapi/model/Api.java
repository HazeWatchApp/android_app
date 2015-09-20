package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Api {

    @SerializedName("pagi")
    @Expose
    private String morning;

    @SerializedName("last_updated")
    @Expose
    private String lastUpdated;

    @SerializedName("lokasi")
    @Expose
    private String area;

    @SerializedName("negeri")
    @Expose
    private String state;

    @Expose
    private Datum latest;

//    @SerializedName("terkini")
//    @Expose
//    private String terkini;

    @SerializedName("petang")
    @Expose
    private String evening;

    @Expose
    private List<Datum> data = new ArrayList<>();

    @SerializedName("tengah_hari")
    @Expose
    private String noon;

    @Expose
    private Location location;

    public String getMorning() {
        return morning;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getArea() {
        return area;
    }

    public String getState() {
        return state;
    }

    public Datum getLatest() {
        return latest;
    }

    public String getEvening() {
        return evening;
    }

    public List<Datum> getData() {
        return data;
    }

    public String getNoon() {
        return noon;
    }

    public Location getLocation() {
        return location;
    }
}
