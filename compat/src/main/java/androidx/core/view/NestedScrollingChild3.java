/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.core.view;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This interface should be implemented by {@link View View} subclasses that wish
 * to support dispatching nested scrolling operations to a cooperating parent
 * {@link android.view.ViewGroup ViewGroup}.
 *
 * <p>Classes implementing this interface should create a final instance of a
 * {@link NestedScrollingChildHelper} as a field and delegate any View methods to the
 * <code>NestedScrollingChildHelper</code> methods of the same signature.</p>
 *
 * <p>Views invoking nested scrolling functionality should always do so from the relevant
 * {@link ViewCompat}, {@link ViewGroupCompat} or {@link ViewParentCompat} compatibility
 * shim static methods. This ensures interoperability with nested scrolling views on all versions
 * of Android.</p>
 */
public interface NestedScrollingChild3 extends NestedScrollingChild2 {

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.
     *
     * <p>The original nested scrolling child (where the input events were received to start the
     * scroll) must provide a non-null <code>consumed</code> parameter with values {0, 0}.
     *
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @param consumed Output. Upon this method returning, will contain the original values plus any
     *                 scroll distances consumed by all of this view's nested scrolling parents up
     *                 the view hierarchy. Index 0 for the x dimension, and index 1 for the y
     *                 dimension
     *
     * @see NestedScrollingParent3#onNestedScroll(View, int, int, int, int, int, int[])
     */
    void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
            @Nullable int[] offsetInWindow, @ViewCompat.NestedScrollType int type,
            @NonNull int[] consumed);
}
