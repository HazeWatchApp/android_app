package me.ebernie.mapi.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

public class FixedSwipeRefreshLayout extends SwipeRefreshLayout {

    private View scrollableView;

    public FixedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollableView(View recyclerView) {
        this.scrollableView = recyclerView;
    }

    @Override
    public boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(scrollableView, -1);
    }
}
