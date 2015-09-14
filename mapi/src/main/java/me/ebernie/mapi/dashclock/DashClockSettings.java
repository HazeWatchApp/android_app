package me.ebernie.mapi.dashclock;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import java.util.Date;
import java.util.List;

import me.ebernie.mapi.api.DataApi;
import me.ebernie.mapi.db.DatabaseHelper;
import me.ebernie.mapi.model.AirPolutionIndex;
import my.codeandroid.hazewatch.R;

/**
 * Created by ebernie on 3/15/14.
 */
public class DashClockSettings extends PreferenceActivity implements DatabaseHelper.PersistableDataListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setIcon(R.drawable.hazeicon);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        DataApi.INSTANCE.getIndex(this, this, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // TODO: if the previous activity on the stack isn't a ConfigurationActivity,
            // launch it.
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupSimplePreferencesScreen(String[] values) {
        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.dashclock_pref);
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // List preference under the category
        ListPreference listPref = new ListPreference(this);
        listPref.setKey(MapiExtension.AREA_PREF); //Refer to get the pref value
        listPref.setEntries(values);
        listPref.setEntryValues(values);
        listPref.setDialogTitle("API Area");
        listPref.setTitle("Area");
        listPref.setSummary("Select the area you wish to display API for");
        root.addPreference(listPref); // Adding under the category
        setPreferenceScreen(root);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference(MapiExtension.AREA_PREF));
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return false;
    }

    /**
     * A preference value change listener that updates the preference's summary to reflect its new
     * value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the preference's value is
     * changed, its summary (line of text below the preference title) is updated to reflect the
     * value. The summary is also immediately updated upon calling this method. The exact display
     * format is dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void updateList(List<AirPolutionIndex> index) {
        //create preference
        if (index != null && !index.isEmpty()) {
            String[] area = new String[index.size()];
            for (int i = 0; i < index.size(); i++) {
                final AirPolutionIndex airPolutionIndex = index.get(i);
                area[i] = airPolutionIndex.getArea();
            }

            setupSimplePreferencesScreen(area);
        }
    }

    @Override
    public void setUpdateDate(Date date) {
        // do nothing
    }
}
