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

/**
 * Translates {@link RecyclerView.OnScrollListener} events to {@link OnPageChangeListener} events.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ScrollEventAdapter extends RecyclerView.OnScrollListener {
    private static final int NO_TARGET = -1;

    private OnPageChangeListener mListener;
    private final @NonNull LinearLayoutManager mLayoutManager;

    // state related fields
    private @AdapterState int mAdapterState;
    private @ViewPager2.ScrollState int mScrollState;
    private ScrollEventValues mScrollValues;
    private int mInitialPosition;
    private int mTarget;
    private boolean mDispatchSelected;
    private boolean mScrollHappened;

    public ScrollEventAdapter(@NonNull LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
        mScrollValues = new ScrollEventValues();
        resetState();
    }

    private void resetState() {
        mAdapterState = AdapterState.IDLE;
        mScrollState = ViewPager2.ScrollState.IDLE;
        mScrollValues.reset();
        mInitialPosition = NO_TARGET;
        mTarget = NO_TARGET;
        mDispatchSelected = false;
        mScrollHappened = false;
    }

    /**
     * This method only deals with some cases of {@link AdapterState} transitions. The rest of
     * the state transition implementation is in the {@link #onScrolled} method.
     */
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (mAdapterState == AdapterState.IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            mAdapterState = AdapterState.IN_PROGRESS_MANUAL_DRAG;
            dispatchStateChanged(ViewPager2.ScrollState.DRAGGING);
            mInitialPosition = getPosition();
            return;
        }

        if (mAdapterState == AdapterState.IN_PROGRESS_MANUAL_DRAG
                && newState == RecyclerView.SCROLL_STATE_SETTLING) {
            if (!mScrollHappened) {
                // Special case of dragging before first (or beyond last) page
                dispatchScrolled(getPosition(), 0f, 0);
            } else {
                dispatchStateChanged(ViewPager2.ScrollState.SETTLING);
                mDispatchSelected = true;
            }
            return;
        }

        if (mAdapterState == AdapterState.IN_PROGRESS_MANUAL_DRAG
                && newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (!mScrollHappened) {
                // Special case of dragging before first (or beyond last) page
                dispatchStateChanged(ViewPager2.ScrollState.IDLE);
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
            mDispatchSelected = false;
            mTarget = (dx + dy > 0) ? values.mPosition + 1 : values.mPosition;
            if (mInitialPosition != mTarget) {
                dispatchSelected(mTarget);
            }
        }

        dispatchScrolled(values.mPosition, values.mOffset, values.mOffsetPx);

        if ((values.mPosition == mTarget || mTarget == NO_TARGET) && values.mOffsetPx == 0) {
            dispatchStateChanged(ViewPager2.ScrollState.IDLE);
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

        boolean isHorizontal = mLayoutManager.getOrientation() == ViewPager2.Orientation.HORIZONTAL;
        int start, sizePx;
        if (isHorizontal) {
            start = firstVisibleView.getLeft();
            sizePx = firstVisibleView.getWidth();
        } else {
            start = firstVisibleView.getTop();
            sizePx = firstVisibleView.getHeight();
        }

        values.mOffsetPx = -start;
        if (values.mOffsetPx < 0) {
            throw new IllegalStateException(String.format("Page can only be offset by a positive "
                    + "amount, not by %d", values.mOffsetPx));
        }
        values.mOffset = sizePx == 0 ? 0 : (float) values.mOffsetPx / sizePx;
        return values;
    }

    /**
     * Let the adapter know a programmatic scroll was initiated.
     */
    public void notifyProgrammaticScroll(int target, boolean smooth) {
        mAdapterState = smooth
                ? AdapterState.IN_PROGRESS_SMOOTH_SCROLL
                : AdapterState.IN_PROGRESS_IMMEDIATE_SCROLL;
        mTarget = target;
        dispatchStateChanged(ViewPager2.ScrollState.SETTLING);
        dispatchSelected(target);
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mListener = listener;
    }

    /**
     * @return true if there is no known scroll in progress
     */
    public boolean isIdle() {
        return mAdapterState == AdapterState.IDLE;
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

    private void dispatchStateChanged(@ViewPager2.ScrollState int state) {
        // Listener contract for immediate-scroll requires not having state change notifications,
        // but only when there was no smooth scroll in progress.
        // By putting a suppress statement in here (rather than next to dispatch calls) we are
        // simplifying the code of the class and enforcing the contract in one place.
        if (mAdapterState == AdapterState.IN_PROGRESS_IMMEDIATE_SCROLL
                && mScrollState == ViewPager2.ScrollState.IDLE) {
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

    @Retention(SOURCE)
    @IntDef({AdapterState.IDLE, AdapterState.IN_PROGRESS_MANUAL_DRAG,
            AdapterState.IN_PROGRESS_SMOOTH_SCROLL, AdapterState.IN_PROGRESS_IMMEDIATE_SCROLL})
    private @interface AdapterState {
        int IDLE = 0;
        int IN_PROGRESS_MANUAL_DRAG = 1;
        int IN_PROGRESS_SMOOTH_SCROLL = 2;
        int IN_PROGRESS_IMMEDIATE_SCROLL = 3;
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
