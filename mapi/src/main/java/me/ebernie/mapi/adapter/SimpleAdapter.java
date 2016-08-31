package me.ebernie.mapi.adapter;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.support.transition.TransitionManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.ebernie.mapi.model.Api;
import me.ebernie.mapi.model.Datum;
import me.ebernie.mapi.util.AnalyticsManager;
import my.codeandroid.hazewatch.R;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {

    private List<Api> mData;

    private final SparseBooleanArray mExpandedPos;

    public SimpleAdapter(List<Api> data) {
        mData = data;
        mExpandedPos = new SparseBooleanArray(mData.size());
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_api;
    }

    public SimpleViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new SimpleViewHolder(view) {

            @Override
            public void onCollapse(Chart chart, int position) {
                mExpandedPos.put(position, false);
                TransitionManager.beginDelayedTransition(parent);
                notifyItemChanged(getAdapterPosition());

                AnalyticsManager.sendEvent(AnalyticsManager.CAT_UX,
                        AnalyticsManager.ACTION_CLICK_COLLAPSE,
                        mData.get(position).getArea(),
                        position);
            }

            @Override
            public void onExpand(Chart chart, int position) {
                mExpandedPos.put(position, true);
                TransitionManager.beginDelayedTransition(parent);
                notifyItemChanged(getAdapterPosition());

                AnalyticsManager.sendEvent(AnalyticsManager.CAT_UX,
                        AnalyticsManager.ACTION_CLICK_EXPAND,
                        mData.get(position).getArea(),
                        position);
            }
        };
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        holder.position = position;

        Api api = mData.get(position);
        String index = api.getLatest().getIndex();
        holder.mTownArea.setText(api.getArea());

        if (!TextUtils.isEmpty(index) && TextUtils.isDigitsOnly(index)) {
            holder.mCurIndex.setText(index);
            holder.mCardView.setCardBackgroundColor(fetchColorForApiIndex(Integer.parseInt(index)));
        } else {
            holder.mCurIndex.setText("--");
            holder.mCardView.setCardBackgroundColor(Color.LTGRAY);
        }

        if (mExpandedPos.get(position, false)) {
            updateChartData(holder.mChart, api);
            if (holder.mChart.getVisibility() != View.VISIBLE) {
                // should expand but is in collapsed mode, expand it
                holder.mChart.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.mChart.getVisibility() == View.VISIBLE) {
                // should be in collapsed mode but its expanded, collapse it
                holder.mChart.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    private static int fetchColorForApiIndex(int index) {
        if (index <= 50) {
            return 0xFF1397F1;
        } else if (index <= 100) {
            return 0xFF40A33F;
        } else if (index <= 200) {
            return 0xFFF6C519;

        } else if (index <= 300) {
            return 0xFFFD8508;

        } else {
            return 0xFFEE2B2A;

        }
    }

    private static void setChartVisibilityNoAnimate(ViewGroup viewGroup, Chart chart, int visibility) {
        // todo use recyclerview animation instead of layout animation
        LayoutTransition layoutTransition = viewGroup.getLayoutTransition();
        viewGroup.setLayoutTransition(null);
        chart.setVisibility(visibility);
        viewGroup.setLayoutTransition(layoutTransition);
    }

    private static void updateChartData(Chart chart, Api api) {
        List<Datum> data = api.getData();
        final int size = data.size();

        ScatterData scatterData = (ScatterData) chart.getData();
        if (scatterData == null) {
            // we dont have a chart data setup

            // create the X-axis
            List<String> xVals = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                xVals.add(data.get(i).getTime());
            }
            // style the data-points
            ScatterDataSet dataSet = new ScatterDataSet(new ArrayList<Entry>(size), null);
            dataSet.setColor(Color.WHITE);
            dataSet.setDrawValues(false);
            dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
            dataSet.setScatterShapeSize(5);

            scatterData = new ScatterData(xVals, dataSet);
            chart.setData(scatterData);
            // legend only available after setData()
            chart.getLegend().setEnabled(false);
        }

        ScatterDataSet dataSet = (ScatterDataSet) scatterData.getDataSetByIndex(0);
        ListIterator<Entry> iterator = dataSet.getYVals().listIterator();
        for (int i = 0; i < size; i++) {
            Datum datum = data.get(i);
            Entry entry;
            if (iterator.hasNext()) {
                // found an existing Entry at Time i
                entry = iterator.next();
                if (TextUtils.isEmpty(datum.getIndex())) {
                    // our data indicates we dont have value, remove existing Entry
                    iterator.remove();
                } else {
                    // we have data, just edit it with our latest value
                    entry.setVal(Float.valueOf(datum.getIndex()));
                    entry.setXIndex(i);
                }
            } else {
                if (TextUtils.isEmpty(datum.getIndex())) {
                    // we have no data and no Entry, do nothing

                } else {
                    // no Entry found, create a new one with our data
                    entry = new Entry(Float.valueOf(datum.getIndex()), i);
                    iterator.add(entry);
                }
            }
        }

        // let the chart know it's data has changed
        scatterData.notifyDataChanged();
//        dataSet.notifyDataSetChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public static abstract class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.card)
        CardView mCardView;

        @BindView(R.id.layout_container)
        ViewGroup mLayoutContainer;
        @BindView(R.id.town_area)
        TextView mTownArea;
        @BindView(R.id.curIndex)
        TextView mCurIndex;

        @BindView(R.id.chart)
        ScatterChart mChart;

        /**
         * This position will keep track of the list position as passed over by
         * {@link SimpleSectionedRecyclerViewAdapter}.
         * Using {@link RecyclerView.ViewHolder#getAdapterPosition()} will return the actual position
         * in the {@link RecyclerView} which includes {@link SimpleSectionedRecyclerViewAdapter.Section}
         */
        int position;

        public SimpleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            mChart.setDescription(null);
            mChart.setDrawBorders(false);
            mChart.setDrawGridBackground(false);
            mChart.setTouchEnabled(false);
            mChart.setNoDataText(mChart.getResources().getString(R.string.warn_no_chart_data));
            mChart.getPaint(Chart.PAINT_INFO).setColor(Color.WHITE);

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setStartAtZero(false);
            leftAxis.setLabelCount(3, false);
            leftAxis.setTextColor(Color.WHITE);
            leftAxis.setGridColor(0xD9000000); // 85pc translucent black
            leftAxis.setDrawAxisLine(false);
            mChart.getAxisRight().setEnabled(false);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setLabelsToSkip(10);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);

            view.setOnClickListener(this);

            overrideLollipopHotspot();
        }

        @Override
        public void onClick(View v) {
            if (mChart.getVisibility() == View.VISIBLE) {
                onCollapse(mChart, position);
            } else {
                onExpand(mChart, position);
            }
        }

        /**
         * Workaround for displaying correct ripple hotspot.
         * It seems to only work perfectly if its the rootview on 5.0
         * Works on 5.1
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void overrideLollipopHotspot() {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP
                    && mCardView instanceof FrameLayout) {
                itemView.setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // FrameLayout getForeground() is available since API 1
                        // but in API 23, getForeground() is now part of View
                        // this will trigger warning that it requires M
                        mCardView.getForeground()
                                .setHotspot(event.getX(), event.getY());

                        return false;
                    }
                });
            }
        }

        public abstract void onExpand(Chart chart, int position);

        public abstract void onCollapse(Chart chart, int position);
    }
}
