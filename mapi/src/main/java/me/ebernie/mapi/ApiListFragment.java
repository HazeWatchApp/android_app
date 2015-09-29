package me.ebernie.mapi;


import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.google.android.gms.location.LocationListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.ebernie.mapi.adapter.GridSpacingItemDecoration;
import me.ebernie.mapi.adapter.SimpleAdapter;
import me.ebernie.mapi.adapter.SimpleSectionedRecyclerViewAdapter;
import me.ebernie.mapi.model.Api;
import me.ebernie.mapi.util.ApiListLoader;
import me.ebernie.mapi.util.DistanceComparator;
import me.ebernie.mapi.util.LocationUtil;
import me.ebernie.mapi.util.MultiMap;
import me.ebernie.mapi.util.PrefUtil;
import me.ebernie.mapi.widget.EmptyRecyclerView;
import me.ebernie.mapi.widget.MultiSwipeRefreshLayout;
import my.codeandroid.hazewatch.BuildConfig;
import my.codeandroid.hazewatch.R;

public class ApiListFragment extends Fragment implements LocationListener,
        LoaderManager.LoaderCallbacks<List<Api>> {

    public static final String DATE_FORMAT = "d MMMM yyyy, EEEE";
    private static final String TAG = ApiListFragment.class.getName();

    private static final float DISTANCE_DELTA_METERS = 1000f;

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
    GridSpacingItemDecoration mDecorator;

    @Nullable
    private Location mLastLocation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        int columnCount = getResources().getInteger(R.integer.num_cols);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        mList.addItemDecoration(new GridSpacingItemDecoration(pad));
        mList.setLayoutManager(new GridLayoutManager(getContext(), columnCount));
        mList.setEmptyView(mEmpty);
        mList.setAdapter(new SimpleAdapter(new ArrayList<Api>()));

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (BuildConfig.DEBUG) Log.d(TAG, "Manually refreshing");
                getLoaderManager().initLoader(0, null, ApiListFragment.this);
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

        getLoaderManager().initLoader(0, null, this);
        LocationUtil.addLocationListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        LocationUtil.addLocationListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mLastLocation == null || mLastLocation.distanceTo(location) >= DISTANCE_DELTA_METERS) {
                mLastLocation = location;
                getLoaderManager().initLoader(0, null, this);
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

        initAdapter(fullList, sections);
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

        initAdapter(fullList, sections);
    }

    private void initAdapter(List<Api> fullList, List<SimpleSectionedRecyclerViewAdapter.Section> sections) {
        SimpleAdapter apiAdapter = new SimpleAdapter(fullList);
        mAdapter = new SimpleSectionedRecyclerViewAdapter(R.layout.list_item_location, android.R.id.text1, apiAdapter);
        mAdapter.setSections(sections);
        if (mList.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager lm = (GridLayoutManager) mList.getLayoutManager();
            GridSpanUpdater spanUpdater = new GridSpanUpdater(lm.getSpanCount(), mAdapter);
            lm.setSpanSizeLookup(spanUpdater);
        }
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
        if (BuildConfig.DEBUG) Log.d(TAG, "Setting list shown");

        if (mRefreshLayout.isRefreshing()) {
            String msg = (mList.getAdapter() == null || mList.getAdapter().getItemCount() == 0)
                    ? getString(R.string.unable_to_load_data)
                    : getString(R.string.data_loaded);
            Snackbar.make(mRefreshLayout, msg, Snackbar.LENGTH_SHORT).show();
        }
        mRefreshLayout.setRefreshing(false);

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

    @Override
    public Loader<List<Api>> onCreateLoader(int id, Bundle args) {
        return new ApiListLoader(getContext(), getString(R.string.data_source));
    }

    @Override
    public void onLoadFinished(Loader<List<Api>> loader, List<Api> data) {
        if (mLastLocation == null) {
            sortByAlphabet(data);
        } else {
            sortForDistance(data, mLastLocation);
        }

        Date lastUpdate = PrefUtil.getLastUpdate(getActivity());
        if (lastUpdate == null) {
            lastUpdate = new Date();
        }
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                .format(lastUpdate);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(date);

        setListShown(true, isResumed());
    }

    @Override
    public void onLoaderReset(Loader<List<Api>> loader) {

    }

    public static class GridSpanUpdater extends GridLayoutManager.SpanSizeLookup {
        private int columnCount;
        private SimpleSectionedRecyclerViewAdapter adapter;

        public GridSpanUpdater(int columnCount, SimpleSectionedRecyclerViewAdapter adapter) {

            this.columnCount = columnCount;
            this.adapter = adapter;

            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            if (columnCount > 1 && adapter.isSectionHeaderPosition(position)) {
                return columnCount;
            }

            return 1;
        }
    }

}
