package net.swiftzer.eric.multilevelappbar;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int LIST_SIZE = 30;
    private static final int ANIMATION_DURATION = 600;
    private static final float COLLAPSE_SCROLL_THRESHOLD = 30;

    private AppBarLayout mAppBarLayout;

    private NestedScrollView mHeaderScrollView;
    private ImageView mHeaderImageView;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView mDetailTextView;
    private CoordinatorLayout.LayoutParams mAppBarLayoutParams;
    private ValueAnimator mCollapsingToolbarAnimator;
    private ValueAnimator mToolbarContentAnimator;
    private GestureDetectorCompat mCollapsingToolbarGestureDetector;
    private GestureDetectorCompat mScrollViewGestureDetector;
    private GestureDetectorCompat mRecyclerViewGestureDetector;
    /**
     * Data for the RecyclerView.
     */
    private String[] listData = new String[LIST_SIZE];
    private int mHeaderImageHeight;
    private boolean mScrollViewScrolledToBottom;
    private boolean isHeaderAnimating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bindings
        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        final ImageView blurredBackground = (ImageView) findViewById(R.id.blurred_background);
        mHeaderScrollView = (NestedScrollView) findViewById(R.id.header_scrollview);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mAppBarLayoutParams = ((CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams());
        mHeaderImageView = (ImageView) findViewById(R.id.header_image);
        mDetailTextView = (TextView) findViewById(R.id.detail_text);

        // Gesture detectors
        mCollapsingToolbarGestureDetector = new GestureDetectorCompat(this, new CollapsingToolbarGestureListener());
        mScrollViewGestureDetector = new GestureDetectorCompat(this, new ScrollViewGestureListener());
        mRecyclerViewGestureDetector = new GestureDetectorCompat(this, new RecyclerViewGestureListener());

        // Use ToolBar as ActionBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Force snapping when dragging inside the AppBarLayout
        AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(AppBarLayout appBarLayout) {
                return false;
            }
        });
        mAppBarLayoutParams.setBehavior(behavior);

        // Get the height of the resized header image that is placed in PercentRelativeLayout and
        // resize the AppBar
        ViewTreeObserver vto = mHeaderImageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                // Find out the image height
                mHeaderImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                mHeaderImageHeight = mHeaderImageView.getMeasuredHeight();
                // Resize
                mAppBarLayoutParams.height = mHeaderImageHeight;
                mAppBarLayout.requestLayout();
                return true;
            }
        });

        // Collapsing toolbar
        collapsingToolbarLayout.setTitleEnabled(false);
        collapsingToolbarLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mCollapsingToolbarGestureDetector.onTouchEvent(event);
            }
        });

        // Header ScrollView (the info text)
        mHeaderScrollView.setNestedScrollingEnabled(false);
        mHeaderScrollView.setVisibility(View.GONE);
        mHeaderScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // scrolled to end?
                mScrollViewScrolledToBottom = isScrolledToBottom(v, scrollY);
            }
        });
        mHeaderScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScrollViewGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        // Blur background
        ViewGroup.LayoutParams blurredImageParams = blurredBackground.getLayoutParams();
        blurredImageParams.height = getFullyExpandedToolbarHeight();
        blurredBackground.setLayoutParams(blurredImageParams);
        blurredBackground.setVisibility(View.VISIBLE);
        Bitmap blurredBitmap = blurImage(BitmapFactory.decodeResource(getResources(), R.drawable.leaf));
        blurredBackground.setImageBitmap(blurredBitmap);

        // Recycler view
        // Create the dummy data for the RecyclerView
        for (int i = 0; i < LIST_SIZE; i++) {
            listData[i] = "Item " + Integer.toString(i);
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(new RecyclerViewAdapter());
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mAppBarLayout.getBottom() - mHeaderImageView.getTop() == mHeaderImageHeight) {
                    // App bar is semi-expanded, scroll up will fully expand the app bar
                    mRecyclerViewGestureDetector.onTouchEvent(event);
                    return false;
                } else if (mAppBarLayoutParams.height >= getFullyExpandedToolbarHeight()) {
                    // App bar is fully expanded, touching the recycler view should fold the app bar
                    if (!isHeaderAnimating) {
                        executeCollapseToSemiExpandedToolbar();
                    }
                    return true;
                } else {
                    return false;
                }
            }

        });
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                // disable scrolling when the app bar is expanding/contracting
                return isHeaderAnimating;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Quick return footer
        Button longTextButton = (Button) findViewById(R.id.long_text_button);
        Button shortTextButton = (Button) findViewById(R.id.short_text_button);
        longTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDetailTextView.setText(getString(R.string.large_text));
            }
        });
        shortTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDetailTextView.setText(getString(R.string.short_text));
            }
        });
    }

    /**
     * Scale down bitmap so that the image looks blurred when placed in ImageView as background.
     * <p/>
     * You can use RenderScript to blur image for better result.
     *
     * @param src source bitmap
     * @return scaled down bitmap
     */
    private Bitmap blurImage(Bitmap src) {
        int oldWidth = src.getWidth();
        int oldHeight = src.getHeight();
        float scaleWidth = 10 / (float) oldWidth;
        float scaleHeight = 10 / (float) oldHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(src, 0, 0, oldWidth, oldHeight, matrix, true);
    }

    /**
     * Check whether the NestedScrollView in header is scrolled to bottom.
     *
     * @param v
     * @param scrollY
     * @return true if it is scrolled to bottom
     */
    private boolean isScrolledToBottom(NestedScrollView v, int scrollY) {
        return v.getChildAt(0).getHeight() <= (v.getHeight() + scrollY - getResources().getDimensionPixelOffset(R.dimen.header_status_bar_margin));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem infoItem = menu.findItem(R.id.action_info);
        if (mAppBarLayoutParams.height >= getFullyExpandedToolbarHeight()) {
            // fully expanded
            infoItem.setVisible(false);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        } else {
            infoItem.setVisible(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Use back arrow icon or close icon
                if (mAppBarLayoutParams.height >= getFullyExpandedToolbarHeight()) {
                    if (!isHeaderAnimating) {
                        executeCollapseToSemiExpandedToolbar();
                    }
                } else {
                    Toast.makeText(this, "Back clicked!", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_info:
                // Hide the info icon when it is fully expanded
                if (!isHeaderAnimating) {
                    executeFullyExpandToolbar();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private int getFullyExpandedToolbarHeight() {
        return getResources().getDisplayMetrics().heightPixels - getResources().getDimensionPixelOffset(R.dimen.fully_expanded_app_bar_margin);
    }

    /**
     * Expand the toolbar from semi-expanded to fully expanded.
     */
    private void executeFullyExpandToolbar() {
        // Toolbar
        if (mCollapsingToolbarAnimator != null && mCollapsingToolbarAnimator.isRunning()) {
            mCollapsingToolbarAnimator.cancel();
        }
        mCollapsingToolbarAnimator = ValueAnimator.ofInt(mAppBarLayoutParams.height, getFullyExpandedToolbarHeight());
        mCollapsingToolbarAnimator.setDuration(ANIMATION_DURATION);
        mCollapsingToolbarAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mCollapsingToolbarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAppBarLayoutParams.height = (int) animation.getAnimatedValue();
                mAppBarLayout.requestLayout();
            }
        });
        mCollapsingToolbarAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isHeaderAnimating = true;
                mLayoutManager.scrollToPosition(0);
                if (mAppBarLayout.getHeight() - mAppBarLayout.getBottom() != 0) {
                    mAppBarLayout.setExpanded(true, false);
                }
                mHeaderScrollView.scrollTo(0, 0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHeaderScrollView.setVisibility(View.VISIBLE);
                supportInvalidateOptionsMenu();
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mHeaderScrollView.setVisibility(View.GONE);
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        // Content
        if (mToolbarContentAnimator != null && mToolbarContentAnimator.isRunning()) {
            mToolbarContentAnimator.cancel();
        }
        mToolbarContentAnimator = ValueAnimator.ofFloat(0, 1);
        mToolbarContentAnimator.setDuration(ANIMATION_DURATION);
        mToolbarContentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mHeaderScrollView.setAlpha((float) animation.getAnimatedValue());
                mHeaderImageView.setImageAlpha((int) ((1f - (float) animation.getAnimatedValue()) * 255));
            }
        });
        mToolbarContentAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isHeaderAnimating = true;
                mHeaderImageView.setVisibility(View.VISIBLE);
                mHeaderScrollView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHeaderScrollView.setVisibility(View.VISIBLE);
                mHeaderImageView.setVisibility(View.GONE);
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mHeaderScrollView.setAlpha(0f);
                mHeaderScrollView.setVisibility(View.GONE);
                mHeaderImageView.setImageAlpha(255);
                mHeaderImageView.setVisibility(View.VISIBLE);
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        // Start
        mCollapsingToolbarAnimator.start();
        mToolbarContentAnimator.start();
    }

    /**
     * Collapse the toolbar from fully expanded to semi-expanded.
     */
    private void executeCollapseToSemiExpandedToolbar() {
        // Toolbar
        if (mCollapsingToolbarAnimator != null && mCollapsingToolbarAnimator.isRunning()) {
            mCollapsingToolbarAnimator.cancel();
        }
        mCollapsingToolbarAnimator = ValueAnimator.ofInt(getFullyExpandedToolbarHeight(), mHeaderImageHeight);
        mCollapsingToolbarAnimator.setDuration(ANIMATION_DURATION);
        mCollapsingToolbarAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mCollapsingToolbarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAppBarLayoutParams.height = (int) animation.getAnimatedValue();
                mAppBarLayout.requestLayout();
            }
        });

        // Content
        if (mToolbarContentAnimator != null && mToolbarContentAnimator.isRunning()) {
            mToolbarContentAnimator.cancel();
        }
        mToolbarContentAnimator = ValueAnimator.ofFloat(1, 0);
        mToolbarContentAnimator.setDuration(ANIMATION_DURATION);
        mToolbarContentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mHeaderScrollView.setAlpha((float) animation.getAnimatedValue());
                mHeaderImageView.setImageAlpha((int) ((1f - (float) animation.getAnimatedValue()) * 255));
            }
        });
        mToolbarContentAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isHeaderAnimating = true;
                mHeaderImageView.setVisibility(View.VISIBLE);
                mHeaderScrollView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHeaderScrollView.setVisibility(View.GONE);
                supportInvalidateOptionsMenu();
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mHeaderScrollView.setAlpha(1);
                mHeaderScrollView.setVisibility(View.VISIBLE);
                mHeaderImageView.setImageAlpha(0);
                mHeaderImageView.setVisibility(View.GONE);
                isHeaderAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        // Start
        mCollapsingToolbarAnimator.start();
        mToolbarContentAnimator.start();
    }

    /**
     * The recycler view adapter.
     */
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((ItemViewHolder) holder).bind(listData[position]);
        }

        @Override
        public int getItemCount() {
            return listData.length;
        }
    }

    /**
     * Recycler view view holder.
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }

        public void bind(String title) {
            textView.setText(title);
        }
    }

    /**
     * Detect scroll up event when the app bar is semi-expanded and fully expand the app bar.
     */
    class RecyclerViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "RecyclerViewGestureListener onScroll " + distanceY);
            if (distanceY < 0 && !isHeaderAnimating) {
                executeFullyExpandToolbar();
            }
            return true;
        }
    }

    /**
     * Detect gesture when the app bar is semi-expanded.
     */
    class CollapsingToolbarGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "CollapsingToolbarGestureListener onSingleTapConfirmed");
            if (isHeaderAnimating) {
                return true;
            }
            // Semi-expanded, fully expand the app bar
            if (mAppBarLayoutParams.height == mHeaderImageHeight) {
                executeFullyExpandToolbar();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "CollapsingToolbarGestureListener onScroll " + distanceY);
            if (isHeaderAnimating) {
                return true;
            }
            if (mAppBarLayoutParams.height == mHeaderImageHeight) {
                // Semi-expanded
                if (distanceY > 0) {
                    // scroll down, collapse the app bar
                    mAppBarLayout.setExpanded(false, true);
                } else if (distanceY < 0) {
                    // scroll up, fully expand the app bar
                    executeFullyExpandToolbar();
                }
            }
            return true;
        }
    }

    /**
     * Detect gesture in the detail text nested scroll view. When the text is scrolled to bottom and
     * scrolled for second time, it will collapse.
     */
    class ScrollViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        private int mScrollDownCounter;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "ScrollViewGestureListener onScroll " + distanceY);
            if (isHeaderAnimating) {
                return true;
            }
            if (distanceY >= COLLAPSE_SCROLL_THRESHOLD && mScrollViewScrolledToBottom ||
                    distanceY >= COLLAPSE_SCROLL_THRESHOLD && mHeaderScrollView.getHeight() >= mHeaderScrollView.getChildAt(0).getHeight()) {
                // scroll down
                mScrollDownCounter++;
                if (mScrollDownCounter >= 2) {
                    mScrollDownCounter = 0;
                    mScrollViewScrolledToBottom = false;
                    executeCollapseToSemiExpandedToolbar();
                }
            } else {
                mScrollDownCounter = 0;
            }
            return true;
        }
    }
}
