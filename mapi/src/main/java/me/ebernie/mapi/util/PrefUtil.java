package me.ebernie.mapi.util;


import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class PrefUtil {

    private static final String KEY_LAST_UPDATE = "last_update";

    public static void saveLastUpdate(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LAST_UPDATE, value)
                .apply();
    }

    @Nullable
    public static String getLastUpdate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LAST_UPDATE, null);
    }

}
