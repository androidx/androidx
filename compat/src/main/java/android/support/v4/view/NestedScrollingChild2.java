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


package android.support.v4.view;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat.NestedScrollType;
import android.support.v4.view.ViewCompat.ScrollAxis;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

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
public interface NestedScrollingChild2 extends NestedScrollingChild {

    /**
     * Begin a nestable scroll operation along the given axes, for the given input type.
     *
     * <p>A view starting a nested scroll promises to abide by the following contract:</p>
     *
     * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
     * of a touch scroll type this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
     * In the case of touch scrolling the nested scroll will be terminated automatically in
     * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
     * In the event of programmatic scrolling the caller must explicitly call
     * {@link #stopNestedScroll(int)} to indicate the end of the nested scroll.</p>
     *
     * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
     * If it returns false the caller may ignore the rest of this contract until the next scroll.
     * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
     *
     * <p>At each incremental step of the scroll the caller should invoke
     * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll}
     * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
     * parent at least partially consumed the scroll and the caller should adjust the amount it
     * scrolls by.</p>
     *
     * <p>After applying the remainder of the scroll delta the caller should invoke
     * {@link #dispatchNestedScroll(int, int, int, int, int[], int) dispatchNestedScroll}, passing
     * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
     * these values differently. See
     * {@link NestedScrollingParent2#onNestedScroll(View, int, int, int, int, int)}.
     * </p>
     *
     * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
     *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
     * @param type the type of input which cause this scroll event
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     *         the current gesture.
     *
     * @see #stopNestedScroll(int)
     * @see #dispatchNestedPreScroll(int, int, int[], int[], int)
     * @see #dispatchNestedScroll(int, int, int, int, int[], int)
     */
    boolean startNestedScroll(@ScrollAxis int axes, @NestedScrollType int type);

    /**
     * Stop a nested scroll in progress for the given input type.
     *
     * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
     *
     * @param type the type of input which cause this scroll event
     * @see #startNestedScroll(int, int)
     */
    void stopNestedScroll(@NestedScrollType int type);

    /**
     * Returns true if this view has a nested scrolling parent for the given input type.
     *
     * <p>The presence of a nested scrolling parent indicates that this view has initiated
     * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
     *
     * @param type the type of input which cause this scroll event
     *
     * @return whether this view has a nested scrolling parent
     */
    boolean hasNestedScrollingParent(@NestedScrollType int type);

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.</p>
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.</p>
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
     * @return true if the event was dispatched, false if it could not be dispatched.
     * @see #dispatchNestedPreScroll(int, int, int[], int[], int)
     */
    boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow,
            @NestedScrollType int type);

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
     * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
     * scrolling operation to consume some or all of the scroll operation before the child view
     * consumes it.</p>
     *
     * @param dx Horizontal scroll distance in pixels
     * @param dy Vertical scroll distance in pixels
     * @param consumed Output. If not null, consumed[0] will contain the consumed component of dx
     *                 and consumed[1] the consumed dy.
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @return true if the parent consumed some or all of the scroll delta
     * @see #dispatchNestedScroll(int, int, int, int, int[], int)
     */
    boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
            @Nullable int[] offsetInWindow, @NestedScrollType int type);
}
