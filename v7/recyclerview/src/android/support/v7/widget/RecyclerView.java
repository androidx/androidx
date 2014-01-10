/*
 * Copyright (C) 2013 The Android Open Source Project
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


package android.support.v7.widget;

import android.content.Context;
import android.database.Observable;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.Pools;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.FocusFinder;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * A flexible view for providing a limited window into a large data set.
 */
public class RecyclerView extends ViewGroup {
    private static final String TAG = "RecyclerView";

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int NO_POSITION = -1;
    public static final long NO_ID = -1;
    public static final int INVALID_TYPE = -1;

    private static final int MAX_SCROLL_DURATION = 2000;

    private final RecyclerViewDataObserver mObserver = new RecyclerViewDataObserver();

    private final Recycler mRecycler = new Recycler();

    private final Runnable mUpdateChildViewsRunnable = new Runnable() {
        public void run() {
            mEatRequestLayout = true;
            updateChildViews();
            mEatRequestLayout = false;
        }
    };

    private final Rect mTempRect = new Rect();

    private final ArrayList<UpdateOp> mPendingUpdates = new ArrayList<UpdateOp>();
    private Pools.Pool<UpdateOp> mUpdateOpPool = new Pools.SimplePool<UpdateOp>(UpdateOp.POOL_SIZE);

    private Adapter mAdapter;
    private LayoutManager mLayout;
    private RecyclerListener mRecyclerListener;
    private final ArrayList<ItemDecoration> mItemDecorations = new ArrayList<ItemDecoration>();
    private final ArrayList<ItemTouchListener> mItemTouchListeners =
            new ArrayList<ItemTouchListener>();
    private final SparseArray<ViewHolder> mAttachedViewsByPosition =
            new SparseArray<ViewHolder>();
    private final LongSparseArray<ViewHolder> mAttachedViewsById =
            new LongSparseArray<ViewHolder>();
    private boolean mIsAttached;
    private boolean mHasFixedSize;
    private boolean mFirstLayoutComplete;
    private boolean mEatRequestLayout;
    private boolean mAdapterUpdateDuringMeasure;
    private final boolean mPostUpdatesOnAnimation;

    private RecycledViewPool mRecyclerPool;

    private EdgeEffectCompat mLeftGlow, mTopGlow, mRightGlow, mBottomGlow;

    private static final int INVALID_POINTER = -1;

    /**
     * The RecyclerView is not currently scrolling.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * The RecyclerView is currently being dragged by outside input such as user touch input.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * The RecyclerView is currently animating to a final position while not under
     * outside control.
     * @see #getScrollState()
     */
    public static final int SCROLL_STATE_SETTLING = 2;

    // Touch/scrolling handling

    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private VelocityTracker mVelocityTracker;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLastTouchX;
    private int mLastTouchY;
    private final int mTouchSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;

    private final ViewFlinger mViewFlinger = new ViewFlinger();

    private OnScrollListener mScrollListener;

    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public RecyclerView(Context context) {
        this(context, null);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final int version = Build.VERSION.SDK_INT;
        mPostUpdatesOnAnimation = version >= 16;

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        setWillNotDraw(ViewCompat.getOverScrollMode(this) == ViewCompat.OVER_SCROLL_NEVER);
    }

    /**
     * RecyclerView can perform several optimizations if it can know in advance that changes in
     * adapter content cannot change the size of the RecyclerView itself.
     * If your use of RecyclerView falls into this category, set this to true.
     *
     * @param hasFixedSize true if adapter changes cannot affect the size of the RecyclerView.
     */
    public void setHasFixedSize(boolean hasFixedSize) {
        mHasFixedSize = hasFixedSize;
    }

    /**
     * @return true if the app has specified that changes in adapter content cannot change
     * the size of the RecyclerView itself.
     */
    public boolean hasFixedSize() {
        return mHasFixedSize;
    }

    /**
     * Set a new adapter to provide child views on demand.
     *
     * @param adapter The new adapter to set, or null to set no adapter.
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            adapter.unregisterAdapterDataObserver(mObserver);
        }
        mAdapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }
        mRecycler.onAdapterChanged();
        requestLayout();
    }

    /**
     * Retrieves the previously set adapter or null if no adapter is set.
     *
     * @return The previously set adapter
     * @see #setAdapter(android.support.v7.widget.RecyclerView.Adapter)
     */
    public Adapter getAdapter() {
        return mAdapter;
    }

    /**
     * Register a listener that will be notified whenever a child view is recycled.
     *
     * <p>This listener will be called when a LayoutManager or the RecyclerView decides
     * that a child view is no longer needed. If an application associates expensive
     * or heavyweight data with item views, this may be a good place to release
     * or free those resources.</p>
     *
     * @param listener Listener to register, or null to clear
     */
    public void setRecyclerListener(RecyclerListener listener) {
        mRecyclerListener = listener;
    }

    /**
     * Set the {@link android.support.v7.widget.RecyclerView.LayoutManager}
     * that this RecyclerView will use.
     *
     * <p>In contrast to other adapter-backed views such as {@link android.widget.ListView}
     * or {@link android.widget.GridView}, RecyclerView allows client code to provide custom
     * layout arrangements for child views. These arrangements are controlled by the
     * {@link android.support.v7.widget.RecyclerView.LayoutManager}.
     * A LayoutManager must be provided for RecyclerView to function.</p>
     *
     * <p>Several default strategies are provided for common uses such as lists and grids.</p>
     *
     * @param layout LayoutManager to use
     */
    public void setLayoutManager(LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }

        mRecycler.clear();
        removeAllViews();
        if (mLayout != null) {
            if (mIsAttached) {
                mLayout.onDetachedFromWindow();
            }
            mLayout.mRecyclerView = null;
        }
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout +
                        " is already attached to a RecyclerView: " + layout.mRecyclerView);
            }
            layout.mRecyclerView = this;
            if (mIsAttached) {
                mLayout.onAttachedToWindow();
            }
        }
        requestLayout();
    }

    /**
     * Retrieve this RecyclerView's {@link RecycledViewPool}. This method will never return null;
     * if no pool is set for this view a new one will be created. See
     * {@link #setRecycledViewPool(android.support.v7.widget.RecyclerView.RecycledViewPool)
     * setRecycledViewPool} for more information.
     *
     * @return The pool used to store recycled item views for reuse.
     * @see #setRecycledViewPool(android.support.v7.widget.RecyclerView.RecycledViewPool)
     */
    public RecycledViewPool getRecycledViewPool() {
        if (mRecyclerPool == null) {
            mRecyclerPool = new RecycledViewPool();
        }
        return mRecyclerPool;
    }

    /**
     * Recycled view pools allow multiple RecyclerViews to share a common pool of scrap views.
     * This can be useful if you have multiple RecyclerViews with adapters that use the same
     * view types, for example if you have several data sets with the same kinds of item views
     * displayed by a {@link android.support.v4.view.ViewPager ViewPager}.
     *
     * @param pool Pool to set. If this parameter is null a new pool will be created and used.
     */
    public void setRecycledViewPool(RecycledViewPool pool) {
        mRecyclerPool = pool;
    }

    /**
     * Return the current scrolling state of the RecyclerView.
     *
     * @return {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or
     * {@link #SCROLL_STATE_SETTLING}
     */
    public int getScrollState() {
        return mScrollState;
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            mViewFlinger.stop();
        }
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(state);
        }
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     * @param index Position in the decoration chain to insert this decoration at. If this value
     *              is negative the decoration will be added at the end.
     */
    public void addItemDecoration(ItemDecoration decor, int index) {
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(false);
        }
        if (index < 0) {
            mItemDecorations.add(decor);
        } else {
            mItemDecorations.add(index, decor);
        }

        // TODO Refresh layout for affected views
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can
     * affect both measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views
     * will be nested; a padding added by an earlier decoration will mean further
     * item decorations in the list will be asked to draw/pad within the previous decoration's
     * given area.</p>
     *
     * @param decor Decoration to add
     */
    public void addItemDecoration(ItemDecoration decor) {
        addItemDecoration(decor, -1);
    }

    /**
     * Remove an {@link ItemDecoration} from this RecyclerView.
     *
     * <p>The given decoration will no longer impact </p>
     *
     * @param decor Decoration to remove
     */
    public void removeItemDecoration(ItemDecoration decor) {
        mItemDecorations.remove(decor);
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(ViewCompat.getOverScrollMode(this) == ViewCompat.OVER_SCROLL_NEVER);
        }

        // TODO: Refresh layout for affected views
    }

    /**
     * Set a listener that will be notified of any changes in scroll state or position.
     *
     * @param listener Listener to set or null to clear
     */
    public void setOnScrollListener(OnScrollListener listener) {
        mScrollListener = listener;
    }

    @Override
    public void scrollTo(int x, int y) {
        Log.e(TAG, "RecyclerView does not support scrolling to an absolute position.");
        // TODO: Implement this in some sort of fallback way. Or throw.
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mLayout == null) {
            throw new IllegalStateException("Cannot scroll without a LayoutManager set. " +
                    "Call setLayoutManager with a non-null argument.");
        }
        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();
        if (canScrollHorizontal || canScrollVertical) {
            scrollByInternal(canScrollHorizontal ? x : 0, canScrollVertical ? y : 0);
        }
    }

    /**
     * Does not perform bounds checking. Used by internal methods that have already validated input.
     */
    void scrollByInternal(int x, int y) {
        int overscrollX = 0, overscrollY = 0;
        if (x != 0) {
            mEatRequestLayout = true;
            final int hresult = mLayout.scrollHorizontallyBy(x, mRecycler);
            mEatRequestLayout = false;
            overscrollX = x - hresult;
        }
        if (y != 0) {
            mEatRequestLayout = true;
            final int vresult = mLayout.scrollVerticallyBy(y, mRecycler);
            mEatRequestLayout = false;
            overscrollY = y - vresult;
        }
        pullGlows(overscrollX, overscrollY);
    }

    /**
     * Animate a scroll by the given amount of pixels along either axis.
     *
     * @param dx Pixels to scroll horizontally
     * @param dy Pixels to scroll vertically
     */
    public void smoothScrollBy(int dx, int dy) {
        if (dx != 0 || dy != 0) {
            mViewFlinger.smoothScrollBy(dx, dy);
        }
    }

    /**
     * Begin a standard fling with an initial velocity along each axis in pixels per second.
     *
     * @param velocityX Initial horizontal velocity in pixels per second
     * @param velocityY Initial vertical velocity in pixels per second
     */
    public void fling(int velocityX, int velocityY) {
        if (Math.abs(velocityX) < mMinFlingVelocity) {
            velocityX = 0;
        }
        if (Math.abs(velocityY) < mMinFlingVelocity) {
            velocityY = 0;
        }
        velocityX = Math.max(-mMaxFlingVelocity, Math.min(velocityX, mMaxFlingVelocity));
        velocityY = Math.max(-mMaxFlingVelocity, Math.min(velocityY, mMaxFlingVelocity));
        if (velocityX != 0 || velocityY != 0) {
            mViewFlinger.fling(velocityX, velocityY);
        }
    }

    /**
     * Apply a pull to relevant overscroll glow effects
     */
    private void pullGlows(int overscrollX, int overscrollY) {
        if (overscrollX < 0) {
            if (mLeftGlow == null) {
                mLeftGlow = new EdgeEffectCompat(getContext());
                mLeftGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            }
            mLeftGlow.onPull(-overscrollX / (float) getWidth());
        } else if (overscrollX > 0) {
            if (mRightGlow == null) {
                mRightGlow = new EdgeEffectCompat(getContext());
                mRightGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            }
            mRightGlow.onPull(overscrollX / (float) getWidth());
        }

        if (overscrollY < 0) {
            if (mTopGlow == null) {
                mTopGlow = new EdgeEffectCompat(getContext());
                mTopGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            }
            mTopGlow.onPull(-overscrollY / (float) getHeight());
        } else if (overscrollY > 0) {
            if (mBottomGlow == null) {
                mBottomGlow = new EdgeEffectCompat(getContext());
                mBottomGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            }
            mBottomGlow.onPull(overscrollY / (float) getHeight());
        }

        if (overscrollX != 0 || overscrollY != 0) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void releaseGlows() {
        boolean needsInvalidate = false;
        if (mLeftGlow != null) needsInvalidate |= mLeftGlow.onRelease();
        if (mTopGlow != null) needsInvalidate |= mTopGlow.onRelease();
        if (mRightGlow != null) needsInvalidate |= mRightGlow.onRelease();
        if (mBottomGlow != null) needsInvalidate |= mBottomGlow.onRelease();
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void absorbGlows(int velocityX, int velocityY) {
        if (velocityX < 0) {
            if (mLeftGlow == null) {
                mLeftGlow = new EdgeEffectCompat(getContext());
                mLeftGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            }
            mLeftGlow.onAbsorb(-velocityX);
        } else if (velocityX > 0) {
            if (mRightGlow == null) {
                mRightGlow = new EdgeEffectCompat(getContext());
                mRightGlow.setSize(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        getMeasuredWidth() - getPaddingLeft() - getPaddingRight());
            }
            mRightGlow.onAbsorb(velocityX);
        }

        if (velocityY < 0) {
            if (mTopGlow == null) {
                mTopGlow = new EdgeEffectCompat(getContext());
                mTopGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            }
            mTopGlow.onAbsorb(-velocityY);
        } else if (velocityY > 0) {
            if (mBottomGlow == null) {
                mBottomGlow = new EdgeEffectCompat(getContext());
                mBottomGlow.setSize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
            }
            mBottomGlow.onAbsorb(velocityY);
        }

        if (velocityX != 0 || velocityY != 0) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    // Focus handling

    @Override
    public View focusSearch(View focused, int direction) {
        final FocusFinder ff = FocusFinder.getInstance();
        View result = ff.findNextFocus(this, focused, direction);
        if (result == null) {
            mEatRequestLayout = true;
            result = mLayout.onFocusSearchFailed(focused, direction, mRecycler);
            mEatRequestLayout = false;
        }
        return result != null ? result : super.focusSearch(focused, direction);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mLayout.onRequestChildFocus(child, focused)) {
            mTempRect.set(0, 0, focused.getWidth(), focused.getHeight());
            offsetDescendantRectToMyCoords(focused, mTempRect);
            offsetRectIntoDescendantCoords(child, mTempRect);
            requestChildRectangleOnScreen(child, mTempRect, !mFirstLayoutComplete);
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        return mLayout.requestChildRectangleOnScreen(child, rect, immediate);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttached = true;
        mFirstLayoutComplete = false;
        if (mLayout != null) {
            mLayout.onAttachedToWindow();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayoutComplete = false;

        mViewFlinger.stop();
        // TODO Mark what our target position was if relevant, then we can jump there
        // on reattach.

        mIsAttached = false;
        if (mLayout != null) {
            mLayout.onDetachedFromWindow();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        final boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        final boolean canScrollVertically = mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);

        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);

                if (mScrollState == SCROLL_STATE_SETTLING) {
                    setScrollState(SCROLL_STATE_DRAGGING);
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN:
                mScrollPointerId = MotionEventCompat.getPointerId(e, actionIndex);
                mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = mInitialTouchY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
            } break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            } break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.clear();
            } break;
        }
        return mScrollState == SCROLL_STATE_DRAGGING;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final boolean canScrollHorizontally = mLayout.canScrollHorizontally();
        final boolean canScrollVertically = mLayout.canScrollVertically();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);

        final int action = MotionEventCompat.getActionMasked(e);
        final int actionIndex = MotionEventCompat.getActionIndex(e);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(e, 0);
                mInitialTouchX = mLastTouchX = (int) (e.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (e.getY() + 0.5f);
            } break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = MotionEventCompat.getPointerId(e, actionIndex);
                mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, actionIndex) + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, actionIndex) + 0.5f);
            } break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(e, mScrollPointerId);
                if (index < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id " +
                            mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
                final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    final int dx = x - mInitialTouchX;
                    final int dy = y - mInitialTouchY;
                    boolean startScroll = false;
                    if (canScrollHorizontally && Math.abs(dx) > mTouchSlop) {
                        mLastTouchX = mInitialTouchX + mTouchSlop * (dx < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                        mLastTouchY = mInitialTouchY + mTouchSlop * (dy < 0 ? -1 : 1);
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }
                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    final int dx = x - mLastTouchX;
                    final int dy = y - mLastTouchY;
                    scrollByInternal(canScrollHorizontally ? -dx : 0,
                            canScrollVertically ? -dy : 0);
                }
                mLastTouchX = x;
                mLastTouchY = y;
            } break;

            case MotionEventCompat.ACTION_POINTER_UP: {
                onPointerUp(e);
            } break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                final float xvel = canScrollHorizontally ?
                        -VelocityTrackerCompat.getXVelocity(mVelocityTracker, mScrollPointerId) : 0;
                final float yvel = canScrollVertically ?
                        -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
                if (xvel != 0 && canScrollHorizontally || yvel != 0 && canScrollVertically) {
                    fling((int) xvel, (int) yvel);
                }

                mVelocityTracker.clear();
                releaseGlows();
            } break;
        }

        return true;
    }

    private void onPointerUp(MotionEvent e) {
        final int actionIndex = MotionEventCompat.getActionIndex(e);
        if (MotionEventCompat.getPointerId(e, actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = MotionEventCompat.getPointerId(e, newIndex);
            mInitialTouchX = mLastTouchX = (int) (MotionEventCompat.getX(e, newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (MotionEventCompat.getY(e, newIndex) + 0.5f);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mAdapterUpdateDuringMeasure) {
            mEatRequestLayout = true;
            updateChildViews();
            mEatRequestLayout = false;
        }

        final int widthMode = MeasureSpec.getMode(widthSpec);
        final int heightMode = MeasureSpec.getMode(heightSpec);
        final int widthSize = MeasureSpec.getSize(widthSpec);
        final int heightSize = MeasureSpec.getSize(heightSpec);

        if (!mHasFixedSize || widthMode == MeasureSpec.UNSPECIFIED ||
                heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException("Non-fixed sizes not yet supported");
        }

        if (mLeftGlow != null) mLeftGlow.setSize(heightSize, widthSize);
        if (mTopGlow != null) mTopGlow.setSize(widthSize, heightSize);
        if (mRightGlow != null) mRightGlow.setSize(heightSize, widthSize);
        if (mBottomGlow != null) mBottomGlow.setSize(widthSize, heightSize);

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAdapter == null) {
            Log.e(TAG, "No adapter attached; skipping layout");
            return;
        }
        mEatRequestLayout = true;
        mLayout.layoutChildren(mAdapter, mRecycler);
        mEatRequestLayout = false;
        mFirstLayoutComplete = true;
    }

    @Override
    public void requestLayout() {
        if (!mEatRequestLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDrawOver(c);
        }

        boolean needsInvalidate = false;
        if (mLeftGlow != null && !mLeftGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(270);
            c.translate(-getHeight() + getPaddingTop(), 0);
            needsInvalidate |= mLeftGlow != null && mLeftGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mTopGlow != null && !mTopGlow.isFinished()) {
            c.translate(getPaddingLeft(), getPaddingTop());
            needsInvalidate |= mTopGlow != null && mTopGlow.draw(c);
        }
        if (mRightGlow != null && !mRightGlow.isFinished()) {
            final int restore = c.save();
            final int width = getWidth();

            c.rotate(90);
            c.translate(-getPaddingTop(), -width);
            needsInvalidate |= mRightGlow != null && mRightGlow.draw(c);
            c.restoreToCount(restore);
        }
        if (mBottomGlow != null && !mBottomGlow.isFinished()) {
            final int restore = c.save();
            c.rotate(180);
            c.translate(-getWidth() + getPaddingLeft(), -getHeight() + getPaddingTop());
            needsInvalidate |= mBottomGlow != null && mBottomGlow.draw(c);
            c.restoreToCount(restore);
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        final int count = mItemDecorations.size();
        for (int i = 0; i < count; i++) {
            mItemDecorations.get(i).onDraw(c);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && mLayout.checkLayoutParams((LayoutParams) p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no layout strategy");
        }
        return mLayout.generateDefaultLayoutParams();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no layout strategy");
        }
        return mLayout.generateLayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (mLayout == null) {
            throw new IllegalStateException("RecyclerView has no layout strategy");
        }
        return mLayout.generateLayoutParams(p);
    }

    void updateChildViews() {
        final int opCount = mPendingUpdates.size();
        for (int i = 0; i < opCount; i++) {
            final UpdateOp op = mPendingUpdates.get(i);
            switch (op.cmd) {
                case UpdateOp.ADD:
                    mRecycler.offsetPositionRecordsForInsert(op.positionStart, op.itemCount);

                    // TODO Animate it in
                    break;
                case UpdateOp.REMOVE:
                    mRecycler.offsetPositionRecordsForRemove(op.positionStart, op.itemCount);

                    // TODO Animate it away
                    break;
                case UpdateOp.UPDATE:
                    markViewRangeDirty(op.positionStart, op.itemCount);
                    break;
            }
            recycleUpdateOp(op);
        }
        mPendingUpdates.clear();
    }

    /**
     * Rebind existing views for the given range, or create as needed. Reposition/rearrange any
     * child views as appropriate and recycle/regenerate views around the range as necessary.
     *
     * @param positionStart Adapter position to start at
     * @param itemCount Number of views that must explicitly be rebound
     */
    void markViewRangeDirty(int positionStart, int itemCount) {
        final int count = getViewHolderCount();
        final int positionEnd = positionStart + itemCount;

        for (int i = 0; i < count; i++) {
            final ViewHolder holder = mAttachedViewsByPosition.valueAt(i);
            final int position = holder.getPosition();
            if (position >= positionStart && position < positionEnd) {
                holder.mIsDirty = true;
            }
        }
    }

    /**
     * Schedule an update of data from the adapter to occur on the next frame.
     * On newer platform versions this happens via the postOnAnimation mechanism and RecyclerView
     * attempts to avoid relayouts if possible.
     * On older platform versions the RecyclerView requests a layout the same way ListView does.
     */
    void postAdapterUpdate(UpdateOp op) {
        mPendingUpdates.add(op);
        if (mPendingUpdates.size() == 1) {
            if (mPostUpdatesOnAnimation && mHasFixedSize && mIsAttached) {
                ViewCompat.postOnAnimation(this, mUpdateChildViewsRunnable);
            } else {
                mAdapterUpdateDuringMeasure = true;
                requestLayout();
            }
        }
    }

    ViewHolder getChildViewHolder(View child) {
        if (child == null) {
            return null;
        }
        return ((LayoutParams) child.getLayoutParams()).mViewHolder;
    }

    public ViewHolder findViewHolderForPosition(int position) {
        return mAttachedViewsByPosition.get(position);
    }

    public ViewHolder findViewHolderForId(long id) {
        return mAttachedViewsById.get(id);
    }

    public ViewHolder getViewHolderForChildAt(int childIndex) {
        return getChildViewHolder(getChildAt(childIndex));
    }

    public int getViewHolderCount() {
        return mAttachedViewsByPosition.size();
    }

    public ViewHolder getViewHolderAt(int holderIndex) {
        return mAttachedViewsByPosition.valueAt(holderIndex);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams lp) {
        super.addView(child, index, lp);
        // TODO: Do something clever to synthesize/wrap an adapter.
    }

    /**
     * Offset the bounds of all child views by <code>dy</code> pixels.
     * Useful for implementing simple scrolling in {@link LayoutManager LayoutManagers}.
     *
     * @param dy Vertical pixel offset to apply to the bounds of all child views
     */
    public void offsetChildrenVertical(int dy) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).offsetTopAndBottom(dy);
        }
    }

    /**
     * Offset the bounds of all child views by <code>dy</code> pixels.
     * Useful for implementing simple scrolling in {@link LayoutManager LayoutManagers}.
     *
     * @param dx Horizontal pixel offset to apply to the bounds of all child views
     */
    public void offsetChildrenHorizontal(int dx) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).offsetLeftAndRight(dx);
        }
    }

    private class ViewFlinger implements Runnable {
        private int mLastFlingX;
        private int mLastFlingY;
        private ScrollerCompat mScroller;
        private Interpolator mInterpolator = sQuinticInterpolator;

        public ViewFlinger() {
            mScroller = ScrollerCompat.create(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                final int x = mScroller.getCurrX();
                final int y = mScroller.getCurrY();
                final int dx = x - mLastFlingX;
                final int dy = y - mLastFlingY;
                mLastFlingX = x;
                mLastFlingY = y;

                int overscrollX = 0, overscrollY = 0;
                if (dx != 0) {
                    mEatRequestLayout = true;
                    final int hresult = mLayout.scrollHorizontallyBy(dx, mRecycler);
                    mEatRequestLayout = false;
                    overscrollX = dx - hresult;
                }
                if (dy != 0) {
                    mEatRequestLayout = true;
                    final int vresult = mLayout.scrollVerticallyBy(dy, mRecycler);
                    mEatRequestLayout = false;
                    overscrollY = dy - vresult;
                }

                if (overscrollX != 0 || overscrollY != 0) {
                    final int vel = (int) mScroller.getCurrVelocity();

                    int velX = 0;
                    if (overscrollX != x) {
                        velX = overscrollX < 0 ? -vel : overscrollX > 0 ? vel : 0;
                    }

                    int velY = 0;
                    if (overscrollY != y) {
                        velY = overscrollY < 0 ? -vel : overscrollY > 0 ? vel : 0;
                    }

                    absorbGlows(velX, velY);
                    if ((velX != 0 || overscrollX == x || mScroller.getFinalX() == 0) &&
                            (velY != 0 || overscrollY == y || mScroller.getFinalY() == 0)) {
                        mScroller.abortAnimation();
                    }
                }

                if (mScroller.isFinished()) {
                    setScrollState(SCROLL_STATE_IDLE);
                } else {
                    ViewCompat.postOnAnimation(RecyclerView.this, this);
                }
            }
        }

        public void fling(int velocityX, int velocityY) {
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.fling(0, 0, velocityX, velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(RecyclerView.this, this);
        }

        public void smoothScrollBy(int dx, int dy) {
            smoothScrollBy(dx, dy, 0, 0);
        }

        public void smoothScrollBy(int dx, int dy, int vx, int vy) {
            smoothScrollBy(dx, dy, computeScrollDuration(dx, dy, vx, vy));
        }

        private float distanceInfluenceForSnapDuration(float f) {
            f -= 0.5f; // center the values about 0.
            f *= 0.3f * Math.PI / 2.0f;
            return (float) Math.sin(f);
        }

        private int computeScrollDuration(int dx, int dy, int vx, int vy) {
            final int absDx = Math.abs(dx);
            final int absDy = Math.abs(dy);
            final boolean horizontal = absDx > absDy;
            final int velocity = (int) Math.sqrt(vx * vx + vy * vy);
            final int delta = (int) Math.sqrt(dx * dx + dy * dy);
            final int containerSize = horizontal ? getWidth() : getHeight();
            final int halfContainerSize = containerSize / 2;
            final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
            final float distance = halfContainerSize + halfContainerSize *
                    distanceInfluenceForSnapDuration(distanceRatio);

            final int duration;
            if (velocity > 0) {
                duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
            } else {
                duration = (int) ((((float) absDx / containerSize) + 1) * 300);
            }
            return Math.min(duration, MAX_SCROLL_DURATION);
        }

        public void smoothScrollBy(int dx, int dy, int duration) {
            smoothScrollBy(dx, dy, duration, sQuinticInterpolator);
        }

        public void smoothScrollBy(int dx, int dy, int duration, Interpolator interpolator) {
            if (mInterpolator != interpolator) {
                mInterpolator = interpolator;
                mScroller = ScrollerCompat.create(getContext(), interpolator);
            }
            setScrollState(SCROLL_STATE_SETTLING);
            mLastFlingX = mLastFlingY = 0;
            mScroller.startScroll(0, 0, dx, dy, duration);
            ViewCompat.postOnAnimation(RecyclerView.this, this);
        }

        public void stop() {
            removeCallbacks(this);
        }

    }

    private class RecyclerViewDataObserver extends AdapterDataObserver {
        // These two fields act like a SparseLongArray for tracking nearby position-id mappings.
        // Values with the same index correspond to one another. mPositions is in ascending order.
        private int[] mPositions;
        private long[] mIds;
        private int mMappingSize;

        void initIdMapping() {

        }

        void clearIdMapping() {
            mPositions = null;
            mIds = null;
            mMappingSize = 0;
        }

        @Override
        public void onChanged() {
            if (mAdapter.hasStableIds()) {
                // TODO Determine what actually changed
            } else {
                mRecycler.onGenericDataChanged();
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            postAdapterUpdate(obtainUpdateOp(UpdateOp.UPDATE, positionStart, itemCount));
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            postAdapterUpdate(obtainUpdateOp(UpdateOp.ADD, positionStart, itemCount));
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            postAdapterUpdate(obtainUpdateOp(UpdateOp.REMOVE, positionStart, itemCount));
        }
    }

    public static class RecycledViewPool {
        private ArrayList<ViewHolder>[] mScrap;
        private int[] mMaxScrap;

        private static final int DEFAULT_MAX_SCRAP = 5;

        public void clear() {
            final int count = mScrap.length;
            for (int i = 0; i < count; i++) {
                mScrap[i].clear();
            }
        }

        public void reset(int typeCount) {
            mScrap = new ArrayList[typeCount];
            mMaxScrap = new int[typeCount];
            for (int i = 0; i < typeCount; i++) {
                mScrap[i] = new ArrayList<ViewHolder>(DEFAULT_MAX_SCRAP);
                mMaxScrap[i] = DEFAULT_MAX_SCRAP;
            }
        }

        public void setMaxScrap(int viewType, int max) {
            mMaxScrap[viewType] = max;
            while (mScrap[viewType].size() > max) {
                mScrap[viewType].remove(mScrap[viewType].size() - 1);
            }
        }

        public ViewHolder getScrapView(int viewType) {
            final ArrayList<ViewHolder> scrapHeap = mScrap[viewType];
            if (!scrapHeap.isEmpty()) {
                final int index = scrapHeap.size() - 1;
                final ViewHolder scrap = scrapHeap.get(index);
                scrapHeap.remove(index);
                return scrap;
            }
            return null;
        }

        public void putScrapView(ViewHolder scrap) {
            final int type = scrap.getItemViewType();
            if (mMaxScrap[type] <= mScrap[type].size()) {
                return;
            }

            scrap.mPosition = NO_POSITION;
            scrap.mItemId = NO_ID;
            scrap.mIsDirty = true;
            mScrap[type].add(scrap);
        }
    }

    public final class Recycler {
        private final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<ViewHolder>();

        public void clear() {
            final int attachedCount = mAttachedScrap.size();
            for (int i = 0; i < attachedCount; i++) {
                removeView(mAttachedScrap.get(i).itemView);
            }
            mAttachedScrap.clear();
        }

        public View getViewForPosition(int position) {
            ViewHolder holder;
            final int type = mAdapter.getItemViewType(position);
            if (mAdapter.hasStableIds()) {
                final long id = mAdapter.getItemId(position);
                holder = getScrapViewForId(id, type);
            } else {
                holder = getScrapViewForPosition(position, type);
            }

            if (holder == null) {
                holder = mAdapter.createViewHolder(RecyclerView.this, type);
                holder.mItemViewType = type;
            }

            if (holder.mIsDirty) {
                mAdapter.bindViewHolder(holder, position);
                holder.mIsDirty = false;
                holder.mPosition = position;
            }

            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp == null) {
                lp = generateDefaultLayoutParams();
                holder.itemView.setLayoutParams(lp);
            } else if (!checkLayoutParams(lp)) {
                lp = generateLayoutParams(lp);
                holder.itemView.setLayoutParams(lp);
            }
            ((LayoutParams) lp).mViewHolder = holder;

            return holder.itemView;
        }

        public void addDetachedScrapView(View scrap) {
            final ViewHolder holder = getChildViewHolder(scrap);
            getRecycledViewPool().putScrapView(holder);
            dispatchViewRecycled(holder);
        }

        public void detachAndScrapView(View scrap) {
            if (scrap.getParent() != RecyclerView.this) {
                throw new IllegalArgumentException("View " + scrap + " is not attached to " +
                        RecyclerView.this);
            }
            final ViewHolder holder = getChildViewHolder(scrap);
            holder.mIsDirty = true;
            removeView(scrap);
            getRecycledViewPool().putScrapView(holder);
            dispatchViewRecycled(holder);
        }

        public void scrapAllViewsAttached() {
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View v = getChildAt(i);
                final ViewHolder holder = getChildViewHolder(v);
                holder.mIsDirty = true;
                mAttachedScrap.add(holder);
            }
        }

        public void detachDirtyScrapViews() {
            final RecycledViewPool pool = getRecycledViewPool();
            final int count = mAttachedScrap.size();
            for (int i = 0; i < count; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (holder.itemView.getParent() == RecyclerView.this && holder.mIsDirty) {
                    removeView(holder.itemView);
                    pool.putScrapView(holder);
                    dispatchViewRecycled(holder);
                }
            }
            mAttachedScrap.clear();
        }

        ViewHolder getScrapViewForPosition(int position, int type) {
            // Look in our attached views first
            final int count = mAttachedScrap.size();
            for (int i = 0; i < count; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (holder.getPosition() == position) {
                    // Assumption: if the position record still matches, the type is also correct.
                    mAttachedScrap.remove(i);
                    return holder;
                }
            }

            return getRecycledViewPool().getScrapView(type);
        }

        ViewHolder getScrapViewForId(long id, int type) {
            // Look in our attached views first
            final int count = mAttachedScrap.size();
            for (int i = 0; i < count; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (holder.getItemId() == id) {
                    if (type == holder.getItemViewType()) {
                        mAttachedScrap.remove(i);
                        return holder;
                    } else {
                        break;
                    }
                }
            }

            // That didn't work, look for an unordered view of the right type instead.
            // The holder's position won't match so the calling code will need to have
            // the adapter rebind it.
            return getRecycledViewPool().getScrapView(type);
        }

        void dispatchViewRecycled(ViewHolder holder) {
            if (mRecyclerListener != null) {
                mRecyclerListener.onViewRecycled(holder);
            }
            if (mAdapter != null) {
                mAdapter.onViewRecycled(holder);
            }
        }

        void onGenericDataChanged() {
            // TODO
        }

        void onAdapterChanged() {
            clear();
            final int typeCount = mAdapter != null ? mAdapter.getItemViewTypeCount() : 0;
            getRecycledViewPool().reset(typeCount);
        }

        void offsetPositionRecordsForInsert(int insertedAt, int count) {
            // TODO
        }

        void offsetPositionRecordsForRemove(int insertedAt, int count) {
            // TODO
        }
    }

    /**
     * Base class for an Adapter
     *
     * <p>Adapters provide a binding from an app-specific data set to views that are displayed
     * within a {@link RecyclerView}.</p>
     */
    public static abstract class Adapter {
        private final AdapterDataObservable mObservable = new AdapterDataObservable();
        private boolean mHasStableIds = false;
        private int mViewTypeCount = 1;

        public abstract ViewHolder createViewHolder(ViewGroup parent, int viewType);
        public abstract void bindViewHolder(ViewHolder holder, int position);

        /**
         * Return the view type of the item at <code>position</code> for the purposes
         * of view recycling.
         *
         * <p>The default implementation of this method returns 0, making the assumption of
         * the default item view type count of 1. If you change the item view type using
         * {@link #setItemViewTypeCount(int)} you should also override this method to return
         * the correct view type for each item in your data set.</p>
         *
         * @param position position to query
         * @return integer in the range [0-{@link #getItemViewTypeCount()}) identifying the type
         *         of the view needed to represent the item at <code>position</code>
         */
        public int getItemViewType(int position) {
            return 0;
        }

        /**
         * Set the number of item view types required by this adapter to display its data set.
         * This may not be changed while the adapter has observers - e.g. while the adapter
         * is set on a {#link RecyclerView}.
         *
         * @param count Number of item view types required
         * @see #getItemViewTypeCount()
         * @see #getItemViewType(int)
         */
        public void setItemViewTypeCount(int count) {
            if (hasObservers()) {
                throw new IllegalStateException("Cannot change the item view type count while " +
                        "the adapter has registered observers.");
            }
            if (count < 1) {
                throw new IllegalArgumentException("Adapter must support at least 1 view type");
            }
            mViewTypeCount = count;
        }

        /**
         * Retrieve the number of item view types required by this adapter to display its data set.
         *
         * @return Number of item view types supported
         * @see #setItemViewTypeCount(int)
         * @see #getItemViewType(int)
         */
        public final int getItemViewTypeCount() {
            return mViewTypeCount;
        }

        public void setHasStableIds(boolean hasStableIds) {
            if (hasObservers()) {
                throw new IllegalStateException("Cannot change whether this adapter has " +
                        "stable IDs while the adapter has registered observers.");
            }
            mHasStableIds = true;
        }

        /**
         * Return the stable ID for the item at <code>position</code>. If {@link #hasStableIds()}
         * would return false this method should return {@link #NO_ID}. The default implementation
         * of this method returns {@link #NO_ID}.
         *
         * @param position Adapter position to query
         * @return the stable ID of the item at position
         */
        public long getItemId(int position) {
            return NO_ID;
        }

        public abstract int getItemCount();

        /**
         * Returns true if this adapter publishes a unique <code>long</code> value that can
         * act as a key for the item at a given position in the data set. If that item is relocated
         * in the data set, the ID returned for that item should be the same.
         *
         * @return true if this adapter's items have stable IDs
         */
        public final boolean hasStableIds() {
            return mHasStableIds;
        }

        /**
         * Called when a view created by this adapter has been recycled.
         *
         * <p>A view is recycled when a {@link LayoutManager} decides that it no longer
         * needs to be attached to its parent {@link RecyclerView}. This can be because it has
         * fallen out of visibility or a set of cached views represented by views still
         * attached to the parent RecyclerView. If an item view has large or expensive data
         * bound to it such as large bitmaps, this may be a good place to release those
         * resources.</p>
         *
         * @param holder The ViewHolder for the view being recycled
         */
        public void onViewRecycled(ViewHolder holder) {
        }

        public final boolean hasObservers() {
            return mObservable.hasObservers();
        }

        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public void notifyItemChanged(int position) {
            mObservable.notifyItemRangeChanged(position, 1);
        }

        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            mObservable.notifyItemRangeChanged(positionStart, itemCount);
        }

        public void notifyDataItemInserted(int position) {
            mObservable.notifyItemRangeInserted(position, 1);
        }

        public void notifyDataItemRangeInserted(int positionStart, int itemCount) {
            mObservable.notifyItemRangeInserted(positionStart, itemCount);
        }

        public void notifyDataItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }

        public void notifyDataItemRangeRemoved(int positionStart, int itemCount) {
            mObservable.notifyItemRangeRemoved(positionStart, itemCount);
        }
    }

    /**
     * A <code>LayoutManager</code> is responsible for measuring and positioning item views
     * within a <code>RecyclerView</code> as well as determining the policy for when to recycle
     * item views that are no longer visible to the user. By changing the <code>LayoutManager</code>
     * a <code>RecyclerView</code> can be used to implement a standard vertically scrolling list,
     * a uniform grid, staggered grids, horizontally scrolling collections and more. Several stock
     * layout managers are provided for general use.
     */
    public static abstract class LayoutManager {
        RecyclerView mRecyclerView;

        public final RecyclerView getRecyclerView() {
            return mRecyclerView;
        }

        public void onAttachedToWindow() {
        }

        public void onDetachedFromWindow() {
        }

        public abstract void layoutChildren(Adapter adapter, Recycler recycler);

        /**
         * Create a default <code>LayoutParams</code> object for a child of the RecyclerView.
         *
         * <p>LayoutStrategies will often want to use a custom <code>LayoutParams</code> type
         * to store extra information specific to the layout. Client code should subclass
         * {@link RecyclerView.LayoutParams} for this purpose.</p>
         *
         * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
         * you must also override
         * {@link #checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams)},
         * {@link #generateLayoutParams(android.view.ViewGroup.LayoutParams)} and
         * {@link #generateLayoutParams(android.content.Context, android.util.AttributeSet)}.</p>
         *
         * @return A new LayoutParams for a child view
         */
        public abstract LayoutParams generateDefaultLayoutParams();

        /**
         * Determines the validity of the supplied LayoutParams object.
         *
         * <p>This should check to make sure that the object is of the correct type
         * and all values are within acceptable ranges.</p>
         *
         * @param lp LayoutParams object to check
         * @return true if this LayoutParams object is valid, false otherwise
         */
        public boolean checkLayoutParams(LayoutParams lp) {
            return lp instanceof LayoutParams;
        }

        /**
         * Create a LayoutParams object suitable for this layout strategy, copying relevant
         * values from the supplied LayoutParams object if possible.
         *
         * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
         * you must also override
         * {@link #checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams)},
         * {@link #generateLayoutParams(android.view.ViewGroup.LayoutParams)} and
         * {@link #generateLayoutParams(android.content.Context, android.util.AttributeSet)}.</p>
         *
         * @param lp Source LayoutParams object to copy values from
         * @return a new LayoutParams object
         */
        public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) lp);
            } else if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) lp);
            } else {
                return new LayoutParams(lp);
            }
        }

        /**
         * Create a LayoutParams object suitable for this layout strategy from
         * an inflated layout resource.
         *
         * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
         * you must also override
         * {@link #checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams)},
         * {@link #generateLayoutParams(android.view.ViewGroup.LayoutParams)} and
         * {@link #generateLayoutParams(android.content.Context, android.util.AttributeSet)}.</p>
         *
         * @param c Context for obtaining styled attributes
         * @param attrs AttributeSet describing the supplied arguments
         * @return a new LayoutParams object
         */
        public LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
            return new LayoutParams(c, attrs);
        }

        /**
         * Scroll horizontally by dx pixels in screen coordinates and return the distance traveled.
         * The default implementation does nothing and returns 0.
         *
         * @param dx distance to scroll by in pixels. X increases as scroll position
         *           approaches the right.
         * @return The actual distance scrolled. The return value will be negative if dx was
         *         negative and scrolling proceeeded in that direction.
         *         <code>Math.abs(result)</code> may be less than dx if a boundary was reached.
         */
        public int scrollHorizontallyBy(int dx, Recycler recycler) {
            return 0;
        }

        /**
         * Scroll vertically by dy pixels in screen coordinates and return the distance traveled.
         * The default implementation does nothing and returns 0.
         *
         * @param dy distance to scroll in pixels. Y increases as scroll position
         *           approaches the bottom.
         * @return The actual distance scrolled. The return value will be negative if dy was
         *         negative and scrolling proceeeded in that direction.
         *         <code>Math.abs(result)</code> may be less than dy if a boundary was reached.
         */
        public int scrollVerticallyBy(int dy, Recycler recycler) {
            return 0;
        }

        /**
         * Query if horizontal scrolling is currently supported. The default implementation
         * returns false.
         *
         * @return True if this LayoutManager can scroll the current contents horizontally
         */
        public boolean canScrollHorizontally() {
            return false;
        }

        /**
         * Query if vertical scrolling is currently supported. The default implementation
         * returns false.
         *
         * @return True if this LayoutManager can scroll the current contents vertically
         */
        public boolean canScrollVertically() {
            return false;
        }

        /**
         * Add a view to the currently attached RecyclerView if needed. If the view has already
         * been added this method does nothing. LayoutManagers should use this method to add
         * views pulled from a {@link Recycler}, as recycled views may already be attached.
         *
         * @param child
         * @param index
         */
        public void addView(View child, int index) {
            final ViewParent oldParent = child.getParent();
            if (oldParent == mRecyclerView) {
                return;
            }
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(child);
            }

            mRecyclerView.addView(child, index);
        }

        public void addView(View child) {
            addView(child, -1);
        }

        /**
         * Called when searching for a focusable view in the given direction has failed
         * for the current content of the RecyclerView.
         *
         * <p>This is the LayoutManager's opportunity to populate views in the given direction
         * to fulfill the request if it can. The LayoutManager should attach and return
         * the view to be focused.</p>
         *
         * @param focused The currently focused view
         * @param direction One of {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
         *                  {@link View#FOCUS_LEFT}, {@link View#FOCUS_RIGHT},
         *                  {@link View#FOCUS_BACKWARD}, {@link View#FOCUS_FORWARD}
         *                  or 0 for not applicable
         * @param recycler The recycler to use for obtaining views for currently offscreen items
         * @return The chosen view to be focused
         */
        public View onFocusSearchFailed(View focused, int direction, Recycler recycler) {
            return null;
        }

        /**
         * Called when a child of the RecyclerView wants a particular rectangle to be positioned
         * onto the screen. See {@link ViewParent#requestChildRectangleOnScreen(android.view.View,
         * android.graphics.Rect, boolean)} for more details.
         *
         * <p>The base implementation will attempt to perform a standard programmatic scroll
         * to bring the given rect into view, within the padded area of the RecyclerView.</p>
         *
         * @param child The direct child making the request.
         * @param rect  The rectangle in the child's coordinates the child
         *              wishes to be on the screen.
         * @param immediate True to forbid animated or delayed scrolling,
         *                  false otherwise
         * @return Whether the group scrolled to handle the operation
         */
        public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
            final int parentLeft = mRecyclerView.getPaddingLeft();
            final int parentTop = mRecyclerView.getPaddingTop();
            final int parentRight = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();
            final int parentBottom = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom();
            final int childLeft = child.getLeft() + rect.left;
            final int childTop = child.getTop() + rect.top;
            final int childRight = childLeft + rect.right;
            final int childBottom = childTop + rect.bottom;

            final int offScreenLeft = Math.min(0, childLeft - parentLeft);
            final int offScreenTop = Math.min(0, childTop - parentTop);
            final int offScreenRight = Math.max(0, childRight - parentRight);
            final int offScreenBottom = Math.max(0, childBottom - parentBottom);

            // Favor the "start" layout direction over the end when bringing one side or the other
            // of a large rect into view.
            final int dx;
            if (ViewCompat.getLayoutDirection(mRecyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                dx = offScreenRight != 0 ? offScreenRight : offScreenLeft;
            } else {
                dx = offScreenLeft != 0 ? offScreenLeft : offScreenRight;
            }

            // Favor bringing the top into view over the bottom
            final int dy = offScreenTop != 0 ? offScreenTop : offScreenBottom;

            if (dx != 0 || dy != 0) {
                if (immediate) {
                    mRecyclerView.scrollBy(dx, dy);
                } else {
                    mRecyclerView.smoothScrollBy(dx, dy);
                }
                return true;
            }
            return false;
        }

        /**
         * Called when a descendant view of the RecyclerView requests focus.
         *
         * <p>If a LayoutManager wishes to override the default behavior of simply requesting
         * the descendant's visible area on screen it may override this method and do so.
         * A LayoutManager wishing to keep focused views aligned in a specific portion of the
         * view may implement that behavior here.</p>
         *
         * <p>If the LayoutManager executes different behavior that should override the default
         * behavior of scrolling the focused child on screen instead of running alongside it,
         * this method should return true.</p>
         *
         * @param child Direct child of the RecyclerView containing the newly focused view
         * @param focused The newly focused view. This may be the same view as child
         * @return true if the default scroll behavior should be suppressed
         */
        public boolean onRequestChildFocus(View child, View focused) {
            return false;
        }
    }

    /**
     * An ItemDecoration allows the application to add a special drawing and layout offset
     * to specific item views from the adapter's data set. This can be useful for drawing dividers
     * between items, highlights, visual grouping boundaries and more.
     *
     * <p>All ItemDecorations are drawn in the order they were added,
     * below the item views themselves.</p>
     */
    public interface ItemDecoration {
        /**
         * Draw any appropriate decorations into the Canvas supplied to the RecyclerView.
         * Any content drawn by this method will appear below item views.
         *
         * @param c Canvas to draw into
         */
        public void onDraw(Canvas c);

        /**
         * Draw any appropriate decorations into the Canvas supplied to the RecyclerView.
         * Any content drawn by this method will appear above item views.
         *
         * @param c Canvas to draw into
         */
        public void onDrawOver(Canvas c);

        /**
         * Retrieve any offsets for the given item. Each field of <code>outRect</code> specifies
         * the number of pixels that the item view should be inset by, similar to padding or margin.
         *
         * <p>If this ItemDecoration does not affect the positioning of item views it should set
         * all four fields of <code>outRect</code> (left, top, right, bottom) to zero
         * before returning.</p>
         *
         * @param outRect Rect to receive the output.
         * @param itemPosition Adapter position of the item to offset
         */
        public void getItemOffsets(Rect outRect, int itemPosition);
    }

    /**
     * An ItemTouchListener allows the application to intercept touch events in progress at the
     * view hierarchy level of the RecyclerView, before those touch events are considered for
     * RecyclerView's own scrolling behavior.
     *
     * <p>This can be useful for applications that wish to implement various forms of gestural
     * manipulation of item views within the RecyclerView. ItemTouchListeners may intercept
     * a touch interaction already in progress even if the RecyclerView is already handling that
     * gesture stream itself for the purposes of scrolling.</p>
     */
    public interface ItemTouchListener {
        /**
         * Silently observe and/or take over touch events sent to the RecyclerView
         * before they are handled by either the RecyclerView itself or its child views.
         *
         * <p>The onInterceptTouchEvent methods of each attached ItemTouchListener will be run
         * in the order in which each listener was added, before any other touch processing
         * by the RecyclerView itself or child views occurs.</p>
         *
         * @param e MotionEvent describing the touch event. All coordinates are in
         *          the RecyclerView's coordinate system.
         * @return true if this ItemTouchListener wishes to begin intercepting touch events, false
         *         to continue with the current behavior and continue observing future events in
         *         the gesture.
         */
        public boolean onInterceptTouchEvent(MotionEvent e);

        /**
         * Process a touch event as part of a gesture that was claimed by returning true from
         * a previous call to {@link #onInterceptTouchEvent}.
         *
         * @param e MotionEvent describing the touch event. All coordinates are in
         *          the RecyclerView's coordinate system.
         */
        public void onTouchEvent(MotionEvent e);
    }

    public interface OnScrollListener {
        public void onScrollStateChanged(int newState);
        public void onScrolled(int dx, int dy);
    }

    public interface RecyclerListener {
        public void onViewRecycled(ViewHolder holder);
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     *
     * <p>{@link Adapter} implementations should subclass ViewHolder and add fields for caching
     * potentially expensive {@link View#findViewById(int)} results.</p>
     *
     * <p>While {@link LayoutParams} belong to the
     * {@link android.support.v7.widget.RecyclerView.LayoutManager},
     * {@link ViewHolder ViewHolders} belong to the adapter. Adapters should feel free to use
     * their own custom ViewHolder implementations to store data that makes binding view contents
     * easier. Implementations should assume that individual item views will hold strong references
     * to <code>ViewHolder</code> objects and that <code>RecyclerView</code> instances may hold
     * strong references to extra off-screen item views for caching purposes</p>
     */
    public static abstract class ViewHolder {
        public final View itemView;

        int mPosition = NO_POSITION;
        long mItemId = NO_ID;
        int mItemViewType = INVALID_TYPE;
        boolean mIsDirty = true;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        public final int getPosition() {
            return mPosition;
        }

        public final long getItemId() {
            return mItemId;
        }

        public final int getItemViewType() {
            return mItemViewType;
        }
    }

    /**
     * Queued operation to happen when child views are update.
     */
    private static class UpdateOp {
        public static final int ADD = 0;
        public static final int REMOVE = 1;
        public static final int UPDATE = 2;

        static final int POOL_SIZE = 30;

        public int cmd;
        public int positionStart;
        public int itemCount;

        public UpdateOp(int cmd, int positionStart, int itemCount) {
            this.cmd = cmd;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }
    }

    UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount) {
        UpdateOp op = mUpdateOpPool.acquire();
        if (op == null) {
            op = new UpdateOp(cmd, positionStart, itemCount);
        } else {
            op.cmd = cmd;
            op.positionStart = positionStart;
            op.itemCount = itemCount;
        }
        return op;
    }

    void recycleUpdateOp(UpdateOp op) {
        mUpdateOpPool.release(op);
    }

    /**
     * {@link android.view.ViewGroup.LayoutParams LayoutParams} subclass for children of
     * {@link RecyclerView}. Custom {@link android.support.v7.widget.RecyclerView.LayoutManager
     * layout managers} are encouraged to create their own <code>LayoutParams</code> subclass
     * to store any additional required per-child view metadata about the layout.
     */
    public static class LayoutParams extends MarginLayoutParams {
        ViewHolder mViewHolder;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.LayoutParams) source);
        }
    }

    /**
     * Observer base class for watching changes to an {@link Adapter}.
     * See {@link Adapter#registerAdapterDataObserver(AdapterDataObserver)}.
     */
    public static abstract class AdapterDataObserver {
        public void onChanged() {
            // Do nothing
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            // do nothing
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            // do nothing
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // do nothing
        }
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }

        public void notifyChanged() {
            // since onChanged() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }

        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            // since onItemRangeChanged() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeChanged(positionStart, itemCount);
            }
        }

        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            // since onItemRangeInserted() is implemented by the app, it could do anything,
            // including removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeInserted(positionStart, itemCount);
            }
        }

        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            // since onItemRangeRemoved() is implemented by the app, it could do anything, including
            // removing itself from {@link mObservers} - and that could cause problems if
            // an iterator is used on the ArrayList {@link mObservers}.
            // to avoid such problems, just march thru the list in the reverse order.
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeRemoved(positionStart, itemCount);
            }
        }
    }
}
