package me.ebernie.mapi.adapter;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.ebernie.mapi.model.Api;
import me.ebernie.mapi.model.Datum;
import my.codeandroid.hazewatch.R;

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {

    private List<Api> mData;

    public SimpleAdapter(List<Api> data) {
        mData = data;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_api;
    }

    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new SimpleViewHolder(view) {
            @Override
            public void onExpand(Chart chart, int position) {
                Api api = mData.get(position);
                List<Datum> data = api.getData();

                final int size = data.size();
                List<String> xVals = new ArrayList<>(size);
                List<Entry> yVals = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
                    Datum datum = data.get(i);
                    xVals.add(datum.getTime());
                    if (!TextUtils.isEmpty(datum.getIndex())) {
                        yVals.add(new Entry(Float.valueOf(datum.getIndex()), i));
                    }
                }

                ScatterDataSet set1 = new ScatterDataSet(yVals, null);
                set1.setColor(Color.WHITE);
//                set1.setCircleColor(Color.WHITE);
                set1.setDrawValues(false);
                set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

                // create a data object with the datasets
//                LineData lineData = new LineData(xVals, set1);
                ScatterData lineData = new ScatterData(xVals, set1);

                // set data
                chart.setData(lineData);
                chart.getLegend().setEnabled(false);
                chart.setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, final int position) {
        Api api = mData.get(position);
        String index = api.getLatest().getIndex();
        holder.mTownArea.setText(api.getArea());
        holder.mCurIndex.setText(index);
        holder.mCardView.setCardBackgroundColor(fetchColorForApiIndex(Integer.parseInt(index)));
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

    public static abstract class SimpleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CardView mCardView;

        @Bind(R.id.town_area)
        TextView mTownArea;
        @Bind(R.id.curIndex)
        TextView mCurIndex;

        @Bind(R.id.chart)
        ScatterChart mChart;

        public SimpleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mCardView = (CardView) view;

            mChart.setDescription(null);
            mChart.setDrawBorders(false);
            mChart.setDrawGridBackground(false);
            mChart.setTouchEnabled(false);

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setStartAtZero(false);
            leftAxis.setLabelCount(3, false);
            mChart.getAxisRight().setEnabled(false);
            mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setLabelsToSkip(10);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mChart.getVisibility() == View.VISIBLE) {
                mChart.setVisibility(View.GONE);
            } else {
                onExpand(mChart, getAdapterPosition());
            }
        }

        public abstract void onExpand(Chart chart, int position);
    }
}
