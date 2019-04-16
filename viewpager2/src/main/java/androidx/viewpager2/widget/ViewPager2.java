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

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
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

    /**
     * Value to indicate that the default caching mechanism of RecyclerView should be used instead
     * of explicitly prefetch and retain pages to either side of the current page.
     * @see #setOffscreenPageLimit(int)
     */
    public static final int OFFSCREEN_PAGE_LIMIT_DEFAULT = 0;

    // reused in layout(...)
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    private CompositeOnPageChangeCallback mExternalPageChangeCallbacks =
            new CompositeOnPageChangeCallback(3);

    int mCurrentItem;
    private int mPendingCurrentItem = NO_POSITION;
    private Parcelable mPendingAdapterState;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private PagerSnapHelper mPagerSnapHelper;
    private ScrollEventAdapter mScrollEventAdapter;
    private FakeDrag mFakeDragger;
    private PageTransformerAdapter mPageTransformerAdapter;
    private boolean mUserInputEnabled = true;
    private int mOffscreenPageLimit = OFFSCREEN_PAGE_LIMIT_DEFAULT;

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

        mLayoutManager = new LinearLayoutManagerImpl(context);
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

        CompositeOnPageChangeCallback pageChangeEventDispatcher =
                new CompositeOnPageChangeCallback(3);
        mScrollEventAdapter.setOnPageChangeCallback(pageChangeEventDispatcher);

        // Callback that updates mCurrentItem after swipes. Also triggered in other cases, but in
        // all those cases mCurrentItem will only be overwritten with the same value.
        final OnPageChangeCallback currentItemUpdater = new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mCurrentItem = position;
            }
        };

        // Add currentItemUpdater before mExternalPageChangeCallbacks, because we need to update
        // internal state first
        pageChangeEventDispatcher.addOnPageChangeCallback(currentItemUpdater);
        pageChangeEventDispatcher.addOnPageChangeCallback(mExternalPageChangeCallbacks);

        // Add mPageTransformerAdapter after mExternalPageChangeCallbacks, because page transform
        // events must be fired after scroll events
        mPageTransformerAdapter = new PageTransformerAdapter(mLayoutManager);
        pageChangeEventDispatcher.addOnPageChangeCallback(mPageTransformerAdapter);

        attachViewToParent(mRecyclerView, 0, mRecyclerView.getLayoutParams());
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

    private void setOrientation(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewPager2);
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
        ss.mCurrentItem = mPendingCurrentItem == NO_POSITION ? mCurrentItem : mPendingCurrentItem;

        if (mPendingAdapterState != null) {
            ss.mAdapterState = mPendingAdapterState;
        } else {
            Adapter adapter = mRecyclerView.getAdapter();
            if (adapter instanceof StatefulAdapter) {
                ss.mAdapterState = ((StatefulAdapter) adapter).saveState();
            }
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
        mPendingCurrentItem = ss.mCurrentItem;
        mPendingAdapterState = ss.mAdapterState;
    }

    private void restorePendingState() {
        if (mPendingCurrentItem == NO_POSITION) {
            // No state to restore, or state is already restored
            return;
        }
        Adapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (mPendingAdapterState != null) {
            if (adapter instanceof StatefulAdapter) {
                ((StatefulAdapter) adapter).restoreState(mPendingAdapterState);
            }
            mPendingAdapterState = null;
        }
        // Now we have an adapter, we can clamp the pending current item and set it
        mCurrentItem = Math.max(0, Math.min(mPendingCurrentItem, adapter.getItemCount() - 1));
        mPendingCurrentItem = NO_POSITION;
        mRecyclerView.scrollToPosition(mCurrentItem);
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

        // State of ViewPager2 and its child (RecyclerView) has been restored now
        restorePendingState();
    }

    static class SavedState extends BaseSavedState {
        int mRecyclerViewId;
        int mCurrentItem;
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
            mCurrentItem = source.readInt();
            mAdapterState = source.readParcelable(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mRecyclerViewId);
            out.writeInt(mCurrentItem);
            out.writeParcelable(mAdapterState, flags);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
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
        mRecyclerView.setAdapter(adapter);
        restorePendingState();
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

    int getPageSize() {
        return getOrientation() == ORIENTATION_HORIZONTAL
                ? getWidth() - getPaddingLeft() - getPaddingRight()
                : getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * @param orientation {@link ViewPager2.Orientation}
     */
    public void setOrientation(@Orientation int orientation) {
        mLayoutManager.setOrientation(orientation);
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

        // 1. Preprocessing (check state, validate item, decide if update is necessary, etc)

        if (isFakeDragging()) {
            throw new IllegalStateException("Cannot change current item when ViewPager2 is fake "
                    + "dragging");
        }
        Adapter adapter = getAdapter();
        if (adapter == null) {
            // Update the pending current item if we're still waiting for the adapter
            if (mPendingCurrentItem != NO_POSITION) {
                mPendingCurrentItem = Math.max(item, 0);
            }
            return;
        }
        if (adapter.getItemCount() <= 0) {
            // Adapter is empty
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

        // 2. Update the item internally

        float previousItem = mCurrentItem;
        mCurrentItem = item;

        if (!mScrollEventAdapter.isIdle()) {
            // Scroll in progress, overwrite previousItem with actual current position
            previousItem = mScrollEventAdapter.getRelativeScrollPosition();
        }

        // 3. Perform the necessary scroll actions on RecyclerView

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
     * <p>Set the number of pages that should be retained to either side of the currently visible
     * page(s). Pages beyond this limit will be recreated from the adapter when needed. Set this to
     * {@link #OFFSCREEN_PAGE_LIMIT_DEFAULT} to use RecyclerView's caching strategy. The given value
     * must either be larger than 0, or {@link #OFFSCREEN_PAGE_LIMIT_DEFAULT}.</p>
     *
     * <p>Pages within {@code limit} pages away from the current page are created and added to the
     * view hierarchy, even though they are not visible on the screen. Pages outside this limit will
     * be removed from the view hierarchy, but the {@code ViewHolder}s will be recycled as usual by
     * {@link RecyclerView}.</p>
     *
     * <p>This is offered as an optimization. If you know in advance the number of pages you will
     * need to support or have lazy-loading mechanisms in place on your pages, tweaking this setting
     * can have benefits in perceived smoothness of paging animations and interaction. If you have a
     * small number of pages (3-4) that you can keep active all at once, less time will be spent in
     * layout for newly created view subtrees as the user pages back and forth.</p>
     *
     * <p>You should keep this limit low, especially if your pages have complex layouts. By default
     * it is set to {@code OFFSCREEN_PAGE_LIMIT_DEFAULT}.</p>
     *
     * @param limit How many pages will be kept offscreen on either side
     * @see #getOffscreenPageLimit()
     */
    public void setOffscreenPageLimit(int limit) {
        if (limit < 1 && limit != OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            throw new IllegalArgumentException(
                    "Offscreen page limit must be OFFSCREEN_PAGE_LIMIT_DEFAULT or a number > 0");
        }
        mOffscreenPageLimit = limit;
        // Trigger layout so prefetch happens through getExtraLayoutSize()
        mRecyclerView.requestLayout();
    }

    /**
     * Returns the number of pages that will be retained to either side of the current page in the
     * view hierarchy in an idle state. Defaults to {@link #OFFSCREEN_PAGE_LIMIT_DEFAULT}.
     *
     * @return How many pages will be kept offscreen on either side
     * @see #setOffscreenPageLimit(int)
     */
    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
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
        if (transformer == mPageTransformerAdapter.getPageTransformer()) {
            return;
        }
        mPageTransformerAdapter.setPageTransformer(transformer);
        requestTransform();
    }

    /**
     * Trigger a call to the registered {@link PageTransformer PageTransformer}'s {@link
     * PageTransformer#transformPage(View, float) transformPage} method. Call this when something
     * has changed which has invalidated the transformations defined by the {@code PageTransformer}
     * that did not trigger a page scroll.
     */
    public void requestTransform() {
        if (mPageTransformerAdapter.getPageTransformer() == null) {
            return;
        }
        float relativePosition = mScrollEventAdapter.getRelativeScrollPosition();
        int position = (int) relativePosition;
        float positionOffset = relativePosition - position;
        int positionOffsetPx = Math.round(getPageSize() * positionOffset);
        mPageTransformerAdapter.onPageScrolled(position, positionOffset, positionOffsetPx);
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
        public CharSequence getAccessibilityClassName() {
            return "androidx.viewpager.widget.ViewPager";
        }

        @Override
        public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            event.setFromIndex(mCurrentItem);
            event.setToIndex(mCurrentItem);
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

    /**
     * Slightly modified LinearLayoutManager to adjust accessibility when user scrolling is
     * disabled.
     */
    private class LinearLayoutManagerImpl extends LinearLayoutManager {
        LinearLayoutManagerImpl(Context context) {
            super(context);
        }

        @Override
        public boolean performAccessibilityAction(@NonNull RecyclerView.Recycler recycler,
                @NonNull RecyclerView.State state, int action, @Nullable Bundle args) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                    if (!isUserInputEnabled()) {
                        return false;
                    }
                    break;
            }
            return super.performAccessibilityAction(recycler, state, action, args);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull RecyclerView.Recycler recycler,
                @NonNull RecyclerView.State state, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(recycler, state, info);
            if (!isUserInputEnabled()) {
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                info.setScrollable(false);
            }
        }

        @Override
        protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state,
                @NonNull int[] extraLayoutSpace) {
            int pageLimit = getOffscreenPageLimit();
            if (pageLimit == OFFSCREEN_PAGE_LIMIT_DEFAULT) {
                // Only do custom prefetching of offscreen pages if requested
                super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                return;
            }
            final int offscreenSpace = getPageSize() * pageLimit;
            extraLayoutSpace[0] = offscreenSpace;
            extraLayoutSpace[1] = offscreenSpace;
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
