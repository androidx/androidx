/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.app;

import android.os.Parcel;
import android.os.Parcelable;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Layout manager that allows the user to flip left and right
 * through pages of data.  The pages are implemented as fragments;
 * you need to implement {@link Adapter} to tell this class the
 * number of pages you have and the fragment to use for each page.
 */
public class FragmentPager extends ViewGroup {
    /**
     * Special kind of Adapter for supplying fragments to the FragmentPager.
     */
    public interface Adapter {
        /**
         * Return the number of fragments available.
         */
        int getCount();

        /**
         * Return the Fragment associated with a specified position.
         */
        Fragment getItem(int position);

        FragmentManager getSupportFragmentManager();
    }

    private static final String TAG = "FragmentPager";
    private static final boolean DEBUG = false;

    private static final boolean USE_CACHE = false;

    static class ItemInfo {
        Fragment fragment;
        int position;
    }

    private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();

    private Adapter mAdapter;
    private int mCurItem;   // Index of currently displayed fragment.
    private int mRestoredCurItem = -1;
    private Scroller mScroller;

    private int mChildWidthMeasureSpec;
    private int mChildHeightMeasureSpec;
    private boolean mInLayout;

    private boolean mScrollingCacheEnabled;

    private boolean mPopulatePending;

    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private int mTouchSlop;
    private float mInitialMotionX;
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    public FragmentPager(Context context) {
        super(context);
        initFragmentPager();
    }

    public FragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFragmentPager();
    }

    void initFragmentPager() {
        setWillNotDraw(false);
        mScroller = new Scroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;

        if (mAdapter != null) {
            mPopulatePending = false;
            if (mRestoredCurItem >= 0) {
                setCurrentItemInternal(mRestoredCurItem, false, true);
                mRestoredCurItem = -1;
            } else {
                populate();
            }
        }
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    public void setCurrentItem(int item) {
        mPopulatePending = false;
        setCurrentItemInternal(item, true, false);
    }

    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (item < 0) {
            item = 0;
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1;
        }
        mCurItem = item;
        populate();
        if (smoothScroll) {
            smoothScrollTo(getWidth()*item, 0);
        } else {
            completeScroll();
            scrollTo(getWidth()*item, 0);
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        setScrollingCacheEnabled(true);
        int sx = getScrollX();
        int sy = getScrollY();
        mScroller.startScroll(sx, sy, x-sx, y-sy);
        invalidate();
    }

    String makeFragmentName(int index) {
        return "android:switcher:" + getId() + ":" + index;
    }

    FragmentTransaction addNewItem(FragmentManager fm, int position, int index,
            FragmentTransaction ft) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;

        // Do we already have this fragment?
        if (ft == null) {
            ft = fm.beginTransaction();
        }

        String name = makeFragmentName(ii.position);
        ii.fragment = fm.findFragmentByTag(name);
        if (ii.fragment != null) {
            if (DEBUG) Log.v(TAG, "Attaching item #" + ii.position + ": f=" + ii.fragment);
            ft.attach(ii.fragment);
        } else {
            ii.fragment = mAdapter.getItem(position);
            if (DEBUG) Log.v(TAG, "Adding item #" + ii.position + ": f=" + ii.fragment);
            ft.add(getId(), ii.fragment, makeFragmentName(ii.position));
        }

        if (index < 0) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        }

        return ft;
    }

    void populate() {
        if (mAdapter == null) {
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            return;
        }

        final FragmentManager fm = mAdapter.getSupportFragmentManager();

        final int startIdx = mCurItem > 0 ? mCurItem - 1 : mCurItem;
        final int endIdx = mCurItem+1;

        int lastIdx = 0;
        FragmentTransaction ft = null;

        for (int i=0; i<mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.position < startIdx || ii.position > endIdx) {
                mItems.remove(i);
                i--;
                if (ft == null) {
                    ft = fm.beginTransaction();
                }
                if (DEBUG) Log.v(TAG, "Detaching item #" + ii.position + ": f=" + ii.fragment
                        + " v=" + ii.fragment.getView());
                ft.detach(ii.fragment);
            } else {
                lastIdx = ii.position + 1;
            }
        }

        if (mItems.size() > 0) {
            int newStartIdx = mItems.get(0).position;
            while (newStartIdx > startIdx) {
                newStartIdx--;
                ft = addNewItem(fm, newStartIdx, 0, ft);
            }
        }

        while (lastIdx <= endIdx) {
            if (lastIdx < mAdapter.getCount()) {
                ft = addNewItem(fm, lastIdx, -1, ft);
            }
            lastIdx++;
        }

        if (ft != null) {
            ft.commit();
            fm.executePendingTransactions();
        }
    }

    public static class SavedState extends BaseSavedState {
        int position;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
        }

        @Override
        public String toString() {
            return "FragmentPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            position = in.readInt();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.position = mCurItem;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (mAdapter != null) {
            setCurrentItemInternal(ss.position, false, true);
        } else {
            mRestoredCurItem = ss.position;
        }
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (mInLayout) {
            addViewInLayout(child, index, params);
            child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
        } else {
            super.addView(child, index, params);
        }

        if (USE_CACHE) {
            if (child.getVisibility() != GONE) {
                child.setDrawingCacheEnabled(mScrollingCacheEnabled);
            } else {
                child.setDrawingCacheEnabled(false);
            }
        }
    }

    ItemInfo infoForChild(View child) {
        for (int i=0; i<mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.fragment.getView() == child) {
                return ii;
            }
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // For simple implementation, or internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        // Children are just made to fill our space.
        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() -
                getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() -
                getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        mPopulatePending = false;
        populate();
        mInLayout = false;

        // Make sure all children have been properly measured.
        final int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (DEBUG) Log.v(TAG, "Measuring #" + i + " " + child
		        + ": " + mChildWidthMeasureSpec);
                child.measure(mChildWidthMeasureSpec, mChildHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Make sure scroll position is set correctly.
        int scrollPos = mCurItem*w;
        if (scrollPos != getScrollX()) {
            completeScroll();
            scrollTo(scrollPos, getScrollY());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        mPopulatePending = false;
        populate();
        mInLayout = false;

        final int count = getChildCount();
        final int width = r-l;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            ItemInfo ii;
            if (child.getVisibility() != GONE && (ii=infoForChild(child)) != null) {
                int loff = width*ii.position;
                int childLeft = getPaddingLeft() + loff;
                int childTop = getPaddingTop();
                if (DEBUG) Log.v(TAG, "Positioning #" + i + " " + child + " f=" + ii.fragment
		        + ":" + childLeft + "," + childTop + " " + child.getMeasuredWidth()
		        + "x" + child.getMeasuredHeight());
                child.layout(childLeft, childTop,
                        childLeft + child.getMeasuredWidth(),
                        childTop + child.getMeasuredHeight());
            }
        }
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished()) {
            if (mScroller.computeScrollOffset()) {
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();

                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                }

                // Keep on drawing until the animation has finished.
                invalidate();
            } else {
                // Done with scroll, clean up state.
                completeScroll();
            }
        }
    }

    private void completeScroll() {
        // Done with scroll, no longer want to cache view drawing.
        setScrollingCacheEnabled(false);
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
        }
        mPopulatePending = false;
        populate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!");
            mIsBeingDragged = false;
            mIsUnableToDrag = false;
            mActivePointerId = INVALID_POINTER;
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!");
                return true;
            }
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!");
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
                if (xDiff > mTouchSlop && xDiff > yDiff) {
                    if (DEBUG) Log.v(TAG, "Starting drag!");
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                    setScrollingCacheEnabled(true);
                } else {
                    if (yDiff > mTouchSlop) {
                        // The finger has moved enough in the vertical
                        // direction to be counted as a drag...  abort
                        // any attempt to drag horizontally, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG) Log.v(TAG, "Starting unable to drag!");
                        mIsUnableToDrag = true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);

                completeScroll();
                mIsBeingDragged = false;
                mIsUnableToDrag = false;

                if (DEBUG) Log.v(TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                        + " mIsBeingDragged=" + mIsBeingDragged
                        + "mIsUnableToDrag=" + mIsUnableToDrag);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // Don't handle edge touches immediately -- they may actually belong to one of our
            // descendants.
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                completeScroll();

                // Remember where the motion event started
                mLastMotionX = mInitialMotionX = ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = MotionEventCompat.findPointerIndex(
                            ev, mActivePointerId);
                    final float x = MotionEventCompat.getX(ev, activePointerIndex);
                    final int deltaX = (int) (mLastMotionX - x);
                    mLastMotionX = x;
                    int scrollX = getScrollX() + deltaX;
                    if (scrollX < 0) {
                        scrollX = 0;
                    } else if (scrollX > ((mAdapter.getCount()-1)*getWidth())) {
                        scrollX = (mAdapter.getCount()-1)*getWidth();
                    }
                    scrollTo(scrollX, getScrollY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int)VelocityTrackerCompat.getYVelocity(
                            velocityTracker, mActivePointerId);
                    mPopulatePending = true;
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)
                            || Math.abs(mInitialMotionX-mLastMotionX) >= (getWidth()/3)) {
                        if (mLastMotionX > mInitialMotionX) {
                            setCurrentItemInternal(mCurItem-1, true, true);
                        } else {
                            setCurrentItemInternal(mCurItem+1, true, true);
                        }
                    } else {
                        setCurrentItemInternal(mCurItem, true, true);
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    setCurrentItemInternal(mCurItem, true, true);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float x = MotionEventCompat.getX(ev, index);
                mLastMotionX = x;
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionX = MotionEventCompat.getX(ev,
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId));
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled;
            if (USE_CACHE) {
                final int size = getChildCount();
                for (int i = 0; i < size; ++i) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        child.setDrawingCacheEnabled(enabled);
                    }
                }
            }
        }
    }
}
