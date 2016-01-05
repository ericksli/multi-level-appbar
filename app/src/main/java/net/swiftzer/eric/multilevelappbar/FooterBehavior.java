package net.swiftzer.eric.multilevelappbar;

import android.animation.Animator;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Quick return behavior of the footer. Hide the footer when the app bar is fully expanded.
 */
public class FooterBehavior extends CoordinatorLayout.Behavior<View> {
    private static final String TAG = FooterBehavior.class.getSimpleName();
    private int mDySinceDirectionChange = 0;
    private Context mContext;

    public FooterBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            AppBarLayout appBarLayout = (AppBarLayout) dependency;
            if (appBarLayout.getBottom() >= getFullyExpandedToolbarHeight() - child.getHeight()) {
                hide(child);
            } else {
                show(child);
            }
        }
        return false;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if (dy > 0 && mDySinceDirectionChange < 0 || dy < 0 && mDySinceDirectionChange > 0) {
            mDySinceDirectionChange = 0;
        }
        mDySinceDirectionChange += dy;

        if (mDySinceDirectionChange > child.getHeight() && child.getVisibility() == View.VISIBLE) {
            hide(child);
        } else if (mDySinceDirectionChange < 0 && child.getVisibility() == View.GONE) {
            show(child);
        }
    }

    private void hide(final View view) {
        view.animate()
                .translationY(view.getHeight())
                .setInterpolator(new FastOutSlowInInterpolator())
                .setDuration(200)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

    private void show(final View view) {
        view.animate()
                .translationY(0)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(200)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .start();
    }

    private int getFullyExpandedToolbarHeight() {
        return mContext.getResources().getDisplayMetrics().heightPixels - mContext.getResources().getDimensionPixelOffset(R.dimen.fully_expanded_app_bar_margin);
    }
}
