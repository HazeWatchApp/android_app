package me.ebernie.mapi.util;


import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrefUtil {

    private static final String KEY_LAST_UPDATE = "last_update";

    public static void saveLastUpdate(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LAST_UPDATE, value)
                .apply();
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    @Nullable
    public static Date getLastUpdate(Context context) {

        String string = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LAST_UPDATE, null);

        try {
            return TextUtils.isEmpty(string) ? null : sdf.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

}
