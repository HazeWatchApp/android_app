package me.ebernie.mapi;


import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.ebernie.mapi.adapter.SimpleAdapter;
import me.ebernie.mapi.adapter.SimpleSectionedRecyclerViewAdapter;
import me.ebernie.mapi.adapter.SpacesItemDecoration;
import me.ebernie.mapi.model.Api;
import me.ebernie.mapi.util.MultiMap;
import me.ebernie.mapi.widget.EmptyRecyclerView;
import my.codeandroid.hazewatch.R;

/**
 * Created by andhie on 9/19/15.
 */
public class ApiListFragment2 extends Fragment {

    public static ApiListFragment2 newInstance() {
        return new ApiListFragment2();
    }

    @Bind(R.id.listContainer)
    View mListContainer;
    @Bind(R.id.progressContainer)
    View mProgressContainer;

    @Bind(android.R.id.list)
    EmptyRecyclerView mList;
    @Bind(android.R.id.empty)
    View mEmpty;

    SimpleSectionedRecyclerViewAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_list2, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        String date = new SimpleDateFormat("d MMMM yyyy, EEEE")
                .format(new Date());
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(date);

        mList.setLayoutManager(new LinearLayoutManager(getActivity()));
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        mList.addItemDecoration(new SpacesItemDecoration(new Rect(pad, pad, pad, pad)));
        mList.setEmptyView(mEmpty);
        mList.setAdapter(new SimpleAdapter(new ArrayList<Api>()));

        FetchDataTask fetchDataTask = new FetchDataTask(new FetchDataTask.DataListener() {
            @Override
            public void onDataReady(List<Api> list) {
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

                setListShown(true, isResumed());
            }

            @Override
            public void onError() {
                setListShown(true, isResumed());
            }
        });

        AsyncTaskCompat.executeParallel(fetchDataTask);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
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
        if ((mListContainer.getVisibility() == View.VISIBLE) == shown) {
            return;
        }
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    private static class FetchDataTask extends AsyncTask<Void, Void, List<Api>> {

        public interface DataListener {
            public void onDataReady(List<Api> list);

            public void onError();
        }

        private DataListener mListener;

        public FetchDataTask(DataListener listener) {
            mListener = listener;
        }

        @Override
        protected List<Api> doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            InputStreamReader reader = null;
            List<Api> list = null;

            try {
                URL url = new URL("https://storage.googleapis.com/hazewatchapp.com/apims_data/index.json");
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                reader = new InputStreamReader(in);

                list = new Gson().fromJson(reader, new TypeToken<ArrayList<Api>>() {
                }.getType());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
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
}