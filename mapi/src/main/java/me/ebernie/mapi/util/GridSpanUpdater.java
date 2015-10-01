package me.ebernie.mapi.util;

import android.support.v7.widget.GridLayoutManager;

import me.ebernie.mapi.adapter.SimpleSectionedRecyclerViewAdapter;

public class GridSpanUpdater extends GridLayoutManager.SpanSizeLookup {
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
