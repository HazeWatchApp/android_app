package me.ebernie.mapi.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import me.ebernie.mapi.model.Api;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class ApiListLoader extends AsyncTaskLoader<List<Api>> {

    /**
     * This file is expected to contain the List<Api> JSON data
     */
    private static final String FILE_NAME = "haze.json";

    private List<Api> mApiList;
    private final String mEndpointUrl;
    private boolean isForce;

    public ApiListLoader(Context context, @NonNull String url, boolean isForce) {
        super(context);
        this.mEndpointUrl = url;
        this.isForce = isForce;

        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Data Source URL must not be null or empty");
        }
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public List<Api> loadInBackground() {

        List<Api> list;
        File file = new File(getContext().getFilesDir(), FILE_NAME);
        if (isForce) {
            list = fetchData();
            isForce = false;

        } else if ((!Util.isOnline(getContext()) || isDataValid()) && file.exists()) {
            list = loadData();
            if (list == null || list.isEmpty()) {
                list = fetchData();
            }
        } else {
            list = fetchData();
        }

        // Done!
        return (list == null) ? Collections.<Api>emptyList() : list;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(@NonNull List<Api> list) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            onReleaseResources(list);
        }
        List<Api> oldApps = mApiList;
        mApiList = list;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(new ArrayList<Api>(list));
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mApiList != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mApiList);
        }

        if (takeContentChanged() || mApiList == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<Api> list) {
        super.onCanceled(list);

        // At this point we can release the resources associated with 'list'
        // if needed.
        onReleaseResources(list);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mApiList != null) {
            onReleaseResources(mApiList);
            mApiList = null;
        }

    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(List<Api> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Checks if the data is valid
     *
     * @return
     */
    private boolean isDataValid() {
        Date lastUpdate = PrefUtil.getLastUpdate(getContext());
        if (lastUpdate == null) {
            // no record exists
            return false;
        } else {
            Calendar now = Calendar.getInstance();
            Calendar cal = (Calendar) now.clone();
            cal.setTime(lastUpdate);

            if (now.get(Calendar.HOUR_OF_DAY) != cal.get(Calendar.HOUR_OF_DAY)) {
                // if the hour is different,  assume there is an update
                return false;
            }
        }
        return true;
    }

    @Nullable
    private List<Api> loadData() {
        // todo APIListLoader should only read from local data source
        // network calls should be done somewhere else or else
        // users would need to open the app to get latest data

        List<Api> list = null;
        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            // expected to only have List<Api>
            fileInputStream = getContext().openFileInput(FILE_NAME);
            inputStreamReader = new InputStreamReader(fileInputStream);
            String inputStreamString = new Scanner(inputStreamReader).useDelimiter("\\A").next();
            list = new Gson().fromJson(inputStreamString, new TypeToken<ArrayList<Api>>() {
            }.getType());

        } catch (FileNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            Crashlytics.getInstance().core.logException(e);
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;

    }

    @Nullable
    private List<Api> fetchData() {
        HttpURLConnection urlConnection = null;
        List<Api> list = null;
        FileOutputStream fileOutputStream = null;
        try {
            URL url = new URL(mEndpointUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String inputStreamString = new Scanner(in).useDelimiter("\\A").next();

            JSONObject obj = new JSONObject(inputStreamString);
            String result = obj.getString("result"); // only store the List<Api>
            fileOutputStream = getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fileOutputStream.write(result.getBytes());

            // ensure result is stored first then save our last_updated
            String last_updated = obj.getString("last_updated");
            PrefUtil.saveLastUpdate(getContext(), last_updated);

            list = new Gson().fromJson(result, new TypeToken<ArrayList<Api>>() {
            }.getType());

        } catch (IOException e) {
            Crashlytics.getInstance().core.logException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            Crashlytics.getInstance().core.logException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }
}
