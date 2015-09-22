package me.ebernie.mapi.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by andhie on 9/19/15.
 */
@Keep
public class ShadowFrameLayoutShadowBehavior extends CoordinatorLayout.Behavior<DrawShadowFrameLayout> {

    private static boolean AT_LEAST_L = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    public ShadowFrameLayoutShadowBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, DrawShadowFrameLayout child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL && !AT_LEAST_L;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, DrawShadowFrameLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyConsumed < 0) {
            // If the scrolling view is scrolling down but not consuming, it's probably be at
            // the top of it's content
            child.setShadowVisible(false, false);
        } else {
            child.setShadowVisible(true, false);
        }
    }
}
