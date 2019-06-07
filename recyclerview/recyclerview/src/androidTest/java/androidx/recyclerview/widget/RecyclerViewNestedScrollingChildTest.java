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

package androidx.recyclerview.widget;

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
import android.view.ViewGroup;
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

/**
 * Small integration tests that verifies that {@link RecyclerView} interacts with the latest
 * version of the nested scroll parents correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewNestedScrollingChildTest {

    private NestedScrollingSpyView mParentSpy;
    private RecyclerView mRecyclerView;

    private void setup(boolean vertical, int scrollDistance, boolean parentAccepts) {

        Context context = ApplicationProvider.getApplicationContext();

        // Create views

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setMinimumWidth(1000);
        mRecyclerView.setMinimumHeight(1000);

        mParentSpy = Mockito.spy(new NestedScrollingSpyView(context));
        mParentSpy.setMinimumWidth(1000);
        mParentSpy.setMinimumHeight(1000);

        // Setup RecyclerView
        int orientation = vertical ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, orientation, false));
        mRecyclerView.setAdapter(new TestAdapter(context, 1000 + scrollDistance, 1, vertical));

        // Create view hierarchy
        mParentSpy.addView(mRecyclerView);

        //  Measure and layout
        int measureSpecWidth = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        int measureSpecHeight = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        mParentSpy.measure(measureSpecWidth, measureSpecHeight);
        mParentSpy.layout(0, 0, 1000, 1000);

        when(mParentSpy.onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt()))
                .thenReturn(parentAccepts);
    }

    @Test
    public void uiFingerDown_vertical_parentHasNestedScrollingChildWithTypeTouch() {
        setup(true, 100, true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_VERTICAL));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerDown_horizontal_parentHasNestedScrollingChildWithTypeTouch() {
        setup(false, 100, true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_HORIZONTAL));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerDown_parentRejects_parentDoesNotHaveNestedScrollingChild() {
        setup(true, 100, false);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerUp_afterFingerDown_parentDoesNotHaveNestedScrollingChild() {
        setup(true, 100, true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent up = MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, 500, 500, 0);
        mRecyclerView.dispatchTouchEvent(down);

        mRecyclerView.dispatchTouchEvent(up);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_NONE));
    }

    @Test
    public void uiFingerScroll_vertical_parentOnNestedPreScrollCalledCorrectly() {
        setup(true, 100, true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300, 0);

        mRecyclerView.dispatchTouchEvent(down);
        mRecyclerView.dispatchTouchEvent(move);

        // Can't verify 'consumed' parameter values due to mutation, so instead capturing actual
        // values manually in the the NestedScrollingSpyView object.
        verify(mParentSpy).onNestedPreScroll(eq(mRecyclerView), eq(0), eq(200), any(int[].class),
                eq(ViewCompat.TYPE_TOUCH));
        assertThat(mParentSpy.onNestedPreScrollConsumedX, is(0));
        assertThat(mParentSpy.onNestedPreScrollConsumedY, is(0));
    }

    @Test
    public void uiFingerScroll_horizontal_parentOnNestedPreScrollCalledCorrectly() {
        setup(false, 100, true);
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 300, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);
        mRecyclerView.dispatchTouchEvent(move);

        // Can't verify 'consumed' parameter values due to mutation, so instead capturing actual
        // values manually in the the NestedScrollingSpyView object.
        verify(mParentSpy).onNestedPreScroll(eq(mRecyclerView), eq(200), eq(0), any(int[].class),
                eq(ViewCompat.TYPE_TOUCH));
        assertThat(mParentSpy.onNestedPreScrollConsumedX, is(0));
        assertThat(mParentSpy.onNestedPreScrollConsumedY, is(0));
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimitVertical_parentOnNestedScrollCalledCorrectly() {
        setup(true, 100, true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 0, 100, 0, 100, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsBeyondLimitHorizontal_parentOnNestedScrollCalledCorrectly() {
        setup(false, 100, true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 300 - touchSlop, 500, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 100, 0, 100, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimitVertical_parentOnNestedScrollCalledCorrectly() {
        setup(true, 100, true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 450 - touchSlop, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 0, 50, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_scrollsWithinLimitHorizontal_parentOnNestedScrollCalledCorrectly() {
        setup(false, 100, true);
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 450 - touchSlop, 500, 0);

        mParentSpy.dispatchTouchEvent(down);
        mParentSpy.dispatchTouchEvent(move);

        verify(mParentSpy).onNestedScroll(mRecyclerView, 50, 0, 0, 0, ViewCompat.TYPE_TOUCH,
                new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_vertical_preSelfPostChainWorks() {
        setup(true, 100, true);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((int[]) args[3])[1] = 50;
                return null;
            }
        }).when(mParentSpy)
                .onNestedPreScroll(any(View.class), anyInt(), anyInt(), any(int[].class), anyInt());
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500, 300, 0);

        mRecyclerView.dispatchTouchEvent(down);
        mRecyclerView.dispatchTouchEvent(move);

        // Can't verify 'consumed' parameter values due to mutation, so instead capturing actual
        // values manually in the the NestedScrollingSpyView object.
        verify(mParentSpy).onNestedPreScroll(eq(mRecyclerView), eq(0), eq(200), any(int[].class),
                eq(ViewCompat.TYPE_TOUCH));
        assertThat(mParentSpy.onNestedPreScrollConsumedX, is(0));
        assertThat(mParentSpy.onNestedPreScrollConsumedY, is(0));

        verify(mParentSpy).onNestedScroll(mRecyclerView, 0, 100, 0, 50 - touchSlop,
                ViewCompat.TYPE_TOUCH, new int[]{0, 0});
    }

    @Test
    public void uiFingerScroll_horizontal_preSelfPostChainWorks() {
        setup(false, 100, true);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ((int[]) args[3])[0] = 50;
                return null;
            }
        }).when(mParentSpy)
                .onNestedPreScroll(any(View.class), anyInt(), anyInt(), any(int[].class), anyInt());
        int touchSlop =
                ViewConfiguration.get(
                        ApplicationProvider.getApplicationContext()).getScaledTouchSlop();
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500, 500, 0);
        MotionEvent move =
                MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 300, 500, 0);

        mRecyclerView.dispatchTouchEvent(down);
        mRecyclerView.dispatchTouchEvent(move);

        // Can't verify 'consumed' parameter values due to mutation, so instead capturing actual
        // values manually in the the NestedScrollingSpyView object.
        verify(mParentSpy).onNestedPreScroll(eq(mRecyclerView), eq(200), eq(0), any(int[].class),
                eq(ViewCompat.TYPE_TOUCH));
        assertThat(mParentSpy.onNestedPreScrollConsumedX, is(0));
        assertThat(mParentSpy.onNestedPreScrollConsumedY, is(0));

        verify(mParentSpy).onNestedScroll(mRecyclerView, 100, 0, 50 - touchSlop, 0,
                ViewCompat.TYPE_TOUCH, new int[]{0, 0});
    }

    @Test
    public void uiFling_vertical_parentHasNestedScrollingChildWithTypeFling() {
        setup(true, 100, true);
        long startTime = SystemClock.uptimeMillis();

        SimpleGestureGeneratorKt
                .simulateFling(mRecyclerView, startTime, 500, 500, Direction.UP);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    @Test
    public void uiFling_horizontal_parentHasNestedScrollingChildWithTypeFling() {
        setup(false, 100, true);
        long startTime = SystemClock.uptimeMillis();

        SimpleGestureGeneratorKt
                .simulateFling(mRecyclerView, startTime, 500, 500, Direction.LEFT);

        assertThat(mParentSpy.axesForTypeTouch, is(ViewCompat.SCROLL_AXIS_NONE));
        assertThat(mParentSpy.axesForTypeNonTouch, is(ViewCompat.SCROLL_AXIS_HORIZONTAL));
    }

    @Test
    public void uiFling_callsNestedFlingsCorrectly() {
        setup(true, 100, true);
        long startTime = SystemClock.uptimeMillis();

        SimpleGestureGeneratorKt
                .simulateFling(mRecyclerView, startTime, 500, 500, Direction.UP);

        InOrder inOrder = Mockito.inOrder(mParentSpy);
        inOrder.verify(mParentSpy).onNestedPreFling(
                eq(mRecyclerView),
                eq(0f),
                anyFloat());
        inOrder.verify(mParentSpy).onNestedFling(
                eq(mRecyclerView),
                eq(0f),
                anyFloat(),
                eq(true));
    }

    @Test
    public void uiDown_duringFling_stopsNestedScrolling() {

        // Arrange

        setup(true, 1000, true);

        final Context context = ApplicationProvider.getApplicationContext();
        FlingData flingData = SimpleGestureGeneratorKt.generateFlingData(context);

        final long firstDownTime = SystemClock.uptimeMillis();
        // Should be after fling events occurred.
        final long secondDownTime = firstDownTime + flingData.getTime() + 100;

        List<MotionEventData> motionEventData = SimpleGestureGeneratorKt
                .generateFlingMotionEventData(flingData, 500, 500, Direction.UP);
        SimpleGestureGeneratorKt
                .dispatchTouchEvents(mRecyclerView, firstDownTime, motionEventData);

        // Sanity check that onStopNestedScroll has not yet been called of type TYPE_NON_TOUCH.
        verify(mParentSpy, never())
                .onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);

        // Act

        MotionEvent down = MotionEvent.obtain(
                secondDownTime,
                secondDownTime,
                MotionEvent.ACTION_DOWN,
                500,
                500,
                0);
        mRecyclerView.dispatchTouchEvent(down);

        // Assert

        verify(mParentSpy).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);
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
        setup(true, 100, true);
        when(mParentSpy.onNestedPreFling(eq(mRecyclerView), anyFloat(), anyFloat()))
                .thenReturn(returnValue);

        long startTime = SystemClock.uptimeMillis();
        SimpleGestureGeneratorKt
                .simulateFling(mRecyclerView, startTime, 500, 500, Direction.UP);

        if (onNestedFlingCalled) {
            verify(mParentSpy).onNestedFling(eq(mRecyclerView), anyFloat(), anyFloat(), eq(true));
        } else {
            verify(mParentSpy, never()).onNestedFling(any(View.class), anyFloat(), anyFloat(),
                    anyBoolean());
        }
    }

    @Test
    public void smoothScrollBy_doesNotStartNestedScrolling() {
        setup(true, 100, true);
        mRecyclerView.smoothScrollBy(0, 100);
        verify(mParentSpy, never()).onStartNestedScroll(
                any(View.class), any(View.class), anyInt(), anyInt());
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public int axesForTypeTouch;
        public int axesForTypeNonTouch;
        public int onNestedPreScrollConsumedX;
        public int onNestedPreScrollConsumedY;

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
            onNestedPreScrollConsumedX = consumed[0];
            onNestedPreScrollConsumedY = consumed[1];
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
        public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private Context mContext;
        private int mOrientationSize;
        private int mItemCount;
        private boolean mVertical;

        TestAdapter(Context context, float orientationSize, int itemCount, boolean vertical) {
            mContext = context;
            mOrientationSize = (int) Math.floor(orientationSize / itemCount);
            mItemCount = itemCount;
            mVertical = vertical;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            View view = new View(mContext);

            int width;
            int height;
            if (mVertical) {
                width = ViewGroup.LayoutParams.MATCH_PARENT;
                height = mOrientationSize;
            } else {
                width = mOrientationSize;
                height = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            view.setMinimumHeight(mOrientationSize);
            return new TestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

    private class TestViewHolder extends RecyclerView.ViewHolder {

        TestViewHolder(View itemView) {
            super(itemView);
        }
    }
}
