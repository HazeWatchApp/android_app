package me.ebernie.mapi.adapter;

import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

    private Rect rect;

    public SpacesItemDecoration(Rect rect) {
        this.rect = rect;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        LinearLayoutManager llm = (LinearLayoutManager) parent.getLayoutManager();
        if (llm.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            // Add left margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0) {
                if (llm.getReverseLayout()) {
                    outRect.right = rect.right;
                } else {
                    outRect.left = rect.left;
                }
            }

            outRect.top = rect.top;
            outRect.bottom = rect.bottom;

            if (llm.getReverseLayout()) {
                outRect.left = rect.left;
            } else {
                outRect.right = rect.right;
            }

        } else {
            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0) {
                if (llm.getReverseLayout()) {
                    outRect.bottom = rect.bottom;
                } else {
                    outRect.top = rect.top;
                }
            }

            outRect.left = rect.left;
            outRect.right = rect.right;

            if (llm.getReverseLayout()) {
                outRect.top = rect.top;

            } else {
                outRect.bottom = rect.bottom;
            }
        }
    }

}
