/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.widget.drawer;

import static androidx.wear.widget.drawer.WearableDrawerView.STATE_IDLE;
import static androidx.wear.widget.drawer.WearableDrawerView.STATE_SETTLING;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.wear.widget.drawer.FlingWatcherFactory.FlingListener;
import androidx.wear.widget.drawer.FlingWatcherFactory.FlingWatcher;
import androidx.wear.widget.drawer.WearableDrawerView.DrawerState;

/**
 * Top-level container that allows interactive drawers to be pulled from the top and bottom edge of
 * the window. For WearableDrawerLayout to work properly, scrolling children must send nested
 * scrolling events. Views that implement {@link androidx.core.view.NestedScrollingChild} do
 * this by default. To enable nested scrolling on frameworks views like {@link
 * android.widget.ListView}, set <code>android:nestedScrollingEnabled="true"</code> on the view in
 * the layout file, or call {@link View#setNestedScrollingEnabled} in code. This includes the main
 * content in a WearableDrawerLayout, as well as the content inside of the drawers.
 *
 * <p>To use WearableDrawerLayout with {@link WearableActionDrawerView} or {@link
 * WearableNavigationDrawerView}, place either drawer in a WearableDrawerLayout.
 *
 * <pre>
 * &lt;androidx.wear.widget.drawer.WearableDrawerLayout [...]&gt;
 *     &lt;FrameLayout android:id=”@+id/content” /&gt;
 *
 *     &lt;androidx.wear.widget.drawer.WearableNavigationDrawerView
 *         android:layout_width=”match_parent”
 *         android:layout_height=”match_parent” /&gt;
 *
 *     &lt;androidx.wear.widget.drawer.WearableActionDrawerView
 *         android:layout_width=”match_parent”
 *         android:layout_height=”match_parent” /&gt;
 *
 * &lt;/androidx.wear.widget.drawer.WearableDrawerLayout&gt;</pre>
 *
 * <p>To use custom content in a drawer, place {@link WearableDrawerView} in a WearableDrawerLayout
 * and specify the layout_gravity to pick the drawer location (the following example is for a top
 * drawer). <b>Note:</b> You must either call {@link WearableDrawerView#setDrawerContent} and pass
 * in your drawer content view, or specify it in the {@code app:drawerContent} XML attribute.
 *
 * <pre>
 * &lt;androidx.wear.widget.drawer.WearableDrawerLayout [...]&gt;
 *     &lt;FrameLayout
 *         android:id=”@+id/content”
 *         android:layout_width=”match_parent”
 *         android:layout_height=”match_parent” /&gt;
 *
 *     &lt;androidx.wear.widget.drawer.WearableDrawerView
 *         android:layout_width=”match_parent”
 *         android:layout_height=”match_parent”
 *         android:layout_gravity=”top”
 *         app:drawerContent="@+id/top_drawer_content" &gt;
 *
 *         &lt;FrameLayout
 *             android:id=”@id/top_drawer_content”
 *             android:layout_width=”match_parent”
 *             android:layout_height=”match_parent” /&gt;
 *
 *     &lt;/androidx.wear.widget.drawer.WearableDrawerView&gt;
 * &lt;/androidx.wear.widget.drawer.WearableDrawerLayout&gt;</pre>
 */
public class WearableDrawerLayout extends FrameLayout
        implements View.OnLayoutChangeListener, NestedScrollingParent, FlingListener {

    private static final String TAG = "WearableDrawerLayout";

    /**
     * Undefined layout_gravity. This is different from {@link Gravity#NO_GRAVITY}. Follow up with
     * frameworks to find out why (b/27576632).
     */
    private static final int GRAVITY_UNDEFINED = -1;

    private static final int PEEK_FADE_DURATION_MS = 150;

    private static final int PEEK_AUTO_CLOSE_DELAY_MS = 1000;

    /**
     * The downward scroll direction for use as a parameter to canScrollVertically.
     */
    private static final int DOWN = 1;

    /**
     * The upward scroll direction for use as a parameter to canScrollVertically.
     */
    private static final int UP = -1;

    /**
     * The percent at which the drawer will be opened when the drawer is released mid-drag.
     */
    private static final float OPENED_PERCENT_THRESHOLD = 0.5f;

    /**
     * When a user lifts their finger off the screen, this may trigger a couple of small scroll
     * events. If the user is scrolling down and the final events from the user lifting their finger
     * are up, this will cause the bottom drawer to peek. To prevent this from happening, we prevent
     * the bottom drawer from peeking until this amount of scroll is exceeded. Note, scroll up
     * events are considered negative.
     */
    private static final int NESTED_SCROLL_SLOP_DP = 5;
    @VisibleForTesting final ViewDragHelper.Callback mTopDrawerDraggerCallback;
    @VisibleForTesting final ViewDragHelper.Callback mBottomDrawerDraggerCallback;
    private final int mNestedScrollSlopPx;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper =
            new NestedScrollingParentHelper(this);
    /**
     * Helper for dragging the top drawer.
     */
    private final ViewDragHelper mTopDrawerDragger;
    /**
     * Helper for dragging the bottom drawer.
     */
    private final ViewDragHelper mBottomDrawerDragger;
    private final boolean mIsAccessibilityEnabled;
    private final FlingWatcherFactory mFlingWatcher;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final ClosePeekRunnable mCloseTopPeekRunnable = new ClosePeekRunnable(Gravity.TOP);
    private final ClosePeekRunnable mCloseBottomPeekRunnable = new ClosePeekRunnable(
            Gravity.BOTTOM);
    /**
     * Top drawer view.
     */
    @Nullable private WearableDrawerView mTopDrawerView;
    /**
     * Bottom drawer view.
     */
    @Nullable private WearableDrawerView mBottomDrawerView;
    /**
     * What we have inferred the scrolling content view to be, should one exist.
     */
    @Nullable private View mScrollingContentView;
    /**
     * Listens to drawer events.
     */
    private DrawerStateCallback mDrawerStateCallback;
    private int mSystemWindowInsetBottom;
    /**
     * Tracks the amount of nested scroll in the up direction. This is used with {@link
     * #NESTED_SCROLL_SLOP_DP} to prevent false drawer peeks.
     */
    private int mCurrentNestedScrollSlopTracker;
    /**
     * Tracks whether the top drawer should be opened after layout.
     */
    private boolean mShouldOpenTopDrawerAfterLayout;
    /**
     * Tracks whether the bottom drawer should be opened after layout.
     */
    private boolean mShouldOpenBottomDrawerAfterLayout;
    /**
     * Tracks whether the top drawer should be peeked after layout.
     */
    private boolean mShouldPeekTopDrawerAfterLayout;
    /**
     * Tracks whether the bottom drawer should be peeked after layout.
     */
    private boolean mShouldPeekBottomDrawerAfterLayout;
    /**
     * Tracks whether the top drawer is in a state where it can be closed. The content in the drawer
     * can scroll, and {@link #mTopDrawerDragger} should not intercept events unless the top drawer
     * is scrolled to the bottom of its content.
     */
    private boolean mCanTopDrawerBeClosed;
    /**
     * Tracks whether the bottom drawer is in a state where it can be closed. The content in the
     * drawer can scroll, and {@link #mBottomDrawerDragger} should not intercept events unless the
     * bottom drawer is scrolled to the top of its content.
     */
    private boolean mCanBottomDrawerBeClosed;
    /**
     * Tracks whether the last scroll resulted in a fling. Fling events do not contain the amount
     * scrolled, which makes it difficult to determine when to unlock an open drawer. To work around
     * this, if the last scroll was a fling and the next scroll unlocks the drawer, pass {@link
     * #mDrawerOpenLastInterceptedTouchEvent} to {@link #onTouchEvent} to start the drawer.
     */
    private boolean mLastScrollWasFling;
    /**
     * The last intercepted touch event. See {@link #mLastScrollWasFling} for more information.
     */
    private MotionEvent mDrawerOpenLastInterceptedTouchEvent;

    public WearableDrawerLayout(Context context) {
        this(context, null);
    }

    public WearableDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearableDrawerLayout(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mFlingWatcher = new FlingWatcherFactory(this);
        mTopDrawerDraggerCallback = new TopDrawerDraggerCallback();
        mTopDrawerDragger =
                ViewDragHelper.create(this, 1f /* sensitivity */, mTopDrawerDraggerCallback);
        mTopDrawerDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_TOP);

        mBottomDrawerDraggerCallback = new BottomDrawerDraggerCallback();
        mBottomDrawerDragger =
                ViewDragHelper.create(this, 1f /* sensitivity */, mBottomDrawerDraggerCallback);
        mBottomDrawerDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);

        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mNestedScrollSlopPx = Math.round(metrics.density * NESTED_SCROLL_SLOP_DP);

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mIsAccessibilityEnabled = accessibilityManager.isEnabled();
    }

    private static void animatePeekVisibleAfterBeingClosed(WearableDrawerView drawer) {
        final View content = drawer.getDrawerContent();
        if (content != null) {
            content.animate()
                    .setDuration(PEEK_FADE_DURATION_MS)
                    .alpha(0)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    content.setVisibility(GONE);
                                }
                            })
                    .start();
        }

        ViewGroup peek = drawer.getPeekContainer();
        peek.setVisibility(VISIBLE);
        peek.animate()
                .setStartDelay(PEEK_FADE_DURATION_MS)
                .setDuration(PEEK_FADE_DURATION_MS)
                .alpha(1)
                .scaleX(1)
                .scaleY(1)
                .start();

        drawer.setIsPeeking(true);
    }

    /**
     * Shows the drawer's contents. If the drawer is peeking, an animation is used to fade out the
     * peek view and fade in the drawer content.
     */
    private static void showDrawerContentMaybeAnimate(WearableDrawerView drawerView) {
        drawerView.bringToFront();
        final View contentView = drawerView.getDrawerContent();
        if (contentView != null) {
            contentView.setVisibility(VISIBLE);
        }

        if (drawerView.isPeeking()) {
            final View peekView = drawerView.getPeekContainer();
            peekView.animate().alpha(0).scaleX(0).scaleY(0).setDuration(PEEK_FADE_DURATION_MS)
                    .start();

            if (contentView != null) {
                contentView.setAlpha(0);
                contentView
                        .animate()
                        .setStartDelay(PEEK_FADE_DURATION_MS)
                        .alpha(1)
                        .setDuration(PEEK_FADE_DURATION_MS)
                        .start();
            }
        } else {
            drawerView.getPeekContainer().setAlpha(0);
            if (contentView != null) {
                contentView.setAlpha(1);
            }
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mSystemWindowInsetBottom = insets.getSystemWindowInsetBottom();

        if (mSystemWindowInsetBottom != 0) {
            MarginLayoutParams layoutParams = (MarginLayoutParams) getLayoutParams();
            layoutParams.bottomMargin = mSystemWindowInsetBottom;
            setLayoutParams(layoutParams);
        }

        return super.onApplyWindowInsets(insets);
    }

    /**
     * Closes drawer after {@code delayMs} milliseconds.
     */
    private void closeDrawerDelayed(final int gravity, long delayMs) {
        switch (gravity) {
            case Gravity.TOP:
                mMainThreadHandler.removeCallbacks(mCloseTopPeekRunnable);
                mMainThreadHandler.postDelayed(mCloseTopPeekRunnable, delayMs);
                break;
            case Gravity.BOTTOM:
                mMainThreadHandler.removeCallbacks(mCloseBottomPeekRunnable);
                mMainThreadHandler.postDelayed(mCloseBottomPeekRunnable, delayMs);
                break;
            default:
                Log.w(TAG, "Invoked a delayed drawer close with an invalid gravity: " + gravity);
        }
    }

    /**
     * Close the specified drawer by animating it out of view.
     *
     * @param gravity Gravity.TOP to move the top drawer or Gravity.BOTTOM for the bottom.
     */
    void closeDrawer(int gravity) {
        closeDrawer(findDrawerWithGravity(gravity));
    }

    /**
     * Close the specified drawer by animating it out of view.
     *
     * @param drawer The drawer view to close.
     */
    void closeDrawer(WearableDrawerView drawer) {
        if (drawer == null) {
            return;
        }
        if (drawer == mTopDrawerView) {
            mTopDrawerDragger.smoothSlideViewTo(
                    mTopDrawerView, 0 /* finalLeft */, -mTopDrawerView.getHeight());
            invalidate();
        } else if (drawer == mBottomDrawerView) {
            mBottomDrawerDragger
                    .smoothSlideViewTo(mBottomDrawerView, 0 /* finalLeft */, getHeight());
            invalidate();
        } else {
            Log.w(TAG, "closeDrawer(View) should be passed in the top or bottom drawer");
        }
    }

    /**
     * Open the specified drawer by animating it into view.
     *
     * @param gravity Gravity.TOP to move the top drawer or Gravity.BOTTOM for the bottom.
     */
    void openDrawer(int gravity) {
        if (!isLaidOut()) {
            switch (gravity) {
                case Gravity.TOP:
                    mShouldOpenTopDrawerAfterLayout = true;
                    break;
                case Gravity.BOTTOM:
                    mShouldOpenBottomDrawerAfterLayout = true;
                    break;
                default: // fall out
            }
            return;
        }
        openDrawer(findDrawerWithGravity(gravity));
    }

    /**
     * Open the specified drawer by animating it into view.
     *
     * @param drawer The drawer view to open.
     */
    void openDrawer(WearableDrawerView drawer) {
        if (drawer == null) {
            return;
        }
        if (!isLaidOut()) {
            if (drawer == mTopDrawerView) {
                mShouldOpenTopDrawerAfterLayout = true;
            } else if (drawer == mBottomDrawerView) {
                mShouldOpenBottomDrawerAfterLayout = true;
            }
            return;
        }

        if (drawer == mTopDrawerView) {
            mTopDrawerDragger
                    .smoothSlideViewTo(mTopDrawerView, 0 /* finalLeft */, 0 /* finalTop */);
            showDrawerContentMaybeAnimate(mTopDrawerView);
            invalidate();
        } else if (drawer == mBottomDrawerView) {
            mBottomDrawerDragger.smoothSlideViewTo(
                    mBottomDrawerView, 0 /* finalLeft */,
                    getHeight() - mBottomDrawerView.getHeight());
            showDrawerContentMaybeAnimate(mBottomDrawerView);
            invalidate();
        } else {
            Log.w(TAG, "openDrawer(View) should be passed in the top or bottom drawer");
        }
    }

    /**
     * Peek the drawer.
     *
     * @param gravity {@link Gravity#TOP} to peek the top drawer or {@link Gravity#BOTTOM} to peek
     * the bottom drawer.
     */
    void peekDrawer(final int gravity) {
        if (!isLaidOut()) {
            // If this view is not laid out yet, postpone the peek until onLayout is called.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "WearableDrawerLayout not laid out yet. Postponing peek.");
            }
            switch (gravity) {
                case Gravity.TOP:
                    mShouldPeekTopDrawerAfterLayout = true;
                    break;
                case Gravity.BOTTOM:
                    mShouldPeekBottomDrawerAfterLayout = true;
                    break;
                default: // fall out
            }
            return;
        }
        final WearableDrawerView drawerView = findDrawerWithGravity(gravity);
        maybePeekDrawer(drawerView);
    }

    /**
     * Peek the given {@link WearableDrawerView}, which may either be the top drawer or bottom
     * drawer. This should only be used after the drawer has been added as a child of the {@link
     * WearableDrawerLayout}.
     */
    void peekDrawer(WearableDrawerView drawer) {
        if (drawer == null) {
            throw new IllegalArgumentException(
                    "peekDrawer(WearableDrawerView) received a null drawer.");
        } else if (drawer != mTopDrawerView && drawer != mBottomDrawerView) {
            throw new IllegalArgumentException(
                    "peekDrawer(WearableDrawerView) received a drawer that isn't a child.");
        }

        if (!isLaidOut()) {
            // If this view is not laid out yet, postpone the peek until onLayout is called.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "WearableDrawerLayout not laid out yet. Postponing peek.");
            }
            if (drawer == mTopDrawerView) {
                mShouldPeekTopDrawerAfterLayout = true;
            } else if (drawer == mBottomDrawerView) {
                mShouldPeekBottomDrawerAfterLayout = true;
            }
            return;
        }

        maybePeekDrawer(drawer);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Do not intercept touch events if a drawer is open. If the content in a drawer scrolls,
        // then the touch event can be intercepted if the content in the drawer is scrolled to
        // the maximum opposite of the drawer's gravity (ex: the touch event can be intercepted
        // if the top drawer is open and scrolling content is at the bottom.
        if ((mBottomDrawerView != null && mBottomDrawerView.isOpened() && !mCanBottomDrawerBeClosed)
                || (mTopDrawerView != null && mTopDrawerView.isOpened()
                && !mCanTopDrawerBeClosed)) {
            mDrawerOpenLastInterceptedTouchEvent = ev;
            return false;
        }

        // Delegate event to drawer draggers.
        final boolean shouldInterceptTop = mTopDrawerDragger.shouldInterceptTouchEvent(ev);
        final boolean shouldInterceptBottom = mBottomDrawerDragger.shouldInterceptTouchEvent(ev);
        return shouldInterceptTop || shouldInterceptBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev == null) {
            Log.w(TAG, "null MotionEvent passed to onTouchEvent");
            return false;
        }
        // Delegate event to drawer draggers.
        mTopDrawerDragger.processTouchEvent(ev);
        mBottomDrawerDragger.processTouchEvent(ev);
        return true;
    }

    @Override
    public void computeScroll() {
        // For scrolling the drawers.
        final boolean topSettling = mTopDrawerDragger.continueSettling(true /* deferCallbacks */);
        final boolean bottomSettling = mBottomDrawerDragger.continueSettling(true /*
        deferCallbacks */);
        if (topSettling || bottomSettling) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (!(child instanceof WearableDrawerView)) {
            return;
        }

        WearableDrawerView drawerChild = (WearableDrawerView) child;
        drawerChild.setDrawerController(new WearableDrawerController(this, drawerChild));
        int childGravity = ((FrameLayout.LayoutParams) params).gravity;
        // Check for preferential gravity if no gravity is set in the layout.
        if (childGravity == Gravity.NO_GRAVITY || childGravity == GRAVITY_UNDEFINED) {
            ((FrameLayout.LayoutParams) params).gravity = drawerChild.preferGravity();
            childGravity = drawerChild.preferGravity();
            drawerChild.setLayoutParams(params);
        }
        WearableDrawerView drawerView;
        if (childGravity == Gravity.TOP) {
            mTopDrawerView = drawerChild;
            drawerView = mTopDrawerView;
        } else if (childGravity == Gravity.BOTTOM) {
            mBottomDrawerView = drawerChild;
            drawerView = mBottomDrawerView;
        } else {
            drawerView = null;
        }

        if (drawerView != null) {
            drawerView.addOnLayoutChangeListener(this);
        }
    }

    @Override
    public void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {
        if (v == mTopDrawerView) {
            // Layout the top drawer base on the openedPercent. It is initially hidden.
            final float openedPercent = mTopDrawerView.getOpenedPercent();
            final int height = v.getHeight();
            final int childTop = -height + (int) (height * openedPercent);
            v.layout(v.getLeft(), childTop, v.getRight(), childTop + height);
        } else if (v == mBottomDrawerView) {
            // Layout the bottom drawer base on the openedPercent. It is initially hidden.
            final float openedPercent = mBottomDrawerView.getOpenedPercent();
            final int height = v.getHeight();
            final int childTop = (int) (getHeight() - height * openedPercent);
            v.layout(v.getLeft(), childTop, v.getRight(), childTop + height);
        }
    }

    /**
     * Sets a listener to be notified of drawer events.
     */
    public void setDrawerStateCallback(DrawerStateCallback callback) {
        mDrawerStateCallback = callback;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mShouldPeekBottomDrawerAfterLayout
                || mShouldPeekTopDrawerAfterLayout
                || mShouldOpenTopDrawerAfterLayout
                || mShouldOpenBottomDrawerAfterLayout) {
            getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    if (mShouldOpenBottomDrawerAfterLayout) {
                                        openDrawerWithoutAnimation(mBottomDrawerView);
                                        mShouldOpenBottomDrawerAfterLayout = false;
                                    } else if (mShouldPeekBottomDrawerAfterLayout) {
                                        peekDrawer(Gravity.BOTTOM);
                                        mShouldPeekBottomDrawerAfterLayout = false;
                                    }

                                    if (mShouldOpenTopDrawerAfterLayout) {
                                        openDrawerWithoutAnimation(mTopDrawerView);
                                        mShouldOpenTopDrawerAfterLayout = false;
                                    } else if (mShouldPeekTopDrawerAfterLayout) {
                                        peekDrawer(Gravity.TOP);
                                        mShouldPeekTopDrawerAfterLayout = false;
                                    }
                                }
                            });
        }
    }

    @Override
    public void onFlingComplete(View view) {
        boolean canTopPeek = mTopDrawerView != null && mTopDrawerView.isAutoPeekEnabled();
        boolean canBottomPeek = mBottomDrawerView != null && mBottomDrawerView.isAutoPeekEnabled();
        boolean canScrollUp = view.canScrollVertically(UP);
        boolean canScrollDown = view.canScrollVertically(DOWN);

        if (canTopPeek && !canScrollUp && !mTopDrawerView.isPeeking()) {
            peekDrawer(Gravity.TOP);
        }
        if (canBottomPeek && (!canScrollUp || !canScrollDown) && !mBottomDrawerView.isPeeking()) {
            peekDrawer(Gravity.BOTTOM);
        }
    }

    @Override // NestedScrollingParent
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override // NestedScrollingParent
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY,
            boolean consumed) {
        return false;
    }

    @Override // NestedScrollingParent
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        maybeUpdateScrollingContentView(target);
        mLastScrollWasFling = true;

        if (target == mScrollingContentView) {
            FlingWatcher flingWatcher = mFlingWatcher.getFor(mScrollingContentView);
            if (flingWatcher != null) {
                flingWatcher.watch();
            }
        }
        // We do not want to intercept the child from receiving the fling, so return false.
        return false;
    }

    @Override // NestedScrollingParent
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
        maybeUpdateScrollingContentView(target);
    }

    @Override // NestedScrollingParent
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed) {

        boolean scrolledUp = dyConsumed < 0;
        boolean scrolledDown = dyConsumed > 0;
        boolean overScrolledUp = dyUnconsumed < 0;
        boolean overScrolledDown = dyUnconsumed > 0;

        // When the top drawer is open, we need to track whether it can be closed.
        if (mTopDrawerView != null && mTopDrawerView.isOpened()) {
            // When the top drawer is overscrolled down or cannot scroll down, we consider it to be
            // at the bottom of its content, so it can be closed.
            mCanTopDrawerBeClosed =
                    overScrolledDown || !mTopDrawerView.getDrawerContent()
                            .canScrollVertically(DOWN);
            // If the last scroll was a fling and the drawer can be closed, pass along the last
            // touch event to start closing the drawer. See the javadocs on mLastScrollWasFling
            // for more information.
            if (mCanTopDrawerBeClosed && mLastScrollWasFling) {
                onTouchEvent(mDrawerOpenLastInterceptedTouchEvent);
            }
            mLastScrollWasFling = false;
            return;
        }

        // When the bottom drawer is open, we need to track whether it can be closed.
        if (mBottomDrawerView != null && mBottomDrawerView.isOpened()) {
            // When the bottom drawer is scrolled to the top of its content, it can be closed.
            mCanBottomDrawerBeClosed = overScrolledUp;
            // If the last scroll was a fling and the drawer can be closed, pass along the last
            // touch event to start closing the drawer. See the javadocs on mLastScrollWasFling
            // for more information.
            if (mCanBottomDrawerBeClosed && mLastScrollWasFling) {
                onTouchEvent(mDrawerOpenLastInterceptedTouchEvent);
            }
            mLastScrollWasFling = false;
            return;
        }

        mLastScrollWasFling = false;

        // The following code assumes that neither drawer is open.

        // The bottom and top drawer are not open. Look at the scroll events to figure out whether
        // a drawer should peek, close it's peek, or do nothing.
        boolean canTopAutoPeek = mTopDrawerView != null && mTopDrawerView.isAutoPeekEnabled();
        boolean canBottomAutoPeek =
                mBottomDrawerView != null && mBottomDrawerView.isAutoPeekEnabled();
        boolean isTopDrawerPeeking = mTopDrawerView != null && mTopDrawerView.isPeeking();
        boolean isBottomDrawerPeeking = mBottomDrawerView != null && mBottomDrawerView.isPeeking();
        boolean scrolledDownPastSlop = false;
        boolean shouldPeekOnScrollDown =
                mBottomDrawerView != null && mBottomDrawerView.isPeekOnScrollDownEnabled();
        if (scrolledDown) {
            mCurrentNestedScrollSlopTracker += dyConsumed;
            scrolledDownPastSlop = mCurrentNestedScrollSlopTracker > mNestedScrollSlopPx;
        }

        if (canTopAutoPeek) {
            if (overScrolledUp && !isTopDrawerPeeking) {
                peekDrawer(Gravity.TOP);
            } else if (scrolledDown && isTopDrawerPeeking && !isClosingPeek(mTopDrawerView)) {
                closeDrawer(Gravity.TOP);
            }
        }

        if (canBottomAutoPeek) {
            if ((overScrolledDown || overScrolledUp) && !isBottomDrawerPeeking) {
                peekDrawer(Gravity.BOTTOM);
            } else if (shouldPeekOnScrollDown && scrolledDownPastSlop && !isBottomDrawerPeeking) {
                peekDrawer(Gravity.BOTTOM);
            } else if ((scrolledUp || (!shouldPeekOnScrollDown && scrolledDown))
                    && isBottomDrawerPeeking
                    && !isClosingPeek(mBottomDrawerView)) {
                closeDrawer(mBottomDrawerView);
            }
        }
    }

    /**
     * Peeks the given drawer if it is not {@code null} and has a peek view.
     */
    private void maybePeekDrawer(WearableDrawerView drawerView) {
        if (drawerView == null) {
            return;
        }
        View peekView = drawerView.getPeekContainer();
        if (peekView == null) {
            return;
        }

        View drawerContent = drawerView.getDrawerContent();
        int layoutGravity = ((FrameLayout.LayoutParams) drawerView.getLayoutParams()).gravity;
        int gravity =
                layoutGravity == Gravity.NO_GRAVITY ? drawerView.preferGravity() : layoutGravity;

        drawerView.setIsPeeking(true);
        peekView.setAlpha(1);
        peekView.setScaleX(1);
        peekView.setScaleY(1);
        peekView.setVisibility(VISIBLE);
        if (drawerContent != null) {
            drawerContent.setAlpha(0);
            drawerContent.setVisibility(GONE);
        }

        if (gravity == Gravity.BOTTOM) {
            mBottomDrawerDragger.smoothSlideViewTo(
                    drawerView, 0 /* finalLeft */, getHeight() - peekView.getHeight());
        } else if (gravity == Gravity.TOP) {
            mTopDrawerDragger.smoothSlideViewTo(
                    drawerView, 0 /* finalLeft */,
                    -(drawerView.getHeight() - peekView.getHeight()));
            if (!mIsAccessibilityEnabled) {
                // Don't automatically close the top drawer when in accessibility mode.
                closeDrawerDelayed(gravity, PEEK_AUTO_CLOSE_DELAY_MS);
            }
        }

        invalidate();
    }

    private void openDrawerWithoutAnimation(WearableDrawerView drawer) {
        if (drawer == null) {
            return;
        }

        int offset;
        if (drawer == mTopDrawerView) {
            offset = mTopDrawerView.getHeight();
        } else if (drawer == mBottomDrawerView) {
            offset = -mBottomDrawerView.getHeight();
        } else {
            Log.w(TAG, "openDrawer(View) should be passed in the top or bottom drawer");
            return;
        }

        drawer.offsetTopAndBottom(offset);
        drawer.setOpenedPercent(1f);
        drawer.onDrawerOpened();
        if (mDrawerStateCallback != null) {
            mDrawerStateCallback.onDrawerOpened(this, drawer);
        }
        showDrawerContentMaybeAnimate(drawer);
        invalidate();
    }

    /**
     * @param gravity the gravity of the child to return.
     * @return the drawer with the specified gravity
     */
    @Nullable
    private WearableDrawerView findDrawerWithGravity(int gravity) {
        switch (gravity) {
            case Gravity.TOP:
                return mTopDrawerView;
            case Gravity.BOTTOM:
                return mBottomDrawerView;
            default:
                Log.w(TAG, "Invalid drawer gravity: " + gravity);
                return null;
        }
    }

    /**
     * Updates {@link #mScrollingContentView} if {@code view} is not a descendant of a {@link
     * WearableDrawerView}.
     */
    private void maybeUpdateScrollingContentView(View view) {
        if (view != mScrollingContentView && !isDrawerOrChildOfDrawer(view)) {
            mScrollingContentView = view;
        }
    }

    /**
     * Returns {@code true} if {@code view} is a descendant of a {@link WearableDrawerView}.
     */
    private boolean isDrawerOrChildOfDrawer(View view) {
        while (view != null && view != this) {
            if (view instanceof WearableDrawerView) {
                return true;
            }

            view = (View) view.getParent();
        }

        return false;
    }

    private boolean isClosingPeek(WearableDrawerView drawerView) {
        return drawerView != null && drawerView.getDrawerState() == STATE_SETTLING;
    }

    @Override // NestedScrollingParent
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target,
            int nestedScrollAxes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
    }

    @Override // NestedScrollingParent
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target,
            int nestedScrollAxes) {
        mCurrentNestedScrollSlopTracker = 0;
        return true;
    }

    @Override // NestedScrollingParent
    public void onStopNestedScroll(@NonNull View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
    }

    private boolean canDrawerContentScrollVertically(
            @Nullable WearableDrawerView drawerView, int direction) {
        if (drawerView == null) {
            return false;
        }

        View drawerContent = drawerView.getDrawerContent();
        if (drawerContent == null) {
            return false;
        }

        return drawerContent.canScrollVertically(direction);
    }

    /**
     * Listener for monitoring events about drawers.
     */
    public static class DrawerStateCallback {

        /**
         * Called when a drawer has settled in a completely open state. The drawer is interactive at
         * this point.
         */
        public void onDrawerOpened(WearableDrawerLayout layout, WearableDrawerView drawerView) {
        }

        /**
         * Called when a drawer has settled in a completely closed state.
         */
        public void onDrawerClosed(WearableDrawerLayout layout, WearableDrawerView drawerView) {
        }

        /**
         * Called when the drawer motion state changes. The new state will be one of {@link
         * WearableDrawerView#STATE_IDLE}, {@link WearableDrawerView#STATE_DRAGGING} or {@link
         * WearableDrawerView#STATE_SETTLING}.
         */
        public void onDrawerStateChanged(WearableDrawerLayout layout, @DrawerState int newState) {
        }
    }

    private void allowAccessibilityFocusOnAllChildren() {
        if (!mIsAccessibilityEnabled) {
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private void allowAccessibilityFocusOnOnly(WearableDrawerView drawer) {
        if (!mIsAccessibilityEnabled) {
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != drawer) {
                child.setImportantForAccessibility(
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }
    }

    /**
     * Base class for top and bottom drawer dragger callbacks.
     */
    private abstract class DrawerDraggerCallback extends ViewDragHelper.Callback {

        public abstract WearableDrawerView getDrawerView();

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            WearableDrawerView drawerView = getDrawerView();
            // Returns true if the dragger is dragging the drawer.
            return child == drawerView && !drawerView.isLocked()
                    && drawerView.getDrawerContent() != null;
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            // Defines the vertical drag range of the drawer.
            return child == getDrawerView() ? child.getHeight() : 0;
        }

        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            showDrawerContentMaybeAnimate((WearableDrawerView) capturedChild);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            final WearableDrawerView drawerView = getDrawerView();
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    boolean openedOrClosed = false;
                    if (drawerView.isOpened()) {
                        openedOrClosed = true;
                        drawerView.onDrawerOpened();
                        allowAccessibilityFocusOnOnly(drawerView);
                        if (mDrawerStateCallback != null) {
                            mDrawerStateCallback
                                    .onDrawerOpened(WearableDrawerLayout.this, drawerView);
                        }

                        // Drawers can be closed if a drag to close them will not cause a scroll.
                        mCanTopDrawerBeClosed = !canDrawerContentScrollVertically(mTopDrawerView,
                                DOWN);
                        mCanBottomDrawerBeClosed = !canDrawerContentScrollVertically(
                                mBottomDrawerView, UP);
                    } else if (drawerView.isClosed()) {
                        openedOrClosed = true;
                        drawerView.onDrawerClosed();
                        allowAccessibilityFocusOnAllChildren();
                        if (mDrawerStateCallback != null) {
                            mDrawerStateCallback
                                    .onDrawerClosed(WearableDrawerLayout.this, drawerView);
                        }
                    } else { // drawerView is peeking
                        allowAccessibilityFocusOnAllChildren();
                    }

                    // If the drawer is fully opened or closed, change it to non-peeking mode.
                    if (openedOrClosed && drawerView.isPeeking()) {
                        drawerView.setIsPeeking(false);
                        drawerView.getPeekContainer().setVisibility(INVISIBLE);
                    }
                    break;
                default: // fall out
            }

            if (drawerView.getDrawerState() != state) {
                drawerView.setDrawerState(state);
                drawerView.onDrawerStateChanged(state);
                if (mDrawerStateCallback != null) {
                    mDrawerStateCallback.onDrawerStateChanged(WearableDrawerLayout.this, state);
                }
            }
        }
    }

    /**
     * For communicating with top drawer view dragger.
     */
    private class TopDrawerDraggerCallback extends DrawerDraggerCallback {

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            if (mTopDrawerView == child) {
                int peekHeight = mTopDrawerView.getPeekContainer().getHeight();
                // The top drawer can be dragged vertically from peekHeight - height to 0.
                return Math.max(peekHeight - child.getHeight(), Math.min(top, 0));
            }
            return 0;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (mTopDrawerView != null
                    && edgeFlags == ViewDragHelper.EDGE_TOP
                    && !mTopDrawerView.isLocked()
                    && (mBottomDrawerView == null || !mBottomDrawerView.isOpened())
                    && mTopDrawerView.getDrawerContent() != null) {

                boolean atTop =
                        mScrollingContentView == null || !mScrollingContentView
                                .canScrollVertically(UP);
                if (!mTopDrawerView.isOpenOnlyAtTopEnabled() || atTop) {
                    mTopDrawerDragger.captureChildView(mTopDrawerView, pointerId);
                }
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            if (releasedChild == mTopDrawerView) {
                // Settle to final position. Either swipe open or close.
                final float openedPercent = mTopDrawerView.getOpenedPercent();

                final int finalTop;
                if (yvel > 0 || (yvel == 0 && openedPercent > OPENED_PERCENT_THRESHOLD)) {
                    // Drawer was being flung open or drawer is mostly open, so finish opening.
                    finalTop = 0;
                } else {
                    // Drawer should be closed to its peek state.
                    animatePeekVisibleAfterBeingClosed(mTopDrawerView);
                    finalTop = mTopDrawerView.getPeekContainer().getHeight() - releasedChild
                            .getHeight();
                }

                mTopDrawerDragger.settleCapturedViewAt(0 /* finalLeft */, finalTop);
                invalidate();
            }
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx,
                int dy) {
            if (changedView == mTopDrawerView) {
                // Compute the offset and invalidate will move the drawer during layout.
                final int height = changedView.getHeight();
                mTopDrawerView.setOpenedPercent((float) (top + height) / height);
                invalidate();
            }
        }

        @Override
        public WearableDrawerView getDrawerView() {
            return mTopDrawerView;
        }
    }

    /**
     * For communicating with bottom drawer view dragger.
     */
    private class BottomDrawerDraggerCallback extends DrawerDraggerCallback {

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            if (mBottomDrawerView == child) {
                // The bottom drawer can be dragged vertically from (parentHeight - height) to
                // (parentHeight - peekHeight).
                int parentHeight = getHeight();
                int peekHeight = mBottomDrawerView.getPeekContainer().getHeight();
                return Math.max(parentHeight - child.getHeight(),
                        Math.min(top, parentHeight - peekHeight));
            }
            return 0;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (mBottomDrawerView != null
                    && edgeFlags == ViewDragHelper.EDGE_BOTTOM
                    && !mBottomDrawerView.isLocked()
                    && (mTopDrawerView == null || !mTopDrawerView.isOpened())
                    && mBottomDrawerView.getDrawerContent() != null) {
                // Tells the dragger which view to start dragging.
                mBottomDrawerDragger.captureChildView(mBottomDrawerView, pointerId);
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            if (releasedChild == mBottomDrawerView) {
                // Settle to final position. Either swipe open or close.
                final int parentHeight = getHeight();
                final float openedPercent = mBottomDrawerView.getOpenedPercent();
                final int finalTop;
                if (yvel < 0 || (yvel == 0 && openedPercent > OPENED_PERCENT_THRESHOLD)) {
                    // Drawer was being flung open or drawer is mostly open, so finish opening it.
                    finalTop = parentHeight - releasedChild.getHeight();
                } else {
                    // Drawer should be closed to its peek state.
                    animatePeekVisibleAfterBeingClosed(mBottomDrawerView);
                    finalTop = getHeight() - mBottomDrawerView.getPeekContainer().getHeight();
                }
                mBottomDrawerDragger.settleCapturedViewAt(0 /* finalLeft */, finalTop);
                invalidate();
            }
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx,
                int dy) {
            if (changedView == mBottomDrawerView) {
                // Compute the offset and invalidate will move the drawer during layout.
                final int height = changedView.getHeight();
                final int parentHeight = getHeight();

                mBottomDrawerView.setOpenedPercent((float) (parentHeight - top) / height);
                invalidate();
            }
        }

        @Override
        public WearableDrawerView getDrawerView() {
            return mBottomDrawerView;
        }
    }

    /**
     * Runnable that closes the given drawer if it is just peeking.
     */
    private class ClosePeekRunnable implements Runnable {

        private final int mGravity;

        private ClosePeekRunnable(int gravity) {
            mGravity = gravity;
        }

        @Override
        public void run() {
            WearableDrawerView drawer = findDrawerWithGravity(mGravity);
            if (drawer != null
                    && !drawer.isOpened()
                    && drawer.getDrawerState() == STATE_IDLE) {
                closeDrawer(mGravity);
            }
        }
    }
}
