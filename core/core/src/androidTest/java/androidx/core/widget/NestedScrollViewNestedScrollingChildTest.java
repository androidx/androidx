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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.testutils.Direction;
import androidx.testutils.FlingData;
import androidx.testutils.MotionEventData;
import androidx.testutils.SimpleGestureGeneratorKt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

/**
 * Small integration tests that verifies that {@link NestedScrollView} interacts with the latest
 * version of the nested scroll parents correctly.
*/
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewNestedScrollingChildTest {

    private NestedScrollingSpyView mParentSpy;
    private NestedScrollView mNestedScrollView;

    @Before
    public void setup() {

        // Create views

        View child = new View(ApplicationProvider.getApplicationContext());
        child.setMinimumWidth(1000);
        child.setMinimumHeight(1100);

        mNestedScrollView = new NestedScrollView(ApplicationProvider.getApplicationContext());
        mNestedScrollView.setMinimumWidth(1000);
        mNestedScrollView.setMinimumHeight(1000);

        mParentSpy = Mockito.spy(
                new NestedScrollingSpyView(ApplicationProvider.getApplicationContext()));
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
    }

    @Test
    public void uiFingerDown_parentHasNestedScrollingChildWithTypeTouch() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mNestedScrollView.dispatchTouchEvent(down);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_VERTICAL));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerDown_parentRejects_parentDoesNotHaveNestedScrollingChild() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(false);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mNestedScrollView.dispatchTouchEvent(down);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerUp_afterFingerDown_parentDoesNotHaveNestedScrollingChild() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent up = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 500, 500, 0);
        mNestedScrollView.dispatchTouchEvent(down);

        mNestedScrollView.dispatchTouchEvent(up);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerScroll_parentOnNestedPreScrollCalledCorrectly() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300, 0);

        mNestedScrollView.dispatchTouchEvent(down);
        mNestedScrollView.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedPreScroll(mNestedScrollView, 0, 200, new int[]{0, 0},
                ViewCompat.TYPE_TOUCH);
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimit_parentOnNestedScrollCalledCorrectly() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300 - touchSlop, 0);

        mNestedScrollView.dispatchTouchEvent(down);
        mNestedScrollView.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mNestedScrollView, 0, 100, 0, 100, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimit_parentOnNestedScrollCalledCorrectly() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 450 - touchSlop, 0);

        mNestedScrollView.dispatchTouchEvent(down);
        mNestedScrollView.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mNestedScrollView, 0, 50, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_preSelfPostChainWorks() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((int[]) args[3])[1] = 50;
                return null; // void method, so return null
            }
        }).when(mParentSpy)
                .onNestedPreScroll(any(View.class), anyInt(), anyInt(), any(int[].class), anyInt());
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300, 0);

        mNestedScrollView.dispatchTouchEvent(down);
        mNestedScrollView.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedPreScroll(mNestedScrollView, 0, 200, new int[]{0, 0},
                ViewCompat.TYPE_TOUCH);
        verify(mParentSpy).onNestedScroll(mNestedScrollView, 0, 100, 0, 50 - touchSlop,
                ViewCompat.TYPE_TOUCH, new int[]{0, 0});
    }

    @Test
    public void uiFling_parentHasNestedScrollingChildWithTypeFling() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        long startTime = SystemClock.uptimeMillis();
        SimpleGestureGeneratorKt
                .simulateFling(mNestedScrollView, startTime, 500, 500, Direction.UP);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    @Test
    public void uiFling_callsNestedFlingsCorrectly() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        long startTime = SystemClock.uptimeMillis();
        SimpleGestureGeneratorKt
                .simulateFling(mNestedScrollView, startTime, 500, 500, Direction.UP);

        InOrder inOrder = Mockito.inOrder(mParentSpy);
        inOrder.verify(mParentSpy).onNestedPreFling(
                eq(mNestedScrollView),
                eq(0f),
                anyFloat());
        inOrder.verify(mParentSpy).onNestedFling(
                eq(mNestedScrollView),
                eq(0f),
                anyFloat(),
                eq(true));
    }

    @Test
    public void uiDown_duringFling_stopsNestedScrolling() {

        // Arrange

        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);

        final Context context = ApplicationProvider.getApplicationContext();
        FlingData flingData = SimpleGestureGeneratorKt.generateFlingData(context);

        final long firstDownTime = SystemClock.uptimeMillis();
        // Should be after fling events occurred.
        final long secondDownTime = firstDownTime + flingData.getTime() + 100;

        List<MotionEventData> motionEventData = SimpleGestureGeneratorKt
                .generateFlingMotionEventData(flingData, 500, 500, Direction.UP);
        SimpleGestureGeneratorKt
                .dispatchTouchEvents(mNestedScrollView, firstDownTime, motionEventData);

        // Sanity check that onStopNestedScroll has not yet been called of type TYPE_NON_TOUCH.
        verify(mParentSpy, never())
                .onStopNestedScroll(mNestedScrollView, ViewCompat.TYPE_NON_TOUCH);

        // Act

        MotionEvent down = MotionEvent.obtain(
                secondDownTime,
                secondDownTime,
                MotionEvent.ACTION_DOWN,
                500,
                500,
                0);
        mNestedScrollView.dispatchTouchEvent(down);

        // Assert

        verify(mParentSpy).onStopNestedScroll(mNestedScrollView, ViewCompat.TYPE_NON_TOUCH);
    }

    @Test
    public void uiFlings_parentReturnsFalseForOnNestedPreFling_onNestedFlingCalled() {
        uiFlings_returnValueOfOnNestedPreFlingDeterminesCallToOnNestedFling(false, true);
    }

    @Test
    public void uiFlings_parentReturnsTrueForOnNestedPreFling_onNestedFlingNotCalled() {
        uiFlings_returnValueOfOnNestedPreFlingDeterminesCallToOnNestedFling(true, false);
    }

    private void uiFlings_returnValueOfOnNestedPreFlingDeterminesCallToOnNestedFling(
            boolean returnValue, boolean onNestedFlingCalled) {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        when(mParentSpy.onNestedPreFling(eq(mNestedScrollView), anyFloat(), anyFloat()))
                .thenReturn(returnValue);

        long startTime = SystemClock.uptimeMillis();
        SimpleGestureGeneratorKt
                .simulateFling(mNestedScrollView, startTime, 500, 500, Direction.UP);

        if (onNestedFlingCalled) {
            verify(mParentSpy).onNestedFling(eq(mNestedScrollView), anyFloat(), anyFloat(),
                    eq(true));
        } else {
            verify(mParentSpy, never()).onNestedFling(any(View.class), anyFloat(), anyFloat(),
                    anyBoolean());
        }
    }

    @Test
    public void smoothScrollBy_doesNotStartNestedScrolling() {
        mNestedScrollView.smoothScrollBy(0, 100);
        verify(mParentSpy, never()).onStartNestedScroll(
                any(View.class), any(View.class), anyInt(), anyInt());
    }

    @Test
    public void smoothScrollBy_stopsInProgressNestedScroll() {
        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(true);
        mNestedScrollView.fling(100);
        mNestedScrollView.smoothScrollBy(0, 100);
        verify(mParentSpy).onStopNestedScroll(mNestedScrollView, ViewCompat.TYPE_NON_TOUCH);
    }

    @Test
    public void fling_startsNestedScrolling() {
        mNestedScrollView.fling(100);
        verify(mParentSpy).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                View.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public int axesForTypeTouch;
        public int axesForTypeNonTouch;

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
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                axesForTypeNonTouch = axes;
            } else {
                axesForTypeTouch = axes;
            }
        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                axesForTypeNonTouch = ViewCompat.SCROLL_AXIS_NONE;
            } else {
                axesForTypeTouch = ViewCompat.SCROLL_AXIS_NONE;
            }
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

        @Override
        public void setNestedScrollingEnabled(boolean enabled) {

        }

        @Override
        public boolean isNestedScrollingEnabled() {
            return false;
        }

        @Override
        public boolean startNestedScroll(int axes) {
            return false;
        }

        @Override
        public void stopNestedScroll() {

        }

        @Override
        public boolean hasNestedScrollingParent() {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
                int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(
                @NonNull View target, int dx, int dy, @NonNull int[] consumed) {

        }

        @Override
        public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(@NonNull  View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }
    }
}
