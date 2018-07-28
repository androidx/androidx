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

/**
 * This interface should be implemented by {@link android.view.ViewGroup ViewGroup} subclasses
 * that wish to support scrolling operations delegated by a nested child view.
 *
 * <p>Classes implementing this interface should create a final instance of a
 * {@link NestedScrollingParentHelper} as a field and delegate any View or ViewGroup methods
 * to the <code>NestedScrollingParentHelper</code> methods of the same signature.</p>
 *
 * <p>Views invoking nested scrolling functionality should always do so from the relevant
 * {@link ViewCompat}, {@link ViewGroupCompat} or {@link ViewParentCompat} compatibility
 * shim static methods. This ensures interoperability with nested scrolling views on all versions
 * of Android.</p>
 */
public interface NestedScrollingParent3 extends NestedScrollingParent2 {

    /**
     * React to a nested scroll in progress.
     *
     * <p>This method will be called when the ViewParent's current nested scrolling child view
     * dispatches a nested scroll event. To receive calls to this method the ViewParent must have
     * previously returned <code>true</code> for a call to
     * {@link #onStartNestedScroll(View, View, int, int)}.
     *
     * <p>Both the consumed and unconsumed portions of the scroll distance are reported to the
     * ViewParent. An implementation may choose to use the consumed portion to match or chase scroll
     * position of multiple child elements, for example. The unconsumed portion may be used to
     * allow continuous dragging of multiple scrolling or draggable elements, such as scrolling
     * a list within a vertical drawer where the drawer begins dragging once the edge of inner
     * scrolling content is reached.
     *
     * <p>This method is called when a nested scrolling child invokes
     * {@link NestedScrollingChild3#dispatchNestedScroll(int, int, int, int, int[], int, int[])}} or
     * one of methods it overloads.
     *
     * <p>An implementation must report how many pixels of the the x and y scroll distances were
     * consumed by this nested scrolling parent by adding the consumed distances to the
     * <code>consumed</code> parameter. If this View also implements {@link NestedScrollingChild3},
     * <code>consumed</code> should also be passed up to it's nested scrolling parent so that the
     * parent may also add any scroll distance it consumes. Index 0 corresponds to dx and index 1
     * corresponds to dy.
     *
     * @param target The descendant view controlling the nested scroll
     * @param dxConsumed Horizontal scroll distance in pixels already consumed by target
     * @param dyConsumed Vertical scroll distance in pixels already consumed by target
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by target
     * @param dyUnconsumed Vertical scroll distance in pixels not consumed by target
     * @param type the type of input which cause this scroll event
     * @param consumed Output. Upon this method returning, will contain the scroll
     *                 distances consumed by this nested scrolling parent and the scroll distances
     *                 consumed by any other parent up the view hierarchy
     *
     * @see NestedScrollingChild3#dispatchNestedScroll(int, int, int, int, int[], int, int[])
     */
    void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, @ViewCompat.NestedScrollType int type, @NonNull int[] consumed);

}
