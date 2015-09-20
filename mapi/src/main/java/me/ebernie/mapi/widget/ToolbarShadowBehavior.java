package me.ebernie.mapi.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import my.codeandroid.hazewatch.R;

/**
 * Created by andhie on 9/19/15.
 */
@Keep
public class ToolbarShadowBehavior extends CoordinatorLayout.Behavior<Toolbar> {

    private static boolean AT_LEAST_L = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private int mMaxElevation;

    public ToolbarShadowBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaxElevation = context.getResources().getDimensionPixelSize(R.dimen.appbar_elevation);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, Toolbar child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL && AT_LEAST_L;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, Toolbar child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyUnconsumed < 0) {
            // If the scrolling view is scrolling down but not consuming, it's probably be at
            // the top of it's content
            child.setElevation(0);

        } else {
            child.setElevation(mMaxElevation);
        }

    }

}
