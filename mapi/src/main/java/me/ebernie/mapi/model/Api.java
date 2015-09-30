package me.ebernie.mapi.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Api {

    @SerializedName("pagi")
    @Expose
    private String morning;

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

    /**
     * @param lat
     * @param lng
     * @return distance in meters
     */
    public double getDistance(double lat, double lng) {
        if (location != null && location.getCoordinates() != null) {
            Coordinates c = location.getCoordinates();
            float[] result = new float[1];
            android.location.Location.distanceBetween(lat, lng, c.getLatitude(), c.getLongitude(), result);
            return result[0];
        }

        return 0;
    }
}
