/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.viewpager2.widget;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_PAGE_DOWN;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_PAGE_LEFT;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_PAGE_RIGHT;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_PAGE_UP;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.viewpager2.R;
import androidx.viewpager2.adapter.StatefulAdapter;

import java.lang.annotation.Retention;

/**
 * ViewPager2 replaces {@link androidx.viewpager.widget.ViewPager}, addressing most of its
 * predecessor’s pain-points, including right-to-left layout support, vertical orientation,
 * modifiable Fragment collections, etc.
 *
 * @see androidx.viewpager.widget.ViewPager
 */
public final class ViewPager2 extends ViewGroup {
    @Retention(SOURCE)
    @IntDef({ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL})
    public @interface Orientation {
    }

    public static final int ORIENTATION_HORIZONTAL = RecyclerView.HORIZONTAL;
    public static final int ORIENTATION_VERTICAL = RecyclerView.VERTICAL;

    @Retention(SOURCE)
    @IntDef({SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING})
    public @interface ScrollState {
    }

    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    private static final AccessibilityViewCommand ACTION_PAGE_FORWARD =
            new AccessibilityViewCommand() {
        @Override
        public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
            ViewPager2 viewPager = (ViewPager2) view;
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            return true;
        }
    };

    private static final AccessibilityViewCommand ACTION_PAGE_BACKWARD =
            new AccessibilityViewCommand() {
        @Override
        public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
            ViewPager2 viewPager = (ViewPager2) view;
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
            return true;
        }
    };

    // reused in layout(...)
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    private CompositeOnPageChangeCallback mExternalPageChangeCallbacks =
            new CompositeOnPageChangeCallback(3);

    int mCurrentItem;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private PagerSnapHelper mPagerSnapHelper;
    private ScrollEventAdapter mScrollEventAdapter;
    private FakeDrag mFakeDragger;
    private PageTransformerAdapter mPageTransformerAdapter;
    private CompositeOnPageChangeCallback mPageChangeEventDispatcher;
    private boolean mUserInputEnabled = true;
    private RecyclerView.AdapterDataObserver mAdapterDataObserver;

    public ViewPager2(@NonNull Context context) {
        super(context);
        initialize(context, null);
    }

    public ViewPager2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public ViewPager2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    @RequiresApi(21)
    public ViewPager2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        mRecyclerView = new RecyclerViewImpl(context);
        mRecyclerView.setId(ViewCompat.generateViewId());
        ViewCompat.setImportantForAccessibility(mRecyclerView,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        setOrientation(context, attrs);

        mRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRecyclerView.addOnChildAttachStateChangeListener(enforceChildFillListener());

        // Create ScrollEventAdapter before attaching PagerSnapHelper to RecyclerView, because the
        // attach process calls PagerSnapHelperImpl.findSnapView, which uses the mScrollEventAdapter
        mScrollEventAdapter = new ScrollEventAdapter(mLayoutManager);
        // Create FakeDrag before attaching PagerSnapHelper, same reason as above
        mFakeDragger = new FakeDrag(this, mScrollEventAdapter, mRecyclerView);
        mPagerSnapHelper = new PagerSnapHelperImpl();
        mPagerSnapHelper.attachToRecyclerView(mRecyclerView);
        // Add mScrollEventAdapter after attaching mPagerSnapHelper to mRecyclerView, because we
        // don't want to respond on the events sent out during the attach process
        mRecyclerView.addOnScrollListener(mScrollEventAdapter);

        mPageChangeEventDispatcher = new CompositeOnPageChangeCallback(3);
        mScrollEventAdapter.setOnPageChangeCallback(mPageChangeEventDispatcher);

        mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updatePageAccessibilityActions();
            }
        };

        // Callback that updates mCurrentItem after swipes. Also triggered in other cases, but in
        // all those cases mCurrentItem will only be overwritten with the same value.
        final OnPageChangeCallback currentItemUpdater = new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (mCurrentItem != position) {
                    mCurrentItem = position;
                    updatePageAccessibilityActions();
                }
            }
        };

        // Add currentItemUpdater before mExternalPageChangeCallbacks, because we need to update
        // internal state first
        mPageChangeEventDispatcher.addOnPageChangeCallback(currentItemUpdater);
        mPageChangeEventDispatcher.addOnPageChangeCallback(mExternalPageChangeCallbacks);

        // Add mPageTransformerAdapter after mExternalPageChangeCallbacks, because page transform
        // events must be fired after scroll events
        mPageTransformerAdapter = new PageTransformerAdapter(mLayoutManager);
        mPageChangeEventDispatcher.addOnPageChangeCallback(mPageTransformerAdapter);

        attachViewToParent(mRecyclerView, 0, mRecyclerView.getLayoutParams());

        if (ViewCompat.getImportantForAccessibility(this)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    /**
     * A lot of places in code rely on an assumption that the page fills the whole ViewPager2.
     *
     * TODO(b/70666617) Allow page width different than width/height 100%/100%
     */
    private RecyclerView.OnChildAttachStateChangeListener enforceChildFillListener() {
        return new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                RecyclerView.LayoutParams layoutParams =
                        (RecyclerView.LayoutParams) view.getLayoutParams();
                if (layoutParams.width != LayoutParams.MATCH_PARENT
                        || layoutParams.height != LayoutParams.MATCH_PARENT) {
                    throw new IllegalStateException(
                            "Pages must fill the whole ViewPager2 (use match_parent)");
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // nothing
            }
        };
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return "androidx.viewpager.widget.ViewPager";
    }

    /**
     * Update the ViewPager2's available page accessibility actions. These are updated in response
     * to page, adapter, and orientation changes.
     */
    void updatePageAccessibilityActions() {
        ViewCompat.removeAccessibilityAction(this, ACTION_PAGE_LEFT.getId());
        ViewCompat.removeAccessibilityAction(this, ACTION_PAGE_RIGHT.getId());
        ViewCompat.removeAccessibilityAction(this, ACTION_PAGE_UP.getId());
        ViewCompat.removeAccessibilityAction(this, ACTION_PAGE_DOWN.getId());

        if (getAdapter() == null) {
            return;
        }

        int itemCount = getAdapter().getItemCount();
        if (itemCount == 0) {
            return;
        }

        if (!isUserInputEnabled()) {
            return;
        }

        if (getOrientation() == ORIENTATION_HORIZONTAL) {
            boolean isLayoutRtl = isLayoutRtl();
            AccessibilityNodeInfoCompat.AccessibilityActionCompat actionPageForward =
                    isLayoutRtl ? ACTION_PAGE_LEFT : ACTION_PAGE_RIGHT;
            AccessibilityNodeInfoCompat.AccessibilityActionCompat actionPageBackward =
                    isLayoutRtl ? ACTION_PAGE_RIGHT : ACTION_PAGE_LEFT;

            if (mCurrentItem < itemCount - 1) {
                ViewCompat.replaceAccessibilityAction(this, actionPageForward, null,
                        ACTION_PAGE_FORWARD);
            }
            if (mCurrentItem > 0) {
                ViewCompat.replaceAccessibilityAction(this, actionPageBackward, null,
                        ACTION_PAGE_BACKWARD);
            }
        } else {
            if (mCurrentItem < itemCount - 1) {
                ViewCompat.replaceAccessibilityAction(this, ACTION_PAGE_DOWN, null,
                        ACTION_PAGE_FORWARD);
            }
            if (mCurrentItem > 0) {
                ViewCompat.replaceAccessibilityAction(this, ACTION_PAGE_UP, null,
                        ACTION_PAGE_BACKWARD);
            }
        }
    }

    private boolean isLayoutRtl() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    private void setOrientation(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewPager2);
        if (BuildCompat.isAtLeastQ()) {
            saveAttributeDataForStyleable(context, R.styleable.ViewPager2, attrs, a, 0, 0);
        }
        try {
            setOrientation(
                    a.getInt(R.styleable.ViewPager2_android_orientation, ORIENTATION_HORIZONTAL));
        } finally {
            a.recycle();
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.mRecyclerViewId = mRecyclerView.getId();
        ss.mOrientation = getOrientation();
        ss.mCurrentItem = mCurrentItem;
        ss.mUserScrollable = mUserInputEnabled;
        ss.mScrollInProgress =
                mLayoutManager.findFirstCompletelyVisibleItemPosition() != mCurrentItem;

        Adapter adapter = mRecyclerView.getAdapter();
        if (adapter instanceof StatefulAdapter) {
            ss.mAdapterState = ((StatefulAdapter) adapter).saveState();
        }

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setOrientation(ss.mOrientation);
        mCurrentItem = ss.mCurrentItem;
        setUserInputEnabled(ss.mUserScrollable);
        if (ss.mScrollInProgress) {
            // A scroll was in progress, so the RecyclerView is not at mCurrentItem right now. Move
            // it to mCurrentItem instantly in the _next_ frame, as RecyclerView is not yet fired up
            // at this moment. Remove the event dispatcher during this time, as it will fire a
            // scroll event for the current position, which has already been fired before the config
            // change.
            final ScrollEventAdapter scrollEventAdapter = mScrollEventAdapter;
            final OnPageChangeCallback eventDispatcher = mPageChangeEventDispatcher;
            scrollEventAdapter.setOnPageChangeCallback(null);
            final RecyclerView recyclerView = mRecyclerView; // to avoid a synthetic accessor
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    scrollEventAdapter.setOnPageChangeCallback(eventDispatcher);
                    scrollEventAdapter.notifyRestoreCurrentItem(mCurrentItem);
                    recyclerView.scrollToPosition(mCurrentItem);
                }
            });
        } else {
            mScrollEventAdapter.notifyRestoreCurrentItem(mCurrentItem);
        }

        if (ss.mAdapterState != null) {
            Adapter adapter = mRecyclerView.getAdapter();
            if (adapter instanceof StatefulAdapter) {
                ((StatefulAdapter) adapter).restoreState(ss.mAdapterState);
            }
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // RecyclerView changed an id, so we need to reflect that in the saved state
        Parcelable state = container.get(getId());
        if (state instanceof SavedState) {
            final int previousRvId = ((SavedState) state).mRecyclerViewId;
            final int currentRvId = mRecyclerView.getId();
            container.put(currentRvId, container.get(previousRvId));
            container.remove(previousRvId);
        }

        super.dispatchRestoreInstanceState(container);
    }

    static class SavedState extends BaseSavedState {
        int mRecyclerViewId;
        @Orientation int mOrientation;
        int mCurrentItem;
        boolean mUserScrollable;
        boolean mScrollInProgress;
        Parcelable mAdapterState;

        @RequiresApi(24)
        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            readValues(source, loader);
        }

        SavedState(Parcel source) {
            super(source);
            readValues(source, null);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        private void readValues(Parcel source, ClassLoader loader) {
            mRecyclerViewId = source.readInt();
            mOrientation = source.readInt();
            mCurrentItem = source.readInt();
            mUserScrollable = source.readByte() != 0;
            mScrollInProgress = source.readByte() != 0;
            mAdapterState = source.readParcelable(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mRecyclerViewId);
            out.writeInt(mOrientation);
            out.writeInt(mCurrentItem);
            out.writeByte((byte) (mUserScrollable ? 1 : 0));
            out.writeByte((byte) (mScrollInProgress ? 1 : 0));
            out.writeParcelable(mAdapterState, flags);
        }

        static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source, ClassLoader loader) {
                return Build.VERSION.SDK_INT >= 24
                        ? new SavedState(source, loader)
                        : new SavedState(source);
            }

            @Override
            public SavedState createFromParcel(Parcel source) {
                return createFromParcel(source, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * @see androidx.viewpager2.adapter.FragmentStateAdapter
     * @see RecyclerView#setAdapter(Adapter)
     */
    public void setAdapter(@Nullable Adapter adapter) {
        Adapter oldAdapter = mRecyclerView.getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }

        mRecyclerView.setAdapter(adapter);
        updatePageAccessibilityActions();
        adapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    public @Nullable Adapter getAdapter() {
        return mRecyclerView.getAdapter();
    }

    @Override
    public void onViewAdded(View child) {
        // TODO(b/70666620): consider adding a support for Decor views
        throw new IllegalStateException(
                getClass().getSimpleName() + " does not support direct child views");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO(b/70666622): consider margin support
        // TODO(b/70666626): consider delegating all this to RecyclerView
        // TODO(b/70666625): write automated tests for this

        measureChild(mRecyclerView, widthMeasureSpec, heightMeasureSpec);
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();
        int childState = mRecyclerView.getMeasuredState();

        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        width = Math.max(width, getSuggestedMinimumWidth());
        height = Math.max(height, getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = mRecyclerView.getMeasuredWidth();
        int height = mRecyclerView.getMeasuredHeight();

        // TODO(b/70666626): consider delegating padding handling to the RecyclerView to avoid
        // an unnatural page transition effect: http://shortn/_Vnug3yZpQT
        mTmpContainerRect.left = getPaddingLeft();
        mTmpContainerRect.right = r - l - getPaddingRight();
        mTmpContainerRect.top = getPaddingTop();
        mTmpContainerRect.bottom = b - t - getPaddingBottom();

        Gravity.apply(Gravity.TOP | Gravity.START, width, height, mTmpContainerRect, mTmpChildRect);
        mRecyclerView.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right,
                mTmpChildRect.bottom);
    }

    /**
     * @param orientation {@link ViewPager2.Orientation}
     */
    public void setOrientation(@Orientation int orientation) {
        mLayoutManager.setOrientation(orientation);
        updatePageAccessibilityActions();
    }

    public @Orientation int getOrientation() {
        return mLayoutManager.getOrientation();
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item. Silently ignored if the adapter is not set or
     * empty. Clamps item to the bounds of the adapter.
     *
     * TODO(b/123069219): verify first layout behavior
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        setCurrentItem(item, true);
    }

    /**
     * Set the currently selected page. If {@code smoothScroll = true}, will perform a smooth
     * animation from the current item to the new item. Silently ignored if the adapter is not set
     * or empty. Clamps item to the bounds of the adapter.
     *
     * @param item Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (isFakeDragging()) {
            throw new IllegalStateException("Cannot change current item when ViewPager2 is fake "
                    + "dragging");
        }
        Adapter adapter = getAdapter();
        if (adapter == null || adapter.getItemCount() <= 0) {
            return;
        }
        item = Math.max(item, 0);
        item = Math.min(item, adapter.getItemCount() - 1);

        if (item == mCurrentItem && mScrollEventAdapter.isIdle()) {
            // Already at the correct page
            return;
        }
        if (item == mCurrentItem && smoothScroll) {
            // Already scrolling to the correct page, but not yet there. Only handle instant scrolls
            // because then we need to interrupt the current smooth scroll.
            return;
        }

        float previousItem = mCurrentItem;
        mCurrentItem = item;
        updatePageAccessibilityActions();

        if (!mScrollEventAdapter.isIdle()) {
            // Scroll in progress, overwrite previousItem with actual current position
            previousItem = mScrollEventAdapter.getRelativeScrollPosition();
        }

        mScrollEventAdapter.notifyProgrammaticScroll(item, smoothScroll);
        if (!smoothScroll) {
            mRecyclerView.scrollToPosition(item);
            return;
        }

        // For smooth scroll, pre-jump to nearby item for long jumps.
        if (Math.abs(item - previousItem) > 3) {
            mRecyclerView.scrollToPosition(item > previousItem ? item - 3 : item + 3);
            // TODO(b/114361680): call smoothScrollToPosition synchronously (blocked by b/114019007)
            mRecyclerView.post(new SmoothScrollToPosition(item, mRecyclerView));
        } else {
            mRecyclerView.smoothScrollToPosition(item);
        }
    }

    /**
     * Returns the currently selected page. If no page can sensibly be selected because there is no
     * adapter or the adapter is empty, returns 0.
     *
     * @return Currently selected page
     */
    public int getCurrentItem() {
        return mCurrentItem;
    }

    /**
     * Returns the current scroll state of the ViewPager2. Returned value is one of can be one of
     * {@link #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}.
     *
     * @return The scroll state that was last dispatched to {@link
     *         OnPageChangeCallback#onPageScrollStateChanged(int)}
     */
    @ScrollState
    public int getScrollState() {
        return mScrollEventAdapter.getScrollState();
    }

    /**
     * Start a fake drag of the pager.
     *
     * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager2 with the
     * touch scrolling of another view, while still letting the ViewPager2 control the snapping
     * motion and fling behavior. (e.g. parallax-scrolling tabs.) Call {@link #fakeDragBy(float)} to
     * simulate the actual drag motion. Call {@link #endFakeDrag()} to complete the fake drag and
     * fling as necessary.
     *
     * <p>A fake drag can be interrupted by a real drag. From that point on, all calls to {@code
     * fakeDragBy} and {@code endFakeDrag} will be ignored until the next fake drag is started by
     * calling {@code beginFakeDrag}. If you need the ViewPager2 to ignore touch events and other
     * user input during a fake drag, use {@link #setUserInputEnabled(boolean)}. If a real or fake
     * drag is already in progress, this method will return {@code false}.
     *
     * @return {@code true} if the fake drag began successfully, {@code false} if it could not be
     *         started
     *
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     * @see #isFakeDragging()
     */
    public boolean beginFakeDrag() {
        return mFakeDragger.beginFakeDrag();
    }

    /**
     * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first. Drag
     * happens in the direction of the orientation. Positive offsets will drag to the previous page,
     * negative values to the next page, with one exception: if layout direction is set to RTL and
     * the ViewPager2's orientation is horizontal, then the behavior will be inverted. This matches
     * the deltas of touch events that would cause the same real drag.
     *
     * <p>If the pager is not in the fake dragging state anymore, it ignores this call and returns
     * {@code false}.
     *
     * @param offsetPxFloat Offset in pixels to drag by
     * @return {@code true} if the fake drag was executed. If {@code false} is returned, it means
     *         there was no fake drag to end.
     *
     * @see #beginFakeDrag()
     * @see #endFakeDrag()
     * @see #isFakeDragging()
     */
    public boolean fakeDragBy(float offsetPxFloat) {
        return mFakeDragger.fakeDragBy(offsetPxFloat);
    }

    /**
     * End a fake drag of the pager.
     *
     * @return {@code true} if the fake drag was ended. If {@code false} is returned, it means there
     *         was no fake drag to end.
     *
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     * @see #isFakeDragging()
     */
    public boolean endFakeDrag() {
        return mFakeDragger.endFakeDrag();
    }

    /**
     * Returns {@code true} if a fake drag is in progress.
     *
     * @return {@code true} if currently in a fake drag, {@code false} otherwise.
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean isFakeDragging() {
        return mFakeDragger.isFakeDragging();
    }

    /**
     * Snaps the ViewPager2 to the closest page
     */
    void snapToPage() {
        // Method copied from PagerSnapHelper#snapToTargetExistingView
        // When fixing something here, make sure to update that method as well
        View view = mPagerSnapHelper.findSnapView(mLayoutManager);
        if (view == null) {
            return;
        }
        int[] snapDistance = mPagerSnapHelper.calculateDistanceToFinalSnap(mLayoutManager, view);
        //noinspection ConstantConditions
        if (snapDistance[0] != 0 || snapDistance[1] != 0) {
            mRecyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
        }
    }

    /**
     * Enable or disable user initiated scrolling. This includes touch input (scroll and fling
     * gestures) and accessibility input. Disabling keyboard input is not yet supported. When user
     * initiated scrolling is disabled, programmatic scrolls through {@link #setCurrentItem(int,
     * boolean) setCurrentItem} still work. By default, user initiated scrolling is enabled.
     *
     * @param enabled {@code true} to allow user initiated scrolling, {@code false} to block user
     *        initiated scrolling
     * @see #isUserInputEnabled()
     */
    public void setUserInputEnabled(boolean enabled) {
        mUserInputEnabled = enabled;
        updatePageAccessibilityActions();
    }

    /**
     * Returns if user initiated scrolling between pages is enabled. Enabled by default.
     *
     * @return {@code true} if users can scroll the ViewPager2, {@code false} otherwise
     * @see #setUserInputEnabled(boolean)
     */
    public boolean isUserInputEnabled() {
        return mUserInputEnabled;
    }

    /**
     * Add a callback that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link OnPageChangeCallback}.
     *
     * <p>Components that add a callback should take care to remove it when finished.
     *
     * @param callback callback to add
     */
    public void registerOnPageChangeCallback(@NonNull OnPageChangeCallback callback) {
        mExternalPageChangeCallbacks.addOnPageChangeCallback(callback);
    }

    /**
     * Remove a callback that was previously added via
     * {@link #registerOnPageChangeCallback(OnPageChangeCallback)}.
     *
     * @param callback callback to remove
     */
    public void unregisterOnPageChangeCallback(@NonNull OnPageChangeCallback callback) {
        mExternalPageChangeCallbacks.removeOnPageChangeCallback(callback);
    }

    /**
     * Sets a {@link PageTransformer} that will be called for each attached page whenever the
     * scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding behavior.
     *
     * @param transformer PageTransformer that will modify each page's animation properties
     */
    public void setPageTransformer(@Nullable PageTransformer transformer) {
        // TODO: add support for reverseDrawingOrder: b/112892792
        // TODO: add support for pageLayerType: b/112893074
        mPageTransformerAdapter.setPageTransformer(transformer);
    }

    @Override
    @RequiresApi(17)
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);
        updatePageAccessibilityActions();
    }

    /**
     * Slightly modified RecyclerView to get ViewPager behavior in accessibility and to
     * enable/disable user scrolling.
     */
    private class RecyclerViewImpl extends RecyclerView {
        RecyclerViewImpl(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            event.setFromIndex(mCurrentItem);
            event.setToIndex(mCurrentItem);
            event.setSource(ViewPager2.this);
            event.setClassName(ViewPager2.this.getAccessibilityClassName());
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return isUserInputEnabled() && super.onTouchEvent(event);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return isUserInputEnabled() && super.onInterceptTouchEvent(ev);
        }
    }

    private class PagerSnapHelperImpl extends PagerSnapHelper {
        PagerSnapHelperImpl() {
        }

        @Nullable
        @Override
        public View findSnapView(RecyclerView.LayoutManager layoutManager) {
            // When interrupting a smooth scroll with a fake drag, we stop RecyclerView's scroll
            // animation, which fires a scroll state change to IDLE. PagerSnapHelper then kicks in
            // to snap to a page, which we need to prevent here.
            // Simplifying that case: during a fake drag, no snapping should occur.
            return isFakeDragging() ? null : super.findSnapView(layoutManager);
        }
    }

    private static class SmoothScrollToPosition implements Runnable {
        private final int mPosition;
        private final RecyclerView mRecyclerView;

        SmoothScrollToPosition(int position, RecyclerView recyclerView) {
            mPosition = position;
            mRecyclerView = recyclerView; // to avoid a synthetic accessor
        }

        @Override
        public void run() {
            mRecyclerView.smoothScrollToPosition(mPosition);
        }
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public abstract static class OnPageChangeCallback {
        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position Position index of the first page currently being displayed.
         *                 Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        public void onPageScrolled(int position, float positionOffset,
                @Px int positionOffsetPixels) {
        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        public void onPageSelected(int position) {
        }

        /**
         * Called when the scroll state changes. Useful for discovering when the user begins
         * dragging, when a fake drag is started, when the pager is automatically settling to the
         * current page, or when it is fully stopped/idle. {@code state} can be one of {@link
         * #SCROLL_STATE_IDLE}, {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}.
         */
        public void onPageScrollStateChanged(@ScrollState int state) {
        }
    }

    /**
     * A PageTransformer is invoked whenever a visible/attached page is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the page views using animation properties.
     *
     * <p>As property animation is only supported as of Android 3.0 and forward,
     * setting a PageTransformer on a ViewPager on earlier platform versions will
     * be ignored.</p>
     */
    public interface PageTransformer {

        /**
         * Apply a property transformation to the given page.
         *
         * @param page Apply the transformation to this page
         * @param position Position of page relative to the current front-and-center
         *                 position of the pager. 0 is front and center. 1 is one full
         *                 page position to the right, and -1 is one page position to the left.
         */
        void transformPage(@NonNull View page, float position);
    }
}
