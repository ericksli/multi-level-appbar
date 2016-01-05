package net.swiftzer.eric.multilevelappbar;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;


/**
 * Control the visibility of the Floating Action Button.
 */
public class FABBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
    private Context mContext;

    public FABBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            updateFabVisibility(parent, (AppBarLayout) dependency, child);
        }
        return false;
    }

    private boolean updateFabVisibility(CoordinatorLayout parent,
                                        AppBarLayout appBarLayout, FloatingActionButton child) {
        Toolbar toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);
        int[] toolbarCoords = new int[2];
        // Get the toolbar coordinates to know the status bar height
        toolbar.getLocationOnScreen(toolbarCoords);

        if (appBarLayout.getBottom() - toolbarCoords[1] <= 2 * toolbar.getHeight() || appBarLayout.getBottom() >= getFullyExpandedToolbarHeight() - child.getHeight()) {
            // If the anchor's bottom is below the seam or fully expanded, we'll animate our FAB out
            child.hide();
        } else {
            // Else, we'll animate our FAB back in
            child.show();
        }
        return true;
    }

    private int getFullyExpandedToolbarHeight() {
        return mContext.getResources().getDisplayMetrics().heightPixels - mContext.getResources().getDimensionPixelOffset(R.dimen.fully_expanded_app_bar_margin);
    }

}
