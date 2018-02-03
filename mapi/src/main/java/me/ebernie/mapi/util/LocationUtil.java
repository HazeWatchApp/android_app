package me.ebernie.mapi.util;


import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;

import com.google.android.gms.location.LocationListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class LocationUtil {

    @Nullable
    private static Location bestLocation;

    private static List<WeakReference<LocationListener>> mListeners;

    public static void updateLocation(@Nullable final Location location) {
        bestLocation = location;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatchLocationUpdates(location);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    dispatchLocationUpdates(location);
                }
            });
        }
    }

    @Nullable
    public static Location getLocation() {
        return bestLocation;
    }

    /**
     * This method keeps the listener via a {@link WeakReference}. Due to this design, always add
     * your listeners again in {@link Activity#onResume()}.
     *
     * @param listener
     */
    @MainThread
    public static void addLocationListener(LocationListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        } else {
            for (int i = 0, z = mListeners.size(); i < z; i++) {
                WeakReference<LocationListener> ref = mListeners.get(i);
                if (ref != null && ref.get() == listener) {
                    return;
                }
            }
        }

        mListeners.add(new WeakReference<LocationListener>(listener));
    }

    /**
     * Removes the given listener
     *
     * @param listener
     */
    @MainThread
    public static void removeLocationListener(LocationListener listener) {
        if (mListeners != null) {
            Iterator<WeakReference<LocationListener>> i = mListeners.iterator();
            while (i.hasNext()) {
                LocationListener item = i.next().get();
                if ((item == listener) || (item == null)) {
                    i.remove();
                }
            }
        }
    }

    private static void dispatchLocationUpdates(@Nullable Location location) {
        if (mListeners != null) {
            List<WeakReference<LocationListener>> list = new ArrayList<>(mListeners);
            for (int i = 0, size = list.size(); i < size; i++) {
                LocationListener item = list.get(i).get();
                if (item != null) {
                    item.onLocationChanged(location);
                }
            }
        }
    }
}
