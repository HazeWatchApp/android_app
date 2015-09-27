package me.ebernie.mapi;


import android.content.Context;
import android.graphics.Rect;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.location.LocationListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.ebernie.mapi.adapter.SimpleAdapter;
import me.ebernie.mapi.adapter.SimpleSectionedRecyclerViewAdapter;
import me.ebernie.mapi.adapter.SpacesItemDecoration;
import me.ebernie.mapi.model.Api;
import me.ebernie.mapi.util.DistanceComparator;
import me.ebernie.mapi.util.LocationUtil;
import me.ebernie.mapi.util.MultiMap;
import me.ebernie.mapi.widget.EmptyRecyclerView;
import me.ebernie.mapi.widget.MultiSwipeRefreshLayout;
import my.codeandroid.hazewatch.BuildConfig;
import my.codeandroid.hazewatch.R;

public class ApiListFragment extends Fragment implements LocationListener {

    public static final String DATE_FORMAT = "d MMMM yyyy, EEEE";
    private static final String TAG = ApiListFragment.class.getName();

    private static final float DISTANCE_DELTA_METERS = 1000f;
    SimpleDateFormat sdf;

    public static ApiListFragment newInstance() {
        return new ApiListFragment();

    }

    @Bind(R.id.refreshLayout)
    MultiSwipeRefreshLayout mRefreshLayout;
    @Bind(R.id.listContainer)
    View mListContainer;

    @Bind(android.R.id.list)
    EmptyRecyclerView mList;
    @Bind(android.R.id.empty)
    View mEmpty;

    SimpleSectionedRecyclerViewAdapter mAdapter;

    @Nullable
    private Location mLastLocation;

    private List<Api> mApiList = Collections.emptyList();

    private FetchDataTask mFetchDataTask;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                .format(new Date());
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(date);

        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        mList.addItemDecoration(new SpacesItemDecoration(new Rect(pad, pad, pad, pad)));
        mList.setEmptyView(mEmpty);
        mList.setAdapter(new SimpleAdapter(new ArrayList<Api>()));

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (BuildConfig.DEBUG) Log.d(TAG, "Manually refreshing");
                fetchData();
            }
        });
        mRefreshLayout.setColorSchemeColors(R.color.color_primary);
        mRefreshLayout.setSwipeableChildren(android.R.id.list, android.R.id.empty);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(true);
            }
        });
        File file = new File(getActivity().getFilesDir() + "/haze.json");
        if ((!isOnline() || !isMoreThanTenMinutes()) && file.exists()) {
            loadData();
        } else {
            fetchData();
        }

        LocationUtil.addLocationListener(this);

    }

    /**
     * if more than 10 minutes, return true
     * @return
     */
    private boolean isMoreThanTenMinutes(){
        Date strDate = null;
        try {
            strDate = sdf.parse(getTimestamp());
            return (new Date().getTime() - strDate.getTime()) > 10 * 60 * 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * check if online
     * @return boolean
     */
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocationUtil.addLocationListener(this);
    }

    private void loadData() {
        LoadDataTask loadDataTask = new LoadDataTask(new DataListener() {
            @Override
            public void onDataReady(List<Api> list) {
                mApiList = list;
                if (mLastLocation == null) {
                    sortByAlphabet(list);
                } else {
                    sortForDistance(list, mLastLocation);
                }

                setListShown(true, isResumed());
            }

            @Override
            public void onError() {
                setListShown(true, isResumed());
            }
        });
        AsyncTaskCompat.executeParallel(loadDataTask);
    }

    private void fetchData() {
        if (mFetchDataTask != null && mFetchDataTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        mFetchDataTask = new FetchDataTask(new DataListener() {
            @Override
            public void onDataReady(List<Api> list) {
                if (isDetached()) {
                    return;
                }

                mApiList = list;
                saveTimestamp();
                if (mLastLocation == null) {
                    sortByAlphabet(list);
                } else {
                    sortForDistance(list, mLastLocation);
                }

                setListShown(true, isResumed());
            }

            @Override
            public void onError() {
                setListShown(true, isResumed());
            }
        });
        AsyncTaskCompat.executeParallel(mFetchDataTask, getString(R.string.data_source));
    }

    private void saveTimestamp() {

        String currentDateandTime = sdf.format(new Date());
        getActivity()
                .getSharedPreferences("hazewatch", Context.MODE_PRIVATE)
                .edit()
                .putString("last_update", currentDateandTime)
                .apply();
    }

    private String getTimestamp(){
        return getActivity()
                .getSharedPreferences("hazewatch", Context.MODE_PRIVATE)
                .getString("last_update", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        if (mFetchDataTask != null && mFetchDataTask.getStatus() == AsyncTask.Status.RUNNING) {
            mFetchDataTask.cancel(true);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mLastLocation == null || mLastLocation.distanceTo(location) >= DISTANCE_DELTA_METERS) {
                sortForDistance(mApiList, location);
                mLastLocation = location;
            }
        }
    }

    private void sortByAlphabet(List<Api> list) {
        //todo merge the algo with sortByDistance

        // 14 states, 10 area max (sarawak)
        MultiMap<String, Api> indices = new MultiMap<>();
        Api api;
        for (int i = 0, size = list.size(); i < size; i++) {
            api = list.get(i);
            indices.put(api.getState(), api);
        }

        List<Api> fullList = new LinkedList<>();

        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>(14);

        String[] array = indices.keySet().toArray(new String[14]);
        Arrays.sort(array);

//                Log.i("tag", "total states = " + array.length);
        for (int i = 0, size = array.length; i < size; i++) {
            String key = array[i];
            sections.add(new SimpleSectionedRecyclerViewAdapter.Section(fullList.size(), key));
            fullList.addAll(indices.get(key));
//                    Log.i("tag", key + ": total area = " + indices.get(key).size());
        }

        SimpleAdapter apiAdapter = new SimpleAdapter(fullList);
        mAdapter = new SimpleSectionedRecyclerViewAdapter(R.layout.list_item_location, android.R.id.text1, apiAdapter);
        mAdapter.setSections(sections);
        mList.setAdapter(mAdapter);
    }

    private void sortForDistance(List<Api> list, @Nullable Location location) {
        //todo merge the algo with sortByAlphabet

        if (location == null) {
            sortByAlphabet(list);
            return;
        }

        // 14 states, 10 area max (sarawak)
        MultiMap<String, Api> indices = new MultiMap<>();
        Api api;
        for (int i = 0, size = list.size(); i < size; i++) {
            api = list.get(i);
            indices.put(api.getState(), api);
        }

        DistanceComparator comparator = new DistanceComparator(location);

        List<Api> nearestAreaInState = new ArrayList<>(14);
        for (String state : indices.keySet()) {
            // sort all areas in a state based on distance
            List<Api> areaList = indices.get(state);
            Collections.sort(areaList, comparator);
            if (!areaList.isEmpty()) {
                // pick the nearest and store in the list
                nearestAreaInState.add(areaList.get(0));
            }
        }

        // sort the nearest state as dictated by area distance
        Collections.sort(nearestAreaInState, comparator);

        List<Api> fullList = new LinkedList<>();
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>(14);

        for (int i = 0, size = nearestAreaInState.size(); i < size; i++) {
            String key = nearestAreaInState.get(i).getState();
            sections.add(new SimpleSectionedRecyclerViewAdapter.Section(fullList.size(), key));
            fullList.addAll(indices.get(key));
//                    Log.i("tag", key + ": total area = " + indices.get(key).size());
        }

        SimpleAdapter apiAdapter = new SimpleAdapter(fullList);
        mAdapter = new SimpleSectionedRecyclerViewAdapter(R.layout.list_item_location, android.R.id.text1, apiAdapter);
        mAdapter.setSections(sections);
        mList.setAdapter(mAdapter);
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown   If true, the list view is shown; if false, the progress
     *                indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     *                new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        mRefreshLayout.setRefreshing(false);
        if (BuildConfig.DEBUG) Log.d(TAG, "Setting list shown");

        String msg = (mList.getAdapter() == null || mList.getAdapter().getItemCount() == 0)
                ? getString(R.string.unable_to_load_data)
                : getString(R.string.data_loaded) + " " + getTimestamp();
        Snackbar.make(mRefreshLayout, msg, Snackbar.LENGTH_SHORT).show();

        if ((mListContainer.getVisibility() == View.VISIBLE) == shown) {
            return;
        }
        if (shown) {
            if (animate) {
//                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
//                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
//                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
//            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
//                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
//                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
//                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
//            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);

        }
    }

    public interface DataListener {
        void onDataReady(List<Api> list);

        void onError();
    }

    private class FetchDataTask extends AsyncTask<String, Void, List<Api>> {

        private DataListener mListener;

        public FetchDataTask(DataListener listener) {
            mListener = listener;
        }

        @Override
        protected List<Api> doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            InputStreamReader reader = null;
            String dataSource = params[0];
            List<Api> list = null;
            FileOutputStream fileOutputStream = null;
            ByteArrayOutputStream byteBuffer = null;
            try {

                URL url = new URL(dataSource);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String inputStreamString = new Scanner(in).useDelimiter("\\A").next();

                list = new Gson().fromJson(inputStreamString, new TypeToken<ArrayList<Api>>() {
                }.getType());

                fileOutputStream = getActivity().openFileOutput("haze.json", Context.MODE_PRIVATE);
                fileOutputStream.write(inputStreamString.getBytes());


            } catch (IOException e) {
                Crashlytics.getInstance().core.logException(e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Crashlytics.getInstance().core.logException(e);
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (byteBuffer != null) {
                    try {
                        byteBuffer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<Api> list) {
            if (mListener != null) {
                if (list == null) {
                    mListener.onError();
                } else {
                    mListener.onDataReady(list);
                }
            }
        }
    }

    private class LoadDataTask extends AsyncTask<String, Void, List<Api>> {

        private DataListener mListener;

        public LoadDataTask(DataListener listener) {
            mListener = listener;
        }

        @Override
        protected List<Api> doInBackground(String... params) {
            List<Api> list = null;
            FileInputStream fileInputStream = null;
            InputStreamReader inputStreamReader = null;
            try {
                fileInputStream = getActivity().openFileInput("haze.json");
                inputStreamReader = new InputStreamReader(fileInputStream);
                list = new Gson().fromJson(inputStreamReader, new TypeToken<ArrayList<Api>>() {
                }.getType());

            } catch (FileNotFoundException e) {
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

        @Override
        protected void onCancelled() {
            mListener = null;
        }

        @Override
        protected void onPostExecute(List<Api> list) {
            if (mListener != null) {
                if (list == null) {
                    mListener.onError();
                } else {
                    mListener.onDataReady(list);
                }
            }
        }
    }
}
