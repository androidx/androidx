/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL;
import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE;
import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING;
import static androidx.viewpager2.widget.ViewPager2.ScrollState;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener;

import java.lang.annotation.Retention;
import java.util.Locale;

/**
 * Translates {@link RecyclerView.OnScrollListener} events to {@link OnPageChangeListener} events
 * for {@link ViewPager2}. As part of this process, it keeps track of the current scroll position
 * relative to the pages and exposes this position via ({@link #getRelativeScrollPosition()}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ScrollEventAdapter extends RecyclerView.OnScrollListener {
    @Retention(SOURCE)
    @IntDef({STATE_IDLE, STATE_IN_PROGRESS_MANUAL_DRAG, STATE_IN_PROGRESS_SMOOTH_SCROLL,
            STATE_IN_PROGRESS_IMMEDIATE_SCROLL})
    private @interface AdapterState {
    }

    private static final int STATE_IDLE = 0;
    private static final int STATE_IN_PROGRESS_MANUAL_DRAG = 1;
    private static final int STATE_IN_PROGRESS_SMOOTH_SCROLL = 2;
    private static final int STATE_IN_PROGRESS_IMMEDIATE_SCROLL = 3;

    private static final int NO_POSITION = -1;

    private OnPageChangeListener mListener;
    private final @NonNull LinearLayoutManager mLayoutManager;

    // state related fields
    private @AdapterState int mAdapterState;
    private @ViewPager2.ScrollState int mScrollState;
    private ScrollEventValues mScrollValues;
    private int mDragStartPosition;
    private int mTarget;
    private boolean mDispatchSelected;
    private boolean mScrollHappened;

    public ScrollEventAdapter(@NonNull LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
        mScrollValues = new ScrollEventValues();
        resetState();
    }

    private void resetState() {
        mAdapterState = STATE_IDLE;
        mScrollState = SCROLL_STATE_IDLE;
        mScrollValues.reset();
        mDragStartPosition = NO_POSITION;
        mTarget = NO_POSITION;
        mDispatchSelected = false;
        mScrollHappened = false;
    }

    /**
     * This method only deals with some cases of {@link AdapterState} transitions. The rest of
     * the state transition implementation is in the {@link #onScrolled} method.
     */
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        // User started a drag (not dragging -> dragging)
        if (mAdapterState != STATE_IN_PROGRESS_MANUAL_DRAG
                && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            // Remember we're performing a drag
            mAdapterState = STATE_IN_PROGRESS_MANUAL_DRAG;
            if (mTarget != NO_POSITION) {
                // Target was set means programmatic scroll was in progress
                // Update "drag start page" to reflect the page that ViewPager2 thinks it is at
                mDragStartPosition = mTarget;
                // Reset target because drags have no target until released
                mTarget = NO_POSITION;
            } else {
                // ViewPager2 was at rest, set "drag start page" to current page
                mDragStartPosition = getPosition();
            }
            dispatchStateChanged(SCROLL_STATE_DRAGGING);
            return;
        }

        // Drag is released, RecyclerView is snapping to page (dragging -> settling)
        // Note that mAdapterState is not updated, to remember we were dragging when settling
        if (mAdapterState == STATE_IN_PROGRESS_MANUAL_DRAG
                && newState == RecyclerView.SCROLL_STATE_SETTLING) {
            if (!mScrollHappened) {
                // Pages didn't move during drag, so must be at the start or end of the list
                // ViewPager's contract requires at least one scroll event though
                dispatchScrolled(getPosition(), 0f, 0);
            } else {
                dispatchStateChanged(SCROLL_STATE_SETTLING);
                // Determine target page and dispatch onPageSelected on next scroll event
                mDispatchSelected = true;
                // Reset value to recognise if onPageSelected has been fired when going to idle
                mScrollHappened = false;
            }
            return;
        }

        // Drag has settled (dragging && settling -> idle)
        if (mAdapterState == STATE_IN_PROGRESS_MANUAL_DRAG
                && newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (!mScrollHappened) {
                // Special case if we were snapped at a page when going from dragging to settling
                // Happens if there was no velocity or if it was the first or last page
                if (mDispatchSelected) {
                    // Fire onPageSelected when snapped page is different from initial position
                    // E.g.: smooth scroll from 0 to 1, interrupt with drag at 0.5, release at 0
                    updateScrollEventValues();
                    if (mDragStartPosition != mScrollValues.mPosition) {
                        dispatchSelected(mScrollValues.mPosition);
                    }
                }
                // Normally idle is fired in onScrolled, but scroll did not happen
                dispatchStateChanged(SCROLL_STATE_IDLE);
                resetState();
            }
            return;
        }
    }

    /**
     * This method only deals with some cases of {@link AdapterState} transitions. The rest of
     * the state transition implementation is in the {@link #onScrollStateChanged} method.
     */
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        mScrollHappened = true;
        ScrollEventValues values = updateScrollEventValues();

        if (mDispatchSelected) {
            // Drag started settling, need to calculate target page and dispatch onPageSelected now
            mDispatchSelected = false;
            mTarget = (dy > 0 || (dy == 0 && dx < 0 == isLayoutRTL()))
                    ? values.mPosition + 1 : values.mPosition;
            if (mDragStartPosition != mTarget) {
                dispatchSelected(mTarget);
            }
        }

        dispatchScrolled(values.mPosition, values.mOffset, values.mOffsetPx);

        if ((values.mPosition == mTarget || mTarget == NO_POSITION) && values.mOffsetPx == 0
                && mScrollState != SCROLL_STATE_DRAGGING) {
            // When the target page is reached and the user is not dragging anymore, we're settled,
            // so go to idle.
            // Special case and a bit of a hack when there is no target: RecyclerView is being
            // initialized and fires a single scroll event. This flags mScrollHappened, so we need
            // to reset our state. However, we don't want to dispatch idle. But that won't happen;
            // because we were already idle.
            dispatchStateChanged(SCROLL_STATE_IDLE);
            resetState();
        }
    }

    /**
     * Calculates the current position and the offset (as a percentage and in pixels) of that
     * position from the center.
     */
    private ScrollEventValues updateScrollEventValues() {
        ScrollEventValues values = mScrollValues;

        values.mPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (values.mPosition == RecyclerView.NO_POSITION) {
            return values.reset();
        }
        View firstVisibleView = mLayoutManager.findViewByPosition(values.mPosition);
        if (firstVisibleView == null) {
            return values.reset();
        }

        boolean isHorizontal = mLayoutManager.getOrientation() == ORIENTATION_HORIZONTAL;
        int start, sizePx;
        if (isHorizontal) {
            sizePx = firstVisibleView.getWidth();
            if (!isLayoutRTL()) {
                start = firstVisibleView.getLeft();
            } else {
                start = sizePx - firstVisibleView.getRight();
            }
        } else {
            sizePx = firstVisibleView.getHeight();
            start = firstVisibleView.getTop();
        }

        values.mOffsetPx = -start;
        if (values.mOffsetPx < 0) {
            throw new IllegalStateException(String.format(Locale.US, "Page can only be offset by a "
                    + "positive amount, not by %d", values.mOffsetPx));
        }
        values.mOffset = sizePx == 0 ? 0 : (float) values.mOffsetPx / sizePx;
        return values;
    }

    /**
     * Let the adapter know a programmatic scroll was initiated.
     */
    public void notifyProgrammaticScroll(int target, boolean smooth) {
        mAdapterState = smooth
                ? STATE_IN_PROGRESS_SMOOTH_SCROLL
                : STATE_IN_PROGRESS_IMMEDIATE_SCROLL;
        boolean hasNewTarget = mTarget != target;
        mTarget = target;
        dispatchStateChanged(SCROLL_STATE_SETTLING);
        if (hasNewTarget) {
            dispatchSelected(target);
        }
    }

    private boolean isLayoutRTL() {
        return mLayoutManager.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mListener = listener;
    }

    /**
     * @return true if there is no known scroll in progress
     */
    public boolean isIdle() {
        return mAdapterState == STATE_IDLE;
    }

    /**
     * Calculates the scroll position of the currently visible item of the ViewPager relative to its
     * width. Calculated by adding the fraction by which the first visible item is off screen to its
     * adapter position. E.g., if the ViewPager is currently scrolling from the second to the third
     * page, the returned value will be between 1 and 2. Thus, non-integral values mean that the
     * the ViewPager is settling towards its {@link ViewPager2#getCurrentItem() current item}, or
     * the user may be dragging it.
     *
     * @return The current scroll position of the ViewPager, relative to its width
     */
    public float getRelativeScrollPosition() {
        updateScrollEventValues();
        return mScrollValues.mPosition + mScrollValues.mOffset;
    }

    private void dispatchStateChanged(@ScrollState int state) {
        // Listener contract for immediate-scroll requires not having state change notifications,
        // but only when there was no smooth scroll in progress.
        // By putting a suppress statement in here (rather than next to dispatch calls) we are
        // simplifying the code of the class and enforcing the contract in one place.
        if (mAdapterState == STATE_IN_PROGRESS_IMMEDIATE_SCROLL
                && mScrollState == SCROLL_STATE_IDLE) {
            return;
        }
        if (mScrollState == state) {
            return;
        }

        mScrollState = state;
        if (mListener != null) {
            mListener.onPageScrollStateChanged(state);
        }
    }

    private void dispatchSelected(int target) {
        if (mListener != null) {
            mListener.onPageSelected(target);
        }
    }

    private void dispatchScrolled(int position, float offset, int offsetPx) {
        if (mListener != null) {
            mListener.onPageScrolled(position, offset, offsetPx);
        }
    }

    private int getPosition() {
        return mLayoutManager.findFirstVisibleItemPosition();
    }

    static class ScrollEventValues {
        int mPosition;
        float mOffset;
        int mOffsetPx;

        ScrollEventValues reset() {
            mPosition = RecyclerView.NO_POSITION;
            mOffset = 0f;
            mOffsetPx = 0;
            return this;
        }
    }
}
