package me.ebernie.mapi.util;

import android.location.Location;
import android.support.annotation.NonNull;

import java.util.Comparator;

import me.ebernie.mapi.model.Api;

/**
 * Created by andhie on 9/26/15.
 */
public class DistanceComparator implements Comparator<Api> {

    private double mLat, mLng;

    public DistanceComparator(@NonNull Location location) {
        mLat = location.getLatitude();
        mLng = location.getLongitude();
    }

    @Override
    public int compare(Api lhs, Api rhs) {
        return Double.compare(lhs.getDistance(mLat, mLng), rhs.getDistance(mLat, mLng));
    }
}
