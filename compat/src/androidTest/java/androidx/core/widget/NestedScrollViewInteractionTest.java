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

package androidx.core.widget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewInteractionTest {

    private NestedScrollingSpyView mParentSpy;
    private NestedScrollView mNestedScrollView;

    @Before
    public void setup() {

        // Create views

        View child = new View(InstrumentationRegistry.getContext());
        child.setMinimumWidth(1000);
        child.setMinimumHeight(1100);

        mNestedScrollView = new NestedScrollView(InstrumentationRegistry.getContext());
        mNestedScrollView.setMinimumWidth(1000);
        mNestedScrollView.setMinimumHeight(1000);

        mParentSpy = Mockito.spy(new NestedScrollingSpyView(InstrumentationRegistry.getContext()));
        mParentSpy.setMinimumWidth(1000);
        mParentSpy.setMinimumHeight(1000);

        // Create view hierarchy

        mNestedScrollView.addView(child);
        mParentSpy.addView(mNestedScrollView);

        //  Measure and layout

        int measureSpecWidth =
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        int measureSpecHeight =
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        mParentSpy.measure(measureSpecWidth, measureSpecHeight);
        mParentSpy.layout(0, 0, 1000, 1000);

        // Enable nested scrolling

        doReturn(true)
                .when(mParentSpy)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimit_remainderPassedToParent() {
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mNestedScrollView, 0, 100, 0, 100, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimit_remainderPassedToParent() {
        int touchSlop =
                ViewConfiguration.get(InstrumentationRegistry.getContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 450 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mNestedScrollView, 0, 50, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {

        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return false;
        }

        @Override
        public void stopNestedScroll(int type) {

        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
        }
    }
}
