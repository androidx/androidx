/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class TestedFrameLayout extends FrameLayout implements NestedScrollingParent {

    static final int TEST_NESTED_SCROLL_MODE_IGNORE = 0;
    static final int TEST_NESTED_SCROLL_MODE_CONSUME = 1;

    private int mNestedScrollMode;
    private int mNestedFlingMode;
    private boolean mNestedStopNestedScrollCalled;

    public TestedFrameLayout(Context context) {
        super(context);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // Always start nested scroll
        return mNestedFlingMode == TEST_NESTED_SCROLL_MODE_CONSUME
                || mNestedScrollMode == TEST_NESTED_SCROLL_MODE_CONSUME;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.d("TestedFrameLayout", "onNestedPreFling: " + mNestedFlingMode);

        return mNestedFlingMode == TEST_NESTED_SCROLL_MODE_CONSUME;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (mNestedScrollMode == TEST_NESTED_SCROLL_MODE_CONSUME) {
            // We consume all scroll deltas
            consumed[0] = dx;
            consumed[1] = dy;
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed) {
        // ignore
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        // ignore
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // ignore
    }

    @Override
    public int getNestedScrollAxes() {
        // We can scroll in both direction
        return ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedStopNestedScrollCalled = true;
    }

    public boolean stopNestedScrollCalled() {
        return mNestedStopNestedScrollCalled;
    }

    public void setNestedScrollMode(int mode) {
        mNestedScrollMode = mode;
    }

    public void setNestedFlingMode(int mode) {
        mNestedFlingMode = mode;
    }
}
