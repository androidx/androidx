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

package androidx.swiperefreshlayout.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Small integration tests that verifies correctness of {@link SwipeRefreshLayout}'s
 * NestedScrollingParent2 implementation.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SwipeRefreshLayoutNestedScrollingParent2Test {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NestedScrollingSpyView mParent;
    private View mChild;

    @Before
    public void instantiateMembers() {
        mSwipeRefreshLayout = new SwipeRefreshLayout(ApplicationProvider.getApplicationContext());
        mParent = spy(new NestedScrollingSpyView(ApplicationProvider.getApplicationContext()));
        mChild = new View(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void onStartNestedScroll_scrollAxisIncludesVerticalAndTypeTouch_returnsTrue() {
        int vertical = ViewCompat.SCROLL_AXIS_VERTICAL;
        int both = ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;
        onStartNestedScroll(vertical, ViewCompat.TYPE_TOUCH, true);
        onStartNestedScroll(both, ViewCompat.TYPE_TOUCH, true);
    }

    @Test
    public void onStartNestedScroll_typeIsNotTouch_returnsFalse() {
        int vertical = ViewCompat.SCROLL_AXIS_VERTICAL;
        int both = ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;
        onStartNestedScroll(vertical, ViewCompat.TYPE_NON_TOUCH, false);
        onStartNestedScroll(both, ViewCompat.TYPE_NON_TOUCH, false);
    }

    @Test
    public void onStartNestedScroll_scrollAxisExcludesVertical_returnsFalse() {
        int horizontal = ViewCompat.SCROLL_AXIS_HORIZONTAL;
        int neither = ViewCompat.SCROLL_AXIS_NONE;
        onStartNestedScroll(horizontal, ViewCompat.TYPE_TOUCH, false);
        onStartNestedScroll(neither, ViewCompat.TYPE_TOUCH, false);
    }

    @Test
    public void onNestedScrollAccepted_callsParentsOnStartNestedScrollWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();

        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onStartNestedScroll(
                mSwipeRefreshLayout,
                mSwipeRefreshLayout,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        verify(mParent, times(1)).onStartNestedScroll(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
    }

    @Test
    public void onNestedScrollAccepted_callsParentsOnNestedScrollAcceptedWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

        mSwipeRefreshLayout.onNestedScrollAccepted(
                mChild,
                mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onNestedScrollAccepted(
                mSwipeRefreshLayout,
                mSwipeRefreshLayout,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        verify(mParent, times(1)).onNestedScrollAccepted(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
    }

    @Test
    public void onNestedScrollAccepted_bothOrientations_pOnNestedScrollAcceptedCalledWithVert() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

        mSwipeRefreshLayout.onNestedScrollAccepted(
                mChild,
                mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL,
                ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onNestedScrollAccepted(
                any(View.class),
                any(View.class),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL),
                anyInt());
        verify(mParent, times(1)).onNestedScrollAccepted(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
    }

    @Test
    public void onNestedScrollAccepted_parentRejects_parentOnNestedScrollAcceptedNotCalled() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

        mSwipeRefreshLayout.onNestedScrollAccepted(
                mChild,
                mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        verify(mParent, never()).onNestedScrollAccepted(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
    }

    @Test
    public void onStopNestedScroll_parentOnStopNestedScrollCalledWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onStopNestedScroll(mChild, ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onStopNestedScroll(mSwipeRefreshLayout,
                ViewCompat.TYPE_TOUCH);
        verify(mParent, times(1)).onStopNestedScroll(any(View.class), anyInt());
    }

    @Test
    public void onStopNestedScroll_parentRejects_parentOnStopNestedScrollNotCalled() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL);

        mSwipeRefreshLayout.onStopNestedScroll(mChild, ViewCompat.TYPE_TOUCH);

        verify(mParent, never()).onStopNestedScroll(any(View.class), anyInt());
    }

    @Test
    public void onNestedScroll_parentOnNestedScrollCalledWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedScroll(mChild, 1, 2, 3, 4, ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onNestedScroll(
                mSwipeRefreshLayout,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH);
        verify(mParent, times(1)).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt());
    }

    @Test
    public void onNestedScroll_parentRejects_parentOnNestedScrollNotCalled() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedScroll(mChild, 1, 2, 3, 4, ViewCompat.TYPE_TOUCH);

        verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onNestedPreScroll_parentOnNestedPreScrollCalledWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedPreScroll(mChild, 1, 2, new int[]{0, 0},
                ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onNestedPreScroll(eq(mSwipeRefreshLayout), eq(1), eq(2),
                eq(new int[]{0, 0}), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, times(1)).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                any(int[].class), anyInt());
    }

    @Test
    public void onNestedPreScroll_parentRejects_parentOnNestedPreScrollNotCalled() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedPreScroll(mChild, 1, 2, new int[2],
                ViewCompat.TYPE_TOUCH);

        verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                any(int[].class), anyInt());
    }

    // onNestedPreScroll, srl dragged down, parent on NestedPreScroll called with remainder

    @Test
    public void onNestedPreScroll_mSwipeRefreshPulledPartWay_parentReceivesRemainder() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        // Make sure the distance to trigger is greater than our pull so we don't accidentally
        // fire refresh.
        mSwipeRefreshLayout.setDistanceToTriggerSync(50);
        // Pull refresh down part way first so when we scroll up, we are testing that we consumed
        // the same portion that we scrolled down and passed remainder to parent.
        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, -20, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedPreScroll(mChild, 0, 50, new int[2], ViewCompat.TYPE_TOUCH);

        verify(mParent, times(1)).onNestedPreScroll(eq(mSwipeRefreshLayout), eq(0), eq(30),
                eq(new int[]{0, 0}), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, times(1)).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                any(int[].class), anyInt());
    }

    @Test
    public void onNestedPreScroll_mSwipeRefreshPulledPartWay_mutatesConsumedCorrectAmount() {

        // Arrange

        setupNestedScrollViewWithParentAndChild();

        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

        // Mutate consumed in call to onNestedPreScroll.
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                int[] consumed = (int[]) invocation.getArguments()[3];
                consumed[1] += 5;
                return null;
            }}).when(mParent)
                .onNestedPreScroll(any(View.class), anyInt(), anyInt(), any(int[].class), anyInt());

        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild, ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        // Make sure the distance to trigger is greater than our pull so we don't accidentally
        // fire refresh.
        mSwipeRefreshLayout.setDistanceToTriggerSync(50);

        // Pull refresh down part way first so when we scroll up, we are testing that we consumed
        // the same portion that we scrolled down and passed remainder to parent.
        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, -20, ViewCompat.TYPE_TOUCH);

        int[] consumed = new int[2];

        // Act

        mSwipeRefreshLayout.onNestedPreScroll(mChild, 0, 50, consumed, ViewCompat.TYPE_TOUCH);

        // Assert

        assertThat(consumed, is(new int[]{0, 25}));
    }

    private void onStartNestedScroll(int iScrollAxis, int type, boolean oRetValue) {
        boolean retVal = mSwipeRefreshLayout.onStartNestedScroll(mChild, mChild, iScrollAxis,
                type);
        assertThat(retVal, is(oRetValue));
    }

    private void setupNestedScrollViewWithParentAndChild() {

        mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));
        mSwipeRefreshLayout.setMinimumHeight(100);

        mChild.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));
        mChild.setMinimumHeight(100);

        mSwipeRefreshLayout.addView(mChild);
        mParent.addView(mSwipeRefreshLayout);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        mParent.measure(widthMeasureSpec, heightMeasureSpec);
        mParent.layout(0, 0, 100, 100);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingParent2 {

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
        public boolean onStartNestedScroll(View child, View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(View target) {

        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        }

        @Override
        public boolean onNestedFling(View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }
    }

}
