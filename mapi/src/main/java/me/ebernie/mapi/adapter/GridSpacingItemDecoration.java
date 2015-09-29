package me.ebernie.mapi.adapter;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;


public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private final Rect rect;
    private GridLayoutManager manager;
    private GridLayoutManager.SpanSizeLookup spanSizeLookup;
    int spacing;

    public GridSpacingItemDecoration(Rect rect) {
        this.rect = rect;
        spacing = rect.left;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        if (manager != parent.getLayoutManager()) {
            manager = (GridLayoutManager) parent.getLayoutManager();
        }
        if (spanSizeLookup != manager.getSpanSizeLookup()) {
            spanSizeLookup = manager.getSpanSizeLookup();
        }
        int spanCount = spanSizeLookup.getSpanSize(position);
        int numColumn = manager.getSpanCount();

        if (numColumn > 1 && spanCount == 1) {
            int offsetFromHeader = 0;
            // calculate backwards from the card to the nearest header
            for (int i = position - 1; i >= 0; i--) {
                if (spanSizeLookup.getSpanSize(i) == 1) {
                    offsetFromHeader++;
                } else {
                    break;
                }
            }

            // column starts from 0..n
            int column = offsetFromHeader % numColumn;
            outRect.left = column * (spacing / numColumn);
            outRect.right = spacing - (column + 1) * (spacing / numColumn);
            if (offsetFromHeader >= numColumn) {
                // only add top when we are 3rd card onwards
                outRect.top = spacing;
            }
        } else if (position > 0) {
            // for pos 0, we already have recyclerview's padding
            outRect.top = spacing;
        }
    }
}
