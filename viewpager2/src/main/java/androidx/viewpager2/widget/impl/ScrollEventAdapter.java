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

package androidx.viewpager2.widget.impl;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Translates {@link RecyclerView.OnScrollListener} events to
 * {@link androidx.viewpager.widget.ViewPager.OnPageChangeListener} events.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ScrollEventAdapter extends RecyclerView.OnScrollListener {
    private static final int NO_TARGET = -1;

    private final @NonNull List<OnPageChangeListener> mListeners = new ArrayList<>(3);
    private final @NonNull LinearLayoutManager mLayoutManager;

    // reused for efficiency
    private final @NonNull Rect mRect = new Rect();

    // state related fields
    private @AdapterState int mAdapterState;
    private int mInitialPosition;
    private int mTarget;
    private boolean mDispatchSelected;
    private boolean mScrollHappened;

    public ScrollEventAdapter(@NonNull LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
        resetState();
    }

    private void resetState() {
        mAdapterState = AdapterState.IDLE;
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
        if (mListeners.isEmpty()) {
            return;
        }

        mScrollHappened = true;

        int position = getPosition();
        View firstVisibleView = mLayoutManager.findViewByPosition(position);
        if (firstVisibleView == null) {
            return;
        }

        firstVisibleView.getGlobalVisibleRect(mRect); // TODO: check if to use globalOffset variant

        boolean isHorizontal = mLayoutManager.getOrientation() == ViewPager2.Orientation.HORIZONTAL;
        int visiblePx = isHorizontal ? (mRect.right - mRect.left) : (mRect.bottom - mRect.top);
        int sizePx = isHorizontal ? firstVisibleView.getWidth() : firstVisibleView.getHeight();

        int offsetPx = sizePx - visiblePx;
        float offset = (float) offsetPx / sizePx;

        if (mDispatchSelected) {
            mDispatchSelected = false;
            mTarget = (dx + dy > 0) ? position + 1 : position;
            if (mInitialPosition != mTarget) {
                dispatchSelected(mTarget);
            }
        }

        dispatchScrolled(position, offset, offsetPx);

        if (position == mTarget && offsetPx == 0) {
            dispatchStateChanged(ViewPager2.ScrollState.IDLE);
            resetState();
        }
    }

    /**
     * Let the adapter know a programmatic scroll was initiated.
     */
    public void notifyProgrammaticScroll(int target, boolean smooth) {
        if (isIdle()) {
            mAdapterState = smooth
                    ? AdapterState.IN_PROGRESS_SMOOTH_SCROLL
                    : AdapterState.IN_PROGRESS_IMMEDIATE_SCROLL;
            mTarget = target;
            dispatchStateChanged(ViewPager2.ScrollState.SETTLING);
            dispatchSelected(target);
        }
    }

    /**
     * Registers a listener
     */
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener
     */
    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Removes all listeners
     */
    public void clearOnPageChangeListeners() {
        mListeners.clear();
    }

    /**
     * @return true if there is no known scroll in progress
     */
    public boolean isIdle() {
        return mAdapterState == AdapterState.IDLE;
    }

    private void dispatchStateChanged(@ViewPager2.ScrollState int state) {
        // Listener contract for immediate-scroll requires not having state change notifications.
        // By putting a suppress statement in here (rather than next to dispatch calls) we are
        // simplifying the code of the class and enforcing the contract in one place.
        if (mAdapterState == AdapterState.IN_PROGRESS_IMMEDIATE_SCROLL) {
            return;
        }

        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageScrollStateChanged(state);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    private void dispatchSelected(int target) {
        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageSelected(target);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    private void dispatchScrolled(int position, float offset, int offsetPx) {
        try {
            for (OnPageChangeListener listener : mListeners) {
                listener.onPageScrolled(position, offset, offsetPx);
            }
        } catch (ConcurrentModificationException ex) {
            throwListenerListModifiedWhileInUse(ex);
        }
    }

    private void throwListenerListModifiedWhileInUse(ConcurrentModificationException parent) {
        throw new IllegalStateException(
                "Adding and removing listeners during dispatch to listeners is not supported",
                parent);
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
}
