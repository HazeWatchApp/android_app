package me.ebernie.mapi.dashclock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.Date;
import java.util.List;

import me.ebernie.mapi.MainActivity;
import me.ebernie.mapi.api.DataApi;
import me.ebernie.mapi.db.DatabaseHelper;
import me.ebernie.mapi.model.AirPolutionIndex;
import my.codeandroid.hazewatch.R;

/**
 * Created by ebernie on 3/15/14.
 */
public class MapiExtension extends DashClockExtension implements DatabaseHelper.PersistableDataListener {
    private static final String TAG = "MapiExtension";

    public static final String AREA_PREF = "area_pref";
    private static String area = null;

    @Override
    protected void onUpdateData(int reason) {
        // Get preference value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String name = sp.getString(AREA_PREF, null);

        if (name != null) {
            area = name;
            DataApi.INSTANCE.getIndex(this, this, false);
        }

    }

    @Override
    public void updateList(List<AirPolutionIndex> index) {
        if (index != null && !index.isEmpty()) {
            String api1 = null;
            String api2 = null;
            String api3 = null;
            for (int i = 0; i < index.size(); i++) {
                final AirPolutionIndex polutionIndex = index.get(i);
                if (polutionIndex.getArea().equalsIgnoreCase(area)) {
                    api1 = polutionIndex.getSevenAmIndex();
                    api2 = polutionIndex.getElevenAmIndex();
                    api3 = polutionIndex.getFivePmIndex();
                }
            }

            // Publish the extension data update.
            String content = "API - 7 am: " + api1 + ", 11 am: " + api2 + ", 5 pm: " + api3;
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.hazeicon)
                    .status(getString(R.string.app_name))
                    .expandedTitle(content)
                    .expandedBody(area)
                    .contentDescription(content)
                    .clickIntent(new Intent(this, MainActivity.class)));
        }
    }

    @Override
    public void setUpdateDate(Date date) {
        //do nothing
    }
}
